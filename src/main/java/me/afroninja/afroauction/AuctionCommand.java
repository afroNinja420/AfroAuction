package me.afroninja.afroauction;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionCommand implements CommandExecutor, TabCompleter {
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

        if (args.length == 0) {
            player.sendMessage(plugin.getMessage("invalid-usage"));
            return true;
        }

        String subcommand = args[0].toLowerCase();

        if (subcommand.equals("create")) {
            if (args.length != 3) {
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
                startPrice = Double.parseDouble(args[1]);
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
                duration = parseDuration(args[2]);
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
        } else if (subcommand.equals("bid")) {
            if (args.length != 2) {
                player.sendMessage("§cUsage: /pa bid <amount>");
                return true;
            }

            double bidAmount;
            try {
                bidAmount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessage("invalid-price-format"));
                return true;
            }

            // Find auction at the targeted chest
            Block block = player.getTargetBlock(null, 5);
            if (block == null || !(block.getState() instanceof Chest)) {
                player.sendMessage("§cYou must be looking at the auction chest to bid!");
                return true;
            }

            Auction auction = auctionManager.getAuction(block.getLocation());
            if (auction == null) {
                player.sendMessage("§cNo auction found at this chest!");
                return true;
            }

            auction.placeBid(player, bidAmount);
            return true;
        }

        player.sendMessage(plugin.getMessage("invalid-usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest subcommands
            if ("create".startsWith(args[0].toLowerCase())) {
                completions.add("create");
            }
            if ("bid".startsWith(args[0].toLowerCase())) {
                completions.add("bid");
            }
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("create")) {
                completions.add("<price>");
            } else if (subcommand.equals("bid")) {
                completions.add("<amount>");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            completions.add("<duration>");
        }

        return completions;
    }

    private long parseDuration(String durationStr) {
        Pattern pattern = Pattern.compile("(?i)(\\d+d)?(\\d+h)?(\\d+m)?(\\d+s)?");
        Matcher matcher = pattern.matcher(durationStr);
        if (!matcher.matches()) {
            plugin.getLogger().info("Duration parse failed: " + durationStr + " does not match pattern");
            throw new IllegalArgumentException("Invalid duration format");
        }

        long duration = 0;
        String lowercaseDuration = durationStr.toLowerCase();
        if (lowercaseDuration.contains("d")) {
            Pattern dayPattern = Pattern.compile("(\\d+)d");
            Matcher dayMatcher = dayPattern.matcher(lowercaseDuration);
            if (dayMatcher.find()) {
                duration += Long.parseLong(dayMatcher.group(1)) * 24 * 3600;
                plugin.getLogger().info("Parsed days: " + dayMatcher.group(1));
            }
        }
        if (lowercaseDuration.contains("h")) {
            Pattern hourPattern = Pattern.compile("(\\d+)h");
            Matcher hourMatcher = hourPattern.matcher(lowercaseDuration);
            if (hourMatcher.find()) {
                duration += Long.parseLong(hourMatcher.group(1)) * 3600;
                plugin.getLogger().info("Parsed hours: " + hourMatcher.group(1));
            }
        }
        if (lowercaseDuration.contains("m")) {
            Pattern minutePattern = Pattern.compile("(\\d+)m");
            Matcher minuteMatcher = minutePattern.matcher(lowercaseDuration);
            if (minuteMatcher.find()) {
                duration += Long.parseLong(minuteMatcher.group(1)) * 60;
                plugin.getLogger().info("Parsed minutes: " + minuteMatcher.group(1));
            }
        }
        if (lowercaseDuration.contains("s")) {
            Pattern secondPattern = Pattern.compile("(\\d+)s");
            Matcher secondMatcher = secondPattern.matcher(lowercaseDuration);
            if (secondMatcher.find()) {
                duration += Long.parseLong(secondMatcher.group(1));
                plugin.getLogger().info("Parsed seconds: " + secondMatcher.group(1));
            }
        }

        if (duration == 0) {
            plugin.getLogger().info("Duration parse failed: Total duration is 0 for input " + durationStr);
            throw new IllegalArgumentException("Duration must be greater than 0");
        }
        plugin.getLogger().info("Successfully parsed duration: " + durationStr + " to " + duration + " seconds");
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