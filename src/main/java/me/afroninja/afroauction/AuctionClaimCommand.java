package me.afroninja.afroauction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionClaimCommand implements CommandExecutor {
    private final AfroAuction plugin;
    private final PendingItemsManager pendingItemsManager;

    public AuctionClaimCommand(AfroAuction plugin, PendingItemsManager pendingItemsManager) {
        this.plugin = plugin;
        this.pendingItemsManager = pendingItemsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        int claimedCount = 0;

        // Get all pending items for the player
        ItemStack[] pendingItems = pendingItemsManager.getPendingItems(playerUUID);
        if (pendingItems == null || pendingItems.length == 0) {
            player.sendMessage(plugin.getMessage("claim-no-items"));
            return true;
        }

        // Try to add each item to the player's inventory
        for (ItemStack item : pendingItems) {
            if (item == null) continue;

            int firstEmpty = player.getInventory().firstEmpty();
            if (firstEmpty != -1) {
                player.getInventory().setItem(firstEmpty, item);
                pendingItemsManager.removePendingItem(playerUUID, item);
                claimedCount++;
                String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
                player.sendMessage(plugin.getMessage("claim-success", "%item%", itemName));
            }
        }

        if (claimedCount == 0) {
            player.sendMessage(plugin.getMessage("claim-no-space"));
        }

        return true;
    }
}