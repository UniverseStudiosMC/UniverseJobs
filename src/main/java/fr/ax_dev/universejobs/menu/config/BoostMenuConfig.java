package fr.ax_dev.universejobs.menu.config;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class BoostMenuConfig {
    
    private final UniverseJobs plugin;
    private YamlConfiguration config;
    
    private String title = "<gold><b>Boost Manager</b></gold>";
    private int size = 54;
    
    private BoostItemConfig xpBoostConfig;
    private BoostItemConfig moneyBoostConfig;
    private FillerConfig fillerConfig;
    private NavigationConfig navigationConfig;
    private Map<String, CustomItemConfig> customItems;
    private AutoRefreshConfig autoRefreshConfig;
    private SoundsConfig soundsConfig;
    private PermissionsConfig permissionsConfig;
    private MessagesConfig messagesConfig;
    private BoostCalculationMode boostCalculationMode = BoostCalculationMode.MULTIPLICATIVE;
    
    public BoostMenuConfig(UniverseJobs plugin) {
        this.plugin = plugin;
        this.customItems = new HashMap<>();
    }
    
    public void loadConfiguration() {
        File configFile = new File(plugin.getDataFolder(), "menus/boost-manager.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("menus/boost-manager.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        loadBasicSettings();
        loadBoostConfigs();
        loadFillerConfig();
        loadNavigationConfig();
        loadCustomItems();
        loadAutoRefreshConfig();
        loadSoundsConfig();
        loadPermissionsConfig();
        loadMessagesConfig();
        loadBoostCalculationMode();
    }
    
    private void loadBasicSettings() {
        title = config.getString("title", "<gold><b>Boost Manager</b></gold>");
        size = config.getInt("size", 54);
    }
    
    private void loadBoostConfigs() {
        ConfigurationSection xpSection = config.getConfigurationSection("xp-boosts");
        if (xpSection != null) {
            xpBoostConfig = new BoostItemConfig();
            xpBoostConfig.enabled = xpSection.getBoolean("enabled", true);
            xpBoostConfig.slots = xpSection.getIntegerList("slots");
            
            ConfigurationSection itemSection = xpSection.getConfigurationSection("item");
            if (itemSection != null) {
                xpBoostConfig.material = Material.valueOf(itemSection.getString("material", "EXPERIENCE_BOTTLE"));
                xpBoostConfig.displayName = itemSection.getString("display-name", "<!italic><white>XP Boost <gold>{boost_id}</gold></white>");
                xpBoostConfig.lore = itemSection.getStringList("lore");
                xpBoostConfig.glow = itemSection.getBoolean("glow", false);
                xpBoostConfig.customModelData = itemSection.getInt("custom-model-data", 0);
            }
        }
        
        ConfigurationSection moneySection = config.getConfigurationSection("money-boosts");
        if (moneySection != null) {
            moneyBoostConfig = new BoostItemConfig();
            moneyBoostConfig.enabled = moneySection.getBoolean("enabled", true);
            moneyBoostConfig.slots = moneySection.getIntegerList("slots");
            
            ConfigurationSection itemSection = moneySection.getConfigurationSection("item");
            if (itemSection != null) {
                moneyBoostConfig.material = Material.valueOf(itemSection.getString("material", "GOLD_INGOT"));
                moneyBoostConfig.displayName = itemSection.getString("display-name", "<!italic><white>Money Boost <gold>{boost_id}</gold></white>");
                moneyBoostConfig.lore = itemSection.getStringList("lore");
                moneyBoostConfig.glow = itemSection.getBoolean("glow", false);
                moneyBoostConfig.customModelData = itemSection.getInt("custom-model-data", 0);
            }
        }
    }
    
    private void loadFillerConfig() {
        ConfigurationSection fillerSection = config.getConfigurationSection("filler");
        if (fillerSection != null) {
            fillerConfig = new FillerConfig();
            fillerConfig.enabled = fillerSection.getBoolean("enabled", true);
            fillerConfig.material = Material.valueOf(fillerSection.getString("material", "GRAY_STAINED_GLASS_PANE"));
            fillerConfig.displayName = fillerSection.getString("display-name", "<!italic><gray> </gray>");
            fillerConfig.lore = fillerSection.getStringList("lore");
            fillerConfig.slots = fillerSection.getIntegerList("slots");
        }
    }
    
    private void loadNavigationConfig() {
        ConfigurationSection navSection = config.getConfigurationSection("navigation");
        if (navSection != null) {
            navigationConfig = new NavigationConfig();
            
            loadNavigationItem(navSection.getConfigurationSection("close"), navigationConfig.closeItem = new NavigationItemConfig());
            loadNavigationItem(navSection.getConfigurationSection("refresh"), navigationConfig.refreshItem = new NavigationItemConfig());
            loadNavigationItem(navSection.getConfigurationSection("info"), navigationConfig.infoItem = new NavigationItemConfig());
        }
    }
    
    private void loadNavigationItem(ConfigurationSection section, NavigationItemConfig item) {
        if (section != null) {
            item.enabled = section.getBoolean("enabled", true);
            item.slots = section.getIntegerList("slots");
            item.material = Material.valueOf(section.getString("material", "BARRIER"));
            item.displayName = section.getString("display-name", "");
            item.lore = section.getStringList("lore");
            item.glow = section.getBoolean("glow", false);
        }
    }
    
    private void loadCustomItems() {
        ConfigurationSection customSection = config.getConfigurationSection("custom-items");
        if (customSection != null) {
            for (String key : customSection.getKeys(false)) {
                ConfigurationSection itemSection = customSection.getConfigurationSection(key);
                if (itemSection != null) {
                    CustomItemConfig customItem = new CustomItemConfig();
                    customItem.enabled = itemSection.getBoolean("enabled", true);
                    customItem.slots = itemSection.getIntegerList("slots");
                    customItem.material = Material.valueOf(itemSection.getString("material", "STONE"));
                    customItem.displayName = itemSection.getString("display-name", "");
                    customItem.lore = itemSection.getStringList("lore");
                    customItems.put(key, customItem);
                }
            }
        }
    }
    
    private void loadAutoRefreshConfig() {
        ConfigurationSection refreshSection = config.getConfigurationSection("auto-refresh");
        if (refreshSection != null) {
            autoRefreshConfig = new AutoRefreshConfig();
            autoRefreshConfig.enabled = refreshSection.getBoolean("enabled", true);
            autoRefreshConfig.interval = refreshSection.getInt("interval", 20);
        }
    }
    
    private void loadSoundsConfig() {
        ConfigurationSection soundsSection = config.getConfigurationSection("sounds");
        if (soundsSection != null) {
            soundsConfig = new SoundsConfig();
            
            loadSoundConfig(soundsSection.getConfigurationSection("open"), soundsConfig.openSound = new SoundConfig());
            loadSoundConfig(soundsSection.getConfigurationSection("remove-boost"), soundsConfig.removeBoostSound = new SoundConfig());
            loadSoundConfig(soundsSection.getConfigurationSection("close"), soundsConfig.closeSound = new SoundConfig());
        }
    }
    
    private void loadSoundConfig(ConfigurationSection section, SoundConfig sound) {
        if (section != null) {
            sound.enabled = section.getBoolean("enabled", true);
            try {
                sound.sound = Sound.valueOf(section.getString("sound", "UI_BUTTON_CLICK"));
            } catch (IllegalArgumentException e) {
                sound.sound = Sound.UI_BUTTON_CLICK;
            }
            sound.volume = (float) section.getDouble("volume", 0.5);
            sound.pitch = (float) section.getDouble("pitch", 1.0);
        }
    }
    
    private void loadPermissionsConfig() {
        ConfigurationSection permsSection = config.getConfigurationSection("permissions");
        if (permsSection != null) {
            permissionsConfig = new PermissionsConfig();
            permissionsConfig.viewPermission = permsSection.getString("view", "universejobs.admin.boost.gui");
            permissionsConfig.removePermission = permsSection.getString("remove", "universejobs.admin.boost.remove");
        }
    }
    
    private void loadMessagesConfig() {
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            messagesConfig = new MessagesConfig();
            messagesConfig.boostRemoved = messagesSection.getString("boost-removed", "<!italic><green>Removed {type} boost: <gold>{boost_id}</gold></green>");
            messagesConfig.noPermission = messagesSection.getString("no-permission", "<!italic><red>You don't have permission to do that!</red>");
            messagesConfig.refreshClicked = messagesSection.getString("refresh-clicked", "<!italic><aqua>Refreshing boost list...</aqua>");
        }
    }
    
    private void loadBoostCalculationMode() {
        String modeStr = config.getString("boost-calculation-mode", "MULTIPLICATIVE");
        try {
            boostCalculationMode = BoostCalculationMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid boost calculation mode: " + modeStr + ", using MULTIPLICATIVE");
            boostCalculationMode = BoostCalculationMode.MULTIPLICATIVE;
        }
    }
    
    public enum BoostCalculationMode {
        ADDITIVE,       // Mode 1: 2.5x + 2.5x = 5.0x
        MULTIPLICATIVE, // Mode 2: 2.5x * 2.5x = 6.25x
        HIGHEST        // Mode 3: Only use the highest multiplier
    }
    
    public static class BoostItemConfig {
        public boolean enabled = true;
        public List<Integer> slots = new ArrayList<>();
        public Material material = Material.EXPERIENCE_BOTTLE;
        public String displayName = "";
        public List<String> lore = new ArrayList<>();
        public boolean glow = false;
        public int customModelData = 0;
    }
    
    public static class FillerConfig {
        public boolean enabled = true;
        public Material material = Material.GRAY_STAINED_GLASS_PANE;
        public String displayName = "";
        public List<String> lore = new ArrayList<>();
        public List<Integer> slots = new ArrayList<>();
    }
    
    public static class NavigationConfig {
        public NavigationItemConfig closeItem = new NavigationItemConfig();
        public NavigationItemConfig refreshItem = new NavigationItemConfig();
        public NavigationItemConfig infoItem = new NavigationItemConfig();
    }
    
    public static class NavigationItemConfig {
        public boolean enabled = true;
        public List<Integer> slots = new ArrayList<>();
        public Material material = Material.BARRIER;
        public String displayName = "";
        public List<String> lore = new ArrayList<>();
        public boolean glow = false;
    }
    
    public static class CustomItemConfig {
        public boolean enabled = true;
        public List<Integer> slots = new ArrayList<>();
        public Material material = Material.STONE;
        public String displayName = "";
        public List<String> lore = new ArrayList<>();
    }
    
    public static class AutoRefreshConfig {
        public boolean enabled = true;
        public int interval = 20;
    }
    
    public static class SoundsConfig {
        public SoundConfig openSound = new SoundConfig();
        public SoundConfig removeBoostSound = new SoundConfig();
        public SoundConfig closeSound = new SoundConfig();
    }
    
    public static class SoundConfig {
        public boolean enabled = true;
        public Sound sound = Sound.UI_BUTTON_CLICK;
        public float volume = 0.5f;
        public float pitch = 1.0f;
    }
    
    public static class PermissionsConfig {
        public String viewPermission = "universejobs.admin.boost.gui";
        public String removePermission = "universejobs.admin.boost.remove";
    }
    
    public static class MessagesConfig {
        public String boostRemoved = "";
        public String noPermission = "";
        public String refreshClicked = "";
    }
    
    public String getTitle() { return title; }
    public int getSize() { return size; }
    public BoostItemConfig getXpBoostConfig() { return xpBoostConfig; }
    public BoostItemConfig getMoneyBoostConfig() { return moneyBoostConfig; }
    public FillerConfig getFillerConfig() { return fillerConfig; }
    public NavigationConfig getNavigationConfig() { return navigationConfig; }
    public Map<String, CustomItemConfig> getCustomItems() { return customItems; }
    public AutoRefreshConfig getAutoRefreshConfig() { return autoRefreshConfig; }
    public SoundsConfig getSoundsConfig() { return soundsConfig; }
    public PermissionsConfig getPermissionsConfig() { return permissionsConfig; }
    public MessagesConfig getMessagesConfig() { return messagesConfig; }
    public BoostCalculationMode getBoostCalculationMode() { return boostCalculationMode; }
}