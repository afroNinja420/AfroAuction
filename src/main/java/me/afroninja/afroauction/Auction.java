package me.afroninja.afroauction;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Auction {
    private final AfroAuction plugin;
    private final Location chestLocation;
    private final ItemStack item;
    private final double startPrice;
    private double currentPrice;
    private UUID highestBidder;
    private int bidCount;
    private long endTime;
    private final BukkitRunnable timerTask;

    public Auction(AfroAuction plugin, Location chestLocation, ItemStack item, double startPrice, int durationSeconds) {
        this.plugin = plugin;
        this.chestLocation = chestLocation;
        this.item = item.clone();
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.bidCount = 0;
        this.endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        this.timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() >= endTime) {
                    endAuction();
                    cancel();
                }
            }
        };
        this.timerTask.runTaskTimer(plugin, 0L, 20L);
    }

    public boolean placeBid(Player player, double bidAmount) {
        if (bidAmount < currentPrice + plugin.getConfig().getDouble("min-bid-increment")) {
            return false;
        }
        if (!plugin.getEconomy().has(player, bidAmount)) {
            return false;
        }

        if (highestBidder != null) {
            Player previousBidder = Bukkit.getPlayer(highestBidder);
            if (previousBidder != null) {
                plugin.getEconomy().depositPlayer(previousBidder, currentPrice);
                previousBidder.sendMessage(ChatColor.YELLOW + "You were outbid on " + item.getType().name() + "!");
            }
        }

        plugin.getEconomy().withdrawPlayer(player, bidAmount);
        currentPrice = bidAmount;
        highestBidder = player.getUniqueId();
        bidCount++;
        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("bid-sound")), 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "Bid of $" + String.format("%.2f", bidAmount) + " placed!");
        return true;
    }

    private void endAuction() {
        plugin.getAuctionManager().removeAuction(chestLocation);
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String message = plugin.getConfig().getString("auction-end-message")
                .replace("%winner%", highestBidder == null ? "No one" : Bukkit.getOfflinePlayer(highestBidder).getName())
                .replace("%item%", itemName)
                .replace("%price%", String.format("%.2f", currentPrice));
        message = ChatColor.translateAlternateColorCodes('&', message);

        if (plugin.getConfig().getBoolean("broadcast-auction-end")) {
            Bukkit.broadcastMessage(message);
        } else if (highestBidder != null) {
            Player winner = Bukkit.getPlayer(highestBidder);
            if (winner != null) {
                winner.sendMessage(message);
            }
        }

        if (highestBidder != null) {
            Player winner = Bukkit.getPlayer(highestBidder);
            if (winner != null) {
                Map<Integer, ItemStack> undelivered = winner.getInventory().addItem(item);
                if (undelivered.isEmpty()) {
                    winner.sendMessage(ChatColor.GREEN + "You received " + itemName + " from the auction!");
                } else {
                    plugin.getPendingItemsManager().addPendingItem(highestBidder, item);
                    winner.sendMessage(ChatColor.YELLOW + "Your inventory is full! Use /auctionclaim to receive " + itemName + ".");
                    plugin.getLogger().info("Queued auction item for " + winner.getName() + " due to full inventory.");
                }
            } else {
                plugin.getPendingItemsManager().addPendingItem(highestBidder, item);
                plugin.getLogger().info("Stored auction item for offline winner: " + Bukkit.getOfflinePlayer(highestBidder).getName());
            }
        }
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public int getBidCount() {
        return bidCount;
    }

    public long getEndTime() {
        return endTime;
    }

    public UUID getHighestBidder() {
        return highestBidder;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setHighestBidder(UUID highestBidder) {
        this.highestBidder = highestBidder;
    }

    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }
}