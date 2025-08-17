# ⭐ Main Features

JobsAdventure offers a comprehensive array of advanced features to create the perfect jobs experience on your Minecraft server.

## 🏢 Advanced Job System

### Supported Action Types (15+)
JobsAdventure recognizes and rewards a wide variety of actions:

| Action Type | Description | Examples |
|:---|:---|:---|
| **BREAK** | Breaking blocks | Mining, deforestation, excavation |
| **PLACE** | Placing blocks | Construction, farming |
| **KILL** | Killing entities | Combat, hunting, cleanup |
| **FISH** | Fishing | Normal fishing and CustomFishing |
| **INTERACT** | Interacting with objects | Harvesting, crafting |
| **CRAFT** | Crafting items | Crafting, forging |
| **SMELT** | Smelting items | Metallurgy, cooking |
| **BREW** | Brewing potions | Alchemy |
| **ENCHANT** | Enchanting items | Magic |
| **TAME** | Taming animals | Animal training |
| **SHEAR** | Shearing animals | Livestock |
| **MILK** | Milking animals | Dairy production |
| **CUSTOM** | Custom actions | Via API |
| **MYTHICMOB** | MythicMobs creatures | `MYTHICMOB:CreatureName` |
| **PLUGIN** | Plugin integrations | CustomCrops, Nexo, etc. |

### Flexible Experience Curves
Two options for defining progression:

#### 1. Custom Mathematical Formulas
```yaml
xp-equation: "100 * Math.pow(level, 1.8)"
```
Supported functions:
- `Math.pow(level, 2)` - Exponential progression
- `Math.sqrt(level * 500)` - Square root progression
- `Math.log(level + 1)` - Logarithmic progression
- Complex combinations possible

#### 2. Predefined Curve Files
```yaml
xp-curve: "steep"  # Uses /xp-curves/steep.yml
```

### Customizable XP Messages
Three display modes for XP gains:

#### Classic Chat
```yaml
xp-message:
  type: "chat"
  text: "&e+{exp} XP {job} &7(Total: {total_xp})"
```

#### Action Bar
```yaml
xp-message:
  type: "actionbar"
  text: "&6+{exp} XP &7| &e{job} Level {level}"
  duration: 60  # ticks (3 seconds)
```

#### Boss Bar
```yaml
xp-message:
  type: "bossbar"
  text: "+{exp} EXP - {job}"
  color: "green"
  style: "segmented_10"
  duration: 80
  show-progress: true  # Shows level progress
```

## 🎁 Smart Reward System

### Multiple Reward Types

#### Item Rewards
```yaml
items:
  tool:
    material: DIAMOND_PICKAXE
    amount: 1
    display-name: "&bMaster Miner's Pickaxe"
    enchantments:
      efficiency: 3
      unbreaking: 2
    nbt: '{Custom: 1b}'  # Custom NBT
```

#### Economic Rewards
```yaml
economy-reward: 500.0  # Via Vault
```

#### Command Rewards
```yaml
commands:
  - "give {player} diamond 5"
  - "broadcast {player} reached level 50!"
  - "tellraw {player} {\"text\":\"Congratulations!\", \"color\":\"gold\"}"
```

### Advanced Conditions System
Complex AND/OR logic for requirements:

```yaml
requirements:
  logic: "AND"
  permission:
    permission: "vip.level1"
    require: true
  placeholder:
    placeholder: "%player_level%"
    operator: "greater_than"
    value: "25"
  item:
    material: "IRON_PICKAXE"
    accept:
      message: "&aReward claimed!"
      sound: "ENTITY_PLAYER_LEVELUP"
    deny:
      message: "&cYou need an iron pickaxe!"
      sound: "ENTITY_VILLAGER_NO"
```

### Interactive GUI
- **Page navigation** for many rewards
- **Real-time preview** of requirements
- **Visual feedback** for available/claimed rewards
- **Cooldown display** with remaining time

### Flexible Cooldown System
```yaml
cooldown-hours: 24        # 24-hour cooldown
cooldown-days: 7          # 7-day cooldown
repeatable: true          # Can be claimed multiple times
```

## 🔗 Plugin Integrations

### PlaceholderAPI (60+ placeholders)

#### Player Information
```
%jobsadventure_miner_player_level%       # Player's mining level
%jobsadventure_farmer_player_xp%         # Player's farming XP
%jobsglobal_player_totaljobs%            # Total number of jobs
%jobsglobal_player_totallevels%          # Sum of all levels
%jobsglobal_player_rank%                 # Player's global rank
```

#### Leaderboards
```
%jobsadventure_miner_leaderboard_1_name%    # Top 1 miner name
%jobsadventure_miner_leaderboard_1_level%   # Top 1 miner level
%jobsglobal_totalxp_1_displayname%          # Global XP top 1
%jobsglobal_totallevels_5_name%             # 5th player by levels
```

### MythicMobs
Complete integration for custom creatures:
```yaml
actions:
  KILL:
    dragon_boss:
      target: "MYTHICMOB:AncientDragon"
      xp: 1000.0
      name: "Dragon Slaying"
      requirements:
        permission:
          permission: "jobs.boss.dragon"
```

### CustomCrops
Full support for advanced farming:
```yaml
actions:
  BREAK:
    pineapple_harvest:
      target: "customcrops:pineapple_stage_3"
      xp: 15.0
  PLACE:
    pineapple_plant:
      target: "customcrops:pineapple_seed"
      xp: 2.0
```

### CustomFishing
Custom fishing with rare creatures:
```yaml
actions:
  FISH:
    legendary_fish:
      target: "customfishing:golden_salmon"
      xp: 50.0
      requirements:
        time:
          min: 13000  # Night only
          max: 23000
```

### Nexo & ItemsAdder
Custom items and blocks:
```yaml
actions:
  BREAK:
    custom_ore:
      target: "nexo:mythril_ore"
      xp: 25.0
  PLACE:
    custom_block:
      target: "itemsadder:decorative_stone"
      xp: 5.0
```

### MMOItems
Specialized tools and weapons:
```yaml
requirements:
  item:
    mmoitems:
      type: "TOOL"
      id: "MASTER_PICKAXE"
    deny:
      message: "&cYou need the Master Pickaxe!"
```

## 🛡️ Advanced Anti-Cheat System

### NBT Block Protection
- **Automatic marking** of player-placed blocks
- **Smart distinction** between natural and artificial blocks
- **Automatic cleanup** of obsolete NBT data
- **Optimized performance** with no gameplay impact

### Exploit Detection
- **Adaptive cooldowns** to prevent spam
- **Suspicious pattern detection**
- **Strict validation** of all actions
- **Detailed logs** for auditing

### Validation System
```yaml
anti-exploit:
  block-tracking: true
  cooldown-ms: 100        # Cooldown between actions
  max-actions-per-second: 10
  suspicious-threshold: 50
```

## ⚡ Performance and Compatibility

### Folia Compatibility
- **Regionalized threading** for maximum performance
- **Asynchronous operations** to avoid lag
- **Smart synchronization** between regions
- **Horizontal scaling** for large servers

### Advanced Optimizations
- **Multi-level cache** for fast access
- **Data compression** to save space
- **Connection pooling** for database
- **Batch operations** to reduce I/O

### Real-time Monitoring
```
Performance Metrics:
⚡ Action Processing:    < 1ms average
💾 Memory Usage:        < 50MB for 1000 players
🔄 Database Queries:    Batched & async
🧵 Thread Safety:       100% concurrent-safe
📈 Scalability:         Tested up to 5000 players
```

## 🎨 Complete Customization

### Multi-language Messages
Full support for:
- **French** (fr_FR) - Complete translation
- **English** (en_US) - Default language
- **Extensible** - Easy addition of new languages

### GUI Interface
```yaml
gui:
  title: "&6{player}'s Jobs"
  size: 54  # 6-row inventory
  items:
    job_item:
      material: "DIAMOND_PICKAXE"
      display-name: "&e{job_name}"
      lore:
        - "&7Level: &a{level}"
        - "&7XP: &b{current_xp}/{required_xp}"
```

### Sounds and Effects
```yaml
sounds:
  level-up: "ENTITY_PLAYER_LEVELUP"
  xp-gain: "ENTITY_EXPERIENCE_ORB_PICKUP"
  reward-claim: "ENTITY_VILLAGER_YES"
effects:
  level-up:
    - "FIREWORK"
    - "PARTICLE:FLAME:50"
```

## 🔧 Developer API

### Complete Events
```java
// Available events
PlayerJobJoinEvent       - Joining a job
PlayerJobLeaveEvent      - Leaving a job
PlayerXpGainEvent        - XP gain (cancellable)
PlayerLevelUpEvent       - Level advancement
PlayerRewardClaimEvent   - Reward claiming
JobActionEvent           - Job action performed
```

### Easy Integration
```java
// API usage example
JobManager jobManager = JobsAdventure.getInstance().getJobManager();
Player player = Bukkit.getPlayer("Steve");

// Add XP
jobManager.addXp(player, "miner", 100.0);

// Check level
int level = jobManager.getLevel(player, "miner");

// Force level up
jobManager.setLevel(player, "miner", 50);
```

### Custom Extensions
- **Custom actions** via API
- **Custom conditions** for rewards
- **Third-party integrations** simplified
- **Data hooks** for synchronization

## 📊 Data System

### Hybrid Storage
**YAML Files** (default):
- Simple installation
- Ideal for small/medium servers
- Easy backup

**MySQL Database** (optional):
- Maximum performance
- Ideal for large servers/networks
- Cross-server synchronization

### Smart Compression
- **Automatic compression** of large files
- **Configurable threshold** for activation
- **Significant space savings**
- **Preserved performance**

### Advanced Cache
```yaml
cache:
  max-entries: 1000      # Maximum entries
  max-memory-mb: 256     # Maximum memory
  cleanup-interval: 30   # Cleanup in minutes
```

## 🔗 See Also

- [Compatibility and Requirements](compatibility.md)
- [Quick Installation](../installation/quick-start.md)
- [Job Configuration](../jobs-configuration/creating-jobs.md)
- [Developer API](../api/introduction.md)