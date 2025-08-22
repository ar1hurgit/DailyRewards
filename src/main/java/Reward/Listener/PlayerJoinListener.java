package Reward.Listener;

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
        plugin.getLogger().info("[DailyRewards] PlayerJoinListener initialized with PlayerDataManager");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String playerName = player.getName();

        plugin.getLogger().info("[DailyRewards] Processing join event for player: " + playerName + " (" + uuid + ")");

        try {
            if (!playerData.hasPlayerData(uuid)) {
                plugin.getLogger().info("[DailyRewards] New player detected: " + playerName + " (" + uuid + ")");

                // Initialize new player data
                playerData.setDay(uuid, 0);
                LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
                playerData.setLastClaim(uuid, yesterday.toLocalDate().atStartOfDay().toString());

                plugin.getLogger().info("[DailyRewards] Successfully initialized data for new player: " + playerName);
            } else {
                plugin.getLogger().info("[DailyRewards] Returning player detected: " + playerName + " (" + uuid + ")");
                // Optional: You could add checks for daily reward availability here
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[DailyRewards] Error processing join event for player " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}