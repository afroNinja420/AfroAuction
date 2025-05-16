package me.afroninja.afroauction.managers;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import org.bukkit.Location;

import java.util.Collection;
import java.util.UUID;

public class AuctionManager {
    private final AfroAuction plugin;
    private final Collection<Auction> auctions;

    public AuctionManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.auctions = new java.util.HashSet<>();
    }

    public void addAuction(Auction auction) {
        auctions.add(auction);
    }

    public void removeAuction(Auction auction) {
        auctions.remove(auction);
    }

    public void loadAuctions() {
        // Load from auctions.yml (implement as needed)
    }

    public void saveAuctions() {
        // Save to auctions.yml (implement as needed)
    }

    public long getActiveAuctionsCount(UUID playerUUID) {
        return auctions.stream().filter(auction -> auction.getCreator().equals(playerUUID)).count();
    }

    public boolean isChestInUse(Location location) {
        return auctions.stream().anyMatch(auction -> auction.getChestLocation().equals(location));
    }

    public Auction getAuction(Location location) {
        return auctions.stream()
                .filter(auction -> auction.getChestLocation().equals(location))
                .findFirst()
                .orElse(null);
    }
}