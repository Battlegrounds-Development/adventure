package me.remag501.adventure.manager;

import me.remag501.adventure.model.ExtractionZone;
import me.remag501.adventure.setting.SettingsProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ExtractionManager {

    private final SettingsProvider settingsProvider;

    public ExtractionManager(SettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    public ExtractionZone getZone(Location loc) {
        List<ExtractionZone> zones = settingsProvider.getSettings().getExtractionZones().get(loc.getWorld().getName());
        if (zones == null) return null;

        for (ExtractionZone zone : zones) {
            if (zone.contains(loc)) {
                return zone;
            }
        }
        return null;
    }

    public boolean isInPortal(Location loc) {
        List<ExtractionZone> zones = settingsProvider.getSettings().getExtractionZones().get(loc.getWorld().getName());
        if (zones == null) return false;
        return zones.stream().anyMatch(zone -> zone.inPortal(loc));
    }

    public List<ExtractionZone> getZones(String world) {
        return settingsProvider.getSettings().getExtractionZones().getOrDefault(world, Collections.emptyList());
    }

    public ExtractionZone getRandomZone(String worldName) {
        List<ExtractionZone> zones = getZones(worldName);
        if (zones.isEmpty()) return null;
        return zones.get(ThreadLocalRandom.current().nextInt(zones.size()));
    }

    public Location getBorderCenter(String worldName, Location fallbackSpawn, ExtractionZone zone) {
        ExtractionZone selectedZone = zone != null ? zone : getRandomZone(worldName);

        if (selectedZone != null) {
            Location center = selectedZone.getCenterLocation();
            if (center != null && center.getWorld() != null) {
                return center;
            }

            Location beacon = selectedZone.getBeaconLoc();
            if (beacon != null && beacon.getWorld() != null) {
                return beacon.clone();
            }
        }

        World world = Bukkit.getWorld(worldName);
        if (fallbackSpawn != null && world != null) {
            return new Location(world, fallbackSpawn.getX(), fallbackSpawn.getY(), fallbackSpawn.getZ());
        }

        return world != null ? world.getSpawnLocation() : fallbackSpawn;
    }

    public double getFinalBorderSize(double fallbackSize, ExtractionZone zone) {
        if (zone == null) {
            return fallbackSize;
        }

        int widthX = zone.getMaxX() - zone.getMinX() + 1;
        int widthZ = zone.getMaxZ() - zone.getMinZ() + 1;
        return Math.max(1.0, Math.max(widthX, widthZ));
    }

    private int[] toIntArray(List<?> list) {
        if (list == null || list.size() < 3) return new int[]{0, 0, 0};
        return list.stream().mapToInt(o -> ((Number) o).intValue()).toArray();
    }

    private List<Double> toDoubleList(List<?> list) {
        if (list == null || list.isEmpty()) return List.of();
        List<Double> result = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Number num) result.add(num.doubleValue());
        }
        return result;
    }
}
