package me.afroninja.afroauction;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Auction {
    private final AfroAuction plugin;
    private final UUID creator;
    private final Location chestLocation;
    private final ItemStack item;
    private final double startPrice;
    private double currentPrice;
    private UUID highestBidder;
    private int bidCount;
    private long endTime;
    private final BukkitRunnable timerTask;
    private ArmorStand infoHologram;
    private ArmorStand itemHologram;
    private Item floatingItem;

    public Auction(AfroAuction plugin, UUID creator, Location chestLocation, ItemStack item, double startPrice, int durationSeconds) {
        this.plugin = plugin;
        this.creator = creator;
        this.chestLocation = chestLocation;
        this.item = item.clone();
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.bidCount = 0;
        this.endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        spawnHolograms();
        this.timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateHolograms();
                if (System.currentTimeMillis() >= endTime) {
                    endAuction();
                    cancel();
                }
            }
        };
        this.timerTask.runTaskTimer(plugin, 0L, plugin.getConfig().getLong("hologram-update-interval", 20));
    }

    public boolean placeBid(Player player, double bidAmount) {
        double minBid = currentPrice + plugin.getConfig().getDouble("min-bid-increment");
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        if (bidAmount < minBid) {
            player.sendMessage(getMessage("bid-too-low", Map.of("min_bid", String.format("%.2f", minBid))));
            return false;
        }
        if (!plugin.getEconomy().has(player, bidAmount)) {
            player.sendMessage(getMessage("insufficient-funds", Map.of("bid", String.format("%.2f", bidAmount))));
            return false;
        }
        if (!plugin.getConfig().getBoolean("allow-self-bidding") && player.getUniqueId().equals(creator)) {
            player.sendMessage(getMessage("self-bid-disallowed", Map.of()));
            return false;
        }

        if (highestBidder != null) {
            Player previousBidder = Bukkit.getPlayer(highestBidder);
            if (previousBidder != null) {
                plugin.getEconomy().depositPlayer(previousBidder, currentPrice);
                previousBidder.sendMessage(getMessage("outbid", Map.of("item", itemName)));
            }
        }

        plugin.getEconomy().withdrawPlayer(player, bidAmount);
        currentPrice = bidAmount;
        highestBidder = player.getUniqueId();
        bidCount++;
        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("bid-sound")), 1.0f, 1.0f);
        player.sendMessage(getMessage("bid-placed", Map.of(
                "bid", String.format("%.2f", bidAmount),
                "item", itemName
        )));
        updateHolograms();
        return true;
    }

    private void endAuction() {
        removeHolograms();
        plugin.getAuctionManager().removeAuction(chestLocation);
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String winnerName = highestBidder == null ? "No one" : Bukkit.getOfflinePlayer(highestBidder).getName();
        String message = getMessage("auction-ended", Map.of(
                "winner", winnerName,
                "item", itemName,
                "price", String.format("%.2f", currentPrice)
        ));

        if (plugin.getConfig().getBoolean("broadcast-auction-end")) {
            Bukkit.broadcastMessage(message);
        } else if (highestBidder != null) {
            Player winner = Bukkit.getPlayer(highestBidder);
            if (winner != null) {
                winner.sendMessage(message);
            }
        }

        // Handle winner (if any)
        if (highestBidder != null) {
            Player winner = Bukkit.getPlayer(highestBidder);
            if (winner != null) {
                Map<Integer, ItemStack> undelivered = winner.getInventory().addItem(item);
                if (undelivered.isEmpty()) {
                    winner.sendMessage(getMessage("winner-received", Map.of("item", itemName)));
                } else {
                    plugin.getPendingItemsManager().addPendingItem(highestBidder, item);
                    winner.sendMessage(getMessage("winner-inventory-full", Map.of("item", itemName)));
                    plugin.getLogger().info("Queued auction item for " + winner.getName() + " due to full inventory.");
                }
            } else {
                plugin.getPendingItemsManager().addPendingItem(highestBidder, item);
                plugin.getLogger().info("Stored auction item for offline winner: " + Bukkit.getOfflinePlayer(highestBidder).getName());
            }
            // Pay the creator
            plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(creator), currentPrice);
            Player creatorPlayer = Bukkit.getPlayer(creator);
            if (creatorPlayer != null) {
                creatorPlayer.sendMessage(getMessage("creator-paid", Map.of(
                        "item", itemName,
                        "price", String.format("%.2f", currentPrice),
                        "winner", winnerName
                )));
            }
            plugin.getLogger().info("Paid $" + currentPrice + " to creator " + Bukkit.getOfflinePlayer(creator).getName() + " for auction of " + itemName);
        } else {
            // No bids, return item to creator
            plugin.getPendingItemsManager().addPendingItem(creator, item);
            Player creatorPlayer = Bukkit.getPlayer(creator);
            if (creatorPlayer != null) {
                creatorPlayer.sendMessage(getMessage("no-bids", Map.of("item", itemName)));
            }
            plugin.getLogger().info("Returned auction item " + itemName + " to creator " + Bukkit.getOfflinePlayer(creator).getName() + " due to no bids.");
        }
    }

    private void spawnHolograms() {
        double baseHeight = plugin.getConfig().getDouble("hologram-base-height", 1.7);
        double infoOffset = plugin.getConfig().getDouble("hologram-info-offset", 0.5);
        double itemOffset = plugin.getConfig().getDouble("hologram-item-offset", 0.25);
        Location center = chestLocation.clone().add(0.5, baseHeight, 0.5);
        // Info hologram (text)
        infoHologram = (ArmorStand) chestLocation.getWorld().spawnEntity(center.clone().add(0, infoOffset, 0), EntityType.ARMOR_STAND);
        if (infoHologram != null) {
            infoHologram.setInvisible(true);
            infoHologram.setGravity(false);
            infoHologram.setCanPickupItems(false);
            infoHologram.setCustomNameVisible(true);
            infoHologram.setMarker(true);
            plugin.getLogger().info("Spawned info hologram at " + center.clone().add(0, infoOffset, 0));
        } else {
            plugin.getLogger().severe("Failed to spawn info hologram at " + center.clone().add(0, infoOffset, 0));
        }
        // Item hologram (name)
        itemHologram = (ArmorStand) chestLocation.getWorld().spawnEntity(center, EntityType.ARMOR_STAND);
        if (itemHologram != null) {
            itemHologram.setInvisible(true);
            itemHologram.setGravity(false);
            itemHologram.setCanPickupItems(false);
            itemHologram.setCustomNameVisible(true);
            itemHologram.setMarker(true);
            plugin.getLogger().info("Spawned item hologram at " + center);
        } else {
            plugin.getLogger().severe("Failed to spawn item hologram at " + center);
        }
        // Floating item
        floatingItem = chestLocation.getWorld().dropItem(center.clone().add(0, itemOffset, 0), item.clone());
        if (floatingItem != null) {
            floatingItem.setPickupDelay(Integer.MAX_VALUE);
            floatingItem.setVelocity(new Vector(0, 0, 0));
            floatingItem.setCanMobPickup(false);
            plugin.getLogger().info("Spawned floating item at " + center.clone().add(0, itemOffset, 0));
        } else {
            plugin.getLogger().severe("Failed to spawn floating item at " + center.clone().add(0, itemOffset, 0));
        }
        updateHolograms();
    }

    private void updateHolograms() {
        if (infoHologram == null || itemHologram == null || floatingItem == null) {
            plugin.getLogger().warning("Cannot update holograms: one or more entities are null");
            return;
        }
        if (infoHologram.isDead() || itemHologram.isDead() || floatingItem.isDead()) {
            plugin.getLogger().warning("Cannot update holograms: one or more entities are dead");
            return;
        }
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        itemHologram.setCustomName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.hologram-item", "&e%item%").replace("%item%", itemName)));
        infoHologram.setCustomName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.hologram-info", "&aPrice: $%price% | Time: %time%s")
                .replace("%price%", String.format("%.2f", currentPrice))
                .replace("%time%", String.valueOf(getTimeLeftSeconds()))));
        plugin.getLogger().info("Updated holograms: Item=" + itemName + ", Price=$" + currentPrice + ", Time=" + getTimeLeftSeconds() + "s");
    }

    private void removeHolograms() {
        if (infoHologram != null && !infoHologram.isDead()) {
            infoHologram.remove();
            plugin.getLogger().info("Removed info hologram");
        }
        if (itemHologram != null && !itemHologram.isDead()) {
            itemHologram.remove();
            plugin.getLogger().info("Removed item hologram");
        }
        if (floatingItem != null && !floatingItem.isDead()) {
            floatingItem.remove();
            plugin.getLogger().info("Removed floating item");
        }
        infoHologram = null;
        itemHologram = null;
        floatingItem = null;
    }

    private String getMessage(String key, Map<String, String> replacements) {
        String message = plugin.getConfig().getString("messages." + key, "&cMissing message: " + key);
        String originalMessage = message;

        // Find the last color code before %item%
        String lastColorCode = "&f"; // Default to white if no color code
        Pattern colorPattern = Pattern.compile("&[0-9a-fk-or]");
        Matcher matcher = colorPattern.matcher(message);
        while (matcher.find()) {
            if (message.indexOf("%item%", matcher.start()) >= 0) {
                lastColorCode = matcher.group();
            }
        }

        // Replace variables
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            String value = entry.getValue();
            if (entry.getKey().equals("item")) {
                // For %item%, append RESET and the last color code
                value = value + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', lastColorCode);
            }
            message = message.replace(placeholder, value);
        }

        // If no %item% is present, preserve original color handling
        if (!originalMessage.contains("%item%")) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
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

    public long getTimeLeftSeconds() {
        return Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
    }

    public UUID getHighestBidder() {
        return highestBidder;
    }

    public UUID getCreator() {
        return creator;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
        updateHolograms();
    }

    public void setHighestBidder(UUID highestBidder) {
        this.highestBidder = highestBidder;
    }

    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }
}