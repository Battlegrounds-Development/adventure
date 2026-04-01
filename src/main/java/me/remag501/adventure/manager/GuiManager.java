package me.remag501.adventure.manager;

import me.remag501.adventure.setting.AdventureSettings;
import me.remag501.adventure.model.RotationTrack;
import me.remag501.adventure.model.WorldInfo;
import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.adventure.util.MessageUtil;
import me.remag501.adventure.util.SkullUtil;
import me.remag501.core.api.namespace.NamespaceService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class GuiManager {

    private final NamespaceService namespaceService;
    private final RotationManager rotationManager;
    private final SettingsProvider settingsProvider;


    public GuiManager (NamespaceService namespaceService, RotationManager manager, SettingsProvider settingsProvider) {
        this.namespaceService = namespaceService;
        this.rotationManager = manager;
        this.settingsProvider = settingsProvider;
    }

    public void openAdventureGUI(Player player) {
        AdventureSettings settings = settingsProvider.getSettings();

        // GUI Title
        String guiTitle = MessageUtil.color(settings.getGuiTitle());
        Inventory gui = Bukkit.createInventory(null, 36, guiTitle);

        for (RotationTrack rotationTrack: rotationManager.getTracks()) {
            String currentWorld = rotationTrack.getCurrentWorld().getGuiName();
            String nextWorld = rotationTrack.getNextWorld().getGuiName();
            long minutesLeft = rotationTrack.getMinutesUntilNextCycle();
            boolean entryOpen = rotationManager.isEntryOpen(rotationTrack);

            // --- Teleport Item ---
            WorldInfo currentInfo = rotationTrack.getCurrentWorld();

            int tpSlot = rotationTrack.getGui().getTeleportSlot();
            String tpName = rotationTrack.getGui().getTeleportName()
                    .replace("%current_world%", currentWorld);

            // Update teleport name if entry is closed
            if (!entryOpen) {
                tpName = tpName.replace("&a", "&c").replace("&b", "&c").replace("&e", "&c") + " &7(Closed)";
            }

            List<String> tpLore = rotationTrack.getCurrentWorld().getLore()
                    .stream()
                    .map(line -> line.replace("%current_world%", currentWorld))
                    .map(MessageUtil::color)
                    .collect(Collectors.toList());

            String texture = currentInfo.getTexture();

            ItemStack teleportItem = SkullUtil.fromTexture(texture);
            ItemMeta tpMeta = teleportItem.getItemMeta();
            tpMeta.setDisplayName(MessageUtil.color(tpName));
            tpMeta.setLore(tpLore);
            // Add persistent data container for checking on listener
            String id = rotationTrack.getId();
            NamespacedKey guiKey = namespaceService.getWorldIdKey();
            tpMeta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, id);

            teleportItem.setItemMeta(tpMeta);

            gui.setItem(tpSlot, teleportItem);

            // --- Info (Clock) Item ---
            int infoSlot = rotationTrack.getGui().getInfoSlot();
            String infoName = rotationTrack.getGui().getInfoName();

            List<String> infoLore = rotationTrack.getGui().getInfoLore()
                    .stream()
                    .map(line -> line.replace("%minutes_left%", String.valueOf(minutesLeft)))
                    .map(line -> line.replace("%next_world%", nextWorld))
                    .map(MessageUtil::color)
                    .collect(Collectors.toList());

            // Use different material if entry is closed
            Material clockMaterial = entryOpen ? Material.CLOCK : Material.REDSTONE_BLOCK;
            ItemStack clockItem = new ItemStack(clockMaterial);
            ItemMeta clockMeta = clockItem.getItemMeta();
            String clockName = infoName;
            if (!entryOpen) {
                clockName = "&cEntry Closed";
            }
            clockMeta.setDisplayName(MessageUtil.color(clockName));
            clockMeta.setLore(infoLore);
            clockItem.setItemMeta(clockMeta);

            gui.setItem(infoSlot, clockItem);

        }

        player.openInventory(gui);

    }


}
