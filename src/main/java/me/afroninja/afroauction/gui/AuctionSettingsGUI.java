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

/**
 * Manages the settings GUI for an auction, accessible only by the auction creator.
 */
public class AuctionSettingsGUI implements Listener {
    private final AfroAuction plugin;
    private final Auction auction;
    private final Player viewer;
    private final AuctionGUI auctionGUI;
    private final Inventory inventory;

    /**
     * Constructs a new AuctionSettingsGUI instance.
     * @param plugin the AfroAuction plugin instance
     * @param auction the Auction instance
     * @param viewer the player opening the GUI
     * @param auctionGUI the parent AuctionGUI instance
     */
    public AuctionSettingsGUI(AfroAuction plugin, Auction auction, Player viewer, AuctionGUI auctionGUI) {
        this.plugin = plugin;
        this.auction = auction;
        this.viewer = viewer;
        this.auctionGUI = auctionGUI;
        this.inventory = Bukkit.createInventory(null, 27, "Auction Settings");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        initializeInventory();
    }

    /**
     * Opens the settings GUI for the player.
     */
    public void openInventory() {
        viewer.openInventory(inventory);
    }

    /**
     * Initializes the inventory with settings options.
     */
    private void initializeInventory() {
        // Slot 22: Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cBack"));
        backButton.setItemMeta(backMeta);
        inventory.setItem(22, backButton);

        // Placeholder for future settings
        ItemStack placeholder = new ItemStack(Material.PAPER);
        ItemMeta placeholderMeta = placeholder.getItemMeta();
        placeholderMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eSettings Coming Soon"));
        placeholder.setItemMeta(placeholderMeta);
        inventory.setItem(13, placeholder);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;
        event.setCancelled(true);

        if (event.getRawSlot() == 22) {
            // Return to the auction GUI
            auctionGUI.openInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != inventory) return;
        // No additional cleanup needed
    }
}