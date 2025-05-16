package me.afroninja.afroauction.listeners;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import me.afroninja.afroauction.AuctionGUI;
import me.afroninja.afroauction.managers.AuctionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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

        // Check if the player is awaiting a bid from any GUI
        for (Auction auction : auctionManager.getActiveAuctions().values()) {
            AuctionGUI gui = new AuctionGUI(plugin, auction);
            if (gui.isPlayerAwaitingBid(playerUUID)) {
                event.setCancelled(true);
                gui.removePlayerFromAwaitingBid(playerUUID);

                String message = event.getMessage().trim();
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(plugin.getMessage("bid-cancelled"));
                    return;
                }

                double bidAmount;
                try {
                    bidAmount = Double.parseDouble(message);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getMessage("invalid-bid"));
                    return;
                }

                auction.placeBid(player, bidAmount);
                return;
            }
        }
    }
}