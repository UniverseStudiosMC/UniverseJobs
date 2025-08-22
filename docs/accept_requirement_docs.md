# ACCEPT

Provides custom rewards and feedback when requirements are successfully met.

## Description

Accept requirements define what happens when a condition is **successfully met**. They work together with deny requirements to provide complete control over both success and failure scenarios, allowing you to reward players and provide positive feedback.

## Basic Configuration

```yaml
requirements:
  permission:
    permission: "jobs.vip"
    accept:
      message: "&aVIP access granted!"
      sound: "ENTITY_PLAYER_LEVELUP"
      commands:
        - "effect give {player} speed 60 1"
```

## Advanced Configuration

### Message Types

```yaml
requirements:
  placeholder:
    placeholder: "%player_level%"
    operator: "greater_than"
    value: "25"
    accept:
      message:
        type: "BOSSBAR"  # CHAT, ACTIONBAR, BOSSBAR
        text: "&6Level 25+ bonus activated!"
        duration: 100  # ticks for actionbar/bossbar
        bossbar:
          color: "YELLOW"
          style: "SOLID"
      sound: "ENTITY_PLAYER_LEVELUP"
      commands:
        - "eco give {player} 200"
        - "particle effect {player} firework"
```

### Multiple Conditions with Individual Accept Messages

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    level_check:
      placeholder:
        placeholder: "%UniverseJobs_level_example%"
        operator: "greater_equal"
        value: "10"
      accept:
        message: "&aLevel requirement met!"
        sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
    
    vip_check:
      permission:
        permission: "jobs.vip"
      accept:
        message: "&6VIP bonus activated!"
        commands:
          - "effect give {player} haste 120 1"
    
    # Global accept (triggers when ALL conditions are met)
    accept:
      message: "&a✅ All requirements met! Bonus reward!"
      commands:
        - "eco give {player} 500"
        - "broadcast {player} earned a special bonus!"
```

## Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `message` | String/Object | Message to display to player |
| `sound` | String | Sound effect to play |
| `commands` | List | Console commands to execute |
| `cancel-event` | Boolean | Whether to cancel the event (rarely used) |

### Message Configuration

```yaml
accept:
  message:
    type: "CHAT"      # CHAT, ACTIONBAR, BOSSBAR
    text: "&a✅ Success!"
    duration: 60      # Only for ACTIONBAR/BOSSBAR
    bossbar:          # Only for BOSSBAR type
      color: "GREEN"  # RED, BLUE, GREEN, YELLOW, PINK, PURPLE, WHITE
      style: "SOLID"  # SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
```

## Examples

### VIP Mining Bonuses

```yaml
mine_diamond_vip:
  target: "DIAMOND_ORE"
  xp: 30.0
  money: 100.0
  requirements:
    logic: "AND"
    multiple_condition:
      level_check:
        placeholder:
          placeholder: "%UniverseJobs_level_miner%"
          operator: "greater_equal"
          value: "15"
        accept:
          message: "&aProfessional miner bonus!"
          commands:
            - "eco give {player} 50"
      
      vip_check:
        permission:
          permission: "jobs.vip"
        accept:
          message: "&6VIP diamond mining bonus!"
          sound: "ENTITY_PLAYER_LEVELUP"
          commands:
            - "give {player} diamond 1"
      
      tool_check:
        item:
          material: "DIAMOND_PICKAXE"
        accept:
          message: "&bProper tool bonus!"
      
      # Global accept for meeting all requirements
      accept:
        message: "&a🌟 Perfect mining conditions! Triple bonus!"
        commands:
          - "eco give {player} 200"
          - "effect give {player} haste 300 2"
```

### Time-Based Bonuses

```yaml
night_fishing:
  target: "COD"
  xp: 10.0
  requirements:
    logic: "AND"
    multiple_condition:
      time_check:
        time:
          start: "18000"  # Night time
          end: "6000"
        accept:
          message: "&9Night fishing bonus!"
          sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
          commands:
            - "eco give {player} 25"
      
      weather_check:
        weather:
          weather: "RAIN"
        accept:
          message: "&3Rainy weather bonus!"
          commands:
            - "give {player} emerald 1"
      
      # Global accept for perfect conditions
      accept:
        message: "&a🌙 Perfect night fishing! Double XP!"
        commands:
          - "broadcast {player} caught fish under perfect conditions!"
```

### Achievement System

```yaml
master_builder:
  target: "DIAMOND_BLOCK"
  xp: 50.0
  requirements:
    logic: "AND"
    multiple_condition:
      blocks_placed:
        placeholder:
          placeholder: "%player_blocks_placed%"
          operator: "greater_than"
          value: "1000"
        accept:
          message: "&6Master Builder achievement unlocked!"
          sound: "UI_TOAST_CHALLENGE_COMPLETE"
          commands:
            - "title {player} title \"&6Master Builder\""
            - "title {player} subtitle \"&eYou've placed 1000+ blocks!\""
            - "eco give {player} 1000"
            - "broadcast &6{player} &eis now a Master Builder!"
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  placeholder:
    placeholder: "%customcrops_crop_stage%"
    operator: "equals"
    value: "4"
    accept:
      message: "&aPerfect harvest!"
      sound: "ENTITY_PLAYER_LEVELUP"
      commands:
        - "give {player} emerald 2"
        - "effect give {player} saturation 10 1"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  item:
    material: "nexo:master_tool"
    accept:
      message: "&6Master tool bonus activated!"
      sound: "BLOCK_ENCHANTMENT_TABLE_USE"
      commands:
        - "effect give {player} efficiency 300 3"
        - "eco give {player} 100"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  placeholder:
    placeholder: "%itemsadder_tool_durability%"
    operator: "greater_than"
    value: "80"
    accept:
      message: "&aWell-maintained tool bonus!"
      sound: "BLOCK_ANVIL_USE"
      commands:
        - "eco give {player} 25"
```
{% endtab %}
{% endtabs %}