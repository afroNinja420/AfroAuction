package me.afroninja.afroauction.listeners;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import me.afroninja.afroauction.gui.AuctionGUI;
import me.afroninja.afroauction.managers.AuctionManager;
import me.afroninja.afroauction.managers.NotificationManager;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listens for player interactions related to auctions, such as clicking on auction chests.
 */
public class AuctionListener implements Listener {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;
    private final NotificationManager notificationManager;

    /**
     * Constructs a new AuctionListener instance.
     * @param plugin the AfroAuction plugin instance
     * @param auctionManager the AuctionManager instance
     * @param notificationManager the NotificationManager instance
     */
    public AuctionListener(AfroAuction plugin, AuctionManager auctionManager, NotificationManager notificationManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.notificationManager = notificationManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Chest)) return;

        Chest chest = (Chest) block.getState();
        Auction auction = auctionManager.getAuctionByChest(chest.getLocation());
        if (auction == null) return;

        event.setCancelled(true); // Prevent opening the chest

        Player player = event.getPlayer();
        AuctionGUI gui = new AuctionGUI(plugin, auction, player);
        gui.openInventory();
    }
}