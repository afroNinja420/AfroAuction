package me.afroninja.afroauction;

import org.bukkit.ChatColor;
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
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Auction auction = auctionManager.getAuction(event.getClickedBlock().getLocation());
        if (auction != null) {
            event.setCancelled(true); // Prevent chest opening
            AuctionGUI gui = new AuctionGUI(plugin, auction, event.getPlayer());
            gui.open();
        }
    }
}