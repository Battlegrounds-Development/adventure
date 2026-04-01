package me.remag501.adventure.manager;

import me.remag501.adventure.AdventurePlugin;
import me.remag501.adventure.setting.AdventureSettings;
import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.adventure.task.BroadcastTask;
import me.remag501.adventure.util.MessageUtil;
import me.remag501.core.api.task.TaskService;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PenaltyManager {

    private final TaskService taskService;
    private final BroadcastTask broadcastTask;
    private final PDCManager pdcManager;
    private final SettingsProvider settingsProvider;


    public PenaltyManager(TaskService taskService, PDCManager pdcManager, BroadcastTask broadcastTask, SettingsProvider settingsProvider) {
        this.taskService = taskService;
        this.pdcManager = pdcManager;
        this.broadcastTask = broadcastTask;
        this.settingsProvider = settingsProvider;

        // Lambda function to apply penalty based on broadcast task
        broadcastTask.setOnTimeUp(this::applyPenalty);
    }

    public void applyPenalty(String closedWorldName) {
        AdventureSettings settings = settingsProvider.getSettings();
        String penaltyMsg = settings.getPenaltyMessage();
        String soundName = settings.getPenaltySound();

        Bukkit.getLogger().info("Withering the world: " + closedWorldName);

        // Wither away online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().equals(closedWorldName)) continue;

            // Send message
            player.sendMessage(MessageUtil.color(penaltyMsg));

            // Play sound
            try {
                String formattedSound = soundName.toLowerCase().replace("_", ".");
                NamespacedKey soundKey = NamespacedKey.minecraft(formattedSound);
                // Look it up in the SOUNDS registry
                Sound sound = Registry.SOUNDS.get(soundKey);
                // Fallback logic if the sound is invalid/missing
                if (sound == null) {
                    Bukkit.getLogger().warning("[Adventure] Invalid sound in config: " + soundName + ". Defaulting to WITHER_SPAWN.");
                    sound = Sound.ENTITY_WITHER_SPAWN;
                }
                // Play the sound
                player.playSound(player.getLocation(), sound, 1f, 1f);
            } catch (IllegalArgumentException ignored) {
                Bukkit.getLogger().warning("Invalid sound in config: " + soundName);
            }

            penalizePlayer(player);

        }

        // Kill any players who logged out
        pdcManager.incrementWorldVersion(Bukkit.getWorld(closedWorldName));

    }

    public void penalizePlayer(Player player) {
        // Apply Wither
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.WITHER,
                Integer.MAX_VALUE, // Permanent
                3,
                false,
                false,
                true
        ));
        // Apply Blindness
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                Integer.MAX_VALUE, // Permanent
                1,
                false,
                false,
                true
        ));
    }

    public void startBroadcastTask() {
        taskService.subscribe(AdventurePlugin.SYSTEM_ID, "broadcast_task", 20, 20, false, (ticks) -> {
            broadcastTask.run();
            return false;
        });
    }
}
