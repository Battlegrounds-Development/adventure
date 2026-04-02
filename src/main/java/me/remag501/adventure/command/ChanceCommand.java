package me.remag501.adventure.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChanceCommand implements CommandExecutor {

    private static final Pattern ENTRY_PATTERN = Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)%\\s*\"(.+)\"\\s*$");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("adventure.chance")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /chance <weight%\"command\",weight%\"command\",...>");
            return true;
        }

        String rawInput = String.join(" ", args);
        List<String> chunks = splitEntries(rawInput);
        if (chunks.isEmpty()) {
            sender.sendMessage("No entries found. Example: /chance 50%\"say heads\",50%\"say tails\"");
            return true;
        }

        List<WeightedCommand> entries = new ArrayList<>();
        double totalWeight = 0.0;

        for (String chunk : chunks) {
            Matcher matcher = ENTRY_PATTERN.matcher(chunk);
            if (!matcher.matches()) {
                sender.sendMessage("Invalid entry: " + chunk);
                sender.sendMessage("Use format weight%\"command\" (example: 50%\"say heads\")");
                return true;
            }

            double weight;
            try {
                weight = Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ex) {
                sender.sendMessage("Invalid percentage in entry: " + chunk);
                return true;
            }

            if (weight <= 0.0) {
                sender.sendMessage("Weight must be greater than 0: " + chunk);
                return true;
            }

            String commandToRun = matcher.group(2).trim();
            if (commandToRun.isEmpty()) {
                sender.sendMessage("Command cannot be empty: " + chunk);
                return true;
            }

            entries.add(new WeightedCommand(weight, commandToRun));
            totalWeight += weight;
        }

        if (entries.isEmpty() || totalWeight <= 0.0) {
            sender.sendMessage("No valid weighted commands were provided.");
            return true;
        }

        WeightedCommand selected = choose(entries, totalWeight);
        boolean executed = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), selected.command());

        if (!executed) {
            sender.sendMessage("Selected command failed to execute: /" + selected.command());
            return true;
        }

        sender.sendMessage("Executed: /" + selected.command());
        return true;
    }

    private List<String> splitEntries(String input) {
        List<String> entries = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                continue;
            }

            if (c == ',' && !inQuotes) {
                String part = current.toString().trim();
                if (!part.isEmpty()) {
                    entries.add(part);
                }
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        String part = current.toString().trim();
        if (!part.isEmpty()) {
            entries.add(part);
        }

        return entries;
    }

    private WeightedCommand choose(List<WeightedCommand> entries, double totalWeight) {
        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0.0;

        for (WeightedCommand entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }

        return entries.get(entries.size() - 1);
    }

    private record WeightedCommand(double weight, String command) {}
}

