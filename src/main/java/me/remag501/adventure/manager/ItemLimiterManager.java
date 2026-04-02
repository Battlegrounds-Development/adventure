package me.remag501.adventure.manager;

import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.adventure.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemLimiterManager {

    private final SettingsProvider settingsProvider;
    private boolean oraxenLookupInitialized;
    private Method getIdByItemMethod;

    public ItemLimiterManager(SettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    public ItemLimitViolation getEntryViolation(Player player, String worldName) {
        if (player == null || worldName == null) return null;
        if (!settingsProvider.getSettings().isItemLimiterEnabled()) return null;

        Map<String, Integer> limits = settingsProvider.getSettings().getItemLimitsForWorld(worldName);
        if (limits.isEmpty()) return null;

        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            String ruleKey = entry.getKey();
            int maxAllowed = Math.max(0, entry.getValue());
            int current = countMatching(player, ruleKey);

            if (current > maxAllowed) {
                return new ItemLimitViolation(ruleKey, maxAllowed, current);
            }
        }

        return null;
    }

    public String formatEntryDeniedMessage(String worldName, ItemLimitViolation violation) {
        if (violation == null) return "";

        return MessageUtil.color(settingsProvider.getSettings().getItemLimiterEntryDeniedMessage()
                .replace("%world%", worldName)
                .replace("%item%", violation.itemKey())
                .replace("%max%", String.valueOf(violation.maxAllowed()))
                .replace("%current%", String.valueOf(violation.currentAmount())));
    }

    public void enforcePlayerLimits(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!settingsProvider.getSettings().isItemLimiterEnabled()) return;

        World world = player.getWorld();
        Map<String, Integer> limits = settingsProvider.getSettings().getItemLimitsForWorld(world.getName());
        if (limits.isEmpty()) return;

        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            String ruleKey = entry.getKey();
            int maxAllowed = Math.max(0, entry.getValue());

            int totalAmount = countMatching(player, ruleKey);
            if (totalAmount <= maxAllowed) continue;

            int overflow = totalAmount - maxAllowed;
            List<ItemStack> removed = removeOverflow(player, ruleKey, overflow);

            for (ItemStack stack : removed) {
                Item dropped = world.dropItemNaturally(player.getLocation(), stack);
                dropped.setPickupDelay(20);
            }

            String message = settingsProvider.getSettings().getItemLimiterMessage()
                    .replace("%item%", ruleKey)
                    .replace("%max%", String.valueOf(maxAllowed))
                    .replace("%dropped%", String.valueOf(overflow));

            player.sendMessage(MessageUtil.color(message));
        }
    }

    private int countMatching(Player player, String ruleKey) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) continue;
            if (matchesRule(stack, ruleKey)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private List<ItemStack> removeOverflow(Player player, String ruleKey, int overflow) {
        List<ItemStack> removed = new ArrayList<>();
        if (overflow <= 0) return removed;

        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && overflow > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;
            if (!matchesRule(stack, ruleKey)) continue;

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

    private boolean matchesRule(ItemStack stack, String rawRuleKey) {
        if (rawRuleKey == null || rawRuleKey.isBlank()) return false;

        String ruleKey = rawRuleKey.trim();
        int separator = ruleKey.indexOf(':');

        // Legacy support: COBWEB, DIAMOND_SWORD, etc.
        if (separator < 0) {
            return stack.getType().name().equalsIgnoreCase(ruleKey);
        }

        String namespace = ruleKey.substring(0, separator).toLowerCase();
        String value = ruleKey.substring(separator + 1);

        if ("minecraft".equals(namespace)) {
            return stack.getType().getKey().toString().equalsIgnoreCase("minecraft:" + value);
        }

        if ("oraxen".equals(namespace)) {
            String oraxenId = getOraxenItemId(stack);
            return oraxenId != null && oraxenId.equalsIgnoreCase(value);
        }

        return false;
    }

    private String getOraxenItemId(ItemStack stack) {
        Method method = resolveOraxenMethod();
        if (method == null || stack == null) return null;

        try {
            Object result = method.invoke(null, stack);
            return result instanceof String ? (String) result : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Method resolveOraxenMethod() {
        if (oraxenLookupInitialized) return getIdByItemMethod;
        oraxenLookupInitialized = true;

        if (Bukkit.getPluginManager().getPlugin("Oraxen") == null) {
            return null;
        }

        try {
            Class<?> oraxenItems = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            getIdByItemMethod = oraxenItems.getMethod("getIdByItem", ItemStack.class);
        } catch (Exception ignored) {
            getIdByItemMethod = null;
        }

        return getIdByItemMethod;
    }

    public record ItemLimitViolation(String itemKey, int maxAllowed, int currentAmount) {}
}
