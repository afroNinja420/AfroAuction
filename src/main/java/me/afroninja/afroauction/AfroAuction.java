package me.afroninja.afroauction;

import me.afroninja.afroauction.listeners.AuctionListener;
import me.afroninja.afroauction.listeners.ChatBidListener;
import me.afroninja.afroauction.listeners.PlayerJoinListener;
import me.afroninja.afroauction.managers.AuctionManager;
import me.afroninja.afroauction.managers.NotificationManager;
import me.afroninja.afroauction.managers.PendingItemsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * The main class for the AfroAuction plugin, responsible for initialization, lifecycle management,
 * and providing access to core components and utility methods.
 */
public class AfroAuction extends JavaPlugin {
    private FileConfiguration config;
    private Economy economy;
    private AuctionManager auctionManager;
    private NotificationManager notificationManager;
    private PendingItemsManager pendingItemsManager;

    @Override
    public void onEnable() {
        // Ensure the config file exists and is loaded
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        if (config == null) {
            getLogger().severe("Failed to load config.yml, using default configuration.");
            config = getConfig(); // Fallback to default config
            if (config == null) {
                getLogger().severe("Critical error: Configuration is null. Disabling plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        if (!setupEconomy()) {
            getLogger().severe("Vault dependency not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager(this);
        notificationManager = new NotificationManager(this);
        pendingItemsManager = new PendingItemsManager(this);

        auctionManager.loadAuctions();
        notificationManager.loadNotificationSettings();
        pendingItemsManager.loadPendingItems();

        getServer().getPluginManager().registerEvents(new AuctionListener(this, auctionManager), this);
        getServer().getPluginManager().registerEvents(new ChatBidListener(this, auctionManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, pendingItemsManager), this);

        AuctionCommand auctionCommand = new AuctionCommand(this, auctionManager, notificationManager, pendingItemsManager);
        getCommand("pa").setExecutor(auctionCommand);
        getCommand("pa").setTabCompleter(auctionCommand);
        getCommand("claim").setExecutor(auctionCommand);

        getLogger().info("AfroAuction enabled!");
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.saveAuctions();
        }
        if (notificationManager != null) {
            notificationManager.saveNotificationSettings();
        }
        if (pendingItemsManager != null) {
            pendingItemsManager.savePendingItems();
        }
        getLogger().info("AfroAuction disabled!");
    }

    /**
     * Sets up the Vault economy provider if available.
     * @return true if economy setup is successful, false otherwise
     */
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

    /**
     * Retrieves the plugin's configuration.
     * @return the FileConfiguration instance
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Retrieves the Vault economy instance.
     * @return the Economy instance
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Retrieves the AuctionManager instance.
     * @return the AuctionManager instance
     */
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    /**
     * Retrieves the NotificationManager instance.
     * @return the NotificationManager instance
     */
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    /**
     * Retrieves the PendingItemsManager instance.
     * @return the PendingItemsManager instance
     */
    public PendingItemsManager getPendingItemsManager() {
        return pendingItemsManager;
    }

    /**
     * Retrieves a formatted message from the configuration with placeholders replaced.
     * @param key the message key
     * @param placeholders variable number of placeholder key-value pairs
     * @return the formatted message
     */
    public String getMessage(String key, String... placeholders) {
        String message = config.getString("messages." + key, "&cMissing message: " + key);
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Formats an item name by capitalizing each word.
     * @param name the item name to format
     * @return the formatted item name
     */
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