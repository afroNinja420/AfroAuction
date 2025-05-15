package me.afroninja.afroauction;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PendingItemsListener implements Listener {
    private final AfroAuction plugin;

    public PendingItemsListener(AfroAuction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPendingItemsManager().givePendingItems(event.getPlayer());
    }
}