package me.remag501.adventure.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChanceTabCompleter implements TabCompleter {

    private static final List<String> PERCENT_PRESETS = List.of(
            "10",
            "25",
            "50",
            "75"
    );
    private static final Pattern PERCENT_AND_BODY = Pattern.compile("^\\s*(10|25|50|75)%\\s*(.*)$");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("adventure.chance")) {
            return List.of();
        }

        String raw = String.join(" ", args);
        String normalized = raw == null ? "" : raw;

        int lastComma = findLastCommaOutsideQuotes(normalized);
        String prefix = "";
        String active = normalized.trim();

        if (lastComma >= 0) {
            prefix = normalized.substring(0, lastComma + 1);
            active = normalized.substring(lastComma + 1).trim();
        }

        String activeLower = active.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();

        if (active.isBlank()) {
            suggestPercentages(out, prefix, "");
            return out;
        }

        Matcher matcher = PERCENT_AND_BODY.matcher(active);
        if (!matcher.matches()) {
            suggestPercentages(out, prefix, activeLower);
            return out;
        }

        String percent = matcher.group(1);
        String body = matcher.group(2);
        String base = percent + "%";

        if (body.isBlank()) {
            out.add(prefix + base + "\"");
            return out;
        }

        int quoteCount = countQuotes(body);
        String current = base + body;

        if (quoteCount == 0) {
            out.add(prefix + base + "\"");
            return out;
        }

        // While typing inside the command string, suggest only the closing quote.
        if (quoteCount == 1) {
            out.add(prefix + current + "\"");
            return out;
        }

        // Once quoted command is complete, suggest the comma to start next entry.
        if (!current.endsWith(",")) {
            out.add(prefix + current + ",");
        }

        return out;
    }

    private void suggestPercentages(List<String> out, String prefix, String activeLower) {
        for (String preset : PERCENT_PRESETS) {
            String suggestion = preset + "%";
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(activeLower)) {
                out.add(prefix + suggestion);
            }
        }
    }

    private int findLastCommaOutsideQuotes(String input) {
        boolean inQuotes = false;
        int lastComma = -1;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == ',' && !inQuotes) {
                lastComma = i;
            }
        }

        return lastComma;
    }

    private int countQuotes(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '"') {
                count++;
            }
        }
        return count;
    }
}

