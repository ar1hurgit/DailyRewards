package Reward;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RewardsTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // ==== GESTION DES COMMANDES ADMIN ====
        boolean hasAdminPermission = sender.hasPermission("dailyrewards.admin.*")
                || sender.hasPermission("dailyrewards.admin.set")
                || sender.hasPermission("dailyrewards.admin.day");

        if (args.length == 1) {
            // Suggest admin commands if has permission
            if (hasAdminPermission) {
                completions.add("admin");
            }

            // Suggest player commands if has permissions
            if (sender.hasPermission("dailyrewards.get")) {
                completions.add("get");
            }
            if (sender.hasPermission("dailyrewards.baltop")) {
                completions.add("baltop");
                completions.add("bal"); // Short version
            }
            return filter(args[0], completions);
        }

        // ==== GESTION DES SOUS-COMMANDES ADMIN ====
        if (hasAdminPermission && args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) {
                List<String> subCommands = new ArrayList<>();
                if (sender.hasPermission("dailyrewards.admin.set")) subCommands.add("set");
                if (sender.hasPermission("dailyrewards.admin.day")) subCommands.add("day");
                return filter(args[1], subCommands);
            }

            // /rewards admin set [player]
            if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
                return filter(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }

            // /rewards admin day [day]
            if (args.length == 3 && args[1].equalsIgnoreCase("day")) {
                return filter(args[2], Arrays.asList("1", "7", "14", "30"));
            }

            // /rewards admin set [player] [day]
            if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
                return filter(args[3], Arrays.asList("1", "7", "14", "30"));
            }
        }

        // ==== GESTION DES COMMANDES JOUEUR ====
        if (args[0].equalsIgnoreCase("get") && sender.hasPermission("dailyrewards.get")) {
            if (args.length == 2) {
                // Suggest online players for /rewards get
                return filter(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }
        }

        // No more completions for baltop (no arguments needed)
        return completions;
    }

    private List<String> filter(String input, List<String> options) {
        return StringUtil.copyPartialMatches(input, options, new ArrayList<>());
    }
}