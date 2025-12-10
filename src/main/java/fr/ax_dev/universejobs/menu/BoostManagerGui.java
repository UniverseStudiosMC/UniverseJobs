package fr.ax_dev.universejobs.menu;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.bonus.BaseBonus;
import fr.ax_dev.universejobs.bonus.MoneyBonus;
import fr.ax_dev.universejobs.bonus.XpBonus;
import fr.ax_dev.universejobs.menu.config.BoostMenuConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.*;
import java.util.stream.Collectors;

public class BoostManagerGui implements InventoryHolder {
    
    private final UniverseJobs plugin;
    private final MiniMessage miniMessage;
    private final BoostMenuConfig config;
    
    public BoostManagerGui(UniverseJobs plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        // Utiliser la configuration centrale depuis MenuConfig
        this.config = plugin.getMenuManager().getMenuConfig().getBoostMenuConfig();
    }
    
    private Inventory currentInventory;
    
    public void openGui(Player player) {
        if (!player.hasPermission("universejobs.admin.boost")) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                plugin.getLanguageManager().getMessage("boost-gui.no-permission"));
            return;
        }
        
        Inventory gui = createGui();
        updateGuiContent(gui);
        
        // Enregistrer ce GUI auprès du MenuManager pour éviter les conflits
        plugin.getMenuManager().registerBoostGui(player, this);
        
        // Son d'ouverture
        playSound(player, config.getSoundsConfig().openSound);
        
        player.openInventory(gui);
        startAutoUpdate(player, gui);
    }
    
    private Inventory createGui() {
        Component title = miniMessage.deserialize("<!italic>" + config.getTitle());
        this.currentInventory = Bukkit.createInventory(this, config.getSize(), title);
        return currentInventory;
    }
    
    private void updateGuiContent(Inventory gui) {
        gui.clear();
        
        // Récupérer tous les boosts actifs triés par ordre de création
        List<XpBonus> xpBoosts = plugin.getBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getBonusManager().getBoostById(id))
            .filter(Objects::nonNull)
            .filter(BaseBonus::isActive)
            .sorted(Comparator.comparingLong(BaseBonus::getStartTime))
            .collect(Collectors.toList());
            
        List<MoneyBonus> moneyBoosts = plugin.getMoneyBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getMoneyBonusManager().getBoostById(id))
            .filter(Objects::nonNull)
            .filter(BaseBonus::isActive)
            .sorted(Comparator.comparingLong(BaseBonus::getStartTime))
            .collect(Collectors.toList());
        
        // Ajouter les items custom
        addCustomItems(gui, xpBoosts.size(), moneyBoosts.size());
        
        // Ajouter les items de navigation
        addNavigationItems(gui, xpBoosts.size(), moneyBoosts.size());
        
        // Remplir avec des items de fond
        fillWithGlass(gui);
        
        // Placer les boosts XP
        if (config.getXpBoostConfig() != null && config.getXpBoostConfig().enabled) {
            placeXpBoosts(gui, xpBoosts);
        }
        
        // Placer les boosts Money
        if (config.getMoneyBoostConfig() != null && config.getMoneyBoostConfig().enabled) {
            placeMoneyBoosts(gui, moneyBoosts);
        }
    }
    
    private void fillWithGlass(Inventory gui) {
        if (config.getFillerConfig() == null || !config.getFillerConfig().enabled) return;
        
        ItemStack filler = createFillerItem();
        List<Integer> slots = config.getFillerConfig().slots;
        
        if (slots.isEmpty()) {
            // Remplir tous les slots vides
            for (int i = 0; i < gui.getSize(); i++) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, filler);
                }
            }
        } else {
            // Remplir les slots spécifiques
            for (int slot : slots) {
                if (slot >= 0 && slot < gui.getSize()) {
                    gui.setItem(slot, filler);
                }
            }
        }
    }
    
    private ItemStack createFillerItem() {
        BoostMenuConfig.FillerConfig fillerConfig = config.getFillerConfig();
        ItemStack item = new ItemStack(fillerConfig.material);
        ItemMeta meta = item.getItemMeta();
        
        Component displayName = miniMessage.deserialize("<!italic>" + fillerConfig.displayName);
        meta.displayName(displayName);
        
        if (!fillerConfig.lore.isEmpty()) {
            List<Component> lore = fillerConfig.lore.stream()
                .map(line -> miniMessage.deserialize("<!italic>" + line))
                .collect(Collectors.toList());
            meta.lore(lore);
        }
        
        item.setItemMeta(meta);
        return item;
    }

    private void placeXpBoosts(Inventory gui, List<XpBonus> boosts) {
        List<Integer> slots = config.getXpBoostConfig().slots;
        for (int i = 0; i < Math.min(boosts.size(), slots.size()); i++) {
            XpBonus boost = boosts.get(i);
            int slot = slots.get(i);
            ItemStack item = createXpBoostItem(boost);
            gui.setItem(slot, item);
        }
    }

    private void placeMoneyBoosts(Inventory gui, List<MoneyBonus> boosts) {
        List<Integer> slots = config.getMoneyBoostConfig().slots;
        for (int i = 0; i < Math.min(boosts.size(), slots.size()); i++) {
            MoneyBonus boost = boosts.get(i);
            int slot = slots.get(i);
            ItemStack item = createMoneyBoostItem(boost);
            gui.setItem(slot, item);
        }
    }

    private ItemStack createXpBoostItem(XpBonus boost) {
        BoostMenuConfig.BoostItemConfig itemConfig = config.getXpBoostConfig();
        ItemStack item = new ItemStack(itemConfig.material);
        ItemMeta meta = item.getItemMeta();

        // Préparer les variables pour le formatage
        Map<String, String> variables = createVariableMap(boost);

        // Formater le nom avec <!italic>
        String formattedName = formatString(itemConfig.displayName, variables);
        Component displayName = miniMessage.deserialize("<!italic>" + formattedName);
        meta.displayName(displayName);

        // Formater la lore avec <!italic>
        List<Component> lore = itemConfig.lore.stream()
            .map(line -> formatString(line, variables))
            .map(line -> miniMessage.deserialize("<!italic>" + line))
            .collect(Collectors.toList());
        meta.lore(lore);

        if (itemConfig.modelData != null && !itemConfig.modelData.isEmpty()) {
            itemConfig.modelData.applyTo(meta);
        }

        // Glow effect
        if (itemConfig.glow) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Stocker l'ID du boost dans les métadonnées personnalisées
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "boost_id"),
            org.bukkit.persistence.PersistentDataType.STRING,
            boost.getBoostId()
        );

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMoneyBoostItem(MoneyBonus boost) {
        BoostMenuConfig.BoostItemConfig itemConfig = config.getMoneyBoostConfig();
        ItemStack item = new ItemStack(itemConfig.material);
        ItemMeta meta = item.getItemMeta();

        // Préparer les variables pour le formatage
        Map<String, String> variables = createVariableMap(boost);

        // Formater le nom avec <!italic>
        String formattedName = formatString(itemConfig.displayName, variables);
        Component displayName = miniMessage.deserialize("<!italic>" + formattedName);
        meta.displayName(displayName);

        // Formater la lore avec <!italic>
        List<Component> lore = itemConfig.lore.stream()
            .map(line -> formatString(line, variables))
            .map(line -> miniMessage.deserialize("<!italic>" + line))
            .collect(Collectors.toList());
        meta.lore(lore);

        if (itemConfig.modelData != null && !itemConfig.modelData.isEmpty()) {
            itemConfig.modelData.applyTo(meta);
        }

        // Glow effect
        if (itemConfig.glow) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Stocker l'ID du boost dans les métadonnées personnalisées
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "boost_id"),
            org.bukkit.persistence.PersistentDataType.STRING,
            boost.getBoostId()
        );

        item.setItemMeta(meta);
        return item;
    }
    
    private Map<String, String> createVariableMap(BaseBonus boost) {
        Map<String, String> variables = new HashMap<>();
        
        variables.put("boost_id", boost.getBoostId());
        variables.put("multiplier", String.valueOf(boost.getMultiplier()));
        variables.put("remaining_time", boost.getRemainingTimeFormatted());
        
        // Player name
        String playerName;
        if (boost.isGlobal()) {
            playerName = "All Players";
        } else {
            Player player = Bukkit.getPlayer(boost.getPlayerId());
            playerName = player != null ? player.getName() : "Unknown";
        }
        variables.put("player_name", playerName);
        
        // Job info
        String jobInfo = boost.getJobId() == null ? "All Jobs" : boost.getJobId();
        variables.put("job_info", jobInfo);
        
        // Action info
        String actionInfo = formatActionInfo(boost.getActionType(), boost.getActionId());
        variables.put("action_info", actionInfo);
        
        return variables;
    }
    
    private String formatActionInfo(String actionType, String actionId) {
        if (actionType == null) {
            return "All Actions";
        }
        
        if (actionId == null) {
            return actionType;
        }
        
        return actionType + ":" + actionId;
    }
    
    private String formatString(String format, Map<String, String> variables) {
        String result = format;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
    
    private void startAutoUpdate(Player player, Inventory gui) {
        stopAutoUpdate(player);

        if (!plugin.isEnabled()) {
            return;
        }

        if (config.getAutoRefreshConfig() == null || !config.getAutoRefreshConfig().enabled) {
            return;
        }

        long intervalTicks = config.getAutoRefreshConfig().interval;

        if (intervalTicks <= 0) {
            intervalTicks = 20L;
        }

        final long finalIntervalTicks = intervalTicks;
        plugin.getFoliaManager().runTimer(() -> {
            if (plugin.isEnabled() && player.isOnline() && player.getOpenInventory().getTopInventory().equals(gui)) {
                updateGuiContent(gui);
            } else {
                stopAutoUpdate(player);
            }
        }, finalIntervalTicks, finalIntervalTicks);
    }
    
    private void stopAutoUpdate(Player player) {
    }
    
    public void handleClick(Player player, int slot, boolean isRightClick) {
        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || item.getItemMeta() == null) return;
        
        // Vérifier les items de navigation
        if (config.getNavigationConfig() != null) {
            BoostMenuConfig.NavigationConfig nav = config.getNavigationConfig();
            
            // Close button
            if (nav.closeItem.enabled && nav.closeItem.slots.contains(slot)) {
                playSound(player, config.getSoundsConfig().closeSound);
                player.closeInventory();
                return;
            }
            
            // Refresh button
            if (nav.refreshItem.enabled && nav.refreshItem.slots.contains(slot)) {
                fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                    plugin.getLanguageManager().getMessage("boost-gui.refresh-clicked"));
                updateGuiContent(player.getOpenInventory().getTopInventory());
                return;
            }
        }

        if (!isRightClick) return;

        if (!player.hasPermission("universejobs.admin.boost")) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                plugin.getLanguageManager().getMessage("boost-gui.no-permission"));
            return;
        }
        
        // Récupérer l'ID du boost depuis les métadonnées
        String boostId = item.getItemMeta().getPersistentDataContainer().get(
            new org.bukkit.NamespacedKey(plugin, "boost_id"), 
            org.bukkit.persistence.PersistentDataType.STRING
        );
        
        if (boostId != null) {
            boolean removedXp = plugin.getBonusManager().removeBoostById(boostId);
            boolean removedMoney = plugin.getMoneyBonusManager().removeBoostById(boostId);

            if (removedXp || removedMoney) {
                String type = removedXp ? "XP" : "Money";
                updateGuiContent(player.getOpenInventory().getTopInventory());

                playSound(player, config.getSoundsConfig().removeBoostSound);

                fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                    plugin.getLanguageManager().getMessage("boost-gui.removed",
                        "type", type,
                        "boost_id", boostId));
            }
        }
    }
    
    public void onInventoryClose(Player player) {
        stopAutoUpdate(player);
        playSound(player, config.getSoundsConfig().closeSound);
    }
    
    private void addCustomItems(Inventory gui, int xpCount, int moneyCount) {
        if (config.getCustomItems() == null) return;
        
        for (BoostMenuConfig.CustomItemConfig customItem : config.getCustomItems().values()) {
            if (!customItem.enabled) continue;
            
            ItemStack item = new ItemStack(customItem.material);
            ItemMeta meta = item.getItemMeta();
            
            // Display name avec <!italic>
            Component displayName = miniMessage.deserialize("<!italic>" + customItem.displayName);
            meta.displayName(displayName);
            
            // Lore avec <!italic>
            if (!customItem.lore.isEmpty()) {
                List<Component> lore = customItem.lore.stream()
                    .map(line -> miniMessage.deserialize("<!italic>" + line))
                    .collect(Collectors.toList());
                meta.lore(lore);
            }
            
            item.setItemMeta(meta);
            
            // Placer l'item dans les slots spécifiés
            for (int slot : customItem.slots) {
                if (slot >= 0 && slot < gui.getSize()) {
                    gui.setItem(slot, item);
                }
            }
        }
    }
    
    private void addNavigationItems(Inventory gui, int xpCount, int moneyCount) {
        if (config.getNavigationConfig() == null) return;
        
        BoostMenuConfig.NavigationConfig nav = config.getNavigationConfig();
        
        // Close button
        if (nav.closeItem.enabled) {
            ItemStack item = createNavigationItem(nav.closeItem);
            for (int slot : nav.closeItem.slots) {
                if (slot >= 0 && slot < gui.getSize()) {
                    gui.setItem(slot, item);
                }
            }
        }
        
        // Refresh button
        if (nav.refreshItem.enabled) {
            ItemStack item = createNavigationItem(nav.refreshItem);
            for (int slot : nav.refreshItem.slots) {
                if (slot >= 0 && slot < gui.getSize()) {
                    gui.setItem(slot, item);
                }
            }
        }
        
        // Info button
        if (nav.infoItem.enabled) {
            ItemStack item = createNavigationItem(nav.infoItem);
            ItemMeta meta = item.getItemMeta();
            
            // Remplacer les variables dans la lore
            List<Component> lore = new ArrayList<>();
            for (String line : nav.infoItem.lore) {
                String formatted = line
                    .replace("{xp_count}", String.valueOf(xpCount))
                    .replace("{money_count}", String.valueOf(moneyCount))
                    .replace("{total_count}", String.valueOf(xpCount + moneyCount));
                lore.add(miniMessage.deserialize("<!italic>" + formatted));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            
            for (int slot : nav.infoItem.slots) {
                if (slot >= 0 && slot < gui.getSize()) {
                    gui.setItem(slot, item);
                }
            }
        }
    }
    
    private ItemStack createNavigationItem(BoostMenuConfig.NavigationItemConfig itemConfig) {
        ItemStack item = new ItemStack(itemConfig.material);
        ItemMeta meta = item.getItemMeta();
        
        // Display name avec <!italic>
        Component displayName = miniMessage.deserialize("<!italic>" + itemConfig.displayName);
        meta.displayName(displayName);
        
        // Lore avec <!italic>
        if (!itemConfig.lore.isEmpty()) {
            List<Component> lore = itemConfig.lore.stream()
                .map(line -> miniMessage.deserialize("<!italic>" + line))
                .collect(Collectors.toList());
            meta.lore(lore);
        }
        
        // Glow effect
        if (itemConfig.glow) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private void playSound(Player player, BoostMenuConfig.SoundConfig soundConfig) {
        if (soundConfig != null && soundConfig.enabled && soundConfig.sound != null) {
            player.playSound(player.getLocation(), soundConfig.sound, soundConfig.volume, soundConfig.pitch);
        }
    }
    
    public void reloadConfig() {
        // La configuration est rechargée via MenuConfig maintenant
        plugin.getMenuManager().reloadConfigurations();
    }
    
    @Override
    public Inventory getInventory() {
        return currentInventory;
    }
    
    public boolean isInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() == this;
    }
}