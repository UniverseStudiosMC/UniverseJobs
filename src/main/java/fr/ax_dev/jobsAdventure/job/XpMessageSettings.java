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
    
    private final MessageType messageType;
    private final String text;
    private final int actionbarDuration;
    private final BossBarColor bossbarColor;
    private final BossBarStyle bossbarStyle;
    private final int bossbarDuration;
    private final boolean bossbarShowProgress;
    private final String xpMessageFormat;
    private final String moneyMessageFormat;
    
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
            
            // Support both "color" and "bossbar.color" configuration paths
            String colorStr = config.getString("color", config.getString("bossbar.color", "green")).toUpperCase();
            BossBarColor tempBossbarColor;
            try {
                tempBossbarColor = BossBarColor.valueOf(colorStr);
            } catch (IllegalArgumentException e) {
                tempBossbarColor = BossBarColor.GREEN;
            }
            this.bossbarColor = tempBossbarColor;
            
            // Support both "style" and "bossbar.style" configuration paths
            String styleStr = config.getString("style", config.getString("bossbar.style", "solid")).toUpperCase();
            BossBarStyle tempBossbarStyle;
            try {
                tempBossbarStyle = BossBarStyle.valueOf(styleStr);
            } catch (IllegalArgumentException e) {
                tempBossbarStyle = BossBarStyle.SOLID;
            }
            this.bossbarStyle = tempBossbarStyle;
            
            // Support both "duration" and "bossbar.duration" configuration paths
            this.bossbarDuration = config.getInt("duration", config.getInt("bossbar.duration", 60));
            this.bossbarShowProgress = config.getBoolean("show-progress", false);
            
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
}