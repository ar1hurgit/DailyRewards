package Reward;

import Reward.baltop.BaltopGUIManager;
import Reward.day.DayGUICommand;
import Reward.listener.PlayerJoinListener;
import Reward.listener.ChatInputListener;
import Reward.placeholder.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Rewards extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private PlaceholderAPI papiExpansion;
    private DayGUICommand dayGUICommand;

    @Override
    public void onEnable() {
        // Initialisation of the PlayerDataManager
        this.playerDataManager = new PlayerDataManager(this);
        saveDefaultConfig();

        // event registration
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, playerDataManager), this);
        RewardManager rewardManager = new RewardManager(this, playerDataManager);
        GUIManager guiManager = new GUIManager(this, playerDataManager, rewardManager);
        getServer().getPluginManager().registerEvents(guiManager, this);

        BaltopGUIManager baltopGuiManager = new BaltopGUIManager(playerDataManager);
        getServer().getPluginManager().registerEvents(baltopGuiManager, this);


        dayGUICommand = new DayGUICommand(this, playerDataManager);
        getServer().getPluginManager().registerEvents(dayGUICommand, this);
        
        // listener for chat entries
        ChatInputListener chatInputListener = new ChatInputListener(this);
        getServer().getPluginManager().registerEvents(chatInputListener, this);
        dayGUICommand.setChatInputListener(chatInputListener);

        // command registration
        RewardsCommand rewardsCommand = new RewardsCommand(this, guiManager, rewardManager, playerDataManager);
        Objects.requireNonNull(getCommand("rewards")).setExecutor(rewardsCommand);
        Objects.requireNonNull(getCommand("rewards")).setTabCompleter(new RewardsTabCompleter(this));

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiExpansion = new PlaceholderAPI(this, playerDataManager);
            if (papiExpansion.register()) {
                getLogger().info("PlaceholderAPI enabled ");

            }
        }

        // Planning repetitive tasks
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, rewardManager::checkDailyReset, 0L, 20L * 60 * 60);

        getLogger().info("DailyRewards plugin version " + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (papiExpansion != null) {
            papiExpansion.unregister();
        }
        getLogger().info("DailyRewards disabled");
    }

    // Getter for PlayerDataManager if needed elsewhere (not use)
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    // Getter for DayGUICommand
    public DayGUICommand getDayGUICommand() {
        return dayGUICommand;
    }
}