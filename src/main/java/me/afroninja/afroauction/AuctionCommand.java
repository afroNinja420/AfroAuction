package me.afroninja.afroauction;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AuctionCommand implements CommandExecutor {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;

    public AuctionCommand(AfroAuction plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("afroauction.create")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to create auctions!");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /createauction <startPrice> <durationSeconds>");
            return true;
        }

        double startPrice;
        int durationSeconds;
        try {
            startPrice = Double.parseDouble(args[0]);
            durationSeconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Start price and duration must be numbers!");
            return true;
        }

        if (startPrice <= 0) {
            player.sendMessage(ChatColor.RED + "Start price must be greater than 0!");
            return true;
        }
        if (durationSeconds <= 0) {
            player.sendMessage(ChatColor.RED + "Duration must be greater than 0!");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be holding an item to auction!");
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Chest)) {
            player.sendMessage(ChatColor.RED + "You must be looking at a chest!");
            return true;
        }

        if (auctionManager.getAuction(targetBlock.getLocation()) != null) {
            player.sendMessage(ChatColor.RED + "This chest is already an auction!");
            return true;
        }

        ItemStack auctionItem = itemInHand.clone();
        auctionItem.setAmount(1);
        player.getInventory().setItemInMainHand(null);
        String itemName = auctionItem.hasItemMeta() && auctionItem.getItemMeta().hasDisplayName() ? auctionItem.getItemMeta().getDisplayName() : auctionItem.getType().name();
        auctionManager.createAuction(player.getUniqueId(), targetBlock.getLocation(), auctionItem, startPrice, durationSeconds);
        player.sendMessage(ChatColor.GREEN + "Auction created for " + itemName + " at $" + String.format("%.2f", startPrice) + " for " + durationSeconds + " seconds!");
        return true;
    }
}