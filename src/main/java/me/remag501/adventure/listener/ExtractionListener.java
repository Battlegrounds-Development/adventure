package me.remag501.adventure.listener;

import me.remag501.adventure.AdventurePlugin;
import me.remag501.adventure.manager.ExtractionManager;
import me.remag501.adventure.manager.RotationManager;
import me.remag501.adventure.model.ExtractionZone;
import me.remag501.adventure.model.RotationTrack;
import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.adventure.util.MessageUtil;
import me.remag501.core.api.event.EventService;
import me.remag501.core.api.task.TaskService;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExtractionListener {

    private final TaskService taskService;
    private final ExtractionManager extractionManager;
    private final RotationManager rotationManager;
    private final SettingsProvider provider;

    public ExtractionListener(EventService eventService, TaskService taskService, ExtractionManager extractionManager, RotationManager rotationManager, SettingsProvider provider) {
        this.taskService = taskService;
        this.extractionManager = extractionManager;
        this.rotationManager = rotationManager;
        this.provider = provider;

        // Register via EventService
        eventService.subscribe(PlayerMoveEvent.class)
                .filter(this::shouldProcessMove)
                .handler(this::handleMove);
    }

    /**
     * High-performance filter to discard irrelevant movements before logic processing.
     */
    private boolean shouldProcessMove(PlayerMoveEvent event) {
        // 1. Ignore micro movements (same block)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return false;
        }

        // 2. Ignore players not in an active rotation world
        Player player = event.getPlayer();
        RotationTrack track = rotationManager.getTrackByWorld(player.getWorld());
        if (track == null) return false;

//        return player.getWorld().getName().equals(track.getCurrentWorld().getId());
        return true; // allows extraction zones to work across all worlds in the rotation, not just the current one
    }

    private void handleMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Find the zones
        ExtractionZone fromZone = extractionManager.getZone(event.getFrom());
        ExtractionZone toZone = extractionManager.getZone(event.getTo());

        // --- PART A: Handle Player Status (Adding/Removing from Bars) ---

        // A1. Player is leaving a zone
        if (fromZone != null && fromZone != toZone) {
            if (fromZone.isExtracting()) {
                if (fromZone.getExtractionBossBar() != null) {
                    fromZone.getExtractionBossBar().removePlayer(player);
                }

                fromZone.removeExtractingPlayer(player);

                if (fromZone.isEmpty()) {
                    fromZone.cancelExtraction(taskService);
                    String cancelMessage = MessageUtil.color(provider.getSettings().getExtractionCancel());
                    messagePlayersInZone(fromZone, cancelMessage);
                }
            }
            else if (fromZone.isPortalOpen() && fromZone.getPortalBossBar() != null) {
                fromZone.getPortalBossBar().removePlayer(player);
            }

            if (fromZone.isDown() && fromZone.getCooldownBossBar() != null) {
                fromZone.getCooldownBossBar().removePlayer(player);
            }
        }

        // A2. Player is entering a zone
        if (toZone != null && fromZone != toZone) {
            if (toZone.isExtracting()) {
                toZone.addExtractingPlayer(player);
            }
            syncPlayerBossBar(player, toZone);
        }

        // --- PART B: Handle Extraction Start Logic ---
        if (toZone != null && toZone.isEnabled() && !toZone.isExtracting()) {
            startExtraction(player, toZone);
        }
    }

    private void startExtraction(Player player, ExtractionZone zone) {
        String rawStartMsg = provider.getSettings().getExtractionStart();
        String zoneNamePlaceholder = "Extraction Zone";
        String startMsg = MessageUtil.color(rawStartMsg
                .replace("%seconds%", String.valueOf(provider.getSettings().getExtractionDuration()))
                .replace("%zone%", zoneNamePlaceholder));

        messagePlayersInZone(zone, startMsg);

        String title = MessageUtil.color(provider.getSettings().getExtractionBossTitle());
        BossBar bossBar = Bukkit.createBossBar(title, BarColor.GREEN, BarStyle.SOLID);

        bossBar.addPlayer(player);

        World world = player.getWorld();
        for (Player p : world.getPlayers()) {
            if (zone.contains(p.getLocation())) {
                bossBar.addPlayer(p);
            }
        }

        zone.startExtraction(null, bossBar, player);

        final int[] timeLeft = {provider.getSettings().getExtractionDuration()};
        final AtomicBoolean[] alertTriggered = {new AtomicBoolean(false)};

        taskService.subscribe(player.getUniqueId(), "extraction", 0, 20, false, (ticks) -> {
                if (!zone.isExtracting() || zone.getExtractionBossBar() == null) {
                    return true;
                }

                if (timeLeft[0] <= 0) {
                    if (!zone.isEmpty()) {
                        completeExtraction(zone);
                    } else {
                        zone.cancelExtraction(taskService);
                    }
                    return true;
                }

                int alertSeconds = provider.getSettings().getAlertSeconds();
                if (timeLeft[0] == alertSeconds && !alertTriggered[0].get()) {
                    triggerAlert(zone);
                    alertTriggered[0].set(true);
                }

                zone.getExtractionBossBar().setProgress((double) timeLeft[0] / provider.getSettings().getExtractionDuration());
                String updatedTitle = title.replace("%seconds%", String.valueOf(timeLeft[0]));
                zone.getExtractionBossBar().setTitle(updatedTitle);
                timeLeft[0]--;

                return false;
        });

        zone.setExtractionTaskId(player.getUniqueId());
    }

    private void completeExtraction(ExtractionZone zone) {
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;

        Set<UUID> successfulPlayers = new HashSet<>(zone.getExtractingPlayers());
        zone.cancelExtraction(taskService);

        String successMsg = MessageUtil.color(provider.getSettings().getExtractionSuccess());

        for (UUID uuid : successfulPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(successMsg);
            }
        }

        List<Location> gateBlocks = zone.getPortalGateBlocks();
        Map<Location, Material> originalBlocks = new HashMap<>();
        for (Location loc : gateBlocks) {
            originalBlocks.put(loc, loc.getBlock().getType());
            loc.getBlock().setType(Material.AIR);
        }

        zone.setEnabled(false);
        zone.setPortalOpen(true);
        updateBeaconColor(zone, Material.AIR, Material.AIR);

        int portalOpenSeconds = provider.getSettings().getPortalOpenSeconds();
        String portalOpenMessage = provider.getSettings().getExtractionPortalOpen()
                .replace("%seconds%", String.valueOf(portalOpenSeconds));

        messagePlayersInZone(zone, portalOpenMessage);

        BossBar portalBar = Bukkit.createBossBar(
                "§aPortal is open for §a§l" + portalOpenSeconds + " §aseconds...",
                BarColor.GREEN, BarStyle.SOLID
        );

        zone.setPortalBossBar(portalBar);

        for (Player p : world.getPlayers()) {
            if (zone.contains(p.getLocation())) {
                portalBar.addPlayer(p);
            }
        }

        final int[] timeLeft = {portalOpenSeconds};

        taskService.subscribe(AdventurePlugin.SYSTEM_ID, "extraction_portal", 0, 20, false, (ticks) -> {
                if (timeLeft[0] <= 0) {
                    portalBar.removeAll();
                    zone.setPortalOpen(false);

                    for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
                        entry.getKey().getBlock().setType(entry.getValue());
                    }

                    int zoneDownSeconds = provider.getSettings().getDownSeconds();
                    messagePlayersInZone(zone, provider.getSettings().getExtractionDown().replace("%seconds%", String.valueOf(zoneDownSeconds)));

                    updateBeaconColor(zone, Material.RED_STAINED_GLASS, Material.RED_STAINED_GLASS);

                    BossBar downBar = Bukkit.createBossBar(
                            "§cExtraction is down for §c§l" + zoneDownSeconds + " §cseconds...",
                            BarColor.RED, BarStyle.SOLID
                    );

                    for (Player p : world.getPlayers()) {
                        if (zone.contains(p.getLocation())) {
                            downBar.addPlayer(p);
                        }
                    }
                    zone.setCooldownState(downBar);

                    final int[] downTimeLeft = {zoneDownSeconds};

                    taskService.subscribe(AdventurePlugin.SYSTEM_ID, "extraction_beacon", 0, 20, false, (tickss) -> {
                        if (downTimeLeft[0] <= 0) {
                            downBar.removeAll();
                            zone.endCooldown();
                            updateBeaconColor(zone, Material.LIME_STAINED_GLASS, Material.LIME_STAINED_GLASS);
                            messagePlayersInZone(zone, "&aExtraction zone is now open again!");
                            return true;
                        }
                        downBar.setProgress((double) downTimeLeft[0] / zoneDownSeconds);
                        downBar.setTitle("§cExtraction is down for §c§l" + downTimeLeft[0] + " §cseconds...");
                        downTimeLeft[0]--;
                        return false;
                    });
                    return true;
                } else {
                    portalBar.setProgress((double) timeLeft[0] / portalOpenSeconds);
                    portalBar.setTitle("§aPortal is open for §a§l" + timeLeft[0] + " §aseconds...");
                    timeLeft[0]--;
                }

            return false;
        });

    }

    private void updateBeaconColor(ExtractionZone zone, Material glassType1, Material glassType2) {
        Location beaconLoc = zone.getBeaconLoc();
        if (beaconLoc == null) return;
        World world = beaconLoc.getWorld();
        if (world == null) return;

        world.getChunkAtAsync(beaconLoc.getBlockX() >> 4, beaconLoc.getBlockZ() >> 4, false, chunk -> {
            taskService.delay(1, () -> {
                beaconLoc.clone().add(0, 1, 0).getBlock().setType(glassType1, false);
                beaconLoc.clone().add(0, 2, 0).getBlock().setType(glassType2, false);
            });
        });
    }

    private void messagePlayersInZone(ExtractionZone zone, String message) {
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;
        String colorMessage = MessageUtil.color(message);
        for (Player p : world.getPlayers()) {
            if (zone.contains(p.getLocation())) p.sendMessage(colorMessage);
        }
    }

    private void syncPlayerBossBar(Player player, ExtractionZone zone) {
        BossBar activeBar = null;
        if (zone.isExtracting()) activeBar = zone.getExtractionBossBar();
        else if (zone.isPortalOpen()) activeBar = zone.getPortalBossBar();
        else if (zone.isDown()) activeBar = zone.getCooldownBossBar();

        if (activeBar != null && !activeBar.getPlayers().contains(player)) {
            activeBar.addPlayer(player);
        }
    }

    private void triggerAlert(ExtractionZone zone) {
        String rawSound = provider.getSettings().getAlertSound();
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;

        NamespacedKey soundKey = NamespacedKey.minecraft(rawSound.toLowerCase().replace("entity_ender_dragon_growl", "entity.ender_dragon.ambient"));
        Sound alertSound = Registry.SOUNDS.get(soundKey);
        if (alertSound == null) alertSound = Sound.ENTITY_ENDER_DRAGON_GROWL;

        for (Player p : world.getPlayers()) {
            Location soundLoc = zone.getBeaconLoc() != null ? zone.getBeaconLoc() : p.getLocation();
            p.playSound(soundLoc, alertSound, 2.0f, 1.0f);
        }

        Location particleLoc = zone.getParticleLoc() != null ? zone.getParticleLoc() : zone.getBeaconLoc();
        if (particleLoc != null) {
            world.spawnParticle(Particle.FIREWORK, particleLoc, 100, 0.5, 0.5, 0.5, 0.5);
            world.spawnParticle(Particle.DUST, particleLoc, 50, 1.0, 1.0, 1.0, 0.0, new Particle.DustOptions(Color.LIME, 1.5f));
            world.spawnParticle(Particle.DUST, particleLoc, 50, 1.0, 1.0, 1.0, 0.0, new Particle.DustOptions(Color.YELLOW, 1.5f));
            world.spawnParticle(Particle.EXPLOSION, particleLoc, 1, 0, 0, 0, 0);
        }
    }
}