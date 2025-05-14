package me.afroninja.afroauction;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class AuctionManager {
    private final AfroAuction plugin;
    private final Map<Location, Auction> auctions;

    public AuctionManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.auctions = new HashMap<>();
    }

    public void addAuction(Auction auction) {
        auctions.put(auction.getChestLocation(), auction);
    }

    public Auction getAuction(Location location) {
        return auctions.get(location);
    }

    public void removeAuction(Location location) {
        auctions.remove(location);
    }

    public void saveAuctions() {
        // TODO: Save auctions to YAML for persistence
    }
}