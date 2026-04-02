package me.remag501.adventure.listener;

import me.remag501.adventure.manager.ItemLimiterManager;
import me.remag501.core.api.event.EventService;
import me.remag501.core.api.task.TaskService;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class ItemLimiterListener {

    private final TaskService taskService;
    private final ItemLimiterManager itemLimiterManager;

    public ItemLimiterListener(EventService eventService, TaskService taskService, ItemLimiterManager itemLimiterManager) {
        this.taskService = taskService;
        this.itemLimiterManager = itemLimiterManager;

        eventService.subscribe(EntityPickupItemEvent.class)
                .filter(event -> event.getEntity() instanceof Player)
                .handler(event -> enforceNextTick((Player) event.getEntity()));

        eventService.subscribe(InventoryClickEvent.class)
                .filter(event -> event.getWhoClicked() instanceof Player)
                .handler(event -> enforceNextTick((Player) event.getWhoClicked()));

        eventService.subscribe(InventoryDragEvent.class)
                .filter(event -> event.getWhoClicked() instanceof Player)
                .handler(event -> enforceNextTick((Player) event.getWhoClicked()));

        eventService.subscribe(PlayerJoinEvent.class)
                .handler(event -> enforceNextTick(event.getPlayer()));

        eventService.subscribe(PlayerChangedWorldEvent.class)
                .handler(event -> enforceNextTick(event.getPlayer()));
    }

    private void enforceNextTick(Player player) {
        taskService.delay(1, () -> itemLimiterManager.enforcePlayerLimits(player));
    }
}

