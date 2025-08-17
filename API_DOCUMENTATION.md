# JobsAdventure API Documentation

This document provides comprehensive information for developers who want to integrate with or extend JobsAdventure.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Core API Access](#core-api-access)
3. [Events](#events)
4. [Managers](#managers)
5. [Data Models](#data-models)
6. [Custom Extensions](#custom-extensions)
7. [Plugin Integrations](#plugin-integrations)
8. [Best Practices](#best-practices)

---

## Getting Started

### Maven Dependency

Add JobsAdventure as a dependency in your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>fr.ax_dev</groupId>
        <artifactId>jobsadventure</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Plugin.yml Dependency

```yaml
depend: [JobsAdventure]
# or
softdepend: [JobsAdventure]
```

### Basic Setup

```java
public class MyPlugin extends JavaPlugin {
    
    private JobsAdventure jobsAdventure;
    
    @Override
    public void onEnable() {
        Plugin plugin = getServer().getPluginManager().getPlugin("JobsAdventure");
        if (plugin instanceof JobsAdventure) {
            this.jobsAdventure = (JobsAdventure) plugin;
            getLogger().info("JobsAdventure integration enabled!");
        }
    }
}
```

---

## Core API Access

### Getting Plugin Instance

```java
// Static access (recommended)
JobsAdventure plugin = JobsAdventure.getInstance();

// Via PluginManager
Plugin plugin = Bukkit.getPluginManager().getPlugin("JobsAdventure");
if (plugin instanceof JobsAdventure jobsPlugin) {
    // Use jobsPlugin
}
```

### Manager Access

```java
JobsAdventure plugin = JobsAdventure.getInstance();

// Core managers
JobManager jobManager = plugin.getJobManager();
RewardManager rewardManager = plugin.getRewardManager();
ActionProcessor actionProcessor = plugin.getActionProcessor();
XpBonusManager bonusManager = plugin.getBonusManager();
PlaceholderManager placeholderManager = plugin.getPlaceholderManager();
ConfigManager configManager = plugin.getConfigManager();
```

---

## Events

JobsAdventure fires several custom events that you can listen to:

### Available Events

#### PlayerJobJoinEvent
```java
@EventHandler
public void onPlayerJobJoin(PlayerJobJoinEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Event is cancellable
    if (someCondition) {
        event.setCancelled(true);
        event.setCancelReason("Custom cancellation reason");
    }
    
    getLogger().info(player.getName() + " joined job: " + job.getName());
}
```

#### PlayerJobLeaveEvent
```java
@EventHandler
public void onPlayerJobLeave(PlayerJobLeaveEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Event is cancellable
    event.setCancelled(true);
    
    getLogger().info(player.getName() + " left job: " + job.getName());
}
```

#### PlayerXpGainEvent
```java
@EventHandler
public void onPlayerXpGain(PlayerXpGainEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    double originalXp = event.getOriginalXp();
    double finalXp = event.getFinalXp();
    ActionType actionType = event.getActionType();
    String target = event.getTarget();
    
    // Modify XP amount
    event.setFinalXp(finalXp * 1.5); // 50% bonus
    
    // Cancel XP gain
    event.setCancelled(true);
}
```

#### PlayerLevelUpEvent
```java
@EventHandler
public void onPlayerLevelUp(PlayerLevelUpEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    int oldLevel = event.getOldLevel();
    int newLevel = event.getNewLevel();
    
    // This event is not cancellable
    
    // Custom level up effects
    player.sendTitle("§6Level Up!", 
        "§7" + job.getName() + " Level " + newLevel, 10, 70, 20);
    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
}
```

#### PlayerRewardClaimEvent
```java
@EventHandler
public void onPlayerRewardClaim(PlayerRewardClaimEvent event) {
    Player player = event.getPlayer();
    Reward reward = event.getReward();
    Job job = event.getJob();
    
    // Event is cancellable
    if (!player.hasPermission("custom.claim." + reward.getId())) {
        event.setCancelled(true);
        event.setCancelReason("§cYou don't have permission to claim this reward!");
    }
}
```

#### ActionProcessEvent
```java
@EventHandler
public void onActionProcess(ActionProcessEvent event) {
    Player player = event.getPlayer();
    ActionType actionType = event.getActionType();
    String target = event.getTarget();
    Location location = event.getLocation();
    
    // Pre-processing event - fired before XP calculation
    
    // Add custom data for condition evaluation
    event.addContextData("custom_multiplier", 1.5);
    
    // Cancel action processing
    event.setCancelled(true);
}
```

### Event Registration

```java
public class JobsListener implements Listener {
    
    private final MyPlugin plugin;
    
    public JobsListener(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJobJoin(PlayerJobJoinEvent event) {
        // Handle event
    }
}

// In your main plugin class
@Override
public void onEnable() {
    getServer().getPluginManager().registerEvents(new JobsListener(this), this);
}
```

---

## Managers

### JobManager

```java
JobManager jobManager = plugin.getJobManager();

// Job operations
Collection<Job> allJobs = jobManager.getAllJobs();
Collection<Job> enabledJobs = jobManager.getEnabledJobs();
Job job = jobManager.getJob("miner");
boolean hasJob = jobManager.hasJob("miner");

// Player job operations
PlayerJobData playerData = jobManager.getPlayerData(player);
boolean joinSuccess = jobManager.joinJob(player, "miner");
boolean leaveSuccess = jobManager.leaveJob(player, "miner");
Set<String> playerJobs = jobManager.getPlayerJobs(player);

// XP operations
jobManager.addXp(player, "miner", 100.0);
double currentXp = jobManager.getXp(player, "miner");
int currentLevel = jobManager.getLevel(player, "miner");
double xpToNext = jobManager.getXpToNextLevel(player, "miner");
double xpForLevel = jobManager.getXpRequiredForLevel("miner", 10);

// Data management
jobManager.savePlayerData(player);
jobManager.loadPlayerData(player);
jobManager.saveAllPlayerData();
jobManager.reloadJobs();
```

### RewardManager

```java
RewardManager rewardManager = plugin.getRewardManager();

// Reward operations
List<Reward> jobRewards = rewardManager.getJobRewards("miner");
List<Reward> availableRewards = rewardManager.getAvailableRewards(player, "miner");
RewardStatus status = rewardManager.getRewardStatus(player, reward);

// Claim operations
boolean canClaim = rewardManager.canClaimReward(player, reward);
boolean claimSuccess = rewardManager.claimReward(player, reward);

// GUI operations
rewardManager.openRewardGui(player, "miner");
rewardManager.openRewardGui(player, "miner", 0); // Specific page

// Data management
rewardManager.loadPlayerData(player);
rewardManager.unloadPlayerData(player);
rewardManager.reload();
```

### ActionProcessor

```java
ActionProcessor actionProcessor = plugin.getActionProcessor();

// Manual action processing
actionProcessor.processAction(player, ActionType.BREAK, "STONE", location);
actionProcessor.processAction(player, ActionType.KILL, "COW", location, entity);

// Check if action is valid for player
boolean canProcess = actionProcessor.canProcessAction(player, ActionType.BREAK, "DIAMOND_ORE");

// Get action XP (without awarding)
double xpAmount = actionProcessor.calculateXp(player, job, jobAction);
```

### XpBonusManager

```java
XpBonusManager bonusManager = plugin.getBonusManager();

// Apply bonuses
bonusManager.applyBonus(player, "miner", 2.0, Duration.ofHours(1), "VIP Bonus");
bonusManager.applyGlobalBonus(player, 1.5, Duration.ofMinutes(30), "Event Bonus");

// Remove bonuses
bonusManager.removeBonus(player, "miner");
bonusManager.removeAllBonuses(player);

// Query bonuses
List<XpBonus> playerBonuses = bonusManager.getPlayerBonuses(player);
List<XpBonus> jobBonuses = bonusManager.getJobBonuses(player, "miner");
double totalMultiplier = bonusManager.getTotalMultiplier(player, "miner");

// Bonus information
int activeBonuses = bonusManager.getActiveBonusCount();
int totalPlayers = bonusManager.getPlayersWithBonuses();

// Cleanup
bonusManager.cleanupExpiredBonuses();
```

### PlaceholderManager

```java
PlaceholderManager placeholderManager = plugin.getPlaceholderManager();

// Cache management
placeholderManager.clearCache();
placeholderManager.clearJobCache("miner");

// Status
boolean isEnabled = placeholderManager.isPlaceholderApiEnabled();

// Get placeholder instances
JobsLeaderboardPlaceholder jobsPlaceholder = placeholderManager.getJobsLeaderboardPlaceholder();
GlobalLeaderboardPlaceholder globalPlaceholder = placeholderManager.getGlobalLeaderboardPlaceholder();
```

---

## Data Models

### Job

```java
Job job = jobManager.getJob("miner");

// Basic properties
String id = job.getId();
String name = job.getName();
String description = job.getDescription();
List<String> lore = job.getLore();
String permission = job.getPermission();
int maxLevel = job.getMaxLevel();
String icon = job.getIcon();
boolean enabled = job.isEnabled();

// XP configuration
String xpCurveName = job.getXpCurveName();
String xpEquation = job.getXpEquation();
XpCurve xpCurve = job.getXpCurve();
boolean hasCustomCurve = job.hasCustomXpCurve();

// Actions
Set<ActionType> actionTypes = job.getActionTypes();
List<JobAction> breakActions = job.getActions(ActionType.BREAK);
boolean hasBreakActions = job.hasActions(ActionType.BREAK);

// Rewards
String guiReward = job.getGuiReward();
String rewardsFile = job.getRewardsFile();

// XP curve errors
boolean hasError = job.hasXpCurveError();
String errorMessage = job.getXpCurveErrorMessage();
```

### PlayerJobData

```java
PlayerJobData playerData = jobManager.getPlayerData(player);

// Basic info
UUID playerUuid = playerData.getPlayerUuid();
Set<String> jobs = playerData.getJobs();

// Job membership
boolean hasJob = playerData.hasJob("miner");
boolean joinSuccess = playerData.joinJob("miner");
boolean leaveSuccess = playerData.leaveJob("miner");

// XP and levels
double xp = playerData.getXp("miner");
int level = playerData.getLevel("miner");
playerData.addXp("miner", 50.0);
playerData.setXp("miner", 1000.0);
playerData.setLevel("miner", 15);

// Progress information
double[] progress = playerData.getXpProgress("miner");
double currentXpInLevel = progress[0];
double xpNeededForNext = progress[1];
```

### Reward

```java
Reward reward = rewardManager.getReward("miner", "level_10_pickaxe");

// Basic properties
String id = reward.getId();
String name = reward.getName();
String description = reward.getDescription();
int requiredLevel = reward.getRequiredLevel();
boolean repeatable = reward.isRepeatable();
long cooldownHours = reward.getCooldownHours();

// Requirements
ConditionGroup requirements = reward.getRequirements();
boolean hasRequirements = reward.hasRequirements();

// Rewards content
List<ItemStack> items = reward.getItems();
List<String> commands = reward.getCommands();
boolean hasItems = reward.hasItems();
boolean hasCommands = reward.hasCommands();

// Economy (if available)
double economyAmount = reward.getEconomyAmount();
String economyReason = reward.getEconomyReason();
boolean hasEconomy = reward.hasEconomy();
```

### JobAction

```java
JobAction action = job.getActions(ActionType.BREAK).get(0);

// Basic properties
String target = action.getTarget();
double xp = action.getXp();
boolean enabled = action.isEnabled();

// Requirements
ConditionGroup requirements = action.getRequirements();
boolean hasRequirements = action.hasRequirements();

// Message configuration
MessageConfig messageConfig = action.getMessageConfig();
boolean hasMessage = action.hasMessage();
```

---

## Custom Extensions

### Creating Custom Conditions

```java
public class HealthCondition extends AbstractCondition {
    
    private final double minHealth;
    private final double maxHealth;
    
    public HealthCondition(ConfigurationSection config) {
        super(config);
        this.minHealth = config.getDouble("min", 0.0);
        this.maxHealth = config.getDouble("max", 20.0);
    }
    
    @Override
    public ConditionResult evaluate(ConditionContext context) {
        Player player = context.getPlayer();
        double health = player.getHealth();
        
        boolean passed = health >= minHealth && health <= maxHealth;
        
        String message = passed ? 
            "§aHealth requirement met!" :
            "§cYou need between " + minHealth + " and " + maxHealth + " health!";
            
        return new ConditionResult(passed, message);
    }
}
```

### Registering Custom Conditions

```java
public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Register after JobsAdventure is loaded
        getServer().getScheduler().runTask(this, () -> {
            ConditionType.registerCustomType("health", HealthCondition.class);
        });
    }
}
```

### Creating Custom Action Types

```java
public enum CustomActionType {
    CUSTOM_FISH,
    CUSTOM_MINE,
    CUSTOM_CRAFT;
    
    public static void register() {
        ActionType.registerCustomType("CUSTOM_FISH", CustomActionType.CUSTOM_FISH);
        ActionType.registerCustomType("CUSTOM_MINE", CustomActionType.CUSTOM_MINE);
        ActionType.registerCustomType("CUSTOM_CRAFT", CustomActionType.CUSTOM_CRAFT);
    }
}
```

### Custom Event Processing

```java
public class CustomEventListener implements Listener {
    
    private final JobsAdventure plugin;
    private final ActionProcessor actionProcessor;
    
    public CustomEventListener(JobsAdventure plugin) {
        this.plugin = plugin;
        this.actionProcessor = plugin.getActionProcessor();
    }
    
    @EventHandler
    public void onCustomEvent(CustomPluginEvent event) {
        Player player = event.getPlayer();
        String customTarget = event.getTarget();
        Location location = event.getLocation();
        
        // Process custom action
        actionProcessor.processAction(player, 
            ActionType.fromString("CUSTOM_FISH"), 
            customTarget, 
            location);
    }
}
```

### Custom Placeholder Extensions

```java
public class CustomJobsPlaceholder extends PlaceholderExpansion {
    
    private final JobsAdventure plugin;
    
    public CustomJobsPlaceholder(JobsAdventure plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "customjobs";
    }
    
    @Override
    public String getAuthor() {
        return "YourName";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        
        JobManager jobManager = plugin.getJobManager();
        
        // %customjobs_highest_level%
        if (identifier.equals("highest_level")) {
            return String.valueOf(getHighestLevel(player, jobManager));
        }
        
        // %customjobs_total_xp%
        if (identifier.equals("total_xp")) {
            return String.format("%.1f", getTotalXp(player, jobManager));
        }
        
        return null;
    }
    
    private int getHighestLevel(Player player, JobManager jobManager) {
        PlayerJobData data = jobManager.getPlayerData(player);
        return data.getJobs().stream()
            .mapToInt(jobId -> data.getLevel(jobId))
            .max()
            .orElse(0);
    }
    
    private double getTotalXp(Player player, JobManager jobManager) {
        PlayerJobData data = jobManager.getPlayerData(player);
        return data.getJobs().stream()
            .mapToDouble(jobId -> data.getXp(jobId))
            .sum();
    }
}
```

---

## Plugin Integrations

### Hooking into External Plugins

```java
public class ExternalPluginHook implements Listener {
    
    private final JobsAdventure jobsAdventure;
    private final ActionProcessor actionProcessor;
    
    public ExternalPluginHook(JobsAdventure jobsAdventure) {
        this.jobsAdventure = jobsAdventure;
        this.actionProcessor = jobsAdventure.getActionProcessor();
    }
    
    // Example: Custom fishing plugin integration
    @EventHandler
    public void onCustomFish(CustomFishCatchEvent event) {
        Player player = event.getPlayer();
        String fishType = event.getFishType();
        Location location = event.getLocation();
        
        // Award XP for catching specific fish types
        actionProcessor.processAction(player, ActionType.FISH, fishType, location);
    }
    
    // Example: Custom mining plugin integration
    @EventHandler
    public void onCustomMine(CustomMineEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (event.getMultiplier() > 1.0) {
            // Special mining event with multiplier
            double multiplier = event.getMultiplier();
            
            // Calculate base XP
            actionProcessor.processAction(player, ActionType.BREAK, 
                block.getType().name(), block.getLocation());
            
            // Apply additional XP for the multiplier
            JobManager jobManager = jobsAdventure.getJobManager();
            Set<String> jobs = jobManager.getPlayerJobs(player);
            for (String jobId : jobs) {
                Job job = jobManager.getJob(jobId);
                if (job.hasActions(ActionType.BREAK)) {
                    // Add bonus XP
                    jobManager.addXp(player, jobId, 10.0 * multiplier);
                }
            }
        }
    }
}
```

### Economy Integration

```java
public class EconomyIntegration {
    
    private final Economy economy;
    private final JobsAdventure jobsAdventure;
    
    public EconomyIntegration(Economy economy, JobsAdventure jobsAdventure) {
        this.economy = economy;
        this.jobsAdventure = jobsAdventure;
    }
    
    @EventHandler
    public void onLevelUp(PlayerLevelUpEvent event) {
        Player player = event.getPlayer();
        Job job = event.getJob();
        int newLevel = event.getNewLevel();
        
        // Award money based on level and job
        double money = calculateLevelUpReward(job, newLevel);
        economy.depositPlayer(player, money);
        
        MessageUtils.sendMessage(player, 
            "§a+$" + money + " for reaching level " + newLevel + " in " + job.getName());
    }
    
    private double calculateLevelUpReward(Job job, int level) {
        // Custom calculation based on job and level
        double baseReward = 100.0;
        double jobMultiplier = getJobMultiplier(job.getId());
        double levelMultiplier = Math.pow(1.1, level);
        
        return baseReward * jobMultiplier * levelMultiplier;
    }
    
    private double getJobMultiplier(String jobId) {
        return switch (jobId) {
            case "miner" -> 1.2;
            case "farmer" -> 1.0;
            case "hunter" -> 1.5;
            default -> 1.0;
        };
    }
}
```

---

## Best Practices

### Performance Considerations

1. **Use Async Operations When Possible**
```java
// Good - Async data operations
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    jobManager.savePlayerData(player);
});

// Bad - Sync operations in main thread
jobManager.saveAllPlayerData(); // This can cause lag
```

2. **Cache Expensive Calculations**
```java
private final Map<UUID, Integer> cachedLevels = new ConcurrentHashMap<>();

public int getCachedLevel(Player player, String jobId) {
    return cachedLevels.computeIfAbsent(player.getUniqueId(), 
        uuid -> jobManager.getLevel(player, jobId));
}
```

3. **Batch Operations**
```java
// Good - Batch multiple operations
List<Player> players = List.of(player1, player2, player3);
players.forEach(p -> jobManager.addXp(p, "miner", 10.0));

// Better - Use async batch operation
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    players.forEach(p -> jobManager.addXp(p, "miner", 10.0));
});
```

### Error Handling

```java
public void safeJobOperation(Player player, String jobId) {
    try {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager.hasJob(jobId)) {
            jobManager.addXp(player, jobId, 10.0);
        } else {
            getLogger().warning("Job not found: " + jobId);
        }
    } catch (Exception e) {
        getLogger().severe("Error in job operation: " + e.getMessage());
        e.printStackTrace();
    }
}
```

### Event Handling

```java
// Always check if event is cancelled
@EventHandler
public void onPlayerXpGain(PlayerXpGainEvent event) {
    if (event.isCancelled()) return;
    
    // Your logic here
}

// Use appropriate event priorities
@EventHandler(priority = EventPriority.HIGH)
public void onImportantEvent(PlayerJobJoinEvent event) {
    // High priority for important modifications
}

@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
public void onEventLogging(PlayerLevelUpEvent event) {
    // Monitor priority for logging (doesn't modify)
}
```

### Resource Management

```java
public class MyJobsIntegration implements Listener {
    
    private final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(2);
    
    public void cleanup() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
```

### Configuration Management

```java
// Always validate configuration values
public void loadConfig() {
    double multiplier = getConfig().getDouble("xp-multiplier", 1.0);
    if (multiplier <= 0) {
        getLogger().warning("Invalid XP multiplier, using default: 1.0");
        multiplier = 1.0;
    }
    
    String jobId = getConfig().getString("default-job");
    if (jobId != null && !jobManager.hasJob(jobId)) {
        getLogger().warning("Default job not found: " + jobId);
        jobId = null;
    }
}
```

### Testing

```java
// Example unit test for job operations
public class JobOperationTest {
    
    @Test
    public void testXpCalculation() {
        // Mock player and job
        Player mockPlayer = mock(Player.class);
        Job mockJob = mock(Job.class);
        
        when(mockJob.getId()).thenReturn("test_job");
        when(mockPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        
        // Test XP addition
        jobManager.addXp(mockPlayer, "test_job", 100.0);
        
        double xp = jobManager.getXp(mockPlayer, "test_job");
        assertEquals(100.0, xp, 0.01);
    }
}
```

---

This API documentation provides comprehensive coverage of JobsAdventure's extensibility features. Use these examples and patterns to create robust integrations and extensions for the plugin.