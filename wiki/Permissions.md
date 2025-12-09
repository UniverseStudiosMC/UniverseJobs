# Permissions

Complete list of all UniverseJobs permissions.

## Basic Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `universejobs.use` | Access to basic commands | true |
| `universejobs.menu` | Open jobs menu | true |
| `universejobs.list` | List available jobs | true |
| `universejobs.info` | View job information | true |
| `universejobs.stats` | View own stats | true |
| `universejobs.top` | View leaderboards | true |

## Job Permissions

| Permission | Description |
|------------|-------------|
| `universejobs.job.*` | Access to all jobs |
| `universejobs.job.<job_id>` | Access to specific job |
| `universejobs.job.<job_id>.join` | Can join specific job |
| `universejobs.job.<job_id>.leave` | Can leave specific job |

### Max Level Override

| Permission | Description |
|------------|-------------|
| `universejobs.job.<job_id>.maxlevel.<level>` | Override max level |
| `universejobs.job.<job_id>.maxlevel.*` | Unlimited level |

Example:
```
universejobs.job.miner.maxlevel.150  # Allow level 150 in miner
```

## Reward Permissions

| Permission | Description |
|------------|-------------|
| `universejobs.rewards.use` | Access rewards menu |
| `universejobs.rewards.claim` | Claim rewards |
| `universejobs.rewards.<job>.*` | All rewards for job |
| `universejobs.rewards.<job>.<reward_id>` | Specific reward |

## Admin Permissions

| Permission | Description |
|------------|-------------|
| `universejobs.admin` | All admin commands |
| `universejobs.admin.*` | Wildcard for all admin |

### XP Management

| Permission | Description |
|------------|-------------|
| `universejobs.admin.xp` | XP management commands |
| `universejobs.admin.xp.add` | Add XP to players |
| `universejobs.admin.xp.remove` | Remove XP from players |
| `universejobs.admin.xp.set` | Set player XP |

### Level Management

| Permission | Description |
|------------|-------------|
| `universejobs.admin.level` | Level management commands |
| `universejobs.admin.level.set` | Set player level |
| `universejobs.admin.level.add` | Add levels to player |

### Player Management

| Permission | Description |
|------------|-------------|
| `universejobs.admin.reset` | Reset player data |
| `universejobs.admin.forcejoin` | Force player to join job |
| `universejobs.admin.forceleave` | Force player to leave job |

### Boost Management

| Permission | Description |
|------------|-------------|
| `universejobs.admin.boost` | Boost management commands |
| `universejobs.admin.boost.give` | Give boosts |
| `universejobs.admin.boost.remove` | Remove boosts |
| `universejobs.admin.boost.list` | List player boosts |
| `universejobs.admin.boost.giveall` | Give boosts to all |

### Action Limits

| Permission | Description |
|------------|-------------|
| `universejobs.admin.actionlimits` | Action limit commands |
| `universejobs.admin.actionlimits.check` | Check player limits |
| `universejobs.admin.actionlimits.restore` | Restore player limits |
| `universejobs.admin.actionlimits.reset` | Reset all limits |

### Rewards Admin

| Permission | Description |
|------------|-------------|
| `universejobs.admin.rewards` | Rewards admin commands |
| `universejobs.admin.rewards.reset` | Reset claimed rewards |
| `universejobs.admin.rewards.give` | Force give rewards |

### Plugin Management

| Permission | Description |
|------------|-------------|
| `universejobs.admin.reload` | Reload configurations |
| `universejobs.admin.database` | Database commands |
| `universejobs.admin.migration` | Migration commands |

## Multiplier Permissions

Dynamic permissions for permanent multipliers:

### XP Multipliers

| Permission | Effect |
|------------|--------|
| `universejobs.multiplier.exp.1.25` | 1.25x XP |
| `universejobs.multiplier.exp.1.5` | 1.5x XP |
| `universejobs.multiplier.exp.2` | 2x XP |
| `universejobs.multiplier.exp.3` | 3x XP |

### Money Multipliers

| Permission | Effect |
|------------|--------|
| `universejobs.multiplier.money.1.25` | 1.25x Money |
| `universejobs.multiplier.money.1.5` | 1.5x Money |
| `universejobs.multiplier.money.2` | 2x Money |
| `universejobs.multiplier.money.3` | 3x Money |

## Bypass Permissions

| Permission | Description |
|------------|-------------|
| `universejobs.bypass.maxjobs` | Bypass max jobs limit |
| `universejobs.bypass.cooldown` | Bypass reward cooldowns |
| `universejobs.bypass.requirements` | Bypass action requirements |
| `universejobs.bypass.limits` | Bypass action limits |

## LuckPerms Examples

### Basic Player Setup

```bash
# Give basic access
/lp user Steve permission set universejobs.use

# Allow joining miner job
/lp user Steve permission set universejobs.job.miner
```

### VIP Ranks

```bash
# Bronze VIP
/lp group bronze permission set universejobs.multiplier.exp.1.25
/lp group bronze permission set universejobs.multiplier.money.1.25

# Silver VIP
/lp group silver permission set universejobs.multiplier.exp.1.5
/lp group silver permission set universejobs.multiplier.money.1.5
/lp group silver permission set universejobs.bypass.cooldown

# Gold VIP
/lp group gold permission set universejobs.multiplier.exp.2
/lp group gold permission set universejobs.multiplier.money.2
/lp group gold permission set universejobs.bypass.cooldown
/lp group gold permission set universejobs.bypass.maxjobs

# Platinum VIP
/lp group platinum permission set universejobs.multiplier.exp.3
/lp group platinum permission set universejobs.multiplier.money.3
/lp group platinum permission set universejobs.bypass.*
/lp group platinum permission set universejobs.job.*.maxlevel.*
```

### Staff Ranks

```bash
# Helper
/lp group helper permission set universejobs.admin.xp
/lp group helper permission set universejobs.admin.level

# Moderator
/lp group mod permission set universejobs.admin.reset
/lp group mod permission set universejobs.admin.boost
/lp group mod permission set universejobs.admin.actionlimits

# Admin
/lp group admin permission set universejobs.admin.*
```

### Job-Specific Access

```bash
# Premium jobs
/lp group premium permission set universejobs.job.enchanter
/lp group premium permission set universejobs.job.alchemist

# VIP-only jobs
/lp group vip permission set universejobs.job.explorer
```

## Permission Inheritance

Recommended inheritance structure:

```
default
  └── vip
       └── vip+
            └── vip++
                 └── staff
                      └── admin
```

```bash
# Set up inheritance
/lp group vip parent add default
/lp group vip+ parent add vip
/lp group vip++ parent add vip+
/lp group staff parent add vip++
/lp group admin parent add staff
```

## Condition Permissions

Use permissions in action conditions:

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 100
      money: 25
      requirements:
        permission: "universejobs.premium.mining"
```

## Default Permissions

Configure default permissions in your permission plugin:

```bash
# Give all players basic access
/lp group default permission set universejobs.use
/lp group default permission set universejobs.menu
/lp group default permission set universejobs.job.miner
/lp group default permission set universejobs.job.farmer
/lp group default permission set universejobs.job.hunter
```

## Negating Permissions

Remove specific permissions:

```bash
# Remove a specific job from a group
/lp group default permission set universejobs.job.explorer false

# Remove multiplier from specific user
/lp user Steve permission set universejobs.multiplier.exp.2 false
```

## Context-Based Permissions

With LuckPerms, use contexts for temporary permissions:

```bash
# 2x XP for 1 hour
/lp user Steve permission settemp universejobs.multiplier.exp.2 true 1h

# Weekend multiplier (server-side script)
/lp group default permission settemp universejobs.multiplier.exp.1.5 true 2d
```
