# Jobs

Jobs are defined in individual YAML files in the `plugins/UniverseJobs/jobs/` folder.

## Default Jobs

UniverseJobs includes 7 pre-configured jobs:

| Job | File | Description |
|-----|------|-------------|
| Miner | `miner.yml` | Mine ores and stones |
| Farmer | `farmer.yml` | Harvest crops and plant seeds |
| Hunter | `hunter.yml` | Kill mobs and animals |
| Lumberjack | `lumberjack.yml` | Cut trees and wood |
| Brewer | `brewer.yml` | Brew potions |
| Repairer | `repairer.yml` | Repair items on anvils |
| Explorer | `explorer.yml` | Explore new chunks |

## Job File Structure

```yaml
# jobs/miner.yml

# Basic Information
name: "Miner"                       # Display name
description: "Mine ores and stones" # Short description
description-lines:                  # Multi-line description for GUI
  - "<gray>Mine valuable ores"
  - "<gray>to earn money and XP!"
enabled: true                       # Enable/disable the job

# Progression
max-level: 100                      # Maximum level
permission: "universejobs.job.miner" # Required permission (optional)

# Display
icon:
  material: DIAMOND_PICKAXE
  display-name: "<gradient:#FFD700:#FFA500>Miner</gradient>"
  lore:
    - "<gray>Level: <white>{level}"
    - "<gray>XP: <white>{xp}/{xp_required}"
  custom-model-data: 0              # Optional CMD
  glow: false                       # Enchantment glow

# XP Configuration
xp:
  type: "CURVE"                     # "CURVE" or "EQUATION"
  xp: "default"                     # Curve file name or equation

# Rewards Configuration
rewards: "miner_rewards"            # Rewards file name
gui-reward: "miner_rewards_gui"     # Reward GUI file name

# Actions (see Actions page for details)
actions:
  break:
    - target: DIAMOND_ORE
      xp: 50
      money: 10
    - target: IRON_ORE
      xp: 20
      money: 5
```

## Job Properties

### Basic Properties

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Display name (supports MiniMessage) |
| `description` | String | Short description |
| `description-lines` | List | Multi-line description for GUIs |
| `enabled` | Boolean | Enable/disable the job |
| `max-level` | Integer | Maximum achievable level (default: 100) |
| `permission` | String | Permission required to join (optional) |

### Icon Properties

```yaml
icon:
  material: DIAMOND_PICKAXE         # Minecraft material
  display-name: "<gold>Job Name"    # Item display name
  lore:                             # Item lore
    - "Line 1"
    - "Line 2"
  custom-model-data: 0              # Custom model data for resource packs
  glow: false                       # Add enchantment glow
  amount: 1                         # Stack size
```

### XP Configuration

#### Using XP Curves (File-based)

```yaml
xp:
  type: "CURVE"
  xp: "default"                     # References xp-curves/default.yml
```

#### Using Equations

```yaml
xp:
  type: "EQUATION"
  xp: "100 * Math.pow({level}, 2)"  # Mathematical formula
```

Available variables in equations:
- `{level}` - Current level
- `{xp}` - Current XP

Available functions:
- `Math.pow(base, exponent)`
- `Math.sqrt(value)`
- `Math.floor(value)`
- `Math.ceil(value)`

### mcMMO Integration

```yaml
mcmmo:
  superbreaker:
    money-multiplier: 0.5           # Reduce money during ability
    xp-multiplier: 0.5              # Reduce XP during ability
  gigadrillbreaker:
    money-multiplier: 0.5
    xp-multiplier: 0.5
```

## Creating a Custom Job

1. Create a new file in `jobs/` folder (e.g., `fisherman.yml`)

2. Define the basic structure:

```yaml
name: "Fisherman"
description: "Catch fish and treasures"
description-lines:
  - "<gray>Cast your line and"
  - "<gray>reel in the rewards!"
enabled: true
max-level: 100

icon:
  material: FISHING_ROD
  display-name: "<aqua>Fisherman"
  lore:
    - "<gray>Level: <white>{level}"
    - ""
    - "<yellow>Click to view details"

xp:
  type: "EQUATION"
  xp: "50 + ({level} * 25)"

rewards: "fisherman_rewards"
gui-reward: "fisherman_rewards_gui"

actions:
  fish:
    - target: COD
      xp: 10
      money: 2
    - target: SALMON
      xp: 15
      money: 3
    - target: TROPICAL_FISH
      xp: 25
      money: 5
    - target: PUFFERFISH
      xp: 20
      money: 4
    - target: "*"                   # Wildcard for any fish
      xp: 5
      money: 1
```

3. Reload the plugin: `/jobs admin reload`

## Job Placeholders

Use these placeholders in job icon lore:

| Placeholder | Description |
|-------------|-------------|
| `{level}` | Current level |
| `{xp}` | Current XP |
| `{xp_required}` | XP needed for next level |
| `{progress}` | Progress bar |
| `{job_name}` | Job display name |
| `{job_id}` | Job ID |

## Multiple Jobs

Players can have multiple jobs simultaneously. Configure the limit in `config.yml`:

```yaml
jobs:
  max-jobs-per-player: 3
```

To give all jobs by default:

```yaml
jobs:
  all-jobs-by-default: true
```

Or specific jobs:

```yaml
jobs:
  jobs-by-default:
    - "miner"
    - "farmer"
```
