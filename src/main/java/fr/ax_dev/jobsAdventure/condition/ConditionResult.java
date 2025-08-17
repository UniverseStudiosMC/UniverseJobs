package fr.ax_dev.jobsAdventure.condition;

import fr.ax_dev.jobsAdventure.utils.MessageUtils;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Represents the result of a condition check, including actions to take.
 */
public class ConditionResult {
    
    private final boolean allowed;
    private final String denyMessage;
    private final Sound denySound;
    private final List<String> denyCommands;
    private final boolean denyCancelEvent;
    private final String acceptMessage;
    private final Sound acceptSound;
    private final List<String> acceptCommands;
    private final boolean acceptCancelEvent;
    private final boolean isDefault;
    
    /**
     * Create a new condition result with separate accept/deny actions.
     * 
     * @param allowed Whether the action is allowed
     * @param denyMessage Message to send when denied (null for none)
     * @param denySound Sound to play when denied (null for none)
     * @param denyCommands Commands to execute when denied (null for none)
     * @param denyCancelEvent Whether to cancel the event when denied
     * @param acceptMessage Message to send when accepted (null for none)
     * @param acceptSound Sound to play when accepted (null for none)
     * @param acceptCommands Commands to execute when accepted (null for none)
     * @param acceptCancelEvent Whether to cancel the event when accepted
     */
    public ConditionResult(boolean allowed, String denyMessage, Sound denySound, List<String> denyCommands, boolean denyCancelEvent,
                          String acceptMessage, Sound acceptSound, List<String> acceptCommands, boolean acceptCancelEvent) {
        this.allowed = allowed;
        this.denyMessage = denyMessage;
        this.denySound = denySound;
        this.denyCommands = denyCommands;
        this.denyCancelEvent = denyCancelEvent;
        this.acceptMessage = acceptMessage;
        this.acceptSound = acceptSound;
        this.acceptCommands = acceptCommands;
        this.acceptCancelEvent = acceptCancelEvent;
        this.isDefault = false;
    }
    
    /**
     * Create a condition result from configuration section (for accept results).
     * 
     * @param config The configuration section containing result data
     */
    public ConditionResult(ConfigurationSection config) {
        this(config, true);
    }
    
    /**
     * Create a condition result from configuration section.
     * 
     * @param config The configuration section containing result data
     * @param isAccept Whether this is an accept (true) or deny (false) result
     */
    public ConditionResult(ConfigurationSection config, boolean isAccept) {
        this.allowed = isAccept;
        
        if (isAccept) {
            this.denyMessage = null;
            this.denySound = null;
            this.denyCommands = null;
            this.denyCancelEvent = false;
            
            this.acceptMessage = config.getString("message");
            this.acceptSound = parseSound(config.getString("sound"));
            this.acceptCommands = config.getStringList("commands");
            this.acceptCancelEvent = config.getBoolean("cancelEvent", false);
        } else {
            this.denyMessage = config.getString("message");
            this.denySound = parseSound(config.getString("sound"));
            this.denyCommands = config.getStringList("commands");
            this.denyCancelEvent = config.getBoolean("cancelEvent", false);
            
            this.acceptMessage = null;
            this.acceptSound = null;
            this.acceptCommands = null;
            this.acceptCancelEvent = false;
        }
        
        this.isDefault = false;
    }
    
    /**
     * Create a new condition result with separate accept/deny actions (legacy).
     * 
     * @param allowed Whether the action is allowed
     * @param denyMessage Message to send when denied (null for none)
     * @param denySound Sound to play when denied (null for none)
     * @param denyCommands Commands to execute when denied (null for none)
     * @param acceptMessage Message to send when accepted (null for none)
     * @param acceptSound Sound to play when accepted (null for none)
     * @param acceptCommands Commands to execute when accepted (null for none)
     */
    public ConditionResult(boolean allowed, String denyMessage, Sound denySound, List<String> denyCommands,
                          String acceptMessage, Sound acceptSound, List<String> acceptCommands) {
        this(allowed, denyMessage, denySound, denyCommands, false, acceptMessage, acceptSound, acceptCommands, false);
    }
    
    /**
     * Legacy constructor for backward compatibility.
     * 
     * @param allowed Whether the action is allowed
     * @param message Message to send to the player (null for none)
     * @param sound Sound to play (null for none)
     * @param commands Commands to execute (null for none)
     */
    public ConditionResult(boolean allowed, String message, Sound sound, List<String> commands) {
        this(allowed, 
             allowed ? null : message, 
             allowed ? null : sound, 
             allowed ? null : commands,
             false,
             allowed ? message : null,
             allowed ? sound : null,
             allowed ? commands : null,
             false);
    }
    
    /**
     * Parse a sound string to Sound enum.
     * 
     * @param soundString The sound string
     * @return The Sound or null if invalid
     */
    private Sound parseSound(String soundString) {
        if (soundString == null || soundString.isEmpty()) {
            return null;
        }
        try {
            return Sound.valueOf(soundString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Private constructor for default results.
     */
    private ConditionResult(boolean allowed, boolean isDefault) {
        this.allowed = allowed;
        this.denyMessage = null;
        this.denySound = null;
        this.denyCommands = null;
        this.denyCancelEvent = false;
        this.acceptMessage = null;
        this.acceptSound = null;
        this.acceptCommands = null;
        this.acceptCancelEvent = false;
        this.isDefault = isDefault;
    }

    /**
     * Create an allowed result with no actions.
     * 
     * @return Allowed result
     */
    public static ConditionResult allow() {
        return new ConditionResult(true, true);
    }
    
    /**
     * Create an allowed result with accept actions.
     * 
     * @param message The message to send
     * @param sound The sound to play
     * @param commands The commands to execute
     * @return Allowed result with actions
     */
    public static ConditionResult allow(String message, Sound sound, List<String> commands) {
        return new ConditionResult(true, null, null, null, message, sound, commands);
    }
    
    /**
     * Create a denied result with no actions.
     * 
     * @return Denied result
     */
    public static ConditionResult deny() {
        return new ConditionResult(false, true);
    }
    
    /**
     * Create a denied result with a message.
     * 
     * @param message The message to send
     * @return Denied result with message
     */
    public static ConditionResult deny(String message) {
        return new ConditionResult(false, message, null, null, null, null, null);
    }
    
    /**
     * Create a denied result with message and sound.
     * 
     * @param message The message to send
     * @param sound The sound to play
     * @return Denied result with message and sound
     */
    public static ConditionResult deny(String message, Sound sound) {
        return new ConditionResult(false, message, sound, null, null, null, null);
    }
    
    /**
     * Create a denied result with all actions.
     * 
     * @param message The message to send
     * @param sound The sound to play
     * @param commands The commands to execute
     * @return Denied result with all actions
     */
    public static ConditionResult deny(String message, Sound sound, List<String> commands) {
        return new ConditionResult(false, message, sound, commands, null, null, null);
    }
    
    /**
     * Check if the action is allowed.
     * 
     * @return true if allowed
     */
    public boolean isAllowed() {
        return allowed;
    }
    
    /**
     * Check if this is a default result (no custom messages/actions).
     * 
     * @return true if this is a default result
     */
    public boolean isDefault() {
        return isDefault;
    }
    
    /**
     * Get the deny message.
     * 
     * @return The deny message or null
     */
    public String getDenyMessage() {
        return denyMessage;
    }
    
    /**
     * Get the deny sound.
     * 
     * @return The deny sound or null
     */
    public Sound getDenySound() {
        return denySound;
    }
    
    /**
     * Get the deny commands.
     * 
     * @return The deny commands or null
     */
    public List<String> getDenyCommands() {
        return denyCommands;
    }
    
    /**
     * Get the accept message.
     * 
     * @return The accept message or null
     */
    public String getAcceptMessage() {
        return acceptMessage;
    }
    
    /**
     * Get the accept sound.
     * 
     * @return The accept sound or null
     */
    public Sound getAcceptSound() {
        return acceptSound;
    }
    
    /**
     * Get the accept commands.
     * 
     * @return The accept commands or null
     */
    public List<String> getAcceptCommands() {
        return acceptCommands;
    }
    
    /**
     * Check if the event should be cancelled.
     * 
     * @return true if the event should be cancelled
     */
    public boolean shouldCancelEvent() {
        return allowed ? acceptCancelEvent : denyCancelEvent;
    }
    
    /**
     * Get whether to cancel event on deny.
     * 
     * @return true if event should be cancelled on deny
     */
    public boolean getDenyCancelEvent() {
        return denyCancelEvent;
    }
    
    /**
     * Get whether to cancel event on accept.
     * 
     * @return true if event should be cancelled on accept
     */
    public boolean getAcceptCancelEvent() {
        return acceptCancelEvent;
    }
    
    /**
     * Execute the result actions for a player.
     * 
     * @param player The player
     */
    public void execute(Player player) {
        String message;
        Sound sound;
        List<String> commands;
        
        if (allowed) {
            message = acceptMessage;
            sound = acceptSound;
            commands = acceptCommands;
        } else {
            message = denyMessage;
            sound = denySound;
            commands = denyCommands;
        }
        
        if (message != null && !message.isEmpty()) {
            MessageUtils.sendMessage(player, message);
        }
        
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
        
        if (commands != null && !commands.isEmpty()) {
            for (String command : commands) {
                // Replace placeholders in command
                String processedCommand = command.replace("{player}", player.getName());
                
                // Execute as console command
                player.getServer().dispatchCommand(
                    player.getServer().getConsoleSender(),
                    processedCommand
                );
            }
        }
    }
    
    @Override
    public String toString() {
        return "ConditionResult{allowed=" + allowed + 
               ", hasDenyMessage=" + (denyMessage != null) + 
               ", hasDenySound=" + (denySound != null) + 
               ", denyCommandCount=" + (denyCommands != null ? denyCommands.size() : 0) +
               ", denyCancelEvent=" + denyCancelEvent +
               ", hasAcceptMessage=" + (acceptMessage != null) + 
               ", hasAcceptSound=" + (acceptSound != null) + 
               ", acceptCommandCount=" + (acceptCommands != null ? acceptCommands.size() : 0) + 
               ", acceptCancelEvent=" + acceptCancelEvent + "}";
    }
}