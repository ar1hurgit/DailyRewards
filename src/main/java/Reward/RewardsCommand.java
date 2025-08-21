package Reward;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RewardsCommand implements CommandExecutor {

    private final Rewards plugin;
    private final GUIManager guiManager;
    private final PlayerDataManager playerData;

    public RewardsCommand(Rewards plugin, GUIManager guiManager, RewardManager rewardManager, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.playerData = playerData;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Utils.color("&cOnly players can use this command!"));
                return true;
            }
            String uuid = player.getUniqueId().toString();
            playerData.setDay(uuid, 0);
            playerData.setLastClaim(uuid, LocalDateTime.now().minusDays(plugin.getConfig().getInt("reset.delay", 1)).toString());
            player.sendMessage(Utils.color("&aVotre progression a été réinitialisée à Jour 1."));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Utils.color("&cOnly players can use this command!"));
                return true;
            }
            guiManager.openRewardsGUI((Player) sender, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("dailyrewards.admin")) {
                sender.sendMessage(Utils.color("&cYou don't have permission!"));
                return true;
            }
            if (args.length < 2) {
                sendAdminHelp(sender);
                return true;
            }
            switch (args[1].toLowerCase()) {
                case "reload":
                    plugin.reloadConfig();
                    sender.sendMessage(Utils.color("&aConfiguration reloaded!"));
                    break;
                case "set":
                    return handleSetCommand(sender, args);
                case "day":
                    return handleDayCommand(sender, args);
                default:
                    sendAdminHelp(sender);
            }
            return true;
        }
        return false;
    }

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Utils.color("&cUsage: /rewards admin set <player> <day>"));
            return false;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(Utils.color("&cPlayer not found!"));
            return false;
        }
        try {
            int day = Integer.parseInt(args[3]);
            playerData.setDay(target.getUniqueId().toString(), day - 1);
            playerData.setLastClaim(target.getUniqueId().toString(), LocalDateTime.now().minusDays(1).toString());
            sender.sendMessage(Utils.color("&aSet " + target.getName() + " to day " + day));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid day number!"));
            return false;
        }
    }

    private boolean handleDayCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 4) {
            sender.sendMessage(Utils.color("&cUsage: /rewards admin day <day> <money> [command]"));
            return false;
        }
        try {
            int day = Integer.parseInt(args[2]);
            double money = Double.parseDouble(args[3]);
            List<String> commands = new ArrayList<>();
            commands.add("eco give {player} " + money);
            plugin.getConfig().set("rewards.day-" + day, commands);
            plugin.saveConfig();
            sender.sendMessage(Utils.color("&aReward for day " + day + " set!"));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid number format!"));
            return false;
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(Utils.color("&6Admin Commands:"));
        sender.sendMessage(Utils.color("&e/rewards admin reload &7- Reload config"));
        sender.sendMessage(Utils.color("&e/rewards admin set <player> <day> &7- Set player's day"));
        sender.sendMessage(Utils.color("&e/rewards admin day <day> <money> [command] &7- Set day reward"));
    }
}
