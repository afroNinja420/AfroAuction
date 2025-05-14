package me.afroninja.afroauction;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionManager {
    private final AfroAuction plugin;
    private final Map<Location, Auction> activeAuctions = new HashMap<>();
    private final File auctionsFile;
    private final YamlConfiguration auctionsConfig;

    public AuctionManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.auctionsFile = new File(plugin.getDataFolder(), "auctions.yml");
        this.auctionsConfig = YamlConfiguration.loadConfiguration(auctionsFile);
    }

    public void createAuction(UUID creator, Location chestLocation, ItemStack item, double startPrice, int durationSeconds) {
        Auction auction = new Auction(plugin, creator, chestLocation, item, startPrice, durationSeconds);
        activeAuctions.put(chestLocation, auction);
        saveAuctions();
    }

    public Auction getAuction(Location chestLocation) {
        return activeAuctions.get(chestLocation);
    }

    public void removeAuction(Location chestLocation) {
        activeAuctions.remove(chestLocation);
        saveAuctions();
    }

    public Map<Location, Auction> getActiveAuctions() {
        return new HashMap<>(activeAuctions); // Return a copy to prevent external modification
    }

    public void saveAuctions() {
        auctionsConfig.set("auctions", null); // Clear existing data
        int index = 0;
        for (Map.Entry<Location, Auction> entry : activeAuctions.entrySet()) {
            String path = "auctions." + index;
            auctionsConfig.set(path + ".creator", entry.getValue().getCreator().toString());
            auctionsConfig.set(path + ".location", entry.getKey());
            auctionsConfig.set(path + ".item", entry.getValue().getItem());
            auctionsConfig.set(path + ".startPrice", entry.getValue().getStartPrice());
            auctionsConfig.set(path + ".currentPrice", entry.getValue().getCurrentPrice());
            if (entry.getValue().getHighestBidder() != null) {
                auctionsConfig.set(path + ".highestBidder", entry.getValue().getHighestBidder().toString());
            }
            auctionsConfig.set(path + ".bidCount", entry.getValue().getBidCount());
            auctionsConfig.set(path + ".endTime", entry.getValue().getEndTime());
            index++;
        }
        try {
            auctionsConfig.save(auctionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save auctions: " + e.getMessage());
        }
    }

    public void loadAuctions() {
        activeAuctions.clear();
        ConfigurationSection auctionsSection = auctionsConfig.getConfigurationSection("auctions");
        if (auctionsSection == null) {
            return;
        }
        for (String key : auctionsSection.getKeys(false)) {
            String path = "auctions." + key;
            try {
                UUID creator = UUID.fromString(auctionsConfig.getString(path + ".creator"));
                Location location = (Location) auctionsConfig.get(path + ".location");
                ItemStack item = auctionsConfig.getItemStack(path + ".item");
                double startPrice = auctionsConfig.getDouble(path + ".startPrice");
                double currentPrice = auctionsConfig.getDouble(path + ".currentPrice");
                String highestBidderStr = auctionsConfig.getString(path + ".highestBidder");
                UUID highestBidder = highestBidderStr != null ? UUID.fromString(highestBidderStr) : null;
                int bidCount = auctionsConfig.getInt(path + ".bidCount");
                long endTime = auctionsConfig.getLong(path + ".endTime");

                Auction auction = new Auction(plugin, creator, location, item, startPrice, 0); // Duration set later
                auction.setCurrentPrice(currentPrice);
                auction.setHighestBidder(highestBidder);
                auction.setBidCount(bidCount);
                auction.setEndTime(endTime);
                activeAuctions.put(location, auction);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load auction at " + path + ": " + e.getMessage());
            }
        }
    }
}