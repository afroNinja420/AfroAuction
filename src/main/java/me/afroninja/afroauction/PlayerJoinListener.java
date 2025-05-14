package me.afroninja.afroauction;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerJoinListener implements Listener {
    private final PendingItemsManager pendingItemsManager;

    public PlayerJoinListener(PendingItemsManager pendingItemsManager) {
        this.pendingItemsManager = pendingItemsManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var pendingItems = pendingItemsManager.getPendingItems(player.getUniqueId());
        if (!pendingItems.isEmpty()) {
            for (ItemStack item : pendingItems) {
                player.getInventory().addItem(item);
                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
                player.sendMessage(ChatColor.GREEN + "You received " + itemName + " from a won auction!");
            }
            pendingItemsManager.clearPendingItems(player.getUniqueId());
        }
    }
}