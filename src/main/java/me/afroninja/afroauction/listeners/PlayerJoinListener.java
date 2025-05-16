package me.afroninja.afroauction.listeners;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.managers.PendingItemsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final AfroAuction plugin;
    private final PendingItemsManager pendingItemsManager;

    public PlayerJoinListener(AfroAuction plugin, PendingItemsManager pendingItemsManager) {
        this.plugin = plugin;
        this.pendingItemsManager = pendingItemsManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        ItemStack item = pendingItemsManager.getPendingItems(playerUUID);

        if (item != null) {
            if (event.getPlayer().getInventory().firstEmpty() != -1) {
                event.getPlayer().getInventory().addItem(item);
                pendingItemsManager.removePendingItem(playerUUID, item);
                event.getPlayer().sendMessage("§aYou have received a pending item: " + (item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name())) + "!");
            } else {
                event.getPlayer().sendMessage("§cYou have a pending item, but your inventory is full! Use /pa claim when you have space.");
            }
        }
    }
}