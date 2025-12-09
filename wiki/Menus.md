# Menus

UniverseJobs features a fully customizable GUI system. All menus are configured in `plugins/UniverseJobs/menus/`.

## Menu Files

| File | Description |
|------|-------------|
| `main-menu.yml` | Main jobs list menu |
| `job-menu.yml` | Individual job details |
| `actions-menu.yml` | Job actions list |
| `rankings-menu.yml` | Leaderboard display |
| `boost-manager.yml` | Active boosts view |

## Basic Structure

```yaml
# menus/main-menu.yml

# Menu title (supports MiniMessage)
title: "<gradient:#FFD700:#FFA500>Jobs Menu</gradient>"

# Menu size (9, 18, 27, 36, 45, or 54)
size: 54

# Pagination settings
pagination:
  enabled: true
  items-per-page: 21

# Fill empty slots
fill-item:
  enabled: true
  material: BLACK_STAINED_GLASS_PANE
  display-name: " "
  slots: [0, 1, 2, 3, 4, 5, 6, 7, 8]

# Content slots (where jobs/items appear)
content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34]

# Static items (buttons, decorations)
static-items:
  info:
    material: BOOK
    display-name: "<yellow>Information"
    lore:
      - "<gray>Your current jobs: <white>{equipped_jobs}"
      - "<gray>Maximum jobs: <white>{max_jobs}"
    slots: [4]

# Navigation items
navigation:
  previous-page:
    enabled: true
    material: ARROW
    display-name: "<yellow>← Previous Page"
    slots: [45]
  next-page:
    enabled: true
    material: ARROW
    display-name: "<yellow>Next Page →"
    slots: [53]
  back:
    enabled: true
    material: BARRIER
    display-name: "<red>Close"
    slots: [49]
```

## Item Configuration

### Basic Item

```yaml
item:
  material: DIAMOND_PICKAXE
  display-name: "<aqua>Mining Job"
  amount: 1
```

### With Lore

```yaml
item:
  material: DIAMOND_PICKAXE
  display-name: "<aqua>Mining Job"
  lore:
    - "<gray>Level: <white>{level}"
    - "<gray>XP: <white>{xp}/{xp_required}"
    - ""
    - "<yellow>Click to view details"
```

### With Enchantment Glow

```yaml
item:
  material: DIAMOND_PICKAXE
  display-name: "<aqua>Mining Job"
  glow: true
```

### With Custom Model Data

```yaml
item:
  material: PAPER
  display-name: "<gold>Custom Item"
  custom-model-data: 1001
```

### Multiple Slots

```yaml
item:
  material: BLACK_STAINED_GLASS_PANE
  display-name: " "
  slots: [0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53]
```

## Main Menu Configuration

```yaml
# menus/main-menu.yml

title: "<gradient:#FFD700:#FFA500>Available Jobs</gradient>"
size: 54

pagination:
  enabled: true
  items-per-page: 21

fill-item:
  enabled: true
  material: BLACK_STAINED_GLASS_PANE
  display-name: " "
  slots: [0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 50, 51, 52, 53]

content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34]

# Job item format
job-item-format:
  display-name: "{job_name}"
  lore:
    - "{job_description}"
    - ""
    - "<gray>Level: <white>{level}<gray>/<white>{max_level}"
    - "<gray>XP: <white>{xp}<gray>/<white>{xp_required}"
    - "{progress}"
    - ""
    - "<yellow>Click for details"

  # Different states
  joined:
    lore-suffix:
      - ""
      - "<green>✔ Currently working"
  not-joined:
    lore-suffix:
      - ""
      - "<gray>Click to join"
  locked:
    material: BARRIER
    lore-suffix:
      - ""
      - "<red>✖ No permission"

static-items:
  header:
    material: NETHER_STAR
    display-name: "<gold>Your Jobs"
    lore:
      - "<gray>Jobs: <white>{equipped_jobs}<gray>/<white>{max_jobs}"
    slots: [4]
    glow: true

  rankings:
    material: GOLD_INGOT
    display-name: "<yellow>Leaderboards"
    lore:
      - "<gray>View job rankings"
    slots: [49]
    commands:
      - "[close]"
      - "[player] jobs top"

navigation:
  previous-page:
    enabled: true
    material: ARROW
    display-name: "<yellow>← Page {page}"
    slots: [45]
  next-page:
    enabled: true
    material: ARROW
    display-name: "<yellow>Page {page} →"
    slots: [53]
```

## Job Menu Configuration

```yaml
# menus/job-menu.yml

title: "<gradient:#FFD700:#FFA500>{job_name}</gradient>"
size: 54

fill-item:
  enabled: true
  material: GRAY_STAINED_GLASS_PANE
  display-name: " "

static-items:
  job-info:
    material: BOOK
    display-name: "<gold>{job_name}"
    lore:
      - "{job_description_lines}"
      - ""
      - "<gray>Level: <white>{level}<gray>/<white>{max_level}"
      - "<gray>XP: <white>{xp}<gray>/<white>{xp_required}"
      - "{progress}"
    slots: [4]

  actions:
    material: DIAMOND_PICKAXE
    display-name: "<aqua>View Actions"
    lore:
      - "<gray>See all job actions"
      - "<gray>and their rewards"
    slots: [20]
    commands:
      - "[open] actions-menu {job_id}"

  rewards:
    material: CHEST
    display-name: "<gold>Rewards"
    lore:
      - "<gray>Claim level rewards"
    slots: [22]
    commands:
      - "[open] rewards {job_id}"

  leaderboard:
    material: GOLD_INGOT
    display-name: "<yellow>Leaderboard"
    lore:
      - "<gray>Your rank: <white>#{rank}"
    slots: [24]
    commands:
      - "[open] rankings-menu {job_id}"

  join-leave:
    joined:
      material: RED_WOOL
      display-name: "<red>Leave Job"
      lore:
        - "<gray>Click to leave this job"
      slots: [40]
      commands:
        - "[player] jobs leave {job_id}"
        - "[close]"
    not-joined:
      material: GREEN_WOOL
      display-name: "<green>Join Job"
      lore:
        - "<gray>Click to join this job"
      slots: [40]
      commands:
        - "[player] jobs join {job_id}"
        - "[close]"

navigation:
  back:
    enabled: true
    material: ARROW
    display-name: "<red>← Back"
    slots: [45]
```

## Actions Menu Configuration

```yaml
# menus/actions-menu.yml

title: "<gradient:#FFD700:#FFA500>{job_name} Actions</gradient>"
size: 54

pagination:
  enabled: true
  items-per-page: 28

fill-item:
  enabled: true
  material: BLACK_STAINED_GLASS_PANE
  display-name: " "

content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43]

# Action item format
action-item-format:
  display-name: "{action_display_name}"
  lore:
    - "<gray>Type: {action_type}"
    - ""
    - "<green>+{action_xp} XP {action_xp_multiplier}"
    - "<gold>+${action_money} {action_money_multiplier}"
    - "{action_lore}"

  # Hide lines when value is 0
  hide-when-no-money: [4]  # Line numbers to hide
  hide-when-no-xp: [3]

static-items:
  header:
    material: PAPER
    display-name: "<gold>{job_name} Actions"
    lore:
      - "<gray>Total actions: <white>{total_actions}"
    slots: [4]

navigation:
  previous-page:
    enabled: true
    material: ARROW
    display-name: "<yellow>← Previous"
    slots: [45]
  next-page:
    enabled: true
    material: ARROW
    display-name: "<yellow>Next →"
    slots: [53]
  back:
    enabled: true
    material: BARRIER
    display-name: "<red>← Back to Job"
    slots: [49]
```

## Rankings Menu Configuration

```yaml
# menus/rankings-menu.yml

title: "<gradient:#FFD700:#FFA500>Leaderboard</gradient>"
size: 54

fill-item:
  enabled: true
  material: BLACK_STAINED_GLASS_PANE
  display-name: " "

# Player head display for rankings
ranking-item:
  use-player-head: true
  display-name: "<gold>#{rank} <white>{player_name}"
  lore:
    - "<gray>Level: <white>{level}"
    - "<gray>XP: <white>{xp}"

# Top 3 special formatting
top-ranks:
  1:
    glow: true
    display-name: "<gold>🥇 #{rank} {player_name}"
  2:
    glow: true
    display-name: "<gray>🥈 #{rank} {player_name}"
  3:
    glow: true
    display-name: "<#CD7F32>🥉 #{rank} {player_name}"

content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34]

static-items:
  your-rank:
    material: PLAYER_HEAD
    display-name: "<aqua>Your Rank"
    lore:
      - "<gray>Rank: <white>#{player_rank}"
      - "<gray>Level: <white>{level}"
    slots: [49]
    use-player-head: true

navigation:
  global:
    material: NETHER_STAR
    display-name: "<gold>Global Rankings"
    slots: [3]
  per-job:
    material: DIAMOND
    display-name: "<aqua>Job Rankings"
    slots: [5]
  back:
    material: ARROW
    display-name: "<red>← Back"
    slots: [45]
```

## Item Commands

Execute commands when items are clicked:

```yaml
item:
  material: EMERALD
  display-name: "Click Me"
  commands:
    - "[console] give {player} diamond 1"
    - "[player] spawn"
    - "[message] <green>You clicked!"
    - "[close]"
    - "[open] main-menu"
```

| Prefix | Description |
|--------|-------------|
| `[console]` | Run as console |
| `[player]` | Run as player |
| `[message]` | Send message |
| `[close]` | Close menu |
| `[open]` | Open another menu |

## Sounds

Add sounds to menu interactions:

```yaml
sounds:
  open: BLOCK_CHEST_OPEN
  close: BLOCK_CHEST_CLOSE
  click: UI_BUTTON_CLICK
  error: ENTITY_VILLAGER_NO
  success: ENTITY_PLAYER_LEVELUP
```

## Placeholders

Available in menu configurations:

| Placeholder | Description |
|-------------|-------------|
| `{player}` | Player name |
| `{job_id}` | Job ID |
| `{job_name}` | Job display name |
| `{level}` | Current level |
| `{max_level}` | Maximum level |
| `{xp}` | Current XP |
| `{xp_required}` | XP for next level |
| `{progress}` | Progress bar |
| `{rank}` | Player's rank |
| `{equipped_jobs}` | Number of joined jobs |
| `{max_jobs}` | Maximum jobs allowed |
| `{page}` | Current page |
| `{total_pages}` | Total pages |

## MiniMessage Formatting

All text supports MiniMessage format:

```yaml
display-name: "<gradient:#FF0000:#00FF00>Rainbow Text</gradient>"
lore:
  - "<bold><red>Bold Red</bold>"
  - "<italic><blue>Italic Blue</italic>"
  - "<underlined>Underlined</underlined>"
  - "<strikethrough>Strikethrough</strikethrough>"
  - "<rainbow>Rainbow!</rainbow>"
  - "<hover:show_text:'Hover text'>Hover me!</hover>"
```

## Conditional Display

Show different items based on conditions:

```yaml
join-button:
  conditions:
    joined:
      material: RED_WOOL
      display-name: "<red>Leave"
    not-joined:
      material: GREEN_WOOL
      display-name: "<green>Join"
    locked:
      material: BARRIER
      display-name: "<gray>Locked"
```
