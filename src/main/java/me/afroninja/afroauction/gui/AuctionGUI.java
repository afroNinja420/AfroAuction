package me.afroninja.afroauction.gui;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Manages the auction GUI for viewing and bidding on auctions.
 */
public class AuctionGUI implements Listener {
    private final AfroAuction plugin;
    private final Auction auction;
    private final Inventory inventory;
    private final Player viewer;
    private int updateTaskId;

    /**
     * Constructs a new AuctionGUI instance.
     * @param plugin the AfroAuction plugin instance
     * @param auction the Auction instance
     * @param viewer the player opening the GUI
     */
    public AuctionGUI(AfroAuction plugin, Auction auction, Player viewer) {
        this.plugin = plugin;
        this.auction = auction;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(null, 27, "Auction");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
        scheduleUpdate();
    }

    /**
     * Opens the auction GUI for the player.
     */
    public void openInventory() {
        viewer.openInventory(inventory);
    }

    /**
     * Updates the inventory with current auction details.
     */
    private void updateInventory() {
        // Clear inventory
        inventory.clear();

        // Slot 11: Auction details (paper)
        ItemStack details = new ItemStack(Material.PAPER);
        ItemMeta detailsMeta = details.getItemMeta();
        detailsMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eAuction Details"));
        String highestBidderName = auction.getHighestBidder() != null ? plugin.getServer().getOfflinePlayer(auction.getHighestBidder()).getName() : "None";
        long timeLeft = (auction.getEndTime() - System.currentTimeMillis()) / 1000;
        detailsMeta.setLore(Arrays.asList(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eStarting Bid: &f" + String.format("%.2f", auction.getStartPrice())),
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eHighest Bid: &f" + String.format("%.2f", auction.getHighestBid())),
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eBids: &f" + auction.getBidCount()),
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eHighest Bidder: &f" + highestBidderName),
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eTime Left: &f" + timeLeft + "s")
        ));
        details.setItemMeta(detailsMeta);
        inventory.setItem(11, details);

        // Slot 13: Auction item
        inventory.setItem(13, auction.getItem());

        // Slot 15: Bid button
        Material bidMaterial = Material.valueOf(plugin.getConfig().getString("bid-button-item", "EMERALD"));
        ItemStack bidButton = new ItemStack(bidMaterial);
        ItemMeta bidMeta = bidButton.getItemMeta();
        bidMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aPlace Bid"));
        bidButton.setItemMeta(bidMeta);
        inventory.setItem(15, bidButton);

        // Slot 22: Settings button (only for auction creator)
        if (viewer.getUniqueId().equals(auction.getSellerUUID())) {
            ItemStack settingsButton = new ItemStack(Material.ANVIL);
            ItemMeta settingsMeta = settingsButton.getItemMeta();
            settingsMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eSettings"));
            settingsButton.setItemMeta(settingsMeta);
            inventory.setItem(22, settingsButton);
        }
    }

    /**
     * Schedules a repeating task to update the GUI every second.
     */
    private void scheduleUpdate() {
        updateTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (System.currentTimeMillis() >= auction.getEndTime()) {
                plugin.getServer().getScheduler().cancelTask(updateTaskId);
                return;
            }
            updateInventory();
        }, 0L, 20L); // Update every 20 ticks (1 second)
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;
        event.setCancelled(true);

        if (event.getRawSlot() == 15) {
            Player player = (Player) event.getWhoClicked();
            double minBid = auction.getHighestBidder() == null
                    ? auction.getStartPrice()
                    : auction.getHighestBid() * (1 + plugin.getConfig().getDouble("min-bid-percentage-increment", 10.0) / 100.0);
            player.sendMessage(plugin.getMessage("bid-prompt", "%min_bid%", String.format("%.2f", minBid)));
            plugin.addPlayerAwaitingBid(player.getUniqueId(), auction);
            player.closeInventory();
        } else if (event.getRawSlot() == 22 && viewer.getUniqueId().equals(auction.getSellerUUID())) {
            // Open the settings GUI for the auction creator
            AuctionSettingsGUI settingsGUI = new AuctionSettingsGUI(plugin, auction, viewer, this);
            settingsGUI.openInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != inventory) return;
        if (inventory.getViewers().isEmpty()) {
            plugin.getServer().getScheduler().cancelTask(updateTaskId);
        }
    }

    /**
     * Retrieves the auction associated with this GUI.
     * @return the Auction instance
     */
    public Auction getAuction() {
        return auction;
    }
}