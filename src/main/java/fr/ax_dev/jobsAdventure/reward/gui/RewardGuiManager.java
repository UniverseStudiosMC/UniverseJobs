package fr.ax_dev.jobsAdventure.reward.gui;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.job.Job;
import fr.ax_dev.jobsAdventure.reward.Reward;
import fr.ax_dev.jobsAdventure.reward.RewardManager;
import fr.ax_dev.jobsAdventure.reward.RewardStatus;
import fr.ax_dev.jobsAdventure.utils.MessageUtils;
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
    
    private final JobsAdventure plugin;
    private final RewardManager rewardManager;
    private final Map<UUID, Object> openGuis; // Can hold RewardGui or CustomRewardGui
    
    /**
     * Create a new RewardGuiManager.
     * 
     * @param plugin The plugin instance
     * @param rewardManager The reward manager
     */
    public RewardGuiManager(JobsAdventure plugin, RewardManager rewardManager) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
        this.openGuis = new ConcurrentHashMap<>();
        
        // Register event listener
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
            MessageUtils.sendMessage(player, "&cJob not found: " + jobId);
            return;
        }
        
        List<Reward> rewards = rewardManager.getJobRewards(jobId);
        if (rewards.isEmpty()) {
            MessageUtils.sendMessage(player, "&cNo rewards available for job: " + job.getName());
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
            MessageUtils.sendMessage(player, "&cNo rewards available for you in job: " + job.getName());
            return;
        }
        
        // Check if there's a custom GUI configuration for this job
        GuiConfig guiConfig = rewardManager.getGuiConfig(job.getId());
        
        if (guiConfig != null) {
            // Use custom GUI
            CustomRewardGui customGui = new CustomRewardGui(plugin, player, job, availableRewards, guiConfig, 0);
            openGuis.put(player.getUniqueId(), customGui);
            customGui.open();
        } else {
            // Fall back to default GUI
            RewardGui gui = new RewardGui(player, job, availableRewards, 0);
            openGuis.put(player.getUniqueId(), gui);
            gui.open();
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
        
        private static final int REWARDS_PER_PAGE = 45; // 5 rows for rewards
        private static final int GUI_SIZE = 54; // 6 rows total
        
        /**
         * Create a new RewardGui.
         * 
         * @param player The player viewing the GUI
         * @param job The job
         * @param rewards The available rewards
         * @param page The current page (0-based)
         */
        public RewardGui(Player player, Job job, List<Reward> rewards, int page) {
            this.player = player;
            this.job = job;
            this.rewards = rewards;
            this.page = page;
            this.inventory = Bukkit.createInventory(this, GUI_SIZE, 
                    MessageUtils.colorize("&6" + job.getName() + " Rewards"));
            
            setupGui();
        }
        
        /**
         * Setup the GUI with rewards and navigation items.
         */
        private void setupGui() {
            // Clear inventory
            inventory.clear();
            
            // Add rewards for current page
            int startIndex = page * REWARDS_PER_PAGE;
            int endIndex = Math.min(startIndex + REWARDS_PER_PAGE, rewards.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                Reward reward = rewards.get(i);
                int slot = reward.getGuiSlot();
                
                // Auto-assign slot if not specified
                if (slot == -1 || slot >= REWARDS_PER_PAGE || inventory.getItem(slot) != null) {
                    slot = i - startIndex;
                }
                
                // Skip if slot is out of bounds for rewards area
                if (slot >= REWARDS_PER_PAGE) continue;
                
                ItemStack rewardItem = createRewardItem(reward);
                inventory.setItem(slot, rewardItem);
            }
            
            // Add navigation items in bottom row (slots 45-53)
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
            
            // Determine the base item
            ItemStack item;
            if (!reward.getItems().isEmpty()) {
                // Use first reward item as display
                Reward.RewardItem firstItem = reward.getItems().get(0);
                item = ItemBuilder.fromRewardItem(plugin, firstItem).build();
            } else {
                // Use default item
                item = new ItemStack(Material.CHEST);
            }
            
            // Modify the item with reward information
            ItemBuilder builder = new ItemBuilder(plugin, item)
                    .name("&e" + reward.getName())
                    .addLore("")
                    .addLore("&7" + reward.getDescription());
            
            // Add requirement information
            if (reward.getRequiredLevel() > 1) {
                int playerLevel = plugin.getJobManager().getLevel(player, reward.getJobId());
                String levelColor = playerLevel >= reward.getRequiredLevel() ? "&a" : "&c";
                builder.addLore("&7Required Level: " + levelColor + reward.getRequiredLevel());
            }
            
            // Add cooldown information
            if (reward.getCooldownHours() > 0 && status == RewardStatus.RETRIEVED) {
                long lastClaim = rewardManager.getLastClaimTime(player, reward);
                long nextClaim = lastClaim + (reward.getCooldownHours() * 3600000L);
                long timeLeft = nextClaim - System.currentTimeMillis();
                
                if (timeLeft > 0) {
                    String timeString = formatTime(timeLeft);
                    builder.addLore("&7Cooldown: &c" + timeString);
                }
            }
            
            // Add reward items information
            if (!reward.getItems().isEmpty()) {
                builder.addLore("")
                     .addLore("&6Rewards:");
                
                int itemsShown = 0;
                for (Reward.RewardItem rewardItem : reward.getItems()) {
                    if (itemsShown >= 3) {
                        builder.addLore("&7... and " + (reward.getItems().size() - 3) + " more");
                        break;
                    }
                    
                    String itemName = rewardItem.getDisplayName() != null ? 
                            rewardItem.getDisplayName() : rewardItem.getMaterial();
                    builder.addLore("&8- &f" + rewardItem.getAmount() + "x " + itemName);
                    itemsShown++;
                }
            }
            
            // Add economy reward information
            if (reward.hasEconomyReward()) {
                builder.addLore("&8- &f$" + reward.getEconomyReward());
            }
            
            // Add commands information
            if (reward.hasCommands()) {
                builder.addLore("&8- &fSpecial rewards");
            }
            
            // Add status information
            builder.addLore("")
                   .addLore("&7Status: " + status.getIndicator() + " " + status.getDescription());
            
            return builder.build();
        }
        
        /**
         * Setup navigation items in the bottom row.
         */
        private void setupNavigationItems() {
            int totalPages = (int) Math.ceil((double) rewards.size() / REWARDS_PER_PAGE);
            
            // Previous page button
            if (page > 0) {
                ItemStack prevItem = ItemBuilder.createNavigationItem(plugin, Material.ARROW,
                        "&ePrevious Page", "&7Click to go to page " + page);
                inventory.setItem(45, prevItem);
            }
            
            // Page info
            ItemStack pageInfo = ItemBuilder.createNavigationItem(plugin, Material.BOOK,
                    "&6Page " + (page + 1) + " of " + totalPages,
                    "&7Showing rewards for " + job.getName());
            inventory.setItem(49, pageInfo);
            
            // Next page button
            if (page < totalPages - 1) {
                ItemStack nextItem = ItemBuilder.createNavigationItem(plugin, Material.ARROW,
                        "&eNext Page", "&7Click to go to page " + (page + 2));
                inventory.setItem(53, nextItem);
            }
            
            // Close button
            ItemStack closeItem = ItemBuilder.createNavigationItem(plugin, Material.BARRIER,
                    "&cClose", "&7Click to close this menu");
            inventory.setItem(48, closeItem);
            
            // Refresh button
            ItemStack refreshItem = ItemBuilder.createNavigationItem(plugin, Material.EMERALD,
                    "&aRefresh", "&7Click to refresh rewards");
            inventory.setItem(50, refreshItem);
        }
        
        /**
         * Fill empty slots with filler items.
         */
        private void fillEmptySlots() {
            ItemStack filler = ItemBuilder.createFillerItem(plugin);
            
            // Fill navigation row gaps
            for (int i = 46; i <= 52; i++) {
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
            if (slot >= 45) {
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
            switch (slot) {
                case 45: // Previous page
                    if (page > 0) {
                        RewardGui newGui = new RewardGui(player, job, rewards, page - 1);
                        openGuis.put(player.getUniqueId(), newGui);
                        newGui.open();
                    }
                    break;
                    
                case 48: // Close
                    player.closeInventory();
                    break;
                    
                case 50: // Refresh
                    setupGui();
                    break;
                    
                case 53: // Next page
                    int totalPages = (int) Math.ceil((double) rewards.size() / REWARDS_PER_PAGE);
                    if (page < totalPages - 1) {
                        RewardGui newGui = new RewardGui(player, job, rewards, page + 1);
                        openGuis.put(player.getUniqueId(), newGui);
                        newGui.open();
                    }
                    break;
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
                    MessageUtils.sendMessage(player, "&aSuccessfully claimed reward: " + reward.getName());
                    
                    // Refresh the GUI to update status
                    setupGui();
                } else {
                    MessageUtils.sendMessage(player, "&cFailed to claim reward: " + reward.getName());
                }
            } else if (status == RewardStatus.BLOCKED) {
                MessageUtils.sendMessage(player, "&cYou don't meet the requirements for this reward yet.");
            } else if (status == RewardStatus.RETRIEVED) {
                if (reward.isRepeatable() && reward.getCooldownHours() > 0) {
                    long lastClaim = rewardManager.getLastClaimTime(player, reward);
                    long nextClaim = lastClaim + (reward.getCooldownHours() * 3600000L);
                    long timeLeft = nextClaim - System.currentTimeMillis();
                    
                    if (timeLeft > 0) {
                        String timeString = formatTime(timeLeft);
                        MessageUtils.sendMessage(player, "&cYou can claim this reward again in: " + timeString);
                    }
                } else {
                    MessageUtils.sendMessage(player, "&cYou have already claimed this reward.");
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
            int startIndex = page * REWARDS_PER_PAGE;
            
            for (int i = 0; i < Math.min(REWARDS_PER_PAGE, rewards.size() - startIndex); i++) {
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
    
    /**
     * Format time in milliseconds to a human-readable string.
     * 
     * @param timeMs The time in milliseconds
     * @return The formatted time string
     */
    private String formatTime(long timeMs) {
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
}