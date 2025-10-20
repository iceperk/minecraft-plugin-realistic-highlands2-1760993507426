package com.realistichighlands2.commands;

import com.realistichighlands2.RealisticHighlands2;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HighlandsCommand implements CommandExecutor {

    private final RealisticHighlands2 plugin;

    public HighlandsCommand(RealisticHighlands2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("realistichighlands.admin")) {
            sender.sendMessage("§cNie masz uprawnień do użycia tej komendy.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUżycie: /highlands <reload|info>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§aKonfiguracja RealisticHighlands2 została przeładowana. (Brak konfiga w tej wersji, ale dobra praktyka!)");
                break;
            case "info":
                sender.sendMessage("§b--- RealisticHighlands2 Info ---");
                sender.sendMessage("§bWersja: " + plugin.getDescription().getVersion());
                sender.sendMessage("§bAutor: " + plugin.getDescription().getAuthors().get(0));
                sender.sendMessage("§bOpis: " + plugin.getDescription().getDescription());
                sender.sendMessage("§bGenerowane światy: highlands_world"); // Można by dynamicznie pobierać
                sender.sendMessage("§b--------------------------");
                break;
            default:
                sender.sendMessage("§eNieznana podkomenda. Użycie: /highlands <reload|info>");
                break;
        }

        return true;
    }
}
