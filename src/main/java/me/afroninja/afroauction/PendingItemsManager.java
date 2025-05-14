package me.afroninja.afroauction;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PendingItemsManager {
    private final AfroAuction plugin;
    private final File pendingFile;
    private final YamlConfiguration pendingConfig;
    private final Map<UUID, List<ItemStack>> pendingItems = new HashMap<>();

    public PendingItemsManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.pendingFile = new File(plugin.getDataFolder(), "pending.yml");
        this.pendingConfig = YamlConfiguration.loadConfiguration(pendingFile);
    }

    public void addPendingItem(UUID player, ItemStack item) {
        pendingItems.computeIfAbsent(player, k -> new ArrayList<>()).add(item);
        savePendingItems();
    }

    public List<ItemStack> getPendingItems(UUID player) {
        return new ArrayList<>(pendingItems.getOrDefault(player, new ArrayList<>()));
    }

    public void clearPendingItems(UUID player) {
        pendingItems.remove(player);
        savePendingItems();
    }

    public void savePendingItems() {
        pendingConfig.set("pending", null); // Clear existing data
        for (Map.Entry<UUID, List<ItemStack>> entry : pendingItems.entrySet()) {
            String path = "pending." + entry.getKey().toString();
            List<ItemStack> items = entry.getValue();
            for (int i = 0; i < items.size(); i++) {
                pendingConfig.set(path + "." + i, items.get(i));
            }
        }
        try {
            pendingConfig.save(pendingFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pending items: " + e.getMessage());
        }
    }

    public void loadPendingItems() {
        pendingItems.clear();
        ConfigurationSection pendingSection = pendingConfig.getConfigurationSection("pending");
        if (pendingSection == null) {
            return;
        }
        for (String uuidStr : pendingSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<ItemStack> items = new ArrayList<>();
                ConfigurationSection playerSection = pendingSection.getConfigurationSection(uuidStr);
                if (playerSection != null) {
                    for (String key : playerSection.getKeys(false)) {
                        ItemStack item = playerSection.getItemStack(key);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                }
                if (!items.isEmpty()) {
                    pendingItems.put(uuid, items);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in pending.yml: " + uuidStr);
            }
        }
    }
}