package com.realistichighlands2.commands;

import com.realistichighlands2.RealisticHighlands2;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class HighlandsTeleportCommand implements CommandExecutor {

    private final RealisticHighlands2 plugin;
    private final Random random = new Random();

    public HighlandsTeleportCommand(RealisticHighlands2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez gracza.");
            return true;
        }

        if (!sender.hasPermission("realistichighlands.teleport")) {
            sender.sendMessage("§cNie masz uprawnień do użycia tej komendy.");
            return true;
        }

        Player player = (Player) sender;
        World targetWorld = Bukkit.getWorld("highlands_world");

        if (args.length > 0) {
            World specifiedWorld = Bukkit.getWorld(args[0]);
            if (specifiedWorld != null) {
                targetWorld = specifiedWorld;
            } else {
                player.sendMessage("§cNie znaleziono świata o nazwie: " + args[0]);
                return true;
            }
        }

        if (targetWorld == null) {
            player.sendMessage("§cNie znaleziono świata 'highlands_world'. Upewnij się, że plugin go utworzył.");
            return true;
        }

        player.sendMessage("§aTeleportowanie do losowego miejsca w świecie '" + targetWorld.getName() + "'...");

        // Prosta logika znajdowania bezpiecznego miejsca
        int tries = 0;
        Location teleportLocation = null;
        while (teleportLocation == null && tries < 50) {
            int x = random.nextInt(10000) - 5000; // Zakres od -5000 do 4999
            int z = random.nextInt(10000) - 5000; // Zakres od -5000 do 4999

            int y = targetWorld.getHighestBlockYAt(x, z, org.bukkit.generator.ChunkGenerator.BiomeGrid.FarmChunkData.class); // Get highest Y for a block

            if (y > targetWorld.getMinHeight() + 5) { // Ensure it's not too low (e.g., in a deep cave)
                teleportLocation = new Location(targetWorld, x + 0.5, y + 2, z + 0.5);
                if (teleportLocation.getBlock().getType().isSolid() || teleportLocation.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                    teleportLocation = null;
                }
            }
            tries++;
        }

        if (teleportLocation != null) {
            player.teleport(teleportLocation);
            player.sendMessage("§aPomyślnie teleportowano!");
        } else {
            player.sendMessage("§cNie udało się znaleźć bezpiecznego miejsca do teleportacji po wielu próbach.");
        }

        return true;
    }
}
