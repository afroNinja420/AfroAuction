package me.afroninja.afroauction.gui;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

/**
 * Manages the GUI for viewing all active auctions.
 */
public class ActiveAuctionsGUI implements Listener {
    private final AfroAuction plugin;
    private final Map<Location, Auction> activeAuctions;
    private final Player viewer;
    private final Inventory inventory;
    private int updateTaskId;

    /**
     * Constructs a new ActiveAuctionsGUI instance.
     * @param plugin the AfroAuction plugin instance
     * @param activeAuctions the map of active auctions from AuctionManager
     * @param viewer the player opening the GUI
     */
    public ActiveAuctionsGUI(AfroAuction plugin, Map<Location, Auction> activeAuctions, Player viewer) {
        this.plugin = plugin;
        this.activeAuctions = activeAuctions;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(null, 54, "Active Auctions"); // 54 slots for more auctions
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
        scheduleUpdate();
    }

    /**
     * Opens the active auctions GUI for the player.
     */
    public void openInventory() {
        viewer.openInventory(inventory);
    }

    /**
     * Updates the inventory with all active auctions.
     */
    private void updateInventory() {
        inventory.clear();
        int slot = 0;

        for (Auction auction : activeAuctions.values()) {
            if (slot >= 54) break; // Limit to 54 slots

            ItemStack item = auction.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String creatorName = plugin.getServer().getOfflinePlayer(auction.getSellerUUID()).getName();
                long timeLeft = (auction.getEndTime() - System.currentTimeMillis()) / 1000;
                meta.setLore(List.of(
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eStarting Bid: &f" + String.format("%.2f", auction.getStartPrice())),
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eHighest Bid: &f" + String.format("%.2f", auction.getHighestBid())),
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eCreator: &f" + (creatorName != null ? creatorName : "Unknown")),
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eTime Left: &f" + timeLeft + "s")
                ));
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
    }

    /**
     * Schedules a repeating task to update the GUI every second.
     */
    private void scheduleUpdate() {
        updateTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            updateInventory();
        }, 0L, 20L); // Update every 20 ticks (1 second)
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;
        event.setCancelled(true);
        // No actions on click for now; just viewing
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != inventory) return;
        if (inventory.getViewers().isEmpty()) {
            plugin.getServer().getScheduler().cancelTask(updateTaskId);
        }
    }
}