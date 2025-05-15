package me.afroninja.afroauction;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NotificationManager {
    private final AfroAuction plugin;
    private final Map<UUID, Boolean> notificationSettings;

    public NotificationManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.notificationSettings = new HashMap<>();
    }

    public void setNotificationsEnabled(UUID playerUUID, boolean enabled) {
        notificationSettings.put(playerUUID, enabled);
    }

    public boolean hasNotificationsEnabled(UUID playerUUID) {
        return notificationSettings.getOrDefault(playerUUID, true);
    }

    public void sendOutbidMessage(UUID playerUUID, String itemName) {
        if (hasNotificationsEnabled(playerUUID)) {
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null) {
                player.sendMessage(plugin.getMessage("outbid", "%item%", itemName));
            }
        }
    }
}