package me.afroninja.afroauction.listeners;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.managers.PendingItemsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Listens for player join events to deliver pending items.
 */
public class PlayerJoinListener implements Listener {
    private final AfroAuction plugin;
    private final PendingItemsManager pendingItemsManager;

    /**
     * Constructs a new PlayerJoinListener instance.
     * @param plugin the AfroAuction plugin instance
     * @param pendingItemsManager the PendingItemsManager instance
     */
    public PlayerJoinListener(AfroAuction plugin, PendingItemsManager pendingItemsManager) {
        this.plugin = plugin;
        this.pendingItemsManager = pendingItemsManager;
    }

    /**
     * Handles the player join event to deliver pending items.
     * @param event the PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        ItemStack item = pendingItemsManager.getPendingItems(playerUUID);

        if (item != null) {
            if (event.getPlayer().getInventory().firstEmpty() != -1) {
                event.getPlayer().getInventory().addItem(item);
                pendingItemsManager.removePendingItem(playerUUID, item);
                String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
                event.getPlayer().sendMessage(plugin.getMessage("pending-item-received", "%item%", itemName));
            } else {
                event.getPlayer().sendMessage(plugin.getMessage("inventory-full"));
            }
        }
    }
}