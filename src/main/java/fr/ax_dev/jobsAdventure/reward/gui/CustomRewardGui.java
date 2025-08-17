package fr.ax_dev.jobsAdventure.reward.gui;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.job.Job;
import fr.ax_dev.jobsAdventure.reward.Reward;
import fr.ax_dev.jobsAdventure.reward.RewardManager;
import fr.ax_dev.jobsAdventure.reward.RewardStatus;
import fr.ax_dev.jobsAdventure.utils.MessageUtils;
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
    
    private final JobsAdventure plugin;
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
    public CustomRewardGui(JobsAdventure plugin, Player player, Job job, 
                          List<Reward> rewards, GuiConfig config, int page) {
        this.plugin = plugin;
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
        String title = MessageUtils.colorize(config.getTitle().replace("{job}", job.getName()));
        this.inventory = Bukkit.createInventory(this, config.getSize(), title);
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
            // Add job info to lore
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>(meta.getLore() != null ? meta.getLore() : new ArrayList<>());
                lore.add("");
                lore.add(MessageUtils.colorize("&7Job: &e" + job.getName()));
                lore.add(MessageUtils.colorize("&7Total Rewards: &e" + rewards.size()));
                lore.add(MessageUtils.colorize("&7Page: &e" + (currentPage + 1) + "/" + getTotalPages()));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            
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
        
        // Use ItemBuilder to create the reward item
        ItemBuilder builder = new ItemBuilder(plugin, Material.CHEST)
            .name(MessageUtils.colorize(reward.getName()));
        
        // Add status indicator to name
        String statusIndicator = status.getIndicator();
        builder.name(statusIndicator + " " + MessageUtils.colorize(reward.getName()));
        
        // Add status information to lore
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize(reward.getDescription()));
        lore.add("");
        lore.add(MessageUtils.colorize("&7Required Level: &e" + reward.getRequiredLevel()));
        lore.add(MessageUtils.colorize("&7Status: " + status.getDescription()));
        
        if (reward.isRepeatable()) {
            lore.add(MessageUtils.colorize("&7Repeatable: &aYes"));
            if (reward.getCooldownHours() > 0) {
                lore.add(MessageUtils.colorize("&7Cooldown: &e" + formatTimeHours(reward.getCooldownHours())));
            }
        } else {
            lore.add(MessageUtils.colorize("&7Repeatable: &cNo"));
        }
        
        // Add click instruction
        if (status == RewardStatus.RETRIEVABLE) {
            lore.add("");
            lore.add(MessageUtils.colorize("&a▶ Click to claim!"));
        }
        
        builder.lore(lore);
        
        // Set material based on status
        Material material = switch (status) {
            case RETRIEVABLE -> Material.LIME_SHULKER_BOX;
            case BLOCKED -> Material.RED_SHULKER_BOX;
            case RETRIEVED -> Material.GRAY_SHULKER_BOX;
        };
        
        // Create final item with correct material
        ItemStack finalItem = new ItemStack(material);
        ItemMeta finalMeta = finalItem.getItemMeta();
        if (finalMeta != null) {
            finalMeta.setDisplayName(statusIndicator + " " + MessageUtils.colorize(reward.getName()));
            finalMeta.setLore(lore);
            finalItem.setItemMeta(finalMeta);
        }
        
        return finalItem;
    }
    
    /**
     * Format time in hours to a readable string.
     */
    private String formatTimeHours(long hours) {
        if (hours < 24) {
            return hours + "h";
        } else if (hours < 168) { // 7 days
            return (hours / 24) + "d";
        } else {
            return (hours / 168) + "w";
        }
    }
    
    /**
     * Format time in seconds to a readable string.
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "h";
        } else {
            return (seconds / 86400) + "d";
        }
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
                MessageUtils.sendMessage(player, "&aReward claimed successfully!");
                // Refresh GUI to update status
                populateInventory();
            } else {
                MessageUtils.sendMessage(player, "&cFailed to claim reward.");
            }
        } else if (status == RewardStatus.BLOCKED) {
            // Use the new feedback system with custom messages and sounds
            rewardManager.canClaimReward(player, reward, true);
        } else {
            MessageUtils.sendMessage(player, "&7You have already claimed this reward.");
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}