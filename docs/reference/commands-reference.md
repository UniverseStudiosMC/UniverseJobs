# 📋 Complete Commands Reference

This page contains all available commands in JobsAdventure.

## 🎮 Player Commands

### Main Commands

#### `/jobs` or `/job`
**Description**: Main plugin command  
**Permission**: `jobsadventure.use` (default)  
**Usage**: `/jobs <subcommand>`

#### `/jobs help`
**Description**: Display command help  
**Permission**: `jobsadventure.use`  
**Example**:
```
/jobs help
```

### Job Management

#### `/jobs list`
**Description**: Display all available jobs  
**Permission**: `jobsadventure.use`  
**Example**:
```
/jobs list
```
**Output**:
```
=== Available Jobs ===
✓ Miner - Underground resource extraction
✗ Farmer - Agriculture and livestock
✗ Hunter - Combat and survival
```

#### `/jobs join <job>`
**Description**: Join a job  
**Permission**: `jobsadventure.use` + `jobsadventure.job.<job>`  
**Examples**:
```
/jobs join miner
/jobs join farmer
/jobs join hunter
```

#### `/jobs leave <job>`
**Description**: Leave a job  
**Permission**: `jobsadventure.use`  
**Examples**:
```
/jobs leave miner
/jobs leave farmer
```

### Information and Statistics

#### `/jobs info [job|player]`
**Description**: Display detailed information  
**Permission**: `jobsadventure.use`  
**Examples**:
```
/jobs info miner          # Info about Miner job
/jobs info PlayerName      # Info about a player
/jobs info                 # Info about your jobs
```

#### `/jobs stats [player]`
**Description**: Display job statistics  
**Permission**: `jobsadventure.use`  
**Examples**:
```
/jobs stats               # Your statistics
/jobs stats PlayerName    # Another player's stats
```

#### `/jobs top <job>`
**Description**: Display job leaderboard  
**Permission**: `jobsadventure.use`  
**Examples**:
```
/jobs top miner
/jobs top farmer
```

### Reward System

#### `/jobs rewards`
**Description**: Reward system commands  
**Permission**: `jobsadventure.rewards.use`  
**Subcommands**:

##### `/jobs rewards list`
**Description**: List jobs with rewards
```
/jobs rewards list
```

##### `/jobs rewards open <job>`
**Description**: Open job rewards interface
```
/jobs rewards open miner
/jobs rewards open farmer
```

##### `/jobs rewards claim <job> <reward>`
**Description**: Claim a specific reward
```
/jobs rewards claim miner starter_bonus
/jobs rewards claim farmer daily_bonus
```

##### `/jobs rewards info <job> <reward>`
**Description**: Display reward details
```
/jobs rewards info miner tool_upgrade
```

## ⚔️ Administration Commands

### General Management

#### `/jobs reload`
**Description**: Reload all configurations  
**Permission**: `jobsadventure.admin`  
**Example**:
```
/jobs reload
```

#### `/jobs debug [on|off]`
**Description**: Enable/disable debug mode  
**Permission**: `jobsadventure.admin`  
**Examples**:
```
/jobs debug on
/jobs debug off
```

### Player Management

#### `/jobs admin player <player> join <job>`
**Description**: Force a player to join a job  
**Permission**: `jobsadventure.admin`  
**Example**:
```
/jobs admin player Steve join miner
```

#### `/jobs admin player <player> leave <job>`
**Description**: Force a player to leave a job  
**Permission**: `jobsadventure.admin`  
**Example**:
```
/jobs admin player Steve leave miner
```

#### `/jobs admin player <player> setlevel <job> <level>`
**Description**: Set a player's level in a job  
**Permission**: `jobsadventure.admin`  
**Example**:
```
/jobs admin player Steve setlevel miner 50
```

#### `/jobs admin player <player> addxp <job> <xp>`
**Description**: Add XP to a player  
**Permission**: `jobsadventure.admin`  
**Example**:
```
/jobs admin player Steve addxp miner 1000
```

#### `/jobs admin player <player> reset [job]`
**Description**: Reset a player's data  
**Permission**: `jobsadventure.admin`  
**Examples**:
```
/jobs admin player Steve reset          # All jobs
/jobs admin player Steve reset miner    # Specific job
```

### XP Bonus System

#### `/jobs xpbonus <multiplier> <duration>`
**Description**: Give global XP bonus  
**Permission**: `jobsadventure.admin.xpbonus`  
**Parameters**:
- `multiplier`: 0.1 to 10.0 (e.g., 2.0 = +100%)
- `duration`: in seconds (max 86400 = 24h)

**Examples**:
```
/jobs xpbonus 2.0 3600        # +100% XP for 1 hour
/jobs xpbonus 1.5 1800        # +50% XP for 30 minutes
```

#### `/jobs xpbonus <player> <multiplier> <duration>`
**Description**: Give XP bonus to a player  
**Permission**: `jobsadventure.admin.xpbonus`  
**Example**:
```
/jobs xpbonus Steve 3.0 600   # +200% XP for Steve for 10 min
```

#### `/jobs xpbonus <player> <job> <multiplier> <duration>`
**Description**: Give job-specific XP bonus  
**Permission**: `jobsadventure.admin.xpbonus`  
**Example**:
```
/jobs xpbonus Steve miner 2.5 1200  # +150% mining XP for Steve for 20 min
```

### Reward Management

#### `/jobs admin rewards give <player> <job> <reward>`
**Description**: Give a reward to a player  
**Permission**: `jobsadventure.rewards.admin`  
**Example**:
```
/jobs admin rewards give Steve miner starter_bonus
```

#### `/jobs admin rewards reset <player> [job] [reward]`
**Description**: Reset a player's rewards  
**Permission**: `jobsadventure.rewards.admin`  
**Examples**:
```
/jobs admin rewards reset Steve                    # All rewards
/jobs admin rewards reset Steve miner              # All miner rewards
/jobs admin rewards reset Steve miner daily_bonus  # Specific reward
```

### Statistics and Monitoring

#### `/jobs admin stats`
**Description**: Display server statistics  
**Permission**: `jobsadventure.admin`  
**Output**:
```
=== JobsAdventure Statistics ===
Active players: 45
Active jobs: 3
Actions processed (last hour): 2,847
Average performance: 0.8ms
Memory used: 42MB
```

#### `/jobs admin performance`
**Description**: Display performance metrics  
**Permission**: `jobsadventure.admin`  

#### `/jobs admin database [save|load|optimize]`
**Description**: Database management  
**Permission**: `jobsadventure.admin`  
**Examples**:
```
/jobs admin database save      # Force save
/jobs admin database load      # Reload data
/jobs admin database optimize  # Optimize DB
```

## 🔧 Command Aliases

JobsAdventure supports several aliases for easier use:

| Full Command | Alias |
|:---|:---|
| `/jobs` | `/job` |
| `/jobs join` | `/job j` |
| `/jobs leave` | `/job l` |
| `/jobs info` | `/job i` |
| `/jobs stats` | `/job s` |
| `/jobs list` | `/job ls` |
| `/jobs rewards` | `/job r` |

## 📝 Common Usage Examples

### Beginner Player Scenario
```bash
/jobs list                    # See available jobs
/jobs join miner             # Join miner job
/jobs info miner             # Understand the job
# [Mine some blocks]
/jobs stats                  # Check progress
/jobs rewards open miner     # See available rewards
```

### Administrator Scenario
```bash
/jobs admin stats            # Check server status
/jobs xpbonus 2.0 3600      # Double XP event for 1h
/jobs admin player Steve setlevel miner 25  # Adjust a level
/jobs reload                 # Reload after config changes
```

## ⚠️ Important Notes

1. **Permissions**: All commands require appropriate permissions
2. **Parameters**: Parameters in `<>` are required, those in `[]` are optional
3. **Tab Completion**: Most commands support tab completion
4. **Case Sensitivity**: Job and player names are case-sensitive

## 🔗 See Also

- [Permissions List](permissions.md)
- [Administration Guide](../admin-guide/admin-commands.md)
- [Player Guide](../player-guide/commands.md)
- [Command Troubleshooting](../troubleshooting/common-issues.md)