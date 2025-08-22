package Reward;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    // Méthode existante (conservée, mais améliorée)
    public boolean hasPlayerData(String uuid) {
        // Vérifie si le joueur a un "day" défini (0 = nouveau joueur, >0 = existant)
        return playerData.contains(uuid + ".day");
    }

    // Méthode existante (conservée, mais retourne 1 par défaut pour un nouveau joueur)
    public int getDay(String uuid) {
        return playerData.getInt(uuid + ".day", 1); // 1 = nouveau joueur
    }

    // Méthode existante (conservée)
    public void setDay(String uuid, int day) {
        playerData.set(uuid + ".day", day);
        saveData();
    }

    // Méthode existante (conservée, mais retourne une date par défaut si non définie)
    public String getLastClaim(String uuid) {
        return playerData.getString(uuid + ".lastClaim",
                LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    // Méthode existante (conservée)
    public void setLastClaim(String uuid, String time) {
        playerData.set(uuid + ".lastClaim", time);
        saveData();
    }

    // Méthode existante (conservée)
    public void save() {
        saveData();
    }

    // Méthode existante (conservée)
    public FileConfiguration getConfig() {
        return playerData;
    }
}