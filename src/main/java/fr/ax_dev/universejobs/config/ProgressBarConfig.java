package fr.ax_dev.universejobs.config;

import org.bukkit.configuration.ConfigurationSection;

public class ProgressBarConfig {
    
    private final int length;
    private final String completedChar;
    private final String remainingChar;
    private final String completedColor;
    private final String remainingColor;
    private final String percentageColor;
    private final String withPercentageFormat;
    private final String barOnlyFormat;
    private final String percentageOnlyFormat;
    
    public ProgressBarConfig(ConfigurationSection config) {
        this.length = config.getInt("length", 20);
        this.completedChar = config.getString("characters.completed", "█");
        this.remainingChar = config.getString("characters.remaining", "░");
        this.completedColor = config.getString("colors.completed", "<#32CD32>");
        this.remainingColor = config.getString("colors.remaining", "<#404040>");
        this.percentageColor = config.getString("colors.percentage", "<#FFD700>");
        this.withPercentageFormat = config.getString("format.with-percentage", "{completed_bar}{remaining_bar} {percentage_color}{percentage}%");
        this.barOnlyFormat = config.getString("format.bar-only", "{completed_bar}{remaining_bar}");
        this.percentageOnlyFormat = config.getString("format.percentage-only", "{percentage_color}{percentage}%");
    }
    
    public String generateProgressBar(double currentXp, double requiredXp, boolean includePercentage) {
        double percentage = Math.min((currentXp / requiredXp) * 100.0, 100.0);
        int completedLength = (int) Math.round((percentage / 100.0) * length);
        int remainingLength = length - completedLength;
        
        String completedBar = completedColor + completedChar.repeat(Math.max(0, completedLength));
        String remainingBar = remainingColor + remainingChar.repeat(Math.max(0, remainingLength));
        
        String format = includePercentage ? withPercentageFormat : barOnlyFormat;
        
        return format
                .replace("{completed_bar}", completedBar)
                .replace("{remaining_bar}", remainingBar)
                .replace("{percentage_color}", percentageColor)
                .replace("{percentage}", String.valueOf((int) Math.round(percentage)));
    }
    
    public String generatePercentageOnly(double currentXp, double requiredXp) {
        double percentage = Math.min((currentXp / requiredXp) * 100.0, 100.0);
        return percentageOnlyFormat
                .replace("{percentage_color}", percentageColor)
                .replace("{percentage}", String.valueOf((int) Math.round(percentage)));
    }
    
    // Getters
    public int getLength() { return length; }
    public String getCompletedChar() { return completedChar; }
    public String getRemainingChar() { return remainingChar; }
    public String getCompletedColor() { return completedColor; }
    public String getRemainingColor() { return remainingColor; }
    public String getPercentageColor() { return percentageColor; }
    public String getWithPercentageFormat() { return withPercentageFormat; }
    public String getBarOnlyFormat() { return barOnlyFormat; }
    public String getPercentageOnlyFormat() { return percentageOnlyFormat; }
}