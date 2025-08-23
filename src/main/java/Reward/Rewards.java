package Reward;

import Reward.Command.BaltopGUIManager;
import Reward.Listener.PlayerJoinListener;
import Reward.Placeholder.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Rewards extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private PlaceholderAPI papiExpansion;

    @Override
    public void onEnable() {
        // Initialisation du PlayerDataManager avec la variable de classe
        this.playerDataManager = new PlayerDataManager(this);
        saveDefaultConfig();

        // Enregistrement des événements
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, playerDataManager), this);

        RewardManager rewardManager = new RewardManager(this, playerDataManager);
        GUIManager guiManager = new GUIManager(this, playerDataManager, rewardManager);
        getServer().getPluginManager().registerEvents(guiManager, this);

        BaltopGUIManager baltopGuiManager = new BaltopGUIManager(playerDataManager);
        getServer().getPluginManager().registerEvents(baltopGuiManager, this);

        // Enregistrement des commandes
        RewardsCommand rewardsCommand = new RewardsCommand(this, guiManager, rewardManager, playerDataManager);
        Objects.requireNonNull(getCommand("rewards")).setExecutor(rewardsCommand);
        Objects.requireNonNull(getCommand("rewards")).setTabCompleter(new RewardsTabCompleter(this));

        // Enregistrement de PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiExpansion = new PlaceholderAPI(this, playerDataManager);
            if (papiExpansion.register()) {
                getLogger().info("PlaceholderAPI expansion enregistrée !");

            }
        }

        // Planification des tâches répétitives
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, rewardManager::checkDailyReset, 0L, 20L * 60 * 60);

        getLogger().info("Plugin DailyRewards version " + getDescription().getVersion() + " activé");
    }

    @Override
    public void onDisable() {
        if (papiExpansion != null) {
            papiExpansion.unregister();
        }
        getLogger().info("DailyRewards désactivé");
    }

    // Getter pour PlayerDataManager si nécessaire ailleurs
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}