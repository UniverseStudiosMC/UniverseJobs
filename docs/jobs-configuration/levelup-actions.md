# Level Up Actions System

## Overview

UniverseJobs provides a powerful and 100% customizable level up action system. When players level up in a job, you can trigger any combination of actions, from simple messages to complex reward systems.

The system is built on top of existing UniverseJobs components, ensuring optimal performance and consistency with the rest of the plugin.

## Configuration

Level up actions are configured directly in each job's configuration file under the `levelup-actions` section.

```yaml
levelup-actions:
  action_name:
    type: "action_type"
    # Action-specific configuration
```

## Level Targeting Options

Each action can be configured to trigger at specific levels:

### Specific Levels
```yaml
my_action:
  type: "message"
  levels: [5, 10, 25, 50, 100]
  message: "Congratulations on reaching level {level}!"
```

### Level Range
```yaml
my_action:
  type: "sound"
  min-level: 10
  max-level: 50
  sound: "ENTITY_PLAYER_LEVELUP"
```

### Level Intervals
```yaml
my_action:
  type: "money"
  min-level: 5
  level-interval: 5  # Every 5 levels starting from level 5
  amount: 100
```

## Available Action Types

### 1. Message Action
Sends messages to the player who leveled up using the existing message system.

```yaml
welcome_message:
  type: "message"
  levels: [1]
  messages:
    - "&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
    - "&6&l✦ WELCOME TO THE {job} JOB! ✦"
    - "&7You are now a level &e{level} &7{job}!"
    - "&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
```

**Options:**
- `message` or `messages`: Single message or list of messages
- Supports color codes with `&`
- All placeholders supported

### 2. Command Action
Executes commands when a player levels up using the existing command system.

```yaml
tool_reward:
  type: "command"
  levels: [5, 25]
  commands:
    - "give {player} iron_pickaxe 1"
    - "tellraw {player} {\"text\":\"🎁 Level {level} reward!\",\"color\":\"gold\"}"
```

**Options:**
- `command` or `commands`: Single command or list of commands
- Commands are executed as console by default
- Supports all placeholders and command types

### 3. Sound Action
Plays sounds to the player using the existing sound system.

```yaml
level_sound:
  type: "sound"
  min-level: 1
  sound: "ENTITY_PLAYER_LEVELUP"
```

**Options:**
- `sound`: Bukkit sound name
- Falls back to ENTITY_PLAYER_LEVELUP if sound not found

### 4. Title Action
Displays title and subtitle on the player's screen.

```yaml
level_title:
  type: "title"
  min-level: 2
  title: "&6&lLEVEL UP!"
  subtitle: "&7{job} Level &e{level}"
  fade-in: 10
  stay: 50
  fade-out: 20
```

**Options:**
- `title`: Main title text
- `subtitle`: Subtitle text
- `fade-in`: Fade in duration (ticks, default: 10)
- `stay`: Display duration (ticks, default: 70)
- `fade-out`: Fade out duration (ticks, default: 20)

### 5. Particle Action
Spawns particle effects around the player.

```yaml
level_particles:
  type: "particle"
  min-level: 1
  particle: "FLAME"
  count: 25
```

**Options:**
- `particle`: Particle type name
- `count`: Number of particles (default: 20)
- Falls back to FLAME particle if specified particle not found

### 6. Broadcast Action
Sends a message to all online players.

```yaml
milestone_broadcast:
  type: "broadcast"
  levels: [20, 40, 60, 80, 100]
  messages:
    - "&6⚒ {player} &7has reached level &e{level} &7in the {job} job! ⚒"
```

**Options:**
- `message` or `messages`: Message(s) to broadcast
- All placeholders supported

### 7. Boss Bar Action (Simplified)
Shows a temporary notification using title system.

```yaml
level_notification:
  type: "bossbar"
  min-level: 1
  title: "&6Level {level} achieved!"
```

**Note:** Currently implemented as title display. Full boss bar functionality can be added if needed.

## Placeholders

All actions support the following placeholders:

| Placeholder | Description |
|------------|-------------|
| `{player}` | Player's name |
| `{job}` | Job display name |
| `{level}` or `{newlevel}` | New level |
| `{oldlevel}` | Previous level |
| `{totalxp}` | Total XP in the job |
| `{xpgained}` | XP that triggered the level up |

## Complete Example

Here's a comprehensive example from the included miner.yml showing the simplified system:

```yaml
# In miner.yml
levelup-actions:
  # Welcome message for new miners
  welcome_message:
    type: "message"
    levels: [1]
    messages:
      - "&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
      - "&6&l✦ WELCOME TO THE MINER JOB! ✦"
      - "&7You are now a level &e{level} &7miner!"
      - "&7Mine ores and stones to gain experience!"
      - "&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
  
  # Basic level up effects for all levels
  level_up_sound:
    type: "sound"
    min-level: 1
    sound: "ENTITY_PLAYER_LEVELUP"
  
  level_up_title:
    type: "title"
    min-level: 2
    title: "&6&lLEVEL UP!"
    subtitle: "&7Miner Level &e{level}"
    fade-in: 10
    stay: 50
    fade-out: 20
  
  level_up_particles:
    type: "particle"
    min-level: 1
    particle: "FLAME"
    count: 25
  
  # Tool rewards using commands
  iron_pickaxe_reward:
    type: "command"
    levels: [5]
    commands:
      - "give {player} iron_pickaxe 1"
      - "tellraw {player} {\"text\":\"🎁 You received an Iron Pickaxe for reaching level 5!\",\"color\":\"gold\"}"
  
  diamond_pickaxe_reward:
    type: "command"
    levels: [25]
    commands:
      - "give {player} diamond_pickaxe 1"
      - "tellraw {player} {\"text\":\"💎 You received a Diamond Pickaxe for reaching level 25!\",\"color\":\"aqua\"}"
  
  # Money rewards every 10 levels
  money_rewards:
    type: "command"
    min-level: 10
    level-interval: 10
    commands:
      - "eco give {player} {level}00"
      - "tellraw {player} {\"text\":\"💰 You earned ${level}00 for reaching level {level}!\",\"color\":\"green\"}"
  
  # Major milestone announcements
  milestone_broadcast:
    type: "broadcast"
    levels: [20, 40, 60, 80, 100]
    messages:
      - "&6⚒ {player} &7has reached level &e{level} &7in the Miner job! ⚒"
  
  # Special rewards for high levels
  master_miner:
    type: "command"
    levels: [50]
    commands:
      - "broadcast &6🏆 {player} &7has become a &eMaster Miner&7! 🏆"
      - "give {player} diamond 10"
      - "give {player} emerald 5"
```

## Performance Considerations

- **Optimized System**: Built on existing UniverseJobs components for maximum efficiency
- **Reused Components**: Uses existing ConditionResult, MessageUtils, and command systems
- **Minimal Overhead**: Simple action execution without complex abstractions
- **Error Handling**: Failed actions won't prevent other actions from executing
- **Graceful Fallbacks**: Unknown sounds/particles fall back to safe defaults

## System Architecture

The simplified level up action system leverages existing UniverseJobs infrastructure:

- **Messages**: Uses existing `ConditionResult.execute()` for consistent message handling
- **Commands**: Reuses the established command execution system with placeholder support
- **Sounds**: Integrates with the existing sound system with automatic fallbacks
- **Integration**: Works seamlessly with existing reward and messaging systems

## Configuration Tips

### Efficient Action Design
```yaml
# Good: Combine related actions
level_5_rewards:
  type: "command"
  levels: [5]
  commands:
    - "give {player} iron_pickaxe 1"
    - "tellraw {player} {\"text\":\"🎁 Level 5 reward package!\",\"color\":\"gold\"}"
    - "playsound minecraft:entity.player.levelup player {player}"

# Less efficient: Separate everything
level_5_item:
  type: "command"
  levels: [5]
  commands: ["give {player} iron_pickaxe 1"]
level_5_message:
  type: "message"
  levels: [5]
  messages: ["Level 5 reward!"]
level_5_sound:
  type: "sound"
  levels: [5]
  sound: "ENTITY_PLAYER_LEVELUP"
```

### Using Command Actions Effectively
The `command` action type is the most versatile and can handle most use cases:

```yaml
comprehensive_reward:
  type: "command"
  levels: [10, 20, 30]
  commands:
    # Give items
    - "give {player} diamond 5"
    # Economy integration
    - "eco give {player} 1000"
    # Permissions (if you have LuckPerms)
    - "lp user {player} permission set jobs.bonus.{level} true"
    # Custom messages with JSON
    - "tellraw {player} {\"text\":\"🎉 Level {level} Achievement!\",\"color\":\"gold\",\"bold\":true}"
    # Titles via command
    - "title {player} title {\"text\":\"LEVEL UP!\",\"color\":\"gold\"}"
    # Effects via command
    - "effect give {player} minecraft:regeneration 30 1"
```

## Extended Examples

See the included job files for complete, production-ready examples:
- **miner.yml**: Mining progression with tool rewards and broadcasts
- **farmer.yml**: Agricultural progression with starter packs and food rewards  
- **hunter.yml**: Combat progression with weapon/armor upgrades and survival kits