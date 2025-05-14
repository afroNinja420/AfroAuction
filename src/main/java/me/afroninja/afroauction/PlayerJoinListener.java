package me.afroninja.afroauction;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final PendingItemsManager pendingItemsManager;

    public PlayerJoinListener(PendingItemsManager pendingItemsManager) {
        this.pendingItemsManager = pendingItemsManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (pendingItemsManager.getPendingItem(event.getPlayer().getUniqueId()) != null) {
            event.getPlayer().sendMessage("§aYou have pending auction items! Use /auctionclaim to retrieve them.");
        }
    }
}