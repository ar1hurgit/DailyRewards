package Reward;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;


public class GUIManager implements Listener {

    private final Rewards plugin;
    private final PlayerDataManager playerData;
    private final RewardManager rewardManager;
    private final int[] rewardSlots = new int[45];

    public GUIManager(Rewards plugin, PlayerDataManager playerData, RewardManager rewardManager) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.rewardManager = rewardManager;
        for (int i = 0; i < 45; i++) rewardSlots[i] = i;
    }

    public void openRewardsGUI(Player player, int page) {

        int totalDays = plugin.getConfig().getInt("total-days");

        int itemsPerPage = rewardSlots.length;

        int totalPages = (int) Math.ceil((double) totalDays / itemsPerPage);

        Inventory gui = Bukkit.createInventory(new RewardsHolder(page), 54,
                Utils.color(plugin.getConfig().getString("gui.title"))
                        .replace("{page}", String.valueOf(page + 1))
                        .replace("{max}", String.valueOf(totalPages)));



        // Fond gris
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for (int i = 45; i < 54; i++) gui.setItem(i, bg);



        // Récompenses
        String uuid = player.getUniqueId().toString();
        int currentDay = playerData.getDay(uuid);
        String lastClaimStr = playerData.getLastClaim(uuid);

        for (int i = 0; i < itemsPerPage; i++) {
            int day = page * itemsPerPage + i + 1;
            if (day > totalDays) break;
            gui.setItem(rewardSlots[i], rewardManager.createRewardItem(day, currentDay, lastClaimStr));
        }
        // Navigation
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        if (page > 0) {
            gui.setItem(45, Utils.createItem(Material.ARROW, plugin.getConfig().getString("gui.navigation.previous")));
        }
        if (page < totalPages - 1 && totalPages > 1) {
            gui.setItem(53, Utils.createItem(Material.ARROW, plugin.getConfig().getString("gui.navigation.next")));
        }

        player.openInventory(gui);

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RewardsHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getSlot();

        int totalDays = plugin.getConfig().getInt("total-days");
        int itemsPerPage = rewardSlots.length;
        int totalPages = (int) Math.ceil((double) totalDays / itemsPerPage);
        int currentPage = holder.getPage();

        if (slot == 45) { // Précédent
            if (currentPage > 0) {
                openRewardsGUI(player, currentPage - 1);
            }
        } else if (slot == 53) { // Suivant
            if (currentPage < totalPages - 1) {
                openRewardsGUI(player, currentPage + 1);
            }
        } else if (slot >= 0 && slot < 45) { // Clic sur une récompense
            rewardManager.handleRewardClaim(player, event.getCurrentItem());
        }
    }

    public static class RewardsHolder implements InventoryHolder {
        private final int page;
        public RewardsHolder(int page) { this.page = page; }
        public int getPage() { return page; }
        public  Inventory getInventory() { return null; }
    }
}
