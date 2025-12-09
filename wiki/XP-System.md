# XP System

UniverseJobs features a flexible XP and leveling system with multiple configuration options.

## XP Sources

Players earn XP by performing job actions:

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 50                    # Fixed XP amount
      money: 10
```

## Leveling Methods

### Method 1: XP Curves (File-Based)

Define XP requirements per level in a YAML file.

```yaml
# In job file
xp:
  type: "CURVE"
  xp: "default"                 # References xp-curves/default.yml
```

```yaml
# xp-curves/default.yml
1: 100
2: 200
3: 350
4: 550
5: 800
6: 1100
7: 1450
8: 1850
9: 2300
10: 2800
# ... up to max level
100: 1000000
```

Each value represents **total cumulative XP** needed to reach that level.

### Method 2: Equations (Mathematical)

Use mathematical formulas for dynamic XP requirements.

```yaml
# In job file
xp:
  type: "EQUATION"
  xp: "100 * Math.pow({level}, 2)"
```

#### Available Variables

| Variable | Description |
|----------|-------------|
| `{level}` | Target level |
| `{xp}` | Current XP |

#### Available Functions

| Function | Description | Example |
|----------|-------------|---------|
| `Math.pow(x, y)` | Power | `Math.pow(2, 3)` = 8 |
| `Math.sqrt(x)` | Square root | `Math.sqrt(16)` = 4 |
| `Math.floor(x)` | Round down | `Math.floor(3.7)` = 3 |
| `Math.ceil(x)` | Round up | `Math.ceil(3.2)` = 4 |
| `Math.abs(x)` | Absolute value | `Math.abs(-5)` = 5 |
| `Math.min(x, y)` | Minimum | `Math.min(3, 5)` = 3 |
| `Math.max(x, y)` | Maximum | `Math.max(3, 5)` = 5 |

#### Example Equations

```yaml
# Linear growth
xp: "100 * {level}"
# Level 1: 100, Level 50: 5000, Level 100: 10000

# Quadratic growth (slower start, faster later)
xp: "50 * Math.pow({level}, 2)"
# Level 1: 50, Level 50: 125000, Level 100: 500000

# Exponential growth
xp: "100 * Math.pow(1.1, {level})"
# Level 1: 110, Level 50: 11739, Level 100: 1378061

# Custom formula with floor
xp: "Math.floor(100 * {level} + Math.pow({level}, 1.5))"
```

## XP in Actions

### Fixed XP

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 50
```

### Dynamic XP (Equations)

Use equations for scaling rewards:

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: "25 + ({level} * 0.5)"     # Increases with level
```

Available variables in action equations:
| Variable | Description |
|----------|-------------|
| `{level}` | Player's current level in job |
| `{xp}` | Player's current XP |

## XP Multipliers

### Boost System

Temporary multipliers via commands:

```bash
/jobs bonus give Steve xp 2.0 3600   # 2x XP for 1 hour
```

### Permission Multipliers

Permanent multipliers via permissions:

```
universejobs.multiplier.exp.1.5      # 1.5x XP
universejobs.multiplier.exp.2        # 2x XP
```

### Calculation Modes

Configure in `config.yml`:

```yaml
jobs:
  boost-calculation-mode: MULTIPLICATIVE
```

| Mode | Calculation | Example (1.5x + 1.5x) |
|------|-------------|----------------------|
| `ADDITIVE` | Sum all bonuses | 1.0 + 0.5 + 0.5 = 2.0x |
| `MULTIPLICATIVE` | Multiply all | 1.5 × 1.5 = 2.25x |
| `HIGHEST` | Take highest | max(1.5, 1.5) = 1.5x |

## Maximum Level

Set per job in job configuration:

```yaml
# jobs/miner.yml
max-level: 100
```

### Override via Permission

Allow specific players to exceed max level:

```
universejobs.job.miner.maxlevel.150  # Allow up to level 150
universejobs.job.miner.maxlevel.*    # Unlimited
```

## Level Up Events

### Commands on Level Up

Configure commands to run when players level up:

```yaml
# In job file
level-up:
  commands:
    - "[console] eco give {player} {level * 100}"
    - "[message] <gold>Congratulations! You reached level {level}!"

  # Specific level commands
  milestones:
    10:
      - "[console] give {player} diamond 5"
      - "[message] <gold>Level 10 milestone reached!"
    25:
      - "[console] lp user {player} permission set jobs.miner.apprentice"
    50:
      - "[console] give {player} netherite_pickaxe 1"
    100:
      - "[console] lp user {player} permission set jobs.miner.master"
      - "[broadcast] <gold>{player} has mastered the Miner job!"
```

### Messages

Configure level up messages in language files:

```yaml
# languages/en_US.yml
level-up:
  message: "<green>Level Up! You are now level <gold>{level}</gold> in <gold>{job_name}</gold>!"
  title:
    enabled: true
    title: "<gold>Level Up!"
    subtitle: "<gray>You are now level <white>{level}"
  sound:
    enabled: true
    sound: ENTITY_PLAYER_LEVELUP
    volume: 1.0
    pitch: 1.0
```

## XP Loss

### Leave Penalty

Lose XP when leaving a job:

```yaml
# config.yml
leave-penalty:
  enabled: true
  default:
    type: "percentage"          # "level", "xp", or "percentage"
    percentage: 0.1             # Lose 10% of XP
  jobs:
    miner:
      type: "xp"
      percentage: 0.15          # Miner loses 15%
```

### Inactivity Decay

Lose XP over time when inactive:

```yaml
# config.yml
inactivity-decay:
  enabled: true
  days-before-inactive: 30
  default:
    type: "percentage"
    amount: 0.01                # 1% per day
    min-level: 1                # Can't go below level 1
```

## Progress Bar

Customize the XP progress bar:

```yaml
# config.yml
progress-bar:
  length: 10
  characters:
    completed: "■"
    remaining: "□"
  colors:
    completed: "<#32CD32>"
    remaining: "<#404040>"
  format:
    with-percentage: "{completed_bar}{remaining_bar} {percentage}%"
```

Output: `■■■■■□□□□□ 50%`

## Creating Custom XP Curves

1. Create a new file in `xp-curves/`:

```yaml
# xp-curves/hard.yml
1: 500
2: 1500
3: 3500
4: 7000
5: 12000
# ... continue for all levels
```

2. Reference in job:

```yaml
xp:
  type: "CURVE"
  xp: "hard"
```

### Tips for Curve Design

| Progression | Description | Use Case |
|-------------|-------------|----------|
| Linear | Same XP increase each level | Simple, predictable |
| Quadratic | Increasing gaps between levels | Standard RPG feel |
| Exponential | Rapidly increasing requirements | Prestige systems |
| Logarithmic | Faster early, slower late | Casual-friendly |

### Example: Balanced Quadratic

```yaml
# xp-curves/balanced.yml
# Formula: 50 * level^1.8 (rounded)
1: 50
2: 174
3: 360
4: 601
5: 891
10: 3155
20: 10960
50: 55673
100: 199054
```

## Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%universejobs_<job>_level%` | Current level |
| `%universejobs_<job>_xp%` | Current XP |
| `%universejobs_<job>_xp_required%` | XP for next level |
| `%universejobs_<job>_xp_to_next%` | XP remaining |
| `%universejobs_<job>_progress%` | Progress bar |
| `%universejobs_<job>_progress_percent%` | Progress % |
