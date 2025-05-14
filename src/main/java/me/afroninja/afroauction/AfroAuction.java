package me.afroninja.afroauction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class AfroAuction extends JavaPlugin {
    private AuctionManager auctionManager;
    private PendingItemsManager pendingItemsManager;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        auctionManager = new AuctionManager(this);
        pendingItemsManager = new PendingItemsManager(this);
        getCommand("createauction").setExecutor(new AuctionCommand(this, auctionManager));
        getCommand("auctionclaim").setExecutor(new AuctionClaimCommand(this, pendingItemsManager));
        getServer().getPluginManager().registerEvents(new AuctionListener(this, auctionManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(pendingItemsManager), this);
        auctionManager.loadAuctions();
        getLogger().info("AfroAuction has been enabled!");
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) auctionManager.saveAuctions();
        if (pendingItemsManager != null) pendingItemsManager.savePendingItems();
        getLogger().info("AfroAuction has been disabled!");
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

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public PendingItemsManager getPendingItemsManager() {
        return pendingItemsManager;
    }

    public Economy getEconomy() {
        return economy;
    }
}