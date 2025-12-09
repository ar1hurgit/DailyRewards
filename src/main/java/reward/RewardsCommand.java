package reward;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import reward.baltop.BaltopCommand;
import reward.get.GetCommand;
import reward.day.DayCommand;
import reward.set.SetCommand;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
        // DEBUG: show paramètres
        plugin.getLogger().info("[DailyRewards] Reset Config - Enabled: " + plugin.getConfig().getBoolean("reset.enabled", true));
        plugin.getLogger().info("[DailyRewards] Reset Config - Delay (days): " + plugin.getConfig().getInt("reset.delay", 1));

        // admin commands management
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (args.length == 1) {
                if (!checkAdminPermissions(sender)) {
                    sender.sendMessage(Utils.color("&cYou don't have permission!"));
                    return true;
                }
                sendAdminHelp(sender);
                return true;
            }

            switch (args[1].toLowerCase()) {
                case "set":
                    return new SetCommand(plugin, playerData).execute(sender, args);
                case "day":
                    if (args.length < 3) {
                        sender.sendMessage(Utils.color("&cUsage: /rewards admin day <day>"));
                        return true;
                    }
                    return new DayCommand(plugin).execute(sender, new String[]{args[2]});
                default:
                    sender.sendMessage(Utils.color("&cUnknown subcommand!"));
                    return true;
            }
        }

        if (!(sender instanceof Player)) {return true;}

        //command player
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "get":
                    return new GetCommand(playerData).execute(sender, args);
                case "baltop":
                case "bal":
                    return new BaltopCommand(playerData).execute(sender, args);
            }
        }

        Player player = (Player) sender;
        String uuid = player.getUniqueId().toString();
        plugin.getLogger().info("[DailyRewards] Player UUID: " + uuid);

        // dailyrewards management
        int currentDay = playerData.getDay(uuid);
        int rewardsPerPage = 45;
        int page = (currentDay - 1) / rewardsPerPage;

        // reset management
        String lastClaimStr = playerData.getLastClaim(uuid);
        LocalDateTime now = LocalDateTime.now();
        int delayDays = plugin.getConfig().getInt("reset.delay", 1);
        boolean resetEnabled = plugin.getConfig().getBoolean("reset.enabled", true);

        if (lastClaimStr == null || lastClaimStr.isEmpty()) {
            player.sendMessage(Utils.color("&cYour data is not initialized. Please reconnect!"));
            return true;
        }

        //analysis of the last claim date
        LocalDateTime lastClaim;
        try {
            lastClaim = LocalDateTime.parse(lastClaimStr);
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("[DailyRewards] Invalid date format for " + player.getName());
            player.sendMessage(Utils.color("&cError loading your data. Please reconnect!"));
            return true;
        }

        // next day
        LocalDateTime nextReset = lastClaim.plusDays(delayDays);
        boolean shouldReset = resetEnabled && now.isAfter(nextReset);

        if (shouldReset) {
            playerData.setDay(uuid, 0);
            playerData.setLastClaim(uuid, now.minusDays(1).toString());
            player.sendMessage(Utils.color("&4Your progress has been reset to day 1!"));
            page = 0;
        }

        // gui opening
        guiManager.openRewardsGUI(player, page);
        return true;
    }

    private boolean checkAdminPermissions(CommandSender sender) {
        return sender.hasPermission("dailyrewards.admin.*") ||
                sender.hasPermission("dailyrewards.admin.set") ||
                sender.hasPermission("dailyrewards.admin.day");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(Utils.color("&6Admin Commands:"));
        sender.sendMessage(Utils.color("&e/rewards admin set <player> <day> &7- set player day"));
        sender.sendMessage(Utils.color("&e/rewards admin day <day> &7- Configure rewards for a specific day"));
    }
}