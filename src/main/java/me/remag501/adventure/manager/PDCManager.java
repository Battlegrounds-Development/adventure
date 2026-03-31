package me.remag501.adventure.manager;

import me.remag501.core.api.namespace.NamespaceService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class PDCManager {

    private final NamespaceService namespaceService;
    private final NamespacedKey mapKey;

    public PDCManager(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
        this.mapKey = namespaceService.getMapVersionKey();
    }

    /**
     * Increments the world version. Run this when the map rotates.
     */
    public void incrementWorldVersion(World world) {
        int current = getWorldVersion(world);
        world.getPersistentDataContainer().set(mapKey, PersistentDataType.INTEGER, current + 1);
    }

    public int getWorldVersion(World world) {
        return world.getPersistentDataContainer().getOrDefault(mapKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * Gets the version specific to THIS world from the player's PDC.
     */
    public int getPlayerVersionForWorld(Player player, World world) {
        NamespacedKey playerKey = namespaceService.getMapVersionNamespace(world.getName());
        return player.getPersistentDataContainer().getOrDefault(playerKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * Stams the player with the current world version.
     * Use this after a penalty OR after a successful extraction.
     */
    public void syncPlayerToWorld(Player player, World world) {
        NamespacedKey playerKey = namespaceService.getMapVersionNamespace(world.getName());
        int currentWorldVer = getWorldVersion(world);
        player.getPersistentDataContainer().set(playerKey, PersistentDataType.INTEGER, currentWorldVer);
    }

    /**
     * Returns true if the player is behind the current world version.
     */
    public boolean isPlayerOutdated(Player player, World world) {
        // If the player has never been to this world, we don't want to penalize them.
        NamespacedKey playerKey = namespaceService.getMapVersionNamespace(world.getName());
        if (!player.getPersistentDataContainer().has(playerKey, PersistentDataType.INTEGER)) {
            syncPlayerToWorld(player, world); // Mark them as "checked in"
            return false;
        }

        Bukkit.getLogger().info("[Adventure] Comparing get player_version: " + getPlayerVersionForWorld(player, world) + " with world_version: " + getWorldVersion(world));

        return getPlayerVersionForWorld(player, world) < getWorldVersion(world);
    }
}