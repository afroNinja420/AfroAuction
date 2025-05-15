package me.afroninja.afroauction;

import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class AuctionListener implements Listener {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;

    public AuctionListener(AfroAuction plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Location location = event.getClickedBlock().getLocation();
        if (!(event.getClickedBlock().getState() instanceof Chest)) return;

        Auction auction = auctionManager.getAuction(location);
        if (auction != null) {
            // Handle auction interaction (e.g., open GUI)
            event.setCancelled(true);
            // Add GUI opening logic here if needed
        }
    }
}