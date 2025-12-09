package Reward.baltop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import Reward.PlayerDataManager;
import Reward.Utils;
import java.util.*;

public class BaltopGUIManager implements Listener {
    private final PlayerDataManager playerData;
    private static final int PLAYER_SLOTS = 45;
    private static final int[] DECORATION_SLOTS = {45, 46, 47, 48, 50, 51, 52, 53};

    public BaltopGUIManager(PlayerDataManager playerData) {
        this.playerData = playerData;
    }

    public void openBaltopGUI(Player viewer, int page) {
        Map<UUID, Integer> allPlayers = playerData.getAllPlayerDays();
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(allPlayers.entrySet());
        sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        int totalPages = (int) Math.ceil((double) sorted.size() / PLAYER_SLOTS);
        page = Math.max(0, Math.min(page, totalPages - 1));

        // Gui
        BaltopHolder holder = new BaltopHolder(page);
        Inventory gui = Bukkit.createInventory(holder, 54,
                Utils.color("&4&lTop player - Page " + (page + 1) + "/" + totalPages));

        // background
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for (int slot : DECORATION_SLOTS) gui.setItem(slot, bg);

        // players
        int startIndex = page * PLAYER_SLOTS;
        for (int i = 0; i < PLAYER_SLOTS; i++) {
            int index = startIndex + i;
            if (index >= sorted.size()) break;

            Map.Entry<UUID, Integer> entry = sorted.get(index);
            gui.setItem(i, createPlayerHead(entry.getKey(), index + 1, entry.getValue()));
        }

        // Navigation
        if (page > 0) {
            gui.setItem(45, Utils.createItem(Material.ARROW, "&6 ←"));
        }
        if (page < totalPages - 1) {
            gui.setItem(53, Utils.createItem(Material.ARROW, "&6 →"));
        }

        // User PlayerHead
        gui.setItem(49, createViewerHead(viewer));

        viewer.openInventory(gui);
    }

    private ItemStack createPlayerHead(UUID uuid, int rank, int day) {
        org.bukkit.plugin.java.JavaPlugin pluginRef =
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(Reward.Utils.class);

        ItemStack head = Reward.Utils.getPlayerHead(uuid, pluginRef);
        org.bukkit.inventory.meta.ItemMeta meta = head.getItemMeta();

        String name = Reward.Utils.getPlayerName(uuid, pluginRef);
        String display = (name == null || name.isBlank()) ? "§cPlayer #" + rank : name;

        meta.setDisplayName(Reward.Utils.color("&6" + display));
        meta.setLore(java.util.Arrays.asList(
                Reward.Utils.color("&7Rank: &4#" + rank),
                Reward.Utils.color("&7Day: &e" + Reward.Utils.getOrdinal(day))
        ));
        head.setItemMeta(meta);
        return head;
    }


    private ItemStack createViewerHead(Player viewer) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        int viewerDay = playerData.getDay(viewer.getUniqueId().toString());
        int viewerRank = getPlayerRank(viewer.getUniqueId());

        meta.setOwningPlayer(viewer);
        meta.setDisplayName(Utils.color("&a" + viewer.getName()));
        meta.setLore(Arrays.asList(
                Utils.color("&7your rank: &4#" + viewerRank),
                Utils.color("&7your day: &e" + Utils.getOrdinal(viewerDay))
        ));
        head.setItemMeta(meta);
        return head;
    }

    private int getPlayerRank(UUID uuid) {
        Map<UUID, Integer> allPlayers = playerData.getAllPlayerDays();
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(allPlayers.entrySet());
        sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(uuid)) return i + 1;
        }
        return -1;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaltopHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getSlot();

        if (slot == 45) { // previous page
            openBaltopGUI(player, holder.getPage() - 1);
        } else if (slot == 53) { // next page
            openBaltopGUI(player, holder.getPage() + 1);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BaltopHolder) {
            event.setCancelled(true);
        }
    }

    public static class BaltopHolder implements InventoryHolder {
        private final int page;

        public BaltopHolder(int page) {
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
}