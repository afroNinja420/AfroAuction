package me.afroninja.afroauction;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionClaimCommand implements CommandExecutor {
    private final AfroAuction plugin;

    public AuctionClaimCommand(AfroAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }
        Player player = (Player) sender;

        List<ItemStack> pendingItems = plugin.getPendingItemsManager().getPendingItems(player.getUniqueId());
        if (pendingItems.isEmpty()) {
            player.sendMessage(getMessage("claim-no-items"));
            return true;
        }

        for (ItemStack item : pendingItems) {
            String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
            Map<Integer, ItemStack> undelivered = player.getInventory().addItem(item);
            if (undelivered.isEmpty()) {
                player.sendMessage(getMessage("claim-success", Map.of("item", itemName)));
            } else {
                player.sendMessage(getMessage("winner-inventory-full", Map.of("item", itemName)));
            }
        }

        plugin.getPendingItemsManager().clearPendingItems(player.getUniqueId());
        return true;
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