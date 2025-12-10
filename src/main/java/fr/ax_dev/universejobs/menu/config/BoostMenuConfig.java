package fr.ax_dev.universejobs.menu.config;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.item.ModelDataComponentConfig;
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
                xpBoostConfig.material = fr.ax_dev.universejobs.utils.EnumUtils.parseMaterial(itemSection.getString("material", "EXPERIENCE_BOTTLE"), Material.EXPERIENCE_BOTTLE);
                xpBoostConfig.displayName = itemSection.getString("display-name", "<!italic><white>XP Boost <gold>{boost_id}</gold></white>");
                xpBoostConfig.lore = itemSection.getStringList("lore");
                xpBoostConfig.glow = itemSection.getBoolean("glow", false);
                xpBoostConfig.modelData = ModelDataComponentConfig.fromSection(itemSection);
            }
        }
        
        ConfigurationSection moneySection = config.getConfigurationSection("money-boosts");
        if (moneySection != null) {
            moneyBoostConfig = new BoostItemConfig();
            moneyBoostConfig.enabled = moneySection.getBoolean("enabled", true);
            moneyBoostConfig.slots = moneySection.getIntegerList("slots");
            
            ConfigurationSection itemSection = moneySection.getConfigurationSection("item");
            if (itemSection != null) {
                moneyBoostConfig.material = fr.ax_dev.universejobs.utils.EnumUtils.parseMaterial(itemSection.getString("material", "GOLD_INGOT"), Material.GOLD_INGOT);
                moneyBoostConfig.displayName = itemSection.getString("display-name", "<!italic><white>Money Boost <gold>{boost_id}</gold></white>");
                moneyBoostConfig.lore = itemSection.getStringList("lore");
                moneyBoostConfig.glow = itemSection.getBoolean("glow", false);
                moneyBoostConfig.modelData = ModelDataComponentConfig.fromSection(itemSection);
            }
        }
    }
    
    private void loadFillerConfig() {
        ConfigurationSection fillerSection = config.getConfigurationSection("filler");
        if (fillerSection != null) {
            fillerConfig = new FillerConfig();
            fillerConfig.enabled = fillerSection.getBoolean("enabled", true);
            fillerConfig.material = fr.ax_dev.universejobs.utils.EnumUtils.parseMaterial(fillerSection.getString("material", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
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
            item.material = fr.ax_dev.universejobs.utils.EnumUtils.parseMaterial(section.getString("material", "BARRIER"), Material.BARRIER);
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
                    customItem.material = fr.ax_dev.universejobs.utils.EnumUtils.parseMaterial(itemSection.getString("material", "STONE"), Material.STONE);
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
            sound.sound = parseSoundSafely(section.getString("sound", "UI_BUTTON_CLICK"));
            sound.volume = (float) section.getDouble("volume", 0.5);
            sound.pitch = (float) section.getDouble("pitch", 1.0);
        }
    }
    
    private Sound parseSoundSafely(String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            return Sound.UI_BUTTON_CLICK;
        }
        
        try {
            // Method 1: Try using Registry.SOUNDS (Paper 1.20+)
            try {
                // First try with the name as-is (for ENTITY_PLAYER_LEVELUP format)
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(
                    soundName.toLowerCase().replace("_", ".")
                );
                
                // Use reflection to access Registry.SOUNDS for compatibility
                java.lang.reflect.Field soundsField = org.bukkit.Registry.class.getField("SOUNDS");
                Object soundsRegistry = soundsField.get(null);
                
                // Call get() method on the registry
                java.lang.reflect.Method getMethod = soundsRegistry.getClass().getMethod("get", org.bukkit.NamespacedKey.class);
                Object result = getMethod.invoke(soundsRegistry, key);
                
                if (result instanceof Sound) {
                    return (Sound) result;
                }
            } catch (NoSuchFieldException e) {
                // Registry.SOUNDS doesn't exist, try fallback methods
            } catch (Exception e) {
                // Registry method failed, try next approach
            }
            
            // Method 2: Try using Sound.valueOf() with reflection to avoid direct call
            try {
                java.lang.reflect.Method valueOfMethod = Sound.class.getMethod("valueOf", String.class);
                Object result = valueOfMethod.invoke(null, soundName.toUpperCase());
                if (result instanceof Sound) {
                    return (Sound) result;
                }
            } catch (Exception e) {
                // valueOf failed, try next approach
            }
            
            // Method 3: Try getting field directly via reflection
            try {
                java.lang.reflect.Field field = Sound.class.getField(soundName.toUpperCase());
                if (field.getType() == Sound.class) {
                    return (Sound) field.get(null);
                }
            } catch (Exception e) {
                // Field access failed
            }
            
        } catch (Exception e) {
            // All methods failed
        }
        
        // Fallback to default sound
        return Sound.UI_BUTTON_CLICK;
    }
    
    public static class BoostItemConfig {
        public boolean enabled = true;
        public List<Integer> slots = new ArrayList<>();
        public Material material = Material.EXPERIENCE_BOTTLE;
        public String displayName = "";
        public List<String> lore = new ArrayList<>();
        public boolean glow = false;
        public ModelDataComponentConfig modelData = ModelDataComponentConfig.empty();
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
    
    
    public String getTitle() { return title; }
    public int getSize() { return size; }
    public BoostItemConfig getXpBoostConfig() { return xpBoostConfig; }
    public BoostItemConfig getMoneyBoostConfig() { return moneyBoostConfig; }
    public FillerConfig getFillerConfig() { return fillerConfig; }
    public NavigationConfig getNavigationConfig() { return navigationConfig; }
    public Map<String, CustomItemConfig> getCustomItems() { return customItems; }
    public AutoRefreshConfig getAutoRefreshConfig() { return autoRefreshConfig; }
    public SoundsConfig getSoundsConfig() { return soundsConfig; }
}