// PlayerDataManager.java
package Reward;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PlayerDataManager {
    private final Rewards plugin;
    private final File dataFile;
    private FileConfiguration playerData;

    public PlayerDataManager(Rewards plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player_data.yml");

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[DailyRewards] Failed to create player_data.yml: " + e.getMessage());
            }
        }
        loadData();
    }

    private void loadData() {
        playerData = YamlConfiguration.loadConfiguration(dataFile);
    }


    private void saveData() {
        try {
            playerData.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[DailyRewards] Failed to save player_data.yml: " + e.getMessage());
        }
    }

    public boolean hasPlayerData(String uuid) {
        return playerData.contains(uuid + ".day");
    }

    public int getDay(String uuid) {
        return playerData.getInt(uuid + ".day", 1);
    }

    public void setDay(String uuid, int day) {
        playerData.set(uuid + ".day", day);
        saveData();
    }

    public String getLastClaim(String uuid) {
        return playerData.getString(uuid + ".lastClaim",
                LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    public void setLastClaim(String uuid, String time) {
        playerData.set(uuid + ".lastClaim", time);
        saveData();
    }

    public void save() {
        saveData();
    }

    public FileConfiguration getConfig() {
        return playerData;
    }

    public Map<UUID, Integer> getAllPlayerDays() {
        Map<UUID, Integer> playerDays = new HashMap<>();
        ConfigurationSection root = playerData.getConfigurationSection("");
        if (root == null) return playerDays;

        for (String uuidStr : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                int day = playerData.getInt(uuidStr + ".day", 0);
                playerDays.put(uuid, day);
            } catch (IllegalArgumentException ignored) {}
        }
        return playerDays;
    }
    public int getPlayerDay(UUID uuid) {
        return getDay(uuid.toString());
    }
    public void reload() {
        loadData();
    }
    public int getPlayerday(UUID uuid) {
        String uuidString = uuid.toString();
        String uuidWithoutDashes = uuidString.replace("-", "");

        // Essayer d'abord avec le format sans tirets (le plus probable)
        if (playerData.contains(uuidWithoutDashes + ".day")) {
            return playerData.getInt(uuidWithoutDashes + ".day", 1);
        }
        // Sinon essayer avec le format avec tirets
        else if (playerData.contains(uuidString + ".day")) {
            return playerData.getInt(uuidString + ".day", 1);
        }
        // Par défaut
        return 1;
    }

    public List<PlayerData> getTopPlayers(int limit) {
        List<PlayerData> topPlayers = new ArrayList<>();
        // Implémentation pour récupérer les meilleurs joueurs
        for (String key : playerData.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int claims = playerData.getInt(key + ".totalClaims", 0);
                topPlayers.add(new PlayerData(uuid, claims));
            } catch (IllegalArgumentException ignored) {}
        }

        topPlayers.sort((a, b) -> Integer.compare(b.getTotalClaims(), a.getTotalClaims()));
        return topPlayers.subList(0, Math.min(limit, topPlayers.size()));
    }

    // Classe interne pour les données joueur
    public static class PlayerData {
        private final UUID uuid;
        private final int totalClaims;

        public PlayerData(UUID uuid, int totalClaims) {
            this.uuid = uuid;
            this.totalClaims = totalClaims;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getTotalClaims() {
            return totalClaims;
        }
    }


}