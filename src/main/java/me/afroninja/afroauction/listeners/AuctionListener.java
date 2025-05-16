package me.afroninja.afroauction.listeners;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import me.afroninja.afroauction.AuctionGUI;
import me.afroninja.afroauction.managers.AuctionManager;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listens for player interactions with auction chests to open the auction GUI.
 */
public class AuctionListener implements Listener {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;

    /**
     * Constructs a new AuctionListener instance.
     * @param plugin the AfroAuction plugin instance
     * @param auctionManager the AuctionManager instance
     */
    public AuctionListener(AfroAuction plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    /**
     * Handles player interactions with blocks to open the auction GUI if the block is an auction chest.
     * @param event the PlayerInteractEvent
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        if (block.getState() instanceof Chest) {
            Auction auction = auctionManager.getAuction(block.getLocation());
            if (auction != null) {
                event.setCancelled(true);
                new AuctionGUI(plugin, auction).openInventory(event.getPlayer());
            }
        }
    }
}