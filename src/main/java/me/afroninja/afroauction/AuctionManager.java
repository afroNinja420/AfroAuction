package me.afroninja.afroauction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
    private final File auctionsFile;
    private final YamlConfiguration auctionsConfig;

    public AuctionManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.auctions = new HashMap<>();
        this.auctionsFile = new File(plugin.getDataFolder(), "auctions.yml");
        this.auctionsConfig = YamlConfiguration.loadConfiguration(auctionsFile);
    }

    public void addAuction(Auction auction) {
        auctions.put(auction.getChestLocation(), auction);
        saveAuctions();
    }

    public Auction getAuction(Location location) {
        return auctions.get(location);
    }

    public void removeAuction(Location location) {
        auctions.remove(location);
        saveAuctions();
    }

    public void saveAuctions() {
        auctionsConfig.set("auctions", null); // Clear existing data
        int index = 0;
        for (Auction auction : auctions.values()) {
            String path = "auctions." + index;
            Location loc = auction.getChestLocation();
            auctionsConfig.set(path + ".world", loc.getWorld().getName());
            auctionsConfig.set(path + ".x", loc.getX());
            auctionsConfig.set(path + ".y", loc.getY());
            auctionsConfig.set(path + ".z", loc.getZ());
            auctionsConfig.set(path + ".item", auction.getItem());
            auctionsConfig.set(path + ".startPrice", auction.getStartPrice());
            auctionsConfig.set(path + ".currentPrice", auction.getCurrentPrice());
            auctionsConfig.set(path + ".bidCount", auction.getBidCount());
            auctionsConfig.set(path + ".endTime", auction.getEndTime());
            if (auction.getHighestBidder() != null) {
                auctionsConfig.set(path + ".highestBidder", auction.getHighestBidder().toString());
            }
            index++;
        }
        try {
            auctionsConfig.save(auctionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save auctions: " + e.getMessage());
        }
    }

    public void loadAuctions() {
        auctions.clear();
        ConfigurationSection auctionsSection = auctionsConfig.getConfigurationSection("auctions");
        if (auctionsSection == null) return;

        for (String key : auctionsSection.getKeys(false)) {
            String path = "auctions." + key;
            String worldName = auctionsConfig.getString(path + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World " + worldName + " not found for auction " + key);
                continue;
            }
            double x = auctionsConfig.getDouble(path + ".x");
            double y = auctionsConfig.getDouble(path + ".y");
            double z = auctionsConfig.getDouble(path + ".z");
            Location location = new Location(world, x, y, z);
            ItemStack item = auctionsConfig.getItemStack(path + ".item");
            double startPrice = auctionsConfig.getDouble(path + ".startPrice");
            int durationSeconds = (int) Math.max(1, (auctionsConfig.getLong(path + ".endTime") - System.currentTimeMillis()) / 1000);
            Auction auction = new Auction(plugin, location, item, startPrice, durationSeconds);
            auction.setCurrentPrice(auctionsConfig.getDouble(path + ".currentPrice"));
            auction.setBidCount(auctionsConfig.getInt(path + ".bidCount"));
            String bidderUuid = auctionsConfig.getString(path + ".highestBidder");
            if (bidderUuid != null) {
                auction.setHighestBidder(UUID.fromString(bidderUuid));
            }
            auctions.put(location, auction);
        }
    }
}