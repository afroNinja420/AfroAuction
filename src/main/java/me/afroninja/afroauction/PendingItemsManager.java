package me.afroninja.afroauction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PendingItemsManager {
    private final AfroAuction plugin;
    private final Map<UUID, ItemStack> pendingItems;

    public PendingItemsManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.pendingItems = new HashMap<>();
    }

    public void addPendingItem(UUID playerUUID, ItemStack item) {
        pendingItems.put(playerUUID, item.clone());
        savePendingItems();
    }

    public ItemStack getPendingItem(UUID playerUUID) {
        return pendingItems.get(playerUUID);
    }

    public void removePendingItem(UUID playerUUID) {
        pendingItems.remove(playerUUID);
        savePendingItems();
    }

    public void savePendingItems() {
        File file = new File(plugin.getDataFolder(), "pending.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("pending", null);
        for (Map.Entry<UUID, ItemStack> entry : pendingItems.entrySet()) {
            config.set("pending." + entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pending.yml: " + e.getMessage());
        }
    }

    public void loadPendingItems() {
        File file = new File(plugin.getDataFolder(), "pending.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("pending")) {
            return;
        }

        for (String key : config.getConfigurationSection("pending").getKeys(false)) {
            try {
                UUID playerUUID = UUID.fromString(key);
                ItemStack item = config.getItemStack("pending." + key);
                pendingItems.put(playerUUID, item);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load pending item for " + key + ": " + e.getMessage());
            }
        }
    }
}