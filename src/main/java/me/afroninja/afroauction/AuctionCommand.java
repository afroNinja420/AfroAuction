package me.afroninja.afroauction;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("afroauction.create")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to create auctions!");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /createauction <startPrice> <durationSeconds>");
            return true;
        }

        try {
            double startPrice = Double.parseDouble(args[0]);
            int durationSeconds = Integer.parseInt(args[1]);

            if (startPrice < 0) {
                player.sendMessage(ChatColor.RED + "Start price must be non-negative!");
                return true;
            }
            if (durationSeconds <= 0) {
                player.sendMessage(ChatColor.RED + "Duration must be positive!");
                return true;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "You must hold an item in your main hand!");
                return true;
            }

            Block targetBlock = player.getTargetBlock(null, 5);
            if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
                player.sendMessage(ChatColor.RED + "You must be looking at a chest!");
                return true;
            }

            if (auctionManager.getAuction(targetBlock.getLocation()) != null) {
                player.sendMessage(ChatColor.RED + "This chest is already an auction!");
                return true;
            }

            Auction auction = new Auction(plugin, targetBlock.getLocation(), item, startPrice, durationSeconds);
            auctionManager.addAuction(auction);
            player.getInventory().setItemInMainHand(null); // Remove item
            player.sendMessage(ChatColor.GREEN + "Auction created for " + item.getType().name() + "!");
            return true;

        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid start price or duration!");
            return true;
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "An error occurred while creating the auction!");
            plugin.getLogger().severe("Error creating auction: " + e.getMessage());
            return true;
        }
    }
}