package me.afroninja.afroauction.gui;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Represents the main GUI for managing auctions, allowing players to view active auctions.
 */
public class MainGUI {
    private final AfroAuction plugin;
    private final Player player;
    private Inventory inventory;

    /**
     * Constructs a new MainGUI instance.
     * @param plugin the AfroAuction plugin instance
     * @param player the player opening the GUI
     */
    public MainGUI(AfroAuction plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        initializeInventory();
    }

    /**
     * Initializes the inventory with active auctions.
     */
    private void initializeInventory() {
        List<Auction> auctions = plugin.getAuctionManager().getActiveAuctions();
        int size = Math.min(54, (auctions.size() / 9 + 1) * 9); // Ensure at least one row, max 6 rows
        inventory = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', "&eActive Auctions"));

        int slot = 0;
        for (Auction auction : auctions) {
            if (slot >= size) break; // Prevent overflow

            ItemStack item = auction.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6" + plugin.formatItemName(item.getType().name())));
                meta.setLore(java.util.Arrays.asList(
                        ChatColor.translateAlternateColorCodes('&', "&eBid: &f" + plugin.formatCurrency(auction.getHighestBid())),
                        ChatColor.translateAlternateColorCodes('&', "&eTime Left: &f" + formatTime(auction.getEndTime() - System.currentTimeMillis()))
                ));
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);

            slot++;
        }

        // Add bid button if space allows
        if (slot < size) {
            ItemStack bidButton = new ItemStack(Material.valueOf(plugin.getConfig().getString("bid-button-item", "EMERALD")));
            ItemMeta meta = bidButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aPlace Bid"));
                bidButton.setItemMeta(meta);
            }
            inventory.setItem(slot, bidButton);
        }
    }

    /**
     * Opens the inventory for the player.
     */
    public void openInventory() {
        player.openInventory(inventory);
    }

    /**
     * Formats the time left into a concise hh,mm,ss format, showing only the smallest non-zero units.
     * @param milliseconds the time left in milliseconds
     * @return the formatted time string (e.g., "5m, 43s" or "43s")
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds <= 0) return "0s";

        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder timeStr = new StringBuilder();
        if (hours > 0) {
            timeStr.append(hours).append("h");
            if (minutes > 0) timeStr.append(", ").append(minutes).append("m");
        } else if (minutes > 0) {
            timeStr.append(minutes).append("m");
            if (seconds > 0) timeStr.append(", ").append(seconds).append("s");
        } else {
            timeStr.append(seconds).append("s");
        }
        return timeStr.toString();
    }
}