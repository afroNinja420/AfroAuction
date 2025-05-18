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

import java.util.ArrayList;
import java.util.List;
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
    private List<ArmorStand> hologramStands;
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
        this.hologramStands = new ArrayList<>();
        createHologram();
        scheduleEnd();
        scheduleHologramUpdate();
    }

    /**
     * Places a bid on the auction if valid.
     * @param bidder the player placing the bid
     * @param bidAmount the bid amount
     * @return true if the bid was successful, false otherwise
     */
    public boolean placeBid(Player bidder, double bidAmount) {
        if (System.currentTimeMillis() >= endTime) {
            bidder.sendMessage(plugin.getMessage("auction-ended"));
            return false;
        }

        // Determine the minimum bid
        double minBid;
        if (highestBidder == null) {
            // First bid: must be at least the starting price
            minBid = startPrice;
        } else {
            // Subsequent bids: must be at least 10% higher than the current bid
            double minPercentageIncrement = plugin.getConfig().getDouble("min-bid-percentage-increment", 10.0);
            minBid = highestBid * (1 + minPercentageIncrement / 100.0);
        }

        if (bidAmount < minBid) {
            String messageKey = highestBidder == null ? "bid-too-low" : "bid-percentage-too-low";
            String placeholderKey = highestBidder == null ? "%current_bid%" : "%min_percentage%";
            String placeholderValue = highestBidder == null ? String.format("%.2f", startPrice) : String.format("%.2f", minBid);
            bidder.sendMessage(plugin.getMessage(messageKey, placeholderKey, placeholderValue));
            return false;
        }

        if (!economy.has(bidder, bidAmount)) {
            bidder.sendMessage(plugin.getMessage("insufficient-funds"));
            return false;
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
        return true;
    }

    /**
     * Creates a hologram above the auction chest using multiple armor stands and a floating item.
     */
    private void createHologram() {
        double baseHeight = plugin.getConfig().getDouble("hologram-base-height", 1.0);
        double lineSpacing = plugin.getConfig().getDouble("hologram-line-spacing", 0.25);
        double itemOffset = plugin.getConfig().getDouble("hologram-item-offset", 0.0);

        // Create three armor stands for each line
        Location baseLocation = chestLocation.clone().add(0.5, baseHeight, 0.5); // Center above chest

        // Line 1: Item name (top)
        Location line1Location = baseLocation.clone();
        ArmorStand line1 = (ArmorStand) chestLocation.getWorld().spawnEntity(line1Location, EntityType.ARMOR_STAND);
        setupArmorStand(line1);
        hologramStands.add(line1);

        // Line 2: Bid amount (middle)
        Location line2Location = baseLocation.clone().subtract(0, lineSpacing, 0);
        ArmorStand line2 = (ArmorStand) chestLocation.getWorld().spawnEntity(line2Location, EntityType.ARMOR_STAND);
        setupArmorStand(line2);
        hologramStands.add(line2);

        // Line 3: Time left (bottom)
        Location line3Location = baseLocation.clone().subtract(0, 2 * lineSpacing, 0);
        ArmorStand line3 = (ArmorStand) chestLocation.getWorld().spawnEntity(line3Location, EntityType.ARMOR_STAND);
        setupArmorStand(line3);
        hologramStands.add(line3);

        // Spawn floating item above the topmost line
        Location itemLocation = baseLocation.clone().add(0, itemOffset, 0);
        floatingItem = chestLocation.getWorld().dropItem(itemLocation, item);
        floatingItem.setPickupDelay(Integer.MAX_VALUE); // Prevent pickup
        floatingItem.setVelocity(new Vector(0, 0, 0)); // No initial velocity
        floatingItem.setGravity(false); // Make it float
        floatingItem.setInvulnerable(true); // Prevent despawning

        updateHologramText();
    }

    /**
     * Sets up common properties for an armor stand used in holograms.
     * @param stand the ArmorStand to configure
     */
    private void setupArmorStand(ArmorStand stand) {
        stand.setInvisible(true);
        stand.setGravity(false);
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.setCanPickupItems(false);
        stand.setSmall(true);
    }

    /**
     * Updates the hologram text with current auction details.
     */
    private void updateHologramText() {
        if (hologramStands.isEmpty()) return;
        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
        long timeLeftSeconds = (endTime - System.currentTimeMillis()) / 1000;

        // Line 1: Item name
        hologramStands.get(0).setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6" + itemName));

        // Line 2: Bid amount using custom currency symbol
        String formattedBid = plugin.formatCurrency(highestBid);
        hologramStands.get(1).setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eBid: &f" + formattedBid));

        // Line 3: Time left in concise hh,mm,ss format
        String formattedTime = formatTime(timeLeftSeconds);
        hologramStands.get(2).setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eTime: &f" + formattedTime));
    }

    /**
     * Formats the time left into a concise hh,mm,ss format, showing only the smallest non-zero units.
     * @param seconds the time left in seconds
     * @return the formatted time string (e.g., "5m, 43s" or "43s")
     */
    private String formatTime(long seconds) {
        if (seconds <= 0) return "0s";

        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder timeStr = new StringBuilder();
        if (hours > 0) {
            timeStr.append(hours).append("h");
            if (minutes > 0) {
                timeStr.append(", ").append(minutes).append("m");
            }
        } else if (minutes > 0) {
            timeStr.append(minutes).append("m");
            if (seconds > 0) {
                timeStr.append(", ").append(seconds).append("s");
            }
        } else {
            timeStr.append(seconds).append("s");
        }
        return timeStr.toString();
    }

    /**
     * Schedules a repeating task to update the hologram text.
     */
    private void scheduleHologramUpdate() {
        int updateInterval = plugin.getConfig().getInt("hologram-update-interval", 20);
        updateTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (System.currentTimeMillis() >= endTime) {
                plugin.getServer().getScheduler().cancelTask(updateTaskId);
                return;
            }
            updateHologramText();
        }, 0L, updateInterval);
    }

    /**
     * Ends the auction, distributing items and funds accordingly.
     */
    private void endAuction() {
        auctionManager.removeAuction(chestLocation);

        // Remove hologram
        for (ArmorStand stand : hologramStands) {
            if (stand != null) {
                stand.remove();
            }
        }
        hologramStands.clear();
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
     * Sets the highest bid amount.
     * @param highestBid the new highest bid
     */
    public void setHighestBid(double highestBid) {
        this.highestBid = highestBid;
    }

    /**
     * Retrieves the number of bids placed.
     * @return the number of bids
     */
    public int getBidCount() {
        return bidCount;
    }

    /**
     * Sets the number of bids placed.
     * @param bidCount the new bid count
     */
    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }

    /**
     * Retrieves the UUID of the highest bidder.
     * @return the highest bidder's UUID, or null if no bids
     */
    public UUID getHighestBidder() {
        return highestBidder;
    }

    /**
     * Sets the UUID of the highest bidder.
     * @param highestBidder the new highest bidder's UUID
     */
    public void setHighestBidder(UUID highestBidder) {
        this.highestBidder = highestBidder;
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