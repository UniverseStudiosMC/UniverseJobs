# Boosts & Multipliers

UniverseJobs provides a comprehensive boost system to multiply XP and money earnings.

## Boost Types

| Type | Description |
|------|-------------|
| XP Boost | Multiplies XP earnings |
| Money Boost | Multiplies money earnings |

## Boost Scope

| Scope | Description |
|-------|-------------|
| Global | Applies to all jobs |
| Job-Specific | Applies to a single job |
| Action-Specific | Applies to specific action types |

## Giving Boosts

### Via Commands

```bash
# Give XP boost to all jobs
/jobs bonus give <player> xp <multiplier> <duration_seconds>

# Give XP boost to specific job
/jobs bonus give <player> xp <multiplier> <duration_seconds> <job_id>

# Give Money boost
/jobs bonus give <player> money <multiplier> <duration_seconds>

# Examples
/jobs bonus give Steve xp 2.0 3600           # 2x XP for 1 hour (all jobs)
/jobs bonus give Steve money 1.5 7200 miner  # 1.5x money for 2 hours (miner only)
```

### Via Permissions

Players with these permissions get permanent multipliers:

```
universejobs.multiplier.exp.<amount>
universejobs.multiplier.money.<amount>
```

Examples:
- `universejobs.multiplier.exp.1.5` - 1.5x XP permanently
- `universejobs.multiplier.exp.2` - 2x XP permanently
- `universejobs.multiplier.money.1.25` - 1.25x money permanently
- `universejobs.multiplier.money.2` - 2x money permanently

These can be given via permission plugins like LuckPerms:

```bash
# Give permanent 1.5x XP boost
/lp user Steve permission set universejobs.multiplier.exp.1.5

# Give 2x money boost for VIP group
/lp group vip permission set universejobs.multiplier.money.2
```

## Calculation Modes

Configure how multiple boosts stack in `config.yml`:

```yaml
jobs:
  boost-calculation-mode: MULTIPLICATIVE
```

### ADDITIVE

Boosts are added together:

```
Base: 100 XP
Boost 1: 1.5x (+50%)
Boost 2: 1.5x (+50%)
Total multiplier: 1.0 + 0.5 + 0.5 = 2.0x
Result: 100 × 2.0 = 200 XP
```

### MULTIPLICATIVE (Default)

Boosts are multiplied together:

```
Base: 100 XP
Boost 1: 1.5x
Boost 2: 1.5x
Total multiplier: 1.5 × 1.5 = 2.25x
Result: 100 × 2.25 = 225 XP
```

### HIGHEST

Only the highest boost applies:

```
Base: 100 XP
Boost 1: 1.5x
Boost 2: 2.0x
Total multiplier: max(1.5, 2.0) = 2.0x
Result: 100 × 2.0 = 200 XP
```

## Boost Properties

When creating boosts programmatically or via commands:

| Property | Description |
|----------|-------------|
| `playerId` | Target player UUID |
| `jobId` | Target job (null = all jobs) |
| `multiplier` | Boost factor (e.g., 1.5 = +50%) |
| `duration` | Duration in seconds |
| `reason` | Reason for the boost |
| `grantedBy` | Who granted the boost |
| `boostId` | Unique identifier |
| `isGlobal` | Applies to all jobs |
| `actionType` | Specific action type (optional) |
| `actionId` | Specific action ID (optional) |

## Managing Boosts

### List Active Boosts

```bash
/jobs bonus list <player>
```

Shows all active boosts with:
- Boost ID
- Type (XP/Money)
- Multiplier value
- Time remaining
- Target job (or "Global")

### Remove Boosts

```bash
# Remove specific boost
/jobs bonus remove <player> <boost_id>

# Remove all boosts
/jobs bonus removeall <player>
```

## Boost GUI

Players can view their active boosts in a GUI:

```bash
/jobs boosts
```

Configure the boost GUI in `menus/boost-manager.yml`:

```yaml
title: "<gradient:#FFD700:#FFA500>Active Boosts</gradient>"
size: 27

fill-item:
  enabled: true
  material: BLACK_STAINED_GLASS_PANE
  display-name: " "

boost-item:
  xp:
    material: EXPERIENCE_BOTTLE
    display-name: "<green>XP Boost"
    lore:
      - "<gray>Multiplier: <white>{multiplier}x"
      - "<gray>Time left: <white>{time_remaining}"
      - "<gray>Job: <white>{job_name}"
  money:
    material: GOLD_INGOT
    display-name: "<gold>Money Boost"
    lore:
      - "<gray>Multiplier: <white>{multiplier}x"
      - "<gray>Time left: <white>{time_remaining}"
      - "<gray>Job: <white>{job_name}"

no-boosts:
  enabled: true
  material: BARRIER
  display-name: "<red>No Active Boosts"
  slot: 13
```

## Placeholders

Use these placeholders for boost information:

| Placeholder | Description |
|-------------|-------------|
| `%universejobs_boost_active%` | Has active boost (true/false) |
| `%universejobs_boost_xp_multiplier%` | Current XP multiplier |
| `%universejobs_boost_money_multiplier%` | Current money multiplier |
| `%universejobs_boost_time_remaining%` | Time until boost expires |
| `%universejobs_boost_<job>_xp_multiplier%` | XP multiplier for specific job |
| `%universejobs_boost_<job>_money_multiplier%` | Money multiplier for specific job |

## Event Boosts

Create server-wide boost events:

```bash
# Give everyone online 2x XP for 1 hour
/jobs bonus giveall xp 2.0 3600

# Give everyone 1.5x money for mining for 30 minutes
/jobs bonus giveall money 1.5 1800 miner
```

## Integration Examples

### LuckPerms VIP Ranks

```bash
# Bronze VIP: 1.25x XP
/lp group bronze permission set universejobs.multiplier.exp.1.25

# Silver VIP: 1.5x XP, 1.25x Money
/lp group silver permission set universejobs.multiplier.exp.1.5
/lp group silver permission set universejobs.multiplier.money.1.25

# Gold VIP: 2x XP, 1.5x Money
/lp group gold permission set universejobs.multiplier.exp.2
/lp group gold permission set universejobs.multiplier.money.1.5
```

### Weekend Events (via Plugin/Script)

```bash
# Friday 6 PM - Give 48h boost
/jobs bonus giveall xp 1.5 172800
/jobs bonus giveall money 1.5 172800
```

### Reward Command

In reward configuration:

```yaml
rewards:
  xp_boost:
    name: "XP Boost"
    required-level: 25
    commands:
      - "[console] jobs bonus give {player} xp 2.0 3600"
```

## Display in Action Messages

When a player has active boosts, the action messages show the multiplier:

```
+50 XP (x1.5) | +$10 (x1.5)
```

Configure the display format in `config.yml`:

```yaml
placeholders:
  action_xp_multiplier: "<gray>(<white>x{value}<gray>)"
  action_money_multiplier: "<gray>(<white>x{value}<gray>)"
```
