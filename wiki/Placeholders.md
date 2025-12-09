# Placeholders

UniverseJobs provides extensive PlaceholderAPI integration for use in scoreboards, holograms, chat formats, and more.

## Requirements

- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) installed

## General Placeholders

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%universejobs_totaljobs%` | Total number of available jobs | `7` |
| `%universejobs_equippedjobs%` | Number of jobs player has | `2` |
| `%universejobs_currentjobs%` | Names of current jobs | `Miner, Farmer` |
| `%universejobs_maxjobs%` | Maximum jobs allowed | `3` |

## Job-Specific Placeholders

Replace `<job>` with the job ID (e.g., `miner`, `farmer`).

### Level & XP

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%universejobs_<job>_level%` | Current level | `25` |
| `%universejobs_<job>_xp%` | Current XP | `1500` |
| `%universejobs_<job>_xp_required%` | XP needed for next level | `2000` |
| `%universejobs_<job>_xp_to_next%` | XP remaining for next level | `500` |
| `%universejobs_<job>_maxlevel%` | Maximum level for job | `100` |

### Progress

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%universejobs_<job>_progress%` | Progress bar | `■■■■■□□□□□` |
| `%universejobs_<job>_progress_percent%` | Progress percentage | `75%` |
| `%universejobs_<job>_progress_decimal%` | Progress as decimal | `0.75` |

### Job Info

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%universejobs_<job>_name%` | Job display name | `Miner` |
| `%universejobs_<job>_description%` | Job description | `Mine ores...` |
| `%universejobs_<job>_joined%` | Is player in this job | `true` |

### Examples

```
%universejobs_miner_level%          → 25
%universejobs_miner_xp%             → 1500
%universejobs_miner_xp_required%    → 2000
%universejobs_miner_progress%       → ■■■■■■■□□□
%universejobs_farmer_joined%        → true
```

## Leaderboard Placeholders

### Global Leaderboard

All jobs combined ranking:

| Placeholder | Description |
|-------------|-------------|
| `%universejobs_global_rank%` | Player's global rank |
| `%universejobs_global_rank_<pos>_player%` | Player name at position |
| `%universejobs_global_rank_<pos>_level%` | Total level at position |

Examples:
```
%universejobs_global_rank%              → 5
%universejobs_global_rank_1_player%     → Steve
%universejobs_global_rank_1_level%      → 342
%universejobs_global_rank_10_player%    → Alex
```

### Job Leaderboard

Per-job rankings:

| Placeholder | Description |
|-------------|-------------|
| `%universejobs_<job>_rank%` | Player's rank in job |
| `%universejobs_leaderboard_<job>_rank_<pos>_player%` | Player name at position |
| `%universejobs_leaderboard_<job>_rank_<pos>_level%` | Level at position |
| `%universejobs_leaderboard_<job>_rank_<pos>_xp%` | XP at position |

Examples:
```
%universejobs_miner_rank%                           → 3
%universejobs_leaderboard_miner_rank_1_player%      → Steve
%universejobs_leaderboard_miner_rank_1_level%       → 100
%universejobs_leaderboard_miner_rank_1_xp%          → 50000
```

## Boost Placeholders

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%universejobs_boost_active%` | Has any active boost | `true` |
| `%universejobs_boost_xp_active%` | Has XP boost | `true` |
| `%universejobs_boost_money_active%` | Has money boost | `false` |
| `%universejobs_boost_xp_multiplier%` | Total XP multiplier | `2.0` |
| `%universejobs_boost_money_multiplier%` | Total money multiplier | `1.5` |
| `%universejobs_boost_time_remaining%` | Time until boost expires | `45:30` |

### Job-Specific Boosts

| Placeholder | Description |
|-------------|-------------|
| `%universejobs_boost_<job>_xp_multiplier%` | XP multiplier for job |
| `%universejobs_boost_<job>_money_multiplier%` | Money multiplier for job |
| `%universejobs_boost_<job>_time_remaining%` | Time remaining for job boost |

## Statistics Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%universejobs_stats_total_xp%` | Total XP earned across all jobs |
| `%universejobs_stats_total_money%` | Total money earned |
| `%universejobs_stats_total_actions%` | Total actions performed |
| `%universejobs_stats_<job>_actions%` | Actions in specific job |

## Usage Examples

### Scoreboard (Featherboard)

```yaml
lines:
  - "&6Jobs"
  - "&7Miner: &f%universejobs_miner_level%"
  - "&7  %universejobs_miner_progress%"
  - "&7Farmer: &f%universejobs_farmer_level%"
  - "&7  %universejobs_farmer_progress%"
  - ""
  - "&6Boost: &a%universejobs_boost_xp_multiplier%x XP"
```

### Tab (TAB Plugin)

```yaml
header:
  - "&6Level: &f%universejobs_miner_level% &7| &6XP: &f%universejobs_miner_xp%"
```

### Hologram (DecentHolograms)

```yaml
lines:
  - "&6&lMiner Leaderboard"
  - "&e1. &f%universejobs_leaderboard_miner_rank_1_player% &7- Lvl &f%universejobs_leaderboard_miner_rank_1_level%"
  - "&e2. &f%universejobs_leaderboard_miner_rank_2_player% &7- Lvl &f%universejobs_leaderboard_miner_rank_2_level%"
  - "&e3. &f%universejobs_leaderboard_miner_rank_3_player% &7- Lvl &f%universejobs_leaderboard_miner_rank_3_level%"
```

### Chat Format (EssentialsChat/LuckPerms)

```
[%universejobs_currentjobs%] {displayname}: {message}
```

### BossBar

```
&6Mining Level: &f%universejobs_miner_level% &7| &a%universejobs_miner_progress%
```

## Internal Placeholders

These placeholders are used in UniverseJobs configuration files:

### Job Files

| Placeholder | Description |
|-------------|-------------|
| `{level}` | Current level |
| `{xp}` | Current XP |
| `{xp_required}` | XP for next level |
| `{progress}` | Progress bar |
| `{job_name}` | Job display name |
| `{job_id}` | Job ID |

### Action Files

| Placeholder | Description |
|-------------|-------------|
| `{action_type}` | Action type (Break, Kill, etc.) |
| `{action_target}` | Target material/entity |
| `{action_xp}` | XP reward |
| `{action_money}` | Money reward |
| `{action_display_name}` | Action display name |
| `{action_lore}` | Action lore lines |

### Menu Files

| Placeholder | Description |
|-------------|-------------|
| `{page}` | Current page |
| `{total_pages}` | Total pages |
| `{player}` | Player name |
| `{job_name}` | Job name |

## Custom Placeholder Format

Configure placeholder output in `config.yml`:

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
```

## Progress Bar Customization

Configure the progress bar appearance in `config.yml`:

```yaml
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
    without-percentage: "{completed_bar}{remaining_bar}"
```

## Testing Placeholders

Use PlaceholderAPI's parse command:

```bash
/papi parse me %universejobs_miner_level%
/papi parse me %universejobs_boost_xp_multiplier%
/papi parse me %universejobs_leaderboard_miner_rank_1_player%
```

## Refresh Rate

Placeholders are cached for performance:
- Player stats: Real-time
- Leaderboards: Updated every 10 seconds
- Boosts: Real-time
