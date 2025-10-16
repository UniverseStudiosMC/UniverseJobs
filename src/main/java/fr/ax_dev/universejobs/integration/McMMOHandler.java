package fr.ax_dev.universejobs.integration;

import com.gmail.nossr50.api.AbilityAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.logging.Logger;

public class McMMOHandler {

    private static McMMOHandler instance;
    private final Logger logger;
    private boolean enabled = false;

    private McMMOHandler(Plugin plugin) {
        this.logger = plugin.getLogger();
        initialize(plugin);
    }

    public static McMMOHandler getInstance(Plugin plugin) {
        if (instance == null) {
            instance = new McMMOHandler(plugin);
        }
        return instance;
    }

    private void initialize(Plugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("mcMMO") != null) {
            enabled = true;
            logger.info("[UniverseJobs] mcMMO integration enabled!");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAbilityActive(Player player, String abilityName) {
        if (!enabled || player == null || abilityName == null) {
            return false;
        }

        try {
            return AbilityAPI.isAnyAbilityEnabled(player);
        } catch (Exception e) {
            logger.warning("Error checking mcMMO ability: " + e.getMessage());
            return false;
        }
    }

    public boolean isAnyAbilityActive(Player player) {
        if (!enabled || player == null) {
            return false;
        }

        try {
            return AbilityAPI.isAnyAbilityEnabled(player);
        } catch (Exception e) {
            logger.warning("Error checking mcMMO abilities: " + e.getMessage());
            return false;
        }
    }

    public double getMoneyMultiplier(Player player, String abilityName, Map<String, McMMOAbilityConfig> mcmmoConfig) {
        if (!enabled || mcmmoConfig == null || mcmmoConfig.isEmpty()) {
            return 1.0;
        }

        if (!isAbilityActive(player, abilityName)) {
            return 1.0;
        }

        McMMOAbilityConfig config = mcmmoConfig.get(abilityName.toLowerCase());
        if (config != null) {
            return config.getMoneyAmplifier();
        }

        return 1.0;
    }

    public double getXpMultiplier(Player player, String abilityName, Map<String, McMMOAbilityConfig> mcmmoConfig) {
        if (!enabled || mcmmoConfig == null || mcmmoConfig.isEmpty()) {
            return 1.0;
        }

        if (!isAbilityActive(player, abilityName)) {
            return 1.0;
        }

        McMMOAbilityConfig config = mcmmoConfig.get(abilityName.toLowerCase());
        if (config != null) {
            return config.getXpAmplifier();
        }

        return 1.0;
    }

    public double getActiveAbilityMultiplier(Player player, Map<String, McMMOAbilityConfig> mcmmoConfig, boolean forMoney) {
        if (!enabled || mcmmoConfig == null || mcmmoConfig.isEmpty()) {
            return 1.0;
        }

        if (!isAnyAbilityActive(player)) {
            return 1.0;
        }

        double multiplier = 0.0;

        for (Map.Entry<String, McMMOAbilityConfig> entry : mcmmoConfig.entrySet()) {
            McMMOAbilityConfig config = entry.getValue();
            double currentMultiplier = forMoney ? config.getMoneyAmplifier() : config.getXpAmplifier();
            if (currentMultiplier > multiplier) {
                multiplier = currentMultiplier;
            }
        }

        return multiplier;
    }

    public static class McMMOAbilityConfig {
        private final double moneyAmplifier;
        private final double xpAmplifier;

        public McMMOAbilityConfig(double moneyAmplifier, double xpAmplifier) {
            this.moneyAmplifier = moneyAmplifier;
            this.xpAmplifier = xpAmplifier;
        }

        public double getMoneyAmplifier() {
            return moneyAmplifier;
        }

        public double getXpAmplifier() {
            return xpAmplifier;
        }
    }
}