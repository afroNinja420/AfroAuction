package me.afroninja.afroauction;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PendingItemsManager {
    private final AfroAuction plugin;
    private final File pendingFile;
    private final YamlConfiguration pendingConfig;
    private final List<PendingItem> pendingItems;

    public PendingItemsManager(AfroAuction plugin) {
        this.plugin = plugin;
        this.pendingFile = new File(plugin.getDataFolder(), "pending.yml");
        this.pendingConfig = YamlConfiguration.loadConfiguration(pendingFile);
        this.pendingItems = new ArrayList<>();
        loadPendingItems();
    }

    public void addPendingItem(UUID playerUuid, ItemStack item) {
        pendingItems.add(new PendingItem(playerUuid, item));
        savePendingItems();
    }

    public List<ItemStack> getPendingItems(UUID playerUuid) {
        List<ItemStack> items = new ArrayList<>();
        pendingItems.stream()
                .filter(p -> p.playerUuid.equals(playerUuid))
                .forEach(p -> items.add(p.item));
        return items;
    }

    public void clearPendingItems(UUID playerUuid) {
        pendingItems.removeIf(p -> p.playerUuid.equals(playerUuid));
        savePendingItems();
    }

    private void savePendingItems() {
        pendingConfig.set("pending", null);
        int index = 0;
        for (PendingItem pending : pendingItems) {
            String path = "pending." + index;
            pendingConfig.set(path + ".player", pending.playerUuid.toString());
            pendingConfig.set(path + ".item", pending.item);
            index++;
        }
        try {
            pendingConfig.save(pendingFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pending items: " + e.getMessage());
        }
    }

    private void loadPendingItems() {
        pendingItems.clear();
        ConfigurationSection pendingSection = pendingConfig.getConfigurationSection("pending");
        if (pendingSection == null) return;

        for (String key : pendingSection.getKeys(false)) {
            String path = "pending." + key;
            UUID playerUuid = UUID.fromString(pendingConfig.getString(path + ".player"));
            ItemStack item = pendingConfig.getItemStack(path + ".item");
            if (item != null) {
                pendingItems.add(new PendingItem(playerUuid, item));
            }
        }
    }

    private static class PendingItem {
        final UUID playerUuid;
        final ItemStack item;

        PendingItem(UUID playerUuid, ItemStack item) {
            this.playerUuid = playerUuid;
            this.item = item.clone();
        }
    }
}