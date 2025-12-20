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
        this.rewardsPerPage = config.getRewardSlots(page).size();
        
        createInventory();
        populateInventory();
    }
    
    /**
     * Create the inventory with the configured size and title.
     */
    private void createInventory() {
        String title = config.getTitle()
                .replace("{job}", job.getName())
                .replace("{prefix}", job.getPrefix())
                .replace("{suffix}", job.getSuffix());
        title = MenuUtils.processPlaceholders(player, title);
        this.inventory = Bukkit.createInventory(this, config.getSize(), MessageUtils.colorize(title));
    }
    
    /**
     * Populate the inventory with items.
     */
    private void populateInventory() {
        inventory.clear();

        addCustomItems();
        addNavigationItems();
        addRewardItems();
        addFillItems();
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

        // Back button
        if (nav.getBack() != null) {
            ItemStack item = createItemFromConfig(nav.getBack());
            for (int slot : nav.getBack().getSlots()) {
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
        List<Integer> rewardSlots = config.getRewardSlots(currentPage);
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
        ItemBuilder builder;

        // Check if it's a player head with texture
        if (!guiItem.getPlayerHead().isEmpty()) {
            builder = new ItemBuilder(plugin, Material.PLAYER_HEAD)
                    .playerHead(guiItem.getPlayerHead())
                    .amount(guiItem.getAmount());
        } else {
            builder = ItemBuilder.fromMaterialName(plugin, guiItem.getMaterialName())
                    .amount(guiItem.getAmount());
        }

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
        
        // Apply item flags
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (guiItem.isHideAttributes()) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            }
            if (guiItem.isHideEnchants()) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            // Glowing effect
            if (guiItem.isGlowing() && guiItem.getEnchantments().isEmpty()) {
                item.addUnsafeEnchantment(Enchantment.LURE, 1);
                if (!guiItem.isHideEnchants()) {
                    // Only add hide enchants flag if not already set
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
            }

            item.setItemMeta(meta);
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
        // Create item with configured material
        ItemBuilder builder = ItemBuilder.fromMaterialName(plugin, materialName);
        if (builder == null) {
            builder = new ItemBuilder(plugin, Material.CHEST);
        }
        
        // Format display name using status-specific template
        String displayName = rewardConfig.getDisplayName(statusKey)
            .replace("{reward_name}", reward.getName());
        displayName = MenuUtils.processPlaceholders(player, displayName);
        builder.name(displayName);

        // Create lore from status-specific template
        List<String> lore = new ArrayList<>();
        for (String line : rewardConfig.getLoreTemplate(statusKey)) {
            String processedLine = line;

            processedLine = processedLine.replace("{level}", String.valueOf(reward.getRequiredLevel()));
            processedLine = processedLine.replace("{reward_name}", reward.getName());
            processedLine = processedLine.replace("{description}", reward.getDescription());
            processedLine = processedLine.replace("{reward_description}", reward.getDescription());

            if (processedLine.contains("{player_level}")) {
                int playerLevel = plugin.getJobManager().getLevel(player, reward.getJobId());
                processedLine = processedLine.replace("{player_level}", String.valueOf(playerLevel));
            }

            if (processedLine.contains("{claim_date}") && status == RewardStatus.RETRIEVED) {
                long claimTime = rewardManager.getLastClaimTime(player, reward);
                String claimDate = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(claimTime));
                processedLine = processedLine.replace("{claim_date}", claimDate);
            }

            processedLine = MenuUtils.processPlaceholders(player, processedLine);
            lore.add(processedLine);
        }
        
        builder.lore(lore);
        return builder.build();
    }
    
    /**
     * Check if there is a next page.
     */
    private boolean hasNextPage() {
        return (currentPage + 1) * rewardsPerPage < rewards.size();
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
        List<Integer> rewardSlots = config.getRewardSlots(currentPage);
        
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
                plugin.getRewardGuiManager().updateOpenGui(player, newGui);
                newGui.open();
            }
        } else if (nav.getNextPage() != null && nav.getNextPage().getSlots().contains(slot)) {
            if (hasNextPage()) {
                // Create new GUI for next page
                CustomRewardGui newGui = new CustomRewardGui(plugin, player, job, rewards, config, currentPage + 1);
                plugin.getRewardGuiManager().updateOpenGui(player, newGui);
                newGui.open();
            }
        } else if (nav.getClose() != null && nav.getClose().getSlots().contains(slot)) {
            player.closeInventory();
        } else if (nav.getRefresh() != null && nav.getRefresh().getSlots().contains(slot)) {
            // Refresh GUI
            populateInventory();
        } else if (nav.getBack() != null && nav.getBack().getSlots().contains(slot)) {
            // Back to job menu
            player.closeInventory();
            plugin.getMenuManager().openJobMenu(player, job.getId());
        }
    }
    
    /**
     * Handle clicking on a reward.
     */
    private void handleRewardClick(Reward reward) {
        RewardStatus status = rewardManager.getRewardStatus(player, reward);

        if (status == RewardStatus.RETRIEVABLE) {
            if (rewardManager.claimReward(player, reward)) {
                MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.success", "reward", reward.getName(), "level", String.valueOf(reward.getRequiredLevel())));
                populateInventory();
            } else {
                MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.failed", "reward", reward.getName()));
            }
        } else if (status == RewardStatus.BLOCKED) {
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