package Reward;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class RewardManager {

    private final Rewards plugin;
    private final PlayerDataManager playerData;

    public RewardManager(Rewards plugin, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
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

            if (day != currentDay + 1) {
                player.sendMessage(Utils.color(plugin.getConfig().getString("messages.invalid-order")));
                return;
            }
            if (lastClaimStr != null && !isReadyToClaim(lastClaimStr)) {
                player.sendMessage(Utils.color(plugin.getConfig().getString("messages.wait-message")
                        .replace("{time}", String.valueOf(getRemainingHours(lastClaimStr)))));
                return;
            }

            for (String command : plugin.getConfig().getStringList("rewards.day-" + day)) {
                if (command.startsWith("item:")) {
                    ItemStack rewardItem = Utils.deserializeItemStack(command.substring(5));
                    if (rewardItem != null) player.getInventory().addItem(rewardItem);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
                }
            }

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
