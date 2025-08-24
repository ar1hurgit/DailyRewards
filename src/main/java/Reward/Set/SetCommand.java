package Reward.Set;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import Reward.PlayerDataManager;
import Reward.Rewards;
import Reward.Utils;

import java.time.LocalDateTime;

public class SetCommand {
    private final Rewards plugin;
    private final PlayerDataManager playerData;

    public SetCommand(Rewards plugin, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    public boolean execute(CommandSender sender, String[] args) {

        if (args.length != 4) {
            sender.sendMessage(Utils.color("&cUsage: /rewards admin set <player> <day>"));
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        String uuidStr = target.getUniqueId().toString();


        if (!playerData.hasPlayerData(uuidStr)) {
            sender.sendMessage(Utils.color("&cPlayer not found in data!"));
            return false;
        }

        try {
            int day = Integer.parseInt(args[3]);
            if (day <= 0) {
                sender.sendMessage(Utils.color("&cInvalid day! Must be at least 1."));
                return false;
            }

            int adjustedDay = day - 1;
            playerData.setDay(uuidStr, adjustedDay);

            // Réinitialisation de lastClaim pour permettre une réclamation immédiate
            playerData.setLastClaim(uuidStr, LocalDateTime.now().minusDays(1).toString());

            sender.sendMessage(Utils.color("&aSet " + target.getName() + "'s reward day to " + day));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid day format! Please enter a valid number."));
            return false;
        }
    }
}