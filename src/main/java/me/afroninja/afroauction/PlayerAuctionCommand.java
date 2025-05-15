package me.afroninja.afroauction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerAuctionCommand implements CommandExecutor {
    private final AfroAuction plugin;
    private final AuctionManager auctionManager;
    private final PendingItemsManager pendingItemsManager;
    private final NotificationManager notificationManager;
    private final AuctionCommand auctionCommand;
    private final AuctionClaimCommand claimCommand;

    public PlayerAuctionCommand(AfroAuction plugin, AuctionManager auctionManager, PendingItemsManager pendingItemsManager, NotificationManager notificationManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.pendingItemsManager = pendingItemsManager;
        this.notificationManager = notificationManager;
        this.auctionCommand = new AuctionCommand(plugin, auctionManager);
        this.claimCommand = new AuctionClaimCommand(plugin, pendingItemsManager);
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
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        switch (subcommand) {
            case "create":
                return auctionCommand.onCommand(sender, command, label, subArgs);
            case "claim":
                return claimCommand.onCommand(sender, command, label, subArgs);
            case "notify":
                UUID playerUUID = player.getUniqueId();
                notificationManager.toggleNotifications(playerUUID);
                boolean enabled = notificationManager.hasNotificationsEnabled(playerUUID);
                player.sendMessage(plugin.getMessage(enabled ? "notify-enabled" : "notify-disabled"));
                return true;
            case "help":
                player.sendMessage(plugin.getMessage("help-header"));
                player.sendMessage(plugin.getMessage("help-create"));
                player.sendMessage(plugin.getMessage("help-claim"));
                player.sendMessage(plugin.getMessage("help-notify"));
                player.sendMessage(plugin.getMessage("help-help"));
                return true;
            default:
                player.sendMessage(plugin.getMessage("invalid-usage"));
                return true;
        }
    }
}