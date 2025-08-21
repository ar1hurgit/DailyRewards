package Reward;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Rewards extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PlayerDataManager playerDataManager = new PlayerDataManager(this);
        RewardManager rewardManager = new RewardManager(this, playerDataManager);
        GUIManager guiManager = new GUIManager(this, playerDataManager, rewardManager);

        getServer().getPluginManager().registerEvents(guiManager, this);

        RewardsCommand rewardsCommand = new RewardsCommand(this, guiManager, rewardManager, playerDataManager);
        Objects.requireNonNull(getCommand("rewards")).setExecutor(rewardsCommand);
        Objects.requireNonNull(getCommand("rewards")).setTabCompleter(new RewardsTabCompleter());

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, rewardManager::checkDailyReset, 0L, 20L * 60 * 60);
    }


}
