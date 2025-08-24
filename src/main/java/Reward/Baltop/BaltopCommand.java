// BaltopCommand.java
package Reward.Baltop;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import Reward.PlayerDataManager;
import Reward.Utils;

public class BaltopCommand {
    private final PlayerDataManager playerData;
    private final BaltopGUIManager guiManager;

    public BaltopCommand(PlayerDataManager playerData) {
        this.playerData = playerData;
        this.guiManager = new BaltopGUIManager(playerData);
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.color("&cOnly players can use this command!"));
            return true;
        }

        if (!sender.hasPermission("dailyrewards.baltop")) {
            sender.sendMessage(Utils.color("&cYou don't have permission!"));
            return true;
        }

        guiManager.openBaltopGUI((Player) sender, 0);
        return true;
    }
}