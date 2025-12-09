package Reward.listener;

import Reward.PlayerDataManager;
import Reward.Rewards;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.time.LocalDateTime;

public class PlayerJoinListener implements Listener {

    private final Rewards plugin;
    private final PlayerDataManager playerData;

    public PlayerJoinListener(Rewards plugin, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
        plugin.getLogger().info("PlayerJoinListener initialized with PlayerDataManager");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String playerName = player.getName();

        plugin.getLogger().info("Processing join event for player: " + playerName + " (" + uuid + ")");

        try {
            if (!playerData.hasPlayerData(uuid)) {
                plugin.getLogger().info("New player detected: " + playerName + " (" + uuid + ")");

                // Initialize new player data
                playerData.setDay(uuid, 0);
                LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
                playerData.setLastClaim(uuid, yesterday.toLocalDate().atStartOfDay().toString());

                plugin.getLogger().info("Successfully initialized data for new player: " + playerName);
            } else {
                plugin.getLogger().info(" Returning player detected: " + playerName + " (" + uuid + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing join event for player " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}