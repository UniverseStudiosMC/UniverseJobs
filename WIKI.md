# JobsAdventure - Complete Documentation

Welcome to the comprehensive JobsAdventure documentation. This is a professional-grade Minecraft jobs plugin with extensive features and integrations.

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Architecture](#architecture)
4. [Job System](#job-system)
5. [XP and Leveling](#xp-and-leveling)
6. [Reward System](#reward-system)
7. [Configuration](#configuration)
8. [Commands](#commands)
9. [Placeholders](#placeholders)
10. [Integrations](#integrations)
11. [API and Development](#api-and-development)
12. [Troubleshooting](#troubleshooting)

---

## Overview

JobsAdventure is a sophisticated jobs plugin that allows players to join professions, gain experience through various activities, and earn rewards based on their progress. The plugin features extensive customization options, multiple external plugin integrations, and a comprehensive reward system.

### Key Features

- **Multiple Job Support** - Unlimited configurable jobs with unique actions and rewards
- **Advanced XP System** - Mathematical curves, level caps, and bonus multipliers
- **Comprehensive Reward System** - Items, commands, and economy integration with GUI interface
- **External Plugin Integration** - Nexo, ItemsAdder, CustomCrops, CustomFishing, MythicMobs, MMOItems
- **PlaceholderAPI Support** - Extensive placeholders for leaderboards and statistics
- **Anti-Exploit Protection** - Block tracking system to prevent XP farming
- **Performance Optimized** - Async operations, caching, and concurrent data structures
- **Highly Configurable** - YAML-based configuration with hot reloading

### Requirements

- **Minecraft Server**: 1.17+ (Paper recommended)
- **Java**: 17+
- **Dependencies**: None (PlaceholderAPI optional but recommended)
- **Compatible Plugins**: Nexo, ItemsAdder, CustomCrops, CustomFishing, MythicMobs, MMOItems

---

## Quick Start

### Installation

1. Download the JobsAdventure plugin JAR file
2. Place it in your server's `plugins` folder
3. Install PlaceholderAPI (optional but recommended)
4. Start your server
5. Configure jobs in the `plugins/JobsAdventure/jobs/` folder
6. Use `/jobs reload` to apply configuration changes

### Basic Configuration

The plugin creates example job files on first run:
- `jobs/miner.yml` - Mining job example
- `jobs/farmer.yml` - Farming job example  
- `jobs/hunter.yml` - Hunting job example

### First Steps for Players

1. `/jobs list` - View available jobs
2. `/jobs join <job>` - Join a job
3. Start performing job-related activities to gain XP
4. `/jobs info` - View your progress
5. `/jobs rewards open <job>` - View and claim rewards

---

## Architecture

JobsAdventure follows a modular, manager-based architecture designed for performance and extensibility.

### Core Components

```
JobsAdventure (Main Plugin)
├── JobManager (Job system management)
├── RewardManager (Reward system and GUI)
├── ActionProcessor (Event processing and XP awarding)
├── XpBonusManager (Temporary XP bonuses)
├── PlaceholderManager (PlaceholderAPI integration)
├── ConfigManager (Configuration management)
└── Various Listeners (Event handling and integrations)
```

### Data Flow

```
Player Action → Event Trigger → Listener → ActionProcessor
    ↓
Condition Check → XP Calculation → Bonus Application → XP Award
    ↓
Level Check → Reward Qualification → Message Display
```

### Plugin Integration

The plugin uses dedicated event listeners for each external plugin:
- **NexoEventListener** - Custom block events from Nexo
- **ItemsAdderEventListener** - Custom items and blocks from ItemsAdder
- **CustomCropsEventListener** - Advanced farming from CustomCrops
- **CustomFishingEventListener** - Custom fishing from CustomFishing

---

## Job System

### Job Structure

Each job is defined in a YAML file in the `jobs/` folder with the following structure:

```yaml
name: "Miner"
description: "Extract valuable resources from the earth"
icon: "DIAMOND_PICKAXE"
enabled: true
max-level: 100
permission: "jobsadventure.job.miner"
lore:
  - "&7Mine blocks to gain experience"
  - "&7and unlock valuable rewards"

# XP System
xp-curve: "mining_curve"  # File-based curve
# OR
xp-equation: "100 * Math.pow(level, 1.5)"  # Mathematical equation

# XP Message Configuration
xp-message:
  enabled: true
  display-type: "action_bar"  # action_bar, chat, title, boss_bar
  message: "&a+{xp} XP &7({current}/{required})"
  sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
  volume: 0.5
  pitch: 1.0

# Actions
actions:
  break:
    stone:
      target: "STONE"
      xp: 1.0
      requirements:
        conditions:
          world:
            type: "world"
            values: ["world"]
    diamond_ore:
      target: "DIAMOND_ORE"
      xp: 10.0
      requirements:
        conditions:
          depth:
            type: "placeholder"
            placeholder: "%player_y%"
            operator: "<="
            value: "16"

# Rewards Configuration
gui-reward: "miner_gui"  # Links to gui/miner_gui.yml
rewards: "miner_rewards"  # Links to rewards/miner_rewards.yml
```

### Action Types

JobsAdventure supports numerous action types:

- **BREAK** - Block breaking (vanilla + custom)
- **PLACE** - Block placement
- **KILL** - Entity killing (vanilla + MythicMobs)
- **HARVEST** - Crop harvesting
- **INTERACT** - Block/entity interaction
- **BREED** - Animal breeding
- **FISH** - Fishing (vanilla + CustomFishing)
- **CRAFT** - Item crafting
- **SMELT** - Item smelting
- **ENCHANT** - Item enchanting
- **TRADE** - Villager trading
- **TAME** - Animal taming
- **SHEAR** - Sheep shearing
- **MILK** - Cow milking
- **EAT** - Food consumption
- **CUSTOM** - Plugin-specific actions

### Condition System

Actions can have complex requirements using the condition system:

```yaml
requirements:
  operator: "AND"  # AND/OR logic
  conditions:
    biome:
      type: "biome"
      values: ["DESERT", "BADLANDS"]
    item:
      type: "item"
      material: "DIAMOND_PICKAXE"
      enchantments:
        EFFICIENCY: 3
    permission:
      type: "permission"
      permission: "miner.advanced"
  groups:
    time_check:
      operator: "OR"
      conditions:
        day:
          type: "time"
          min: 0
          max: 12000
        night:
          type: "time"
          min: 12000
          max: 24000
```

### Available Condition Types

- **biome** - Check player's current biome
- **world** - Restrict to specific worlds
- **time** - Check in-game time ranges
- **weather** - Validate weather conditions
- **permission** - Check player permissions
- **item** - Validate held items (supports MMOItems)
- **placeholder** - Use PlaceholderAPI placeholders for complex checks

---

## XP and Leveling

### XP Curves

JobsAdventure supports two types of XP progression systems:

#### 1. Mathematical Equations
```yaml
xp-equation: "100 * Math.pow(level, 1.5)"
```

Supported functions:
- `Math.pow(base, exponent)` - Power function
- `Math.sqrt(number)` - Square root
- `Math.floor(number)` - Floor function
- `Math.ceil(number)` - Ceiling function
- Basic operators: `+`, `-`, `*`, `/`, `(`, `)`

#### 2. File-Based Curves

Create curves in `xp-curves/` folder:

```yaml
# xp-curves/mining_curve.yml
name: "Mining Curve"
description: "Balanced progression for miners"
levels:
  1: 0
  2: 100
  3: 250
  4: 450
  5: 700
  # ... continue up to max level
```

Pre-included curves:
- **linear** - Consistent XP increases
- **steep** - Rapid progression at high levels
- **gentle** - Slow, steady progression
- **combat** - Optimized for fighting
- **mining** - Balanced for resource gathering

### XP Bonus System

Temporary XP bonuses can be applied to players:

```bash
# Give 2x XP bonus for 1 hour to all online players
/jobs xpbonus give * 2.0 1h "Server Event"

# Give 1.5x XP bonus for 30 minutes to specific player for mining
/jobs xpbonus give PlayerName 1.5 30m miner "VIP Bonus"

# Remove all bonuses from a player
/jobs xpbonus remove PlayerName

# List active bonuses
/jobs xpbonus list PlayerName

# View bonus system statistics
/jobs xpbonus info
```

### Level Calculation

The plugin handles level calculations automatically:
- **Level-up detection** when XP thresholds are reached
- **Level-down prevention** (levels cannot decrease)
- **Max level enforcement** based on job configuration
- **XP overflow handling** for max level players

---

## Reward System

### Reward Configuration

Rewards are defined in YAML files in the `rewards/` folder:

```yaml
# rewards/miner_rewards.yml
name: "Miner Rewards"
description: "Rewards for dedicated miners"

rewards:
  level_5_pickaxe:
    name: "&6Iron Pickaxe"
    description: "&7A sturdy tool for mining"
    required-level: 5
    repeatable: false
    
    requirements:
      conditions:
        permission:
          type: "permission"
          permission: "miner.rewards.basic"
    
    items:
      - material: "IRON_PICKAXE"
        amount: 1
        name: "&6Miner's Iron Pickaxe"
        lore:
          - "&7Efficiency II"
          - "&7Unbreaking I"
        enchantments:
          EFFICIENCY: 2
          UNBREAKING: 1
    
    commands:
      - "give {player} diamond 5"
      - "broadcast &a{player} reached mining level 5!"

  weekly_bonus:
    name: "&bWeekly Mining Bonus"
    description: "&7Weekly reward for active miners"
    required-level: 10
    repeatable: true
    cooldown-hours: 168  # 1 week
    
    economy:
      amount: 1000
      reason: "Weekly mining bonus"
    
    commands:
      - "xp add {player} 100"
```

### Reward Types

- **Items** - Custom items with names, lore, and enchantments
- **Commands** - Execute server commands as rewards
- **Economy** - Money rewards (when economy plugin is present)
- **Messages** - Send custom messages to players

### Reward GUI System

#### GUI Configuration

Create custom GUIs in the `gui/` folder:

```yaml
# gui/miner_gui.yml
title: "&8Miner Rewards"
size: 54  # Must be multiple of 9

# Fill empty slots
fill-items:
  enabled: true
  material: "GRAY_STAINED_GLASS_PANE"
  name: " "
  slots: []  # Empty = fill all empty slots

# Custom decorative items
items:
  info:
    material: "BOOK"
    name: "&6Mining Information"
    lore:
      - "&7Job: &eMiner"
      - "&7Your Level: &a{level}"
      - "&7Your XP: &a{xp}"
    slots: [4]
    glowing: true

# Navigation
navigation:
  previous-page:
    material: "ARROW"
    name: "&ePrevious Page"
    slots: [45]
  next-page:
    material: "ARROW"
    name: "&eNext Page"
    slots: [53]
  close:
    material: "BARRIER"
    name: "&cClose"
    slots: [49]
  refresh:
    material: "EMERALD"
    name: "&aRefresh"
    slots: [48]

# Reward item slots (where actual rewards appear)
reward-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34]
```

### Reward Status System

Rewards have three states:
- **RETRIEVABLE** (Green) - Can be claimed
- **BLOCKED** (Red) - Requirements not met
- **RETRIEVED** (Gray) - Already claimed

### Claiming Rewards

Players can claim rewards through:
1. **GUI Interface** - `/jobs rewards open <job>`
2. **Direct Commands** - `/jobs rewards claim <job> <reward>`
3. **Automatic claiming** (if configured)

---

## Configuration

### Main Configuration (config.yml)

```yaml
# Main plugin settings
settings:
  debug: false
  save-interval: 300  # seconds
  language: "en"

# Default XP settings
xp:
  default-curve: "linear"
  max-level: 100
  base-multiplier: 1.0

# Permission-based multipliers
xp-multipliers:
  "jobsadventure.multiplier.vip": 1.5
  "jobsadventure.multiplier.premium": 2.0
  "jobsadventure.multiplier.admin": 10.0

# Anti-exploit protection
protection:
  block-tracking:
    enabled: true
    cleanup-interval: 3600  # seconds
    max-tracked-blocks: 10000

# Messages
messages:
  prefix: "&8[&6Jobs&8] "
  no-permission: "&cYou don't have permission for this!"
  job-joined: "&aYou joined the {job} job!"
  job-left: "&cYou left the {job} job!"
  max-level: "&eYou've reached the maximum level!"
  
# Sounds
sounds:
  level-up: "ENTITY_PLAYER_LEVELUP"
  xp-gain: "ENTITY_EXPERIENCE_ORB_PICKUP"
  reward-claim: "ENTITY_PLAYER_LEVELUP"
  error: "BLOCK_NOTE_BLOCK_BASS"
```

### Hot Reloading

All configurations support hot reloading:
```bash
/jobs reload  # Reloads all configurations
```

Changes applied without restart:
- Job configurations
- Reward configurations  
- GUI layouts
- XP curves
- Main plugin settings

---

## Commands

### Player Commands

```bash
# Job Management
/jobs list                          # List available jobs
/jobs join <job>                    # Join a job
/jobs leave <job>                   # Leave a job
/jobs info [job] [player]           # Show job/player information
/jobs stats [player]                # Show detailed statistics

# Reward System
/jobs rewards open <job>            # Open rewards GUI
/jobs rewards list [job]            # List available rewards
/jobs rewards claim <job> <reward>  # Claim specific reward
/jobs rewards info <job> <reward>   # Show reward details
```

### Admin Commands

```bash
# XP Bonus Management
/jobs xpbonus give <player|*> <multiplier> <duration> [job] [reason]
/jobs xpbonus remove <player> [job]
/jobs xpbonus list [player]
/jobs xpbonus info
/jobs xpbonus cleanup

# Reward Administration
/jobs rewards admin give <player> <job> <reward>    # Give reward to player
/jobs rewards admin reset <player> [job] [reward]   # Reset reward status
/jobs rewards admin reload                          # Reload reward configs

# System Administration
/jobs reload                        # Reload all configurations
/jobs debug <on|off>               # Toggle debug mode
```

### Permissions

```yaml
# Basic permissions
jobsadventure.use                   # Basic plugin usage
jobsadventure.join.*                # Join any job
jobsadventure.join.<job>            # Join specific job
jobsadventure.rewards.*             # Access all rewards
jobsadventure.rewards.<job>         # Access job-specific rewards

# Admin permissions
jobsadventure.admin                 # Full admin access
jobsadventure.admin.reload          # Reload configurations
jobsadventure.admin.xpbonus         # Manage XP bonuses
jobsadventure.admin.rewards         # Manage rewards
jobsadventure.admin.debug           # Debug mode access

# XP Multipliers
jobsadventure.multiplier.vip        # 1.5x XP multiplier
jobsadventure.multiplier.premium    # 2.0x XP multiplier
jobsadventure.multiplier.admin      # 10.0x XP multiplier
```

---

## Placeholders

JobsAdventure provides extensive PlaceholderAPI integration. See [placeholders_guide.md](placeholders_guide.md) for complete documentation.

### Job-Specific Placeholders

```
%jobsadventure_<job>_player_level%          # Player's level in job
%jobsadventure_<job>_player_xp%             # Player's XP in job
%jobsadventure_<job>_player_rank%           # Player's rank in job leaderboard
%jobsadventure_<job>_player_progress%       # XP progress to next level
%jobsadventure_<job>_player_progresspercent% # Progress percentage
%jobsadventure_<job>_player_hasjob%         # Whether player has the job
```

### Leaderboard Placeholders

```
%jobsadventure_<job>_leaderboard_<position>_name%      # Player name at position
%jobsadventure_<job>_leaderboard_<position>_level%     # Player level at position
%jobsadventure_<job>_leaderboard_<position>_xp%        # Player XP at position
%jobsadventure_<job>_leaderboard_<position>_formatted% # Formatted leaderboard entry
```

### Global Statistics

```
%jobsglobal_totallevels_<position>_name%     # Top player by total levels
%jobsglobal_totaljobs_<position>_name%       # Top player by job count
%jobsglobal_totalxp_<position>_name%         # Top player by total XP
%jobsglobal_player_totallevels%              # Player's total levels
%jobsglobal_player_totaljobs%                # Player's job count
%jobsglobal_player_totalxp%                  # Player's total XP
```

---

## Integrations

### PlaceholderAPI

**Installation**: Required for placeholder functionality
**Features**: 
- Job statistics and leaderboards
- Global player statistics
- Real-time data with caching
- Custom formatting options

### Nexo Integration

**Installation**: Automatic when Nexo is detected
**Features**:
- Custom block breaking/placing XP
- Nexo-specific material detection
- Integration with job actions
- Block protection compatibility

### ItemsAdder Integration

**Installation**: Automatic when ItemsAdder is detected
**Features**:
- Custom item requirements in conditions
- Custom block XP rewards
- ItemsAdder material recognition
- Advanced item validation

### CustomCrops Integration

**Installation**: Automatic when CustomCrops is detected
**Features**:
- Crop breaking XP rewards
- Crop harvesting actions
- Crop placement tracking
- Growth stage detection

### CustomFishing Integration

**Installation**: Automatic when CustomFishing is detected
**Features**:
- Custom fishing loot XP
- Integration with fishing actions
- Custom fish type detection
- Fishing competition support

### MythicMobs Integration

**Installation**: Automatic when MythicMobs is detected
**Features**:
- Custom mob killing XP
- MythicMob type detection
- Integration with kill actions
- Advanced mob identification

### MMOItems Integration

**Installation**: Automatic when MMOItems is detected
**Features**:
- MMOItem validation in conditions
- Custom item type checking
- Advanced item requirements
- Stat-based conditions

---

## API and Development

### Event API

JobsAdventure fires custom events for developers:

```java
// Player joins a job
PlayerJobJoinEvent event = new PlayerJobJoinEvent(player, job);
Bukkit.getPluginManager().callEvent(event);

// Player gains XP
PlayerXpGainEvent event = new PlayerXpGainEvent(player, job, xpAmount);
Bukkit.getPluginManager().callEvent(event);

// Player levels up
PlayerLevelUpEvent event = new PlayerLevelUpEvent(player, job, newLevel);
Bukkit.getPluginManager().callEvent(event);

// Player claims reward
PlayerRewardClaimEvent event = new PlayerRewardClaimEvent(player, reward);
Bukkit.getPluginManager().callEvent(event);
```

### Developer API

```java
// Get plugin instance
JobsAdventure plugin = JobsAdventure.getInstance();

// Access managers
JobManager jobManager = plugin.getJobManager();
RewardManager rewardManager = plugin.getRewardManager();

// Player job operations
PlayerJobData playerData = jobManager.getPlayerData(player);
boolean hasJob = playerData.hasJob("miner");
int level = playerData.getLevel("miner");
double xp = playerData.getXp("miner");

// Add XP programmatically
jobManager.addXp(player, "miner", 100.0);

// Reward operations
RewardStatus status = rewardManager.getRewardStatus(player, reward);
boolean claimed = rewardManager.claimReward(player, reward);
```

### Custom Condition Development

Create custom conditions by extending AbstractCondition:

```java
public class CustomCondition extends AbstractCondition {
    
    public CustomCondition(ConfigurationSection config) {
        super(config);
        // Initialize custom parameters
    }
    
    @Override
    public ConditionResult evaluate(ConditionContext context) {
        // Implement your condition logic
        boolean passed = /* your logic here */;
        
        return new ConditionResult(passed, 
            passed ? getAcceptMessage() : getDenyMessage());
    }
}
```

### Custom Action Type Development

Register custom action types for specific plugin integration:

```java
// In your plugin's onEnable()
ActionType.registerCustomType("MY_CUSTOM_ACTION", MyCustomActionProcessor.class);
```

---

## Troubleshooting

### Common Issues

#### 1. Jobs Not Loading
**Symptoms**: Jobs folder empty or jobs not appearing in `/jobs list`
**Solutions**:
- Check file permissions on jobs folder
- Verify YAML syntax in job files
- Check server logs for parsing errors
- Ensure job files have `.yml` extension

#### 2. XP Not Being Awarded
**Symptoms**: Players not gaining XP for actions
**Solutions**:
- Check if player has job permission
- Verify action configuration in job file
- Check condition requirements (biome, world, etc.)
- Enable debug mode: `/jobs debug on`
- Check anti-exploit protection settings

#### 3. Placeholders Not Working
**Symptoms**: Placeholders showing as text instead of values
**Solutions**:
- Install PlaceholderAPI plugin
- Use `/papi reload` after JobsAdventure installation
- Check placeholder syntax
- Verify job names match exactly

#### 4. Rewards GUI Not Opening
**Symptoms**: GUI command not working or showing empty inventory
**Solutions**:
- Check GUI configuration file syntax
- Verify reward file exists and is valid
- Check player permissions for rewards
- Review server logs for errors

#### 5. Performance Issues
**Symptoms**: Server lag when players are active
**Solutions**:
- Reduce save-interval in config
- Limit tracked blocks in protection settings
- Check for excessive XP bonus usage
- Monitor debug logs for bottlenecks

### Debug Mode

Enable comprehensive logging:
```bash
/jobs debug on
```

Debug information includes:
- Action processing details
- Condition evaluation results
- XP calculation steps
- Performance metrics
- Error stack traces

### Log Analysis

Check these log patterns:

```
[JobsAdventure] Player XP: +5.0 for action BREAK:STONE
[JobsAdventure] Condition failed: BiomeCondition - Expected DESERT, got PLAINS
[JobsAdventure] Level up! Player reached level 10 in miner
[JobsAdventure] Reward claimed: weekly_bonus by PlayerName
```

### Configuration Validation

The plugin automatically validates configurations and logs issues:
- Missing required fields
- Invalid material names
- Malformed XP equations
- Circular dependencies in conditions

### Performance Monitoring

Monitor performance with:
```bash
/jobs xpbonus info  # View bonus system statistics
```

Watch for:
- High number of active bonuses
- Excessive cleanup operations
- Memory usage patterns
- Database operation times

### Support and Resources

- **Issue Reporting**: Check server logs and provide full error messages
- **Configuration Help**: Validate YAML syntax online before testing
- **Performance Tuning**: Monitor server TPS and adjust save intervals
- **Custom Development**: Use the API documentation for extensions

---

## Version History and Updates

### Latest Features
- Advanced GUI system with custom layouts
- Global leaderboard placeholders
- Enhanced anti-exploit protection
- Multiple external plugin integrations
- Mathematical XP curve support
- Comprehensive reward system

### Planned Features
- Database storage support
- Advanced economy integration
- Quest system integration
- Multi-server synchronization
- Web interface for statistics
- Advanced analytics and reporting

---

*This documentation covers JobsAdventure v1.0+. For specific version features, check the plugin's changelog and release notes.*