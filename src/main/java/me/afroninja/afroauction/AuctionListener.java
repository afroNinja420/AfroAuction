package me.afroninja.afroauction;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class AuctionListener implements Listener {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;

    public AuctionListener(AfroAuction plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        plugin.getLogger().info("AuctionListener registered");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        plugin.getLogger().info("PlayerInteractEvent triggered: " + event.getAction());
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            plugin.getLogger().info("Not a right-click block action: " + event.getAction());
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            plugin.getLogger().info("No block clicked");
            return;
        }
        plugin.getLogger().info("Block clicked: " + block.getType() + " at " + block.getLocation());
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            plugin.getLogger().info("Block is not a chest: " + block.getType());
            return;
        }

        Location location = block.getLocation();
        Auction auction = auctionManager.getAuction(location);
        if (auction == null) {
            plugin.getLogger().info("No auction found for chest at " + location);
            return;
        }

        plugin.getLogger().info("Auction found for chest at " + location + ": " + auction.getItem().getItemMeta().getDisplayName());
        event.setCancelled(true);
        Player player = event.getPlayer();
        try {
            new AuctionGUI(plugin, auctionManager, auction, player).open();
            plugin.getLogger().info("Opened AuctionGUI for player " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to open AuctionGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}