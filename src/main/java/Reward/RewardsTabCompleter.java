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

        // ==== VÉRIFICATION GLOBALE : LE JOUEUR A-T-IL UNE PERMISSION ADMIN ? ====
        boolean hasAdminPermission = sender.hasPermission("dailyrewards.admin.*")
                || sender.hasPermission("dailyrewards.admin.set")
                || sender.hasPermission("dailyrewards.admin.day");
        // ==== SI PAS DE PERMISSION, ON RENVOIE RIEN ====
        if (!hasAdminPermission) {
            return completions;
        }

        // ==== SUGGESTION POUR LE PREMIER ARGUMENT ====
        if (args.length == 1) {
            completions.add("admin");
            return filter(args[0], completions);
        }

        // ==== SUGGESTIONS POUR /rewards admin ====
        if (args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) {
                List<String> subCommands = new ArrayList<>();
                if (sender.hasPermission("dailyrewards.admin.*") || sender.hasPermission("dailyrewards.admin.set")) {
                    subCommands.add("set");
                }
                if (sender.hasPermission("dailyrewards.admin.*") || sender.hasPermission("dailyrewards.admin.day")) {
                    subCommands.add("day");
                }
                return filter(args[1], subCommands);
            }

            // ==== SUGGESTIONS POUR /rewards admin set [?] ====
            if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
                return filter(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }

            // ==== SUGGESTIONS POUR /rewards admin day [?] ====
            if (args.length == 3 && args[1].equalsIgnoreCase("day")) {
                return filter(args[2], Arrays.asList("1", "7", "30"));
            }

            // ==== SUGGESTIONS POUR /rewards admin set [joueur] [?] ====
            if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
                return filter(args[3], Arrays.asList("1", "7", "30"));
            }
        }

        return completions;
    }

    private List<String> filter(String input, List<String> options) {
        return StringUtil.copyPartialMatches(input, options, new ArrayList<>());
    }
}