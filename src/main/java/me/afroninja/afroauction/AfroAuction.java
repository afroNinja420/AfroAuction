package me.afroninja.afroauction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // Pattern to match color codes (§ followed by 0-9, a-f, k-o, r)
        Pattern colorPattern = Pattern.compile("§[0-9a-fk-or]");
        String lastColor = "&f"; // Default to white if no color found

        // Replace placeholders
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            if (placeholder.equals("%item%")) {
                // Find the last color code before %item%
                Matcher matcher = colorPattern.matcher(message);
                while (matcher.find()) {
                    if (message.indexOf(placeholder, matcher.start()) >= matcher.start()) {
                        lastColor = matcher.group();
                    }
                }
                // Format item name: title case with spaces if no custom name
                String itemName = value;
                if (!itemName.contains("{")) { // No custom name (JSON)
                    itemName = formatItemName(itemName);
                }
                // Apply default color if no custom color, otherwise use existing color
                if (!itemName.contains("§")) {
                    String defaultItemColor = config.getString("default-item-color", "&b").replace("&", "§"); // Default aqua
                    itemName = defaultItemColor + itemName + "§r" + lastColor.replace("§", "&");
                } else {
                    itemName = itemName + "§r" + lastColor.replace("§", "&");
                }
                message = message.replace(placeholder, itemName);
            } else {
                message = message.replace(placeholder, value);
            }
        }

        return message.replace("&", "§");
    }

    public String formatItemName(String itemName) {
        // Replace underscores with spaces and convert to title case
        String[] words = itemName.replace("_", " ").toLowerCase().split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            formatted.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return formatted.toString().trim();
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