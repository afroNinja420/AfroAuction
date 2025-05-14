package me.afroninja.afroauction;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Auction {
    private final AfroAuction plugin;
    private final UUID creator;
    private final ItemStack item;
    private final Location chestLocation;
    private double currentBid;
    private UUID highestBidder;
    private final long endTime;
    private final List<Entity> hologramEntities;
    private final BukkitRunnable updateTask;

    public Auction(AfroAuction plugin, UUID creator, ItemStack item, Location chestLocation, double startingBid, long duration) {
        this.plugin = plugin;
        this.creator = creator;
        this.item = item.clone();
        this.chestLocation = chestLocation;
        this.currentBid = startingBid;
        this.highestBidder = null;
        this.endTime = System.currentTimeMillis() + (duration * 1000);
        this.hologramEntities = new ArrayList<>();

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

        Location baseLoc = chestLocation.clone().add(0.5, baseHeight, 0.5);

        // Create three ArmorStands for text lines (item name, time, bid)
        for (int i = 0; i < 3; i++) {
            Location lineLoc = baseLoc.clone().add(0, i * lineSpacing, 0);
            ArmorStand stand = (ArmorStand) chestLocation.getWorld().spawnEntity(lineLoc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomNameVisible(true);
            stand.setMarker(true);
            hologramEntities.add(stand);
        }

        // Create Item entity for floating item
        Location itemLoc = baseLoc.clone().add(0, 3 * lineSpacing + itemOffset, 0);
        Item itemEntity = chestLocation.getWorld().dropItem(itemLoc, item.clone());
        itemEntity.setPickupDelay(Integer.MAX_VALUE);
        itemEntity.setVelocity(new Vector(0, 0, 0));
        itemEntity.setCanMobPickup(false);
        hologramEntities.add(itemEntity);
    }

    private void updateHologram() {
        if (hologramEntities.isEmpty()) return;

        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String bidLine = highestBidder == null
                ? plugin.getMessage("hologram-bid-starting", "%price%", String.format("%.2f", currentBid))
                : plugin.getMessage("hologram-bid-highest", "%price%", String.format("%.2f", currentBid));
        String timeLine = plugin.getMessage("hologram-time", "%time%", formatTimeRemaining());
        String itemLine = plugin.getMessage("hologram-item", "%item%", itemName);

        // Update ArmorStand custom names in order: item name, time, bid
        ((ArmorStand) hologramEntities.get(0)).setCustomName(itemLine);
        ((ArmorStand) hologramEntities.get(1)).setCustomName(timeLine);
        ((ArmorStand) hologramEntities.get(2)).setCustomName(bidLine);
        // Item entity (index 3) doesn't need updating
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
        for (Entity entity : hologramEntities) {
            entity.remove();
        }
        hologramEntities.clear();

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