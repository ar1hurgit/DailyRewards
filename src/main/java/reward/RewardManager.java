package reward;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;

public class RewardManager {

    private final Rewards plugin;
    private final PlayerDataManager playerData;
    private File moneyFile;
    private FileConfiguration moneyConfig;

    public RewardManager(Rewards plugin, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
        initMoneyStorage();
    }

    private void initMoneyStorage() {
        try {
            moneyFile = new File(plugin.getDataFolder(), "money.yml");
            if (!moneyFile.exists()) {
                plugin.getDataFolder().mkdirs();
                moneyFile.createNewFile();
            }
            moneyConfig = YamlConfiguration.loadConfiguration(moneyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[DailyRewards] Failed to initialize money.yml: " + e.getMessage());
        }
    }

    private List<String> getMoneyCommandsForDay(int day) {
        if (moneyConfig == null) initMoneyStorage();
        String key = "day-" + day + "-commands";
        List<String> list = moneyConfig.getStringList(key);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public void checkDailyReset() {
        if (LocalDateTime.now().getHour() == 0) {
            boolean resetEnabled = plugin.getConfig().getBoolean("reset.enabled", true);
            int resetDelay = plugin.getConfig().getInt("reset.delay", 1);

            for (String uuid : playerData.getConfig().getKeys(false)) {
                String lastClaimStr = playerData.getLastClaim(uuid);
                if (lastClaimStr == null) continue;

                try {
                    LocalDateTime lastClaim = LocalDateTime.parse(lastClaimStr);
                    LocalDate lastClaimDate = lastClaim.toLocalDate();
                    LocalDate currentDate = LocalDate.now();
                    long daysBetween = ChronoUnit.DAYS.between(lastClaimDate, currentDate);

                    if (resetEnabled && daysBetween > resetDelay) {
                        playerData.setDay(uuid, 0);
                        playerData.setLastClaim(uuid, LocalDateTime.of(currentDate, LocalTime.MIDNIGHT).toString());
                    }
                } catch (Exception ignored) {
                }
            }
            playerData.save();
        }
    }

    public ItemStack createRewardItem(int day, int currentDay, String lastClaimStr) {
        Material mat;
        String name;
        List<String> lore = new ArrayList<>();

        if (day <= currentDay) {
            mat = Material.CHEST;
            name = Utils.color("&aDay " + day);
            lore.add(Utils.color("&7Already claimed!"));
        } else if (day == currentDay + 1) {
            if (lastClaimStr == null || isReadyToClaim(lastClaimStr)) {
                mat = Material.GOLD_BLOCK;
                name = Utils.color("&eDay " + day);
                lore.add(Utils.color("&7Click to claim!"));
            } else {
                mat = Material.REDSTONE_BLOCK;
                name = Utils.color("&cDay " + day);
                lore.add(Utils.color("&7Come back in " + getRemainingHours(lastClaimStr) + "h!"));
            }
        } else {
            mat = Material.BARRIER;
            name = Utils.color("&cDay " + day);
            lore.add(Utils.color("&7Claim previous days first!"));
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void handleRewardClaim(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BARRIER) return;

        try {
            int day = Integer.parseInt(ChatColor.stripColor(item.getItemMeta().getDisplayName()).split(" ")[1]);
            String uuid = player.getUniqueId().toString();
            int currentDay = playerData.getDay(uuid);
            String lastClaimStr = playerData.getLastClaim(uuid);

            // Check if the clicked day is the next claimable day
            if (day != currentDay + 1) {
                player.sendMessage(Utils.color(plugin.getConfig().getString("messages.invalid-order")));
                return;
            }

            // Check cooldown
            if (lastClaimStr != null && !isReadyToClaim(lastClaimStr)) {
                player.sendMessage(Utils.color(plugin.getConfig().getString("messages.wait-message")
                        .replace("{time}", String.valueOf(getRemainingHours(lastClaimStr)))));
                return;
            }

            // Collect all item rewards for this day
            List<ItemStack> itemsToGive = new ArrayList<>();
            for (String command : plugin.getConfig().getStringList("rewards.day-" + day)) {
                if (command.startsWith("item:")) {
                    ItemStack rewardItem = Utils.deserializeItemStack(command.substring(5));
                    if (rewardItem != null) itemsToGive.add(rewardItem);
                }
            }

            // Count free inventory slots
            long freeSlots = player.getInventory().getStorageContents().length
                    - java.util.Arrays.stream(player.getInventory().getStorageContents())
                    .filter(i -> i != null && i.getType() != Material.AIR)
                    .count();

            // If there are not enough free slots, cancel the claim
            if (freeSlots < itemsToGive.size()) {
                player.sendMessage(Utils.color("&cYou do not have enough inventory space!"));
                return;
            }

            // Give the item rewards
            for (ItemStack it : itemsToGive) {
                player.getInventory().addItem(it);
            }

            // Execute other normal commands (money, etc.)
            for (String command : plugin.getConfig().getStringList("rewards.day-" + day)) {
                if (!command.startsWith("item:")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
                }
            }

            // Execute money.yml commands
            List<String> moneyCmds = getMoneyCommandsForDay(day);
            if (!moneyCmds.isEmpty()) {
                for (String command : moneyCmds) {
                    if (command != null && !command.trim().isEmpty()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
                    }
                }
            }

            // Update player data
            playerData.setDay(uuid, day);
            playerData.setLastClaim(uuid, LocalDateTime.now().toString());
            player.closeInventory();

            player.sendMessage(Utils.color(plugin.getConfig().getString("messages.claim-success")
                    .replace("{day}", String.valueOf(day))));

        } catch (Exception e) {
            player.sendMessage(Utils.color("&cError claiming reward!"));
            plugin.getLogger().warning("Error handling reward claim: " + e.getMessage());
        }
    }


    private boolean isReadyToClaim(String lastClaimStr) {
        try {
            LocalDateTime lastClaim = LocalDateTime.parse(lastClaimStr);
            return LocalDate.now().isAfter(lastClaim.toLocalDate());
        } catch (Exception e) {
            return true;
        }
    }

    private long getRemainingHours(String lastClaimStr) {
        try {
            LocalDateTime lastClaim = LocalDateTime.parse(lastClaimStr);
            LocalDateTime nextMidnight = lastClaim.toLocalDate().plusDays(1).atStartOfDay();
            return Math.max(ChronoUnit.HOURS.between(LocalDateTime.now(), nextMidnight), 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
