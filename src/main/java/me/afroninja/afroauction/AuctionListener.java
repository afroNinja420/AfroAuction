package me.afroninja.afroauction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        if (!event.getView().getTitle().startsWith(plugin.getMessage("gui-title", "%item%", ""))) return;

        event.setCancelled(true); // Prevent all item movement

        if (event.getRawSlot() == 15) { // Bid button slot
            ItemStack item = inventory.getItem(15);
            if (item != null && item.getType() == org.bukkit.Material.EMERALD) {
                player.closeInventory();
                player.sendMessage("§aPlease enter your bid amount in chat: /pa bid <amount>");
            }
        }
    }

    private void openAuctionGUI(Player player, Auction auction) {
        Inventory gui = Bukkit.createInventory(null, 27, plugin.getMessage("gui-title", "%item%", getItemName(auction.getItem())));

        // Slot 11: Auction Info
        double startingBid = auction.getStartingBid();
        double currentBid = auction.getCurrentBid();
        long timeRemaining = (auction.getEndTime() - System.currentTimeMillis()) / 1000;
        int bidCount = auction.getBidCount();
        String highestBidder = auction.getHighestBidder() != null
                ? plugin.getServer().getOfflinePlayer(auction.getHighestBidder()).getName()
                : "None";
        String highestBid = auction.getHighestBidder() != null
                ? String.format("%.2f", currentBid)
                : "None";

        gui.setItem(11, createInfoItem(
                "§e" + plugin.getMessage("gui-info-title"),
                "§7Starting/Current Bid: §a$" + String.format("%.2f", startingBid) + " / $" + String.format("%.2f", currentBid),
                "§7Time Left: §a" + formatTime(timeRemaining),
                "§7Number of Bids: §a" + bidCount,
                "§7Highest Bidder: §a" + highestBidder,
                "§7Highest Bid: §a$" + highestBid
        ));

        // Slot 13: Item being sold
        gui.setItem(13, auction.getItem());

        // Slot 15: Place Bid button
        double bidIncrement = plugin.getConfig().getDouble("min-bid-increment", 1.0);
        double nextBid = currentBid + bidIncrement;
        gui.setItem(15, createBidButton("§a" + plugin.getMessage("gui-bid-button", "%amount%", String.format("%.2f", nextBid)), nextBid));

        player.openInventory(gui);
    }

    private ItemStack createInfoItem(String name, String... lore) {
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
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}