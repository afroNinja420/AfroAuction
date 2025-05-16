package me.afroninja.afroauction.listeners;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import me.afroninja.afroauction.managers.AuctionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listens for chat messages to allow players to place bids using a !bid command.
 */
public class ChatBidListener implements Listener {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;

    /**
     * Constructs a new ChatBidListener instance.
     * @param plugin the AfroAuction plugin instance
     * @param auctionManager the AuctionManager instance
     */
    public ChatBidListener(AfroAuction plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    /**
     * Handles chat events to process bid commands.
     * @param event the AsyncPlayerChatEvent
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.startsWith("!bid")) {
            event.setCancelled(true);
            String[] parts = message.split(" ");
            if (parts.length != 2) {
                player.sendMessage(plugin.getMessage("invalid-bid-format"));
                return;
            }

            try {
                double bidAmount = Double.parseDouble(parts[1]);
                Auction auction = auctionManager.getAuction(player.getTargetBlock(null, 5).getLocation());
                if (auction == null) {
                    player.sendMessage(plugin.getMessage("no-auction-target"));
                    return;
                }
                auction.placeBid(player, bidAmount);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("invalid-price-format"));
            }
        }
    }
}