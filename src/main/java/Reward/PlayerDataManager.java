package Reward;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class PlayerDataManager {
    private final Rewards plugin;
    private final File dataFile;
    private final FileConfiguration playerData;

    public PlayerDataManager(Rewards plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player_data.yml");

        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        this.playerData = YamlConfiguration.loadConfiguration(dataFile);
    }

    public int getDay(String uuid) { return playerData.getInt(uuid + ".day", 0); }

    public void setDay(String uuid, int day) {
        playerData.set(uuid + ".day", day);
        save();
    }

    public String getLastClaim(String uuid) {
        return playerData.getString(uuid + ".lastClaim");
    }

    public void setLastClaim(String uuid, String time) {
        playerData.set(uuid + ".lastClaim", time);
        save();
    }

    public void save() {
        try { playerData.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("Failed to save player data!"); }
    }

    public FileConfiguration getConfig() { return playerData; }
}
