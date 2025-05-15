package me.afroninja.afroauction;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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

    public boolean hasNotificationsEnabled(UUID playerUUID) {
        return notificationSettings.getOrDefault(playerUUID, true); // Default to true
    }

    public void toggleNotifications(UUID playerUUID) {
        boolean currentState = notificationSettings.getOrDefault(playerUUID, true);
        notificationSettings.put(playerUUID, !currentState);
        saveNotificationSettings();
    }

    public void saveNotificationSettings() {
        File file = new File(plugin.getDataFolder(), "notifications.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("notifications", null);
        for (Map.Entry<UUID, Boolean> entry : notificationSettings.entrySet()) {
            config.set("notifications." + entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save notifications.yml: " + e.getMessage());
        }
    }

    public void loadNotificationSettings() {
        File file = new File(plugin.getDataFolder(), "notifications.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("notifications")) {
            return;
        }

        for (String key : config.getConfigurationSection("notifications").getKeys(false)) {
            try {
                UUID playerUUID = UUID.fromString(key);
                boolean enabled = config.getBoolean("notifications." + key, true);
                notificationSettings.put(playerUUID, enabled);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load notification settings for " + key + ": " + e.getMessage());
            }
        }
    }
}