package fr.ax_dev.universejobs.condition;

import fr.ax_dev.universejobs.config.MessageConfig;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import fr.ax_dev.universejobs.UniverseJobs;

import java.util.List;

/**
 * Abstract base class for conditions with common message/command handling.
 */
public abstract class AbstractCondition implements Condition {
    
    protected final ConditionResult denyResult;
    protected final ConditionResult acceptResult;
    
    /**
     * Create a condition from configuration with message support.
     * 
     * @param config The configuration section
     */
    protected AbstractCondition(ConfigurationSection config) {
        // Parse deny configuration
        ConfigurationSection denySection = config.getConfigurationSection("deny");
        MessageConfig denyMessage = null;
        List<String> denyCommands = null;
        Sound denySound = null;
        boolean denyCancelEvent = false;
        
        if (denySection != null) {
            // Check for new message format
            ConfigurationSection denyMessageSection = denySection.getConfigurationSection("message");
            if (denyMessageSection != null) {
                denyMessage = new MessageConfig(denyMessageSection);
            } else {
                // Legacy format support
                String legacyMessage = denySection.getString("message");
                if (legacyMessage != null) {
                    denyMessage = new MessageConfig(legacyMessage);
                }
            }
            
            // Parse commands
            denyCommands = MessageConfig.parseCommands(denySection, "commands");
            
            // Parse sound (legacy support)
            String denySoundStr = denySection.getString("sound");
            if (denySoundStr != null) {
                try {
                    // Try new Registry method first
                    NamespacedKey key = NamespacedKey.minecraft(denySoundStr.toLowerCase());
                    denySound = Registry.SOUNDS.get(key);
                    if (denySound == null) {
                        // Fallback to valueOf for compatibility
                        denySound = Sound.valueOf(denySoundStr.toUpperCase());
                    }
                } catch (Exception ignored) {}
            }
            
            denyCancelEvent = denySection.getBoolean("cancel-event", false);
        }
        
        // Parse accept configuration
        ConfigurationSection acceptSection = config.getConfigurationSection("accept");
        MessageConfig acceptMessage = null;
        List<String> acceptCommands = null;
        Sound acceptSound = null;
        boolean acceptCancelEvent = false;
        
        if (acceptSection != null) {
            // Check for new message format
            ConfigurationSection acceptMessageSection = acceptSection.getConfigurationSection("message");
            if (acceptMessageSection != null) {
                acceptMessage = new MessageConfig(acceptMessageSection);
            } else {
                // Legacy format support
                String legacyMessage = acceptSection.getString("message");
                if (legacyMessage != null) {
                    acceptMessage = new MessageConfig(legacyMessage);
                }
            }
            
            // Parse commands
            acceptCommands = MessageConfig.parseCommands(acceptSection, "commands");
            
            // Parse sound (legacy support)
            String acceptSoundStr = acceptSection.getString("sound");
            if (acceptSoundStr != null) {
                try {
                    // Try new Registry method first
                    NamespacedKey key = NamespacedKey.minecraft(acceptSoundStr.toLowerCase());
                    acceptSound = Registry.SOUNDS.get(key);
                    if (acceptSound == null) {
                        // Fallback to valueOf for compatibility
                        acceptSound = Sound.valueOf(acceptSoundStr.toUpperCase());
                    }
                } catch (Exception ignored) {}
            }
            
            acceptCancelEvent = acceptSection.getBoolean("cancel-event", false);
        }
        
        // Create enhanced condition results
        this.denyResult = new EnhancedConditionResult(false, denyMessage, denySound, denyCommands, denyCancelEvent, null, null, null, false);
        this.acceptResult = new EnhancedConditionResult(true, null, null, null, false, acceptMessage, acceptSound, acceptCommands, acceptCancelEvent);
    }
    
    @Override
    public ConditionResult getDenyResult() {
        return denyResult;
    }
    
    @Override
    public ConditionResult getAcceptResult() {
        return acceptResult;
    }
    
    /**
     * Enhanced condition result that supports the new message format.
     */
    private static class EnhancedConditionResult extends ConditionResult {
        private final MessageConfig denyMessageConfig;
        private final MessageConfig acceptMessageConfig;
        
        public EnhancedConditionResult(boolean allowed, 
                                      MessageConfig denyMessageConfig, Sound denySound, List<String> denyCommands, boolean denyCancelEvent,
                                      MessageConfig acceptMessageConfig, Sound acceptSound, List<String> acceptCommands, boolean acceptCancelEvent) {
            super(allowed, 
                  denyMessageConfig != null ? denyMessageConfig.getText() : null, 
                  denySound, denyCommands, denyCancelEvent,
                  acceptMessageConfig != null ? acceptMessageConfig.getText() : null, 
                  acceptSound, acceptCommands, acceptCancelEvent);
            this.denyMessageConfig = denyMessageConfig;
            this.acceptMessageConfig = acceptMessageConfig;
        }
        
        @Override
        public void execute(Player player) {
            MessageConfig messageConfig = isAllowed() ? acceptMessageConfig : denyMessageConfig;
            List<String> commands = isAllowed() ? getAcceptCommands() : getDenyCommands();
            Sound sound = isAllowed() ? getAcceptSound() : getDenySound();
            
            // Handle message display based on type
            if (messageConfig != null && messageConfig.hasContent()) {
                String text = messageConfig.getText();
                
                switch (messageConfig.getType()) {
                    case CHAT:
                        MessageUtils.sendMessage(player, text);
                        break;
                        
                    case ACTIONBAR:
                        MessageUtils.sendActionBar(player, text);
                        // Schedule clear after duration
                        if (messageConfig.getDuration() > 0) {
                            UniverseJobs.getInstance().getFoliaManager().runLater(() -> {
                                if (player.isOnline()) {
                                    MessageUtils.sendActionBar(player, "");
                                }
                            }, messageConfig.getDuration());
                        }
                        break;
                        
                    case BOSSBAR:
                        BossBar bossBar = org.bukkit.Bukkit.createBossBar(
                            MessageUtils.colorize(text),
                            messageConfig.getBossbarColor(),
                            messageConfig.getBossbarStyle()
                        );
                        bossBar.setProgress(1.0);
                        bossBar.addPlayer(player);
                        
                        // Remove after duration
                        if (messageConfig.getDuration() > 0) {
                            UniverseJobs.getInstance().getFoliaManager().runLater(() -> {
                                if (player.isOnline()) {
                                    bossBar.removePlayer(player);
                                }
                            }, messageConfig.getDuration());
                        }
                        break;
                }
            }
            
            // Play sound
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
            
            // Execute commands
            if (commands != null && !commands.isEmpty()) {
                for (String command : commands) {
                    String processedCommand = command.replace("{player}", player.getName());
                    player.getServer().dispatchCommand(
                        player.getServer().getConsoleSender(),
                        processedCommand
                    );
                }
            }
        }
    }
}