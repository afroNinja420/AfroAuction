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

    public ItemStack getPendingItems(UUID playerUUID) {
        return pendingItems.get(playerUUID);
    }

    public void removePendingItem(UUID playerUUID, ItemStack item) {
        if (pendingItems.get(playerUUID) != null && pendingItems.get(playerUUID).isSimilar(item)) {
            pendingItems.remove(playerUUID);
        }
    }

    public void givePendingItems(Player player) {
        UUID playerUUID = player.getUniqueId();
        ItemStack item = getPendingItems(playerUUID);
        if (item != null) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(item);
                player.sendMessage(plugin.getMessage("pending-item-received", "%item%", item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name())));
                removePendingItem(playerUUID, item);
            } else {
                player.sendMessage(plugin.getMessage("inventory-full", "%item%", item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name())));
            }
        }
    }
}