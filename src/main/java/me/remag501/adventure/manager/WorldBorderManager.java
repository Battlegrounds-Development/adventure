package me.remag501.adventure.manager;

import me.remag501.adventure.AdventurePlugin;
import me.remag501.adventure.model.ExtractionZone;
import me.remag501.adventure.model.RotationTrack;
import me.remag501.adventure.model.WorldInfo;
import me.remag501.adventure.setting.AdventureSettings;
import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.core.api.task.TaskService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class WorldBorderManager {

    private final TaskService taskService;
    private final RotationManager rotationManager;
    private final ExtractionManager extractionManager;
    private final SettingsProvider settingsProvider;

    private final Map<String, BorderState> states = new HashMap<>();

    public WorldBorderManager(TaskService taskService, RotationManager rotationManager, ExtractionManager extractionManager, SettingsProvider settingsProvider) {
        this.taskService = taskService;
        this.rotationManager = rotationManager;
        this.extractionManager = extractionManager;
        this.settingsProvider = settingsProvider;
        startTicker();
    }

    public void reload() {
        states.clear();
    }

    private void startTicker() {
        taskService.subscribe(AdventurePlugin.SYSTEM_ID, "world_border_tracks", 0, 20, false, (ticks) -> {
            tickAllTracks();
            return false;
        });
    }

    private void tickAllTracks() {
        AdventureSettings settings = settingsProvider.getSettings();
        if (!settings.isWorldBorderEnabled()) return;

        long warnSeconds = settings.getWarnMinutes() * 60L;

        for (RotationTrack track : rotationManager.getTracks()) {
            WorldInfo worldInfo = track.getCurrentWorld();
            World world = Bukkit.getWorld(worldInfo.getId());
            if (world == null) continue;

            long secondsLeft = track.getSecondsUntilNextCycle();
            BorderState state = states.computeIfAbsent(track.getId(), ignored -> new BorderState());
            WorldBorder border = world.getWorldBorder();

            if (shouldResetState(state, worldInfo.getId(), secondsLeft)) {
                initializeBorder(track, world, worldInfo, state, settings, secondsLeft, warnSeconds);
            }

            updateCenter(border, state, secondsLeft);

            if (!state.finalCollapse && secondsLeft <= warnSeconds) {
                state.finalCollapse = true;
                state.phase = BorderPhase.MOVING;
                state.phaseSecondsLeft = 0;
                broadcastToWorld(world, settings.getWorldBorderFinalPushMessage(), worldInfo);
            }

            if (state.finalCollapse) {
                forceCloseByCycleEnd(border, state, secondsLeft);
            } else {
                pulseClose(border, world, worldInfo, state, settings);
            }

            state.lastSecondsLeft = secondsLeft;
        }
    }

    private boolean shouldResetState(BorderState state, String worldId, long secondsLeft) {
        if (state.worldId == null || !state.worldId.equals(worldId)) {
            return true;
        }

        // Countdown wrapped to a new cycle.
        return secondsLeft > state.lastSecondsLeft;
    }

    private void initializeBorder(RotationTrack track, World world, WorldInfo worldInfo, BorderState state, AdventureSettings settings, long secondsLeft, long warnSeconds) {
        ExtractionZone targetZone = extractionManager.getRandomZone(world.getName());
        Location targetCenter = extractionManager.getBorderCenter(world.getName(), settings.getExtractionSpawn(), targetZone);
        if (targetCenter == null || targetCenter.getWorld() == null) {
            targetCenter = world.getSpawnLocation();
        }

        Location startCenter = getConfiguredStartCenter(world, settings);

        double finalSize = extractionManager.getFinalBorderSize(settings.getWorldBorderFinalSize(), targetZone);
        double startSize = Math.max(settings.getWorldBorderStartSize(), finalSize);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(startCenter);
        border.setSize(startSize);

        state.worldId = world.getName();
        state.currentSize = startSize;
        state.finalSize = finalSize;
        state.startCenterX = startCenter.getX();
        state.startCenterZ = startCenter.getZ();
        state.targetCenterX = targetCenter.getX();
        state.targetCenterZ = targetCenter.getZ();
        state.initialSeconds = Math.max(1L, secondsLeft);
        state.phase = BorderPhase.PAUSED;
        state.phaseSecondsLeft = settings.getWorldBorderPauseSeconds();
        state.warningSent = false;
        state.moveCount = 0;
        state.finalCollapse = false;
        state.lastSecondsLeft = secondsLeft;

        if (settings.isWorldBorderTrajectoryLog()) {
            Bukkit.getLogger().info("[Adventure] Border trajectory for track " + track.getId()
                    + ": map=" + world.getName()
                    + ", start-center=" + startCenter.getBlockX() + "," + startCenter.getBlockY() + "," + startCenter.getBlockZ()
                    + ", target-center=" + targetCenter.getBlockX() + "," + targetCenter.getBlockY() + "," + targetCenter.getBlockZ()
                    + ", start-size=" + startSize
                    + ", warn-at=" + warnSeconds + "s"
                    + ", final-size=" + finalSize);
        }

        if (state.phaseSecondsLeft <= 0) {
            startMovingPhase(border, world, worldInfo, state, settings);
        }
    }

    private void pulseClose(WorldBorder border, World world, WorldInfo worldInfo, BorderState state, AdventureSettings settings) {
        state.currentSize = border.getSize();

        if (state.phase == BorderPhase.PAUSED) {
            int warningSeconds = settings.getWorldBorderWarningSeconds();
            if (!state.warningSent && warningSeconds > 0 && state.phaseSecondsLeft <= warningSeconds) {
                String warning = settings.getWorldBorderWarningMessage()
                        .replace("%seconds%", String.valueOf(Math.max(1, state.phaseSecondsLeft)));
                broadcastToWorld(world, warning, worldInfo);
                state.warningSent = true;
            }

            state.phaseSecondsLeft--;
            if (state.phaseSecondsLeft <= 0) {
                startMovingPhase(border, world, worldInfo, state, settings);
            }
            return;
        }

        state.phaseSecondsLeft--;
        if (state.phaseSecondsLeft <= 0) {
            state.phase = BorderPhase.PAUSED;
            state.phaseSecondsLeft = settings.getWorldBorderPauseSeconds();
            state.warningSent = false;
        }
    }

    private void startMovingPhase(WorldBorder border, World world, WorldInfo worldInfo, BorderState state, AdventureSettings settings) {
        state.currentSize = border.getSize();
        if (state.currentSize <= state.finalSize) {
            state.phase = BorderPhase.PAUSED;
            state.phaseSecondsLeft = settings.getWorldBorderPauseSeconds();
            state.warningSent = false;
            return;
        }

        double progressiveMultiplier = Math.min(settings.getWorldBorderProgressiveMax(),
                1.0 + (state.moveCount * settings.getWorldBorderProgressiveStep()));
        double shrinkAmount = settings.getWorldBorderMoveAmount() * progressiveMultiplier;
        double targetSize = Math.max(state.finalSize, state.currentSize - shrinkAmount);
        int moveSeconds = Math.max(1, settings.getWorldBorderMoveSeconds());

        border.setSize(targetSize, moveSeconds);
        state.phase = BorderPhase.MOVING;
        state.phaseSecondsLeft = moveSeconds;
        state.moveCount++;

        broadcastToWorld(world, settings.getWorldBorderMoveMessage(), worldInfo);
    }

    private void forceCloseByCycleEnd(WorldBorder border, BorderState state, long secondsLeft) {
        if (secondsLeft <= 0) {
            state.currentSize = state.finalSize;
            border.setSize(state.currentSize);
            return;
        }

        state.currentSize = border.getSize();
        if (state.currentSize <= state.finalSize) {
            border.setSize(state.finalSize);
            return;
        }

        // Re-issue vanilla timed movement so final size is guaranteed exactly at cycle end.
        border.setSize(state.finalSize, Math.max(1, (int) secondsLeft));
    }

    private Location getConfiguredStartCenter(World world, AdventureSettings settings) {
        var coords = settings.getWorldBorderStartCoordinate();
        if (coords != null && coords.size() >= 3) {
            return new Location(world, coords.get(0), coords.get(1), coords.get(2));
        }
        return world.getSpawnLocation();
    }

    private void updateCenter(WorldBorder border, BorderState state, long secondsLeft) {
        if (state.initialSeconds <= 0) return;

        double progress = 1.0 - (Math.max(0L, secondsLeft) / (double) state.initialSeconds);
        progress = Math.max(0.0, Math.min(1.0, progress));

        double x = lerp(state.startCenterX, state.targetCenterX, progress);
        double z = lerp(state.startCenterZ, state.targetCenterZ, progress);
        border.setCenter(x, z);
    }

    private double lerp(double a, double b, double t) {
        return a + ((b - a) * t);
    }

    private void broadcastToWorld(World world, String messageTemplate, WorldInfo worldInfo) {
        String formatted = messageTemplate
                .replace("%map%", worldInfo.getChatName())
                .replace("%world%", world.getName());

        for (Player player : world.getPlayers()) {
            player.sendMessage(formatted);
        }
    }

    private static final class BorderState {
        private String worldId;
        private double currentSize;
        private double finalSize;
        private double startCenterX;
        private double startCenterZ;
        private double targetCenterX;
        private double targetCenterZ;
        private long initialSeconds;
        private BorderPhase phase = BorderPhase.PAUSED;
        private int phaseSecondsLeft;
        private boolean warningSent;
        private int moveCount;
        private boolean finalCollapse;
        private long lastSecondsLeft = Long.MAX_VALUE;
    }

    private enum BorderPhase {
        PAUSED,
        MOVING
    }
}
