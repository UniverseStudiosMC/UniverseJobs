# JobsAdventure Placeholders Guide

This guide presents all available placeholders with the JobsAdventure ranking system.

## Job Leaderboards

### General format
```
%jobsadventure_<job>_<type>_<parameter>%
```

### Leaderboard placeholders

#### Ranking by position
```
%jobsadventure_<job>_leaderboard_<position>_<info>%
```

**Examples:**
- `%jobsadventure_miner_leaderboard_1_name%` - Name of #1 miner
- `%jobsadventure_miner_leaderboard_1_level%` - Level of #1 miner
- `%jobsadventure_miner_leaderboard_1_xp%` - XP of #1 miner
- `%jobsadventure_miner_leaderboard_1_formatted%` - Full format of #1 player

**Available information:**
- `name` or `player` - Player name
- `level` - Player level
- `xp` - Player experience points
- `rank` or `position` - Position in leaderboard
- `formatted` - Full format (ex: "#1 PlayerName - Level 25 (1500.0 XP)")

### Player placeholders

#### Connected player statistics
```
%jobsadventure_<job>_player_<info>%
```

**Examples:**
- `%jobsadventure_miner_player_level%` - Player level in miner job
- `%jobsadventure_miner_player_xp%` - Player XP in miner job
- `%jobsadventure_miner_player_rank%` - Player rank in miner leaderboard
- `%jobsadventure_miner_player_progress%` - Progress to next level (ex: "450.0/1000.0")
- `%jobsadventure_miner_player_progresspercent%` - Progress percentage (ex: "45.0%")
- `%jobsadventure_miner_player_hasjob%` - If player has this job (true/false)

### Job information placeholders

#### General job information
```
%jobsadventure_<job>_job_<info>%
```

**Examples:**
- `%jobsadventure_miner_job_name%` - Job display name
- `%jobsadventure_miner_job_description%` - Job description
- `%jobsadventure_miner_job_maxlevel%` - Job maximum level
- `%jobsadventure_miner_job_enabled%` - If job is enabled
- `%jobsadventure_miner_job_playercount%` - Number of players with this job

## Global Leaderboards

### General format
```
%jobsglobal_<type>_<parameter>%
```

### Total levels leaderboard

#### Top cumulative levels
```
%jobsglobal_totallevels_<position>_<info>%
```

**Examples:**
- `%jobsglobal_totallevels_1_name%` - Player with most cumulative levels
- `%jobsglobal_totallevels_1_value%` - Total levels of #1 player
- `%jobsglobal_totallevels_1_formatted%` - Full format

### Total jobs leaderboard

#### Top number of jobs owned
```
%jobsglobal_totaljobs_<position>_<info>%
```

**Examples:**
- `%jobsglobal_totaljobs_1_name%` - Player with most jobs
- `%jobsglobal_totaljobs_1_value%` - Number of jobs of #1 player
- `%jobsglobal_totaljobs_1_formatted%` - Full format

### Total XP leaderboard

#### Top cumulative XP
```
%jobsglobal_totalxp_<position>_<info>%
```

**Examples:**
- `%jobsglobal_totalxp_1_name%` - Player with most total XP
- `%jobsglobal_totalxp_1_value%` - Total XP of #1 player
- `%jobsglobal_totalxp_1_formatted%` - Full format

### Global player statistics

#### Personal global stats
```
%jobsglobal_player_<stat>%
```

**Examples:**
- `%jobsglobal_player_totallevels%` - Player's total levels
- `%jobsglobal_player_totaljobs%` - Player's number of jobs
- `%jobsglobal_player_totalxp%` - Player's total XP
- `%jobsglobal_player_avgLevel%` - Player's average level
- `%jobsglobal_player_rank_totallevels%` - Player rank in levels leaderboard
- `%jobsglobal_player_rank_totaljobs%` - Player rank in jobs leaderboard
- `%jobsglobal_player_rank_totalxp%` - Player rank in XP leaderboard

## Usage Examples

### Miner leaderboard panel

```
=== TOP MINERS ===
#1: %jobsadventure_miner_leaderboard_1_formatted%
#2: %jobsadventure_miner_leaderboard_2_formatted%
#3: %jobsadventure_miner_leaderboard_3_formatted%

Your rank: %jobsadventure_miner_player_rank%
Your level: %jobsadventure_miner_player_level%
Your progress: %jobsadventure_miner_player_progresspercent%
```

### Global leaderboard panel

```
=== GLOBAL LEADERBOARDS ===

Top Levels:
#1: %jobsglobal_totallevels_1_name% (%jobsglobal_totallevels_1_value% levels)

Top Jobs:
#1: %jobsglobal_totaljobs_1_name% (%jobsglobal_totaljobs_1_value% jobs)

Top XP:
#1: %jobsglobal_totalxp_1_name% (%jobsglobal_totalxp_1_value% XP)

Your stats:
- Total levels: %jobsglobal_player_totallevels%
- Jobs: %jobsglobal_player_totaljobs%
- Total XP: %jobsglobal_player_totalxp%
- Average level: %jobsglobal_player_avgLevel%
```

### Dynamic hologram

```yaml
# Example for DeluxeMenus or HolographicDisplays
lines:
  - "&6&l=== TOP FARMERS ==="
  - "&e#1: &f%jobsadventure_farmer_leaderboard_1_name% &7- &aLevel %jobsadventure_farmer_leaderboard_1_level%"
  - "&e#2: &f%jobsadventure_farmer_leaderboard_2_name% &7- &aLevel %jobsadventure_farmer_leaderboard_2_level%"
  - "&e#3: &f%jobsadventure_farmer_leaderboard_3_name% &7- &aLevel %jobsadventure_farmer_leaderboard_3_level%"
  - ""
  - "&bFarmer players: &f%jobsadventure_farmer_job_playercount%"
```

## Supported Jobs

Replace `<job>` with your job ID:
- `miner` - Miner
- `farmer` - Farmer
- `hunter` - Hunter
- `fisherman` - Fisherman
- And all other jobs configured on your server

## Optimization

- Leaderboards are cached for 30 seconds for individual jobs
- Global leaderboards are cached for 1 minute
- Cache is automatically cleared when players level up

## Support

If a placeholder returns an unexpected value:
- Check that PlaceholderAPI is installed
- Check that the job exists and is enabled
- Check that the requested position exists in the leaderboard
- Check server logs for any errors