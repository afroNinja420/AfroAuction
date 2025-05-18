package me.afroninja.afroauction.managers;

import me.afroninja.afroauction.AfroAuction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages notification settings and sends outbid messages to players.
 */
public class NotificationManager {
    private final AfroAuction plugin;
    private final Map<UUID, Boolean> notificationSettings;

    /**
     * Constructs a new NotificationManager instance.
     * @param plugin the AfroAuction plugin instance
     */
    public NotificationManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.notificationSettings = new HashMap<>();
        loadNotificationSettings();
    }

    /**
     * Sets whether a player has notifications enabled.
     * @param playerUUID the player's UUID
     * @param enabled true to enable, false to disable
     */
    public void setNotificationsEnabled(UUID playerUUID, boolean enabled) {
        notificationSettings.put(playerUUID, enabled);
    }

    /**
     * Checks if a player has notifications enabled.
     * @param playerUUID the player's UUID
     * @return true if notifications are enabled, false otherwise
     */
    public boolean hasNotificationsEnabled(UUID playerUUID) {
        return notificationSettings.getOrDefault(playerUUID, true);
    }

    /**
     * Sends an outbid message to a player if notifications are enabled.
     * @param playerUUID the player's UUID
     * @param itemName the name of the auctioned item
     */
    public void sendOutbidMessage(UUID playerUUID, String itemName) {
        if (!hasNotificationsEnabled(playerUUID)) return;
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player != null) {
            player.sendMessage(plugin.getMessage("outbid", "%item%", itemName));
        }
    }

    /**
     * Loads notification settings from the notifications.yml file.
     */
    public void loadNotificationSettings() {
        File notificationsFile = new File(plugin.getDataFolder(), "notifications.yml");
        if (!notificationsFile.exists()) {
            try {
                notificationsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create notifications.yml: " + e.getMessage());
            }
            return;
        }

        FileConfiguration notificationsConfig = YamlConfiguration.loadConfiguration(notificationsFile);
        for (String key : notificationsConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(key);
            boolean enabled = notificationsConfig.getBoolean(key);
            notificationSettings.put(playerUUID, enabled);
        }
    }

    /**
     * Saves notification settings to the notifications.yml file.
     */
    public void saveNotificationSettings() {
        File notificationsFile = new File(plugin.getDataFolder(), "notifications.yml");
        FileConfiguration notificationsConfig = YamlConfiguration.loadConfiguration(notificationsFile);
        for (Map.Entry<UUID, Boolean> entry : notificationSettings.entrySet()) {
            notificationsConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            notificationsConfig.save(notificationsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save notifications.yml: " + e.getMessage());
        }
    }
}