package me.afroninja.afroauction;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AuctionClaimCommand implements CommandExecutor {
    private final AfroAuction plugin;
    private final PendingItemsManager pendingItemsManager;

    public AuctionClaimCommand(AfroAuction plugin, PendingItemsManager pendingItemsManager) {
        this.plugin = plugin;
        this.pendingItemsManager = pendingItemsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        var pendingItems = pendingItemsManager.getPendingItems(player.getUniqueId());
        if (pendingItems.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no pending auction items to claim.");
            return true;
        }

        Map<ItemStack, Boolean> deliveryStatus = new HashMap<>();
        for (ItemStack item : pendingItems) {
            String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
            Map<Integer, ItemStack> undelivered = player.getInventory().addItem(item);
            if (undelivered.isEmpty()) {
                player.sendMessage(ChatColor.GREEN + "You received " + itemName + " from a won auction!");
                deliveryStatus.put(item, true);
            } else {
                player.sendMessage(ChatColor.YELLOW + "Your inventory is full! Clear space and try /auctionclaim again for " + itemName + ".");
                deliveryStatus.put(item, false);
            }
        }

        pendingItemsManager.clearPendingItems(player.getUniqueId());
        deliveryStatus.forEach((item, delivered) -> {
            if (!delivered) {
                pendingItemsManager.addPendingItem(player.getUniqueId(), item);
            }
        });

        return true;
    }
}