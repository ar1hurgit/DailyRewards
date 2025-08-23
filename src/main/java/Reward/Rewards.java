package Reward;

import Reward.Command.BaltopGUIManager;
import Reward.Listener.PlayerJoinListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Rewards extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PlayerDataManager playerDataManager = new PlayerDataManager(this);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, playerDataManager), this);
        RewardManager rewardManager = new RewardManager(this, playerDataManager);
        GUIManager guiManager = new GUIManager(this, playerDataManager, rewardManager);

        getServer().getPluginManager().registerEvents(guiManager, this);
        BaltopGUIManager baltopGuiManager = new BaltopGUIManager(playerDataManager);
        getServer().getPluginManager().registerEvents(baltopGuiManager, this);


        RewardsCommand rewardsCommand = new RewardsCommand(this, guiManager, rewardManager, playerDataManager);
        Objects.requireNonNull(getCommand("rewards")).setExecutor(rewardsCommand);
        Objects.requireNonNull(getCommand("rewards")).setTabCompleter(new RewardsTabCompleter(this));

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, rewardManager::checkDailyReset, 0L, 20L * 60 * 60);

    }
}