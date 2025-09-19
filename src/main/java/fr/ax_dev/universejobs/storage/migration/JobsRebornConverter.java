package fr.ax_dev.universejobs.storage.migration;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class JobsRebornConverter {

    private final UniverseJobs plugin;
    private final File jobsRebornFolder;
    private final File universeJobsFolder;

    public JobsRebornConverter(UniverseJobs plugin) {
        this.plugin = plugin;

        // Try multiple possible locations for Jobs plugin folder
        File jobsPluginFolder = null;

        // First, check if Jobs plugin is loaded
        if (plugin.getServer().getPluginManager().getPlugin("Jobs") != null) {
            jobsPluginFolder = plugin.getServer().getPluginManager().getPlugin("Jobs").getDataFolder();
            plugin.getLogger().info("Found active Jobs plugin at: " + jobsPluginFolder.getAbsolutePath());
        } else {
            // Check common locations
            String[] possiblePaths = {
                "Jobs",
                "JobsReborn",
                "jobs",
                "jobsreborn"
            };

            for (String path : possiblePaths) {
                File candidate = new File(plugin.getDataFolder().getParent(), path);
                if (candidate.exists() && candidate.isDirectory()) {
                    jobsPluginFolder = candidate;
                    plugin.getLogger().info("Found JobsReborn folder at: " + jobsPluginFolder.getAbsolutePath());
                    break;
                }
            }

            if (jobsPluginFolder == null) {
                jobsPluginFolder = new File(plugin.getDataFolder().getParent(), "Jobs");
                plugin.getLogger().warning("No existing JobsReborn folder found, will try: " + jobsPluginFolder.getAbsolutePath());
            }
        }

        this.jobsRebornFolder = jobsPluginFolder;
        this.universeJobsFolder = new File(plugin.getDataFolder(), "jobs");
    }

    public ConversionResult convertAllJobs() {
        return convertJobs(null);
    }

    public ConversionResult convertJobs(String specificJobName) {
        ConversionResult result = new ConversionResult();

        if (!jobsRebornFolder.exists()) {
            result.error = "JobsReborn folder not found: " + jobsRebornFolder.getAbsolutePath();
            return result;
        }

        File[] jobFiles = findJobConfigFiles();
        if (jobFiles == null || jobFiles.length == 0) {
            result.error = "No JobsReborn job files found";
            return result;
        }

        try {
            // Check if we have individual job files or a single config file
            if (jobFiles.length == 1 && jobFiles[0].getName().contains("Config")) {
                // Single config file with Jobs section
                FileConfiguration jobsConfig = YamlConfiguration.loadConfiguration(jobFiles[0]);
                convertJobsFromConfig(jobsConfig, result, specificJobName);
            } else {
                // Individual job files
                convertIndividualJobFiles(jobFiles, result, specificJobName);
            }
            result.success = true;
        } catch (Exception e) {
            result.error = "Failed to convert jobs: " + e.getMessage();
            plugin.getLogger().log(Level.SEVERE, "Job conversion failed", e);
        }

        return result;
    }

    private File[] findJobConfigFiles() {
        if (!jobsRebornFolder.exists()) {
            return null;
        }

        // Check for individual job files in jobs/ subfolder
        File jobsSubfolder = new File(jobsRebornFolder, "jobs");
        if (jobsSubfolder.exists() && jobsSubfolder.isDirectory()) {
            plugin.getLogger().info("Found jobs subfolder: " + jobsSubfolder.getAbsolutePath());

            File[] jobFiles = jobsSubfolder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
            if (jobFiles != null && jobFiles.length > 0) {
                plugin.getLogger().info("Found " + jobFiles.length + " job files in jobs subfolder:");
                for (File jobFile : jobFiles) {
                    plugin.getLogger().info("- " + jobFile.getName());
                }
                return jobFiles;
            }
        }

        // Fallback: look for single config file with Jobs section
        String[] possibleNames = {
            "jobs.yml",
            "jobConfig.yml",
            "generalConfig.yml",
            "jobsConfig.yml",
            "Jobs.yml",
            "JobsConfig.yml"
        };

        for (String name : possibleNames) {
            File file = new File(jobsRebornFolder, name);
            if (file.exists()) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    if (config.contains("Jobs")) {
                        return new File[]{file};
                    }
                } catch (Exception e) {
                    // Ignore read errors
                }
            }
        }

        return null;
    }

    private void convertIndividualJobFiles(File[] jobFiles, ConversionResult result, String specificJobName) {
        if (!universeJobsFolder.exists()) {
            universeJobsFolder.mkdirs();
        }

        for (File jobFile : jobFiles) {
            try {
                String jobName = jobFile.getName().replace(".yml", "").replace(".yaml", "");

                // Skip if we're looking for a specific job and this isn't it
                if (specificJobName != null && !jobName.equalsIgnoreCase(specificJobName)) {
                    continue;
                }

                plugin.getLogger().info("Converting job file: " + jobFile.getName());

                FileConfiguration jobConfig = YamlConfiguration.loadConfiguration(jobFile);
                convertSingleJobFromFile(jobName, jobConfig);
                result.convertedJobs.add(jobName.toLowerCase());
                result.jobsConverted++;

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to convert job file: " + jobFile.getName(), e);
                result.failedJobs.add(jobFile.getName() + ": " + e.getMessage());
            }
        }

        if (specificJobName != null && result.jobsConverted == 0) {
            result.error = "Job '" + specificJobName + "' not found in JobsReborn files";
        }
    }

    private void convertSingleJobFromFile(String jobName, FileConfiguration jobConfig) throws Exception {
        String jobId = jobName.toLowerCase();
        File outputFile = new File(universeJobsFolder, jobId + ".yml");

        FileConfiguration output = new YamlConfiguration();

        // Check if data is under a job section (like "Builder")
        ConfigurationSection jobSection = null;
        String capitalizedJobName = jobName.substring(0, 1).toUpperCase() + jobName.substring(1).toLowerCase();

        if (jobConfig.contains(capitalizedJobName)) {
            jobSection = jobConfig.getConfigurationSection(capitalizedJobName);
            plugin.getLogger().info("Found job data under section: " + capitalizedJobName);
        } else if (jobConfig.contains(jobName.toLowerCase())) {
            jobSection = jobConfig.getConfigurationSection(jobName.toLowerCase());
            plugin.getLogger().info("Found job data under section: " + jobName.toLowerCase());
        } else if (jobConfig.contains(jobName.toUpperCase())) {
            jobSection = jobConfig.getConfigurationSection(jobName.toUpperCase());
            plugin.getLogger().info("Found job data under section: " + jobName.toUpperCase());
        } else {
            // Data might be at root level
            plugin.getLogger().info("Using root level data for job: " + jobName);
        }

        // Use job section if found, otherwise use root config
        ConfigurationSection dataSource = jobSection != null ? jobSection : jobConfig;

        // Convert basic job properties - use exactly what's in JobsReborn
        output.set("name", convertJobDisplayName(jobName, dataSource));
        output.set("description", convertJobDescription(dataSource));
        output.set("enabled", dataSource.getBoolean("enabled", true));
        output.set("max-level", dataSource.getInt("max-level", 100));
        output.set("permission", "UniverseJobs.job." + jobId);

        convertJobIcon(dataSource, output, jobName);
        convertRewards(jobId, output);
        convertXpConfig(dataSource, output);
        convertActionsFromFile(dataSource, output);
        convertLevelupActionsFromFile(dataSource, output);

        output.save(outputFile);
    }

    private void convertJobsFromConfig(FileConfiguration jobsConfig, ConversionResult result, String specificJobName) {
        if (!universeJobsFolder.exists()) {
            universeJobsFolder.mkdirs();
        }

        ConfigurationSection jobsSection = jobsConfig.getConfigurationSection("Jobs");
        if (jobsSection == null) {
            result.error = "No 'Jobs' section found in configuration";
            return;
        }

        for (String jobName : jobsSection.getKeys(false)) {
            try {
                // Skip if we're looking for a specific job and this isn't it
                if (specificJobName != null && !jobName.equalsIgnoreCase(specificJobName)) {
                    continue;
                }

                ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobName);
                if (jobSection != null) {
                    convertSingleJob(jobName, jobSection);
                    result.convertedJobs.add(jobName.toLowerCase());
                    result.jobsConverted++;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to convert job: " + jobName, e);
                result.failedJobs.add(jobName + ": " + e.getMessage());
            }
        }

        if (specificJobName != null && result.jobsConverted == 0) {
            result.error = "Job '" + specificJobName + "' not found in JobsReborn configuration";
        }
    }

    private void convertSingleJob(String jobName, ConfigurationSection jobSection) throws IOException {
        String jobId = jobName.toLowerCase();
        File outputFile = new File(universeJobsFolder, jobId + ".yml");

        FileConfiguration output = new YamlConfiguration();

        output.set("name", convertJobDisplayName(jobName, jobSection));
        output.set("description", convertJobDescription(jobSection));
        output.set("enabled", jobSection.getBoolean("enabled", true));
        output.set("max-level", jobSection.getInt("max-level", 100));
        output.set("permission", "UniverseJobs.job." + jobId);

        convertJobIcon(jobSection, output);
        convertRewards(jobId, output);
        convertXpConfig(jobSection, output);
        convertActions(jobSection, output);
        convertLevelupActions(jobSection, output);

        output.save(outputFile);
    }

    private String convertJobDisplayName(String jobName, ConfigurationSection dataSource) {
        plugin.getLogger().info("Converting display name for job: " + jobName);
        plugin.getLogger().info("Available keys in data source: " + dataSource.getKeys(false));

        // Get display name exactly as it is in JobsReborn
        String displayName = dataSource.getString("displayName", null);
        if (displayName == null) {
            displayName = dataSource.getString("displayname", null);
        }
        if (displayName == null) {
            displayName = dataSource.getString("fullname", null);
        }
        if (displayName == null) {
            displayName = dataSource.getString("name", jobName);
        }

        // Get ChatColour exactly as it is in JobsReborn
        String chatColor = dataSource.getString("ChatColour", null);
        if (chatColor == null) {
            chatColor = dataSource.getString("chatcolour", null);
        }
        if (chatColor == null) {
            chatColor = dataSource.getString("color", null);
        }

        plugin.getLogger().info("Converting job display name - jobName: " + jobName + ", displayName: " + displayName + ", chatColor: " + chatColor);

        // If we have a ChatColour, convert it and prepend to display name
        if (chatColor != null) {
            String hexColor = convertChatColorToHex(chatColor);
            String result = hexColor + displayName;
            plugin.getLogger().info("Final display name with color: " + result);
            return result;
        } else {
            // No color found, just return the display name as-is
            plugin.getLogger().info("Final display name without color: " + displayName);
            return displayName;
        }
    }

    private List<String> convertJobDescription(ConfigurationSection dataSource) {
        plugin.getLogger().info("Converting description - available keys: " + dataSource.getKeys(false));
        plugin.getLogger().info("Checking FullDescription...");

        // Try FullDescription first (JobsReborn format)
        if (dataSource.contains("FullDescription")) {
            List<String> fullDesc = dataSource.getStringList("FullDescription");
            plugin.getLogger().info("Found FullDescription: " + fullDesc);
            if (fullDesc != null && !fullDesc.isEmpty()) {
                // Return FullDescription exactly as it is (keep colors if any)
                return fullDesc;
            }
        }

        // Try other description fields
        String description = dataSource.getString("description", null);
        if (description == null) {
            description = dataSource.getString("desc", null);
        }
        if (description == null) {
            description = dataSource.getString("info", null);
        }

        plugin.getLogger().info("No FullDescription found, using description: " + description);

        if (description == null || description.isEmpty()) {
            // Return empty list if no description found - let UniverseJobs handle defaults
            return new ArrayList<>();
        }

        // Return description exactly as it is (keep colors if any)
        String[] lines = description.split("\\n");
        return Arrays.asList(lines);
    }

    private void convertJobIcon(ConfigurationSection jobSection, FileConfiguration output) {
        convertJobIcon(jobSection, output);
    }

    private void convertJobIcon(ConfigurationSection dataSource, FileConfiguration output, String jobName) {
        plugin.getLogger().info("Converting icon for job: " + jobName);

        // Get icon exactly as it is in JobsReborn
        String iconMaterial = null;

        if (dataSource.contains("Gui.ItemStack")) {
            String itemStack = dataSource.getString("Gui.ItemStack");
            plugin.getLogger().info("Found Gui.ItemStack: " + itemStack);
            if (itemStack != null && !itemStack.isEmpty()) {
                // Parse ItemStack format: "brick_stairs;DURABILITY:1;hideenchants"
                String[] parts = itemStack.split(";");
                if (parts.length > 0) {
                    String originalMaterial = parts[0];
                    iconMaterial = convertMaterialName(originalMaterial.toUpperCase());
                    plugin.getLogger().info("Parsed material: " + originalMaterial + " -> " + iconMaterial);
                }
            }
        } else {
            plugin.getLogger().info("No Gui.ItemStack found, checking other icon fields...");
            // Try other possible icon fields
            if (dataSource.contains("icon")) {
                iconMaterial = convertMaterialName(dataSource.getString("icon").toUpperCase());
                plugin.getLogger().info("Found icon field: " + iconMaterial);
            }
        }

        // Only set icon if we found one in JobsReborn config
        if (iconMaterial != null && !iconMaterial.isEmpty()) {
            plugin.getLogger().info("Final icon material: " + iconMaterial);
            output.set("icon.material", iconMaterial);
            output.set("icon.custom-model-data", "");
        } else {
            plugin.getLogger().warning("No icon found in JobsReborn config for job: " + jobName);
            // Don't set any icon - let UniverseJobs use its defaults
        }
    }

    private String convertMaterialName(String oldMaterial) {
        // Only convert JobsReborn legacy material names that are actually different
        switch (oldMaterial.toLowerCase()) {
            case "oaklog": return "OAK_LOG";
            case "wood": return "OAK_PLANKS";
            case "log": return "OAK_LOG";
            case "raw_fish": return "COD";
            case "cooked_fish": return "COOKED_COD";
            // Wood items (1.13+ conversion)
            case "wood_sword": return "WOODEN_SWORD";
            case "wood_pickaxe": return "WOODEN_PICKAXE";
            case "wood_axe": return "WOODEN_AXE";
            case "wood_shovel": return "WOODEN_SHOVEL";
            case "wood_hoe": return "WOODEN_HOE";
            // JobsReborn specific material conversions
            case "brick_stairs": return "BRICK_STAIRS";
            case "brickstairs": return "BRICK_STAIRS";
            // Most modern materials just need UPPER_CASE
            default: return oldMaterial.toUpperCase();
        }
    }

    // Removed getDefaultIconForJob - no hardcoding of default icons

    private void convertRewards(String jobId, FileConfiguration output) {
        // Don't hardcode rewards - let UniverseJobs handle defaults
        // Only convert if JobsReborn had specific reward configuration
    }

    private void convertXpConfig(ConfigurationSection jobSection, FileConfiguration output) {
        // Use default XP curve - let UniverseJobs handle XP configuration
        output.set("xp.type", "CURVE");
        output.set("xp.xp", "default");
    }

    private void convertActions(ConfigurationSection jobSection, FileConfiguration output) {
        Map<String, Object> actions = new HashMap<>();

        ConfigurationSection breakSection = jobSection.getConfigurationSection("Break");
        if (breakSection != null) {
            actions.put("BREAK", convertActionSection(breakSection, "BREAK"));
        }

        ConfigurationSection placeSection = jobSection.getConfigurationSection("Place");
        if (placeSection != null) {
            actions.put("PLACE", convertActionSection(placeSection, "PLACE"));
        }

        ConfigurationSection killSection = jobSection.getConfigurationSection("Kill");
        if (killSection != null) {
            actions.put("KILL", convertActionSection(killSection, "KILL"));
        }

        ConfigurationSection fishSection = jobSection.getConfigurationSection("Fish");
        if (fishSection != null) {
            actions.put("FISH", convertActionSection(fishSection, "FISH"));
        }

        ConfigurationSection craftSection = jobSection.getConfigurationSection("Craft");
        if (craftSection != null) {
            actions.put("CRAFT", convertActionSection(craftSection, "CRAFT"));
        }

        ConfigurationSection smeltSection = jobSection.getConfigurationSection("Smelt");
        if (smeltSection != null) {
            actions.put("SMELT", convertActionSection(smeltSection, "SMELT"));
        }

        ConfigurationSection brewSection = jobSection.getConfigurationSection("Brew");
        if (brewSection != null) {
            actions.put("BREW", convertActionSection(brewSection, "BREW"));
        }

        ConfigurationSection enchantSection = jobSection.getConfigurationSection("Enchant");
        if (enchantSection != null) {
            actions.put("ENCHANT", convertActionSection(enchantSection, "ENCHANT"));
        }

        ConfigurationSection repairSection = jobSection.getConfigurationSection("Repair");
        if (repairSection != null) {
            actions.put("REPAIR", convertActionSection(repairSection, "REPAIR"));
        }

        ConfigurationSection tameSection = jobSection.getConfigurationSection("Tame");
        if (tameSection != null) {
            actions.put("TAME", convertActionSection(tameSection, "TAME"));
        }

        ConfigurationSection breedSection = jobSection.getConfigurationSection("Breed");
        if (breedSection != null) {
            actions.put("BREED", convertActionSection(breedSection, "BREED"));
        }

        ConfigurationSection milkSection = jobSection.getConfigurationSection("Milk");
        if (milkSection != null) {
            actions.put("MILK", convertActionSection(milkSection, "MILK"));
        }

        ConfigurationSection shearSection = jobSection.getConfigurationSection("Shear");
        if (shearSection != null) {
            actions.put("SHEAR", convertActionSection(shearSection, "SHEAR"));
        }

        ConfigurationSection dyeSection = jobSection.getConfigurationSection("Dye");
        if (dyeSection != null) {
            actions.put("DYE", convertActionSection(dyeSection, "DYE"));
        }

        output.set("actions", actions);
    }

    private void convertActionsFromFile(ConfigurationSection dataSource, FileConfiguration output) {
        plugin.getLogger().info("Converting actions from file - available keys: " + dataSource.getKeys(false));
        Map<String, Object> allActions = new HashMap<>();

        // List of all possible action types in JobsReborn
        String[] actionTypes = {
            "Break", "Place", "Kill", "Fish", "Craft", "Smelt", "Enchant", "Brew",
            "Repair", "Tame", "Breed", "Milk", "Shear", "Dye", "Explore",
            "Collect", "Bucket", "Bake", "StripLogs", "Vax", "TNTBreak",
            "VTrade", "MMKill", "custom-kill", "PyroFishingPro", "CustomFishing", "Brush"
        };

        for (String actionType : actionTypes) {
            if (dataSource.contains(actionType)) {
                plugin.getLogger().info("Found action type: " + actionType);
                ConfigurationSection actionSection = dataSource.getConfigurationSection(actionType);
                if (actionSection != null) {
                    plugin.getLogger().info("Action section keys for " + actionType + ": " + actionSection.getKeys(false));
                    Map<String, Object> convertedActions = convertActionSection(actionSection, actionType);
                    plugin.getLogger().info("Converted " + convertedActions.size() + " actions for " + actionType);
                    if (!convertedActions.isEmpty()) {
                        // Convert COLLECT to BREAK (COLLECT doesn't exist in UniverseJobs)
                        String targetActionType = actionType.equals("Collect") ? "BREAK" : actionType.toUpperCase();
                        allActions.put(targetActionType, convertedActions);
                    }
                } else {
                    plugin.getLogger().info("Action section is null for: " + actionType);
                }
            }
        }

        plugin.getLogger().info("Total converted action types: " + allActions.size());
        output.set("actions", allActions);
    }

    private Map<String, Object> convertActionSection(ConfigurationSection section, String actionType) {
        Map<String, Object> actions = new HashMap<>();

        for (String materialKey : section.getKeys(false)) {
            if (section.isConfigurationSection(materialKey)) {
                ConfigurationSection materialSection = section.getConfigurationSection(materialKey);

                // Parse material name and age from JobsReborn format (ex: "sweet_berries-2")
                String[] materialParts = materialKey.split("-");
                String baseMaterial = materialParts[0];
                Integer age = null;
                if (materialParts.length > 1) {
                    try {
                        age = Integer.parseInt(materialParts[1]);
                    } catch (NumberFormatException e) {
                        // Ignore if not a number
                    }
                }

                String actionId = actionType.toLowerCase() + "_" + baseMaterial.toLowerCase();
                if (age != null) {
                    actionId += "_age_" + age;
                }

                Map<String, Object> actionData = new HashMap<>();
                actionData.put("target", convertMaterialName(baseMaterial).toUpperCase());
                actionData.put("display-name", formatDisplayName(baseMaterial));
                actionData.put("lore", new ArrayList<>());

                // Add age if present
                if (age != null) {
                    actionData.put("age", age);
                }

                // Get experience and income values
                double experience = materialSection.getDouble("experience", 1.0);
                double income = materialSection.getDouble("income", experience);
                double points = materialSection.getDouble("points", experience);

                actionData.put("xp", experience);
                actionData.put("money", income);

                actions.put(actionId, actionData);
            } else {
                // Handle materials list format: "material;income;exp" or just "material;income"
                if (materialKey.equals("materials")) {
                    List<String> materialsList = section.getStringList(materialKey);
                    for (String materialEntry : materialsList) {
                        String[] parts = materialEntry.split(";");
                        if (parts.length >= 2) {
                            String material = parts[0];
                            String actionId = actionType.toLowerCase() + "_" + material.toLowerCase();

                            Map<String, Object> actionData = new HashMap<>();
                            actionData.put("target", convertMaterialName(material).toUpperCase());
                            actionData.put("display-name", formatDisplayName(material));
                            actionData.put("lore", new ArrayList<>()); // Empty lore

                            try {
                                double income = Double.parseDouble(parts[1]);
                                double experience = parts.length > 2 ? Double.parseDouble(parts[2]) : income;

                                actionData.put("xp", experience);
                                actionData.put("money", income);
                            } catch (NumberFormatException e) {
                                actionData.put("xp", 1.0);
                                actionData.put("money", 1.0);
                            }

                            actions.put(actionId, actionData);
                        }
                    }
                } else {
                    // Simple format with direct value
                    // Parse material name and age from JobsReborn format (ex: "sweet_berries-2")
                    String[] materialParts = materialKey.split("-");
                    String baseMaterial = materialParts[0];
                    Integer age = null;
                    if (materialParts.length > 1) {
                        try {
                            age = Integer.parseInt(materialParts[1]);
                        } catch (NumberFormatException e) {
                            // Ignore if not a number
                        }
                    }

                    String actionId = actionType.toLowerCase() + "_" + baseMaterial.toLowerCase();
                    if (age != null) {
                        actionId += "_age_" + age;
                    }

                    Map<String, Object> actionData = new HashMap<>();
                    actionData.put("target", convertMaterialName(baseMaterial).toUpperCase());
                    actionData.put("display-name", formatDisplayName(baseMaterial));
                    actionData.put("lore", new ArrayList<>()); // Empty lore

                    // Add age if present
                    if (age != null) {
                        actionData.put("age", age);
                    }

                    double value = section.getDouble(materialKey, 1.0);
                    actionData.put("xp", value);
                    actionData.put("money", value);

                    actions.put(actionId, actionData);
                }
            }
        }

        return actions;
    }

    private String formatDisplayName(String material) {
        String[] parts = material.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            result.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }
        return result.toString();
    }

    private void convertLevelupActions(ConfigurationSection jobSection, FileConfiguration output) {
        Map<String, Object> levelupActions = new HashMap<>();

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "broadcast");
        broadcast.put("min-level", 1);
        broadcast.put("level-interval", 10);

        String jobColor = convertChatColorToHex(jobSection.getString("ChatColour", "&a"));
        String jobName = jobSection.getString("displayname", jobSection.getName());

        broadcast.put("messages", Arrays.asList(
            "<#90db8a>{player} <#9a9c9a>has reached level " + jobColor + "{level} <#9a9c9a>in " + jobColor + jobName + "<#9a9c9a>!"
        ));
        levelupActions.put("broadcast", broadcast);

        Map<String, Object> title = new HashMap<>();
        title.put("type", "title");
        title.put("min-level", 1);
        title.put("level-interval", 1);
        title.put("title", jobColor + "Reached level {level}");
        title.put("subtitle", "<#9a9c9a>in " + jobColor + jobName + "<#9a9c9a>!");
        title.put("fade-in", 5);
        title.put("stay", 20);
        title.put("fade-out", 10);
        levelupActions.put("title", title);

        Map<String, Object> sound = new HashMap<>();
        sound.put("type", "sound");
        sound.put("min-level", 1);
        sound.put("level-interval", 1);
        sound.put("sound", "ENTITY_PLAYER_LEVELUP");
        levelupActions.put("sound", sound);

        Map<String, Object> particle = new HashMap<>();
        particle.put("type", "particle");
        particle.put("min-level", 1);
        particle.put("level-interval", 1);
        particle.put("count", "20");
        particle.put("particle", "TOTEM");
        levelupActions.put("particle", particle);

        output.set("levelup-actions", levelupActions);
    }

    private void convertLevelupActionsFromFile(ConfigurationSection dataSource, FileConfiguration output) {
        // Don't hardcode levelup actions - let UniverseJobs use defaults
        // JobsReborn levelup system is different from UniverseJobs
        // Only convert if there were specific levelup actions in JobsReborn config
    }

    private String convertChatColorToHex(String chatColor) {
        if (chatColor == null) return "<#32CD32>";

        // Handle JobsReborn color names
        switch (chatColor.toLowerCase()) {
            case "black": return "<#000000>";
            case "dark_blue": return "<#0000AA>";
            case "dark_green": return "<#00AA00>";
            case "dark_aqua": return "<#00AAAA>";
            case "dark_red": return "<#AA0000>";
            case "dark_purple": return "<#AA00AA>";
            case "gold": return "<#FFAA00>";
            case "gray": return "<#AAAAAA>";
            case "dark_gray": return "<#555555>";
            case "blue": return "<#5555FF>";
            case "green": return "<#55FF55>";
            case "aqua": return "<#55FFFF>";
            case "red": return "<#FF5555>";
            case "light_purple": return "<#FF55FF>";
            case "yellow": return "<#FFFF55>";
            case "white": return "<#FFFFFF>";
            // Handle legacy color codes
            case "&0": return "<#000000>";
            case "&1": return "<#0000AA>";
            case "&2": return "<#00AA00>";
            case "&3": return "<#00AAAA>";
            case "&4": return "<#AA0000>";
            case "&5": return "<#AA00AA>";
            case "&6": return "<#FFAA00>";
            case "&7": return "<#AAAAAA>";
            case "&8": return "<#555555>";
            case "&9": return "<#5555FF>";
            case "&a": return "<#55FF55>";
            case "&b": return "<#55FFFF>";
            case "&c": return "<#FF5555>";
            case "&d": return "<#FF55FF>";
            case "&e": return "<#FFFF55>";
            case "&f": return "<#FFFFFF>";
            default:
                if (chatColor.startsWith("#")) {
                    return "<" + chatColor + ">";
                }
                return "<#32CD32>";
        }
    }

    public List<String> getAvailableJobNames() {
        List<String> jobNames = new ArrayList<>();

        if (!jobsRebornFolder.exists()) {
            return jobNames;
        }

        File[] jobFiles = findJobConfigFiles();
        if (jobFiles == null || jobFiles.length == 0) {
            return jobNames;
        }

        // Check if we have individual job files or a single config file
        if (jobFiles.length == 1 && jobFiles[0].getName().contains("Config")) {
            // Single config file with Jobs section
            try {
                FileConfiguration jobsConfig = YamlConfiguration.loadConfiguration(jobFiles[0]);
                ConfigurationSection jobsSection = jobsConfig.getConfigurationSection("Jobs");
                if (jobsSection != null) {
                    jobNames.addAll(jobsSection.getKeys(false));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error reading JobsReborn config: " + e.getMessage());
            }
        } else {
            // Individual job files
            for (File jobFile : jobFiles) {
                String jobName = jobFile.getName().replace(".yml", "").replace(".yaml", "");
                // Skip example and none files
                if (!jobName.toLowerCase().startsWith("_example") && !jobName.toLowerCase().equals("none")) {
                    jobNames.add(jobName);
                }
            }
        }

        return jobNames;
    }

    public static class ConversionResult {
        public boolean success = false;
        public int jobsConverted = 0;
        public List<String> convertedJobs = new ArrayList<>();
        public List<String> failedJobs = new ArrayList<>();
        public String error = null;

        public boolean isSuccessful() {
            return success;
        }

        public int getTotalConverted() {
            return jobsConverted;
        }
    }
}