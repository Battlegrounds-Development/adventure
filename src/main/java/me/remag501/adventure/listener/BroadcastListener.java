package me.remag501.adventure.listener;

import me.remag501.adventure.manager.RotationManager;
import me.remag501.adventure.model.RotationTrack;
import me.remag501.core.api.event.EventService;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class BroadcastListener {

    private final RotationManager rotationManager;

    public BroadcastListener(EventService eventService, RotationManager rotationManager) {
        this.rotationManager = rotationManager;

        // Self-register using the new EventService API
        eventService.subscribe(PlayerChangedWorldEvent.class)
                .handler(this::handleWorldChange);
    }

    private void handleWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String currentWorldName = player.getWorld().getName();

        for (RotationTrack track : rotationManager.getTracks()) {
            BossBar bar = track.getWarningBossBar();
            if (bar == null) continue;

            String activeWorldId = track.getCurrentWorld().getId();

            if (currentWorldName.equals(activeWorldId)) {
                bar.addPlayer(player);
            } else {
                bar.removePlayer(player);
            }
        }
    }
}