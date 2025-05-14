package me.afroninja.afroauction;

import org.bukkit.entity.Player;
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
        Player player = event.getPlayer();
        ItemStack[] pendingItems = pendingItemsManager.getPendingItems(player.getUniqueId());
        if (pendingItems != null && pendingItems.length > 0) {
            player.sendMessage("§aYou have " + pendingItems.length + " pending auction items! Use /auctionclaim to retrieve them.");
        }
    }
}