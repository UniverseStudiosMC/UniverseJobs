# JobsAdventure Commands and Permissions

Complete reference for all commands, permissions, and administrative features in JobsAdventure.

## Table of Contents

1. [Command Overview](#command-overview)
2. [Player Commands](#player-commands)
3. [Admin Commands](#admin-commands)
4. [Permission System](#permission-system)
5. [Tab Completion](#tab-completion)
6. [Command Usage Examples](#command-usage-examples)
7. [Administrative Guide](#administrative-guide)

---

## Command Overview

JobsAdventure uses a hierarchical command structure with `/jobs` as the base command. All functionality is accessible through subcommands with comprehensive tab completion.

### Command Structure
```
/jobs <subcommand> [arguments...]
```

### Permission Model
- **Player permissions**: Control access to basic job features
- **Admin permissions**: Control access to administrative features
- **Job-specific permissions**: Control access to individual jobs
- **Multiplier permissions**: Grant XP multipliers to players

---

## Player Commands

### Basic Job Management

#### `/jobs list`
**Description**: Display all available jobs with their status and requirements.

**Permission**: `jobsadventure.command.list`

**Usage**: `/jobs list`

**Output Example**:
```
§6§l=== Available Jobs ===

§a✓ Miner §7- §eMining and excavation
  §7Level Requirement: §eNone
  §7Permission: §ejobsadventure.job.miner
  §7Max Level: §e100
  §7Players: §e42

§c✗ Hunter §7- §eHunting and combat
  §7Level Requirement: §eMining Level 25
  §7Permission: §ejobsadventure.job.hunter
  §7Max Level: §e75
  §7Players: §e18

§7 Use §e/jobs join <job> §7to join a job
```

#### `/jobs join <job>`
**Description**: Join a specific job.

**Permission**: `jobsadventure.command.join` + job-specific permission

**Usage**: `/jobs join miner`

**Requirements**:
- Must have job-specific permission
- Cannot already have the job
- Must meet any job requirements

**Output Examples**:
```
§aSuccess: You joined the Miner job!
§cError: You already have the Miner job!
§cError: You need permission to join this job!
```

#### `/jobs leave <job>`
**Description**: Leave a specific job.

**Permission**: `jobsadventure.command.leave`

**Usage**: `/jobs leave miner`

**Note**: XP and levels are preserved when leaving a job.

**Output Examples**:
```
§aYou left the Miner job!
§cError: You don't have the Miner job!
```

### Information Commands

#### `/jobs info [job] [player]`
**Description**: Display detailed information about jobs or players.

**Permission**: `jobsadventure.command.info`

**Usage Variants**:
- `/jobs info` - Show your job summary
- `/jobs info miner` - Show information about the miner job
- `/jobs info PlayerName` - Show another player's job summary (requires admin permission)
- `/jobs info miner PlayerName` - Show another player's progress in miner job (requires admin permission)

**Output Examples**:

*Personal Job Summary*:
```
§6§l=== Your Jobs ===

§eMiner §7- Level §a25 §7(§a12,450§7/§e15,000 XP§7)
  §7Progress: §a[████████████░░░░░░░░] §a83%
  §7XP to next level: §e2,550
  
§eHunter §7- Level §a18 §7(§a8,200§7/§e10,500 XP§7)
  §7Progress: §a[███████████░░░░░░░░░] §a78%
  §7XP to next level: §e2,300

§7Total Jobs: §e2 §7| Total Levels: §e43
```

*Job Information*:
```
§6§l=== Miner Job ===

§7Name: §eMiner
§7Description: §fExtract valuable resources from the earth
§7Max Level: §e100
§7Enabled: §aYes
§7Permission: §ejobsadventure.job.miner

§7XP Curve: §ebalanced_mining
§7Current Players: §e42

§7Main Actions:
§7• Break §estone §7- §a1.0 XP
§7• Break §ediamond_ore §7- §a20.0 XP
§7• Break §egold_ore §7- §a6.0 XP

§7Use §e/jobs join miner §7to join this job
```

#### `/jobs stats [player]`
**Description**: Display comprehensive statistics.

**Permission**: `jobsadventure.command.stats`

**Usage**: 
- `/jobs stats` - Show your statistics
- `/jobs stats PlayerName` - Show another player's statistics (requires admin permission)

**Output Example**:
```
§6§l=== JobsAdventure Statistics ===
§7Player: §ePlayerName

§e§lJob Summary:
§7• Total Jobs: §e3
§7• Total Levels: §e67
§7• Total XP: §e45,230.5
§7• Average Level: §e22.3

§e§lJob Details:
§7• §eMiner §7- Level §a35 §7(§a25,450 XP§7)
§7• §eFarmer §7- Level §a20 §7(§a12,300 XP§7)
§7• §eHunter §7- Level §a12 §7(§a7,480.5 XP§7)

§e§lRankings:
§7• Global Level Rank: §a#15
§7• Miner Rank: §a#8
§7• Farmer Rank: §a#23

§e§lActive Bonuses:
§7• VIP Bonus: §a1.5x XP §7(§e2h 15m remaining§7)
§7• Event Bonus: §a2.0x XP §7(§e45m remaining§7)
```

### Reward System Commands

#### `/jobs rewards open <job>`
**Description**: Open the rewards GUI for a specific job.

**Permission**: `jobsadventure.command.rewards` + `jobsadventure.rewards.<job>`

**Usage**: `/jobs rewards open miner`

**Note**: Opens the interactive GUI where players can view and claim rewards.

#### `/jobs rewards list [job]`
**Description**: List available rewards in chat format.

**Permission**: `jobsadventure.command.rewards`

**Usage**:
- `/jobs rewards list` - List rewards for all your jobs
- `/jobs rewards list miner` - List rewards for miner job

**Output Example**:
```
§6§l=== Miner Rewards ===

§a✓ §eStarter Tools §7(Level 5) - §aAvailable
§7  Iron pickaxe and shovel for new miners

§c✗ §eAdvanced Equipment §7(Level 25) - §cLevel Required
§7  Enhanced tools with better enchantments

§8✓ §7Daily Bonus §7(Level 10) - §8Claimed
§7  Daily reward for active miners
§7  §7Next available in: §e18h 45m

§7Use §e/jobs rewards open miner §7to claim rewards
```

#### `/jobs rewards claim <job> <reward>`
**Description**: Claim a specific reward directly.

**Permission**: `jobsadventure.command.rewards`

**Usage**: `/jobs rewards claim miner starter_tools`

**Output Examples**:
```
§aReward 'Starter Tools' claimed successfully!
§cYou don't meet the requirements for this reward!
§eYou've already claimed this reward!
```

#### `/jobs rewards info <job> <reward>`
**Description**: Show detailed information about a specific reward.

**Permission**: `jobsadventure.command.rewards`

**Usage**: `/jobs rewards info miner starter_tools`

**Output Example**:
```
§6§l=== Starter Tools ===

§7Name: §eStarter Tools
§7Description: §fBasic tools for new miners
§7Required Level: §e5
§7Repeatable: §cNo

§7Requirements:
§7• Level 5 or higher in Miner
§7• Permission: jobsadventure.rewards.miner

§7Rewards:
§7• §fIron Pickaxe §7(Efficiency II, Unbreaking I)
§7• §fIron Shovel §7(Efficiency I)
§7• §e5 Diamonds
§7• §a$1,000

§7Status: §aAvailable
```

---

## Admin Commands

### Configuration Management

#### `/jobs reload`
**Description**: Reload all plugin configurations without restarting the server.

**Permission**: `jobsadventure.admin.reload`

**Usage**: `/jobs reload`

**Reloads**:
- Main configuration
- All job configurations
- All reward configurations
- GUI configurations
- XP curve configurations

**Output**: `§aJobsAdventure configuration reloaded successfully!`

#### `/jobs debug <on|off>`
**Description**: Toggle debug mode for troubleshooting.

**Permission**: `jobsadventure.admin.debug`

**Usage**: 
- `/jobs debug on` - Enable debug mode
- `/jobs debug off` - Disable debug mode

**Debug Information**:
- XP calculation details
- Action processing steps
- Condition evaluation results
- Performance metrics
- Error stack traces

### Player Management

#### `/jobs admin join <player> <job>`
**Description**: Force a player to join a job (bypasses requirements).

**Permission**: `jobsadventure.admin.manage`

**Usage**: `/jobs admin join PlayerName miner`

#### `/jobs admin leave <player> <job>`
**Description**: Force a player to leave a job.

**Permission**: `jobsadventure.admin.manage`

**Usage**: `/jobs admin leave PlayerName miner`

#### `/jobs admin setlevel <player> <job> <level>`
**Description**: Set a player's level in a specific job.

**Permission**: `jobsadventure.admin.manage`

**Usage**: `/jobs admin setlevel PlayerName miner 50`

#### `/jobs admin setxp <player> <job> <xp>`
**Description**: Set a player's XP in a specific job.

**Permission**: `jobsadventure.admin.manage`

**Usage**: `/jobs admin setxp PlayerName miner 25000`

#### `/jobs admin addxp <player> <job> <xp>`
**Description**: Add XP to a player's job.

**Permission**: `jobsadventure.admin.manage`

**Usage**: `/jobs admin addxp PlayerName miner 1000`

### XP Bonus Management

#### `/jobs xpbonus give <player|*> <multiplier> <duration> [job] [reason]`
**Description**: Give XP bonuses to players.

**Permission**: `jobsadventure.admin.xpbonus`

**Usage Examples**:
- `/jobs xpbonus give PlayerName 2.0 1h` - 2x bonus for 1 hour (all jobs)
- `/jobs xpbonus give PlayerName 1.5 30m miner` - 1.5x bonus for 30 minutes (miner only)
- `/jobs xpbonus give * 2.0 2h "Server Event"` - 2x bonus for all online players
- `/jobs xpbonus give PlayerName 3.0 45m hunter "VIP Reward"` - 3x bonus with reason

**Duration Formats**:
- `30s` - 30 seconds
- `15m` - 15 minutes
- `2h` - 2 hours
- `1d` - 1 day
- `1w` - 1 week

#### `/jobs xpbonus remove <player> [job]`
**Description**: Remove XP bonuses from a player.

**Permission**: `jobsadventure.admin.xpbonus`

**Usage**:
- `/jobs xpbonus remove PlayerName` - Remove all bonuses
- `/jobs xpbonus remove PlayerName miner` - Remove miner-specific bonuses

#### `/jobs xpbonus list [player]`
**Description**: List active XP bonuses.

**Permission**: `jobsadventure.admin.xpbonus`

**Usage**:
- `/jobs xpbonus list` - List all active bonuses
- `/jobs xpbonus list PlayerName` - List bonuses for specific player

**Output Example**:
```
§6§l=== Active XP Bonuses ===

§ePlayerName:
§7• Global Bonus: §a2.0x §7(§e1h 23m remaining§7) - §fServer Event
§7• Miner Bonus: §a1.5x §7(§e45m remaining§7) - §fVIP Reward

§eOtherPlayer:
§7• Hunter Bonus: §a3.0x §7(§e2h 15m remaining§7) - §fDaily Bonus

§7Total Active Bonuses: §e3
§7Players with Bonuses: §e2
```

#### `/jobs xpbonus info`
**Description**: Show XP bonus system statistics.

**Permission**: `jobsadventure.admin.xpbonus`

**Usage**: `/jobs xpbonus info`

**Output Example**:
```
§6§l=== XP Bonus System Statistics ===

§7Active Bonuses: §e12
§7Players with Bonuses: §e8
§7Total Multiplier Usage: §e18.5x

§7Most Common Bonuses:
§7• VIP Bonus (1.5x): §e5 players
§7• Event Bonus (2.0x): §e3 players
§7• Daily Bonus (1.2x): §e4 players

§7System Health:
§7• Memory Usage: §aLow
§7• Cleanup Last Run: §e5 minutes ago
§7• Expired Bonuses Cleaned: §e3
```

#### `/jobs xpbonus cleanup`
**Description**: Manually clean up expired bonuses.

**Permission**: `jobsadventure.admin.xpbonus`

**Usage**: `/jobs xpbonus cleanup`

**Output**: `§aCleaned up 5 expired XP bonuses.`

### Reward Administration

#### `/jobs rewards admin give <player> <job> <reward>`
**Description**: Give a reward to a player (bypasses requirements).

**Permission**: `jobsadventure.admin.rewards`

**Usage**: `/jobs rewards admin give PlayerName miner starter_tools`

#### `/jobs rewards admin reset <player> [job] [reward]`
**Description**: Reset reward claim status.

**Permission**: `jobsadventure.admin.rewards`

**Usage**:
- `/jobs rewards admin reset PlayerName` - Reset all claimed rewards
- `/jobs rewards admin reset PlayerName miner` - Reset all miner rewards
- `/jobs rewards admin reset PlayerName miner starter_tools` - Reset specific reward

#### `/jobs rewards admin reload`
**Description**: Reload reward configurations only.

**Permission**: `jobsadventure.admin.rewards`

**Usage**: `/jobs rewards admin reload`

---

## Permission System

### Core Permissions

#### Basic Plugin Access
```yaml
# Basic plugin usage
jobsadventure.use:
  description: "Basic access to JobsAdventure features"
  default: true

# Command access
jobsadventure.command.list:
  description: "Access to /jobs list command"
  default: true

jobsadventure.command.join:
  description: "Access to /jobs join command"
  default: true

jobsadventure.command.leave:
  description: "Access to /jobs leave command"
  default: true

jobsadventure.command.info:
  description: "Access to /jobs info command"
  default: true

jobsadventure.command.stats:
  description: "Access to /jobs stats command"
  default: true

jobsadventure.command.rewards:
  description: "Access to reward commands"
  default: true
```

#### Job-Specific Permissions
```yaml
# Individual job access
jobsadventure.job.miner:
  description: "Permission to join the miner job"
  default: false

jobsadventure.job.farmer:
  description: "Permission to join the farmer job"
  default: false

jobsadventure.job.hunter:
  description: "Permission to join the hunter job"
  default: false

# Wildcard job access
jobsadventure.job.*:
  description: "Permission to join any job"
  default: false
```

#### Reward Permissions
```yaml
# Job-specific reward access
jobsadventure.rewards.miner:
  description: "Access to miner rewards"
  default: false

jobsadventure.rewards.farmer:
  description: "Access to farmer rewards"
  default: false

# Wildcard reward access
jobsadventure.rewards.*:
  description: "Access to all job rewards"
  default: false

# Specific reward permissions (optional, for fine-grained control)
jobsadventure.rewards.miner.starter_tools:
  description: "Access to miner starter tools reward"
  default: false
```

#### XP Multiplier Permissions
```yaml
# Multiplier permissions (automatic XP bonuses)
jobsadventure.multiplier.vip:
  description: "1.5x XP multiplier for VIP players"
  default: false

jobsadventure.multiplier.premium:
  description: "2.0x XP multiplier for premium players"
  default: false

jobsadventure.multiplier.mvp:
  description: "3.0x XP multiplier for MVP players"
  default: false

jobsadventure.multiplier.admin:
  description: "10.0x XP multiplier for administrators"
  default: false
```

### Administrative Permissions

#### Core Admin Permissions
```yaml
# Full administrative access
jobsadventure.admin:
  description: "Full administrative access to JobsAdventure"
  default: op

# Configuration management
jobsadventure.admin.reload:
  description: "Permission to reload configurations"
  default: op

jobsadventure.admin.debug:
  description: "Permission to toggle debug mode"
  default: op

# Player management
jobsadventure.admin.manage:
  description: "Permission to manage player jobs and XP"
  default: op

jobsadventure.admin.view:
  description: "Permission to view other players' information"
  default: op
```

#### Specific Admin Permissions
```yaml
# XP bonus management
jobsadventure.admin.xpbonus:
  description: "Permission to manage XP bonuses"
  default: op

jobsadventure.admin.xpbonus.give:
  description: "Permission to give XP bonuses"
  default: op

jobsadventure.admin.xpbonus.remove:
  description: "Permission to remove XP bonuses"
  default: op

# Reward administration
jobsadventure.admin.rewards:
  description: "Permission to manage rewards"
  default: op

jobsadventure.admin.rewards.give:
  description: "Permission to give rewards to players"
  default: op

jobsadventure.admin.rewards.reset:
  description: "Permission to reset reward claims"
  default: op
```

### Permission Groups

#### Recommended Permission Groups

**Default Player**:
```yaml
permissions:
  - jobsadventure.use
  - jobsadventure.command.*
  - jobsadventure.job.miner
  - jobsadventure.job.farmer
  - jobsadventure.rewards.miner
  - jobsadventure.rewards.farmer
```

**VIP Player**:
```yaml
permissions:
  - jobsadventure.use
  - jobsadventure.command.*
  - jobsadventure.job.*
  - jobsadventure.rewards.*
  - jobsadventure.multiplier.vip
```

**Premium Player**:
```yaml
permissions:
  - jobsadventure.use
  - jobsadventure.command.*
  - jobsadventure.job.*
  - jobsadventure.rewards.*
  - jobsadventure.multiplier.premium
```

**Moderator**:
```yaml
permissions:
  - jobsadventure.use
  - jobsadventure.command.*
  - jobsadventure.job.*
  - jobsadventure.rewards.*
  - jobsadventure.admin.view
  - jobsadventure.admin.xpbonus
  - jobsadventure.multiplier.premium
```

**Administrator**:
```yaml
permissions:
  - jobsadventure.admin
  - jobsadventure.multiplier.admin
```

---

## Tab Completion

JobsAdventure provides comprehensive tab completion for all commands to improve usability.

### Completion Features

#### Dynamic Job Completion
- `/jobs join <TAB>` → Shows available jobs player can join
- `/jobs leave <TAB>` → Shows jobs player currently has
- `/jobs info <TAB>` → Shows all jobs + online player names

#### Smart Player Completion
- `/jobs admin setlevel <TAB>` → Shows online players
- `/jobs admin <player> <TAB>` → Shows jobs that player has

#### Context-Aware Suggestions
- `/jobs rewards claim miner <TAB>` → Shows available rewards for miner
- `/jobs xpbonus give PlayerName <TAB>` → Shows multiplier suggestions (1.5, 2.0, etc.)

#### Duration Completion
- `/jobs xpbonus give PlayerName 2.0 <TAB>` → Shows duration examples (30m, 1h, 2h, 1d)

### Completion Examples

```bash
# Basic job management
/jobs <TAB>
├── list
├── join
├── leave
├── info
├── stats
├── rewards
├── xpbonus (admin only)
├── admin (admin only)
├── reload (admin only)
└── debug (admin only)

# Job selection
/jobs join <TAB>
├── miner (if available)
├── farmer (if available)
└── hunter (if level requirement met)

# Reward management
/jobs rewards <TAB>
├── open
├── list
├── claim
├── info
└── admin (admin only)

# Admin commands
/jobs admin <TAB>
├── join
├── leave
├── setlevel
├── setxp
└── addxp

# XP bonus duration suggestions
/jobs xpbonus give PlayerName 2.0 <TAB>
├── 30s
├── 5m
├── 15m
├── 30m
├── 1h
├── 2h
├── 6h
├── 12h
├── 1d
└── 1w
```

---

## Command Usage Examples

### Player Scenarios

#### New Player Getting Started
```bash
# 1. See what jobs are available
/jobs list

# 2. Join the miner job
/jobs join miner

# 3. Check your progress
/jobs info

# 4. See available rewards
/jobs rewards list miner

# 5. Claim a reward when eligible
/jobs rewards claim miner starter_tools
```

#### Experienced Player Managing Multiple Jobs
```bash
# Check overall statistics
/jobs stats

# Compare your progress in different jobs
/jobs info miner
/jobs info farmer
/jobs info hunter

# Check rankings
/jobs info  # Shows your rank in each job

# Open reward GUI for best job
/jobs rewards open hunter
```

### Administrative Scenarios

#### Setting Up a New Player
```bash
# Give them starter jobs
/jobs admin join NewPlayer miner
/jobs admin join NewPlayer farmer

# Give them some starting XP
/jobs admin addxp NewPlayer miner 1000
/jobs admin addxp NewPlayer farmer 500

# Give them a temporary XP bonus
/jobs xpbonus give NewPlayer 2.0 1h "New Player Bonus"
```

#### Running a Server Event
```bash
# Start a double XP event for all players
/jobs xpbonus give * 2.0 2h "Double XP Event"

# Check bonus statistics
/jobs xpbonus info

# End event early if needed
/jobs xpbonus cleanup
```

#### Troubleshooting Player Issues
```bash
# Enable debug mode
/jobs debug on

# Check specific player's status
/jobs info miner PlayerName

# Reset a problematic reward
/jobs rewards admin reset PlayerName miner daily_bonus

# Give missing reward manually
/jobs rewards admin give PlayerName miner starter_tools

# Disable debug mode
/jobs debug off
```

#### Managing Problem Players
```bash
# Remove someone from a job
/jobs admin leave PlayerName hunter

# Reset all their progress (if needed)
/jobs admin setlevel PlayerName miner 1
/jobs admin setxp PlayerName miner 0

# Remove their bonuses
/jobs xpbonus remove PlayerName
```

---

## Administrative Guide

### Daily Administration Tasks

#### Regular Monitoring
```bash
# Check system health
/jobs xpbonus info

# Monitor active bonuses
/jobs xpbonus list

# Check for any debug messages in console
/jobs debug on  # (temporarily, to check for issues)
/jobs debug off
```

#### Weekly Maintenance
```bash
# Reload configurations (after updates)
/jobs reload

# Clean up expired bonuses
/jobs xpbonus cleanup

# Review player statistics for balance
/jobs stats TopPlayer1
/jobs stats TopPlayer2
```

### Event Management

#### Starting Events
```bash
# Weekend double XP
/jobs xpbonus give * 2.0 48h "Weekend Event"

# Job-specific events
/jobs xpbonus give * 1.5 6h miner "Mining Rush Event"
/jobs xpbonus give * 2.0 3h hunter "Hunt Challenge"

# VIP exclusive events
/jobs xpbonus give PlayerName1 3.0 1h "VIP Exclusive"
/jobs xpbonus give PlayerName2 3.0 1h "VIP Exclusive"
```

#### Monitoring Events
```bash
# Check event participation
/jobs xpbonus list

# Monitor bonus usage
/jobs xpbonus info

# End events early if needed
/jobs xpbonus remove *  # Remove all bonuses
```

### Troubleshooting Commands

#### Configuration Issues
```bash
# Test configuration changes
/jobs reload

# Check if specific jobs are working
/jobs info miner
/jobs info farmer

# Verify permissions are working
/jobs list  # (test as different permission levels)
```

#### Player Data Issues
```bash
# Check player data integrity
/jobs info PlayerName

# Reset corrupted data
/jobs admin setlevel PlayerName miner 1
/jobs admin setxp PlayerName miner 0
/jobs admin join PlayerName miner

# Verify reward status
/jobs rewards list miner PlayerName
```

#### Performance Issues
```bash
# Enable debug to find bottlenecks
/jobs debug on

# Monitor XP bonus overhead
/jobs xpbonus info

# Check for excessive bonuses
/jobs xpbonus list

# Clean up if needed
/jobs xpbonus cleanup
```

### Best Practices for Administrators

#### Permission Management
1. **Use Groups**: Set up permission groups rather than individual permissions
2. **Test Permissions**: Regularly test permission setups with test accounts
3. **Document Changes**: Keep track of permission changes for troubleshooting

#### Event Management
1. **Announce Events**: Always announce bonus events to players
2. **Monitor Usage**: Keep track of bonus usage to prevent abuse
3. **Plan Duration**: Use appropriate durations for different event types

#### Data Management
1. **Regular Backups**: Back up player data regularly
2. **Monitor Growth**: Keep track of player progression for balance
3. **Clean Up**: Regularly clean up expired bonuses and old data

#### Configuration Management
1. **Test Changes**: Always test configuration changes on a test server first
2. **Document Settings**: Keep documentation of custom configurations
3. **Version Control**: Use version control for configuration files

---

This comprehensive command and permission reference provides everything needed to effectively manage JobsAdventure on your server. Regular use of these commands will help maintain a balanced and enjoyable jobs system for your players.