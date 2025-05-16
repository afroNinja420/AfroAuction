package me.afroninja.afroauction.managers;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all active auctions, including loading and saving to a file.
 */
public class AuctionManager {
    private final AfroAuction plugin;
    private final Map<Location, Auction> activeAuctions;
    private final Map<Location, UUID> activeChests;

    /**
     * Constructs a new AuctionManager instance.
     * @param plugin the AfroAuction plugin instance
     */
    public AuctionManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.activeAuctions = new HashMap<>();
        this.activeChests = new HashMap<>();
        loadAuctions();
    }

    /**
     * Adds an auction to the manager.
     * @param auction the Auction instance to add
     */
    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getChestLocation(), auction);
        activeChests.put(auction.getChestLocation(), auction.getHighestBidder() != null ? auction.getHighestBidder() : auction.getSellerUUID());
    }

    /**
     * Removes an auction from the manager.
     * @param location the location of the auction chest
     */
    public void removeAuction(Location location) {
        activeAuctions.remove(location);
        activeChests.remove(location);
    }

    /**
     * Retrieves an auction by its chest location.
     * @param location the location of the auction chest
     * @return the Auction instance, or null if not found
     */
    public Auction getAuction(Location location) {
        return activeAuctions.get(location);
    }

    /**
     * Retrieves all active auctions.
     * @return the map of active auctions
     */
    public Map<Location, Auction> getActiveAuctions() {
        return activeAuctions;
    }

    /**
     * Checks if a chest location is in use by an auction.
     * @param location the location to check
     * @return true if the location is in use, false otherwise
     */
    public boolean isChestInUse(Location location) {
        return activeChests.containsKey(location);
    }

    /**
     * Counts the number of active auctions for a player.
     * @param playerUUID the UUID of the player
     * @return the number of active auctions
     */
    public long getActiveAuctionsCount(UUID playerUUID) {
        return activeChests.values().stream().filter(uuid -> uuid.equals(playerUUID)).count();
    }

    /**
     * Loads auctions from the auctions.yml file.
     */
    public void loadAuctions() {
        File auctionsFile = new File(plugin.getDataFolder(), "auctions.yml");
        if (!auctionsFile.exists()) {
            try {
                auctionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create auctions.yml: " + e.getMessage());
            }
            return;
        }

        FileConfiguration auctionsConfig = YamlConfiguration.loadConfiguration(auctionsFile);
        for (String key : auctionsConfig.getKeys(false)) {
            // Load logic here if auctions are saved with serialized data
        }
    }

    /**
     * Saves auctions to the auctions.yml file.
     */
    public void saveAuctions() {
        File auctionsFile = new File(plugin.getDataFolder(), "auctions.yml");
        FileConfiguration auctionsConfig = YamlConfiguration.loadConfiguration(auctionsFile);
        auctionsConfig.set("auctions", null); // Clear old data
        for (Map.Entry<Location, Auction> entry : activeAuctions.entrySet()) {
            // Save logic here if needed
        }
        try {
            auctionsConfig.save(auctionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save auctions.yml: " + e.getMessage());
        }
    }
}