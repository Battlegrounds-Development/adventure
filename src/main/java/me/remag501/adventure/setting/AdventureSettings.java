package me.remag501.adventure.setting;

import me.remag501.adventure.model.*;
import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Instant;
import java.util.*;

public class AdventureSettings {

    // Rotation
    private final List<WorldInfo> worlds = new ArrayList<>();
    private final Map<String, RotationTrack> tracks = new LinkedHashMap<>();

    // Broadcast
    private final int warnMinutes;
    private final String warnMessage;
    private final String newMapMessage;

    // Penalty
    private final String penaltyMessage;
    private final String penaltySound;

    // Extraction
    private final int extractionDuration;
    private final int portalOpenSeconds;
    private final int downSeconds;
    private final Location extractionSpawn;
    private final String extractionStart;
    private final String extractionBossTitle;
    private final String extractionSuccess;
    private final String extractionPortalOpen;
    private final String extractionDown;
    private final String extractionCancel;
//    private final Map<String, String> extractionMessages = new HashMap<>();
    private final Map<String, List<ExtractionZone>> extractionZones = new HashMap<>();

    // Alert Settings
    private final int alertSeconds;
    private final boolean alertParticles;
    private final boolean alertFireworks;
    private final String alertSound;
    private final boolean alertUsePlayerLoc;
    private final Location alertFixedLocation;

    // GUI
    private final String guiTitle;

    // Weather
    private final List<WeatherModel> weatherModels = new ArrayList<>();

    // World border
    private final boolean worldBorderEnabled;
    private final double worldBorderStartSize;
    private final double worldBorderFinalSize;
    private final int worldBorderMoveSeconds;
    private final int worldBorderPauseSeconds;
    private final double worldBorderMoveAmount;
    private final List<Double> worldBorderStartCoordinate;
    private final int worldBorderWarningSeconds;
    private final double worldBorderProgressiveStep;
    private final double worldBorderProgressiveMax;
    private final boolean worldBorderTrajectoryLog;
    private final String worldBorderWarningMessage;
    private final String worldBorderMoveMessage;
    private final String worldBorderFinalPushMessage;

    public AdventureSettings(FileConfiguration config) {
        // --- Rotation ---
        ConfigurationSection rotationSec = config.getConfigurationSection("rotation");
        if (rotationSec == null) {
            throw new IllegalStateException("rotation section missing");
        }

        for (String trackId : rotationSec.getKeys(false)) {

            ConfigurationSection trackSec = rotationSec.getConfigurationSection(trackId);
            if (trackSec == null) continue;

            int cycleMinutes = trackSec.getInt("cycle-minutes");
            Instant startCycle = Instant.parse(trackSec.getString("start-cycle"));

            // --- GUI ---
            TrackGuiConfig gui = new TrackGuiConfig(
                    trackSec.getConfigurationSection("gui"),
                    this::color,
                    this::colorList
            );

            // --- Worlds ---
            List<WorldInfo> worlds = new ArrayList<>();
            List<Map<?, ?>> worldList = trackSec.getMapList("worlds");

            for (Map<?, ?> map : worldList) {
                String id = (String) map.get("id");
                String texture = (String) map.get("texture");
                String chatName = color((String) map.get("chat-name"));
                String guiName = color((String) map.get("gui-name"));

                List<String> lore = colorList((List<String>) map.get("lore"));
                List<String> commands = (List<String>) map.get("commands");

                worlds.add(new WorldInfo(id, texture, chatName, guiName, lore, commands));
            }

            Bukkit.getLogger().info(worlds.toString());

            RotationTrack track = new RotationTrack(
                    trackId,
                    worlds,
                    cycleMinutes,
                    startCycle,
                    gui
            );

            tracks.put(trackId, track);
        }


        // --- Broadcast & Penalty ---
        this.warnMinutes = config.getInt("broadcast.warn-minutes");
        this.warnMessage = color(config.getString("broadcast.warn-message"));
        this.newMapMessage = color(config.getString("broadcast.new-map-message"));
        this.penaltyMessage = color(config.getString("penalty.message"));
        this.penaltySound = config.getString("penalty.sound");

        // --- Extraction ---
        this.extractionDuration = config.getInt("extraction.duration");
        this.portalOpenSeconds = config.getInt("extraction.portal-open-seconds");
        this.downSeconds = config.getInt("extraction.down-seconds");
        this.extractionSpawn = new Location(
                Bukkit.getWorld(config.getString("extraction.spawn.world")),
                config.getDouble("extraction.spawn.x"),
                config.getDouble("extraction.spawn.y"),
                config.getDouble("extraction.spawn.z")
        );

        // Extraction messages
        this.extractionStart = config.getString("extraction.message.start");
        this.extractionBossTitle = config.getString("extraction.message.boss-title");
        this.extractionSuccess = config.getString("extraction.message.success");
        this.extractionPortalOpen = config.getString("extraction.message.portal-open");
        this.extractionDown = config.getString("extraction.message.down");
        this.extractionCancel = config.getString("extraction.message.cancel");

        // Parse Extraction Zones
        ConfigurationSection zonesSec = config.getConfigurationSection("extraction.zones");
        if (zonesSec != null) {
            for (String worldKey : zonesSec.getKeys(false)) {
                List<ExtractionZone> zonesForWorld = new ArrayList<>();
                List<Map<?, ?>> zoneList = config.getMapList("extraction.zones." + worldKey);

                for (Map<?, ?> map : zoneList) {
                    zonesForWorld.add(parseZone(worldKey, map));
                }
                extractionZones.put(worldKey, zonesForWorld);
            }
        }

        // Extraction Alert
        this.alertSeconds = config.getInt("extraction.alert-seconds");
        this.alertParticles = config.getBoolean("extraction.alert.particles");
        this.alertFireworks = config.getBoolean("extraction.alert.fireworks");
        this.alertSound = config.getString("extraction.alert.sound");
        this.alertUsePlayerLoc = config.getBoolean("extraction.alert.location.use-player");
        this.alertFixedLocation = new Location(
                Bukkit.getWorld(config.getString("extraction.alert.location.world")),
                config.getDouble("extraction.alert.location.x"),
                config.getDouble("extraction.alert.location.y"),
                config.getDouble("extraction.alert.location.z")
        );

        // --- GUI ---
        this.guiTitle = color(config.getString("gui.title"));

        // --- Weather ---
        List<Map<?, ?>> weatherList = config.getMapList("weather");
        for (Map<?, ?> entry : weatherList) {
            weatherModels.add(new WeatherModel(
                    (String) entry.get("type"),
                    (String) entry.get("world"),
                    (int) entry.get("min_duration"),
                    (int) entry.get("max_duration"),
                    (int) entry.get("min_frequency"),
                    (int) entry.get("max_frequency")
            ));
        }

        // --- World Border ---
        this.worldBorderEnabled = config.getBoolean("world-border.enabled", true);
        this.worldBorderStartSize = Math.max(1.0, config.getDouble("world-border.start-size", 350.0));
        this.worldBorderFinalSize = Math.max(1.0, config.getDouble("world-border.final-size", 1.0));
        this.worldBorderMoveSeconds = Math.max(1, config.getInt("world-border.move-seconds", 20));
        this.worldBorderPauseSeconds = Math.max(0, config.getInt("world-border.pause-seconds", 10));
        this.worldBorderMoveAmount = Math.max(0.01, config.getDouble("world-border.move-amount", 2.0));
        this.worldBorderStartCoordinate = toDoubleList(config.get("world-border.start-coordinate"));
        this.worldBorderWarningSeconds = Math.max(0, config.getInt("world-border.warning-seconds", 8));
        this.worldBorderProgressiveStep = Math.max(0.0, config.getDouble("world-border.progressive-step", 0.25));
        this.worldBorderProgressiveMax = Math.max(1.0, config.getDouble("world-border.progressive-max", 3.0));
        this.worldBorderTrajectoryLog = config.getBoolean("world-border.trajectory-log", true);
        this.worldBorderWarningMessage = color(config.getString("world-border.warning-message", "&e[Adventure] Border starts moving in &c%seconds%s &ein &e%map%&e."));
        this.worldBorderMoveMessage = color(config.getString("world-border.move-message", "&6[Adventure] Border is moving in &e%map%&6."));
        this.worldBorderFinalPushMessage = color(config.getString("world-border.final-push-message", "&c[Adventure] Final collapse has started in &e%map%&c!"));
    }

    private ExtractionZone parseZone(String worldName, Map<?, ?> map) {
        int[] min = toIntArray(map.get("min"));
        int[] max = toIntArray(map.get("max"));

        // Handle optional portal/beacon data
        int[] pMin = map.containsKey("portal_min") ? toIntArray(map.get("portal_min")) : min;
        int[] pMax = map.containsKey("portal_max") ? toIntArray(map.get("portal_max")) : max;

        List<Double> beacon = toDoubleList(map.get("beacon_loc"));
        List<Double> particle = toDoubleList(map.get("particle_loc"));

        return new ExtractionZone(worldName, min, max, pMin, pMax, beacon, particle);
    }

    // --- Helpers ---

    private String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    private List<String> colorList(List<String> list) {
        list.replaceAll(this::color);
        return list;
    }

    private int[] toIntArray(Object obj) {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return new int[]{((Number) list.get(0)).intValue(), ((Number) list.get(1)).intValue(), ((Number) list.get(2)).intValue()};
        }
        return new int[]{0, 0, 0};
    }

    private List<Double> toDoubleList(Object obj) {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return Arrays.asList(((Number) list.get(0)).doubleValue(), ((Number) list.get(1)).doubleValue(), ((Number) list.get(2)).doubleValue());
        }
        return null;
    }

    // --- Getters ---
    public List<WorldInfo> getWorlds() { return worlds; }
    public List<WeatherModel> getWeatherModels() { return weatherModels; }
    public List<ExtractionZone> getZonesForWorld(String worldId) { return extractionZones.getOrDefault(worldId, Collections.emptyList()); }

    // Remaining getters

    public int getWarnMinutes() {
        return warnMinutes;
    }

    public String getWarnMessage() {
        return warnMessage;
    }

    public String getNewMapMessage() {
        return newMapMessage;
    }

    public String getPenaltyMessage() {
        return penaltyMessage;
    }

    public String getPenaltySound() {
        return penaltySound;
    }

    public int getExtractionDuration() {
        return extractionDuration;
    }

    public Location getExtractionSpawn() {
        return extractionSpawn;
    }

    public String getExtractionStart() {
        return extractionStart;
    }

    public String getExtractionBossTitle() {
        return extractionBossTitle;
    }

    public String getExtractionSuccess() {
        return extractionSuccess;
    }

    public String getExtractionPortalOpen() {
        return extractionPortalOpen;
    }

    public String getExtractionDown() {
        return extractionDown;
    }

    public String getExtractionCancel() {
        return extractionCancel;
    }

    public Map<String, List<ExtractionZone>> getExtractionZones() {
        return extractionZones;
    }

    public int getAlertSeconds() {
        return alertSeconds;
    }

    public boolean isAlertParticles() {
        return alertParticles;
    }

    public boolean isAlertFireworks() {
        return alertFireworks;
    }

    public String getAlertSound() {
        return alertSound;
    }

    public boolean isAlertUsePlayerLoc() {
        return alertUsePlayerLoc;
    }

    public Location getAlertFixedLocation() {
        return alertFixedLocation;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public int getPortalOpenSeconds() {
        return portalOpenSeconds;
    }

    public int getDownSeconds() {
        return downSeconds;
    }

    public Map<String, RotationTrack> getTracks() {
        return tracks;
    }

    public boolean isWorldBorderEnabled() {
        return worldBorderEnabled;
    }

    public double getWorldBorderStartSize() {
        return worldBorderStartSize;
    }

    public double getWorldBorderFinalSize() {
        return worldBorderFinalSize;
    }

    public int getWorldBorderMoveSeconds() {
        return worldBorderMoveSeconds;
    }

    public int getWorldBorderPauseSeconds() {
        return worldBorderPauseSeconds;
    }

    public double getWorldBorderMoveAmount() {
        return worldBorderMoveAmount;
    }

    public List<Double> getWorldBorderStartCoordinate() {
        return worldBorderStartCoordinate;
    }

    public int getWorldBorderWarningSeconds() {
        return worldBorderWarningSeconds;
    }

    public double getWorldBorderProgressiveStep() {
        return worldBorderProgressiveStep;
    }

    public double getWorldBorderProgressiveMax() {
        return worldBorderProgressiveMax;
    }

    public boolean isWorldBorderTrajectoryLog() {
        return worldBorderTrajectoryLog;
    }

    public String getWorldBorderWarningMessage() {
        return worldBorderWarningMessage;
    }

    public String getWorldBorderMoveMessage() {
        return worldBorderMoveMessage;
    }

    public String getWorldBorderFinalPushMessage() {
        return worldBorderFinalPushMessage;
    }

}
