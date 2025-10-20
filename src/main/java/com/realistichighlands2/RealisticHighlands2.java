package com.realistichighlands2;

import com.realistichighlands2.commands.HighlandsCommand;
import com.realistichighlands2.commands.HighlandsTeleportCommand;
import com.realistichighlands2.commands.RegenerateBiomeCommand;
import com.realistichighlands2.generator.RealisticWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class RealisticHighlands2 extends JavaPlugin {

    private static RealisticHighlands2 instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("RealisticHighlands2 enabled!");

        // Register commands
        Objects.requireNonNull(getCommand("highlands")).setExecutor(new HighlandsCommand(this));
        Objects.requireNonNull(getCommand("highlands_teleport")).setExecutor(new HighlandsTeleportCommand(this));
        Objects.requireNonNull(getCommand("regenerate_biome")).setExecutor(new RegenerateBiomeCommand(this));

        // Create or load worlds with custom generator
        createRealisticHighlandsWorld("highlands_world");
    }

    @Override
    public void onDisable() {
        getLogger().info("RealisticHighlands2 disabled!");
    }

    public static RealisticHighlands2 getInstance() {
        return instance;
    }

    private void createRealisticHighlandsWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().info("Creating new RealisticHighlands world: " + worldName);
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new RealisticWorldGenerator());
            Bukkit.createWorld(creator);
        } else {
            getLogger().info("RealisticHighlands world '" + worldName + "' already exists.");
        }
    }
}
