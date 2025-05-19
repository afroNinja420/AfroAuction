package me.afroninja.afroauction.managers;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages all active auctions, including creation, retrieval, and persistence.
 */
public class AuctionManager {
    private final AfroAuction plugin;
    private final Map<Location, Auction> activeAuctions;

    /**
     * Constructs a new AuctionManager instance.
     * @param plugin the AfroAuction plugin instance
     */
    public AuctionManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.activeAuctions = new HashMap<>();
    }

    /**
     * Adds an auction to the active auctions map.
     * @param auction the auction to add
     */
    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getChestLocation(), auction);
    }

    /**
     * Removes an auction from the active auctions map.
     * @param chestLocation the location of the chest associated with the auction
     */
    public void removeAuction(Location chestLocation) {
        activeAuctions.remove(chestLocation);
    }

    /**
     * Retrieves an auction by its associated chest location.
     * @param chestLocation the location of the chest
     * @return the Auction at the specified location, or null if none exists
     */
    public Auction getAuctionByChest(Location chestLocation) {
        return activeAuctions.get(chestLocation);
    }

    /**
     * Checks if a chest is currently in use by an active auction.
     * @param chestLocation the location of the chest
     * @return true if the chest is in use, false otherwise
     */
    public boolean isChestInUse(Location chestLocation) {
        return activeAuctions.containsKey(chestLocation);
    }

    /**
     * Retrieves all active auctions.
     * @return a list of all active auctions
     */
    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    /**
     * Retrieves all auctions created by a specific player.
     * @param playerUUID the UUID of the player
     * @return a list of auctions created by the player
     */
    public List<Auction> getPlayerAuctions(UUID playerUUID) {
        return activeAuctions.values().stream()
                .filter(auction -> auction.getSellerUUID().equals(playerUUID))
                .collect(Collectors.toList());
    }

    /**
     * Counts the number of active auctions for a specific player.
     * @param playerUUID the UUID of the player
     * @return the number of active auctions for the player
     */
    public long getActiveAuctionsCount(UUID playerUUID) {
        return activeAuctions.values().stream()
                .filter(auction -> auction.getSellerUUID().equals(playerUUID))
                .count();
    }

    /**
     * Saves all active auctions to a file.
     */
    public void saveAuctions() {
        File file = new File(plugin.getDataFolder(), "auctions.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Clear existing auctions in the file
        config.set("auctions", null);

        // Serialize each auction
        List<Map<String, Object>> auctionList = new ArrayList<>();
        for (Auction auction : activeAuctions.values()) {
            Map<String, Object> auctionData = new HashMap<>();
            auctionData.put("sellerUUID", auction.getSellerUUID().toString());
            auctionData.put("item", auction.getItem().serialize());
            auctionData.put("chestLocation", auction.getChestLocation().serialize());
            auctionData.put("startPrice", auction.getStartPrice());
            auctionData.put("endTime", auction.getEndTime());
            if (auction.getHighestBidder() != null) {
                auctionData.put("highestBidder", auction.getHighestBidder().toString());
            }
            auctionData.put("highestBid", auction.getHighestBid());
            auctionData.put("bidCount", auction.getBidCount());
            auctionList.add(auctionData);
        }

        config.set("auctions", auctionList);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save auctions: " + e.getMessage());
        }
    }

    /**
     * Loads all auctions from a file.
     */
    public void loadAuctions() {
        File file = new File(plugin.getDataFolder(), "auctions.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<String, Object>> auctionList = (List<Map<String, Object>>) config.getList("auctions", new ArrayList<>());

        for (Map<String, Object> auctionData : auctionList) {
            UUID sellerUUID = UUID.fromString((String) auctionData.get("sellerUUID"));
            ItemStack item = ItemStack.deserialize((Map<String, Object>) auctionData.get("item"));
            Location chestLocation = (Location) Location.deserialize((Map<String, Object>) auctionData.get("chestLocation"));
            double startPrice = (double) auctionData.get("startPrice");
            long endTime = (long) auctionData.get("endTime");

            // Check if auction has expired
            if (System.currentTimeMillis() >= endTime) {
                continue; // Skip expired auctions
            }

            Auction auction = new Auction(plugin, sellerUUID, item, chestLocation, startPrice, (endTime - System.currentTimeMillis()) / 1000);
            if (auctionData.containsKey("highestBidder")) {
                auction.setHighestBidder(UUID.fromString((String) auctionData.get("highestBidder")));
            }
            auction.setHighestBid((double) auctionData.get("highestBid"));
            auction.setBidCount((int) auctionData.get("bidCount"));
            activeAuctions.put(chestLocation, auction);
        }
    }
}