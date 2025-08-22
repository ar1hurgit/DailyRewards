package Reward;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
        // ==== DEBUG: Afficher les paramètres de reset ====
        plugin.getLogger().info("[DailyRewards] Reset Config - Enabled: " + plugin.getConfig().getBoolean("reset.enabled", true));
        plugin.getLogger().info("[DailyRewards] Reset Config - Delay (days): " + plugin.getConfig().getInt("reset.delay", 1));

        // ==== GESTION DES COMMANDES ADMIN ====
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("admin")) {
                if (args.length == 1) {
                    // ==== VÉRIFICATION : AU MOINS UNE PERMISSION ADMIN ====
                    if (!sender.hasPermission("dailyrewards.admin.*")
                            && !sender.hasPermission("dailyrewards.admin.set")
                            && !sender.hasPermission("dailyrewards.admin.day")) {
                        sender.sendMessage(Utils.color("&cYou don't have permission to use this command!"));
                        return true;
                    }
                    sendAdminHelp(sender);
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "set":
                        // ==== VÉRIFICATION : PERMISSION SPÉCIFIQUE OU GLOBALE ====
                        if (!sender.hasPermission("dailyrewards.admin.*")
                                && !sender.hasPermission("dailyrewards.admin.set")) {
                            sender.sendMessage(Utils.color("&cYou don't have permission to use this subcommand!"));
                            return true;
                        }
                        plugin.getLogger().info("[DailyRewards] Admin " + sender.getName() + " used /rewards admin set");
                        boolean setResult = handleSetCommand(sender, args);
                        plugin.getLogger().info("[DailyRewards] Set command result: " + setResult);
                        return setResult;

                    case "day":
                        // ==== VÉRIFICATION : PERMISSION SPÉCIFIQUE OU GLOBALE ====
                        if (!sender.hasPermission("dailyrewards.admin.*")
                                && !sender.hasPermission("dailyrewards.admin.day")) {
                            sender.sendMessage(Utils.color("&cYou don't have permission to use this subcommand!"));
                            return true;
                        }
                        return handleDayCommand(sender, args);

                    default:
                        sender.sendMessage(Utils.color("&cUnknown admin subcommand!"));
                        sendAdminHelp(sender);
                        return true;
                }
            }
        }

        // ==== VÉRIFICATION QUE L'EXPÉDITEUR EST UN JOUEUR ====
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.color("&cOnly players can use this command!"));
            return true;
        }

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

        // ==== VÉRIFICATION SI LE JOUEUR EST DANS LA BASE DE DONNÉES ====
        if (lastClaimStr == null || lastClaimStr.isEmpty()) {
            player.sendMessage(Utils.color("&cYour data is not initialized. Please reconnect to the server!"));
            return true; // Arrête l'exécution du reste du code
        }

        // ==== PARSING DE LA DERNIÈRE RÉCLAMATION ====
        LocalDateTime lastClaim;
        try {
            lastClaim = LocalDateTime.parse(lastClaimStr);
            plugin.getLogger().info("[DailyRewards] Parsed Last Claim: " + lastClaim);
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("[DailyRewards] Invalid date format for " + player.getName() + ": " + lastClaimStr);
            player.sendMessage(Utils.color("&cError loading your rewards data. Please reconnect!"));
            return true; // Arrête l'exécution du reste du code
        }

        // ==== CALCUL DE LA PROCHAINE RÉINITIALISATION ====
        LocalDateTime nextMidnight = lastClaim.toLocalDate().plusDays(delayDays).atStartOfDay();
        boolean shouldReset = resetEnabled && now.isAfter(nextMidnight);

        // ==== DEBUG: Détails de la réinitialisation ====
        plugin.getLogger().info("[DailyRewards] Reset Details:");
        plugin.getLogger().info("  - Player: " + player.getName());
        plugin.getLogger().info("  - Last Claim: " + lastClaim);
        plugin.getLogger().info("  - Next Midnight: " + nextMidnight);
        plugin.getLogger().info("  - Now: " + now);
        plugin.getLogger().info("  - Should Reset: " + shouldReset);

        if (shouldReset) {
            // ==== RÉINITIALISATION ====
            playerData.setDay(uuid, 0); // Réinitialise à Jour 1
            LocalDateTime newLastClaim = now.minusDays(1).toLocalDate().atStartOfDay();
            playerData.setLastClaim(uuid, newLastClaim.toString());
            player.sendMessage(Utils.color("&4Your progress has been reset to Day 1!"));
            page = 0;
            plugin.getLogger().info("[DailyRewards] Reset triggered for " + player.getName() + ". Next reset: " + newLastClaim);
        }

        // ==== OUVERTURE DE L'INTERFACE ====
        guiManager.openRewardsGUI(player, page);
        return true;
    }

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Utils.color("&cUsage: /rewards admin set <player> <day>"));
            return false;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(Utils.color("&cPlayer not found"));
            return false;
        }
        try {
            int day = Integer.parseInt(args[3]);
            if (day <= 0) {
                sender.sendMessage(Utils.color("&cDay must be greater than 0"));
                return false;
            }
            playerData.setDay(target.getUniqueId().toString(), day - 1);
            playerData.setLastClaim(target.getUniqueId().toString(), LocalDateTime.now().minusDays(1).toString());
            sender.sendMessage(Utils.color("&aSet " + target.getName() + " to day " + day));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid day number"));
            return false;
        }
    }

    private boolean handleDayCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.color("&cOnly players can use this command!"));
            return false;
        }

        String moneySystem = plugin.getConfig().getString("monney", "vault");
        boolean isNone = moneySystem != null && moneySystem.equalsIgnoreCase("none");

        if ((isNone && args.length < 3) || (!isNone && args.length < 4)) {
            String usage = isNone
                    ? "&cUsage: /rewards admin day <day>"
                    : "&cUsage: /rewards admin day <day> <amount> ";
            sender.sendMessage(Utils.color(usage));
            return false;
        }

        try {
            int day = Integer.parseInt(args[2]);
            List<String> commands = new ArrayList<>();

            if (!isNone) {
                double amount = Double.parseDouble(args[3]);
                String moneyCmd = buildMoneyCommand(moneySystem, amount);
                if (moneyCmd != null && !moneyCmd.isEmpty()) {
                    commands.add(moneyCmd);
                }
            }

            int extraCmdIndex = isNone ? 3 : 4;
            if (args.length > extraCmdIndex) {
                commands.add(args[extraCmdIndex]);
            } else {
                Player admin = (Player) sender;
                ItemStack itemInHand = admin.getInventory().getItemInMainHand();
                if (itemInHand != null && !itemInHand.getType().isAir()) {
                    String serialized = Utils.serializeItemStack(itemInHand);
                    if (serialized !=null) {
                        commands.add("item:" + serialized);
                    }
                }
            }

            plugin.getConfig().set("rewards.day-" + day, commands);
            plugin.saveConfig();
            sender.sendMessage(Utils.color("&aReward for day " + day + " set!"));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid number format!"));
            return false;
        }
    }

    private String buildMoneyCommand(String moneySystem, double amount) {
        switch (moneySystem.toLowerCase()) {
            case "vault":
            case "essentialsx":
                return "eco give {player} " + amount;
            case "playerpoints":
                return "points give {player} " + amount + " -s";
            case "coinsengine":
                return "coins give {player} " + amount;
            case "ultraeconomy":
                String currency = plugin.getConfig().getString("ultraeconomy.currency", "default");
                if (currency == null || currency.isEmpty()) currency = "default";
                return "addbalance {player} " + currency + " " + amount;
            case "votingplugin":
                return "av User {player} AddPoints " + amount;
            case "none":
                return "" ;
            default:
                return "eco give {player} " + amount;
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        String moneySystem = plugin.getConfig().getString("monney", "vault");
        boolean isNone = moneySystem != null&& moneySystem.equalsIgnoreCase("none");
        sender.sendMessage(Utils.color("&6Admin Commands:"));
        sender.sendMessage(Utils.color("&e/rewards admin set <player> <day> &7- Set player's day"));
        sender.sendMessage(Utils.color(isNone
                ? "&e/rewards admin day <day>&7- Set day reward"
                : "&e/rewards admin day <day> <amount> &7- Set day reward"));
    }
}