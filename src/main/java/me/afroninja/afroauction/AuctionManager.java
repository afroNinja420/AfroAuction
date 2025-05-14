package me.afroninja.afroauction;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AuctionManager {
    private final AfroAuction plugin;
    private final List<Auction> auctions;
    private final Map<UUID, Long> lastAuctionTimes;

    public AuctionManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.auctions = new ArrayList<>();
        this.lastAuctionTimes = new HashMap<>();
    }

    public void addAuction(Auction auction) {
        auctions.add(auction);
        lastAuctionTimes.put(auction.getCreator(), System.currentTimeMillis());
        saveAuctions();
    }

    public void removeAuction(Auction auction) {
        auctions.remove(auction);
        saveAuctions();
    }

    public Auction getAuction(Location location) {
        for (Auction auction : auctions) {
            Location auctionLoc = auction.getChestLocation();
            if (auctionLoc.getWorld().equals(location.getWorld()) &&
                    auctionLoc.getBlockX() == location.getBlockX() &&
                    auctionLoc.getBlockY() == location.getBlockY() &&
                    auctionLoc.getBlockZ() == location.getBlockZ()) {
                return auction;
            }
        }
        return null;
    }

    public List<Auction> getActiveAuctions(UUID playerUUID) {
        List<Auction> playerAuctions = new ArrayList<>();
        for (Auction auction : auctions) {
            if (auction.getCreator().equals(playerUUID)) {
                playerAuctions.add(auction);
            }
        }
        return playerAuctions;
    }

    public long getLastAuctionTime(UUID playerUUID) {
        return lastAuctionTimes.getOrDefault(playerUUID, 0L);
    }

    public void saveAuctions() {
        File file = new File(plugin.getDataFolder(), "auctions.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("auctions", null);
        for (int i = 0; i < auctions.size(); i++) {
            Auction auction = auctions.get(i);
            config.set("auctions." + i + ".creator", auction.getCreator().toString());
            config.set("auctions." + i + ".item", auction.getItem());
            config.set("auctions." + i + ".chestLocation", auction.getChestLocation());
            config.set("auctions." + i + ".currentBid", auction.getCurrentBid());
            if (auction.getHighestBidder() != null) {
                config.set("auctions." + i + ".highestBidder", auction.getHighestBidder().toString());
            }
            config.set("auctions." + i + ".endTime", auction.getEndTime());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save auctions.yml: " + e.getMessage());
        }
    }

    public void loadAuctions() {
        File file = new File(plugin.getDataFolder(), "auctions.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("auctions")) {
            return;
        }

        for (String key : config.getConfigurationSection("auctions").getKeys(false)) {
            try {
                UUID creator = UUID.fromString(config.getString("auctions." + key + ".creator"));
                ItemStack item = config.getItemStack("auctions." + key + ".item");
                Location chestLocation = (Location) config.get("auctions." + key + ".chestLocation");
                double currentBid = config.getDouble("auctions." + key + ".currentBid");
                UUID highestBidder = config.contains("auctions." + key + ".highestBidder") ? UUID.fromString(config.getString("auctions." + key + ".highestBidder")) : null;
                long endTime = config.getLong("auctions." + key + ".endTime");

                Auction auction = new Auction(plugin, creator, item, chestLocation, currentBid, (endTime - System.currentTimeMillis()) / 1000);
                if (highestBidder != null) {
                    auction.placeBid(plugin.getServer().getPlayer(highestBidder), currentBid);
                }
                auctions.add(auction);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load auction " + key + ": " + e.getMessage());
            }
        }
    }
}