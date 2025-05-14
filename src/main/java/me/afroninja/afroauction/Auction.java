package me.afroninja.afroauction;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class Auction {
    private final AfroAuction plugin;
    private final UUID creator;
    private final ItemStack item;
    private final Location chestLocation;
    private double currentBid;
    private UUID highestBidder;
    private final long endTime;
    private Hologram hologram;
    private final BukkitRunnable updateTask;

    public Auction(AfroAuction plugin, UUID creator, ItemStack item, Location chestLocation, double startingBid, long duration) {
        this.plugin = plugin;
        this.creator = creator;
        this.item = item.clone();
        this.chestLocation = chestLocation;
        this.currentBid = startingBid;
        this.highestBidder = null;
        this.endTime = System.currentTimeMillis() + (duration * 1000);

        createHologram();
        updateHologram();

        this.updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateHologram();
                if (System.currentTimeMillis() >= endTime) {
                    endAuction();
                    cancel();
                }
            }
        };
        updateTask.runTaskTimer(plugin, 0L, plugin.getConfig().getLong("hologram-update-interval", 20L));
    }

    private void createHologram() {
        double baseHeight = plugin.getConfig().getDouble("hologram-base-height", 1.7);
        double lineSpacing = plugin.getConfig().getDouble("hologram-line-spacing", 0.25);
        double itemOffset = plugin.getConfig().getDouble("hologram-item-offset", 0.25);

        hologram = HologramsAPI.createHologram(plugin, chestLocation.clone().add(0.5, baseHeight, 0.5));
        hologram.appendTextLine("");
        hologram.appendTextLine("");
        hologram.appendTextLine("");
        hologram.appendItemLine(item.clone());

        for (int i = 0; i < 3; i++) {
            hologram.getLine(i).setLocation(hologram.getLocation().clone().add(0, i * lineSpacing, 0));
        }
        hologram.getLine(3).setLocation(hologram.getLocation().clone().add(0, 3 * lineSpacing + itemOffset, 0));
    }

    private void updateHologram() {
        if (hologram == null || hologram.isDeleted()) return;

        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String bidLine = highestBidder == null
                ? plugin.getMessage("hologram-bid-starting", "%price%", String.format("%.2f", currentBid))
                : plugin.getMessage("hologram-bid-highest", "%price%", String.format("%.2f", currentBid));
        String timeLine = plugin.getMessage("hologram-time", "%time%", formatTimeRemaining());
        String itemLine = plugin.getMessage("hologram-item", "%item%", itemName);

        hologram.getLine(0).removeLine();
        hologram.insertTextLine(0, bidLine);
        hologram.getLine(1).removeLine();
        hologram.insertTextLine(1, timeLine);
        hologram.getLine(2).removeLine();
        hologram.insertTextLine(2, itemLine);
    }

    private String formatTimeRemaining() {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        if (remaining <= 0) return "0s";

        long days = remaining / (24 * 3600);
        remaining %= (24 * 3600);
        long hours = remaining / 3600;
        remaining %= 3600;
        long minutes = remaining / 60;
        long seconds = remaining % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d, ");
        if (hours > 0 || days > 0) sb.append(hours).append("h, ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m, ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    public boolean placeBid(Player player, double bid) {
        if (!plugin.getConfig().getBoolean("allow-self-bidding", false) && creator.equals(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("self-bid-disallowed"));
            return false;
        }

        double minBid = currentBid + plugin.getConfig().getDouble("min-bid-increment", 1.0);
        if (bid < minBid) {
            player.sendMessage(plugin.getMessage("bid-too-low", "%min_bid%", String.format("%.2f", minBid)));
            return false;
        }

        if (!plugin.getEconomy().has(player, bid)) {
            player.sendMessage(plugin.getMessage("insufficient-funds", "%bid%", String.format("%.2f", bid)));
            return false;
        }

        if (highestBidder != null) {
            Player previousBidder = plugin.getServer().getPlayer(highestBidder);
            if (previousBidder != null) {
                String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
                previousBidder.sendMessage(plugin.getMessage("outbid", "%item%", itemName));
                plugin.getEconomy().depositPlayer(previousBidder, currentBid);
            }
        }

        plugin.getEconomy().withdrawPlayer(player, bid);
        currentBid = bid;
        highestBidder = player.getUniqueId();

        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        player.sendMessage(plugin.getMessage("bid-placed", "%bid%", String.format("%.2f", bid), "%item%", itemName));
        player.playSound(player.getLocation(), plugin.getConfig().getString("bid-sound", "ENTITY_EXPERIENCE_ORB_PICKUP"), 1.0f, 1.0f);

        updateHologram();
        return true;
    }

    public void endAuction() {
        updateTask.cancel();
        if (hologram != null) {
            hologram.delete();
        }

        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        if (highestBidder == null) {
            plugin.getPendingItemsManager().addPendingItem(creator, item);
            Player creatorPlayer = plugin.getServer().getPlayer(creator);
            if (creatorPlayer != null) {
                creatorPlayer.sendMessage(plugin.getMessage("no-bids", "%item%", itemName));
            }
        } else {
            Player winner = plugin.getServer().getPlayer(highestBidder);
            if (winner != null && winner.getInventory().firstEmpty() != -1) {
                winner.getInventory().addItem(item);
                winner.sendMessage(plugin.getMessage("winner-received", "%item%", itemName));
            } else {
                plugin.getPendingItemsManager().addPendingItem(highestBidder, item);
                if (winner != null) {
                    winner.sendMessage(plugin.getMessage("winner-inventory-full", "%item%", itemName));
                }
            }

            Player creatorPlayer = plugin.getServer().getPlayer(creator);
            if (creatorPlayer != null) {
                plugin.getEconomy().depositPlayer(creatorPlayer, currentBid);
                String winnerName = winner != null ? winner.getName() : plugin.getServer().getOfflinePlayer(highestBidder).getName();
                creatorPlayer.sendMessage(plugin.getMessage("creator-paid", "%item%", itemName, "%winner%", winnerName, "%price%", String.format("%.2f", currentBid)));
            }

            if (plugin.getConfig().getBoolean("broadcast-auction-end", true)) {
                String winnerName = winner != null ? winner.getName() : plugin.getServer().getOfflinePlayer(highestBidder).getName();
                plugin.getServer().broadcastMessage(plugin.getMessage("auction-ended", "%item%", itemName, "%winner%", winnerName, "%price%", String.format("%.2f", currentBid)));
            }
        }

        plugin.getAuctionManager().removeAuction(this);
    }

    public UUID getCreator() {
        return creator;
    }

    public ItemStack getItem() {
        return item;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public UUID getHighestBidder() {
        return highestBidder;
    }

    public long getEndTime() {
        return endTime;
    }
}