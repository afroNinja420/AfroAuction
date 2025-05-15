package me.afroninja.afroauction;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionCommand implements CommandExecutor {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;
    private final Map<UUID, Long> cooldowns;

    public AuctionCommand(AfroAuction plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.cooldowns = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 2) {
            player.sendMessage(plugin.getMessage("invalid-usage"));
            return true;
        }

        // Check cooldown
        long cooldownTime = plugin.getConfig().getLong("auction-cooldown", 60);
        long currentTime = System.currentTimeMillis() / 1000;
        UUID playerUUID = player.getUniqueId();
        if (cooldowns.containsKey(playerUUID)) {
            long lastUsed = cooldowns.get(playerUUID);
            if (currentTime - lastUsed < cooldownTime) {
                player.sendMessage(plugin.getMessage("cooldown", "%cooldown%", String.valueOf(cooldownTime - (currentTime - lastUsed))));
                return true;
            }
        }

        // Check active auctions limit
        int maxAuctions = plugin.getConfig().getInt("max-active-auctions", 5);
        long activeAuctions = auctionManager.getActiveAuctionsCount(playerUUID);
        if (activeAuctions >= maxAuctions) {
            player.sendMessage(plugin.getMessage("max-auctions", "%max_auctions%", String.valueOf(maxAuctions)));
            return true;
        }

        // Parse starting price
        double startPrice;
        try {
            startPrice = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("invalid-price-format"));
            return true;
        }

        double minPrice = plugin.getConfig().getDouble("min-start-price", 1.0);
        double maxPrice = plugin.getConfig().getDouble("max-start-price", 1000000.0);
        if (startPrice < minPrice || startPrice > maxPrice) {
            player.sendMessage(plugin.getMessage("invalid-price", "%min_price%", String.format("%.2f", minPrice), "%max_price%", String.format("%.2f", maxPrice)));
            return true;
        }

        // Parse duration
        long duration;
        try {
            duration = parseDuration(args[1]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getMessage("invalid-duration-format"));
            return true;
        }

        long minDuration = plugin.getConfig().getLong("min-auction-duration", 30);
        long maxDuration = plugin.getConfig().getLong("max-auction-duration", 86400);
        if (duration < minDuration || duration > maxDuration) {
            player.sendMessage(plugin.getMessage("invalid-duration", "%min_duration%", String.valueOf(minDuration), "%max_duration%", String.valueOf(maxDuration)));
            return true;
        }

        // Check item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(plugin.getMessage("no-item"));
            return true;
        }

        // Check if looking at a chest
        Block block = player.getTargetBlock(null, 5);
        if (block == null || !(block.getState() instanceof Chest)) {
            player.sendMessage(plugin.getMessage("not-chest"));
            return true;
        }

        Chest chest = (Chest) block.getState();
        if (auctionManager.isChestInUse(chest.getLocation())) {
            player.sendMessage(plugin.getMessage("chest-in-use"));
            return true;
        }

        // Create auction
        player.getInventory().setItemInMainHand(null);
        Auction auction = new Auction(plugin, playerUUID, item, chest.getLocation(), startPrice, duration);
        auctionManager.addAuction(auction);

        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
        player.sendMessage(plugin.getMessage("auction-created", "%item%", itemName, "%price%", String.format("%.2f", startPrice), "%duration%", formatDuration(duration)));
        cooldowns.put(playerUUID, currentTime);

        return true;
    }

    private long parseDuration(String durationStr) {
        Pattern pattern = Pattern.compile("(?i)(\\d+d)?(\\d+h)?(\\d+m)?(\\d+s)?");
        Matcher matcher = pattern.matcher(durationStr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration format");
        }

        long duration = 0;
        for (String part : durationStr.toLowerCase().split("(?<=\\d)(?=\\w)")) {
            if (part.endsWith("d")) {
                duration += Long.parseLong(part.replace("d", "")) * 24 * 3600;
            } else if (part.endsWith("h")) {
                duration += Long.parseLong(part.replace("h", "")) * 3600;
            } else if (part.endsWith("m")) {
                duration += Long.parseLong(part.replace("m", "")) * 60;
            } else if (part.endsWith("s")) {
                duration += Long.parseLong(part.replace("s", ""));
            }
        }

        if (duration == 0) {
            throw new IllegalArgumentException("Duration must be greater than 0");
        }
        return duration;
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