package Reward.day;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import Reward.Rewards;
import Reward.Utils;

public class DayCommand {
    private final Rewards plugin;

    public DayCommand(Rewards plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {return false;}

        // check args
        if (args.length < 1) {
            sender.sendMessage(Utils.color("&cUsage: /rewards admin day <day>"));
            return false;
        }

        try {
            int day = Integer.parseInt(args[0]);
            
            // Use the existing instance to keep the state and the listener chat
            DayGUICommand dayGUI = plugin.getDayGUICommand();
            if (dayGUI == null) {
                plugin.getLogger().severe("[DailyRewards] DayGUICommand instance is null. Aborting.");
                sender.sendMessage(Utils.color("&cInternal error. Please try again later."));
                return false;
            }
            String[] guiArgs = new String[]{"admin", "day", String.valueOf(day)};
            plugin.getLogger().info("[DailyRewards] Opening day GUI for day=" + day + " for " + sender.getName());
            return dayGUI.onCommand(sender, plugin.getCommand("rewards"), "rewards", guiArgs);
            
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid number!"));
            return false;
        }
    }
}