package me.afroninja.afroauction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BidCommand implements CommandExecutor, TabCompleter {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;

    public BidCommand(AfroAuction plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (args.length != 1) {
            player.sendMessage("§cUsage: /bid <amount>");
            return true;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("invalid-price-format"));
            return true;
        }

        // Find an auction to bid on (simplified logic for now)
        Auction auction = auctionManager.getAuction(player.getTargetBlock(null, 5).getLocation());
        if (auction == null) {
            player.sendMessage("§cNo auction found at your target chest!");
            return true;
        }

        auction.placeBid(player, bidAmount);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("<amount>");
        }

        return completions;
    }
}