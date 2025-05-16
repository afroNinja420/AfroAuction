package me.afroninja.afroauction;

import me.afroninja.afroauction.managers.AuctionManager;
import me.afroninja.afroauction.managers.NotificationManager;
import me.afroninja.afroauction.managers.PendingItemsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

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
    private final double startPrice;
    private final long endTime;
    private UUID highestBidder;
    private double highestBid;
    private int bidCount;
    private ArmorStand hologramStand;
    private Item floatingItem;
    private int updateTaskId;

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
        this.startPrice = startPrice;
        this.highestBid = startPrice;
        this.endTime = System.currentTimeMillis() + (duration * 1000);
        this.highestBidder = null;
        this.bidCount = 0;
        createHologram();
        scheduleEnd();
        scheduleHologramUpdate();
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

        double minPercentageIncrement = plugin.getConfig().getDouble("min-bid-percentage-increment", 10.0);
        double minBid = highestBid * (1 + minPercentageIncrement / 100.0);
        if (bidAmount < minBid) {
            bidder.sendMessage(plugin.getMessage("bid-percentage-too-low", "%min_percentage%", String.format("%.2f", minBid)));
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
        bidCount++;
        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
        bidder.sendMessage(plugin.getMessage("bid-placed", "%amount%", String.format("%.2f", bidAmount), "%item%", itemName));
    }

    /**
     * Creates a hologram above the auction chest using an armor stand and floating item.
     */
    private void createHologram() {
        Location hologramLocation = chestLocation.clone().add(0.5, 1.5, 0.5); // Center above chest, 1.5 blocks up
        Location itemLocation = chestLocation.clone().add(0.5, 2.5, 0.5); // Item 2.5 blocks up

        // Spawn armor stand for text
        hologramStand = (ArmorStand) chestLocation.getWorld().spawnEntity(hologramLocation, EntityType.ARMOR_STAND);
        hologramStand.setInvisible(true);
        hologramStand.setGravity(false);
        hologramStand.setCustomNameVisible(true);
        updateHologramText();

        // Spawn floating item
        floatingItem = chestLocation.getWorld().dropItem(itemLocation, item);
        floatingItem.setPickupDelay(Integer.MAX_VALUE); // Prevent pickup
        floatingItem.setVelocity(new Vector(0, 0, 0)); // No initial velocity
        floatingItem.setGravity(false); // Make it float
    }

    /**
     * Updates the hologram text with current auction details.
     */
    private void updateHologramText() {
        if (hologramStand == null) return;
        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
        long timeLeft = (endTime - System.currentTimeMillis()) / 1000;
        String display = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&6" + itemName + "\n" +
                        "&eBid: &f" + String.format("%.2f", highestBid) + "\n" +
                        "&eTime: &f" + timeLeft + "s"
        );
        hologramStand.setCustomName(display);
    }

    /**
     * Schedules a repeating task to update the hologram text every second.
     */
    private void scheduleHologramUpdate() {
        updateTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (System.currentTimeMillis() >= endTime) {
                plugin.getServer().getScheduler().cancelTask(updateTaskId);
                return;
            }
            updateHologramText();
        }, 0L, 20L); // Update every 20 ticks (1 second)
    }

    /**
     * Ends the auction, distributing items and funds accordingly.
     */
    private void endAuction() {
        auctionManager.removeAuction(chestLocation);

        // Remove hologram
        if (hologramStand != null) {
            hologramStand.remove();
            hologramStand = null;
        }
        if (floatingItem != null) {
            floatingItem.remove();
            floatingItem = null;
        }

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
     * Retrieves the starting bid amount.
     * @return the starting bid
     */
    public double getStartPrice() {
        return startPrice;
    }

    /**
     * Retrieves the highest bid amount.
     * @return the highest bid
     */
    public double getHighestBid() {
        return highestBid;
    }

    /**
     * Retrieves the number of bids placed.
     * @return the number of bids
     */
    public int getBidCount() {
        return bidCount;
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

    /**
     * Retrieves the location of the chest associated with this auction.
     * @return the Location of the chest
     */
    public Location getChestLocation() {
        return chestLocation;
    }

    /**
     * Retrieves the UUID of the seller.
     * @return the seller's UUID
     */
    public UUID getSellerUUID() {
        return sellerUUID;
    }
}