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
}