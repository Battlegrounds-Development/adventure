package me.remag501.adventure.command;

import me.remag501.adventure.AdventurePlugin;
import me.remag501.adventure.manager.GuiManager;
import me.remag501.adventure.manager.PDCManager;
import me.remag501.adventure.manager.RotationManager;
import me.remag501.adventure.model.RotationTrack;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdventureCommand implements CommandExecutor {

    private final AdventurePlugin plugin;
    private final PDCManager pdcManager;
    private final GuiManager guiManager;
    private final RotationManager rotationManager;

    public AdventureCommand(AdventurePlugin plugin, PDCManager pdcManager, RotationManager rotationManager, GuiManager guiManager) {
        this.plugin = plugin;
        this.pdcManager = pdcManager;
        this.guiManager = guiManager;
        this.rotationManager = rotationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle /adventure reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("adventure.admin")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            plugin.reloadPluginConfig();
            sender.sendMessage("Adventure plugin configuration reloaded.");
            return true;
        }

        // Handle /adventure tp <track> <player>
        if (args.length == 3 && args[0].equalsIgnoreCase("tp")) {
            if (!sender.hasPermission("adventure.admin")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            String trackName = args[1];
            String playerName = args[2];

            RotationTrack track = rotationManager.getTrackById(trackName);
            if (track == null) {
                sender.sendMessage("Track not found: " + trackName);
                return true;
            }

            // Block entry if outside entry window
            if (!rotationManager.isEntryOpen(track)) {
                sender.sendMessage("Entry is closed for this track. Cannot teleport " + playerName + ".");
                return true;
            }

            // Handle teleport logic
            String currentWorld = track.getCurrentWorld().getId();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rtp player " + playerName + " " + currentWorld);
            pdcManager.syncPlayerToWorld(Bukkit.getPlayer(playerName), Bukkit.getWorld(currentWorld));

            sender.sendMessage("Attempting to teleport " + playerName + " to track: " + trackName);

            return true;
        }

        // Use GUI manager to handle adventure (Ensure sender is a player)
        if (sender instanceof Player) {
            guiManager.openAdventureGUI((Player) sender);
        } else {
            sender.sendMessage("Only players can open the Adventure GUI.");
        }

        return true;
    }

}