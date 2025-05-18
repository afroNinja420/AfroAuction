package me.afroninja.afroauction;

import me.afroninja.afroauction.gui.MainGUI;
import me.afroninja.afroauction.managers.AuctionManager;
import me.afroninja.afroauction.managers.NotificationManager;
import me.afroninja.afroauction.managers.PendingItemsManager;
import me.afroninja.afroauction.listeners.AuctionListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class for the AfroAuction plugin, handling initialization and core functionality.
 */
public class AfroAuction extends JavaPlugin {
    private AuctionManager auctionManager;
    private NotificationManager notificationManager;
    private PendingItemsManager pendingItemsManager;
    private Economy economy;
    private FileConfiguration messages;
    private String currencySymbol;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Load messages.yml
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messages = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(messagesFile);

        // Load currency symbol from config
        currencySymbol = getConfig().getString("currency-symbol", "$");

        // Initialize managers
        auctionManager = new AuctionManager(this);
        notificationManager = new NotificationManager(this);
        pendingItemsManager = new PendingItemsManager(this);

        // Setup economy
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register command
        AuctionCommand command = new AuctionCommand(this, auctionManager, notificationManager, pendingItemsManager);
        getCommand("pa").setExecutor(command);
        getCommand("pa").setTabCompleter(command);

        // Register listeners
        getServer().getPluginManager().registerEvents(new AuctionListener(this, auctionManager, notificationManager), this);

        // Load auctions
        auctionManager.loadAuctions();

        getLogger().info("AfroAuction has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save auctions
        auctionManager.saveAuctions();

        getLogger().info("AfroAuction has been disabled!");
    }

    /**
     * Sets up the economy using Vault.
     * @return true if setup is successful, false otherwise
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
     * Retrieves a formatted message from messages.yml with placeholder replacements.
     * @param key the message key
     * @param placeholders the placeholders to replace
     * @return the formatted message
     */
    public String getMessage(String key, String... placeholders) {
        String message = messages.getString(key, "&cMessage not found: " + key);
        message = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);

        Map<String, String> placeholderMap = new HashMap<>();
        for (int i = 0; i < placeholders.length; i += 2) {
            placeholderMap.put(placeholders[i], placeholders[i + 1]);
        }

        // Apply placeholders
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();
            if (placeholder.equals("%amount%") || placeholder.equals("%price%")) {
                try {
                    double amount = Double.parseDouble(value);
                    value = formatCurrency(amount);
                } catch (NumberFormatException e) {
                    getLogger().warning("Invalid amount format for placeholder " + placeholder + ": " + value);
                }
            }
            message = message.replace(placeholder, value);
        }

        return message;
    }

    /**
     * Formats a currency amount using the configured currency symbol.
     * @param amount the amount to format
     * @return the formatted string (e.g., "$1,234.56" or "€1,234.56" based on config)
     */
    public String formatCurrency(double amount) {
        return String.format("%s%,.2f", currencySymbol, amount);
    }

    /**
     * Formats an item name for display.
     * @param name the raw item name
     * @return the formatted item name
     */
    public String formatItemName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return formatted.toString().trim();
    }

    /**
     * Retrieves the AuctionManager instance.
     * @return the AuctionManager
     */
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    /**
     * Retrieves the NotificationManager instance.
     * @return the NotificationManager
     */
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    /**
     * Retrieves the PendingItemsManager instance.
     * @return the PendingItemsManager
     */
    public PendingItemsManager getPendingItemsManager() {
        return pendingItemsManager;
    }

    /**
     * Retrieves the Economy instance.
     * @return the Economy
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Retrieves the configured currency symbol.
     * @return the currency symbol
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }
}