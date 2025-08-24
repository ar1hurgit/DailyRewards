package Reward.Day;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import Reward.Rewards;
import Reward.Utils;

import java.util.ArrayList;
import java.util.List;

public class DayCommand {
    private final Rewards plugin;

    public DayCommand(Rewards plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.color("&cPlayers only!"));
            return false;
        }

        String moneySystem = plugin.getConfig().getString("monney", "vault");
        boolean isNone = moneySystem.equalsIgnoreCase("none");

        if ((isNone && args.length < 3) || (!isNone && args.length < 4)) {
            sender.sendMessage(Utils.color(isNone ?
                    "&cUsage: /rewards admin day <day>" :
                    "&cUsage: /rewards admin day <day> <amount>"));
            return false;
        }

        try {
            int day = Integer.parseInt(args[2]);
            List<String> commands = new ArrayList<>();

            if (!isNone) {
                double amount = Double.parseDouble(args[3]);
                String moneyCmd = buildMoneyCommand(moneySystem, amount);
                if (!moneyCmd.isEmpty()) commands.add(moneyCmd);
            }

            int extraCmdIndex = isNone ? 3 : 4;
            if (args.length > extraCmdIndex) {
                commands.add(args[extraCmdIndex]);
            } else {
                Player admin = (Player) sender;
                ItemStack itemInHand = admin.getInventory().getItemInMainHand();
                if (!itemInHand.getType().isAir()) {
                    String serialized = Utils.serializeItemStack(itemInHand);
                    if (serialized !=null) {
                        commands.add("item:" + serialized);
                    }
                }
            }

            plugin.getConfig().set("rewards.day-" + day, commands);
            plugin.saveConfig();
            sender.sendMessage(Utils.color("&aReward set for day " + day));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid number!"));
            return false;
        }
    }

    private String buildMoneyCommand(String system, double amount) {
        switch (system.toLowerCase()) {
            case "vault":
            case "essentialsx":
                return "eco give {player} " + amount;
            case "playerpoints":
                return "points give {player} " + amount + " -s";
            case "coinsengine":
                return "coins give {player} " + amount;
            case "ultraeconomy":
                String currency = plugin.getConfig().getString("ultraeconomy.currency", "default");
                return "addbalance {player} " + (currency.isEmpty() ? "default" : currency) + " " + amount;
            case "votingplugin":
                return "av User {player} AddPoints " + amount;
            default:
                return "";
        }
    }
}