package me.remag501.adventure.command;

import me.remag501.adventure.manager.RotationManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdventureTabCompleter implements TabCompleter {

    private final RotationManager rotationManager;

    public AdventureTabCompleter(RotationManager rotationManager) {
        this.rotationManager = rotationManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // First argument: suggest subcommands
            List<String> suggestions = new ArrayList<>();
            suggestions.add("reload");
            suggestions.add("tp");
            return suggestions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("tp")) {
            if (args.length == 2) {
                // Second argument: suggest track IDs
                return rotationManager.getTracks().stream()
                        .map(track -> track.getId())
                        .filter(id -> id.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args.length == 3) {
                // Third argument: suggest online player names
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}

