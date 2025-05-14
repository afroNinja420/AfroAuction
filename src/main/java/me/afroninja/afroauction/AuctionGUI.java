package me.afroninja.afroauction;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

public class AuctionGUI implements Listener {
    private final AfroAuction plugin;
    private final Auction auction;
    private final Player player;
    private final Inventory inventory;

    public AuctionGUI(AfroAuction plugin, Auction auction, Player player) {
        this.plugin = plugin;
        this.auction = auction;
        this.player = player;
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui-title"));
        this.inventory = Bukkit.createInventory(null, 27, title);
        updateInventory();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();
        ItemStack auctionItem = auction.getItem();
        inventory.setItem(13, auctionItem);

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.YELLOW + "Auction Info");
        infoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Start Price: $" + String.format("%.2f", auction.getStartPrice()),
                ChatColor.GRAY + "Current Price: $" + String.format("%.2f", auction.getCurrentPrice()),
                ChatColor.GRAY + "Bids: " + auction.getBidCount(),
                ChatColor.GRAY + "Time Left: " + auction.getTimeLeftSeconds() + "s"
        ));
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(11, infoItem);

        ItemStack bidItem = new ItemStack(Material.EMERALD);
        ItemMeta bidMeta = bidItem.getItemMeta();
        bidMeta.setDisplayName(ChatColor.GREEN + "Place Bid");
        bidMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to enter bid amount in chat!",
                ChatColor.GRAY + "Min Bid: $" + String.format("%.2f", auction.getCurrentPrice() + plugin.getConfig().getDouble("min-bid-increment"))
        ));
        bidItem.setItemMeta(bidMeta);
        inventory.setItem(15, bidItem);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;
        event.setCancelled(true); // Prevent all item interactions

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.EMERALD && event.getSlot() == 15) {
            double minBid = auction.getCurrentPrice() + plugin.getConfig().getDouble("min-bid-increment");
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Enter your bid (min $" + String.format("%.2f", minBid) + ") in chat. Type 'cancel' to abort.");
            new ChatBidListener(plugin, auction, player, minBid);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() == inventory) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
}