package me.afroninja.afroauction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class AfroAuction extends JavaPlugin {
    private Economy economy;
    private AuctionManager auctionManager;
    private PendingItemsManager pendingItemsManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Setup economy
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        auctionManager = new AuctionManager(this);
        pendingItemsManager = new PendingItemsManager(this);

        // Register commands
        getCommand("createauction").setExecutor(new AuctionCommand(this, auctionManager));
        getCommand("auctionclaim").setExecutor(new AuctionClaimCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new AuctionListener(this, auctionManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(pendingItemsManager), this);

        // Load pending items
        pendingItemsManager.loadPendingItems();

        // Load saved auctions
        auctionManager.loadAuctions();
    }

    @Override
    public void onDisable() {
        // Save auctions
        auctionManager.saveAuctions();
        // Save pending items
        pendingItemsManager.savePendingItems();
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