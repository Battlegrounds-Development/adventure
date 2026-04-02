package me.remag501.adventure.manager;

import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.adventure.util.MessageUtil;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemLimiterManager {

    private final SettingsProvider settingsProvider;

    public ItemLimiterManager(SettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    public void enforcePlayerLimits(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!settingsProvider.getSettings().isItemLimiterEnabled()) return;

        World world = player.getWorld();
        Map<String, Integer> limits = settingsProvider.getSettings().getItemLimitsForWorld(world.getName());
        if (limits.isEmpty()) return;

        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            String materialName = entry.getKey();
            int maxAllowed = Math.max(0, entry.getValue());

            int totalAmount = countMaterial(player, materialName);
            if (totalAmount <= maxAllowed) continue;

            int overflow = totalAmount - maxAllowed;
            List<ItemStack> removed = removeOverflow(player, materialName, overflow);

            for (ItemStack stack : removed) {
                Item dropped = world.dropItemNaturally(player.getLocation(), stack);
                dropped.setPickupDelay(20);
            }

            String message = settingsProvider.getSettings().getItemLimiterMessage()
                    .replace("%item%", materialName)
                    .replace("%max%", String.valueOf(maxAllowed))
                    .replace("%dropped%", String.valueOf(overflow));

            player.sendMessage(MessageUtil.color(message));
        }
    }

    private int countMaterial(Player player, String materialName) {
        int total = 0;

        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) continue;
            if (!stack.getType().name().equalsIgnoreCase(materialName)) continue;
            total += stack.getAmount();
        }

        return total;
    }

    private List<ItemStack> removeOverflow(Player player, String materialName, int overflow) {
        List<ItemStack> removed = new ArrayList<>();
        if (overflow <= 0) return removed;

        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && overflow > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;
            if (!stack.getType().name().equalsIgnoreCase(materialName)) continue;

            int amountToTake = Math.min(stack.getAmount(), overflow);
            overflow -= amountToTake;

            ItemStack dropStack = stack.clone();
            dropStack.setAmount(amountToTake);
            removed.add(dropStack);

            int remain = stack.getAmount() - amountToTake;
            if (remain <= 0) {
                contents[i] = null;
            } else {
                stack.setAmount(remain);
                contents[i] = stack;
            }
        }

        player.getInventory().setContents(contents);
        player.updateInventory();
        return removed;
    }
}

