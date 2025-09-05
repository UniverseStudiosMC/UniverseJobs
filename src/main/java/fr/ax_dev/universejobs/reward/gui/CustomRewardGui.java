package fr.ax_dev.universejobs.reward.gui;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.menu.MenuUtils;
import fr.ax_dev.universejobs.reward.Reward;
import fr.ax_dev.universejobs.reward.RewardManager;
import fr.ax_dev.universejobs.reward.RewardStatus;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Customizable reward GUI that uses GuiConfig for layout.
 */
public class CustomRewardGui implements InventoryHolder {
    
    private final UniverseJobs plugin;
    private final LanguageManager languageManager;
    private final Player player;
    private final Job job;
    private final List<Reward> rewards;
    private final GuiConfig config;
    private final RewardManager rewardManager;
    private Inventory inventory;
    private int currentPage;
    private final int rewardsPerPage;
    
    /**
     * Create a new CustomRewardGui.
     * 
     * @param plugin The plugin instance
     * @param player The player viewing the GUI
     * @param job The job
     * @param rewards The available rewards
     * @param config The GUI configuration
     * @param page The current page
     */
    public CustomRewardGui(UniverseJobs plugin, Player player, Job job, 
                          List<Reward> rewards, GuiConfig config, int page) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.player = player;
        this.job = job;
        this.rewards = rewards;
        this.config = config;
        this.rewardManager = plugin.getRewardManager();
        this.currentPage = page;
        this.rewardsPerPage = config.getRewardSlots().size();
        
        createInventory();
        populateInventory();
    }
    
    /**
     * Create the inventory with the configured size and title.
     */
    private void createInventory() {
        String title = config.getTitle().replace("{job}", job.getName());
        title = MenuUtils.processPlaceholders(player, title);
        this.inventory = Bukkit.createInventory(this, config.getSize(), MessageUtils.colorize(title));
    }
    
    /**
     * Populate the inventory with items.
     */
    private void populateInventory() {
        // Clear inventory
        inventory.clear();
        
        // Add fill items if configured
        addFillItems();
        
        // Add custom items
        addCustomItems();
        
        // Add navigation items
        addNavigationItems();
        
        // Add reward items
        addRewardItems();
    }
    
    /**
     * Add fill items to empty slots.
     */
    @SuppressWarnings("unchecked")
    private void addFillItems() {
        Map<String, Object> fillConfig = config.getFillItems();
        if ((Boolean) fillConfig.getOrDefault("enabled", false)) {
            String fillMaterialName = (String) fillConfig.getOrDefault("material", "GRAY_STAINED_GLASS_PANE");
            String fillName = (String) fillConfig.getOrDefault("name", " ");
            List<Integer> fillSlots = (List<Integer>) fillConfig.getOrDefault("slots", new ArrayList<>());
            
            ItemStack fillItem = ItemBuilder.fromMaterialName(plugin, fillMaterialName)
                    .name(fillName)
                    .build();
            
            if (fillSlots.isEmpty()) {
                // Fill all empty slots
                for (int i = 0; i < inventory.getSize(); i++) {
                    if (inventory.getItem(i) == null) {
                        inventory.setItem(i, fillItem);
                    }
                }
            } else {
                // Fill specific slots
                for (int slot : fillSlots) {
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, fillItem);
                    }
                }
            }
        }
    }
    
    /**
     * Add custom items from configuration.
     */
    private void addCustomItems() {
        for (GuiConfig.GuiItem guiItem : config.getItems().values()) {
            ItemStack item = createItemFromConfig(guiItem);
            
            for (int slot : guiItem.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }
    
    /**
     * Add navigation items.
     */
    private void addNavigationItems() {
        GuiConfig.NavigationConfig nav = config.getNavigation();
        
        // Previous page
        if (nav.getPreviousPage() != null && currentPage > 0) {
            ItemStack item = createItemFromConfig(nav.getPreviousPage());
            for (int slot : nav.getPreviousPage().getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item);
                }
            }
        }
        
        // Next page
        if (nav.getNextPage() != null && hasNextPage()) {
            ItemStack item = createItemFromConfig(nav.getNextPage());
            for (int slot : nav.getNextPage().getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item);
                }
            }
        }
        
        // Close button
        if (nav.getClose() != null) {
            ItemStack item = createItemFromConfig(nav.getClose());
            for (int slot : nav.getClose().getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item);
                }
            }
        }
        
        // Refresh button
        if (nav.getRefresh() != null) {
            ItemStack item = createItemFromConfig(nav.getRefresh());
            for (int slot : nav.getRefresh().getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item);
                }
            }
        }
        
        // Info button
        if (nav.getInfo() != null) {
            ItemStack item = createItemFromConfig(nav.getInfo());
            
            for (int slot : nav.getInfo().getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }
    
    /**
     * Add reward items to the configured slots.
     */
    private void addRewardItems() {
        List<Integer> rewardSlots = config.getRewardSlots();
        int startIndex = currentPage * rewardsPerPage;
        
        for (int i = 0; i < rewardsPerPage && i < rewardSlots.size(); i++) {
            int rewardIndex = startIndex + i;
            if (rewardIndex >= rewards.size()) break;
            
            Reward reward = rewards.get(rewardIndex);
            ItemStack rewardItem = createRewardItem(reward);
            
            int slot = rewardSlots.get(i);
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, rewardItem);
            }
        }
    }
    
    /**
     * Create an ItemStack from a GuiItem configuration.
     */
    private ItemStack createItemFromConfig(GuiConfig.GuiItem guiItem) {
        ItemBuilder builder = ItemBuilder.fromMaterialName(plugin, guiItem.getMaterialName())
                .amount(guiItem.getAmount());
        
        // Display name
        if (!guiItem.getDisplayName().isEmpty()) {
            builder.name(guiItem.getDisplayName());
        }
        
        // Lore
        if (!guiItem.getLore().isEmpty()) {
            builder.lore(guiItem.getLore());
        }
        
        // Custom model data
        if (guiItem.getCustomModelData() > 0) {
            builder.customModelData(guiItem.getCustomModelData());
        }
        
        ItemStack item = builder.build();
        
        // Enchantments
        for (Map.Entry<Enchantment, Integer> entry : guiItem.getEnchantments().entrySet()) {
            item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }
        
        // Glowing effect
        if (guiItem.isGlowing() && guiItem.getEnchantments().isEmpty()) {
            item.addUnsafeEnchantment(Enchantment.LURE, 1);
            ItemMeta glowMeta = item.getItemMeta();
            if (glowMeta != null) {
                glowMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(glowMeta);
            }
        }
        
        return item;
    }
    
    /**
     * Create an ItemStack for a reward.
     */
    private ItemStack createRewardItem(Reward reward) {
        RewardStatus status = rewardManager.getRewardStatus(player, reward);
        GuiConfig.RewardItemConfig rewardConfig = config.getRewardItemConfig();
        
        String statusKey = status.name().toLowerCase();
        String materialName = rewardConfig.getMaterial(statusKey);
        String statusIndicator = rewardConfig.getStatusIndicator(statusKey);
        
        // Create item with configured material
        ItemBuilder builder = ItemBuilder.fromMaterialName(plugin, materialName);
        if (builder == null) {
            builder = new ItemBuilder(plugin, Material.CHEST);
        }
        
        // Format display name
        String displayName = rewardConfig.getNameFormat()
            .replace("{status}", statusIndicator)
            .replace("{name}", reward.getName());
        displayName = MenuUtils.processPlaceholders(player, displayName);
        builder.name(displayName);
        
        // Create lore from configured format
        List<String> lore = new ArrayList<>();
        for (String line : rewardConfig.getLoreFormat()) {
            if (line.contains("{description}")) {
                String desc = MenuUtils.processPlaceholders(player, reward.getDescription());
                lore.add(desc);
            } else if (line.contains("{level}")) {
                String levelLine = line.replace("{level}", String.valueOf(reward.getRequiredLevel()));
                lore.add(MenuUtils.processPlaceholders(player, levelLine));
            } else if (line.contains("{status_description}")) {
                String statusLine = line.replace("{status_description}", status.getDescription());
                lore.add(MenuUtils.processPlaceholders(player, statusLine));
            } else if (line.contains("{repeatable_info}")) {
                if (reward.isRepeatable()) {
                    lore.add(MenuUtils.processPlaceholders(player, rewardConfig.getText("repeatable_yes")));
                    if (reward.getCooldownHours() > 0) {
                        String timeStr = formatTime(reward.getCooldownHours(), rewardConfig.getTimeFormat());
                        String cooldownLine = rewardConfig.getText("cooldown_prefix") + timeStr;
                        lore.add(MenuUtils.processPlaceholders(player, cooldownLine));
                    }
                } else {
                    lore.add(MenuUtils.processPlaceholders(player, rewardConfig.getText("repeatable_no")));
                }
            } else if (line.contains("{click_instruction}")) {
                if (status == RewardStatus.RETRIEVABLE) {
                    lore.add("");
                    String instruction = MenuUtils.processPlaceholders(player, rewardConfig.getClickInstruction());
                    lore.add(instruction);
                }
            } else if (!line.isEmpty()) {
                lore.add(MenuUtils.processPlaceholders(player, line));
            } else {
                lore.add("");
            }
        }
        
        builder.lore(lore);
        return builder.build();
    }
    
    /**
     * Format time using the configured format.
     */
    private String formatTime(long hours, String format) {
        String result = format;
        
        if (hours < 24) {
            result = result.replace("{hours}", String.valueOf(hours))
                          .replace("{days}", "0")
                          .replace("{weeks}", "0");
        } else if (hours < 168) {
            result = result.replace("{hours}", String.valueOf(hours % 24))
                          .replace("{days}", String.valueOf(hours / 24))
                          .replace("{weeks}", "0");
        } else {
            result = result.replace("{hours}", String.valueOf(hours % 24))
                          .replace("{days}", String.valueOf((hours % 168) / 24))
                          .replace("{weeks}", String.valueOf(hours / 168));
        }
        
        if (format.equals("{hours}h")) {
            if (hours < 24) {
                return hours + "h";
            } else if (hours < 168) {
                return (hours / 24) + "d";
            } else {
                return (hours / 168) + "w";
            }
        }
        
        return result;
    }
    
    /**
     * Check if there is a next page.
     */
    private boolean hasNextPage() {
        return (currentPage + 1) * rewardsPerPage < rewards.size();
    }
    
    /**
     * Get the total number of pages.
     */
    private int getTotalPages() {
        return (int) Math.ceil((double) rewards.size() / rewardsPerPage);
    }
    
    /**
     * Open the GUI for the player.
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Check if an inventory belongs to this GUI.
     */
    public boolean isInventory(Inventory inventory) {
        return this.inventory.equals(inventory);
    }
    
    /**
     * Handle click on a specific slot.
     */
    public void handleClick(int slot) {
        List<Integer> rewardSlots = config.getRewardSlots();
        
        // Check if clicked on a reward slot
        int rewardSlotIndex = rewardSlots.indexOf(slot);
        if (rewardSlotIndex != -1) {
            int rewardIndex = currentPage * rewardsPerPage + rewardSlotIndex;
            if (rewardIndex < rewards.size()) {
                Reward reward = rewards.get(rewardIndex);
                handleRewardClick(reward);
                return;
            }
        }
        
        // Check navigation clicks
        GuiConfig.NavigationConfig nav = config.getNavigation();
        
        if (nav.getPreviousPage() != null && nav.getPreviousPage().getSlots().contains(slot)) {
            if (currentPage > 0) {
                // Create new GUI for previous page
                CustomRewardGui newGui = new CustomRewardGui(plugin, player, job, rewards, config, currentPage - 1);
                newGui.open();
            }
        } else if (nav.getNextPage() != null && nav.getNextPage().getSlots().contains(slot)) {
            if (hasNextPage()) {
                // Create new GUI for next page
                CustomRewardGui newGui = new CustomRewardGui(plugin, player, job, rewards, config, currentPage + 1);
                newGui.open();
            }
        } else if (nav.getClose() != null && nav.getClose().getSlots().contains(slot)) {
            player.closeInventory();
        } else if (nav.getRefresh() != null && nav.getRefresh().getSlots().contains(slot)) {
            // Refresh GUI
            populateInventory();
        }
    }
    
    /**
     * Handle clicking on a reward.
     */
    private void handleRewardClick(Reward reward) {
        RewardStatus status = rewardManager.getRewardStatus(player, reward);
        
        if (status == RewardStatus.RETRIEVABLE) {
            if (rewardManager.claimReward(player, reward)) {
                MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.success", "reward", reward.getName()));
                // Refresh GUI to update status
                populateInventory();
            } else {
                MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.failed", "reward", reward.getName()));
            }
        } else if (status == RewardStatus.BLOCKED) {
            // Use the new feedback system with custom messages and sounds
            rewardManager.canClaimReward(player, reward, true);
        } else {
            MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.already-claimed"));
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}