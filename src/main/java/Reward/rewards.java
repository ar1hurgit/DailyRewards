package Reward;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class rewards extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private FileConfiguration config;
    private FileConfiguration playerData;
    private File dataFile;
    private final int[] rewardSlots = new int[45];

    @Override
    public void onEnable() {
        // Initialise les slots de 0 à 44
        for (int i = 0; i < 45; i++) rewardSlots[i] = i;

        // Chargement de la configuration
        saveDefaultConfig();
        config = getConfig();

        // Configuration des données joueur
        dataFile = new File(getDataFolder(), "player_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerData = YamlConfiguration.loadConfiguration(dataFile);

        // Enregistrement des events et commandes
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("rewards").setExecutor(this);
        getCommand("rewards").setTabCompleter(this);

        // Vérification quotidienne de réinitialisation (toutes les heures)
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::checkDailyReset, 0L, 20L * 60 * 60);
    }

    private void checkDailyReset() {
        // Vérifie si c'est minuit (heure 0)
        if (LocalDateTime.now().getHour() == 0) {
            boolean resetEnabled = config.getBoolean("reset.enabled", true);
            int resetDelay = config.getInt("reset.delay", 1);

            for (String uuid : playerData.getKeys(false)) {
                String lastClaimStr = playerData.getString(uuid + ".lastClaim");
                if (lastClaimStr != null) {
                    try {
                        LocalDateTime lastClaim = LocalDateTime.parse(lastClaimStr);
                        LocalDate lastClaimDate = lastClaim.toLocalDate();
                        LocalDate currentDate = LocalDate.now();

                        // Calcul du nombre de jours écoulés depuis la dernière réclamation
                        long daysBetween = ChronoUnit.DAYS.between(lastClaimDate, currentDate);

                        if (resetEnabled && daysBetween > resetDelay) {
                            // Réinitialisation du joueur
                            playerData.set(uuid + ".day", 0);
                            // Mise à jour de lastClaim à minuit actuel pour éviter les réinitialisations immédiates
                            LocalDateTime resetTime = LocalDateTime.of(currentDate, LocalTime.MIDNIGHT);
                            playerData.set(uuid + ".lastClaim", resetTime.toString());
                        }
                    } catch (Exception e) {
                        getLogger().warning("Format de date invalide pour " + uuid);
                    }
                }
            }
            savePlayerData();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Commande pour le reset manuel (optionnel)
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(color("&cOnly players can use this command!"));
                return true;
            }
            // Si l'auto-reset est activé, la commande n'est pas nécessaire
            if (config.getBoolean("reset.enabled", true)) {
                sender.sendMessage(color("&cL'auto-reset est activé, vous n'avez pas besoin d'utiliser cette commande."));
                return true;
            }
            Player player = (Player) sender;
            String uuid = player.getUniqueId().toString();
            playerData.set(uuid + ".day", 0);
            // On met à jour la date de la dernière réclamation pour simuler un reset effectif
            playerData.set(uuid + ".lastClaim", LocalDateTime.now().minusDays(config.getInt("reset.delay", 1)).toString());
            savePlayerData();
            player.sendMessage(color("&aVotre progression a été réinitialisée à Jour 1."));
            return true;
        }

        // Commande principale sans arguments : ouvre le GUI
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(color("&cOnly players can use this command!"));
                return true;
            }
            openRewardsGUI((Player) sender, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("dailyrewards.admin")) {
                sender.sendMessage(color("&cYou don't have permission!"));
                return true;
            }
            if (args.length < 2) {
                sendAdminHelp(sender);
                return true;
            }
            switch (args[1].toLowerCase()) {
                case "reload":
                    reloadConfig();
                    playerData = YamlConfiguration.loadConfiguration(dataFile);
                    sender.sendMessage(color("&aConfiguration reloaded!"));
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
            sender.sendMessage(color("&cUsage: /rewards admin set <player> <day>"));
            return false;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(color("&cPlayer not found!"));
            return false;
        }
        try {
            int day = Integer.parseInt(args[3]);
            String uuid = target.getUniqueId().toString();
            playerData.set(uuid + ".day", day - 1);
            playerData.set(uuid + ".lastClaim", LocalDateTime.now().minusDays(1).toString());
            savePlayerData();
            sender.sendMessage(color("&aSet " + target.getName() + " to day " + day));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(color("&cInvalid day number!"));
            return false;
        }
    }

    private boolean handleDayCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 4) {
            sender.sendMessage(color("&cUsage: /rewards admin day <day> <money> [command]"));
            return false;
        }
        Player admin = (Player) sender;
        try {
            int day = Integer.parseInt(args[2]);
            double money = Double.parseDouble(args[3]);
            ItemStack itemInHand = admin.getInventory().getItemInMainHand();
            List<String> commands = new ArrayList<>();
            commands.add("eco give {player} " + money);
            if (args.length > 4) {
                commands.add(args[4]);
            } else if (!itemInHand.getType().isAir()) {
                String serialized = serializeItemStack(itemInHand);
                if (serialized != null) {
                    commands.add("item:" + serialized);
                }
            }
            config.set("rewards.day-" + day, commands);
            saveConfig();
            sender.sendMessage(color("&aReward for day " + day + " set!"));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(color("&cInvalid number format!"));
            return false;
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(color("&6Admin Commands:"));
        sender.sendMessage(color("&e/rewards admin reload &7- Reload config"));
        sender.sendMessage(color("&e/rewards admin set <player> <day> &7- Set player's day"));
        sender.sendMessage(color("&e/rewards admin day <day> <money> [command] &7- Set day reward"));
    }

    // InventoryHolder personnalisé pour identifier le GUI
    public class RewardsHolder implements InventoryHolder {
        private final int page;
        public RewardsHolder(int page) {
            this.page = page;
        }
        public int getPage() {
            return page;
        }
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private void openRewardsGUI(Player player, int page) {
        int totalDays = config.getInt("total-days");
        int itemsPerPage = rewardSlots.length;
        int totalPages = (int) Math.ceil((double) totalDays / itemsPerPage);
        Inventory gui = Bukkit.createInventory(new RewardsHolder(page), 54,
                color(config.getString("gui.title"))
                        .replace("{page}", String.valueOf(page + 1))
                        .replace("{max}", String.valueOf(totalPages)));

        // Fond de l'interface
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, bg);
        }

        // Ajout des récompenses
        String uuid = player.getUniqueId().toString();
        int currentDay = playerData.getInt(uuid + ".day", 0);
        String lastClaimStr = playerData.getString(uuid + ".lastClaim");

        for (int i = 0; i < itemsPerPage; i++) {
            int day = page * itemsPerPage + i + 1;
            if (day > totalDays) break;
            gui.setItem(rewardSlots[i], createRewardItem(day, currentDay, lastClaimStr));
        }

        // Boutons de navigation
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(color(config.getString("gui.navigation.previous")));
            prev.setItemMeta(prevMeta);
            gui.setItem(45, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(color(config.getString("gui.navigation.next")));
            next.setItemMeta(nextMeta);
            gui.setItem(53, next);
        }
        player.openInventory(gui);
    }

    private ItemStack createRewardItem(int day, int currentDay, String lastClaimStr) {
        Material mat;
        String name;
        List<String> lore = new ArrayList<>();
        if (day <= currentDay) {
            mat = Material.CHEST;
            name = color("&aDay " + day);
            lore.add(color("&7Already claimed!"));
        } else if (day == currentDay + 1) {
            if (lastClaimStr == null || isReadyToClaim(lastClaimStr)) {
                mat = Material.GOLD_BLOCK;
                name = color("&eDay " + day);
                lore.add(color("&7Click to claim!"));
            } else {
                mat = Material.REDSTONE_BLOCK;
                name = color("&cDay " + day);
                lore.add(color("&7Come back in " + getRemainingHours(lastClaimStr) + "h!"));
            }
        } else {
            mat = Material.BARRIER;
            name = color("&cDay " + day);
            lore.add(color("&7Claim previous days first!"));
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isReadyToClaim(String lastClaimStr) {
        try {
            LocalDateTime lastClaim = LocalDateTime.parse(lastClaimStr);
            LocalDate lastClaimDate = lastClaim.toLocalDate();
            LocalDate currentDate = LocalDate.now();

            // Vérifie si la date actuelle est après la date de dernière réclamation
            return currentDate.isAfter(lastClaimDate);
        } catch (Exception e) {
            return true;
        }
    }

    private long getRemainingHours(String lastClaimStr) {
        try {
            LocalDateTime lastClaim = LocalDateTime.parse(lastClaimStr);
            // Calcule le prochain minuit après la dernière réclamation
            LocalDateTime nextMidnight = lastClaim.toLocalDate().plusDays(1).atStartOfDay();
            long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), nextMidnight);

            return Math.max(hours, 0); // Retourne 0 si le temps est écoulé
        } catch (Exception e) {
            return 0;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RewardsHolder)) return;
        //getLogger().info("Click dans le GUI détecté : slot " + event.getSlot());
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        // Navigation
        if (slot == 45) {
            RewardsHolder holder = (RewardsHolder) event.getInventory().getHolder();
            int currentPage = holder.getPage();
            if (currentPage > 0) {
                openRewardsGUI(player, currentPage - 1);
            }
        } else if (slot == 53) {
            RewardsHolder holder = (RewardsHolder) event.getInventory().getHolder();
            int currentPage = holder.getPage();
            int totalDays = config.getInt("total-days");
            int itemsPerPage = rewardSlots.length;
            int totalPages = (int) Math.ceil((double) totalDays / itemsPerPage);
            if (currentPage < totalPages - 1) {
                openRewardsGUI(player, currentPage + 1);
            }
        }
        // Réclamation de récompense
        else if (slot >= 0 && slot < 45) {
            handleRewardClaim(player, event.getCurrentItem());
        }
    }

    private void handleRewardClaim(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BARRIER) return;
        try {
            int day = Integer.parseInt(ChatColor.stripColor(item.getItemMeta().getDisplayName()).split(" ")[1]);
            String uuid = player.getUniqueId().toString();
            int currentDay = playerData.getInt(uuid + ".day", 0);
            String lastClaimStr = playerData.getString(uuid + ".lastClaim");
            if (day != currentDay + 1) {
                player.sendMessage(color(config.getString("messages.invalid-order")));
                return;
            }
            if (lastClaimStr != null && !isReadyToClaim(lastClaimStr)) {
                player.sendMessage(color(config.getString("messages.wait-message")
                        .replace("{time}", String.valueOf(getRemainingHours(lastClaimStr)))));
                return;
            }
            // Exécution des commandes associées à la récompense
            for (String command : config.getStringList("rewards.day-" + day)) {
                if (command.startsWith("item:")) {
                    ItemStack rewardItem = deserializeItemStack(command.substring(5));
                    if (rewardItem != null) {
                        player.getInventory().addItem(rewardItem);
                    }
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
                }
            }
            // Mise à jour des données
            playerData.set(uuid + ".day", day);
            playerData.set(uuid + ".lastClaim", LocalDateTime.now().toString());
            savePlayerData();
            player.closeInventory();
            player.sendMessage(color(config.getString("messages.claim-success")
                    .replace("{day}", String.valueOf(day))));
        } catch (Exception e) {
            player.sendMessage(color("&cError claiming reward!"));
            getLogger().warning("Error handling reward claim: " + e.getMessage());
        }
    }

    private void savePlayerData() {
        try {
            playerData.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save player data!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (hasAdminAccess(sender)) {
                completions.add("admin");
            }
            return filterCompletions(args[0], completions);
        }
        if (args[0].equalsIgnoreCase("admin") && hasAdminAccess(sender)) {
            return handleAdminTabComplete(sender, args);
        }
        return completions;
    }

    private List<String> handleAdminTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(Arrays.asList("reload", "set", "day"));
            return filterCompletions(args[1], completions);
        }
        if (args.length == 3) {
            switch (args[1].toLowerCase()) {
                case "set":
                    completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                    break;
                case "day":
                    completions.addAll(Arrays.asList("1", "7", "14", "30"));
                    break;
            }
            return filterCompletions(args[2], completions);
        }
        if (args.length == 4) {
            switch (args[1].toLowerCase()) {
                case "set":
                    completions.addAll(Arrays.asList("1", "7", "14", "30"));
                    break;
                case "day":
                    completions.addAll(Arrays.asList("1000", "5000", "10000", "50000"));
                    break;
            }
            return filterCompletions(args[3], completions);
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("day")) {
            Player player = (Player) sender;
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!item.getType().isAir()) {
                completions.add("give {player} " + item.getType() + " " + item.getAmount());
            }
            return filterCompletions(args[4], completions);
        }
        return completions;
    }

    private boolean hasAdminAccess(CommandSender sender) {
        return sender.hasPermission("dailyrewards.admin") || sender.isOp();
    }

    private List<String> filterCompletions(String input, List<String> possibilities) {
        return StringUtil.copyPartialMatches(input, possibilities, new ArrayList<>());
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ItemStack deserializeItemStack(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
