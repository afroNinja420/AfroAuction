package me.afroninja.afroauction;

import me.afroninja.afroauction.gui.MainGUI;
import me.afroninja.afroauction.gui.ActiveAuctionsGUI;
import me.afroninja.afroauction.managers.AuctionManager;
import me.afroninja.afroauction.managers.NotificationManager;
import me.afroninja.afroauction.managers.PendingItemsManager;
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

/**
 * Handles all auction-related commands for the AfroAuction plugin, including /pa and /claim.
 */
public class AuctionCommand implements CommandExecutor, TabCompleter {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;
    private final NotificationManager notificationManager;
    private final PendingItemsManager pendingItemsManager;
    private final Map<UUID, Long> cooldowns;

    /**
     * Constructs a new AuctionCommand instance.
     * @param plugin the AfroAuction plugin instance
     * @param auctionManager the AuctionManager instance
     * @param notificationManager the NotificationManager instance
     * @param pendingItemsManager the PendingItemsManager instance
     */
    public AuctionCommand(AfroAuction plugin, AuctionManager auctionManager, NotificationManager notificationManager, PendingItemsManager pendingItemsManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.notificationManager = notificationManager;
        this.pendingItemsManager = pendingItemsManager;
        this.cooldowns = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Handle /pa command with subcommands
        if (args.length == 0) {
            MainGUI gui = new MainGUI(plugin, player);
            gui.openInventory();
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
        } else if (subcommand.equals("notifications")) {
            UUID playerUUID = player.getUniqueId();
            boolean currentState = notificationManager.hasNotificationsEnabled(playerUUID);
            notificationManager.setNotificationsEnabled(playerUUID, !currentState);
            player.sendMessage(plugin.getMessage("notifications-toggled", "%state%", currentState ? "disabled" : "enabled"));
            return true;
        } else if (subcommand.equals("claim")) {
            return handleClaim(player);
        } else if (subcommand.equals("activeauctions")) {
            if (args.length != 1) {
                player.sendMessage(plugin.getMessage("invalid-usage"));
                return true;
            }
            ActiveAuctionsGUI gui = new ActiveAuctionsGUI(plugin, auctionManager.getActiveAuctions(), player);
            gui.openInventory();
            return true;
        } else if (subcommand.equals("help")) {
            if (args.length != 1) {
                player.sendMessage(plugin.getMessage("invalid-usage"));
                return true;
            }
            displayHelp(player);
            return true;
        }

        player.sendMessage(plugin.getMessage("invalid-usage"));
        return true;
    }

    /**
     * Displays the help message with all available commands and their usage.
     * @param player the player to send the help message to
     */
    private void displayHelp(Player player) {
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&m----------&e[ &aAfroAuction Help &e]&m----------"));
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a/pa create <price> <duration>"));
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f  Start an auction for the item in your hand."));
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f  Example: /pa create 100 30s"));
        player.sendMessage("");
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a/pa notifications"));
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f  Toggle auction notifications on or off."));
        player.sendMessage("");
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a/pa claim"));
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f  Claim pending items from ended auctions."));
        player.sendMessage("");
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a/pa activeauctions"));
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f  View all currently active auctions in a GUI."));
        player.sendMessage("");
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a/pa help"));
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f  Show this help message with all commands."));
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&m-------------------------------------"));
    }

    /**
     * Handles the claim command logic for retrieving pending items.
     * @param player the player claiming items
     * @return true to indicate the command was processed
     */
    public boolean handleClaim(Player player) {
        UUID playerUUID = player.getUniqueId();
        ItemStack item = pendingItemsManager.getPendingItems(playerUUID);

        if (item == null) {
            player.sendMessage(plugin.getMessage("no-pending-items"));
            return true;
        }

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
            pendingItemsManager.removePendingItem(playerUUID, item);
            String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : plugin.formatItemName(item.getType().name());
            player.sendMessage(plugin.getMessage("claim-success", "%item%", itemName));
        } else {
            player.sendMessage(plugin.getMessage("inventory-full"));
        }

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
            if ("notifications".startsWith(args[0].toLowerCase())) {
                completions.add("notifications");
            }
            if ("claim".startsWith(args[0].toLowerCase())) {
                completions.add("claim");
            }
            if ("activeauctions".startsWith(args[0].toLowerCase())) {
                completions.add("activeauctions");
            }
            if ("help".startsWith(args[0].toLowerCase())) {
                completions.add("help");
            }
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("create")) {
                completions.add("<price>");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            completions.add("<duration>");
        }

        return completions;
    }

    /**
     * Parses a duration string into seconds.
     * @param durationStr the duration string (e.g., 1d2h3m4s)
     * @return the duration in seconds
     * @throws IllegalArgumentException if the format is invalid
     */
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

    /**
     * Formats a duration in seconds into a human-readable string.
     * @param seconds the duration in seconds
     * @return the formatted duration string
     */
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