package me.remag501.adventure.manager;

import me.remag501.adventure.AdventurePlugin;
import me.remag501.adventure.model.WeatherModel;
import me.remag501.adventure.setting.SettingsProvider;
import me.remag501.adventure.weather.BlizzardWeather;
import me.remag501.adventure.weather.WeatherEffect;
import me.remag501.core.api.task.TaskService;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.List;
import java.util.Map;

public class WeatherManager {

    // Stores a map of all weather types to classes
    private static final Map<String, WeatherEffect> WEATHER_REGISTRY = Map.of(
            "blizzard", new BlizzardWeather()
    );

    private final TaskService taskService;
    private final SettingsProvider settingsProvider;

    private List<WeatherModel> weathers;

    public WeatherManager(TaskService taskService, SettingsProvider settingsProvider) {
        this.taskService = taskService;
        this.settingsProvider = settingsProvider;

        weathers = settingsProvider.getSettings().getWeatherModels();
        startScheduling();
    }

    public void reload() {
        this.weathers = settingsProvider.getSettings().getWeatherModels();
    }


    private void startScheduling() {
        for (WeatherModel model : weathers) {
            scheduleNext(model);
        }
    }

    private void scheduleNext(WeatherModel model) {
        int delayTicks = model.randomFrequencyMinutes() * 60 * 20; // Won't wait for previous weather to finish up

        taskService.delay(delayTicks, () -> {
            startWeather(model);
            scheduleNext(model); // chain forever
        });

    }

    private void startWeather(WeatherModel model) {
        World world = Bukkit.getWorld(model.getWorld());
        if (world == null) return;

        WeatherEffect effect = WEATHER_REGISTRY.get(model.getType().toLowerCase());
        if (effect == null) return;

        int durationTicks = model.randomDurationSeconds() * 20;

        effect.start(world);
        Bukkit.getLogger().info("[Adventure] Starting weather " + model.getType() + " for " + (durationTicks / 20) + " seconds in world " + world.getName() + ".");

        taskService.subscribe(AdventurePlugin.SYSTEM_ID, model.getType(), 0, 20, false, (ticks) -> {
            effect.tick(world);
            return false;
        });

        taskService.delay(durationTicks, () -> {
            taskService.stopTask(AdventurePlugin.SYSTEM_ID, model.getType());
        });

    }
}
