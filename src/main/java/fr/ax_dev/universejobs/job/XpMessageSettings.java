package fr.ax_dev.universejobs.job;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration settings for how XP messages are displayed for a specific job.
 */
public class XpMessageSettings {
    
    private static final String CONFIG_DURATION = "duration";
    
    public enum MessageType {
        CHAT,
        ACTIONBAR,
        BOSSBAR
    }
    
    public enum BossBarColor {
        PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
    }
    
    public enum BossBarStyle {
        SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
    }
    
    public enum BossBarFlag {
        DARKEN_SKY, PLAY_BOSS_MUSIC, CREATE_FOG
    }
    
    private final MessageType messageType;
    private final String text;
    private final int actionbarDuration;
    private final BossBarColor bossbarColor;
    private final BossBarStyle bossbarStyle;
    private int bossbarDuration;
    private final boolean bossbarShowProgress;
    private final String xpMessageFormat;
    private final String moneyMessageFormat;
    private final java.util.Set<BossBarFlag> bossbarFlags;
    
    // TITLE specific settings
    private final int titleFadeIn;
    private final int titleFadeOut;
    private final int titleStay;
    
    /**
     * Create XP message settings from configuration.
     * 
     * @param config The configuration section, may be null for defaults
     */
    public XpMessageSettings(ConfigurationSection config) {
        if (config == null) {
            // Default settings
            this.messageType = MessageType.ACTIONBAR;
            this.text = "&e+{xp} XP ({job})";
            this.actionbarDuration = 60;
            this.bossbarColor = BossBarColor.GREEN;
            this.bossbarStyle = BossBarStyle.SOLID;
            this.bossbarDuration = 60;
            this.bossbarShowProgress = false;
            this.xpMessageFormat = "+{xp} XP";
            this.moneyMessageFormat = "+{money} money";
            this.bossbarFlags = new java.util.HashSet<>();
            this.titleFadeIn = 10;
            this.titleFadeOut = 20;
            this.titleStay = 70;
        } else {
            // Parse configuration
            this.messageType = parseMessageType(config);
            this.text = config.getString("text", "&e+{xp} XP ({job})");
            this.actionbarDuration = config.getInt("actionbar.duration", 60);
            
            ConfigurationSection optionsSection = config.getConfigurationSection("options");
            
            this.bossbarColor = parseBossBarColor(config, optionsSection);
            this.bossbarStyle = parseBossBarStyle(config, optionsSection);
            this.bossbarDuration = parseDuration(config, optionsSection);
            this.bossbarShowProgress = parseShowProgress(config, optionsSection);
            this.bossbarFlags = parseBossBarFlags(optionsSection);
            
            // Title settings
            int[] titleSettings = parseTitleSettingsArray(optionsSection);
            this.titleFadeIn = titleSettings[0];
            this.titleFadeOut = titleSettings[1];
            this.titleStay = titleSettings[2];
            
            // Custom message formats
            this.xpMessageFormat = config.getString("xp", "+{xp} XP");
            this.moneyMessageFormat = config.getString("money", "+{money} money");
        }
    }
    
    /**
     * Parse message type from config.
     */
    private MessageType parseMessageType(ConfigurationSection config) {
        String typeStr = config.getString("type", "actionbar").toUpperCase();
        try {
            return MessageType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return MessageType.ACTIONBAR;
        }
    }
    
    /**
     * Parse BossBar color from config.
     */
    private BossBarColor parseBossBarColor(ConfigurationSection config, ConfigurationSection optionsSection) {
        String colorStr = getConfigValue(optionsSection, config, "color", "bossbar.color", "green");
        try {
            return BossBarColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBarColor.GREEN;
        }
    }
    
    /**
     * Parse BossBar style from config.
     */
    private BossBarStyle parseBossBarStyle(ConfigurationSection config, ConfigurationSection optionsSection) {
        String styleStr = getConfigValue(optionsSection, config, "style", "bossbar.style", "solid");
        try {
            return BossBarStyle.valueOf(styleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBarStyle.SOLID;
        }
    }
    
    /**
     * Parse duration from config, supporting both string and int values.
     */
    private int parseDuration(ConfigurationSection config, ConfigurationSection optionsSection) {
        if (optionsSection != null) {
            Object durationObj = optionsSection.get(CONFIG_DURATION);
            if (durationObj instanceof String) {
                try {
                    return Integer.parseInt((String) durationObj);
                } catch (NumberFormatException e) {
                    return 60; // Default fallback
                }
            } else {
                return optionsSection.getInt(CONFIG_DURATION, 60);
            }
        } else {
            return config.getInt(CONFIG_DURATION, config.getInt("bossbar.duration", 60));
        }
    }
    
    /**
     * Parse show progress setting from config.
     */
    private boolean parseShowProgress(ConfigurationSection config, ConfigurationSection optionsSection) {
        if (optionsSection != null) {
            return optionsSection.getBoolean("show-progress", false);
        } else {
            return config.getBoolean("show-progress", false);
        }
    }
    
    /**
     * Parse BossBar flags from config.
     */
    private java.util.Set<BossBarFlag> parseBossBarFlags(ConfigurationSection optionsSection) {
        java.util.Set<BossBarFlag> flags = new java.util.HashSet<>();
        if (optionsSection != null) {
            java.util.List<String> flagStrings = optionsSection.getStringList("flags");
            for (String flagStr : flagStrings) {
                try {
                    BossBarFlag flag = BossBarFlag.valueOf(flagStr.toUpperCase());
                    flags.add(flag);
                } catch (IllegalArgumentException e) {
                    // Invalid flag, skip it
                }
            }
        }
        return flags;
    }
    
    /**
     * Parse title-specific settings from config and return as array.
     * @return int array with [fadeIn, fadeOut, stay]
     */
    private int[] parseTitleSettingsArray(ConfigurationSection optionsSection) {
        if (optionsSection != null) {
            return new int[] {
                optionsSection.getInt("fade-in", 10),
                optionsSection.getInt("fade-out", 20),
                optionsSection.getInt("stay", 70)
            };
        } else {
            // Default values
            return new int[] { 10, 20, 70 };
        }
    }
    
    /**
     * Get config value with fallback logic.
     */
    private String getConfigValue(ConfigurationSection optionsSection, ConfigurationSection config, 
                                 String optionsKey, String fallbackKey, String defaultValue) {
        if (optionsSection != null) {
            return optionsSection.getString(optionsKey, defaultValue);
        } else {
            return config.getString(optionsKey, config.getString(fallbackKey, defaultValue));
        }
    }
    
    /**
     * Get the message display type.
     * 
     * @return The message type
     */
    public MessageType getMessageType() {
        return messageType;
    }
    
    /**
     * Get the custom text format.
     * 
     * @return The text format with placeholders
     */
    public String getText() {
        return text;
    }
    
    /**
     * Get the actionbar display duration in ticks.
     * 
     * @return Duration in ticks
     */
    public int getActionbarDuration() {
        return actionbarDuration;
    }
    
    /**
     * Get the bossbar color.
     * 
     * @return The bossbar color
     */
    public BossBarColor getBossbarColor() {
        return bossbarColor;
    }
    
    /**
     * Get the bossbar style (segments).
     * 
     * @return The bossbar style
     */
    public BossBarStyle getBossbarStyle() {
        return bossbarStyle;
    }
    
    /**
     * Get the bossbar display duration in ticks.
     * 
     * @return Duration in ticks
     */
    public int getBossbarDuration() {
        return bossbarDuration;
    }
    
    /**
     * Check if bossbar should show XP progress instead of full bar.
     * 
     * @return true if progress should be shown
     */
    public boolean shouldShowProgress() {
        return bossbarShowProgress;
    }
    
    /**
     * Get the custom XP message format.
     * 
     * @return The XP message format with {xp} placeholder
     */
    public String getXpMessageFormat() {
        return xpMessageFormat;
    }
    
    /**
     * Get the custom money message format.
     * 
     * @return The money message format with {money} placeholder
     */
    public String getMoneyMessageFormat() {
        return moneyMessageFormat;
    }
    
    /**
     * Get the bossbar flags.
     * 
     * @return Set of bossbar flags
     */
    public java.util.Set<BossBarFlag> getBossbarFlags() {
        return new java.util.HashSet<>(bossbarFlags);
    }
    
    /**
     * Get the title fade-in duration in ticks.
     * 
     * @return Fade-in duration in ticks
     */
    public int getTitleFadeIn() {
        return titleFadeIn;
    }
    
    /**
     * Get the title fade-out duration in ticks.
     * 
     * @return Fade-out duration in ticks
     */
    public int getTitleFadeOut() {
        return titleFadeOut;
    }
    
    /**
     * Get the title stay duration in ticks.
     * 
     * @return Stay duration in ticks
     */
    public int getTitleStay() {
        return titleStay;
    }
    
    /**
     * Process the message text by replacing custom placeholders.
     * Replaces {message_xp} and {message_money} with their respective formats.
     * 
     * @param xp The XP amount
     * @param money The money amount
     * @return The processed message text
     */
    public String processMessage(double xp, double money) {
        String processedText = this.text;
        
        // Replace {message_xp} with the formatted XP message
        if (xp > 0) {
            String xpMessage = xpMessageFormat.replace("{xp}", String.valueOf((int) xp));
            processedText = processedText.replace("{message_xp}", xpMessage);
        } else {
            processedText = processedText.replace("{message_xp}", "");
        }
        
        // Replace {message_money} with the formatted money message
        if (money > 0) {
            String moneyMessage = moneyMessageFormat.replace("{money}", String.valueOf(money));
            processedText = processedText.replace("{message_money}", moneyMessage);
        } else {
            processedText = processedText.replace("{message_money}", "");
        }
        
        // Replace standard placeholders
        processedText = processedText.replace("{xp}", String.valueOf((int) xp));
        processedText = processedText.replace("{money}", String.valueOf(money));
        
        return processedText;
    }
    
    /**
     * Convert to Bukkit BossBar color.
     * 
     * @return The Bukkit BarColor
     */
    public org.bukkit.boss.BarColor toBukkitBarColor() {
        switch (bossbarColor) {
            case PINK: return org.bukkit.boss.BarColor.PINK;
            case BLUE: return org.bukkit.boss.BarColor.BLUE;
            case RED: return org.bukkit.boss.BarColor.RED;
            case GREEN: return org.bukkit.boss.BarColor.GREEN;
            case YELLOW: return org.bukkit.boss.BarColor.YELLOW;
            case PURPLE: return org.bukkit.boss.BarColor.PURPLE;
            case WHITE: return org.bukkit.boss.BarColor.WHITE;
            default: return org.bukkit.boss.BarColor.GREEN;
        }
    }
    
    /**
     * Convert to Bukkit BossBar style.
     * 
     * @return The Bukkit BarStyle
     */
    public org.bukkit.boss.BarStyle toBukkitBarStyle() {
        switch (bossbarStyle) {
            case SOLID: return org.bukkit.boss.BarStyle.SOLID;
            case SEGMENTED_6: return org.bukkit.boss.BarStyle.SEGMENTED_6;
            case SEGMENTED_10: return org.bukkit.boss.BarStyle.SEGMENTED_10;
            case SEGMENTED_12: return org.bukkit.boss.BarStyle.SEGMENTED_12;
            case SEGMENTED_20: return org.bukkit.boss.BarStyle.SEGMENTED_20;
            default: return org.bukkit.boss.BarStyle.SOLID;
        }
    }
    
    /**
     * Convert to Bukkit BossBar flags.
     * 
     * @return Array of Bukkit BarFlag
     */
    public org.bukkit.boss.BarFlag[] toBukkitBarFlags() {
        java.util.List<org.bukkit.boss.BarFlag> bukkitFlags = new java.util.ArrayList<>();
        
        for (BossBarFlag flag : bossbarFlags) {
            switch (flag) {
                case DARKEN_SKY:
                    bukkitFlags.add(org.bukkit.boss.BarFlag.DARKEN_SKY);
                    break;
                case PLAY_BOSS_MUSIC:
                    bukkitFlags.add(org.bukkit.boss.BarFlag.PLAY_BOSS_MUSIC);
                    break;
                case CREATE_FOG:
                    bukkitFlags.add(org.bukkit.boss.BarFlag.CREATE_FOG);
                    break;
            }
        }
        
        return bukkitFlags.toArray(new org.bukkit.boss.BarFlag[0]);
    }
}