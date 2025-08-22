package Reward;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        // ==== DEBUG: Afficher les paramètres de reset ====
        plugin.getLogger().info("[DailyRewards] Reset Config - Enabled: " + plugin.getConfig().getBoolean("reset.enabled", true));
        plugin.getLogger().info("[DailyRewards] Reset Config - Delay (days): " + plugin.getConfig().getInt("reset.delay", 1));

        // ==== GESTION DES COMMANDES ADMIN ====
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
                    return handleSetCommand(sender, args);
                case "day":
                    return handleDayCommand(sender, args);
                default:
                    sender.sendMessage(Utils.color("&cUnknown subcommand!"));
                    return true;
            }
        }

        // ==== VÉRIFICATION QUE L'EXPÉDITEUR EST UN JOUEUR POUR LES COMMANDES NON-ADMIN ====
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.color("&cOnly players can use this command!"));
            return true;
        }

        // ==== GESTION DES COMMANDES JOUEUR ====
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "get":
                    return handleGetCommand(sender, args);
                case "baltop":
                case "bal":
                    return handleBaltopCommand(sender);
            }
        }

        // ==== CODE ORIGINAL POUR LES JOUEURS ====
        Player player = (Player) sender;
        String uuid = player.getUniqueId().toString();
        plugin.getLogger().info("[DailyRewards] Player UUID: " + uuid);

        // ==== GESTION DES JOURS DE RÉCOMPENSE ====
        int currentDay = playerData.getDay(uuid);
        int rewardsPerPage = 45;
        int page = (currentDay - 1) / rewardsPerPage;

        // ==== GESTION DE LA RÉINITIALISATION ====
        String lastClaimStr = playerData.getLastClaim(uuid);
        LocalDateTime now = LocalDateTime.now();
        int delayDays = plugin.getConfig().getInt("reset.delay", 1);
        boolean resetEnabled = plugin.getConfig().getBoolean("reset.enabled", true);

        if (lastClaimStr == null || lastClaimStr.isEmpty()) {
            player.sendMessage(Utils.color("&cYour data is not initialized. Please reconnect!"));
            return true;
        }

        // ==== PARSING DE LA DERNIÈRE RÉCLAMATION ====
        LocalDateTime lastClaim;
        try {
            lastClaim = LocalDateTime.parse(lastClaimStr);
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("[DailyRewards] Invalid date format for " + player.getName());
            player.sendMessage(Utils.color("&cError loading your data. Please reconnect!"));
            return true;
        }

        // ==== CALCUL DE LA PROCHAINE RÉINITIALISATION ====
        LocalDateTime nextReset = lastClaim.plusDays(delayDays);
        boolean shouldReset = resetEnabled && now.isAfter(nextReset);

        if (shouldReset) {
            playerData.setDay(uuid, 0);
            playerData.setLastClaim(uuid, now.minusDays(1).toString());
            player.sendMessage(Utils.color("&4Your progress has been reset to Day 1!"));
            page = 0;
        }

        // ==== OUVERTURE DE L'INTERFACE ====
        guiManager.openRewardsGUI(player, page);
        return true;
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
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
                return handleSetCommand(sender, args);
            case "day":
                return handleDayCommand(sender, args);
            default:
                sender.sendMessage(Utils.color("&cUnknown subcommand!"));
                return true;
        }
    }

    private boolean checkAdminPermissions(CommandSender sender) {
        return sender.hasPermission("dailyrewards.admin.*") ||
                sender.hasPermission("dailyrewards.admin.set") ||
                sender.hasPermission("dailyrewards.admin.day");
    }

    private void handleResetLogic(Player player, String uuid) {
        String lastClaimStr = playerData.getLastClaim(uuid);
        LocalDateTime now = LocalDateTime.now();
        int delayDays = plugin.getConfig().getInt("reset.delay", 1);
        boolean resetEnabled = plugin.getConfig().getBoolean("reset.enabled", true);

        if (lastClaimStr == null || lastClaimStr.isEmpty()) {
            player.sendMessage(Utils.color("&cData not initialized! Reconnect."));
            return;
        }

        try {
            LocalDateTime lastClaim = LocalDateTime.parse(lastClaimStr);
            LocalDateTime nextReset = lastClaim.plusDays(delayDays);

            if (resetEnabled && now.isAfter(nextReset)) {
                playerData.setDay(uuid, 0);
                playerData.setLastClaim(uuid, now.minusDays(1).toString());
                player.sendMessage(Utils.color("&4Progress reset to Day 1!"));
            }
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("Invalid date format for " + player.getName());
            player.sendMessage(Utils.color("&cData error! Reconnect."));
        }
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
            if (day <= 0) {
                sender.sendMessage(Utils.color("&cInvalid day!"));
                return false;
            }

            playerData.setDay(target.getUniqueId().toString(), day - 1);
            playerData.setLastClaim(target.getUniqueId().toString(), LocalDateTime.now().minusDays(1).toString());
            sender.sendMessage(Utils.color("&aSet " + target.getName() + " to day " + day));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid number!"));
            return false;
        }
    }

    private boolean handleDayCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.color("&cPlayers only!"));
            return false;
        }

        String moneySystem = plugin.getConfig().getString("monney", "vault");
        boolean isNone = moneySystem != null && moneySystem.equalsIgnoreCase("none");

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

            if (args.length > 3) {
                commands.add(args[3]);
            } else {
                ItemStack item = ((Player) sender).getInventory().getItemInMainHand();
                if (item != null && !item.getType().isAir()) {
                    String serialized = Utils.serializeItemStack(item);
                    if (serialized != null) commands.add("item:" + serialized);
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
    private boolean handleGetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dailyrewards.get")) {
            sender.sendMessage(Utils.color("&cYou don't have permission!"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Utils.color("&cUsage: /rewards get <player>"));
            return false;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Utils.color("&cPlayer not found!"));
            return false;
        }

        int day = playerData.getDay(target.getUniqueId().toString());
        sender.sendMessage(Utils.color("&a" + target.getName() +
                " is on " + Utils.getOrdinal(day) + " day!"));
        return true;
    }

    private boolean handleBaltopCommand(CommandSender sender) {
        if (!sender.hasPermission("dailyrewards.baltop")) {
            sender.sendMessage(Utils.color("&cYou don't have permission!"));
            return true;
        }

        Map<UUID, Integer> allPlayers = playerData.getAllPlayerDays();

        // Sort players by day descending
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(allPlayers.entrySet());
        sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        sender.sendMessage(Utils.color("&4&lTop 10 Players:"));
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            Map.Entry<UUID, Integer> entry = sorted.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            sender.sendMessage(Utils.color("&6#" + (i+1) + " &e" +
                    (name != null ? name : "Unknown") + " &7- " +
                    Utils.getOrdinal(entry.getValue())));
        }
        return true;
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

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(Utils.color("&6Admin Commands:"));
        sender.sendMessage(Utils.color("&e/rewards admin set <player> <day> &7- Set player day"));
        sender.sendMessage(Utils.color("&e/rewards admin day <day> [amount] &7- Configure rewards"));
    }
}