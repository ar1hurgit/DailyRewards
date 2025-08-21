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

    public RewardsTabCompleter() {
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command  cmd, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("admin");
            return filter(args[0], completions);
        }
        if (args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) return filter(args[1], Arrays.asList("reload", "set", "day"));
            if (args.length == 3 && args[1].equalsIgnoreCase("set"))
                return filter(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            if (args.length == 3 && args[1].equalsIgnoreCase("day"))
                return filter(args[2], Arrays.asList("1", "7", "14", "30"));
            if (args.length == 4 && args[1].equalsIgnoreCase("set"))
                return filter(args[3], Arrays.asList("1", "7", "14", "30"));
        }
        return completions;
    }

    private List<String> filter(String input, List<String> options) {
        return StringUtil.copyPartialMatches(input, options, new ArrayList<>());
    }
}
