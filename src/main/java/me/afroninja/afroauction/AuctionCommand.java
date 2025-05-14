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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionCommand implements CommandExecutor {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public AuctionCommand(AfroAuction plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("afroauction.create")) {
            sender.sendMessage(getMessage("no-permission", Map.of("action", "create auctions")));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(getMessage("invalid-usage"));
            return true;
        }

        double startPrice;
        try {
            startPrice = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(getMessage("invalid-price-format"));
            return true;
        }

        double minPrice = plugin.getConfig().getDouble("min-start-price");
        double maxPrice = plugin.getConfig().getDouble("max-start-price");
        if (startPrice < minPrice || startPrice > maxPrice) {
            player.sendMessage(getMessage("invalid-price", Map.of(
                    "min_price", String.format("%.2f", minPrice),
                    "max_price", String.format("%.2f", maxPrice)
            )));
            return true;
        }

        int durationSeconds;
        try {
            durationSeconds = parseDuration(args[1]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(getMessage("invalid-duration-format"));
            return true;
        }

        int minDuration = plugin.getConfig().getInt("min-auction-duration");
        int maxDuration = plugin.getConfig().getInt("max-auction-duration");
        if (durationSeconds < minDuration || durationSeconds > maxDuration) {
            player.sendMessage(getMessage("invalid-duration", Map.of(
                    "min_duration", formatDuration(minDuration),
                    "max_duration", formatDuration(maxDuration)
            )));
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(getMessage("no-item"));
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Chest)) {
            player.sendMessage(getMessage("not-chest"));
            return true;
        }

        if (auctionManager.getAuction(targetBlock.getLocation()) != null) {
            player.sendMessage(getMessage("chest-in-use"));
            return true;
        }

        // Check cooldown
        long cooldown = plugin.getConfig().getLong("auction-cooldown") * 1000;
        long lastAuction = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() < lastAuction + cooldown) {
            long remaining = (lastAuction + cooldown - System.currentTimeMillis()) / 1000;
            player.sendMessage(getMessage("cooldown", Map.of("cooldown", String.valueOf(remaining))));
            return true;
        }

        // Check max active auctions
        int maxAuctions = plugin.getConfig().getInt("max-active-auctions");
        long activeAuctions = auctionManager.getActiveAuctions().values().stream()
                .filter(a -> a.getCreator().equals(player.getUniqueId()))
                .count();
        if (activeAuctions >= maxAuctions) {
            player.sendMessage(getMessage("max-auctions", Map.of("max_auctions", String.valueOf(maxAuctions))));
            return true;
        }

        ItemStack auctionItem = itemInHand.clone();
        auctionItem.setAmount(1);
        String itemName = auctionItem.hasItemMeta() && auctionItem.getItemMeta().hasDisplayName() ? auctionItem.getItemMeta().getDisplayName() : auctionItem.getType().name();
        player.getInventory().setItemInMainHand(null);
        auctionManager.createAuction(player.getUniqueId(), targetBlock.getLocation(), auctionItem, startPrice, durationSeconds);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(getMessage("auction-created", Map.of(
                "item", itemName,
                "price", String.format("%.2f", startPrice),
                "duration", formatDuration(durationSeconds)
        )));
        return true;
    }

    private int parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Empty duration");
        }

        Pattern pattern = Pattern.compile("(\\d+)([dhms])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        int totalSeconds = 0;

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            switch (unit) {
                case "d":
                    totalSeconds += value * 24 * 3600;
                    break;
                case "h":
                    totalSeconds += value * 3600;
                    break;
                case "m":
                    totalSeconds += value * 60;
                    break;
                case "s":
                    totalSeconds += value;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid unit: " + unit);
            }
        }

        if (totalSeconds == 0) {
            throw new IllegalArgumentException("No valid duration found");
        }

        return totalSeconds;
    }

    private String formatDuration(int seconds) {
        if (seconds <= 0) return "0s";

        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder time = new StringBuilder();
        if (days > 0) time.append(days).append("d, ");
        if (hours > 0 || days > 0) time.append(hours).append("h, ");
        if (minutes > 0 || hours > 0 || days > 0) time.append(minutes).append("m, ");
        time.append(seconds).append("s");

        return time.toString();
    }

    private String getMessage(String key) {
        return getMessage(key, Map.of());
    }

    private String getMessage(String key, Map<String, String> replacements) {
        String message = plugin.getConfig().getString("messages." + key, "&cMissing message: " + key);
        String originalMessage = message;

        // Find the last color code before %item%
        String lastColorCode = "&f"; // Default to white if no color code
        Pattern colorPattern = Pattern.compile("&[0-9a-fk-or]");
        Matcher matcher = colorPattern.matcher(message);
        while (matcher.find()) {
            if (message.indexOf("%item%", matcher.start()) >= 0) {
                lastColorCode = matcher.group();
            }
        }

        // Replace variables
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            String value = entry.getValue();
            if (entry.getKey().equals("item")) {
                // For %item%, append RESET and the last color code
                value = value + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', lastColorCode);
            }
            message = message.replace(placeholder, value);
        }

        // If no %item% is present, preserve original color handling
        if (!originalMessage.contains("%item%")) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}