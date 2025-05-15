package me.afroninja.afroauction;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        pendingItems.put(playerUUID, item);
    }

    public void givePendingItems(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (pendingItems.containsKey(playerUUID)) {
            ItemStack item = pendingItems.remove(playerUUID);
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(item);
                player.sendMessage(plugin.getMessage("pending-item-received", "%item%", item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name())));
            } else {
                player.sendMessage(plugin.getMessage("inventory-full", "%item%", item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name())));
                addPendingItem(playerUUID, item); // Re-add if inventory is full
            }
        }
    }
}