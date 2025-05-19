package me.afroninja.afroauction.listeners;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import me.afroninja.afroauction.managers.AuctionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

/**
 * Listens for chat messages to process bids placed by players.
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

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Auction auction = plugin.getAuctionForPlayer(playerUUID);
        if (auction == null) return;

        event.setCancelled(true);

        try {
            double bidAmount = Double.parseDouble(event.getMessage());
            if (auction.placeBid(player, bidAmount)) {
                plugin.removePlayerFromAwaitingBid(playerUUID);
                player.sendMessage(plugin.getMessage("bid-placed", "%amount%", String.valueOf(bidAmount), "%item%", auction.getItem().getType().name()));
            } else {
                player.sendMessage(plugin.getMessage("bid-too-low", "%current_bid%", String.valueOf(auction.getHighestBid())));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("invalid-price-format"));
            plugin.removePlayerFromAwaitingBid(playerUUID);
        }
    }
}