package me.afroninja.afroauction.managers;

import me.afroninja.afroauction.AfroAuction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages pending items for players, allowing storage and retrieval.
 */
public class PendingItemsManager {
    private final AfroAuction plugin;
    private final Map<UUID, ItemStack> pendingItems;

    /**
     * Constructs a new PendingItemsManager instance.
     * @param plugin the AfroAuction plugin instance
     */
    public PendingItemsManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.pendingItems = new HashMap<>();
        loadPendingItems();
    }

    /**
     * Adds a pending item for a player.
     * @param playerUUID the player's UUID
     * @param item the ItemStack to add
     */
    public void addPendingItem(UUID playerUUID, ItemStack item) {
        pendingItems.put(playerUUID, item);
    }

    /**
     * Retrieves a pending item for a player.
     * @param playerUUID the player's UUID
     * @return the pending ItemStack, or null if none
     */
    public ItemStack getPendingItems(UUID playerUUID) {
        return pendingItems.get(playerUUID);
    }

    /**
     * Removes a pending item for a player.
     * @param playerUUID the player's UUID
     * @param item the ItemStack to remove
     */
    public void removePendingItem(UUID playerUUID, ItemStack item) {
        if (pendingItems.get(playerUUID) != null && pendingItems.get(playerUUID).isSimilar(item)) {
            pendingItems.remove(playerUUID);
        }
    }

    /**
     * Gives pending items to a player if their inventory has space.
     * @param playerUUID the player's UUID
     * @param player the player to give items to
     */
    public void givePendingItems(UUID playerUUID, Player player) {
        ItemStack item = pendingItems.get(playerUUID);
        if (item != null && player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
            pendingItems.remove(playerUUID);
        }
    }

    /**
     * Loads pending items from the pending_items.yml file.
     */
    public void loadPendingItems() {
        File pendingItemsFile = new File(plugin.getDataFolder(), "pending_items.yml");
        if (!pendingItemsFile.exists()) {
            try {
                pendingItemsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create pending_items.yml: " + e.getMessage());
            }
            return;
        }

        FileConfiguration pendingItemsConfig = YamlConfiguration.loadConfiguration(pendingItemsFile);
        for (String key : pendingItemsConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(key);
            ItemStack item = pendingItemsConfig.getItemStack(key);
            if (item != null) {
                pendingItems.put(playerUUID, item);
            }
        }
    }

    /**
     * Saves pending items to the pending_items.yml file.
     */
    public void savePendingItems() {
        File pendingItemsFile = new File(plugin.getDataFolder(), "pending_items.yml");
        FileConfiguration pendingItemsConfig = YamlConfiguration.loadConfiguration(pendingItemsFile);
        pendingItemsConfig.set("pending_items", null); // Clear old data
        for (Map.Entry<UUID, ItemStack> entry : pendingItems.entrySet()) {
            pendingItemsConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            pendingItemsConfig.save(pendingItemsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pending_items.yml: " + e.getMessage());
        }
    }
}