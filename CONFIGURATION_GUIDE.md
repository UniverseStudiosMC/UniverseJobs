# JobsAdventure Configuration Guide

This comprehensive guide covers all configuration aspects of JobsAdventure, from basic setup to advanced customization.

## Table of Contents

1. [Configuration Overview](#configuration-overview)
2. [Main Configuration](#main-configuration)
3. [Job Configuration](#job-configuration)
4. [Reward Configuration](#reward-configuration)
5. [GUI Configuration](#gui-configuration)
6. [XP Curve Configuration](#xp-curve-configuration)
7. [Advanced Features](#advanced-features)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

---

## Configuration Overview

JobsAdventure uses a multi-file configuration system for maximum flexibility and organization:

```
plugins/JobsAdventure/
├── config.yml                 # Main plugin configuration
├── jobs/                      # Job definitions
│   ├── miner.yml
│   ├── farmer.yml
│   └── hunter.yml
├── rewards/                   # Reward configurations
│   ├── miner_rewards.yml
│   ├── farmer_rewards.yml
│   └── hunter_rewards.yml
├── gui/                       # GUI layout configurations
│   ├── miner_gui.yml
│   ├── farmer_gui.yml
│   └── default_gui.yml
├── xp-curves/                 # XP curve definitions
│   ├── linear.yml
│   ├── steep.yml
│   └── gentle.yml
└── data/                      # Player data (auto-generated)
    ├── uuid1.yml
    └── uuid2.yml
```

### Key Features

- **Hot Reloading**: All configurations can be reloaded without server restart
- **Validation**: Automatic validation with helpful error messages
- **Defaults**: Missing values are automatically filled with sensible defaults
- **Comments**: Extensive inline documentation in config files
- **Placeholders**: Support for PlaceholderAPI in messages and some values

---

## Main Configuration

### config.yml Structure

```yaml
# ================================
# JobsAdventure Main Configuration
# ================================

# Plugin settings
settings:
  # Enable debug logging for troubleshooting
  debug: false
  
  # Auto-save interval in seconds (0 to disable)
  save-interval: 300
  
  # Default language (future feature)
  language: "en"
  
  # Maximum jobs a player can have (0 = unlimited)
  max-jobs-per-player: 0

# Default XP system settings
xp:
  # Default XP curve for jobs without specific curves
  default-curve: "linear"
  
  # Default maximum level for all jobs
  default-max-level: 100
  
  # Base XP multiplier applied to all XP gains
  base-multiplier: 1.0
  
  # Level-up sound settings
  level-up-sound:
    enabled: true
    sound: "ENTITY_PLAYER_LEVELUP"
    volume: 1.0
    pitch: 1.0

# Permission-based XP multipliers
# Players with these permissions get XP multiplied
xp-multipliers:
  "jobsadventure.multiplier.vip": 1.5
  "jobsadventure.multiplier.premium": 2.0
  "jobsadventure.multiplier.mvp": 3.0
  "jobsadventure.multiplier.admin": 10.0

# Anti-exploit protection settings
protection:
  block-tracking:
    # Enable block tracking to prevent placed-block XP farming
    enabled: true
    
    # How often to clean up tracking data (seconds)
    cleanup-interval: 3600
    
    # Maximum blocks to track per chunk
    max-tracked-blocks: 1000
    
    # NBT tag used for tracking (change if conflicts occur)
    nbt-tag: "jobsadventure_placed"

# Bonus system settings
bonuses:
  # Maximum bonuses per player
  max-bonuses-per-player: 5
  
  # Maximum global bonuses
  max-global-bonuses: 10
  
  # Cleanup interval for expired bonuses (seconds)
  cleanup-interval: 900

# Message system
messages:
  # Message prefix for all plugin messages
  prefix: "&8[&6Jobs&8] "
  
  # Core messages
  no-permission: "&cYou don't have permission to do this!"
  player-not-found: "&cPlayer not found!"
  job-not-found: "&cJob '{job}' not found!"
  reward-not-found: "&cReward '{reward}' not found!"
  
  # Job messages
  job-joined: "&aYou joined the {job} job!"
  job-left: "&cYou left the {job} job!"
  job-already-joined: "&eYou already have the {job} job!"
  job-not-joined: "&cYou don't have the {job} job!"
  job-max-level: "&eYou've reached the maximum level in {job}!"
  job-permission-required: "&cYou need permission to join {job}!"
  
  # XP messages
  xp-gained: "&a+{xp} XP &7in {job}"
  level-up: "&6Level Up! &e{job} Level {level}"
  
  # Reward messages
  reward-claimed: "&aReward '{reward}' claimed successfully!"
  reward-claim-failed: "&cFailed to claim reward '{reward}'!"
  reward-already-claimed: "&eYou've already claimed this reward!"
  reward-not-available: "&cThis reward is not available to you!"
  reward-level-required: "&cYou need level {level} in {job} to claim this!"
  
  # Bonus messages
  bonus-applied: "&aXP bonus applied: {multiplier}x for {duration}!"
  bonus-expired: "&eYour XP bonus has expired!"
  bonus-removed: "&cXP bonus removed!"
  
  # Admin messages
  config-reloaded: "&aConfiguration reloaded successfully!"
  debug-enabled: "&aDebug mode enabled!"
  debug-disabled: "&cDebug mode disabled!"

# Sound effects
sounds:
  # XP gain sound
  xp-gain:
    enabled: true
    sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
    volume: 0.5
    pitch: 1.0
  
  # Level up sound  
  level-up:
    enabled: true
    sound: "ENTITY_PLAYER_LEVELUP"
    volume: 1.0
    pitch: 1.0
  
  # Reward claim sound
  reward-claim:
    enabled: true
    sound: "ENTITY_PLAYER_LEVELUP"
    volume: 0.8
    pitch: 1.2
  
  # Error sound
  error:
    enabled: true
    sound: "BLOCK_NOTE_BLOCK_BASS"
    volume: 1.0
    pitch: 0.5

# Database settings (future feature)
database:
  type: "file"  # file, mysql, sqlite
  
  # MySQL settings (when type: mysql)
  mysql:
    host: "localhost"
    port: 3306
    database: "jobsadventure"
    username: "user"
    password: "password"
    table-prefix: "ja_"

# Integration settings
integrations:
  placeholderapi:
    # Enable PlaceholderAPI integration
    enabled: true
    
    # Cache duration for leaderboards (seconds)
    cache-duration: 30
    
    # Cache duration for global stats (seconds)
    global-cache-duration: 60
  
  economy:
    # Enable economy integration (requires Vault)
    enabled: false
    
    # Default currency symbol
    currency-symbol: "$"

# Performance settings
performance:
  # Use async operations where possible
  async-operations: true
  
  # Thread pool size for async operations
  thread-pool-size: 2
  
  # Batch size for bulk operations
  batch-size: 50

# Debug settings
debug:
  # Log all XP gains
  log-xp-gains: false
  
  # Log action processing
  log-actions: false
  
  # Log condition evaluations
  log-conditions: false
  
  # Log performance metrics
  log-performance: false
```

### Configuration Validation

The plugin automatically validates the main configuration and will:
- Set default values for missing options
- Log warnings for invalid values
- Provide helpful error messages for common mistakes

---

## Job Configuration

### Basic Job Structure

```yaml
# jobs/example_job.yml

# ============================
# Basic Job Information
# ============================

# Display name (supports color codes)
name: "&6Example Job"

# Job description
description: "An example job for demonstration"

# Material name for GUI icon
icon: "DIAMOND_PICKAXE"

# Whether this job is enabled
enabled: true

# Maximum level for this job
max-level: 100

# Permission required to join this job
permission: "jobsadventure.job.example"

# Lore lines for GUI display (supports color codes)
lore:
  - "&7This is an example job"
  - "&7demonstrating the configuration"
  - "&7system of JobsAdventure"

# ============================
# XP System Configuration
# ============================

# Option 1: Use a predefined XP curve file
xp-curve: "linear"

# Option 2: Use a mathematical equation
# xp-equation: "100 * Math.pow(level, 1.5)"

# Option 3: Use default curve (omit both options above)

# ============================
# XP Message Configuration
# ============================

xp-message:
  # Enable XP messages for this job
  enabled: true
  
  # Display type: action_bar, chat, title, boss_bar
  display-type: "action_bar"
  
  # Message format (placeholders available)
  message: "&a+{xp} XP &7({current}/{required})"
  
  # Sound configuration
  sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
  volume: 0.5
  pitch: 1.0
  
  # Batch messages to reduce spam
  batch:
    enabled: true
    duration: 1000  # milliseconds
    format: "&a+{total_xp} XP &7({count} actions)"

# ============================
# Job Actions Configuration
# ============================

actions:
  # Block breaking actions
  break:
    # Simple action - just material and XP
    stone:
      target: "STONE"
      xp: 1.0
    
    # Advanced action with requirements
    diamond_ore:
      target: "DIAMOND_ORE"
      xp: 20.0
      enabled: true
      
      # Custom message for this action
      message:
        enabled: true
        message: "&6+{xp} XP &7for mining {target}!"
        sound: "BLOCK_STONE_BREAK"
      
      # Requirements that must be met
      requirements:
        operator: "AND"
        conditions:
          # Must be below Y level 16
          depth:
            type: "placeholder"
            placeholder: "%player_y%"
            operator: "<="
            value: "16"
          
          # Must have diamond pickaxe
          tool:
            type: "item"
            material: "DIAMOND_PICKAXE"
          
          # Must be in specific world
          world:
            type: "world"
            values: ["world"]
        
        # Deny configuration (when requirements not met)
        deny:
          message:
            enabled: true
            message: "&cYou need a diamond pickaxe and be below Y16!"
            sound: "ENTITY_VILLAGER_NO"
  
  # Entity killing actions
  kill:
    # Vanilla mobs
    zombie:
      target: "ZOMBIE"
      xp: 5.0
    
    cow:
      target: "COW"
      xp: 2.0
      requirements:
        conditions:
          permission:
            type: "permission"
            permission: "example.kill.animals"
    
    # MythicMobs integration (if available)
    boss_mob:
      target: "mythicmobs:ancient_dragon"
      xp: 500.0
      requirements:
        conditions:
          level:
            type: "placeholder"
            placeholder: "%jobsadventure_example_player_level%"
            operator: ">="
            value: "50"
  
  # Block placement actions
  place:
    # Placing specific blocks
    cobblestone:
      target: "COBBLESTONE"
      xp: 0.5
      
      # Anti-exploit: low XP for placing common blocks
      requirements:
        conditions:
          # Only in creative mode gets reduced XP
          gamemode:
            type: "placeholder"
            placeholder: "%player_gamemode%"
            operator: "!="
            value: "CREATIVE"
  
  # Fishing actions
  fish:
    # Vanilla fish
    cod:
      target: "COD"
      xp: 3.0
    
    # CustomFishing integration (if available)
    legendary_fish:
      target: "customfishing:legendary_salmon"
      xp: 100.0
  
  # Crafting actions
  craft:
    # Crafted items
    iron_sword:
      target: "IRON_SWORD"
      xp: 10.0
    
    # MMOItems integration (if available)
    custom_weapon:
      target: "mmoitems:SWORD:FIRE_BLADE"
      xp: 50.0
  
  # Harvesting actions (CustomCrops integration)
  harvest:
    wheat:
      target: "WHEAT"
      xp: 2.0
    
    # Custom crops
    magic_crop:
      target: "customcrops:magic_wheat"
      xp: 20.0
  
  # Breeding actions
  breed:
    cow:
      target: "COW"
      xp: 5.0
    
    horse:
      target: "HORSE"
      xp: 15.0
  
  # Smelting actions
  smelt:
    iron_ingot:
      target: "IRON_INGOT"
      xp: 3.0
    
    gold_ingot:
      target: "GOLD_INGOT"
      xp: 5.0
  
  # Enchanting actions
  enchant:
    # Any enchantment
    any:
      target: "*"
      xp: 10.0
    
    # Specific enchantment levels
    sharpness_5:
      target: "SHARPNESS:5"
      xp: 50.0
  
  # Custom actions (plugin-specific)
  custom:
    # Integration with other plugins
    special_action:
      target: "custom_plugin:special_event"
      xp: 25.0

# ============================
# Reward System Configuration
# ============================

# Link to GUI configuration file
gui-reward: "example_gui"

# Link to rewards configuration file
rewards: "example_rewards"

# ============================
# Advanced Job Settings
# ============================

# Job-specific settings
settings:
  # Allow players to leave this job
  allow-leave: true
  
  # Announce level ups to server
  announce-levelups: false
  
  # Custom join/leave commands
  join-commands:
    - "broadcast &a{player} joined the {job} job!"
  
  leave-commands:
    - "broadcast &c{player} left the {job} job!"
```

### Action Types Reference

| Action Type | Description | Target Format | Example |
|-------------|-------------|---------------|---------|
| `break` | Block breaking | Material name | `STONE`, `DIAMOND_ORE` |
| `place` | Block placement | Material name | `COBBLESTONE`, `WHEAT` |
| `kill` | Entity killing | Entity type or MythicMob | `ZOMBIE`, `mythicmobs:boss` |
| `fish` | Fishing | Fish type or custom | `COD`, `customfishing:salmon` |
| `craft` | Item crafting | Item or MMOItem | `IRON_SWORD`, `mmoitems:SWORD:BLADE` |
| `smelt` | Item smelting | Result item | `IRON_INGOT` |
| `enchant` | Item enchanting | Enchantment | `SHARPNESS:5`, `*` |
| `harvest` | Crop harvesting | Crop type | `WHEAT`, `customcrops:corn` |
| `breed` | Animal breeding | Animal type | `COW`, `HORSE` |
| `interact` | Block/entity interaction | Target type | `VILLAGER`, `CRAFTING_TABLE` |
| `trade` | Villager trading | Trade result | `EMERALD` |
| `tame` | Animal taming | Animal type | `WOLF`, `CAT` |
| `shear` | Sheep shearing | Always `SHEEP` | `SHEEP` |
| `milk` | Cow milking | Always `COW` | `COW` |
| `eat` | Food consumption | Food item | `BREAD`, `APPLE` |
| `custom` | Plugin-specific | Custom format | `plugin:action` |

### Condition Types Reference

| Condition Type | Description | Configuration | Example |
|----------------|-------------|---------------|---------|
| `world` | Check player's world | `values: [world1, world2]` | Restrict to specific worlds |
| `biome` | Check player's biome | `values: [DESERT, PLAINS]` | Biome restrictions |
| `time` | Check in-game time | `min: 0, max: 12000` | Day/night restrictions |
| `weather` | Check weather | `values: [CLEAR, RAIN]` | Weather-based actions |
| `permission` | Check permission | `permission: "example.perm"` | Permission requirements |
| `item` | Check held item | `material: DIAMOND_PICKAXE` | Tool requirements |
| `placeholder` | PlaceholderAPI check | `placeholder: "%player_y%"` | Complex conditions |

---

## Reward Configuration

### Basic Reward Structure

```yaml
# rewards/example_rewards.yml

# ============================
# Reward Configuration
# ============================

name: "Example Job Rewards"
description: "Rewards for the example job"

# Individual rewards
rewards:
  # Level-based reward
  level_5_tools:
    # Display information
    name: "&6Starter Tools"
    description: "&7Basic tools for new workers"
    
    # Requirements
    required-level: 5
    repeatable: false
    
    # Permission requirement (optional)
    permission: "example.rewards.starter"
    
    # Additional requirements using condition system
    requirements:
      conditions:
        world:
          type: "world"
          values: ["world"]
    
    # Item rewards
    items:
      - material: "IRON_PICKAXE"
        amount: 1
        name: "&6Worker's Pickaxe"
        lore:
          - "&7A sturdy tool for hard work"
          - "&7Efficiency II"
        enchantments:
          EFFICIENCY: 2
          UNBREAKING: 1
        custom-model-data: 12345
      
      - material: "IRON_SHOVEL"
        amount: 1
        name: "&6Worker's Shovel"
        lore:
          - "&7Perfect for digging"
    
    # Command rewards (executed as console)
    commands:
      - "give {player} diamond 5"
      - "xp add {player} 100"
      - "broadcast &a{player} earned starter tools!"
    
    # Economy reward (requires Vault)
    economy:
      amount: 1000
      reason: "Level 5 bonus"
  
  # Repeatable reward with cooldown
  daily_bonus:
    name: "&bDaily Worker Bonus"
    description: "&7Daily reward for active workers"
    
    required-level: 10
    repeatable: true
    cooldown-hours: 24
    
    # Complex requirements
    requirements:
      operator: "AND"
      conditions:
        # Must be online for at least 1 hour today
        playtime:
          type: "placeholder"
          placeholder: "%statistic_time_played%"
          operator: ">="
          value: "3600000"  # milliseconds
        
        # Must have completed at least 50 actions today
        actions:
          type: "placeholder"
          placeholder: "%jobsadventure_example_player_level%"
          operator: ">="
          value: "10"
      
      # Custom deny message
      deny:
        message:
          enabled: true
          message: "&cYou must be level 10+ and play for 1 hour to claim this!"
          sound: "ENTITY_VILLAGER_NO"
    
    # Mixed rewards
    items:
      - material: "EMERALD"
        amount: 5
        name: "&aDaily Emerald Bonus"
    
    commands:
      - "jobsadventure xpbonus give {player} 1.5 1h daily \"Daily XP Bonus\""
    
    economy:
      amount: 500
      reason: "Daily bonus"
  
  # High-level exclusive reward
  master_equipment:
    name: "&5Master's Equipment Set"
    description: "&7Legendary equipment for masters"
    
    required-level: 75
    repeatable: false
    
    # Strict requirements
    requirements:
      operator: "AND"
      conditions:
        # Must have multiple jobs at high level
        total_levels:
          type: "placeholder"
          placeholder: "%jobsglobal_player_totallevels%"
          operator: ">="
          value: "200"
        
        # Must have special permission
        vip:
          type: "permission"
          permission: "example.vip"
      
      groups:
        # Either condition can be met
        alternative_requirements:
          operator: "OR"
          conditions:
            # Either be an admin
            admin:
              type: "permission"
              permission: "example.admin"
            
            # Or have played for 100 hours
            veteran:
              type: "placeholder"
              placeholder: "%statistic_time_played%"
              operator: ">="
              value: "360000000"  # 100 hours in milliseconds
    
    # Valuable items
    items:
      - material: "NETHERITE_PICKAXE"
        amount: 1
        name: "&5Master's Netherite Pickaxe"
        lore:
          - "&7The ultimate mining tool"
          - "&7Efficiency V"
          - "&7Fortune III"
          - "&7Unbreaking III"
          - "&7Mending"
        enchantments:
          EFFICIENCY: 5
          FORTUNE: 3
          UNBREAKING: 3
          MENDING: 1
      
      - material: "NETHERITE_HELMET"
        amount: 1
        name: "&5Master's Crown"
        lore:
          - "&7Symbol of true mastery"
          - "&7Protection IV"
          - "&7Respiration III"
          - "&7Aqua Affinity"
        enchantments:
          PROTECTION: 4
          RESPIRATION: 3
          AQUA_AFFINITY: 1
    
    # Special commands
    commands:
      - "lp user {player} permission set example.master true"
      - "broadcast &5{player} has achieved Master status!"
      - "title {player} title &5MASTER"
      - "title {player} subtitle &7You have achieved true mastery!"
    
    economy:
      amount: 10000
      reason: "Master achievement bonus"
  
  # Event-based reward
  weekend_special:
    name: "&cWeekend Special"
    description: "&7Special weekend bonus"
    
    required-level: 1
    repeatable: true
    cooldown-hours: 168  # 1 week
    
    # Time-based requirements
    requirements:
      conditions:
        weekend:
          type: "placeholder"
          placeholder: "%server_time_formatted_E%"
          operator: "contains"
          value: "Sat|Sun"
    
    items:
      - material: "EXPERIENCE_BOTTLE"
        amount: 10
        name: "&aWeekend XP Boost"
    
    commands:
      - "jobsadventure xpbonus give {player} 2.0 2h weekend \"Weekend Bonus\""

# ============================
# Global Reward Settings
# ============================

settings:
  # Default GUI configuration
  default-gui: "default_rewards_gui"
  
  # Announcement settings
  announcements:
    # Announce when players claim major rewards
    major-rewards: true
    major-level-threshold: 50
    
    # Broadcast format
    format: "&a{player} claimed &e{reward}&a!"
  
  # Auto-claim settings
  auto-claim:
    enabled: false
    level-threshold: 10  # Only auto-claim rewards below this level
    
  # Cooldown settings
  cooldowns:
    # Use real-time or playtime for cooldowns
    use-playtime: false
    
    # Cooldown format in messages
    format: "{days}d {hours}h {minutes}m"
```

### Item Configuration

```yaml
# Detailed item configuration example
items:
  - material: "DIAMOND_SWORD"
    amount: 1
    
    # Basic metadata
    name: "&c&lLegendary Blade"
    lore:
      - "&7A weapon of legends"
      - "&7forged in dragon fire"
      - ""
      - "&cDamage: &f+15"
      - "&eSharpness V"
      - "&eLooting III"
    
    # Enchantments
    enchantments:
      SHARPNESS: 5
      LOOTING: 3
      UNBREAKING: 3
      MENDING: 1
    
    # Visual effects
    glowing: true
    custom-model-data: 789
    
    # Item flags (hide enchantments, attributes, etc.)
    item-flags:
      - "HIDE_ENCHANTS"
      - "HIDE_ATTRIBUTES"
    
    # Custom attributes (1.16+)
    attributes:
      - attribute: "GENERIC_ATTACK_DAMAGE"
        amount: 15.0
        operation: "ADD_NUMBER"
        slot: "HAND"
      
      - attribute: "GENERIC_ATTACK_SPEED"
        amount: 0.2
        operation: "ADD_SCALAR"
        slot: "HAND"
    
    # NBT data (advanced)
    nbt: "{CustomTags:[\"legendary\",\"reward\"]}"
    
    # MMOItems integration (if available)
    mmo-item:
      type: "SWORD"
      id: "LEGENDARY_BLADE"
      
    # Oraxen integration (if available)
    oraxen-item: "legendary_sword"
    
    # ItemsAdder integration (if available)
    items-adder: "weapons:legendary_sword"
```

---

## GUI Configuration

### Basic GUI Layout

```yaml
# gui/example_gui.yml

# ============================
# GUI Configuration
# ============================

# GUI title (supports color codes and placeholders)
title: "&8{job} Rewards - Page {page}"

# Inventory size (must be multiple of 9, max 54)
size: 54

# ============================
# Fill Items (Background)
# ============================

fill-items:
  enabled: true
  material: "GRAY_STAINED_GLASS_PANE"
  name: " "  # Empty name to hide
  
  # Specific slots to fill (empty array = fill all empty slots)
  slots: []
  
  # Alternative: fill specific slots only
  # slots: [0, 1, 2, 6, 7, 8, 45, 46, 47, 51, 52, 53]

# ============================
# Custom Items (Decorative)
# ============================

items:
  # Job information display
  job_info:
    material: "BOOK"
    name: "&6{job} Information"
    lore:
      - "&7Job: &e{job}"
      - "&7Your Level: &a{level}"
      - "&7Your XP: &a{xp}/{required}"
      - "&7Progress: &a{progress_percent}%"
      - ""
      - "&7Total Rewards: &e{total_rewards}"
      - "&7Available: &a{available_rewards}"
      - "&7Claimed: &c{claimed_rewards}"
    
    # Slots where this item appears
    slots: [4]
    
    # Visual effects
    glowing: true
    amount: 1
    custom-model-data: 0
    
    # Enchantments for visual effect
    enchantments:
      LURE: 1
    
    # Hide enchantment glint
    item-flags:
      - "HIDE_ENCHANTS"
  
  # Decorative borders
  border_top:
    material: "BLUE_STAINED_GLASS_PANE"
    name: " "
    slots: [0, 1, 2, 6, 7, 8]
  
  border_bottom:
    material: "BLUE_STAINED_GLASS_PANE"
    name: " "
    slots: [45, 46, 47, 51, 52, 53]
  
  # Status indicators
  available_indicator:
    material: "LIME_CONCRETE"
    name: "&aAvailable Rewards"
    lore:
      - "&7Green items can be claimed"
    slots: [3]
  
  blocked_indicator:
    material: "RED_CONCRETE"
    name: "&cBlocked Rewards"
    lore:
      - "&7Red items cannot be claimed yet"
    slots: [5]

# ============================
# Navigation Configuration
# ============================

navigation:
  # Previous page button
  previous-page:
    material: "ARROW"
    name: "&e← Previous Page"
    lore:
      - "&7Click to go to the previous page"
    slots: [48]
    
    # Only show if not on first page
    show-condition: "{page} > 1"
  
  # Next page button
  next-page:
    material: "ARROW"
    name: "&eNext Page →"
    lore:
      - "&7Click to go to the next page"
    slots: [50]
    
    # Only show if there are more pages
    show-condition: "{page} < {max_pages}"
  
  # Close button
  close:
    material: "BARRIER"
    name: "&cClose"
    lore:
      - "&7Click to close this GUI"
    slots: [49]
  
  # Refresh button
  refresh:
    material: "EMERALD"
    name: "&aRefresh"
    lore:
      - "&7Click to refresh the GUI"
      - "&7(Updates reward statuses)"
    slots: [47]
    glowing: true
  
  # Information button
  info:
    material: "INFORMATION_SIGN"  # 1.14+
    name: "&bInformation"
    lore:
      - "&7GUI Information:"
      - "&7• Green = Can claim"
      - "&7• Red = Cannot claim"
      - "&7• Gray = Already claimed"
      - ""
      - "&7Left click to claim rewards"
      - "&7Right click for details"
    slots: [53]

# ============================
# Reward Slots Configuration
# ============================

# Slots where reward items will be displayed
reward-slots: [
  10, 11, 12, 13, 14, 15, 16,  # Row 2
  19, 20, 21, 22, 23, 24, 25,  # Row 3
  28, 29, 30, 31, 32, 33, 34,  # Row 4
  37, 38, 39, 40, 41, 42, 43   # Row 5
]

# ============================
# Advanced GUI Settings
# ============================

settings:
  # Pagination
  rewards-per-page: 21  # Should match reward-slots size
  
  # Click actions
  click-actions:
    # Left click to claim
    left-click: "claim"
    
    # Right click for information
    right-click: "info"
    
    # Shift click for bulk actions (future feature)
    shift-click: "none"
  
  # Update settings
  auto-refresh:
    enabled: true
    interval: 30  # seconds
  
  # Sound effects
  sounds:
    open: "BLOCK_CHEST_OPEN"
    close: "BLOCK_CHEST_CLOSE"
    page-turn: "UI_BUTTON_CLICK"
    claim-success: "ENTITY_PLAYER_LEVELUP"
    claim-fail: "ENTITY_VILLAGER_NO"
  
  # Animations (future feature)
  animations:
    enabled: false
    open-animation: "fade"
    close-animation: "slide"

# ============================
# Placeholder Configuration
# ============================

# Custom placeholders for this GUI
placeholders:
  # Job-specific placeholders
  job: "%jobsadventure_{job_id}_job_name%"
  level: "%jobsadventure_{job_id}_player_level%"
  xp: "%jobsadventure_{job_id}_player_xp%"
  required: "%jobsadventure_{job_id}_job_xp_required%"
  progress_percent: "%jobsadventure_{job_id}_player_progresspercent%"
  
  # GUI-specific placeholders
  page: "{current_page}"
  max_pages: "{total_pages}"
  total_rewards: "{total_reward_count}"
  available_rewards: "{available_reward_count}"
  claimed_rewards: "{claimed_reward_count}"
```

### GUI Layouts

#### Compact Layout (27 slots)
```yaml
# gui/compact_rewards.yml
title: "&8{job} Rewards"
size: 27

reward-slots: [10, 11, 12, 13, 14, 15, 16]

navigation:
  previous-page:
    slots: [18]
  next-page:
    slots: [26]
  close:
    slots: [22]
  info:
    slots: [4]
```

#### Large Layout (54 slots)
```yaml
# gui/large_rewards.yml
title: "&8{job} Rewards - {page}/{max_pages}"
size: 54

reward-slots: [
  9, 10, 11, 12, 13, 14, 15, 16, 17,
  18, 19, 20, 21, 22, 23, 24, 25, 26,
  27, 28, 29, 30, 31, 32, 33, 34, 35,
  36, 37, 38, 39, 40, 41, 42, 43, 44
]

# Full border design with navigation
```

---

## XP Curve Configuration

### Mathematical Curves

```yaml
# xp-curves/exponential.yml

# ============================
# Mathematical XP Curve
# ============================

name: "Exponential Curve"
description: "Exponential growth for challenging progression"

# Mathematical equation for XP calculation
equation: "100 * Math.pow(level, 2.5)"

# Alternative equations:
# Linear: "level * 1000"
# Quadratic: "Math.pow(level, 2) * 50"
# Logarithmic: "1000 * Math.log(level + 1) * level"
# Custom: "Math.floor(100 * Math.pow(1.2, level) + level * 50)"

# Validation settings
validation:
  # Test levels for validation
  test-levels: [1, 10, 25, 50, 75, 100]
  
  # Maximum allowed XP value
  max-xp: 999999999
  
  # Ensure positive values
  require-positive: true
```

### Level-Based Curves

```yaml
# xp-curves/custom_levels.yml

# ============================
# Level-Based XP Curve
# ============================

name: "Custom Level Curve"
description: "Manually defined XP requirements per level"

# Manual level definitions
levels:
  1: 0        # Level 1 requires 0 XP (starting level)
  2: 100      # Level 2 requires 100 total XP
  3: 250      # Level 3 requires 250 total XP
  4: 450      # Level 4 requires 450 total XP
  5: 700      # Level 5 requires 700 total XP
  6: 1000     # Level 6 requires 1000 total XP
  7: 1350     # And so on...
  8: 1750
  9: 2200
  10: 2700
  
  # Skip to higher levels
  15: 5500
  20: 10000
  25: 16000
  30: 24000
  35: 34000
  40: 46000
  45: 60000
  50: 76000
  
  # End game levels
  75: 150000
  100: 250000

# Interpolation for missing levels
interpolation:
  # Method: linear, exponential, logarithmic
  method: "linear"
  
  # Auto-generate missing levels
  auto-generate: true
```

### Specialized Curves

```yaml
# xp-curves/combat_optimized.yml

# ============================
# Combat-Optimized XP Curve
# ============================

name: "Combat Optimized"
description: "Balanced for PvP and PvE activities"

# Multi-phase equation
phases:
  # Early game (levels 1-20): Fast progression
  early:
    levels: "1-20"
    equation: "level * 500"
  
  # Mid game (levels 21-60): Moderate progression
  mid:
    levels: "21-60"
    equation: "10000 + (level - 20) * 1000"
  
  # End game (levels 61-100): Slow progression
  end:
    levels: "61-100"
    equation: "50000 + Math.pow(level - 60, 2) * 100"

# Bonus multipliers for specific level ranges
bonuses:
  # 25% XP bonus for levels 10-15 (learning phase)
  learning_boost:
    levels: "10-15"
    multiplier: 1.25
  
  # 50% XP penalty for levels 90+ (prestige)
  prestige_challenge:
    levels: "90-100"
    multiplier: 0.5
```

---

## Advanced Features

### Conditional Configuration

```yaml
# Dynamic configuration based on conditions
dynamic-config:
  # Server-specific settings
  server-settings:
    # Different curves for different server types
    survival:
      default-curve: "steep"
      max-level: 100
    
    creative:
      default-curve: "linear"
      max-level: 50
    
    skyblock:
      default-curve: "gentle"
      max-level: 200
  
  # Time-based modifications
  events:
    # Double XP weekends
    weekend_boost:
      condition: "%server_time_formatted_E% contains Sat|Sun"
      xp-multiplier: 2.0
      active-jobs: ["miner", "farmer"]
    
    # Holiday events
    christmas_event:
      condition: "%server_time_formatted_MM-dd% == 12-25"
      special-rewards: true
      bonus-curve: "christmas_special"
```

### Multi-Server Configuration

```yaml
# config/multi-server.yml

# ============================
# Multi-Server Configuration
# ============================

# Server synchronization
sync:
  enabled: true
  method: "database"  # database, file, redis
  
  # What to sync between servers
  sync-data:
    player-xp: true
    player-levels: true
    player-jobs: true
    rewards-claimed: true
    bonuses: false  # Keep bonuses server-specific
  
  # Server-specific overrides
  servers:
    survival:
      xp-multiplier: 1.0
      enabled-jobs: ["miner", "farmer", "hunter"]
    
    creative:
      xp-multiplier: 0.5
      enabled-jobs: ["builder", "artist"]
    
    skyblock:
      xp-multiplier: 1.5
      enabled-jobs: ["miner", "farmer"]
      special-curves: ["skyblock_mining", "skyblock_farming"]

# Database configuration for sync
database:
  type: "mysql"
  host: "localhost"
  port: 3306
  database: "jobsadventure_sync"
  username: "ja_user"
  password: "secure_password"
  
  # Connection settings
  connection-pool:
    minimum-idle: 2
    maximum-pool-size: 10
    connection-timeout: 30000
  
  # Table names
  tables:
    players: "ja_players"
    jobs: "ja_player_jobs"
    rewards: "ja_player_rewards"
    bonuses: "ja_player_bonuses"
```

### Advanced Placeholder Integration

```yaml
# Custom placeholder configurations
placeholders:
  # Custom formatting
  formats:
    xp: "%.1f"
    currency: "$%,.2f"
    time: "%H:%M:%S"
    date: "%Y-%m-%d"
  
  # Custom calculations
  calculations:
    # Average level across all jobs
    average-level: "sum(all_job_levels) / count(jobs)"
    
    # XP efficiency (XP per hour)
    xp-efficiency: "total_xp / (playtime_hours || 1)"
    
    # Progress to next milestone
    milestone-progress: "(current_level % 10) / 10 * 100"
  
  # Conditional placeholders
  conditional:
    # Show different messages based on level
    level-status:
      conditions:
        - condition: "level < 10"
          value: "&cNovice"
        - condition: "level < 25"
          value: "&eApprentice"
        - condition: "level < 50"
          value: "&aJourneyman"
        - condition: "level < 75"
          value: "&bExpert"
        - condition: "level < 100"
          value: "&5Master"
        - condition: "level >= 100"
          value: "&6Grandmaster"
```

---

## Best Practices

### Performance Optimization

1. **Use Appropriate Save Intervals**
```yaml
settings:
  save-interval: 300  # 5 minutes for active servers
  # save-interval: 600  # 10 minutes for smaller servers
```

2. **Limit Tracked Blocks**
```yaml
protection:
  block-tracking:
    max-tracked-blocks: 1000  # Per chunk
    cleanup-interval: 3600    # 1 hour
```

3. **Optimize Placeholder Caching**
```yaml
integrations:
  placeholderapi:
    cache-duration: 30          # 30 seconds for job data
    global-cache-duration: 60   # 1 minute for global data
```

### Configuration Organization

1. **Use Descriptive Names**
```yaml
# Good
name: "&6Master Miner"
rewards: "miner_endgame_rewards"

# Bad
name: "Job1"
rewards: "rewards1"
```

2. **Group Related Actions**
```yaml
actions:
  # Ores
  break:
    coal_ore: { target: "COAL_ORE", xp: 2.0 }
    iron_ore: { target: "IRON_ORE", xp: 4.0 }
    gold_ore: { target: "GOLD_ORE", xp: 6.0 }
    diamond_ore: { target: "DIAMOND_ORE", xp: 20.0 }
  
  # Stones
  break:
    stone: { target: "STONE", xp: 1.0 }
    cobblestone: { target: "COBBLESTONE", xp: 0.5 }
```

3. **Use Comments Extensively**
```yaml
# ============================
# MINING JOB CONFIGURATION
# Last updated: 2024-01-01
# ============================

# Experience curve: Exponential growth
# Levels 1-25: Fast progression for new players
# Levels 26-75: Moderate progression
# Levels 76-100: Slow progression for prestige
xp-curve: "mining_balanced"
```

### Security Considerations

1. **Validate User Input**
```yaml
# Use specific permissions
permission: "jobs.miner.join"

# Validate placeholder values
requirements:
  conditions:
    level-check:
      type: "placeholder"
      placeholder: "%jobsadventure_example_player_level%"
      operator: ">="
      value: "1"  # Always use string values for consistency
```

2. **Limit Resource Usage**
```yaml
# Prevent abuse
settings:
  max-jobs-per-player: 3
  
bonuses:
  max-bonuses-per-player: 5

protection:
  block-tracking:
    max-tracked-blocks: 1000
```

3. **Use Safe Commands**
```yaml
commands:
  # Good - specific commands
  - "give {player} diamond 1"
  - "jobsadventure xpbonus give {player} 1.5 1h reward"
  
  # Avoid - dangerous commands
  # - "op {player}"  # Never do this!
  # - "execute as {player} run ..."  # Be very careful
```

---

## Troubleshooting

### Common Configuration Errors

#### 1. YAML Syntax Errors
```yaml
# Error: Missing quotes
name: This is a name with spaces  # WRONG

# Correct: Use quotes for strings with spaces
name: "This is a name with spaces"  # CORRECT

# Error: Incorrect indentation
actions:
break:  # WRONG - should be indented
  stone:
    target: "STONE"

# Correct: Proper indentation
actions:
  break:  # CORRECT - properly indented
    stone:
      target: "STONE"
```

#### 2. Invalid Material Names
```yaml
# Error: Wrong material name
target: "DIAMOND_BLOCK_ORE"  # WRONG - doesn't exist

# Correct: Valid material name
target: "DIAMOND_ORE"  # CORRECT

# Tip: Check valid materials for your server version
```

#### 3. XP Curve Errors
```yaml
# Error: Invalid equation syntax
xp-equation: "level * Math.invalid(2)"  # WRONG

# Correct: Valid equation
xp-equation: "level * Math.pow(2, 1.5)"  # CORRECT

# Error: Missing level definitions
levels:
  2: 100  # WRONG - missing level 1
  3: 250

# Correct: Include all levels from 1
levels:
  1: 0    # CORRECT - start from level 1
  2: 100
  3: 250
```

### Validation Tools

Use these commands to validate your configuration:

```bash
# Check main config
/jobs reload

# Check specific job
/jobs debug on
/jobs info miner

# Check rewards
/jobs rewards list miner

# Check XP curves
/jobs info miner  # Shows XP curve status
```

### Debug Information

Enable debug mode for detailed logging:

```yaml
settings:
  debug: true

debug:
  log-xp-gains: true
  log-actions: true
  log-conditions: true
  log-performance: true
```

This will log detailed information about:
- XP calculations and awards
- Action processing steps
- Condition evaluation results
- Performance metrics

### Performance Monitoring

Monitor these metrics for optimal performance:

1. **Memory Usage**
   - Player data cache size
   - Tracked block count
   - Active bonus count

2. **Processing Time**
   - Action processing duration
   - Database operation time
   - Placeholder calculation time

3. **Event Frequency**
   - Actions processed per second
   - Level ups per hour
   - Reward claims per day

---

This configuration guide provides comprehensive coverage of all JobsAdventure configuration options. Use it as a reference when setting up and customizing your jobs system.