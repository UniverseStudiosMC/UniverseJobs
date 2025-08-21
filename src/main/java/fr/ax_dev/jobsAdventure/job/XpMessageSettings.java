package fr.ax_dev.jobsAdventure.job;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration settings for how XP messages are displayed for a specific job.
 */
public class XpMessageSettings {
    
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
            this.text = "&e+{xp} XP ({job})"; // Default text format
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
            String typeStr = config.getString("type", "actionbar").toUpperCase();
            MessageType tempMessageType;
            try {
                tempMessageType = MessageType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                tempMessageType = MessageType.ACTIONBAR;
            }
            this.messageType = tempMessageType;
            
            this.text = config.getString("text", "&e+{xp} XP ({job})");
            this.actionbarDuration = config.getInt("actionbar.duration", 60);
            
            // Check for options section first, then fallback to direct config
            ConfigurationSection optionsSection = config.getConfigurationSection("options");
            
            // Color configuration
            String colorStr;
            if (optionsSection != null) {
                colorStr = optionsSection.getString("color", "green");
            } else {
                // Fallback to old format
                colorStr = config.getString("color", config.getString("bossbar.color", "green"));
            }
            BossBarColor tempBossbarColor;
            try {
                tempBossbarColor = BossBarColor.valueOf(colorStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                tempBossbarColor = BossBarColor.GREEN;
            }
            this.bossbarColor = tempBossbarColor;
            
            // Style configuration
            String styleStr;
            if (optionsSection != null) {
                styleStr = optionsSection.getString("style", "solid");
            } else {
                // Fallback to old format
                styleStr = config.getString("style", config.getString("bossbar.style", "solid"));
            }
            BossBarStyle tempBossbarStyle;
            try {
                tempBossbarStyle = BossBarStyle.valueOf(styleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                tempBossbarStyle = BossBarStyle.SOLID;
            }
            this.bossbarStyle = tempBossbarStyle;
            
            // Duration configuration (always in ticks) - works for BOSSBAR, ACTIONBAR, TITLE
            if (optionsSection != null) {
                // Support both string and int for duration
                Object durationObj = optionsSection.get("duration");
                if (durationObj instanceof String) {
                    try {
                        this.bossbarDuration = Integer.parseInt((String) durationObj);
                    } catch (NumberFormatException e) {
                        this.bossbarDuration = 60; // Default fallback
                    }
                } else {
                    this.bossbarDuration = optionsSection.getInt("duration", 60);
                }
            } else {
                // Fallback to old format
                this.bossbarDuration = config.getInt("duration", config.getInt("bossbar.duration", 60));
            }
            
            // Show progress configuration
            if (optionsSection != null) {
                this.bossbarShowProgress = optionsSection.getBoolean("show-progress", false);
            } else {
                // Fallback to old format
                this.bossbarShowProgress = config.getBoolean("show-progress", false);
            }
            
            // BossBar flags configuration
            this.bossbarFlags = new java.util.HashSet<>();
            if (optionsSection != null) {
                java.util.List<String> flagStrings = optionsSection.getStringList("flags");
                for (String flagStr : flagStrings) {
                    try {
                        BossBarFlag flag = BossBarFlag.valueOf(flagStr.toUpperCase());
                        this.bossbarFlags.add(flag);
                    } catch (IllegalArgumentException e) {
                        // Invalid flag, skip it
                    }
                }
            }
            
            // TITLE specific configurations
            if (optionsSection != null) {
                this.titleFadeIn = optionsSection.getInt("fade-in", 10);
                this.titleFadeOut = optionsSection.getInt("fade-out", 20);
                this.titleStay = optionsSection.getInt("stay", 70);
            } else {
                // Default values
                this.titleFadeIn = 10;
                this.titleFadeOut = 20;
                this.titleStay = 70;
            }
            
            // Custom message formats
            this.xpMessageFormat = config.getString("xp", "+{xp} XP");
            this.moneyMessageFormat = config.getString("money", "+{money} money");
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