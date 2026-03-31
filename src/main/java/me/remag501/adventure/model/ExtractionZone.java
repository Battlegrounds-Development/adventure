package me.remag501.adventure.model;

import me.remag501.core.api.task.TaskService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ExtractionZone {

    private final String world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    private final int portalMinX, portalMinY, portalMinZ;
    private final int portalMaxX, portalMaxY, portalMaxZ;

    private final Location beaconLoc;
    private final Location particleLoc;

    private boolean enabled;
    private boolean portalOpen;

    // --- ZONE STATE FIELDS ---
    private BossBar extractionBossBar;
    private UUID extractionTaskId; // REPLACED: BukkitRunnable -> UUID handle
    // Players currently in the zone during countdown
    private final Set<UUID> extractingPlayers = new HashSet<>();

    // BossBar used during the "Portal Open" period (NEW)
    private BossBar portalBossBar;

    // BossBar used during the "Zone Down" period (for visibility fix)
    private BossBar cooldownBossBar;

    // Original Constructor
    public ExtractionZone(String world, int[] min, int[] max, int[] portalMin, int[] portalMax,
                          List<Double> beaconLoc, List<Double> particleLoc) {

        this.world = world;

        // Main zone
        this.minX = Math.min(min[0], max[0]);
        this.minY = Math.min(min[1], max[1]);
        this.minZ = Math.min(min[2], max[2]);
        this.maxX = Math.max(min[0], max[0]);
        this.maxY = Math.max(min[1], max[1]);
        this.maxZ = Math.max(min[2], max[2]);

        // Portal bounds
        this.portalMinX = Math.min(portalMin[0], portalMax[0]);
        this.portalMinY = Math.min(portalMin[1], portalMax[1]);
        this.portalMinZ = Math.min(portalMin[2], portalMax[2]);
        this.portalMaxX = Math.max(portalMin[0], portalMax[0]);
        this.portalMaxY = Math.max(portalMin[1], portalMax[1]);
        this.portalMaxZ = Math.max(portalMin[2], portalMax[2]);

        // Beacon location
        this.beaconLoc = beaconLoc != null && beaconLoc.size() == 3
                ? new Location(Bukkit.getWorld(world), beaconLoc.get(0), beaconLoc.get(1), beaconLoc.get(2))
                : null;

        // Particle location
        this.particleLoc = particleLoc != null && particleLoc.size() == 3
                ? new Location(Bukkit.getWorld(world), particleLoc.get(0), particleLoc.get(1), particleLoc.get(2))
                : null;

        enabled = true;
    }

    // --- GETTERS/SETTERS FOR STATE ---

    /** Checks if the extraction timer is currently running. */
    public boolean isExtracting() {
        return extractionTaskId != null;
    }

    /** Checks if the zone is currently in the disabled cooldown phase. */
    public boolean isDown() {
        return !enabled && !portalOpen;
    }

    public BossBar getExtractionBossBar() {
        return extractionBossBar;
    }

    // NEW: Getter for the Portal BossBar
    public BossBar getPortalBossBar() {
        return portalBossBar;
    }

    // NEW: Setter for the Portal BossBar
    public void setPortalBossBar(BossBar portalBossBar) {
        this.portalBossBar = portalBossBar;
    }

    public BossBar getCooldownBossBar() {
        return cooldownBossBar;
    }

    /** Checks if there are any players currently in the extraction zone. */
    public boolean isEmpty() {
        return extractingPlayers.isEmpty();
    }

    /** Returns the UUIDs of players currently standing in the extraction zone. */
    public Set<UUID> getExtractingPlayers() {
        return extractingPlayers;
    }

    /** Sets the Task ID handle. */
    public void setExtractionTaskId(UUID taskId) {
        this.extractionTaskId = taskId;
    }

    // --- METHODS FOR STATE MANAGEMENT ---

    /** Initializes the extraction state for the zone. */
    public void startExtraction(BukkitRunnable task, BossBar bar, Player initialPlayer) {
        this.extractionBossBar = bar;
        // The listener calls setExtractionTask separately after creating the Runnable
        addExtractingPlayer(initialPlayer);
    }

    /** Adds a player to the tracking set. */
    public void addExtractingPlayer(Player player) {
        extractingPlayers.add(player.getUniqueId());
    }

    /** Removes a player from the tracking set. */
    public void removeExtractingPlayer(Player player) {
        extractingPlayers.remove(player.getUniqueId());
        // BossBar removal is not needed here as the bar is global to the world.
    }

    /** Cleans up and resets the active extraction state. */
    public void cancelExtraction(TaskService taskService) {
        if (extractionTaskId != null) {
            // Use your Core API to kill the task
            taskService.stopTask(extractionTaskId, "extraction");
            extractionTaskId = null;
        }
        if (extractionBossBar != null) {
            extractionBossBar.removeAll();
            extractionBossBar = null;
        }
        extractingPlayers.clear();
    }

    /** Sets the active cooldown state and BossBar. */
    public void setCooldownState(BossBar bar) {
        this.cooldownBossBar = bar;
    }

    /** Cleans up and resets the cooldown state, re-enabling the zone. */
    public void endCooldown() {
        if (cooldownBossBar != null) {
            cooldownBossBar.removeAll();
        }
        cooldownBossBar = null;
        this.enabled = true; // Zone is now truly ready
    }

    // --- EXISTING METHODS ---

    public boolean isPortalOpen() {
        return portalOpen;
    }

    public void setPortalOpen(boolean portalOpen) {
        this.portalOpen = portalOpen;
        // Clean up the portal boss bar when the portal closes
        if (!portalOpen && portalBossBar != null) {
            portalBossBar.removeAll();
            portalBossBar = null;
        }
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public boolean inPortal(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= portalMinX && x <= portalMaxX &&
                y >= portalMinY && y <= portalMaxY &&
                z >= portalMinZ && z <= portalMaxZ;
    }

    public Location getBeaconLoc() {
        return beaconLoc;
    }

    public Location getParticleLoc() {
        return particleLoc;
    }

    public String getWorld() {
        return world;
    }

    public List<Location> getPortalGateBlocks() {
        List<Location> blocks = new ArrayList<>();
        World w = Bukkit.getWorld(world);
        if (w == null) return blocks;

        for (int x = portalMinX; x <= portalMaxX; x++) {
            for (int y = portalMinY; y <= portalMaxY; y++) {
                for (int z = portalMinZ; z <= portalMaxZ; z++) {
                    blocks.add(new Location(w, x, y, z));
                }
            }
        }
        return blocks;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getReopenSeconds() {
        return 179;
    }

    public int getOpenSeconds() {
        return 11;
    }
}
