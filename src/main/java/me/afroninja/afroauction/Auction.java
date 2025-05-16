package me.afroninja.afroauction;

import me.afroninja.afroauction.managers.AuctionManager;
import me.afroninja.afroauction.managers.NotificationManager;
import me.afroninja.afroauction.managers.PendingItemsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents an auction instance, managing bidding, scheduling, and ending logic.
 */
public class Auction {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;
    private final NotificationManager notificationManager;
    private final PendingItemsManager pendingItemsManager;
    private final Economy economy;
    private final UUID sellerUUID;
    private final ItemStack item;
    private final Location chestLocation;
    private final long endTime;
    private UUID highestBidder;
    private double highestBid;

    /**
     * Constructs a new Auction instance.
     * @param plugin the AfroAuction plugin instance
     * @param sellerUUID the UUID of the auction seller
     * @param item the item being auctioned
     * @param chestLocation the location of the chest associated with the auction
     * @param startPrice the starting bid price
     * @param duration the duration of the auction in seconds
     */
    public Auction(AfroAuction plugin, UUID sellerUUID, ItemStack item, Location chestLocation, double startPrice, long duration) {
        this.plugin = plugin;
        this.auctionManager = plugin.getAuctionManager();
        this.notificationManager = plugin.getNotificationManager();
        this.pendingItemsManager = plugin.getPendingItemsManager();
        this.economy = plugin.getEconomy();
        this.sellerUUID = sellerUUID;
        this.item = item;
        this.chestLocation = chestLocation;
        this.highestBid = startPrice;
        this.endTime = System.currentTimeMillis() + (duration * 1000);
        this.highestBidder = null;
        scheduleEnd();
    }

    /**
     * Places a bid on the auction if valid.
     * @param bidder the player placing the bid
     * @param bidAmount the bid amount
     */
    public void placeBid(Player bidder, double bidAmount) {
        if (System.currentTimeMillis() >= endTime) {
            bidder.sendMessage(plugin.getMessage("auction-ended"));
            return;
        }

        if (bidAmount <= highestBid) {
            bidder.sendMessage(plugin.getMessage("bid-too-low", "%current_bid%", String.format("%.2f", highestBid)));
            return;
        }

        double minBidIncrement = plugin.getConfig().getDouble("min-bid-increment", 1.0);
        if (bidAmount < highestBid + minBidIncrement) {
            bidder.sendMessage(plugin.getMessage("bid-increment-too-low", "%min_increment%", String.format("%.2f", minBidIncrement)));
            return;
        }

        if (!economy.has(bidder, bidAmount)) {
            bidder.sendMessage(plugin.getMessage("insufficient-funds"));
            return;
        }

        // Refund the previous highest bidder
        if (highestBidder != null) {
            Player previousBidder = plugin.getServer().getPlayer(highestBidder);
            if (previousBidder != null) {
                economy.depositPlayer(previousBidder, highestBid);
                String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
                notificationManager.sendOutbidMessage(highestBidder, itemName);
            }
        }

        // Deduct the bid amount from the new bidder
        economy.withdrawPlayer(bidder, bidAmount);
        highestBidder = bidder.getUniqueId();
        highestBid = bidAmount;
        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
        bidder.sendMessage(plugin.getMessage("bid-placed", "%amount%", String.format("%.2f", bidAmount), "%item%", itemName));
    }

    /**
     * Ends the auction, distributing items and funds accordingly.
     */
    private void endAuction() {
        auctionManager.removeAuction(chestLocation);

        if (highestBidder == null) {
            pendingItemsManager.addPendingItem(sellerUUID, item);
            Player seller = plugin.getServer().getPlayer(sellerUUID);
            if (seller != null) {
                String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
                seller.sendMessage(plugin.getMessage("auction-no-bids", "%item%", itemName));
            }
        } else {
            Player winner = plugin.getServer().getPlayer(highestBidder);
            if (winner != null) {
                pendingItemsManager.addPendingItem(highestBidder, item);
                String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
                winner.sendMessage(plugin.getMessage("auction-won", "%item%", itemName, "%amount%", String.format("%.2f", highestBid)));
            }

            Player seller = plugin.getServer().getPlayer(sellerUUID);
            if (seller != null) {
                economy.depositPlayer(seller, highestBid);
                String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
                seller.sendMessage(plugin.getMessage("auction-sold", "%item%", itemName, "%amount%", String.format("%.2f", highestBid)));
            }
        }
    }

    /**
     * Schedules the auction end task.
     */
    private void scheduleEnd() {
        long delay = endTime - System.currentTimeMillis();
        if (delay <= 0) {
            endAuction();
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, this::endAuction, delay / 50);
    }

    /**
     * Retrieves the auctioned item.
     * @return the ItemStack being auctioned
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Retrieves the highest bid amount.
     * @return the highest bid
     */
    public double getHighestBid() {
        return highestBid;
    }

    /**
     * Retrieves the UUID of the highest bidder.
     * @return the highest bidder's UUID, or null if no bids
     */
    public UUID getHighestBidder() {
        return highestBidder;
    }

    /**
     * Retrieves the auction end time.
     * @return the end time in milliseconds
     */
    public long getEndTime() {
        return endTime;
    }
}