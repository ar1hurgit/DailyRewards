package Reward.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import Reward.Rewards;
import Reward.day.DayGUICommand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatInputListener implements Listener {
    private final Rewards plugin;
    private final Map<UUID, Boolean> waitingForMoneyInput = new HashMap<>();

    public ChatInputListener(Rewards plugin) {
        this.plugin = plugin;
    }

    public void setWaitingForMoneyInput(Player player, boolean waiting) {
        if (player !=null) {
            waitingForMoneyInput.put(player.getUniqueId(), waiting);
        }
    }

    public boolean isWaitingForMoneyInput(Player player) {
        return player != null&& waitingForMoneyInput.getOrDefault(player.getUniqueId(), false);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (waitingForMoneyInput.getOrDefault(playerUUID, false)) {
            plugin.getLogger().info("[DailyRewards] Chat captured for money input from " + player.getName() + ": '" + event.getMessage() + "'");
            event.setCancelled(true);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                DayGUICommand dayGUI = plugin.getDayGUICommand();

                if (dayGUI !=null) {
                    plugin.getLogger().info("[DailyRewards] Dispatching message to DayGUICommand.processMoneyInput()");
                    dayGUI.processMoneyInput(event.getMessage());
                } else {
                    player.sendMessage("§cError: Unable to process input. Please try again.");
                    plugin.getLogger().severe("[DailyRewards] dayGUICommand is null while processing chat input.");
                }

                waitingForMoneyInput.remove(playerUUID); // cleanup
                plugin.getLogger().info("[DailyRewards] Cleared waiting flag for " + player.getName());
            });
        }
    }
}