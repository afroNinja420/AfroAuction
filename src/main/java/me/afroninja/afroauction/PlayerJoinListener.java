package me.afroninja.afroauction;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

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
            Map<ItemStack, Boolean> deliveryStatus = new HashMap<>();
            for (ItemStack item : pendingItems) {
                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
                Map<Integer, ItemStack> undelivered = player.getInventory().addItem(item);
                if (undelivered.isEmpty()) {
                    player.sendMessage(ChatColor.GREEN + "You received " + itemName + " from a won auction!");
                    deliveryStatus.put(item, true);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Your inventory is full! Use /auctionclaim to receive " + itemName + ".");
                    deliveryStatus.put(item, false);
                }
            }
            pendingItemsManager.clearPendingItems(player.getUniqueId());
            deliveryStatus.forEach((item, delivered) -> {
                if (!delivered) {
                    pendingItemsManager.addPendingItem(player.getUniqueId(), item);
                }
            });
        }
    }
}