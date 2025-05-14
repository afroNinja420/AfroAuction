package me.afroninja.afroauction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AuctionGUI implements Listener {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;
    private final Auction auction;
    private final Player player;
    private final Inventory inventory;

    public AuctionGUI(AfroAuction plugin, AuctionManager auctionManager, Auction auction, Player player) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.auction = auction;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 27, plugin.getMessage("gui-title", "%item%", getItemName()));

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    private String getItemName() {
        return auction.getItem().getItemMeta().hasDisplayName() ? auction.getItem().getItemMeta().getDisplayName() : auction.getItem().getType().name();
    }

    private void updateInventory() {
        inventory.clear();

        // Auction item in slot 13 (center of second row)
        ItemStack itemDisplay = auction.getItem().clone();
        inventory.setItem(13, itemDisplay);

        // Info item in slot 11 (left of auction item)
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(plugin.getMessage("gui-info-title"));
        infoMeta.setLore(Arrays.asList(
                plugin.getMessage("gui-bid-label", "%bid%", String.format("%.2f", auction.getCurrentBid())),
                plugin.getMessage("gui-time-label", "%time%", formatTimeRemaining())
        ));
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(11, infoItem);

        // Bid button in slot 15 (right of auction item)
        ItemStack bidButton = new ItemStack(Material.EMERALD);
        ItemMeta bidMeta = bidButton.getItemMeta();
        double nextBid = auction.getCurrentBid() + plugin.getConfig().getDouble("min-bid-increment", 1.0);
        bidMeta.setDisplayName(plugin.getMessage("gui-bid-button", "%amount%", String.format("%.2f", nextBid)));
        bidButton.setItemMeta(bidMeta);
        inventory.setItem(15, bidButton);
    }

    private String formatTimeRemaining() {
        long remaining = (auction.getEndTime() - System.currentTimeMillis()) / 1000;
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

    public void open() {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() != 15) {
            return;
        }

        double nextBid = auction.getCurrentBid() + plugin.getConfig().getDouble("min-bid-increment", 1.0);
        if (auction.placeBid(player, nextBid)) {
            updateInventory();
        }
    }
}