package me.remag501.adventure.listener;

import me.remag501.adventure.manager.PDCManager;
import me.remag501.adventure.manager.RotationManager;
import me.remag501.adventure.model.RotationTrack;
import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.adventure.util.MessageUtil;
import me.remag501.core.api.event.EventService;
import me.remag501.core.api.namespace.NamespaceService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;

public class GuiListener {

    private final RotationManager rotationManager;
    private final PDCManager pdcManager;
    private final NamespaceService namespaceService;

    public GuiListener(EventService eventService, NamespaceService namespaceService, PDCManager pdcManager, RotationManager rotationManager, SettingsProvider provider) {
        this.pdcManager = pdcManager;
        this.rotationManager = rotationManager;
        this.namespaceService = namespaceService;

        eventService.subscribe(InventoryClickEvent.class)
                // Filter: Only handle clicks in our specific GUI
                .filter(event -> {
                    String guiTitle = MessageUtil.color(provider.getSettings().getGuiTitle());
                    return event.getView().getTitle().equals(guiTitle);
                })
                .handler(this::handleGuiClick);
    }

    private void handleGuiClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) || event.getCurrentItem() == null) return;

        if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            String actionId = event.getCurrentItem().getItemMeta()
                    .getPersistentDataContainer()
                    .get(namespaceService.getWorldIdKey(), PersistentDataType.STRING);

            if (actionId == null) return;

            for (RotationTrack rotationTrack : rotationManager.getTracks()) {
                if (actionId.equals(rotationTrack.getId())) {
                    String currentWorld = rotationTrack.getCurrentWorld().getId();

                    // Teleport and Sync
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rtp player " + player.getName() + " " + currentWorld);
                    player.closeInventory();

                    // Ensure the world exists before syncing
                    org.bukkit.World targetWorld = Bukkit.getWorld(currentWorld);
                    if (targetWorld != null) {
                        pdcManager.syncPlayerToWorld(player, targetWorld);
                    }
                    break;
                }
            }
        }
    }
}