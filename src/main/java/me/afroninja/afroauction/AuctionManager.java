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
    private final Map<Location, Auction> auctions;
    private final File auctionFile;
    private final YamlConfiguration auctionConfig;

    public AuctionManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.auctions = new HashMap<>();
        this.auctionFile = new File(plugin.getDataFolder(), "auctions.yml");
        this.auctionConfig = YamlConfiguration.loadConfiguration(auctionFile);
    }

    public void createAuction(UUID creator, Location chestLocation, ItemStack item, double startPrice, int durationSeconds) {
        Auction auction = new Auction(plugin, creator, chestLocation, item, startPrice, durationSeconds);
        auctions.put(chestLocation, auction);
        saveAuctions();
    }

    public Auction getAuction(Location chestLocation) {
        return auctions.get(chestLocation);
    }

    public void removeAuction(Location chestLocation) {
        auctions.remove(chestLocation);
        saveAuctions();
    }

    public void saveAuctions() {
        auctionConfig.set("auctions", null);
        int index = 0;
        for (Map.Entry<Location, Auction> entry : auctions.entrySet()) {
            String path = "auctions." + index;
            auctionConfig.set(path + ".creator", entry.getValue().getCreator().toString());
            auctionConfig.set(path + ".chestLocation", entry.getKey());
            auctionConfig.set(path + ".item", entry.getValue().getItem());
            auctionConfig.set(path + ".startPrice", entry.getValue().getStartPrice());
            auctionConfig.set(path + ".currentPrice", entry.getValue().getCurrentPrice());
            auctionConfig.set(path + ".highestBidder", entry.getValue().getHighestBidder() != null ? entry.getValue().getHighestBidder().toString() : null);
            auctionConfig.set(path + ".bidCount", entry.getValue().getBidCount());
            auctionConfig.set(path + ".endTime", entry.getValue().getEndTime());
            index++;
        }
        try {
            auctionConfig.save(auctionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save auctions: " + e.getMessage());
        }
    }

    public void loadAuctions() {
        auctions.clear();
        ConfigurationSection auctionSection = auctionConfig.getConfigurationSection("auctions");
        if (auctionSection == null) return;

        for (String key : auctionSection.getKeys(false)) {
            String path = "auctions." + key;
            UUID creator = UUID.fromString(auctionConfig.getString(path + ".creator"));
            Location chestLocation = (Location) auctionConfig.get(path + ".chestLocation");
            ItemStack item = auctionConfig.getItemStack(path + ".item");
            double startPrice = auctionConfig.getDouble(path + ".startPrice");
            double currentPrice = auctionConfig.getDouble(path + ".currentPrice");
            String highestBidderStr = auctionConfig.getString(path + ".highestBidder");
            UUID highestBidder = highestBidderStr != null ? UUID.fromString(highestBidderStr) : null;
            int bidCount = auctionConfig.getInt(path + ".bidCount");
            long endTime = auctionConfig.getLong(path + ".endTime");

            if (item == null || chestLocation == null) {
                plugin.getLogger().warning("Skipping invalid auction at index " + key);
                continue;
            }

            Auction auction = new Auction(plugin, creator, chestLocation, item, startPrice, 0);
            auction.setCurrentPrice(currentPrice);
            auction.setHighestBidder(highestBidder);
            auction.setBidCount(bidCount);
            auction.setEndTime(endTime);
            auctions.put(chestLocation, auction);
        }
    }

    private void setEndTime(Auction auction, long endTime) {
        try {
            java.lang.reflect.Field endTimeField = Auction.class.getDeclaredField("endTime");
            endTimeField.setAccessible(true);
            endTimeField.setLong(auction, endTime);
            endTimeField.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            plugin.getLogger().severe("Failed to set endTime for auction: " + e.getMessage());
        }
    }
}