package me.afroninja.afroauction.managers;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import org.bukkit.Location;

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
     * Loads auctions from storage (e.g., a file or database).
     */
    public void loadAuctions() {
        // Placeholder: Implement loading logic once AfroAuction.java is shared
        plugin.getLogger().info("Loading auctions...");
        // This method will be expanded based on AfroAuction.java's implementation
    }

    /**
     * Saves auctions to storage (e.g., a file or database).
     */
    public void saveAuctions() {
        // Placeholder: Implement saving logic once AfroAuction.java is shared
        plugin.getLogger().info("Saving auctions...");
        // This method will be expanded based on AfroAuction.java's implementation
    }
}