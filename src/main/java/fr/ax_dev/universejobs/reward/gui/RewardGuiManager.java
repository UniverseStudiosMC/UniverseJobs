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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all GUI operations for the reward system.
 * Handles creating, updating, and managing reward GUIs.
 */
public class RewardGuiManager implements Listener {
    
    private final UniverseJobs plugin;
    private final LanguageManager languageManager;
    private final RewardManager rewardManager;
    private final Map<UUID, Object> openGuis; // Can hold RewardGui or CustomRewardGui
    
    /**
     * Create a new RewardGuiManager.
     * 
     * @param plugin The plugin instance
     * @param rewardManager The reward manager
     */
    public RewardGuiManager(UniverseJobs plugin, RewardManager rewardManager) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.rewardManager = rewardManager;
        this.openGuis = new ConcurrentHashMap<>();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Open the rewards GUI for a specific job.
     * 
     * @param player The player to show the GUI to
     * @param jobId The job ID
     */
    public void openRewardsGui(Player player, String jobId) {
        Job job = plugin.getJobManager().getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(player, languageManager.getMessage("rewards.gui.job-not-found", "job", jobId));
            return;
        }
        
        List<Reward> rewards = rewardManager.getJobRewards(jobId);
        if (rewards.isEmpty()) {
            MessageUtils.sendMessage(player, languageManager.getMessage("rewards.gui.no-rewards", "job", job.getName()));
            return;
        }
        
        // Filter rewards by permission
        List<Reward> availableRewards = new ArrayList<>();
        for (Reward reward : rewards) {
            if (reward.getPermission() == null || player.hasPermission(reward.getPermission())) {
                availableRewards.add(reward);
            }
        }
        
        if (availableRewards.isEmpty()) {
            MessageUtils.sendMessage(player, languageManager.getMessage("rewards.gui.no-available-rewards", "job", job.getName()));
            return;
        }
        
        // Check if there's a custom GUI configuration for this job
        GuiConfig guiConfig = rewardManager.getGuiConfig(job.getId());
        
        if (guiConfig != null) {
            // Use custom GUI
            CustomRewardGui customGui = new CustomRewardGui(plugin, player, job, availableRewards, guiConfig, 0);
            openGuis.put(player.getUniqueId(), customGui);
            customGui.open();
        }
    }
    
    /**
     * Handle inventory click events for reward GUIs.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Object guiObj = openGuis.get(player.getUniqueId());
        if (guiObj == null) return;
        
        event.setCancelled(true);
        
        if (guiObj instanceof CustomRewardGui customGui) {
            if (customGui.isInventory(event.getInventory())) {
                customGui.handleClick(event.getSlot());
            }
        } else if (guiObj instanceof RewardGui gui) {
            if (gui.isInventory(event.getInventory())) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
                
                gui.handleClick(event.getSlot(), clickedItem);
            }
        }
    }
    
    /**
     * Handle inventory close events for reward GUIs.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        Object guiObj = openGuis.get(player.getUniqueId());
        if (guiObj != null) {
            boolean shouldRemove = false;
            
            if (guiObj instanceof CustomRewardGui customGui) {
                shouldRemove = customGui.isInventory(event.getInventory());
            } else if (guiObj instanceof RewardGui gui) {
                shouldRemove = gui.isInventory(event.getInventory());
            }
            
            if (shouldRemove) {
                openGuis.remove(player.getUniqueId());
            }
        }
    }
    
    /**
     * Update the open GUI for a player.
     *
     * @param player The player
     * @param gui The new GUI
     */
    public void updateOpenGui(Player player, Object gui) {
        openGuis.put(player.getUniqueId(), gui);
    }

    /**
     * Close all open GUIs.
     */
    public void closeAllGuis() {
        for (Object guiObj : openGuis.values()) {
            if (guiObj instanceof RewardGui gui) {
                gui.close();
            } else if (guiObj instanceof CustomRewardGui customGui) {
                // CustomRewardGui doesn't have a close method, just clear the map
            }
        }
        openGuis.clear();
    }
    
    /**
     * Represents a reward GUI for a specific job.
     */
    private class RewardGui implements InventoryHolder {
        
        private final Player player;
        private final Job job;
        private final List<Reward> rewards;
        private final int page;
        private final Inventory inventory;
        private final GuiConfig.DefaultGuiConfig config;
        
        private final int rewardsPerPage;
        private final int guiSize;
        
        /**
         * Create a new RewardGui.
         * 
         * @param player The player viewing the GUI
         * @param job The job
         * @param rewards The available rewards
         * @param page The current page (0-based)
         * @param config The default GUI configuration
         */
        public RewardGui(Player player, Job job, List<Reward> rewards, int page, GuiConfig.DefaultGuiConfig config) {
            this.player = player;
            this.job = job;
            this.rewards = rewards;
            this.page = page;
            this.config = config;
            this.rewardsPerPage = config.getRewardsPerPage();
            this.guiSize = config.getSize();
            
            String title = config.getTitleFormat().replace("{job}", job.getName());
            title = MenuUtils.processPlaceholders(player, title);
            this.inventory = Bukkit.createInventory(this, guiSize, MessageUtils.colorize(title));
            
            setupGui();
        }
        
        /**
         * Setup the GUI with rewards and navigation items.
         */
        private void setupGui() {
            // Clear inventory
            inventory.clear();
            
            // Add rewards for current page
            int startIndex = page * rewardsPerPage;
            int endIndex = Math.min(startIndex + rewardsPerPage, rewards.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                Reward reward = rewards.get(i);
                int slot = reward.getGuiSlot();
                
                // Auto-assign slot if not specified
                if (slot == -1 || slot >= rewardsPerPage || inventory.getItem(slot) != null) {
                    slot = i - startIndex;
                }
                
                // Skip if slot is out of bounds for rewards area
                if (slot >= rewardsPerPage) continue;
                
                ItemStack rewardItem = createRewardItem(reward);
                inventory.setItem(slot, rewardItem);
            }
            
            // Add navigation items
            setupNavigationItems();
            
            // Fill empty slots with filler items
            fillEmptySlots();
        }
        
        /**
         * Create an ItemStack representing a reward.
         * 
         * @param reward The reward
         * @return The reward ItemStack
         */
        private ItemStack createRewardItem(Reward reward) {
            RewardStatus status = rewardManager.getRewardStatus(player, reward);
            GuiConfig.RewardItemConfig rewardConfig = config.getRewardItemConfig();
            
            String statusKey = status.name().toLowerCase();
            String materialName = rewardConfig.getMaterial(statusKey);
            String statusIndicator = rewardConfig.getStatusIndicator(statusKey);
            
            // Create item with configured material or use reward item
            ItemBuilder builder;
            if (!reward.getItems().isEmpty()) {
                // Use first reward item as display
                Reward.RewardItem firstItem = reward.getItems().get(0);
                builder = ItemBuilder.fromRewardItem(plugin, firstItem);
            } else {
                // Use configured material
                builder = ItemBuilder.fromMaterialName(plugin, materialName);
                if (builder == null) {
                    builder = new ItemBuilder(plugin, Material.CHEST);
                }
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
                    handleRepeatableInfo(reward, lore, rewardConfig);
                } else if (line.contains("{click_instruction}")) {
                    if (status == RewardStatus.RETRIEVABLE) {
                        lore.add("");
                        String instruction = MenuUtils.processPlaceholders(player, rewardConfig.getClickInstruction());
                        lore.add(instruction);
                    }
                } else if (line.contains("{reward_items}")) {
                    addRewardItemsLore(reward, lore);
                } else if (line.contains("{economy_reward}")) {
                    if (reward.hasEconomyReward()) {
                        String economyLine = line.replace("{economy_reward}", String.valueOf(reward.getEconomyReward()));
                        lore.add(MenuUtils.processPlaceholders(player, economyLine));
                    } else {
                        String economyLine = line.replace("{economy_reward}", "");
                        if (!economyLine.trim().isEmpty()) {
                            lore.add(MenuUtils.processPlaceholders(player, economyLine));
                        }
                    }
                } else if (line.contains("{commands}")) {
                    if (reward.hasCommands()) {
                        String commandLine = line.replace("{commands}", rewardConfig.getText("special_rewards"));
                        lore.add(MenuUtils.processPlaceholders(player, commandLine));
                    } else {
                        String commandLine = line.replace("{commands}", "");
                        if (!commandLine.trim().isEmpty()) {
                            lore.add(MenuUtils.processPlaceholders(player, commandLine));
                        }
                    }
                } else if (line.contains("{cooldown}")) {
                    addCooldownLore(reward, status, lore, line, rewardConfig);
                } else if (!line.isEmpty()) {
                    lore.add(MenuUtils.processPlaceholders(player, line));
                } else {
                    lore.add("");
                }
            }
            
            builder.lore(lore);
            return builder.build();
        }
        
        private void handleRepeatableInfo(Reward reward, List<String> lore, GuiConfig.RewardItemConfig rewardConfig) {
            if (reward.isRepeatable()) {
                lore.add(MenuUtils.processPlaceholders(player, rewardConfig.getText("repeatable_yes")));
            } else {
                lore.add(MenuUtils.processPlaceholders(player, rewardConfig.getText("repeatable_no")));
            }
        }
        
        private void addRewardItemsLore(Reward reward, List<String> lore) {
            if (!reward.getItems().isEmpty()) {
                GuiConfig.RewardItemConfig rewardConfig = config.getRewardItemConfig();
                lore.add("");
                lore.add(MenuUtils.processPlaceholders(player, rewardConfig.getText("rewards_title")));
                
                int itemsShown = 0;
                for (Reward.RewardItem rewardItem : reward.getItems()) {
                    if (itemsShown >= 3) {
                        String moreLine = rewardConfig.getText("more_items")
                            .replace("{count}", String.valueOf(reward.getItems().size() - 3));
                        lore.add(MenuUtils.processPlaceholders(player, moreLine));
                        break;
                    }
                    
                    String itemName = rewardItem.getDisplayName() != null ? 
                            rewardItem.getDisplayName() : rewardItem.getMaterial();
                    String itemLine = rewardConfig.getText("item_format")
                        .replace("{amount}", String.valueOf(rewardItem.getAmount()))
                        .replace("{name}", itemName);
                    lore.add(MenuUtils.processPlaceholders(player, itemLine));
                    itemsShown++;
                }
            }
        }
        
        private void addCooldownLore(Reward reward, RewardStatus status, List<String> lore, String line, GuiConfig.RewardItemConfig rewardConfig) {
            if (reward.getCooldownHours() > 0 && status == RewardStatus.RETRIEVED) {
                long lastClaim = rewardManager.getLastClaimTime(player, reward);
                long nextClaim = lastClaim + (reward.getCooldownHours() * 3600000L);
                long timeLeft = nextClaim - System.currentTimeMillis();
                
                if (timeLeft > 0) {
                    String timeString = formatTime(timeLeft, rewardConfig.getTimeFormat());
                    String cooldownLine = line.replace("{cooldown}", timeString);
                    lore.add(MenuUtils.processPlaceholders(player, cooldownLine));
                }
            }
        }
        
        private String formatTime(long timeMs, String format) {
            if (format.equals("{hours}h")) {
                return formatTimeSimple(timeMs);
            }
            
            long hours = timeMs / 3600000L;
            long days = hours / 24;
            long weeks = days / 7;
            
            return format.replace("{hours}", String.valueOf(hours % 24))
                        .replace("{days}", String.valueOf(days % 7))
                        .replace("{weeks}", String.valueOf(weeks));
        }
        
        private String formatTimeSimple(long timeMs) {
            long seconds = timeMs / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (days > 0) {
                return days + "d " + (hours % 24) + "h";
            } else if (hours > 0) {
                return hours + "h " + (minutes % 60) + "m";
            } else if (minutes > 0) {
                return minutes + "m " + (seconds % 60) + "s";
            } else {
                return seconds + "s";
            }
        }
        
        /**
         * Setup navigation items using configured slots, materials, names and lore.
         */
        private void setupNavigationItems() {
            int totalPages = (int) Math.ceil((double) rewards.size() / rewardsPerPage);
            
            // Previous page button
            if (page > 0) {
                createNavigationItem("previous", totalPages, page);
            }
            
            // Page info
            createNavigationItem("info", totalPages, page);
            
            // Next page button
            if (page < totalPages - 1) {
                createNavigationItem("next", totalPages, page + 2);
            }
            
            // Close button
            createNavigationItem("close", totalPages, page);
            
            // Refresh button
            createNavigationItem("refresh", totalPages, page);
        }
        
        private void createNavigationItem(String type, int totalPages, int targetPage) {
            int slot = config.getNavigationSlot(type);
            if (slot >= 0 && slot < inventory.getSize()) {
                String material = config.getNavigationMaterial(type);
                String name = config.getNavigationName(type);
                List<String> loreLines = config.getNavigationLore(type);
                
                // Replace placeholders in name
                name = name.replace("{current_page}", String.valueOf(page + 1))
                          .replace("{total_pages}", String.valueOf(totalPages))
                          .replace("{target_page}", String.valueOf(targetPage))
                          .replace("{job}", job.getName());
                name = MenuUtils.processPlaceholders(player, name);
                
                // Replace placeholders in lore
                List<String> processedLore = new ArrayList<>();
                for (String loreLine : loreLines) {
                    String processed = loreLine.replace("{current_page}", String.valueOf(page + 1))
                                              .replace("{total_pages}", String.valueOf(totalPages))
                                              .replace("{target_page}", String.valueOf(targetPage))
                                              .replace("{job}", job.getName());
                    processedLore.add(MenuUtils.processPlaceholders(player, processed));
                }
                
                // Create item
                ItemBuilder builder = ItemBuilder.fromMaterialName(plugin, material);
                if (builder == null) {
                    builder = new ItemBuilder(plugin, Material.STONE);
                }
                
                ItemStack navItem = builder.name(name).lore(processedLore).build();
                inventory.setItem(slot, navItem);
            }
        }
        
        /**
         * Fill empty slots with configured filler items.
         */
        private void fillEmptySlots() {
            ItemBuilder fillerBuilder = ItemBuilder.fromMaterialName(plugin, config.getFillerMaterial());
            if (fillerBuilder == null) {
                fillerBuilder = new ItemBuilder(plugin, Material.GRAY_STAINED_GLASS_PANE);
            }
            
            ItemStack filler = fillerBuilder.name(config.getFillerName()).build();
            
            // Fill empty slots in navigation area
            int navAreaStart = rewardsPerPage;
            for (int i = navAreaStart; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, filler);
                }
            }
        }
        
        /**
         * Handle clicks on items in the GUI.
         * 
         * @param slot The clicked slot
         * @param item The clicked item
         */
        public void handleClick(int slot, ItemStack item) {
            // Handle navigation clicks
            if (slot >= rewardsPerPage) {
                handleNavigationClick(slot);
                return;
            }
            
            // Handle reward clicks
            Reward clickedReward = getRewardAtSlot(slot);
            if (clickedReward != null) {
                handleRewardClick(clickedReward);
            }
        }
        
        /**
         * Handle navigation button clicks.
         * 
         * @param slot The clicked slot
         */
        private void handleNavigationClick(int slot) {
            if (slot == config.getNavigationSlot("previous") && page > 0) {
                // Previous page
                RewardGui newGui = new RewardGui(player, job, rewards, page - 1, config);
                openGuis.put(player.getUniqueId(), newGui);
                newGui.open();
            } else if (slot == config.getNavigationSlot("close")) {
                // Close
                player.closeInventory();
            } else if (slot == config.getNavigationSlot("refresh")) {
                // Refresh
                setupGui();
            } else if (slot == config.getNavigationSlot("next")) {
                // Next page
                int totalPages = (int) Math.ceil((double) rewards.size() / rewardsPerPage);
                if (page < totalPages - 1) {
                    RewardGui newGui = new RewardGui(player, job, rewards, page + 1, config);
                    openGuis.put(player.getUniqueId(), newGui);
                    newGui.open();
                }
            }
        }
        
        /**
         * Handle reward item clicks.
         * 
         * @param reward The clicked reward
         */
        private void handleRewardClick(Reward reward) {
            RewardStatus status = rewardManager.getRewardStatus(player, reward);
            
            if (status == RewardStatus.RETRIEVABLE) {
                // Try to claim the reward
                if (rewardManager.claimReward(player, reward)) {
                    MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.success", "reward", reward.getName()));
                    
                    // Refresh the GUI to update status
                    setupGui();
                } else {
                    MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.failed", "reward", reward.getName()));
                }
            } else if (status == RewardStatus.BLOCKED) {
                MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.requirements-not-met"));
            } else if (status == RewardStatus.RETRIEVED) {
                if (reward.isRepeatable() && reward.getCooldownHours() > 0) {
                    long lastClaim = rewardManager.getLastClaimTime(player, reward);
                    long nextClaim = lastClaim + (reward.getCooldownHours() * 3600000L);
                    long timeLeft = nextClaim - System.currentTimeMillis();
                    
                    if (timeLeft > 0) {
                        String timeString = formatTimeSimple(timeLeft);
                        MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.cooldown", "time", timeString));
                    }
                } else {
                    MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.already-claimed"));
                }
            }
        }
        
        /**
         * Get the reward at a specific slot.
         * 
         * @param slot The slot
         * @return The reward or null if none
         */
        private Reward getRewardAtSlot(int slot) {
            int startIndex = page * rewardsPerPage;
            
            for (int i = 0; i < Math.min(rewardsPerPage, rewards.size() - startIndex); i++) {
                Reward reward = rewards.get(startIndex + i);
                int rewardSlot = reward.getGuiSlot();
                
                if (rewardSlot == -1) {
                    rewardSlot = i;
                }
                
                if (rewardSlot == slot) {
                    return reward;
                }
            }
            
            return null;
        }
        
        /**
         * Open the GUI for the player.
         */
        public void open() {
            player.openInventory(inventory);
        }
        
        /**
         * Close the GUI.
         */
        public void close() {
            player.closeInventory();
        }
        
        /**
         * Check if this GUI owns the given inventory.
         * 
         * @param inv The inventory to check
         * @return true if this GUI owns the inventory
         */
        public boolean isInventory(Inventory inv) {
            return inventory.equals(inv);
        }
        
        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}