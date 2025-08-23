package Reward;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import Reward.Command.BaltopCommand;
import Reward.Command.GetCommand;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import Reward.PlayerDataManager;
import Reward.Rewards;
import Reward.RewardManager;
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
                    return new GetCommand(playerData).execute(sender, args);
                case "baltop":
                case "bal":
                    return new BaltopCommand(playerData).execute(sender, args);
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

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        // Check if the command format is correct (e.g., "/rewards admin set <player> <day>")
        if (args.length < 3) { // Assuming "admin" and "set" are part of the command label
            sender.sendMessage(Utils.color("&cUsage: /rewards admin set <player> <day>"));
            return false;
        }

        // Extract the player name and day (adjust indices based on your command structure)
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]); // args[2] for player name
        String uuidStr = target.getUniqueId().toString();

        if (!playerData.hasPlayerData(uuidStr)) {
            sender.sendMessage(Utils.color("&cPlayer not found in data!"));
            return false;
        }

        try {
            int day = Integer.parseInt(args[3]); // args[3] for day
            if (day <= -1) {
                sender.sendMessage(Utils.color("&cInvalid day! Must be at least 0."));
                return false;
            }

            // Set the day (remove "-1" unless intentional)
            playerData.setDay(uuidStr, day);

            // Optionally, reset lastClaim to allow immediate claiming (or another logic)
            playerData.setLastClaim(uuidStr, LocalDateTime.now().toString());

            sender.sendMessage(Utils.color("&aSet " + target.getName() + " to day " + day));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid number! Please enter a valid day."));
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

    private boolean checkAdminPermissions(CommandSender sender) {
        return sender.hasPermission("dailyrewards.admin.*") ||
                sender.hasPermission("dailyrewards.admin.set") ||
                sender.hasPermission("dailyrewards.admin.day");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(Utils.color("&6Admin Commands:"));
        sender.sendMessage(Utils.color("&e/rewards admin set <player> <day> &7- Set player day"));
        sender.sendMessage(Utils.color("&e/rewards admin day <day> [amount] &7- Configure rewards"));
    }
    private String getTopPlayersDebug() {
        // Version simplifiée pour le debug
        return "Top1: JoueurTest: 10 | Top2: AutreJoueur: 8";
    }
    private void testPlaceholders(CommandSender sender, Player player) {
        sender.sendMessage("§6=== Debug Placeholders ===");
        sender.sendMessage("§eJoueur: §f" + player.getName());
        sender.sendMessage("§eUUID: §f" + player.getUniqueId());
        sender.sendMessage("");

        // Utiliser l'instance de playerDataManager (non statique)
        int currentDay = playerData.getPlayerday(player.getUniqueId());

        sender.sendMessage("§b%dailyrewards_player%: §f" + currentDay);
        sender.sendMessage("§b%dailyrewards_player_name%: §f" + player.getName());

        // Pour le top players, utilisez aussi l'instance
        String topPlayers = getTopPlayersDebug();
        sender.sendMessage("§b%dailyrewards_bal%: §f" + topPlayers);
        sender.sendMessage("§b%dailyrewards_current%: §fRécompense jour " + currentDay);

        sender.sendMessage("");
        sender.sendMessage("§aPlaceholders testés avec succès !");
    }
    private void handlePlaceholderDebug(CommandSender sender, String[] args) {
        Player targetPlayer;

        if (args.length >= 3) {
            targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                sender.sendMessage("§cJoueur non trouvé ou hors ligne.");
                return;
            }
        } else if (sender instanceof Player) {
            targetPlayer = (Player) sender;
        } else {
            sender.sendMessage("§cVous devez spécifier un joueur depuis la console.");
            return;
        }

        // Tester tous les placeholders
        testPlaceholders(sender, targetPlayer);
    }
}