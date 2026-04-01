package me.remag501.adventure.manager;

import me.remag501.adventure.AdventurePlugin;
import me.remag501.adventure.setting.AdventureSettings;
import me.remag501.adventure.model.RotationTrack;
import me.remag501.adventure.model.WorldInfo;
import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.core.api.task.TaskService;
import org.bukkit.World;

import java.util.*;

public class RotationManager {

    private final TaskService taskService;
    private final SettingsProvider settingsProvider;
    private final Map<String, RotationTrack> tracks = new HashMap<>();
    private final Map<String, RotationTrack> worldToTrack = new HashMap<>();

    public RotationManager(TaskService taskService, SettingsProvider settingsProvider) {
        this.taskService = taskService;
        this.settingsProvider = settingsProvider;
        loadTracks(settingsProvider.getSettings());
        startTicker();
    }

    public void reloadSettings() {
        tracks.clear();
        worldToTrack.clear();
        loadTracks(settingsProvider.getSettings());
    }

    private void loadTracks(AdventureSettings settings) {
        // For now: one track using existing config

            for (RotationTrack track : settings.getTracks().values()) {
                tracks.put(track.getId(), track);

                for (WorldInfo world : track.getWorlds()) {
                    worldToTrack.put(world.getId(), track);
                }
            }

    }

    private void startTicker() {

        taskService.subscribe(AdventurePlugin.SYSTEM_ID, "rotation_tracks", 0, 20, (ticks) -> {
            tracks.values().forEach(RotationTrack::tick);
            return false;
        });

    }

    public RotationTrack getTrackByWorld(World world) {
        return worldToTrack.get(world.getName());
    }

    public RotationTrack getTrackById(String id) {
        return tracks.get(id);
    }

    public Collection<RotationTrack> getTracks() {
        return tracks.values();
    }

    public boolean isEntryOpen(RotationTrack track) {
        if (track == null) return false;
        long warnSeconds = settingsProvider.getSettings().getWarnMinutes() * 60L;
        return track.getSecondsUntilNextCycle() > warnSeconds;
    }

    public boolean isEntryOpen(String trackId) {
        return isEntryOpen(getTrackById(trackId));
    }
}
