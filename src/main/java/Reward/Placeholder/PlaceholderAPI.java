package Reward.Placeholder;

import Reward.PlayerDataManager;
import Reward.Rewards;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlaceholderAPI extends PlaceholderExpansion {

    private final Rewards plugin;
    private final PlayerDataManager playerDataManager;

    public PlaceholderAPI(Rewards plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dailyrewards";
    }

    @Override
    public @NotNull String getAuthor() {
        return "black hole studio";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        UUID uuid = player.getUniqueId();

        switch (params.toLowerCase()) {
            case "day": // %dailyrewards_day%
                return String.valueOf(playerDataManager.getPlayerDay(uuid));

            case "current": // %dailyrewards_current%
                int day = playerDataManager.getPlayerDay(uuid);
                return plugin.getConfig().getString("rewards.day" + day + ".name", "Récompense jour " + day);

            case "top": // %dailyrewards_top%
                return getTopPlayersFormatted(10);

            case "top_simple": // %dailyrewards_top_simple%
                return getTopPlayersSimple(10);

            case "top_colored": // %dailyrewards_top_colored%
                return getTopPlayersColored(10);

            default:
                return null;
        }
    }

    private String getTopPlayersFormatted(int limit) {
        try {
            // Utiliser le même système que BaltopGUI
            Map<UUID, Integer> allPlayers = playerDataManager.getAllPlayerDays();
            List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(allPlayers.entrySet());
            sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            StringBuilder result = new StringBuilder();

            if (sorted.isEmpty()) {
                return "Aucun joueur dans le classement";
            }

            int displayLimit = Math.min(limit, sorted.size());

            for (int i = 0; i < displayLimit; i++) {
                Map.Entry<UUID, Integer> entry = sorted.get(i);
                String playerName = getPlayerName(entry.getKey());
                int days = entry.getValue();

                result.append("§e#")
                        .append(i + 1)
                        .append(" §f- §b")
                        .append(playerName)
                        .append(" §f- §a")
                        .append(days)
                        .append(" récompenses\n");
            }

            return result.toString().trim();
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur dans getTopPlayersFormatted: " + e.getMessage());
            return "Erreur de classement";
        }
    }

    private String getTopPlayersColored(int limit) {
        try {
            Map<UUID, Integer> allPlayers = playerDataManager.getAllPlayerDays();
            List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(allPlayers.entrySet());
            sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            StringBuilder result = new StringBuilder();

            if (sorted.isEmpty()) {
                return "§cAucun joueur dans le classement";
            }

            String[] colors = {"§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"};
            int displayLimit = Math.min(limit, sorted.size());

            for (int i = 0; i < displayLimit; i++) {
                Map.Entry<UUID, Integer> entry = sorted.get(i);
                String playerName = getPlayerName(entry.getKey());
                int days = entry.getValue();

                String color = colors[i % colors.length];
                result.append(color)
                        .append("#")
                        .append(i + 1)
                        .append(" §f- ")
                        .append(playerName)
                        .append(" §7- §a")
                        .append(days)
                        .append(" récompenses\n");
            }

            return result.toString().trim();
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur dans getTopPlayersColored: " + e.getMessage());
            return "Erreur de classement";
        }
    }

    private String getTopPlayersSimple(int limit) {
        try {
            Map<UUID, Integer> allPlayers = playerDataManager.getAllPlayerDays();
            List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(allPlayers.entrySet());
            sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            StringBuilder result = new StringBuilder();

            int displayLimit = Math.min(limit, sorted.size());

            for (int i = 0; i < displayLimit; i++) {
                Map.Entry<UUID, Integer> entry = sorted.get(i);
                String playerName = getPlayerName(entry.getKey());
                int days = entry.getValue();

                result.append("#")
                        .append(i + 1)
                        .append(" - ")
                        .append(playerName)
                        .append(" - ")
                        .append(days)
                        .append(" récompenses\n");
            }

            return result.toString().trim();
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur dans getTopPlayersSimple: " + e.getMessage());
            return "Erreur de classement";
        }
    }

    private String getPlayerName(UUID uuid) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() != null) {
                return offlinePlayer.getName();
            }

            // Fallback: essayez de récupérer le nom depuis votre PlayerDataManager si possible
            // (vous devrez peut-être ajouter une méthode pour stocker les noms)
            return "Joueur #" + uuid.toString().substring(0, 8);

        } catch (Exception e) {
            return "Joueur inconnu";
        }
    }
}