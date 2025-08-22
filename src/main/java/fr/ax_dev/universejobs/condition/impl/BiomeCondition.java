package fr.ax_dev.universejobs.condition.impl;

import fr.ax_dev.universejobs.condition.*;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Condition that checks the biome the player is in.
 */
public class BiomeCondition extends AbstractCondition {
    
    private final Set<Biome> allowedBiomes;
    private final boolean blacklist;
    
    /**
     * Create a biome condition from configuration.
     * 
     * @param config The configuration section
     */
    public BiomeCondition(ConfigurationSection config) {
        super(config);
        java.util.List<String> biomeNames = config.getStringList("biomes");
        this.allowedBiomes = biomeNames.stream()
                .map(name -> {
                    try {
                        // Try new Registry method first
                        NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
                        Biome biome = Registry.BIOME.get(key);
                        if (biome == null) {
                            // Fallback to valueOf for compatibility
                            biome = Biome.valueOf(name.toUpperCase());
                        }
                        return biome;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(biome -> biome != null)
                .collect(Collectors.toSet());
        
        this.blacklist = config.getBoolean("blacklist", false);
    }
    
    @Override
    public boolean isMet(Player player, Event event, ConditionContext context) {
        Biome playerBiome = player.getLocation().getBlock().getBiome();
        boolean inList = allowedBiomes.contains(playerBiome);
        
        return blacklist ? !inList : inList;
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.BIOME;
    }
    
    
    @Override
    public String toString() {
        return "BiomeCondition{biomes=" + allowedBiomes.size() + ", blacklist=" + blacklist + "}";
    }
}