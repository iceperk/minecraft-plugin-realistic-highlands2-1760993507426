package com.realistichighlands2.commands;

import com.realistichighlands2.RealisticHighlands2;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class RegenerateBiomeCommand implements CommandExecutor {

    private final RealisticHighlands2 plugin;

    public RegenerateBiomeCommand(RealisticHighlands2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("realistichighlands.admin")) {
            sender.sendMessage("§cNie masz uprawnień do użycia tej komendy.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez gracza.");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        if (!world.getName().equals("highlands_world")) { // Sprawdzamy czy to "nasz" świat
            player.sendMessage("§cNie możesz użyć tej komendy poza światem 'highlands_world'.");
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            player.sendMessage("§eUżycie: /regenerate_biome <biome_type> [radius]");
            return true;
        }

        Biome targetBiome;
        try {
            targetBiome = Biome.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cNieprawidłowy typ biomu: " + args[0] + ". Dostępne biomy: " + String.join(", ", Biome.values().stream().map(Enum::name).toList()));
            return true;
        }

        int radius = 0; // Regeneruje tylko aktualny chunk
        if (args.length == 2) {
            try {
                radius = Integer.parseInt(args[1]);
                if (radius < 0 || radius > 10) {
                    player.sendMessage("§cPromień musi być liczbą od 0 do 10.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cNieprawidłowy promień. Musi być liczbą.");
                return true;
            }
        }

        player.sendMessage("§aRozpoczynam regenerację biomu na aktualnej pozycji...");

        int startChunkX = player.getLocation().getChunk().getX();
        int startChunkZ = player.getLocation().getChunk().getZ();

        final Biome finalTargetBiome = targetBiome;
        final int finalRadius = radius;

        CompletableFuture.runAsync(() -> {
            for (int xOffset = -finalRadius; xOffset <= finalRadius; xOffset++) {
                for (int zOffset = -finalRadius; zOffset <= finalRadius; zOffset++) {
                    int chunkX = startChunkX + xOffset;
                    int chunkZ = startChunkZ + zOffset;

                    Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                    // Zmieniamy biom dla każdego bloku w chunku
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                                chunk.setBiome(x, y, z, finalTargetBiome);
                            }
                        }
                    }
                    chunk.unload(false); // Unload and re-load to apply changes
                    chunk.load(true);
                    Bukkit.getScheduler().runTask(plugin, () -> world.refreshChunk(chunkX, chunkZ)); // Refresh for clients
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§aPomyślnie zmieniono biom na '" + finalTargetBiome.name() + "' w promieniu " + finalRadius + " chunków."));
        });

        return true;
    }
}
