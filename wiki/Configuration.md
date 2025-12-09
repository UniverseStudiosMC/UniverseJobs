# Configuration

Main configuration file: `plugins/UniverseJobs/config.yml`

## General Settings

```yaml
debug: false                    # Enable debug messages
language:
  locale: en_US                 # Language file (en_US, fr_FR, etc.)
main-command: "jobs"            # Main command alias
create-example-jobs: true       # Create example jobs on first run
```

## Database Configuration

```yaml
database:
  type: "sqlite"                # "sqlite" or "mysql"

  mysql:
    host: "localhost"
    port: 3306
    database: "universejobs"
    username: "user"
    password: "password"

  prefix: "universejobs_"       # Table prefix

  pool:
    min-connections: 2
    max-connections: 10
    connection-timeout-ms: 30000
```

## Jobs Settings

```yaml
jobs:
  all-jobs-by-default: false    # Players have all jobs by default
  jobs-by-default: []           # Jobs given to new players
  max-jobs-per-player: 3        # Maximum simultaneous jobs
  boost-calculation-mode: MULTIPLICATIVE  # ADDITIVE, MULTIPLICATIVE, or HIGHEST
```

### Boost Calculation Modes

| Mode | Description | Example |
|------|-------------|---------|
| `ADDITIVE` | Adds multipliers together | 1.5x + 1.5x = 3.0x |
| `MULTIPLICATIVE` | Multiplies multipliers | 1.5x × 1.5x = 2.25x |
| `HIGHEST` | Takes the highest multiplier | max(1.5x, 2.0x) = 2.0x |

## Leave Penalty

Penalize players when they leave a job:

```yaml
leave-penalty:
  enabled: false
  default:
    type: "level"               # "level", "xp", or "percentage"
    percentage: 0.1             # 10% loss
  jobs:
    miner:
      type: "xp"
      percentage: 0.15          # 15% XP loss for miner
```

### Penalty Types

| Type | Description |
|------|-------------|
| `level` | Lose X levels |
| `xp` | Lose X XP |
| `percentage` | Lose X% of current XP |

## Inactivity Decay

Reduce XP/levels for inactive players:

```yaml
inactivity-decay:
  enabled: false
  days-before-inactive: 30      # Days before decay starts
  days-before-removal: 90       # Days before job removal

  default:
    type: "percentage"          # "level", "xp", or "percentage"
    amount: 0.01                # 1% per day
    min-level: 1                # Cannot go below this level

  jobs:
    miner:
      type: "percentage"
      amount: 0.015             # 1.5% per day for miner
      min-level: 1
```

## Block Protection

Prevent XP farming from placed blocks:

```yaml
block-protection:
  enabled: true
  blacklist:                    # Blocks that always give XP (crops, etc.)
    - "WHEAT"
    - "CARROTS"
    - "POTATOES"
    - "BEETROOTS"
    - "NETHER_WART"
    - "COCOA"
    - "MELON_STEM"
    - "PUMPKIN_STEM"
    - "SWEET_BERRY_BUSH"
    - "CAVE_VINES"
    - "CAVE_VINES_PLANT"
    - "TALL_GRASS"
    - "GRASS"
    - "FERN"
    - "LARGE_FERN"
```

## Progress Bar

Customize the XP progress bar:

```yaml
progress-bar:
  length: 10                    # Number of characters
  characters:
    completed: "■"
    remaining: "□"
  colors:
    completed: "<#32CD32>"      # Green
    remaining: "<#404040>"      # Gray
  format:
    with-percentage: "{completed_bar}{remaining_bar} {percentage}%"
    without-percentage: "{completed_bar}{remaining_bar}"
```

## Performance Settings

```yaml
performance:
  batching-xp: 40               # XP message batch interval (ticks)
  batching-money: 60            # Money message batch interval (ticks)
  batching-others: 40           # Other messages batch interval (ticks)
```

## Usage Multiplier

Dynamic multiplier based on job popularity:

```yaml
usage-multiplier:
  enabled: false
  default:
    min: 0.5                    # Minimum multiplier (popular jobs)
    max: 2.0                    # Maximum multiplier (unpopular jobs)
  jobs:
    miner:
      min: 0.3
      max: 2.5
```

## Placeholders Format

Customize placeholder output formats:

```yaml
placeholders:
  action_type:
    default: "<white>{value}"
    BREAK: "<red>{value}"
    PLACE: "<green>{value}"
    KILL: "<dark_red>{value}"
    HARVEST: "<yellow>{value}"
  action_target: "<gold>{value}"
  action_xp: "<green>{value}"
  action_money: "<gold>{value}"
  action_xp_multiplier: "<gray>(<white>x{value}<gray>)"
  action_money_multiplier: "<gray>(<white>x{value}<gray>)"
  action_lore: "<gray>{value}"
```

## mcMMO Integration

```yaml
mcmmo:
  superbreaker:
    money-multiplier: 0.5       # 50% money during Super Breaker
    xp-multiplier: 0.5          # 50% XP during Super Breaker
  gigadrillbreaker:
    money-multiplier: 0.5
    xp-multiplier: 0.5
```
