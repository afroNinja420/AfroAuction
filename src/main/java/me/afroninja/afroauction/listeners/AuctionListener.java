package me.afroninja.afroauction.listeners;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import me.afroninja.afroauction.gui.AuctionGUI;
import me.afroninja.afroauction.managers.AuctionManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listens for player interactions to handle auction creation and GUI opening.
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !block.getType().toString().contains("CHEST")) return;

        Player player = event.getPlayer();
        Auction auction = auctionManager.getAuction(block.getLocation());

        if (auction != null) {
            event.setCancelled(true);
            AuctionGUI gui = new AuctionGUI(plugin, auction, player);
            gui.openInventory();
        }
    }
}