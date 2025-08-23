package Reward;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class RewardsTabCompleter implements TabCompleter {

    private final JavaPlugin plugin;

    public RewardsTabCompleter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private List<String> allKnownPlayerNames() {
        Set<String> out = new LinkedHashSet<>();

        // Online names
        out.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));

        // From player_data.yml (UUID -> name via Utils + usercache.json)
        File dataFile = new File(plugin.getDataFolder(), "player_data.yml");
        if (dataFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
            for (String key : cfg.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = Utils.getPlayerName(uuid, plugin);
                    if (name != null && !name.isBlank()) out.add(name); // pas d’UUID si nom inconnu
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return new ArrayList<>(out);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        boolean hasAdminPermission = sender.hasPermission("dailyrewards.admin.*")
                || sender.hasPermission("dailyrewards.admin.set")
                || sender.hasPermission("dailyrewards.admin.day");

        if (args.length == 1) {
            if (hasAdminPermission) completions.add("admin");
            if (sender.hasPermission("dailyrewards.get")) completions.add("get");
            if (sender.hasPermission("dailyrewards.baltop")) {
                completions.add("baltop");
                completions.add("bal");
            }
            return filter(args[0], completions);
        }

        if (hasAdminPermission && args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) {
                List<String> subs = new ArrayList<>();
                if (sender.hasPermission("dailyrewards.admin.set")) subs.add("set");
                if (sender.hasPermission("dailyrewards.admin.day")) subs.add("day");
                return filter(args[1], subs);
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
                return filter(args[2], allKnownPlayerNames());
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("day")) {
                return filter(args[2], Arrays.asList("1", "7", "14", "30"));
            }

            if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
                return filter(args[3], Arrays.asList("1", "7", "14", "30"));
            }
        }

        if (args[0].equalsIgnoreCase("get") && sender.hasPermission("dailyrewards.get")) {
            if (args.length == 2) {
                return filter(args[1], allKnownPlayerNames());
            }
        }

        return completions;
    }

    private List<String> filter(String input, List<String> options) {
        return StringUtil.copyPartialMatches(input, options, new ArrayList<>());
    }
}
