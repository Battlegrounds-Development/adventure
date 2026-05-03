package me.remag501.adventure;

import me.remag501.adventure.command.AdventureCommand;
import me.remag501.adventure.command.AdventureTabCompleter;
import me.remag501.adventure.command.ChanceCommand;
import me.remag501.adventure.command.ChanceTabCompleter;
import me.remag501.adventure.listener.BroadcastListener;
import me.remag501.adventure.listener.ExtractionListener;
import me.remag501.adventure.listener.GuiListener;
import me.remag501.adventure.listener.ItemLimiterListener;
import me.remag501.adventure.listener.JoinListener;
import me.remag501.adventure.manager.*;
import me.remag501.adventure.placeholder.BGSExpansion;
import me.remag501.adventure.setting.AdventureSettings;
import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.adventure.task.BroadcastTask;
import me.remag501.core.api.BGSApi;
import me.remag501.core.api.command.CommandService;
import me.remag501.core.api.event.EventService;
import me.remag501.core.api.namespace.NamespaceService;
import me.remag501.core.api.oraxen.OraxenService;
import me.remag501.core.api.task.TaskService;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Bukkit;

import java.util.UUID;

public class AdventurePlugin extends JavaPlugin {

    public static final UUID SYSTEM_ID = UUID.nameUUIDFromBytes("adventure-system".getBytes());

    private RotationManager rotationManager;
    private GuiManager guiManager;
    private ExtractionManager extractionManager;
    private PenaltyManager penaltyManager;
    private WeatherManager weatherManager;
    private WorldBorderManager worldBorderManager;
    private BroadcastTask broadcastTask;
    private PDCManager pdcManager;
    private ItemLimiterManager itemLimiterManager;

    private AdventureSettings settings;
    private SettingsProvider provider;


    @Override
    public void onEnable() {
        // Config stuff
        saveDefaultConfig();
        this.provider = new SettingsProvider(this);
        this.settings = provider.getSettings();

        // Get services from BGS Api
        EventService eventService = BGSApi.events();
        TaskService taskService = BGSApi.tasks();
        NamespaceService namespaceService = BGSApi.namespaces();
        CommandService commandService = BGSApi.commands();
        OraxenService oraxenService = BGSApi.oraxen();

        // Initialize managers
        // Independent Managers (Leaves)
        this.extractionManager = new ExtractionManager(provider);
        this.pdcManager = new PDCManager(namespaceService);
        this.rotationManager = new RotationManager(taskService, provider);
        this.itemLimiterManager = new ItemLimiterManager(provider, oraxenService);

        // Managers that need Rotation (Workers)
        this.broadcastTask = new BroadcastTask(taskService, rotationManager, settings);
        this.weatherManager = new WeatherManager(taskService, provider);
        this.worldBorderManager = new WorldBorderManager(taskService, rotationManager, extractionManager, provider);

        // Complex Managers (Controllers)
        this.penaltyManager = new PenaltyManager(taskService, pdcManager, broadcastTask, provider);
        this.guiManager = new GuiManager(namespaceService, rotationManager, provider);

        // Register commands
        AdventureCommand adventureCommand = new AdventureCommand(this, pdcManager, rotationManager, guiManager, itemLimiterManager);
        getCommand("adventure").setExecutor(adventureCommand);
        getCommand("adventure").setTabCompleter(new AdventureTabCompleter(rotationManager));
        getCommand("chance").setExecutor(new ChanceCommand());
        getCommand("chance").setTabCompleter(new ChanceTabCompleter());
        commandService.registerSubcommand("adventure", adventureCommand);

        // Register Listener
        new ExtractionListener(eventService, taskService, extractionManager, rotationManager, provider);
        new JoinListener(eventService, pdcManager, rotationManager, penaltyManager);
        new GuiListener(eventService, namespaceService, pdcManager, rotationManager, provider, itemLimiterManager);
        new BroadcastListener(eventService, rotationManager);
        new ItemLimiterListener(eventService, taskService, itemLimiterManager);

        // Start broadcasting messages
        penaltyManager.startBroadcastTask();

        // Register placeholder
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BGSExpansion(rotationManager).register();
            getLogger().info("AdventureBGS Placeholders registered!");
        }
    }

    @Override
    public void onDisable() {
        // Cleanup if needed
    }

    public void reloadPluginConfig() {
        this.reloadConfig();
        provider.updateConfig(this);
        this.settings = provider.getSettings();

        // Change to update managers instead
        rotationManager.reloadSettings();
        weatherManager.reload();
        worldBorderManager.reload();

    }

}
