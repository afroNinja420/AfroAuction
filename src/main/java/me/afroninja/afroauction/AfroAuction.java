package me.afroninja.afroauction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class AfroAuction extends JavaPlugin {
    private FileConfiguration config;
    private Economy economy;
    private AuctionManager auctionManager;
    private NotificationManager notificationManager;
    private PendingItemsManager pendingItemsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault dependency not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager(this);
        notificationManager = new NotificationManager(this);
        pendingItemsManager = new PendingItemsManager(this);

        auctionManager.loadAuctions();

        getServer().getPluginManager().registerEvents(new AuctionListener(this, auctionManager), this);
        getServer().getPluginManager().registerEvents(new PendingItemsListener(this), this);

        PlayerAuctionCommand auctionCommand = new PlayerAuctionCommand(this, auctionManager, notificationManager);
        getCommand("pa").setExecutor(auctionCommand);
        getCommand("pa").setTabCompleter(auctionCommand);

        getLogger().info("AfroAuction enabled!");
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.saveAuctions();
        }
        getLogger().info("AfroAuction disabled!");
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

    public FileConfiguration getConfig() {
        return config;
    }

    public Economy getEconomy() {
        return economy;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public PendingItemsManager getPendingItemsManager() {
        return pendingItemsManager;
    }

    public String getMessage(String key, String... placeholders) {
        String message = config.getString("messages." + key, "&cMissing message: " + key);
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }

    public String formatItemName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return formatted.toString().trim();
    }
}