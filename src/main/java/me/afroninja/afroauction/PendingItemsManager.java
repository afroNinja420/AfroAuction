package me.afroninja.afroauction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PendingItemsManager {
    private final AfroAuction plugin;
    private final Map<UUID, ItemStack[]> pendingItems;

    public PendingItemsManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.pendingItems = new HashMap<>();
    }

    public void addPendingItem(UUID playerUUID, ItemStack item) {
        ItemStack[] currentItems = pendingItems.getOrDefault(playerUUID, new ItemStack[0]);
        ItemStack[] newItems = new ItemStack[currentItems.length + 1];
        System.arraycopy(currentItems, 0, newItems, 0, currentItems.length);
        newItems[currentItems.length] = item;
        pendingItems.put(playerUUID, newItems);
        savePendingItems();
    }

    public void removePendingItem(UUID playerUUID, ItemStack itemToRemove) {
        ItemStack[] currentItems = pendingItems.getOrDefault(playerUUID, new ItemStack[0]);
        if (currentItems.length == 0) return;

        List<ItemStack> updatedItems = new ArrayList<>();
        for (ItemStack item : currentItems) {
            if (item != null && !item.equals(itemToRemove)) {
                updatedItems.add(item);
            }
        }

        if (updatedItems.isEmpty()) {
            pendingItems.remove(playerUUID);
        } else {
            pendingItems.put(playerUUID, updatedItems.toArray(new ItemStack[0]));
        }
        savePendingItems();
    }

    public void removePendingItem(UUID playerUUID) {
        pendingItems.remove(playerUUID);
        savePendingItems();
    }

    public ItemStack[] getPendingItems(UUID playerUUID) {
        return pendingItems.getOrDefault(playerUUID, new ItemStack[0]);
    }

    public Map<UUID, ItemStack[]> getPendingItemsMap() {
        return pendingItems;
    }

    public void savePendingItems() {
        File file = new File(plugin.getDataFolder(), "pending-items.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("pending-items", null);
        for (Map.Entry<UUID, ItemStack[]> entry : pendingItems.entrySet()) {
            config.set("pending-items." + entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pending-items.yml: " + e.getMessage());
        }
    }

    public void loadPendingItems() {
        File file = new File(plugin.getDataFolder(), "pending-items.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("pending-items")) {
            return;
        }

        for (String key : config.getConfigurationSection("pending-items").getKeys(false)) {
            try {
                UUID playerUUID = UUID.fromString(key);
                ItemStack[] items = config.getList("pending-items." + key).toArray(new ItemStack[0]);
                pendingItems.put(playerUUID, items);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load pending items for " + key + ": " + e.getMessage());
            }
        }
    }
}