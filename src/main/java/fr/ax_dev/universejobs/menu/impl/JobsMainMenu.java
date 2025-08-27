package fr.ax_dev.universejobs.menu.impl;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.menu.BaseMenu;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;
import fr.ax_dev.universejobs.menu.utils.MenuItemUtils;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main jobs menu showing all available jobs.
 * Players can click on jobs to open individual job menus.
 */
public class JobsMainMenu extends BaseMenu {
    
    private final List<Job> availableJobs;
    private final PlayerJobData playerData;
    
    public JobsMainMenu(UniverseJobs plugin, org.bukkit.entity.Player player, SingleMenuConfig config) {
        super(plugin, player, config);
        
        this.playerData = plugin.getJobManager().getPlayerData(player.getUniqueId());
        this.availableJobs = plugin.getJobManager().getJobs().values().stream()
            .filter(job -> job.isEnabled())
            .filter(job -> job.getPermission() == null || player.hasPermission(job.getPermission()))
            .sorted(Comparator.comparing(Job::getName))
            .collect(Collectors.toList());
    }
    
    @Override
    protected void populateInventory() {
        // Clear inventory first
        inventory.clear();
        
        // Add static items first
        addStaticItems();
        
        // Add job items
        addJobItems();
        
        // Add navigation items
        addNavigationItems();
        
        // Fill empty slots
        addFillItems();
    }
    
    /**
     * Add static items defined in configuration.
     */
    private void addStaticItems() {
        Map<String, String> placeholders = getGeneralPlaceholders();
        MenuItemUtils.addStaticItems(inventory, config.getStaticItems(), placeholders,
            config -> createMenuItem(config, placeholders));
    }
    
    /**
     * Add job items to the menu.
     */
    private void addJobItems() {
        List<Integer> contentSlots = config.getContentSlots();
        int startIndex = currentPage * config.getItemsPerPage();
        int endIndex = Math.min(startIndex + config.getItemsPerPage(), availableJobs.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Job job = availableJobs.get(i);
            int slotIndex = i - startIndex;
            
            if (slotIndex >= contentSlots.size()) break;
            
            int slot = contentSlots.get(slotIndex);
            ItemStack jobItem = createJobItem(job);
            inventory.setItem(slot, jobItem);
        }
    }
    
    /**
     * Create an ItemStack representing a job.
     */
    private ItemStack createJobItem(Job job) {
        // Get job icon material
        Material iconMaterial;
        try {
            iconMaterial = Material.valueOf(job.getIcon().toUpperCase());
        } catch (IllegalArgumentException e) {
            iconMaterial = Material.STONE;
        }
        
        ItemStack item = new ItemStack(iconMaterial);
        
        // Get player's job data for this job
        boolean hasJob = playerData.hasJob(job.getId());
        int playerLevel = hasJob ? playerData.getLevel(job.getId()) : 0;
        long playerXp = hasJob ? (long) playerData.getXp(job.getId()) : 0;
        
        // Create custom placeholders for this job
        Map<String, String> placeholders = MenuItemUtils.createJobPlaceholders(job.getId(), job.getName(), job.getDescription());
        placeholders.put("job_max_level", String.valueOf(job.getMaxLevel()));
        placeholders.put("player_level", String.valueOf(playerLevel));
        placeholders.put("player_xp", String.valueOf(playerXp));
        placeholders.put("has_job", hasJob ? "Yes" : "No");
        placeholders.put("job_status", hasJob ? "&aJoined" : "&7Not Joined");
        
        // Calculate next level XP if available
        if (hasJob && job.getXpCurve() != null && playerLevel < job.getMaxLevel()) {
            long nextLevelXp = (long) job.getXpCurve().getXpForLevel(playerLevel + 1);
            long xpToNext = nextLevelXp - playerXp;
            placeholders.put("xp_to_next", String.valueOf(Math.max(0, xpToNext)));
            placeholders.put("next_level_xp", String.valueOf(nextLevelXp));
        } else {
            placeholders.put("xp_to_next", "0");
            placeholders.put("next_level_xp", "0");
        }
        
        // Create default job item configuration
        List<String> lore = new ArrayList<>();
        lore.add("&7" + job.getDescription());
        lore.add("");
        lore.add("&7Max Level: &e{job_max_level}");
        
        if (hasJob) {
            lore.add("&7Your Level: &a{player_level}");
            lore.add("&7Your XP: &b{player_xp}");
            if (playerLevel < job.getMaxLevel()) {
                lore.add("&7XP to Next: &e{xp_to_next}");
            }
            lore.add("");
            lore.add("&7Status: {job_status}");
        } else {
            lore.add("&7Your Level: &c0");
            lore.add("&7Status: {job_status}");
        }
        
        // Add job lore from configuration
        lore.addAll(job.getLore());
        
        lore.add("");
        lore.add("&e▶ Click to open job menu");
        
        // Create menu item config for this job
        Map<String, Object> jobConfigMap = MenuItemUtils.createItemConfigMap(
            job.getIcon(), "&e&l" + job.getName(), lore, hasJob
        );
        jobConfigMap.put("action", "open_job");
        jobConfigMap.put("action-value", job.getId());
        
        MenuItemConfig jobItemConfig = new MenuItemConfig(new SimpleConfigurationSection(jobConfigMap));
        return createMenuItem(jobItemConfig, placeholders);
    }
    
    /**
     * Add navigation items to the menu.
     */
    private void addNavigationItems() {
        Map<String, MenuItemConfig> navItems = config.getNavigationItems();
        MenuItemUtils.addNavigationItems(inventory, navItems, currentPage, hasNextPage(),
            getNavigationPlaceholders(), config -> createMenuItem(config, getNavigationPlaceholders()));
    }
    
    /**
     * Get placeholders for navigation items.
     */
    private Map<String, String> getNavigationPlaceholders() {
        Map<String, String> placeholders = MenuItemUtils.createNavigationPlaceholders(
            currentPage, availableJobs.size(), config.getItemsPerPage());
        placeholders.put("total_jobs", String.valueOf(availableJobs.size()));
        placeholders.put("player_jobs", String.valueOf(playerData.getJobs().size()));
        return placeholders;
    }
    
    /**
     * Get general placeholders for static items.
     */
    private Map<String, String> getGeneralPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("total_jobs", String.valueOf(availableJobs.size()));
        placeholders.put("player_jobs", String.valueOf(playerData.getJobs().size()));
        return placeholders;
    }
    
    @Override
    public void handleClick(int slot, InventoryClickEvent event) {
        // Handle navigation clicks first
        if (handleNavigationClick(slot)) {
            return;
        }
        
        // Handle job clicks
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        // Find which job was clicked
        List<Integer> contentSlots = config.getContentSlots();
        int slotIndex = contentSlots.indexOf(slot);
        
        if (slotIndex >= 0) {
            int jobIndex = currentPage * config.getItemsPerPage() + slotIndex;
            if (jobIndex < availableJobs.size()) {
                Job clickedJob = availableJobs.get(jobIndex);
                
                // Open the individual job menu
                plugin.getMenuManager().openJobMenu(player, clickedJob.getId());
            }
        }
    }
    
    @Override
    protected boolean hasNextPage() {
        return (currentPage + 1) * config.getItemsPerPage() < availableJobs.size();
    }
    
    /**
     * Get total number of pages.
     */
    private int getTotalPages() {
        return (int) Math.ceil((double) availableJobs.size() / config.getItemsPerPage());
    }
    
}