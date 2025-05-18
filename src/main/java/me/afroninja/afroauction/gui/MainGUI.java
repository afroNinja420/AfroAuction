package me.afroninja.afroauction.gui;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.AuctionCommand; // Added import
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
 * Manages the main GUI for the AfroAuction plugin, providing access to key features.
 */
public class MainGUI implements Listener {
    private final AfroAuction plugin;
    private final Player player;
    private final Inventory inventory;

    /**
     * Constructs a new MainGUI instance.
     * @param plugin the AfroAuction plugin instance
     * @param player the player opening the GUI
     */
    public MainGUI(AfroAuction plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 27, "AfroAuction");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        initializeInventory();
    }

    /**
     * Opens the main GUI for the player.
     */
    public void openInventory() {
        player.openInventory(inventory);
    }

    /**
     * Initializes the inventory with buttons for main actions.
     */
    private void initializeInventory() {
        // Slot 11: Active Auctions button
        ItemStack activeAuctionsButton = new ItemStack(Material.CHEST);
        ItemMeta activeAuctionsMeta = activeAuctionsButton.getItemMeta();
        activeAuctionsMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aView Active Auctions"));
        activeAuctionsButton.setItemMeta(activeAuctionsMeta);
        inventory.setItem(11, activeAuctionsButton);

        // Slot 13: Notifications button
        ItemStack notificationsButton = new ItemStack(Material.BELL);
        ItemMeta notificationsMeta = notificationsButton.getItemMeta();
        notificationsMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aToggle Notifications"));
        notificationsButton.setItemMeta(notificationsMeta);
        inventory.setItem(13, notificationsButton);

        // Slot 15: My Auctions button
        ItemStack myAuctionsButton = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta myAuctionsMeta = myAuctionsButton.getItemMeta();
        myAuctionsMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aView My Auctions"));
        myAuctionsButton.setItemMeta(myAuctionsMeta);
        inventory.setItem(15, myAuctionsButton);

        // Slot 17: Create Auction button (placeholder)
        ItemStack createAuctionButton = new ItemStack(Material.ANVIL);
        ItemMeta createAuctionMeta = createAuctionButton.getItemMeta();
        createAuctionMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eCreate Auction (Coming Soon)"));
        createAuctionButton.setItemMeta(createAuctionMeta);
        inventory.setItem(17, createAuctionButton);

        // Slot 19: Claim Items button
        ItemStack claimItemsButton = new ItemStack(Material.HOPPER);
        ItemMeta claimItemsMeta = claimItemsButton.getItemMeta();
        claimItemsMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aClaim Items"));
        claimItemsButton.setItemMeta(claimItemsMeta);
        inventory.setItem(19, claimItemsButton);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;
        event.setCancelled(true);

        Player clickedPlayer = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 11) {
            // Open Active Auctions GUI
            ActiveAuctionsGUI gui = new ActiveAuctionsGUI(plugin, plugin.getAuctionManager().getActiveAuctions(), clickedPlayer);
            gui.openInventory();
        } else if (slot == 13) {
            // Toggle Notifications
            plugin.getNotificationManager().setNotificationsEnabled(clickedPlayer.getUniqueId(), !plugin.getNotificationManager().hasNotificationsEnabled(clickedPlayer.getUniqueId()));
            clickedPlayer.sendMessage(plugin.getMessage("notifications-toggled", "%state%", plugin.getNotificationManager().hasNotificationsEnabled(clickedPlayer.getUniqueId()) ? "enabled" : "disabled"));
        } else if (slot == 15) {
            // Open Player Auctions GUI
            PlayerAuctionsGUI gui = new PlayerAuctionsGUI(plugin, plugin.getAuctionManager().getPlayerAuctions(clickedPlayer.getUniqueId()), clickedPlayer);
            gui.openInventory();
        } else if (slot == 19) {
            // Handle Claim Items
            AuctionCommand auctionCommand = new AuctionCommand(plugin, plugin.getAuctionManager(), plugin.getNotificationManager(), plugin.getPendingItemsManager());
            auctionCommand.handleClaim(clickedPlayer);
        }
        // Slot 17 (Create Auction) is a placeholder, so no action is taken
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != inventory) return;
        // No cleanup needed
    }
}