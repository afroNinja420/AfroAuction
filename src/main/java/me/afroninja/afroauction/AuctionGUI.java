package me.afroninja.afroauction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Manages the graphical user interface for auctions, allowing players to view and bid on auctions.
 */
public class AuctionGUI implements Listener {
    private final AfroAuction plugin;
    private final Auction auction;
    private Inventory inventory;

    /**
     * Constructs a new AuctionGUI instance.
     * @param plugin the AfroAuction plugin instance
     * @param auction the auction to display
     */
    public AuctionGUI(AfroAuction plugin, Auction auction) {
        this.plugin = plugin;
        this.auction = auction;
        setupInventory();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Sets up the auction inventory with items and information.
     */
    private void setupInventory() {
        inventory = Bukkit.createInventory(null, 9, "Auction - " + (auction.getItem().getItemMeta().hasDisplayName() ? auction.getItem().getItemMeta().getDisplayName() : plugin.formatItemName(auction.getItem().getType().name())));
        inventory.setItem(4, auction.getItem());

        ItemStack bidItem = new ItemStack(org.bukkit.Material.GOLD_INGOT);
        ItemMeta bidMeta = bidItem.getItemMeta();
        bidMeta.setDisplayName(plugin.getMessage("gui-bid-item", "&eBid: %amount%", String.format("%.2f", auction.getHighestBid())));
        bidItem.setItemMeta(bidMeta);
        inventory.setItem(2, bidItem);

        ItemStack timeItem = new ItemStack(org.bukkit.Material.CLOCK);
        ItemMeta timeMeta = timeItem.getItemMeta();
        long timeLeft = (auction.getEndTime() - System.currentTimeMillis()) / 1000;
        timeMeta.setDisplayName(plugin.getMessage("gui-time-left", "&eTime Left: %time%", timeLeft + "s"));
        timeItem.setItemMeta(timeMeta);
        inventory.setItem(6, timeItem);
    }

    /**
     * Opens the auction inventory for a player.
     * @param player the player to open the inventory for
     */
    public void openInventory(Player player) {
        player.openInventory(inventory);
    }

    /**
     * Handles clicks within the auction inventory.
     * @param event the InventoryClickEvent
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().equals(inventory)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == org.bukkit.Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            if (event.getSlot() == 2) { // Bid item slot
                auction.placeBid(player, auction.getHighestBid() + plugin.getConfig().getDouble("min-bid-increment", 1.0));
                player.closeInventory();
                setupInventory(); // Refresh inventory
            }
        }
    }
}