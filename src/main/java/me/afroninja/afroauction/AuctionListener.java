package me.afroninja.afroauction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AuctionListener implements Listener {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;

    public AuctionListener(AfroAuction plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Location location = event.getClickedBlock().getLocation();
        if (!(event.getClickedBlock().getState() instanceof Chest)) return;

        Auction auction = auctionManager.getAuction(location);
        if (auction == null) return;

        event.setCancelled(true);
        openAuctionGUI(event.getPlayer(), auction);
    }

    private void openAuctionGUI(Player player, Auction auction) {
        Inventory gui = Bukkit.createInventory(null, 9, plugin.getMessage("gui-title", "%item%", getItemName(auction.getItem())));
        double currentBid = auction.getCurrentBid();
        long timeRemaining = (auction.getEndTime() - System.currentTimeMillis()) / 1000;

        // Add info items
        gui.setItem(2, createInfoItem("§e" + plugin.getMessage("gui-info-title"), "§a" + plugin.getMessage("gui-bid-label", "%bid%", String.format("%.2f", currentBid))));
        gui.setItem(4, createInfoItem("§e" + plugin.getMessage("gui-info-title"), "§a" + plugin.getMessage("gui-time-label", "%time%", formatTime(timeRemaining))));

        // Add bid buttons
        double bidIncrement = plugin.getConfig().getDouble("min-bid-increment", 1.0);
        gui.setItem(6, createBidButton("§a" + plugin.getMessage("gui-bid-button", "%amount%", String.format("%.2f", currentBid + bidIncrement)), currentBid + bidIncrement));

        player.openInventory(gui);
    }

    private ItemStack createInfoItem(String name, String lore) {
        ItemStack item = new ItemStack(org.bukkit.Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBidButton(String name, double bidAmount) {
        ItemStack item = new ItemStack(org.bukkit.Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.Arrays.asList("§7Click to bid $" + String.format("%.2f", bidAmount)));
        item.setItemMeta(meta);
        return item;
    }

    private String getItemName(ItemStack item) {
        return item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        long minutes = seconds / 60;
        seconds %= 60;
        return (minutes > 0 ? minutes + "m " : "") + seconds + "s";
    }
}