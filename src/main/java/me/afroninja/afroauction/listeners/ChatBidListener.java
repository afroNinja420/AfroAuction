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
 * Listens for chat messages to handle bid inputs after clicking the bid button in the GUI.
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
        if (auction != null) {
            event.setCancelled(true);

            String message = event.getMessage().trim();
            if (message.equalsIgnoreCase("cancel")) {
                plugin.removePlayerFromAwaitingBid(playerUUID);
                player.sendMessage(plugin.getMessage("bid-cancelled"));
                return;
            }

            double bidAmount;
            try {
                bidAmount = Double.parseDouble(message);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("invalid-bid"));
                // Prompt to try again
                double minBid = auction.getHighestBidder() == null
                        ? auction.getStartPrice()
                        : auction.getHighestBid() * (1 + plugin.getConfig().getDouble("min-bid-percentage-increment", 10.0) / 100.0);
                player.sendMessage(plugin.getMessage("bid-prompt", "%min_bid%", String.format("%.2f", minBid)));
                return;
            }

            boolean bidSuccessful = auction.placeBid(player, bidAmount);
            if (bidSuccessful) {
                plugin.removePlayerFromAwaitingBid(playerUUID);
            } else {
                // Bid failed (e.g., too low or insufficient funds), prompt to try again
                double minBid = auction.getHighestBidder() == null
                        ? auction.getStartPrice()
                        : auction.getHighestBid() * (1 + plugin.getConfig().getDouble("min-bid-percentage-increment", 10.0) / 100.0);
                player.sendMessage(plugin.getMessage("bid-prompt", "%min_bid%", String.format("%.2f", minBid)));
            }
        }
    }
}