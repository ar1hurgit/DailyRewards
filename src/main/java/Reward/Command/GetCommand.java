package Reward.Command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import Reward.PlayerDataManager;
import Reward.Utils;

public class GetCommand {
    private final PlayerDataManager playerData;

    public GetCommand(PlayerDataManager playerData) {
        this.playerData = playerData;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dailyrewards.get")) {
            sender.sendMessage(Utils.color("&cYou don't have permission!"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Utils.color("&cUsage: /rewards get <player>"));
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String uuidStr = target.getUniqueId().toString();

        if (!playerData.hasPlayerData(uuidStr)) {
            sender.sendMessage(Utils.color("&cPlayer not found in data!"));
            return false;
        }

        // Affiche les informations
        int day = playerData.getDay(uuidStr);
        sender.sendMessage(Utils.color("&a" + target.getName() +
                " is on " + Utils.getOrdinal(day) + " day!"));
        return true;
    }
}