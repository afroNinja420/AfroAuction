package me.afroninja.afroauction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        ItemStack item = pendingItemsManager.getPendingItem(player.getUniqueId());
        if (item == null) {
            sender.sendMessage(plugin.getMessage("claim-no-items"));
            return true;
        }

        if (player.getInventory().firstEmpty() == -1) {
            sender.sendMessage(plugin.getMessage("winner-inventory-full", "%item%", item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name()));
            return true;
        }

        player.getInventory().addItem(item);
        pendingItemsManager.removePendingItem(player.getUniqueId());
        sender.sendMessage(plugin.getMessage("claim-success", "%item%", item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name()));
        return true;
    }
}