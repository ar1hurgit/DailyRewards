package Reward.day;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import Reward.PlayerDataManager;
import Reward.Utils;
import Reward.listener.ChatInputListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DayGUICommand implements CommandExecutor, Listener {
    private final Plugin plugin;
    private final PlayerDataManager playerData;
    private final int maxDays = 100;
    private Inventory currentGUI;
    private Player currentPlayer;
    private int currentDay;
    private final Set<Integer> itemSlots = new HashSet<>();
    private ChatInputListener chatInputListener;
    private File moneyFile;
    private FileConfiguration moneyConfig;

    public DayGUICommand(Plugin plugin, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        initMoneyStorage();
    }
    private void reopenMainGUI() {
        if (currentPlayer !=null && currentPlayer.isOnline() && currentGUI !=null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                currentPlayer.openInventory(currentGUI);
            }, 1L);
        }
    }
    public void setChatInputListener(ChatInputListener listener) {

        this.chatInputListener = listener;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        plugin.getLogger().info("[DailyRewards] DayGUICommand.onCommand args=" + String.join(", ", args));
        if (!(sender instanceof Player)) {return true;}

        try {
            if (args.length == 0) {
                sender.sendMessage(Utils.color("&cUsage: /rewards admin day <day>"));
                return true;
            }

            String dayArg = args[args.length - 1];
            plugin.getLogger().info("[DailyRewards] day arg parsed=" + dayArg);
            currentDay = Integer.parseInt(dayArg);
            if (currentDay < 1 || currentDay > maxDays) {
                sender.sendMessage(Utils.color("&cInvalid day! Must be between 1 and " + maxDays));
                return true;
            }

            currentPlayer = (Player) sender;
            openDayGUI(currentDay);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.color("&cInvalid number! Please enter a valid day."));
            return true;
        }
    }

    private void openDayGUI(int day) {
        plugin.getLogger().info("[DailyRewards] Opening day GUI day=" + day + ", player=" + (currentPlayer != null ? currentPlayer.getName() : "null"));
        currentGUI = Bukkit.createInventory(null, 54, Utils.color("&6Day " + day + "/" + maxDays));

        //item area slots
        prepareItemAreaSlots();

        //border
        addBorderPanes();

        //navigation arrows
        addNavigationArrows();

        // Conditionally add money block if enabled
        if (isMoneyEnabled()) {
            addMoneyBlock();
        }

        //Save button
        addSaveButton();

        // Load existing items for this day from config
        loadDayItemsFromConfig(day);

        currentPlayer.openInventory(currentGUI);
    }

    private void initMoneyStorage() {
        moneyFile = new File(plugin.getDataFolder(), "money.yml");
        if (!moneyFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                moneyFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create money.yml: " + e.getMessage());
            }
        }
        moneyConfig = YamlConfiguration.loadConfiguration(moneyFile);
    }

    private List<String> getMoneyCommandsForDay(int day) {
        String key = "day-" + day + "-commands";
        return new ArrayList<>(moneyConfig.getStringList(key));
    }

    private void setMoneyCommandsForDay(int day, List<String> commands) {
        String key = "day-" + day + "-commands";
        moneyConfig.set(key, commands);
        try {
            moneyConfig.save(moneyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save money.yml: " + e.getMessage());
        }
    }

    private boolean isMoneyEnabled() {
        if (plugin == null) {
            return false;
        } else {
            plugin.getConfig();
        }

        String moneySystem = plugin.getConfig().getString("monney", "none").toLowerCase();
        boolean isValid = Set.of("vault", "playerpoints", "coinsengine", "ultraeconomy").contains(moneySystem);

        if (!isValid && !moneySystem.equals("none")) {
            plugin.getLogger().warning("Système d'argent invalide configuré : " + moneySystem);
        }

        return isValid;
    }

    private void prepareItemAreaSlots() {
        itemSlots.clear();
        int[] starts = {10, 19, 28, 37};
        for (int rowStart : starts) {
            for (int i = 0; i < 7; i++) {
                itemSlots.add(rowStart + i);
            }
        }
    }

    private void addBorderPanes() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(Utils.color("&7"));
        pane.setItemMeta(meta);

        // Top row except arrows and center if occupied later
        for (int i = 0; i < 9; i++) {
            if (i == 0 || i == 8) continue; // arrows
            currentGUI.setItem(i, pane);
        }
        // Sides
        for (int r = 1; r <= 4; r++) {
            currentGUI.setItem(r * 9, pane);
            currentGUI.setItem(r * 9 + 8, pane);
        }
        // Bottom row
        for (int i = 45; i < 54; i++) {
            currentGUI.setItem(i, pane);
        }
    }

    private void addNavigationArrows() {
        // Left arrow (previous day)
        ItemStack leftArrow = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = leftArrow.getItemMeta();
        meta.setDisplayName(Utils.color("&ePrevious day"));
        leftArrow.setItemMeta(meta);
        currentGUI.setItem(0, leftArrow);

        // Right arrow (next day)
        ItemStack rightArrow = new ItemStack(Material.SPECTRAL_ARROW);
        meta = rightArrow.getItemMeta();
        meta.setDisplayName(Utils.color("&eNext day"));
        rightArrow.setItemMeta(meta);
        currentGUI.setItem(8, rightArrow);
    }

    private void addMoneyBlock() {
        int money = playerData.getDayMoney(currentDay);
        plugin.getLogger().info("[DailyRewards] addMoneyBlock day=" + currentDay + ", money=" + money);
        ItemStack goldBlock = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = goldBlock.getItemMeta();
        meta.setDisplayName(Utils.color("&6Money: " + money));

        // add the existing orders to lore
        List<String> lore = new ArrayList<>();
        List<String> moneyCommands = getMoneyCommandsForDay(currentDay);
        if (moneyCommands != null && !moneyCommands.isEmpty()) {
            lore.add(Utils.color("&7Money Commands:"));
            for (String cmd : moneyCommands) {
                lore.add(Utils.color("&f- " + cmd.replace("{player}", currentPlayer.getName())));
            }
            plugin.getLogger().info("[DailyRewards] Loaded " + moneyCommands.size() + " command(s) from money.yml for day=" + currentDay);
        } else {
            String moneySystem = plugin.getConfig().getString("monney", "none").toLowerCase();
            if (!moneySystem.equals("none")) {
                String command = buildMoneyCommand(moneySystem, money);
                if (!command.isEmpty()) {
                    lore.add(Utils.color("&7Command: &f" + command.replace("{player}", currentPlayer.getName())));
                    plugin.getLogger().info("[DailyRewards] No commands in money.yml; default command=" + command);
                }
            }
        }

        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }

        goldBlock.setItemMeta(meta);
        currentGUI.setItem(4, goldBlock);
    }
    private void openMoneyInput() {
        //money input in the chat
        if (chatInputListener != null) {
            chatInputListener.setWaitingForMoneyInput(currentPlayer, true);
        }
        currentPlayer.sendMessage(Utils.color("&6Enter the money amount in chat (or type 'cancel'):"));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (currentPlayer != null && currentPlayer.isOnline()) {
                currentPlayer.closeInventory();
            }
        }, 1L);
    }

    @EventHandler
    public void onMoneyInputClick(InventoryClickEvent event) {
        //todo delete
        return;
    }

    public void processMoneyInput(String input) {
        plugin.getLogger().info("[DailyRewards] processMoneyInput input='" + input + "' day=" + currentDay + ", player=" + (currentPlayer != null ? currentPlayer.getName() : "null"));
        if (input.equalsIgnoreCase("cancel")) {
            currentPlayer.sendMessage(Utils.color("&cMoney input cancelled."));
            reopenMainGUI(); // Reopen the GUI after cancellation
            return;
        }

        try {
            int money = Integer.parseInt(input);
            plugin.getLogger().info("[DailyRewards] Parsed money=" + money);
            
            if (money <= 0) {
                currentPlayer.sendMessage(Utils.color("&cMoney value must be positive!"));
                reopenMainGUI();
                return;
            }

            if (money > 1_000_000) {
                currentPlayer.sendMessage(Utils.color("&cMoney value too high! Maximum: 1,000,000"));
                reopenMainGUI();
                return;
            }

            playerData.setDayMoney(currentDay, money);
            plugin.getLogger().info("[DailyRewards] PlayerDataManager set day money: day=" + currentDay + ", value=" + money);
            currentPlayer.sendMessage(Utils.color("&aMoney amount set to: &e" + money));

            // Immediately update money.yml
            String moneySystem = plugin.getConfig().getString("monney", "none").toLowerCase();
            List<String> commands = new ArrayList<>();
            if (!moneySystem.equals("none")) {
                String command = buildMoneyCommand(moneySystem, money);
                if (!command.isEmpty()) {
                    commands.add(command);
                }
            }
            setMoneyCommandsForDay(currentDay, commands);
            plugin.getLogger().info("[DailyRewards] money.yml updated on chat input for day=" + currentDay + " commands=" + commands);
            addMoneyBlock(); // Update the GUI
            reopenMainGUI(); //Reopen the GUI after success

        } catch (NumberFormatException e) {
            currentPlayer.sendMessage(Utils.color("&cInvalid money value! Please enter a valid number."));
            plugin.getLogger().warning("[DailyRewards] Invalid money format: " + e.getMessage());
            reopenMainGUI();
        }
    }


    private void addSaveButton() {
        ItemStack save = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = save.getItemMeta();
        meta.setDisplayName(Utils.color("&aSave"));
        save.setItemMeta(meta);
        currentGUI.setItem(49, save);
    }


    private void loadDayItemsFromConfig(int day) {
        List<String> entries = plugin.getConfig().getStringList("rewards.day-" + day);
        int idx = 0;
        for (String entry : entries) {
            if (entry == null) continue;
            if (entry.startsWith("item:")) {
                String data = entry.substring("item:".length());
                ItemStack item = Utils.deserializeItemStack(data);
                if (item != null) {
                    Integer targetSlot = getNextFreeItemSlot(idx);
                    if (targetSlot == null) break;
                    currentGUI.setItem(targetSlot, item);
                    idx++;
                }
            }
        }
    }

    private Integer getNextFreeItemSlot(int fallbackIndex) {
        int count = 0;
        for (Integer slot : itemSlots) {
            if (currentGUI.getItem(slot) == null || currentGUI.getItem(slot).getType().isAir()) {
                if (count == fallbackIndex) return slot;
                count++;
            }
        }
        for (Integer slot : itemSlots) {
            if (currentGUI.getItem(slot) == null || currentGUI.getItem(slot).getType().isAir()) {
                return slot;
            }
        }
        return null;
    }

    private Integer getFirstEmptyItemSlot() {
        for (Integer slot : itemSlots) {
            ItemStack it = currentGUI.getItem(slot);
            if (it == null || it.getType().isAir()) return slot;
        }
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(currentGUI)) return;

        int slot = event.getRawSlot();

        // Handle navigation arrows
        if (slot == 0) { // Left arrow
            if (currentDay > 1) {
                saveCurrentDay();
                currentDay--;
                openDayGUI(currentDay);
            }
            event.setCancelled(true);
        } else if (slot == 8) { // Right arrow
            if (currentDay < maxDays) {
                saveCurrentDay();
                currentDay++;
                openDayGUI(currentDay);
            }
            event.setCancelled(true);
        } else if (slot == 4 && isMoneyEnabled()) {
            // Money block
            event.setCancelled(true);
            openMoneyInput();
        } else if (slot == 49) {
            // Save button
            event.setCancelled(true);
            saveCurrentDay();
            currentPlayer.sendMessage(Utils.color("&aDay " + currentDay + " saved."));
        } else if (isBorderSlot(slot)) {
            // Prevent taking border panes
            event.setCancelled(true);
        } else if (itemSlots.contains(slot)) {
            // Allow normal handling in item area (move/drag inside GUI)
            event.setCancelled(false);
        } else if (event.isShiftClick() && event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            // Shift-click from player inventory to GUI item area
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir()) {
                Integer target = getFirstEmptyItemSlot();
                if (target != null) {
                    event.setCancelled(true);
                    currentGUI.setItem(target, clicked.clone());
                    event.getClickedInventory().setItem(event.getSlot(), null);
                } else {
                    event.setCancelled(true);
                }
            }
        } else if (slot >= event.getView().getTopInventory().getSize()) {
            // Bottom inventory (player inventory) — allow normal behavior
            event.setCancelled(false);
        } else {
            // Any other top-inventory slot defaults to cancelled
            event.setCancelled(true);
        }
    }

    private void saveCurrentDay() {
        plugin.getLogger().info("[DailyRewards] saveCurrentDay start for day=" + currentDay);
        List<String> serialized = new ArrayList<>();
        for (Integer slot : itemSlots) {
            ItemStack item = currentGUI.getItem(slot);
            if (item !=null && !item.getType().isAir()) {
                String data = Utils.serializeItemStack(item.clone());
                if (data !=null) {
                    serialized.add("item:" + data);
                }
            }
        }

        // Save commands in money.yml
        String moneySystem = plugin.getConfig().getString("monney", "none").toLowerCase();
        List<String> commands = new ArrayList<>();
        if (!moneySystem.equals("none")) {
            int money = playerData.getDayMoney(currentDay);
            String command = buildMoneyCommand(moneySystem, money);
            if (!command.isEmpty()) {
                commands.add(command);
            }
        }
        setMoneyCommandsForDay(currentDay, commands);
        plugin.getLogger().info("[DailyRewards] money.yml write day=" + currentDay + " commands=" + commands);

        plugin.getConfig().set("rewards.day-" + currentDay, serialized);
        plugin.saveConfig();
        plugin.getLogger().info("[DailyRewards] config.yml saved for day=" + currentDay + " items=" + serialized.size());
    }



    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer().equals(currentPlayer) &&
                event.getView().getTitle().equals(Utils.color("&6Enter Money Amount"))) {

            if (chatInputListener !=null) {
                chatInputListener.setWaitingForMoneyInput(currentPlayer, false);
            }
        }
    }

    private boolean isBorderSlot(int slot) {
        // Top row except arrows, money, and possibly center
        if (slot >= 1 && slot <= 3) return true;
        if (slot >= 5 && slot <= 7) return true;
        // Sides
        if (slot % 9 == 0 || (slot + 1) % 9 == 0) {
            // exclude arrows at 0 and 8 handled earlier
            if (slot == 0 || slot == 8) return false;
            // exclude bottom row handled below
            if (slot >= 45) return false;
            return true;
        }
        // Bottom row except save button
        if (slot >= 45 && slot <= 53) {
            return slot != 49;
        }
        return false;
    }
    private String buildMoneyCommand(String system, double amount) {
        switch (system.toLowerCase()) {
            case "vault":
            case "essentialsx":
                return "eco give {player} " + amount;
            case "playerpoints":
                return "points give {player} " + (int)amount + " -s";
            case "coinsengine":
                return "coins give {player} " + (int)amount;
            case "ultraeconomy":
                String currency = plugin.getConfig().getString("ultraeconomy.currency", "default");
                return "addbalance {player} " + (currency.isEmpty() ? "default" : currency) + " " + amount;
            case "votingplugin":
                return "av User {player} AddPoints " + (int)amount;
            default:
                return "";
        }
    }
}