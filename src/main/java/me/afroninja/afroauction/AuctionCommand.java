package me.afroninja.afroauction;

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
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("afroauction.create")) {
            sender.sendMessage(plugin.getMessage("no-permission", "%action%", "create auctions"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("invalid-usage"));
            return true;
        }

        double price;
        try {
            price = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessage("invalid-price-format"));
            return true;
        }

        double minPrice = plugin.getConfig().getDouble("min-start-price", 1.0);
        double maxPrice = plugin.getConfig().getDouble("max-start-price", 1000000.0);
        if (price < minPrice || price > maxPrice) {
            sender.sendMessage(plugin.getMessage("invalid-price", "%min_price%", String.format("%.2f", minPrice), "%max_price%", String.format("%.2f", maxPrice)));
            return true;
        }

        long duration;
        try {
            duration = parseDuration(args[1]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessage("invalid-duration-format"));
            return true;
        }

        long minDuration = plugin.getConfig().getLong("min-auction-duration", 30);
        long maxDuration = plugin.getConfig().getLong("max-auction-duration", 86400);
        if (duration < minDuration || duration > maxDuration) {
            String minDurationStr = formatDuration(minDuration);
            String maxDurationStr = formatDuration(maxDuration);
            sender.sendMessage(plugin.getMessage("invalid-duration", "%min_duration%", minDurationStr, "%max_duration%", maxDurationStr));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(plugin.getMessage("no-item"));
            return true;
        }

        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock == null || (targetBlock.getType() != Material.CHEST && targetBlock.getType() != Material.TRAPPED_CHEST)) {
            sender.sendMessage(plugin.getMessage("not-chest"));
            return true;
        }

        if (auctionManager.getAuction(targetBlock.getLocation()) != null) {
            sender.sendMessage(plugin.getMessage("chest-in-use"));
            return true;
        }

        long cooldown = plugin.getConfig().getLong("auction-cooldown", 60) * 1000;
        long lastAuctionTime = auctionManager.getLastAuctionTime(player.getUniqueId());
        if (System.currentTimeMillis() - lastAuctionTime < cooldown) {
            long remaining = (cooldown - (System.currentTimeMillis() - lastAuctionTime)) / 1000;
            sender.sendMessage(plugin.getMessage("cooldown", "%cooldown%", String.valueOf(remaining)));
            return true;
        }

        int maxAuctions = plugin.getConfig().getInt("max-active-auctions", 5);
        if (auctionManager.getActiveAuctions(player.getUniqueId()).size() >= maxAuctions) {
            sender.sendMessage(plugin.getMessage("max-auctions", "%max_auctions%", String.valueOf(maxAuctions)));
            return true;
        }

        ItemStack auctionItem = item.clone();
        auctionItem.setAmount(1);
        player.getInventory().removeItem(auctionItem);

        Auction auction = new Auction(plugin, player.getUniqueId(), auctionItem, targetBlock.getLocation(), price, duration);
        auctionManager.addAuction(auction);

        String itemName = auctionItem.getItemMeta().hasDisplayName() ? auctionItem.getItemMeta().getDisplayName() : auctionItem.getType().name();
        String durationStr = formatDuration(duration);
        sender.sendMessage(plugin.getMessage("auction-created", "%item%", itemName, "%price%", String.format("%.2f", price), "%duration%", durationStr));
        return true;
    }

    private long parseDuration(String input) {
        long totalSeconds = 0;
        StringBuilder number = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                if (number.length() == 0) {
                    throw new IllegalArgumentException("Invalid duration format");
                }
                long value = Long.parseLong(number.toString());
                switch (c) {
                    case 'd':
                        totalSeconds += value * 24 * 3600;
                        break;
                    case 'h':
                        totalSeconds += value * 3600;
                        break;
                    case 'm':
                        totalSeconds += value * 60;
                        break;
                    case 's':
                        totalSeconds += value;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid duration unit: " + c);
                }
                number = new StringBuilder();
            }
        }
        if (number.length() > 0) {
            throw new IllegalArgumentException("Incomplete duration format");
        }
        if (totalSeconds == 0) {
            throw new IllegalArgumentException("Duration must be greater than 0");
        }
        return totalSeconds;
    }

    private String formatDuration(long seconds) {
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d, ");
        if (hours > 0 || days > 0) sb.append(hours).append("h, ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m, ");
        sb.append(seconds).append("s");
        return sb.toString();
    }
}