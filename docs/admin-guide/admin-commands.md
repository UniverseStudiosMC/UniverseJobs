# ⚔️ Administration Guide - Essential Commands

This guide presents all administration commands to efficiently manage UniverseJobs on your server.

## 🔑 Administrator Permissions

First, make sure you have the right permissions:

| Permission | Description |
|:---|:---|
| `UniverseJobs.admin` | Access to basic administration commands |
| `UniverseJobs.admin.xpbonus` | XP bonus management |
| `UniverseJobs.rewards.admin` | Reward administration |

## 🔧 General Plugin Management

### Reload Configuration
```
/jobs reload
```
**Usage:** After modifying configuration files
**Effect:** Reloads all files without restarting the server

### Debug Mode
```
/jobs debug on
/jobs debug off
```
**Usage:** To diagnose problems
**Effect:** Enables/disables detailed logs in console

### Server Statistics
```
/jobs admin stats
```
**Example output:**
```
=== UniverseJobs Statistics ===
Active players: 42
Active jobs: 3
Actions processed (last hour): 1,847
Average performance: 0.7ms
Memory used: 38MB
Cache hits: 94.2%
```

### Performance Metrics
```
/jobs admin performance
```
Displays detailed metrics for performance optimization.

## 👥 Player Management

### Force a player to join a job
```
/jobs admin player <player> join <job>
```
**Examples:**
```
/jobs admin player Steve join miner
/jobs admin player Alice join farmer
```

### Force a player to leave a job
```
/jobs admin player <player> leave <job>
```
**Example:**
```
/jobs admin player Steve leave miner
```

### Modify a player's level
```
/jobs admin player <player> setlevel <job> <level>
```
**Examples:**
```
/jobs admin player Steve setlevel miner 50
/jobs admin player Alice setlevel farmer 25
```

### Add XP to a player
```
/jobs admin player <player> addxp <job> <xp>
```
**Examples:**
```
/jobs admin player Steve addxp miner 1000
/jobs admin player Alice addxp farmer 500
```

### Remove XP from a player
```
/jobs admin player <player> removexp <job> <xp>
```
**Example:**
```
/jobs admin player Steve removexp miner 200
```

### Reset a player
```
/jobs admin player <player> reset [job]
```
**Examples:**
```
/jobs admin player Steve reset           # All jobs
/jobs admin player Steve reset miner     # Specific job
```

## 🚀 XP Bonus System

### Global XP bonus for all players
```
/jobs xpbonus <multiplier> <duration>
```
**Parameters:**
- `multiplier`: 0.1 to 10.0 (e.g., 2.0 = +100% XP)
- `duration`: in seconds (max 86400 = 24h)

**Examples:**
```
/jobs xpbonus 2.0 3600        # Double XP for 1 hour
/jobs xpbonus 1.5 7200        # +50% XP for 2 hours
/jobs xpbonus 3.0 1800        # Triple XP for 30 minutes
```

### XP bonus for a specific player
```
/jobs xpbonus <player> <multiplier> <duration>
```
**Examples:**
```
/jobs xpbonus VIP_Player 2.5 3600    # +150% XP for a VIP
/jobs xpbonus NewPlayer 1.2 86400     # +20% XP for a beginner (24h)
```

### XP bonus for a specific job
```
/jobs xpbonus <player> <job> <multiplier> <duration>
```
**Examples:**
```
/jobs xpbonus Steve miner 3.0 1800    # Triple mining XP for Steve (30 min)
/jobs xpbonus Alice farmer 2.0 7200   # Double farming XP for Alice (2h)
```

### Common Usage Scenarios

#### Weekend Event
```
/jobs xpbonus 2.0 172800    # Double XP for entire weekend (48h)
```

#### Welcome Bonus
```
/jobs xpbonus NewPlayer 1.5 604800    # +50% XP for 1 week
```

#### Specialized Job Event
```
# Mining week - bonus for all miners
/jobs xpbonus PlayerA miner 2.0 604800
/jobs xpbonus PlayerB miner 2.0 604800
# etc...
```

## 🎁 Reward Management

### Give a reward to a player
```
/jobs admin rewards give <player> <job> <reward>
```
**Examples:**
```
/jobs admin rewards give Steve miner starter_bonus
/jobs admin rewards give Alice farmer daily_bonus
```

### Reset rewards
```
/jobs admin rewards reset <player> [job] [reward]
```
**Examples:**
```
/jobs admin rewards reset Steve                     # All rewards
/jobs admin rewards reset Steve miner               # All miner rewards
/jobs admin rewards reset Steve miner daily_bonus   # Specific reward
```

### Force reward eligibility
```
/jobs admin rewards unlock <player> <job> <reward>
```
Useful for unlocking special rewards during events.

## 💾 Data Management

### Force Save
```
/jobs admin database save
```
Forces saving of all player data.

### Reload Data
```
/jobs admin database load
```
Reloads all data from source (files/DB).

### Database Optimization
```
/jobs admin database optimize
```
Optimizes database performance (MySQL only).

### Data Migration
```
/jobs admin migrate file-to-database
/jobs admin migrate database-to-file
```
Migrates data between files and database.

## 📊 Monitoring and Surveillance

### View active players by job
```
/jobs admin list players <job>
```
**Example:**
```
/jobs admin list players miner
```

### Detailed player statistics
```
/jobs admin info <player>
```
Displays all administrator information about a player.

### Recent action logs
```
/jobs admin log [player] [job] [hours]
```
**Examples:**
```
/jobs admin log                      # All actions (last hour)
/jobs admin log Steve                # Steve's actions (last hour)
/jobs admin log Steve miner 24       # Steve's mining actions (24h)
```

### Performance alerts
```
/jobs admin alerts
```
Displays performance alerts and optimization recommendations.

## 🔧 Maintenance and Diagnostics

### Performance Test
```
/jobs admin benchmark
```
Runs a performance test to diagnose issues.

### Cache Management
```
/jobs admin cache clear
/jobs admin cache info
```
Manages the plugin's internal cache.

### Integrity Check
```
/jobs admin check integrity
```
Verifies data and configuration integrity.

### Data Export
```
/jobs admin export <format> [file]
```
**Supported formats:** `csv`, `json`, `yaml`
**Examples:**
```
/jobs admin export csv player_stats.csv
/jobs admin export json backup.json
```

## 🎯 Common Administration Scenarios

### New Server - Initial Setup
```bash
# 1. Verify installation
/jobs admin stats

# 2. Configure launch event
/jobs xpbonus 2.0 604800    # Double XP for 1 week

# 3. Create test accounts
/jobs admin player TestPlayer join miner
/jobs admin player TestPlayer setlevel miner 10
```

### Special Event - Double XP Weekend
```bash
# Friday evening
/jobs xpbonus 2.0 172800
/broadcast &6[Event] &eDouble XP activated for the weekend!

# Sunday evening - verification
/jobs admin stats    # See event impact
```

### Player Issue - Reset
```bash
# Investigation
/jobs admin info ProblematicPlayer
/jobs admin log ProblematicPlayer 48

# Reset if necessary
/jobs admin player ProblematicPlayer reset
/jobs admin rewards reset ProblematicPlayer
```

### Server Maintenance - Backup
```bash
# Before maintenance
/jobs admin database save
/jobs admin export json backup_$(date).json

# After maintenance
/jobs admin check integrity
/jobs reload
```

## ⚠️ Best Practices

### Regular Backups
- Configure **automatic backups** every hour
- Test **restoration** regularly
- Keep **archives** for several days

### Continuous Monitoring
- Check `/jobs admin stats` daily
- Monitor **performance alerts**
- Watch for unusual **usage spikes**

### Event Management
- **Plan** XP bonuses in advance
- **Communicate** events to players
- **Monitor** impact on economy

### Security
- **Limit** admin permissions
- **Log** all administrator actions
- **Verify** access regularly

## 🆘 Quick Troubleshooting

### Degraded Performance
```bash
/jobs admin performance      # Identify bottlenecks
/jobs admin cache clear      # Clear cache
/jobs admin database optimize # Optimize DB
```

### Corrupted Data
```bash
/jobs admin check integrity  # Diagnostic
/jobs admin database load    # Reload
# If necessary: restore from backup
```

### Plugin Unresponsive
```bash
/jobs debug on              # Enable logs
/jobs reload                # Reload
# Check console for errors
```

## 📚 See Also

- [Player Management](player-management.md)
- [XP Bonus System](xp-bonus-system.md)
- [Monitoring and Debugging](monitoring-debugging.md)
- [Commands Reference](../reference/commands-reference.md)
- [Troubleshooting](../troubleshooting/common-issues.md)