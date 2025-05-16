package me.afroninja.afroauction.listeners;

import me.afroninja.afroauction.AfroAuction;
import me.afroninja.afroauction.Auction;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatBidListener implements Listener {
    private final AfroAuction plugin;
    private final Auction auction;
    private final Player player;
    private final double minBid;

    public ChatBidListener(AfroAuction plugin, Auction auction, Player player, double minBid) {
        this.plugin = plugin;
        this.auction = auction;
        this.player = player;
        this.minBid = minBid;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() != player) return;
        event.setCancelled(true); // Prevent chat message from broadcasting

        String message = event.getMessage().trim();
        HandlerList.unregisterAll(this); // Unregister after processing

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.YELLOW + "Bid cancelled.");
                return;
            }

            try {
                double bidAmount = Double.parseDouble(message);
                if (bidAmount < minBid) {
                    player.sendMessage(ChatColor.RED + "Bid must be at least $" + String.format("%.2f", minBid) + "!");
                    return;
                }
                if (auction.placeBid(player, bidAmount)) {
                    player.sendMessage(ChatColor.GREEN + "Bid placed successfully!");
                } else {
                    player.sendMessage(ChatColor.RED + "Bid failed! Ensure you have enough money ($" + String.format("%.2f", bidAmount) + ").");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid bid amount! Enter a number or 'cancel'.");
            }
        });
    }
}