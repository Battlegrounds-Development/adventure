package me.remag501.adventure.task;

import me.clip.placeholderapi.PlaceholderAPI;
import me.remag501.adventure.AdventurePlugin;
import me.remag501.adventure.setting.AdventureSettings;
import me.remag501.adventure.manager.RotationManager;
import me.remag501.adventure.model.RotationTrack;
import me.remag501.adventure.model.WorldInfo;
import me.remag501.adventure.util.MessageUtil;
import me.remag501.core.api.task.TaskService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class BroadcastTask implements Runnable {

    private final TaskService taskService;
    private final RotationManager rotationManager;

    private AdventureSettings settings;
    private Consumer<String> onTimeUp;

    public BroadcastTask(TaskService taskService, RotationManager rotationManager, AdventureSettings settings) {
        this.taskService = taskService;
        this.rotationManager = rotationManager;
        this.settings = settings;
    }

    @Override
    public void run() {

        for (RotationTrack rotationTrack: rotationManager.getTracks()) { // Change: Loop through every track in rotation manager, use same logic

            long minutesLeft = rotationTrack.getMinutesUntilNextCycle();
            long warnMinutes = settings.getWarnMinutes();

            String currentMap = rotationTrack.getCurrentWorld().getChatName();
            String nextMap = rotationTrack.getNextWorld().getChatName();

            long secondsLeft = rotationTrack.getSecondsUntilNextCycle();
            long warnSeconds = warnMinutes * 60;

            // =======================
            // WARNING PHASE
            // =======================
            if (secondsLeft <= warnSeconds && secondsLeft > warnSeconds - 20 && !rotationTrack.isHasBroadcasted()) {

                String msg = settings.getWarnMessage();
                String formattedMsg = MessageUtil.format(msg, currentMap, nextMap, minutesLeft);
                String broadcastMsg = MessageUtil.color(rotationTrack.getCurrentWorld().getChatName()) + " §fcloses in §c" + warnMinutes + " §fminutes...";
                Bukkit.broadcastMessage(formattedMsg);
                World currentWorld = Bukkit.getWorld(rotationTrack.getCurrentWorld().getId());

                // Make warning into a title bar
                if (currentWorld != null) {
                    for (Player player : currentWorld.getPlayers()) {
                        // sendTitle(title, subtitle, fadeIn, stay, fadeOut)
                        // Using the message as the main title, and an empty string for subtitle
                        player.sendTitle(
                                broadcastMsg,
                                "",
                                10, // 0.5s fade in
                                70, // 3.5s stay
                                20  // 1.0s fade out
                        );
                    }
                }

                // Run next-world commands
                runWorldCommands(null, rotationTrack.getNextWorld());

                startWarningCountdown(rotationTrack);
                rotationTrack.setHasBroadcasted(true);
            }

            // =======================
            // ROTATION OCCURRED
            // =======================
            if (rotationTrack.isNewCycle()) {

                String msg = settings.getNewMapMessage();
                Bukkit.broadcastMessage(MessageUtil.format(msg, currentMap, nextMap, 0));

//                Bukkit.getLogger().info("Reached logic to stop via check new cycle within track");
                stopWarningCountdown(rotationTrack);
                rotationTrack.setHasBroadcasted(false);
            }

        }
    }

    // A simple setter to tell the task what to do when it finishes
    public void setOnTimeUp(Consumer<String> onTimeUp) {
        this.onTimeUp = onTimeUp;
    }
    private void runWorldCommands(Player player, WorldInfo world) {
        // 1. Get the raw list from the object
        List<String> rawCommands = world.getCommands();

        for (String rawCommand : rawCommands) {
            // 2. Parse the placeholders RIGHT NOW
            String processedCommand = PlaceholderAPI.setPlaceholders(player, rawCommand);

            // 3. Execute the command
            // Note: Use dispatchCommand for console or player.chat() for player
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        }
    }

    // =======================
    // WARNING COUNTDOWN
    // =======================
    private void startWarningCountdown(RotationTrack rotation) {

        long totalSeconds = rotation.getSecondsUntilNextCycle();

        WorldInfo worldInfo = rotation.getCurrentWorld();
        World world = Bukkit.getWorld(worldInfo.getId());
        if (world == null) return;

        BossBar warningBossBar = Bukkit.createBossBar(
                "Map closes in " + totalSeconds + "s",
                BarColor.RED,
                BarStyle.SOLID
        );

        rotation.setWarningBossBar(warningBossBar);

        // Add existing players
        world.getPlayers().forEach(warningBossBar::addPlayer);

        AtomicLong timeLeft = new AtomicLong(totalSeconds);

        taskService.subscribe(AdventurePlugin.SYSTEM_ID, "boss_bar", 0, 20, false, (ticks) -> {
            if (timeLeft.get() <= 0 || rotation.isNewCycle()) {
                // Apply penalty to OLD map
                stopWarningCountdown(rotation);
                return true;
            }

            if (timeLeft.get() > 180 )
                warningBossBar.hide();
            else if (timeLeft.get() >= 120) {
                warningBossBar.show();
                warningBossBar.setTitle("§c§lWARNING: §cExtraction closes in §c§l2 §cminutes...");
                warningBossBar.setProgress(1);
            } else if (timeLeft.get() >= 60) {
                warningBossBar.show();
                warningBossBar.setTitle("§c§lWARNING: §cExtraction closes in §c§l1 §cminute...");
                warningBossBar.setProgress(1);
            } else {
                warningBossBar.show();
                warningBossBar.setTitle("§c§lWARNING: §cExtraction closes in §c§l" + timeLeft + " §cseconds...");
                warningBossBar.setProgress(1);
            }

            timeLeft.getAndDecrement();
            return false;
        });

        // Apply penalty after warning ticks
        taskService.delay((int) (totalSeconds * 20), () -> {
            if (onTimeUp != null) {
                onTimeUp.accept(world.getName());
            }
        });

    }

    private synchronized void stopWarningCountdown(RotationTrack rotation) {

        BossBar warningBossBar = rotation.getWarningBossBar();
        Bukkit.getLogger().info("Emptying out boss bar for rotation track " + rotation.getId());

        if (warningBossBar != null) {
            warningBossBar.removeAll();
            rotation.setWarningBossBar(null); // try setting to null?
        }
    }

}
