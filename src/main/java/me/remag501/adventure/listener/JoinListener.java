package me.remag501.adventure.listener;

import me.remag501.adventure.manager.PDCManager;
import me.remag501.adventure.manager.PenaltyManager;
import me.remag501.adventure.manager.RotationManager;
import me.remag501.core.api.event.EventService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener {

    private final PDCManager pdcManager;
    private final PenaltyManager penaltyManager;

    public JoinListener(EventService eventService, PDCManager pdcManager, RotationManager rotationManager, PenaltyManager penaltyManager) {
        this.pdcManager = pdcManager;
        this.penaltyManager = penaltyManager;

        eventService.subscribe(PlayerJoinEvent.class)
                // Filter: Only process players joining a world that is part of a rotation track
                .filter(event -> rotationManager.getTrackByWorld(event.getPlayer().getWorld()) != null)
                .handler(this::handlePenaltyCheck);
    }

    private void handlePenaltyCheck(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        // Check if player version lags behind world version
        if (pdcManager.isPlayerOutdated(player, world)) {
            int pVer = pdcManager.getPlayerVersionForWorld(player, world);
            int wVer = pdcManager.getWorldVersion(world);

            Bukkit.getLogger().info("[Adventure] Penalizing " + player.getName() + ": Player Ver " + pVer + " < World Ver " + wVer);

            player.sendMessage("§c§l(!) §cYou left the game before extracting!");
            penaltyManager.penalizePlayer(player);

            // Sync them so they are safe until the next reset/rotation
            pdcManager.syncPlayerToWorld(player, world);
        }
    }
}