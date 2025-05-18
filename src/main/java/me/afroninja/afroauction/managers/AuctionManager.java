package me.afroninja.afroauction.managers;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages all active auctions in the AfroAuction plugin.
 */
public class AuctionManager {
    private final AfroAuction plugin;
    private final Map<Location, Auction> activeAuctions;
    private File auctionFile;
    private FileConfiguration auctionConfig;

    /**
     * Constructs a new AuctionManager instance.
     * @param plugin the AfroAuction plugin instance
     */
    public AuctionManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.activeAuctions = new HashMap<>();
        this.auctionFile = new File(plugin.getDataFolder(), "auctions.yml");
        this.auctionConfig = YamlConfiguration.loadConfiguration(auctionFile);
    }

    /**
     * Adds an auction to the active auctions map.
     * @param auction the Auction to add
     */
    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getChestLocation(), auction);
    }

    /**
     * Removes an auction from the active auctions map.
     * @param location the location of the auction's chest
     */
    public void removeAuction(Location location) {
        activeAuctions.remove(location);
    }

    /**
     * Retrieves an auction by its chest location.
     * @param location the location of the auction's chest
     * @return the Auction at the specified location, or null if none exists
     */
    public Auction getAuction(Location location) {
        return activeAuctions.get(location);
    }

    /**
     * Checks if a chest is already in use by an active auction.
     * @param location the location of the chest
     * @return true if the chest is in use, false otherwise
     */
    public boolean isChestInUse(Location location) {
        return activeAuctions.containsKey(location);
    }

    /**
     * Retrieves the number of active auctions for a specific player.
     * @param playerUUID the UUID of the player
     * @return the number of active auctions the player has
     */
    public long getActiveAuctionsCount(UUID playerUUID) {
        return activeAuctions.values().stream()
                .filter(auction -> auction.getSellerUUID().equals(playerUUID))
                .count();
    }

    /**
     * Retrieves all active auctions.
     * @return a map of active auctions
     */
    public Map<Location, Auction> getActiveAuctions() {
        return new HashMap<>(activeAuctions);
    }

    /**
     * Retrieves all auctions created by a specific player.
     * @param playerUUID the UUID of the player
     * @return a map of auctions created by the player
     */
    public Map<Location, Auction> getPlayerAuctions(UUID playerUUID) {
        return activeAuctions.entrySet().stream()
                .filter(entry -> entry.getValue().getSellerUUID().equals(playerUUID))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Loads auctions from auctions.yml.
     */
    public void loadAuctions() {
        if (!auctionFile.exists()) {
            try {
                auctionFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create auctions.yml: " + e.getMessage());
            }
            plugin.getLogger().info("No auctions.yml found, starting fresh.");
            return;
        }

        auctionConfig = YamlConfiguration.loadConfiguration(auctionFile);
        activeAuctions.clear();
        for (String key : auctionConfig.getKeys(false)) {
            try {
                UUID sellerUUID = UUID.fromString(auctionConfig.getString(key + ".sellerUUID"));
                ItemStack item = auctionConfig.getItemStack(key + ".item");
                Location chestLocation = (Location) auctionConfig.get(key + ".chestLocation");
                double startPrice = auctionConfig.getDouble(key + ".startPrice");
                long endTime = auctionConfig.getLong(key + ".endTime");
                double highestBid = auctionConfig.getDouble(key + ".highestBid");
                UUID highestBidder = auctionConfig.getString(key + ".highestBidder") != null ? UUID.fromString(auctionConfig.getString(key + ".highestBidder")) : null;
                int bidCount = auctionConfig.getInt(key + ".bidCount");

                // Skip expired auctions
                if (endTime < System.currentTimeMillis()) {
                    plugin.getLogger().info("Skipping expired auction: " + key);
                    continue;
                }

                Auction auction = new Auction(plugin, sellerUUID, item, chestLocation, startPrice, (endTime - System.currentTimeMillis()) / 1000);
                auction.setHighestBid(highestBid);
                auction.setHighestBidder(highestBidder);
                auction.setBidCount(bidCount);
                activeAuctions.put(chestLocation, auction);
                plugin.getLogger().info("Loaded auction: " + key);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load auction " + key + ": " + e.getMessage());
            }
        }
    }

    /**
     * Saves active auctions to auctions.yml.
     */
    public void saveAuctions() {
        auctionConfig.set("auctions", null); // Clear existing data

        int index = 0;
        for (Map.Entry<Location, Auction> entry : activeAuctions.entrySet()) {
            Auction auction = entry.getValue();
            String path = "auctions." + index;
            auctionConfig.set(path + ".sellerUUID", auction.getSellerUUID().toString());
            auctionConfig.set(path + ".item", auction.getItem());
            auctionConfig.set(path + ".chestLocation", auction.getChestLocation());
            auctionConfig.set(path + ".startPrice", auction.getStartPrice());
            auctionConfig.set(path + ".endTime", auction.getEndTime());
            auctionConfig.set(path + ".highestBid", auction.getHighestBid());
            auctionConfig.set(path + ".highestBidder", auction.getHighestBidder() != null ? auction.getHighestBidder().toString() : null);
            auctionConfig.set(path + ".bidCount", auction.getBidCount());
            index++;
        }

        try {
            auctionConfig.save(auctionFile);
            plugin.getLogger().info("Saved " + activeAuctions.size() + " auctions to auctions.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save auctions: " + e.getMessage());
        }
    }
}