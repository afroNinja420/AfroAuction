package me.afroninja.afroauction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class AfroAuction extends JavaPlugin {
    private Economy economy;
    private AuctionManager auctionManager;
    private PendingItemsManager pendingItemsManager;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        // Initialize configuration
        initializeConfig();

        // Initialize Vault economy
        if (!setupEconomy()) {
            getLogger().severe("Vault or an economy plugin is not installed! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        auctionManager = new AuctionManager(this);
        pendingItemsManager = new PendingItemsManager(this);

        // Register commands
        getCommand("createauction").setExecutor(new AuctionCommand(this, auctionManager));
        getCommand("auctionclaim").setExecutor(new AuctionClaimCommand(this, pendingItemsManager));

        // Register listeners
        getServer().getPluginManager().registerEvents(new AuctionListener(this, auctionManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(pendingItemsManager), this);

        // Load data
        auctionManager.loadAuctions();
        pendingItemsManager.loadPendingItems();

        getLogger().info("AfroAuction enabled successfully!");
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

    private void initializeConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
            getLogger().info("Generated new config.yml at " + configFile.getAbsolutePath());
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        try {
            config.load(configFile);
            getLogger().info("Loaded config.yml successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to load config.yml: " + e.getMessage());
            e.printStackTrace();
        }
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

    public String getMessage(String key, String... placeholders) {
        String message = config.getString("messages." + key);
        if (message == null) {
            getLogger().warning("Missing message key: messages." + key);
            message = "&cMissing message: " + key;
        }
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        return message.replace("&", "§");
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

    @Override
    public FileConfiguration getConfig() {
        return config;
    }
}