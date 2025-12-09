# Rewards

Rewards are bonuses players can claim when reaching certain levels in a job. Each job can have its own reward configuration.

## File Structure

Rewards are stored in two types of files:

- `rewards/<job>_rewards.yml` - Reward definitions
- `gui/<job>_rewards_gui.yml` - Reward GUI layout

## Reward Properties

```yaml
# rewards/miner_rewards.yml

rewards:
  diamond_kit:                      # Unique reward ID
    name: "Diamond Mining Kit"      # Display name
    description: "A starter kit"    # Short description
    lore:                           # Additional description lines
      - "<gray>Contains useful items"
      - "<gray>for mining!"
    required-level: 10              # Level required to claim
    enabled: true                   # Enable/disable reward
    repeatable: false               # Can be claimed multiple times
    cooldown-hours: 0               # Hours between claims (if repeatable)
    permission: ""                  # Required permission (optional)
    gui-slot: 0                     # Slot in GUI (-1 for auto)

    # Reward contents
    economy-reward: 1000            # Money given (Vault)
    commands:                       # Commands to execute
      - "[console] give {player} diamond 5"
    items:                          # Items to give
      - material: DIAMOND_PICKAXE
        amount: 1
        display-name: "<aqua>Miner's Pickaxe"
        enchantments:
          EFFICIENCY: 3
          UNBREAKING: 2
```

## Reward Content Types

### Economy Reward

Give money via Vault:

```yaml
economy-reward: 5000                # Give $5000
```

### Items

Give items to the player:

```yaml
items:
  - material: DIAMOND
    amount: 10

  - material: DIAMOND_PICKAXE
    amount: 1
    display-name: "<gradient:#00FFFF:#0080FF>Lucky Pickaxe</gradient>"
    lore:
      - "<gray>A special pickaxe"
      - "<gold>+10% mining speed"
    enchantments:
      EFFICIENCY: 5
      FORTUNE: 3
      UNBREAKING: 3
    custom-model-data: 1001         # For resource packs
```

### Nexo Items

Give custom Nexo items:

```yaml
items:
  - nexo-id: "custom_pickaxe"
    amount: 1
```

### ItemsAdder Items

Give custom ItemsAdder items:

```yaml
items:
  - itemsadder-id: "namespace:custom_item"
    amount: 1
```

### Commands

Execute commands when reward is claimed:

```yaml
commands:
  - "[console] give {player} diamond 5"
  - "[console] eco give {player} 1000"
  - "[player] spawn"
  - "[message] <green>You claimed your reward!"
```

Command prefixes:
| Prefix | Description |
|--------|-------------|
| `[console]` | Run as console |
| `[player]` | Run as player |
| `[message]` | Send message to player |

Available placeholders:
| Placeholder | Description |
|-------------|-------------|
| `{player}` | Player name |
| `{uuid}` | Player UUID |
| `{job}` | Job ID |
| `{level}` | Player's level |

## Repeatable Rewards

Allow rewards to be claimed multiple times:

```yaml
rewards:
  daily_bonus:
    name: "Daily Mining Bonus"
    required-level: 1
    repeatable: true
    cooldown-hours: 24              # 24 hours between claims
    economy-reward: 500
```

## Reward Conditions

Add requirements to rewards:

```yaml
rewards:
  vip_reward:
    name: "VIP Reward"
    required-level: 50
    permission: "jobs.vip"          # Must have this permission

    requirements:                   # Additional conditions
      world: "world"
      placeholder: "%player_level% >= 100"
```

## GUI Configuration

Customize the reward GUI:

```yaml
# gui/miner_rewards_gui.yml

title: "<gradient:#FFD700:#FFA500>Miner Rewards</gradient>"
size: 54                            # Must be multiple of 9

fill-item:
  enabled: true
  material: BLACK_STAINED_GLASS_PANE
  display-name: " "
  slots: [0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53]

back-button:
  enabled: true
  material: ARROW
  display-name: "<red>Back"
  slot: 49

reward-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34]
```

## Reward Display States

Rewards can have different displays based on status:

### Available (Can Claim)

```yaml
available:
  material: EMERALD_BLOCK
  glow: true
  lore-prefix:
    - "<green>✔ Click to claim!"
```

### Locked (Level Too Low)

```yaml
locked:
  material: BARRIER
  glow: false
  lore-prefix:
    - "<red>✖ Requires level {required_level}"
```

### Claimed (Already Claimed)

```yaml
claimed:
  material: GRAY_STAINED_GLASS_PANE
  glow: false
  lore-prefix:
    - "<gray>✔ Already claimed"
```

### On Cooldown (Repeatable)

```yaml
cooldown:
  material: CLOCK
  glow: false
  lore-prefix:
    - "<yellow>⏰ Available in {time_remaining}"
```

## Complete Example

```yaml
# rewards/miner_rewards.yml

rewards:
  # Level 5 reward
  starter_kit:
    name: "<green>Starter Mining Kit"
    description: "Basic tools for new miners"
    lore:
      - "<gray>Your first mining equipment!"
    required-level: 5
    enabled: true
    repeatable: false
    gui-slot: 10
    items:
      - material: STONE_PICKAXE
        amount: 1
        enchantments:
          EFFICIENCY: 1
      - material: TORCH
        amount: 32
      - material: BREAD
        amount: 16

  # Level 10 reward
  iron_upgrade:
    name: "<white>Iron Upgrade"
    description: "Better tools"
    required-level: 10
    gui-slot: 11
    economy-reward: 500
    items:
      - material: IRON_PICKAXE
        amount: 1
        enchantments:
          EFFICIENCY: 2
          UNBREAKING: 1

  # Level 25 reward
  diamond_tools:
    name: "<aqua>Diamond Tools"
    description: "Professional mining equipment"
    required-level: 25
    gui-slot: 12
    economy-reward: 2500
    items:
      - material: DIAMOND_PICKAXE
        amount: 1
        display-name: "<aqua>Miner's Diamond Pickaxe"
        enchantments:
          EFFICIENCY: 4
          UNBREAKING: 3
          FORTUNE: 2

  # Level 50 milestone
  master_miner:
    name: "<gold>Master Miner Package"
    description: "For dedicated miners"
    required-level: 50
    gui-slot: 13
    economy-reward: 10000
    commands:
      - "[console] lp user {player} permission set jobs.miner.master"
    items:
      - material: NETHERITE_PICKAXE
        amount: 1
        display-name: "<gradient:#FFD700:#FFA500>Master's Pickaxe</gradient>"
        lore:
          - "<gray>The ultimate mining tool"
          - ""
          - "<gold>★ Master Miner Reward ★"
        enchantments:
          EFFICIENCY: 5
          FORTUNE: 3
          UNBREAKING: 3
          MENDING: 1

  # Daily repeatable
  daily_bonus:
    name: "<yellow>Daily Mining Bonus"
    description: "Claim every 24 hours"
    required-level: 1
    repeatable: true
    cooldown-hours: 24
    gui-slot: 22
    economy-reward: 250
    items:
      - material: COAL
        amount: 16
```

## Commands

| Command | Description |
|---------|-------------|
| `/jobs rewards` | Open rewards GUI |
| `/jobs claim <reward_id>` | Claim a specific reward |
| `/jobs admin rewards reset <player> <job>` | Reset player's claimed rewards |
