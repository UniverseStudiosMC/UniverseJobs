# Commands

All commands use the base `/jobs` (configurable in `config.yml`).

## Player Commands

### General

| Command | Description | Permission |
|---------|-------------|------------|
| `/jobs` | Open main jobs menu | `universejobs.use` |
| `/jobs menu` | Open main jobs menu | `universejobs.use` |
| `/jobs list` | List all available jobs | `universejobs.use` |
| `/jobs help` | Show help message | `universejobs.use` |

### Job Management

| Command | Description | Permission |
|---------|-------------|------------|
| `/jobs join <job>` | Join a job | `universejobs.use` |
| `/jobs leave <job>` | Leave a job | `universejobs.use` |
| `/jobs info <job>` | Show job details | `universejobs.use` |

### Stats & Progress

| Command | Description | Permission |
|---------|-------------|------------|
| `/jobs stats` | Show your stats for all jobs | `universejobs.use` |
| `/jobs stats <job>` | Show stats for specific job | `universejobs.use` |
| `/jobs top` | Show global leaderboard | `universejobs.use` |
| `/jobs top <job>` | Show job-specific leaderboard | `universejobs.use` |

### Rewards & Boosts

| Command | Description | Permission |
|---------|-------------|------------|
| `/jobs rewards` | Open rewards menu | `universejobs.rewards.use` |
| `/jobs rewards <job>` | Open rewards for specific job | `universejobs.rewards.use` |
| `/jobs claim <reward_id>` | Claim a reward | `universejobs.rewards.use` |
| `/jobs boosts` | View active boosts | `universejobs.use` |

## Admin Commands

All admin commands require `universejobs.admin` permission.

### XP & Level Management

| Command | Description |
|---------|-------------|
| `/jobs admin addxp <player> <job> <amount>` | Add XP to player |
| `/jobs admin removexp <player> <job> <amount>` | Remove XP from player |
| `/jobs admin setxp <player> <job> <amount>` | Set player's XP |
| `/jobs admin setlevel <player> <job> <level>` | Set player's level |
| `/jobs admin addlevel <player> <job> <amount>` | Add levels to player |

### Player Data

| Command | Description |
|---------|-------------|
| `/jobs admin reset <player>` | Reset all job data for player |
| `/jobs admin reset <player> <job>` | Reset specific job data |
| `/jobs admin forcejoin <player> <job>` | Force player to join job |
| `/jobs admin forceleave <player> <job>` | Force player to leave job |

### Boost Management

| Command | Description |
|---------|-------------|
| `/jobs bonus give <player> <type> <multiplier> <duration> [job]` | Give boost |
| `/jobs bonus remove <player> <boost_id>` | Remove specific boost |
| `/jobs bonus removeall <player>` | Remove all boosts |
| `/jobs bonus list <player>` | List player's boosts |
| `/jobs bonus giveall <type> <multiplier> <duration> [job]` | Give boost to all online |

### Action Limits

| Command | Description |
|---------|-------------|
| `/jobs admin actionlimit check <player> <job> <action>` | Check action limit |
| `/jobs admin actionlimit restore <player> <job> <action>` | Restore action limit |
| `/jobs admin actionlimit resetall <player>` | Reset all limits |

### Rewards Admin

| Command | Description |
|---------|-------------|
| `/jobs admin rewards reset <player> <job>` | Reset claimed rewards |
| `/jobs admin rewards give <player> <job> <reward_id>` | Force give reward |

### Plugin Management

| Command | Description |
|---------|-------------|
| `/jobs admin reload` | Reload all configurations |
| `/jobs admin reload jobs` | Reload jobs only |
| `/jobs admin reload config` | Reload main config only |
| `/jobs admin reload menus` | Reload menus only |
| `/jobs admin reload lang` | Reload language files |

### Database

| Command | Description |
|---------|-------------|
| `/jobs admin database info` | Show database info |
| `/jobs admin database migrate` | Migrate data between storage types |
| `/jobs admin database backup` | Create database backup |

### Migration

| Command | Description |
|---------|-------------|
| `/jobs admin migration start` | Start JobsReborn migration |
| `/jobs admin migration status` | Check migration status |

## Command Examples

### Basic Usage

```bash
# Join the miner job
/jobs join miner

# Check your mining stats
/jobs stats miner

# View mining leaderboard
/jobs top miner

# Open rewards menu
/jobs rewards miner
```

### Admin: Managing Players

```bash
# Give player 500 XP in miner job
/jobs admin addxp Steve miner 500

# Set player to level 50
/jobs admin setlevel Steve miner 50

# Reset player's miner job
/jobs admin reset Steve miner

# Force player to join job (bypasses max jobs limit)
/jobs admin forcejoin Steve hunter
```

### Admin: Boosts

```bash
# Give 2x XP boost for 1 hour to all jobs
/jobs bonus give Steve xp 2.0 3600

# Give 1.5x money boost for 2 hours to miner job only
/jobs bonus give Steve money 1.5 7200 miner

# Give server-wide 2x XP weekend event (48 hours)
/jobs bonus giveall xp 2.0 172800

# List Steve's active boosts
/jobs bonus list Steve

# Remove a specific boost
/jobs bonus remove Steve boost_abc123
```

### Admin: Action Limits

```bash
# Check if player hit diamond mining limit
/jobs admin actionlimit check Steve miner BREAK:DIAMOND_ORE

# Restore player's action limit
/jobs admin actionlimit restore Steve miner BREAK:DIAMOND_ORE
```

### Admin: Rewards

```bash
# Reset all claimed rewards for miner job
/jobs admin rewards reset Steve miner

# Force give a specific reward
/jobs admin rewards give Steve miner diamond_kit
```

## Command Aliases

The main command can be changed in `config.yml`:

```yaml
main-command: "jobs"
```

Common aliases that work by default:
- `/jobs`
- `/job`
- `/uj`
- `/universejobs`

## Tab Completion

All commands support tab completion:
- Player names auto-complete
- Job IDs auto-complete
- Action types auto-complete
- Reward IDs auto-complete

## Console Commands

All admin commands can be run from console:

```bash
# From console
jobs admin addxp Steve miner 1000
jobs bonus giveall xp 2.0 3600
jobs admin reload
```

## Command Permissions Summary

| Permission | Description |
|------------|-------------|
| `universejobs.use` | Basic commands |
| `universejobs.admin` | All admin commands |
| `universejobs.admin.xp` | XP management |
| `universejobs.admin.level` | Level management |
| `universejobs.admin.reset` | Reset player data |
| `universejobs.admin.boost` | Boost management |
| `universejobs.admin.actionlimits` | Action limit management |
| `universejobs.admin.reload` | Reload configurations |
| `universejobs.rewards.use` | Use rewards |
| `universejobs.rewards.admin` | Admin rewards |

See [Permissions](Permissions) page for complete permission list.
