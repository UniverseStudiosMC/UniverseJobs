# Changelog v0.4.0

## ✨ **Major Features**

### 🎮 **GUI System for Boost Management**
- **Interactive GUI** for `/jobs admin boost info` replacing text display
- **Auto-refresh** functionality
- **Right-click** to remove boosts directly from GUI
- **Automatic sorting** by boost launch order
- **100% configurable** via `menus/boost-manager.yml`

### 🔢 **Bonus Calculation Mode System**
Three calculation modes available in `config.yml`:
1. **ADDITIVE**: `2.5x + 2.5x = 5.0x` (addition)
2. **MULTIPLICATIVE**: `2.5x × 2.5x = 6.25x` (multiplication - default)  
3. **HIGHEST**: `3.0x, 2.5x = 3.0x` (only the best boost)

### 🏆 **Complete Reward System Overhaul**
- **100% customizable reward GUIs** via YAML configuration
- **PlaceholderAPI integration** throughout reward system
- **Nexo/ItemsAdder integration** for custom items
- **Advanced reward conditions** with feedback messages and sounds
- **GUI positioning system** with configurable slots

### 🗄️ **Database Storage System**  
- **MySQL and SQLite support** with HikariCP connection pooling
- **Async database operations** to prevent main thread blocking
- **Database migration system** with admin commands
- **Leaderboard caching** with automatic invalidation

### 🔄 **Auto-Update System**
- **GitHub integration** for automatic update checking
- **Version comparison** with semantic versioning support

### 📊 **Enhanced Menu System**
- **Customizable job menus** with `menus/` configuration
- **Display name and lore support** for job actions
- **Menu configuration reload** via admin commands
- **Improved security** in menu click handling

## 🎮 **Reward GUI System**

### ⚙️ **Configuration**
```yaml
# gui/example_rewards_gui.yml
title: "<#FFD700><bold>{job} Rewards</bold>"
size: 54

reward-item:
  materials:
    retrievable: LIME_SHULKER_BOX
    blocked: RED_SHULKER_BOX
    retrieved: GRAY_SHULKER_BOX
  
  status-indicators:
    retrievable: "<#32CD32>✓"
    blocked: "<#FF6B6B>✗"
    retrieved: "<#808080>✓"
    
  lore-format:
    - "{description}"
    - "<gray>Required Level: <#FFD700>{level}"
    - "{reward_items}"
    - "{economy_reward}"
    - "{click_instruction}"
```

### 🎯 **Available Placeholders**
- `{name}`, `{description}`, `{level}`, `{status_description}`
- `{repeatable_info}`, `{cooldown}`, `{reward_items}`, `{economy_reward}`
- `{commands}`, `{click_instruction}`

## 🎯 **Boost Manager GUI**

### 📋 **User Interface**
- **Experience bottles** for XP boosts
- **Gold ingots** for Money boosts
- **Navigation items**: close, refresh, information
- **Configurable sound effects**

### ⚙️ **Configuration**
```yaml
# menus/boost-manager.yml
title: "<gold><b>Boost Manager</b></gold>"
size: 54

xp-boosts:
  slots: [10, 11, 12, 13, 14, 15, 16]
  item:
    material: EXPERIENCE_BOTTLE
    display-name: "<white>XP Boost <gold>{boost_id}</gold></white>"

money-boosts:
  slots: [28, 29, 30, 31, 32, 33, 34]  
  item:
    material: GOLD_INGOT
    display-name: "<white>Money Boost <gold>{boost_id}</gold></white>"

sounds:
  open:
    sound: ENTITY_EXPERIENCE_ORB_PICKUP
    volume: 0.5
  remove-boost:
    sound: ENTITY_EXPERIENCE_ORB_PICKUP
    volume: 0.7
```

### 🎨 **Available Variables**
- `{boost_id}`, `{player_name}`, `{job_info}`, `{action_info}`
- `{multiplier}`, `{remaining_time}`, `{xp_count}`, `{money_count}`, `{total_count}`

## 💰 **Enhanced Reward Configuration**

```yaml
# rewards/example_rewards.yml
rewards:
  milestone_100:
    name: "<gradient:#FFD700:#FFA500>Century Milestone</gradient>"
    required-level: 100
    gui-slot: 4
    
    requirements:
      logic: "AND"
      level_check:
        placeholder: "%universejobs_player_level%"
        operator: "greater_equal"
        value: "100"
      deny:
        message: "<#FF6B6B>❌ Requirements not met"
        sound: "BLOCK_ANVIL_LAND"
      accept:
        message: "<gradient:#32CD32:#FFD700>🎉 ACHIEVED! 🎉</gradient>"
        sound: "UI_TOAST_CHALLENGE_COMPLETE"
    
    items:
      nexo_item:
        nexo-id: "custom_hammer"
      itemsadder_item:
        itemsadder-id: "magic_sword"
    
    economy-reward: 10000
    commands:
      - "lp user %player_name% permission set jobs.master true"
```

## 🛠️ **Boost Calculation Configuration**

### 📝 **In config.yml**
```yaml
jobs:
  # Boost Calculation Mode
  # ADDITIVE: Multipliers are added together (2.5x + 2.5x = 5.0x)
  # MULTIPLICATIVE: Multipliers are multiplied (2.5x * 2.5x = 6.25x)
  # HIGHEST: Only the highest multiplier is used (3.0x, 2.5x = 3.0x)
  boost-calculation-mode: MULTIPLICATIVE
```

### 💡 **Calculation Examples**

#### ADDITIVE Mode
```
Boost 1: 1.5x (+0.5)
Boost 2: 2.0x (+1.0)
Total: 1.0 + 0.5 + 1.0 = 2.5x
```

#### MULTIPLICATIVE Mode (default)
```
Boost 1: 1.5x
Boost 2: 2.0x
Total: 1.5 × 2.0 = 3.0x
```

#### HIGHEST Mode
```
Boost 1: 1.5x
Boost 2: 2.0x ← Used
Total: 2.0x (highest)
```

### 🔑 **Permission-based Bonus Multipliers**
- **Hierarchical permission system** for bonus multipliers
- **Automatic calculation** based on player permissions
- **Configurable multiplier values** per permission level

## 🔧 **Database Configuration**

```yaml
# config.yml
database:
  enabled: true
  type: "mysql"  # or "sqlite"
  
  mysql:
    host: "localhost"
    database: "universejobs"
    username: "user"
    password: "password"
    
  connection-pool:
    maximum-pool-size: 10
    connection-timeout: 30000
```

## 🐛 **Bug Fixes**

### 🔒 **Security Fixes**
- **Eliminated all regex DoS vulnerabilities**
- **Enhanced input sanitization** for commands
- **NBT-based tracking** for improved data integrity

### 🔧 **Technical Fixes**
- **Fixed shutdown race conditions** preventing data saving
- **Resolved BossBar duplication** and freezing problems
- **Fixed placeholder processing** in reward commands
- **Corrected GUI navigation** and status display

### ⚡ **Performance Improvements**
- **Ultra-fast caching system** for job actions
- **Async XP message processing** to prevent lag
- **Optimized BossBar management** with synchronization
- **Enhanced connection pooling** for database operations

## 🌍 **Language Files**

Added missing reward messages in `languages/en_US.yml`:
- `rewards.claim.success/failed/already-claimed/requirements-not-met/cooldown`
- `rewards.gui.job-not-found/no-rewards/no-available-rewards`

## 🗂️ **New Files**

- `gui/example_rewards_gui.yml` - Customizable reward GUI template
- `rewards/example_rewards.yml` - Comprehensive reward examples
- Enhanced database configuration in `config.yml`

## 🚪 **Usage**

### 🎮 **For Players**
```bash
# View rewards GUI
/jobs rewards [job_name]

# Check job info
/jobs info
```

### 👑 **For Administrators**

#### Boost Management
```bash
# Open boost management GUI
/jobs admin boost info

# Give boosts
/jobs admin boost give xp * * * 2.0 3600
/jobs admin boost give money Player123 miner * 1.5 1800

# Remove boosts via command
/jobs admin boost remove boost_id_001
# Or right-click in GUI to remove
```

#### Database Management
```bash
# Migrate to database
/jobs admin database migrate
/jobs admin database status
/jobs admin database backup
```

#### Configuration
```bash
# Reload configurations  
/jobs admin reload
/jobs admin reload menus
```

### Job Configuration
```yaml
# jobs/example.yml
rewards: "example_rewards"
gui-reward: "example_rewards_gui"

# config.yml
jobs:
  boost-calculation-mode: MULTIPLICATIVE
```

## 🔒 **Permissions**

| Permission | Description |
|------------|-------------|
| `universejobs.admin.boost` | Full access to boost system (GUI + commands) |
| `universejobs.admin.boost.*` | Access to all admin boost features |
| `universejobs.rewards.view` | View reward GUIs |
| `universejobs.rewards.claim` | Claim rewards |
| `universejobs.admin.database` | Database management commands |
| `universejobs.admin.reload` | Reload configurations |
| `universejobs.bonus.multiplier.*` | Bonus multiplier permissions (hierarchical) |

## Thank you for using UniverseJobs!