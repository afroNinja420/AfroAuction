package me.afroninja.afroauction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class AfroAuction extends JavaPlugin {
    private Economy economy;
    private AuctionManager auctionManager;
    private PendingItemsManager pendingItemsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupConfigDefaults();
        if (!setupEconomy()) {
            getLogger().severe("Vault or economy plugin not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager(this);
        pendingItemsManager = new PendingItemsManager(this);
        getServer().getPluginManager().registerEvents(new AuctionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getCommand("createauction").setExecutor(new AuctionCommand(this, auctionManager));
        getCommand("auctionclaim").setExecutor(new AuctionClaimCommand(this));
        getServer().getPluginManager().registerEvents(new ChatBidListener(this), this);

        auctionManager.loadAuctions();
        pendingItemsManager.loadPendingItems();
        getLogger().info("AfroAuction enabled!");
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.saveAuctions();
        }
        if (pendingItemsManager != null) {
            pendingItemsManager.savePendingItems();
        }
        getLogger().info("AfroAuction disabled!");
    }

    private void setupConfigDefaults() {
        FileConfiguration config = getConfig();

        // Economy Settings
        config.addDefault("min-bid-increment", 10.0);
        config.addDefault("bid-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");

        // Auction Settings
        config.addDefault("broadcast-auction-end", true);
        config.addDefault("max-auction-duration", 3600);
        config.addDefault("min-auction-duration", 30);
        config.addDefault("max-start-price", 1000000.0);
        config.addDefault("min-start-price", 1.0);
        config.addDefault("allow-self-bidding", false);
        config.addDefault("auction-cooldown", 60);
        config.addDefault("max-active-auctions", 5);

        // Hologram Settings
        config.addDefault("hologram-base-height", 1.7);
        config.addDefault("hologram-info-offset", 0.5);
        config.addDefault("hologram-item-offset", 0.25);
        config.addDefault("hologram-update-interval", 20);
        config.addDefault("hologram-visibility-range", 32);

        // Messages
        config.addDefault("messages.player-only", "&cOnly players can use this command!");
        config.addDefault("messages.no-permission", "&cYou don't have permission to %action%!");
        config.addDefault("messages.invalid-usage", "&cUsage: /createauction <startPrice> <durationSeconds>");
        config.addDefault("messages.invalid-numbers", "&cStart price and duration must be numbers!");
        config.addDefault("messages.invalid-price", "&cStart price must be between %min_price% and %max_price%!");
        config.addDefault("messages.invalid-duration", "&cDuration must be between %min_duration% and %max_duration% seconds!");
        config.addDefault("messages.no-item", "&cYou must be holding an item to auction!");
        config.addDefault("messages.not-chest", "&cYou must be looking at a chest!");
        config.addDefault("messages.chest-in-use", "&cThis chest is already an auction!");
        config.addDefault("messages.cooldown", "&cYou must wait %cooldown% seconds before creating another auction!");
        config.addDefault("messages.max-auctions", "&cYou have reached the maximum of %max_auctions% active auctions!");
        config.addDefault("messages.auction-created", "&aAuction created for %item% at $%price% for %duration% seconds!");
        config.addDefault("messages.bid-too-low", "&cYour bid must be at least $%min_bid%!");
        config.addDefault("messages.insufficient-funds", "&cYou don't have enough money to bid $%bid%!");
        config.addDefault("messages.self-bid-disallowed", "&cYou cannot bid on your own auction!");
        config.addDefault("messages.bid-placed", "&aBid of $%bid% placed on %item%!");
        config.addDefault("messages.outbid", "&eYou were outbid on %item%!");
        config.addDefault("messages.auction-ended", "&6%winner% won %item% for $%price%!");
        config.addDefault("messages.winner-received", "&aYou received %item% from the auction!");
        config.addDefault("messages.winner-inventory-full", "&eYour inventory is full! Use /auctionclaim to receive %item%.");
        config.addDefault("messages.creator-paid", "&aYour auction for %item% sold for $%price% to %winner%!");
        config.addDefault("messages.no-bids", "&eYour auction for %item% received no bids. Use /auctionclaim to retrieve it.");
        config.addDefault("messages.claim-no-items", "&cYou have no items to claim!");
        config.addDefault("messages.claim-success", "&aYou claimed %item%!");
        config.addDefault("messages.hologram-item", "&e%item%");
        config.addDefault("messages.hologram-info", "&aPrice: $%price% | Time: %time%s");

        config.options().copyDefaults(true);
        saveConfig();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public PendingItemsManager getPendingItemsManager() {
        return pendingItemsManager;
    }
}