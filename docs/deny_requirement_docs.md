# DENY

Blocks actions and provides custom feedback when requirements are not met.

## Description

When a player attempts an action but doesn't meet the specified requirements, deny actions execute instead of granting XP/money. This allows you to provide clear feedback and prevent unwanted behavior.

## Basic Configuration

```yaml
requirements:
  permission:
    permission: "jobs.vip"
    deny:
      message: "&cYou must be VIP!"
      sound: "ENTITY_VILLAGER_NO"
      cancel-event: true
```

## Advanced Configuration

### Message Types

```yaml
requirements:
  placeholder:
    placeholder: "%player_level%"
    operator: "greater_than"
    value: "25"
    deny:
      message:
        type: "BOSSBAR"  # CHAT, ACTIONBAR, BOSSBAR
        text: "&cLevel 25+ required!"
        duration: 100  # ticks for actionbar/bossbar
        bossbar:
          color: "RED"
          style: "SOLID"
      sound: "ENTITY_VILLAGER_NO"
      commands:
        - "tellraw {player} \"Visit /levelup to progress!\""
```

### Multiple Conditions with Individual Deny Messages

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    level_check:
      placeholder:
        placeholder: "%UniverseJobs_level_example%"
        operator: "greater_equal"
        value: "10"
      deny:
        message: "&cYou must be level 10+ in this job!"
        sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
    
    tool_check:
      item:
        material: "DIAMOND_PICKAXE"
      deny:
        message: "&cYou need a diamond pickaxe!"
        sound: "BLOCK_ANVIL_PLACE"
    
    # Global deny (fallback for conditions without individual deny)
    deny:
      message: "&c❌ Requirements not met!"
      cancel-event: true
      sound: "ENTITY_VILLAGER_NO"
      commands:
        - "eco take {player} 50"  # Penalty for failing
```

## Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `message` | String/Object | Message to display to player |
| `sound` | String | Sound effect to play |
| `cancel-event` | Boolean | Whether to cancel the event completely |
| `commands` | List | Console commands to execute |

### Message Configuration

```yaml
deny:
  message:
    type: "CHAT"      # CHAT, ACTIONBAR, BOSSBAR
    text: "&c❌ Access denied!"
    duration: 60      # Only for ACTIONBAR/BOSSBAR
    bossbar:          # Only for BOSSBAR type
      color: "RED"    # RED, BLUE, GREEN, YELLOW, PINK, PURPLE, WHITE
      style: "SOLID"  # SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
```

## Examples

### VIP Diamond Mining

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
          placeholder: "%UniverseJobs_level_mineur%"
          operator: "greater_equal"
          value: "15"
        deny:
          message: "&cYou must be level 15+ in miner job!"
      
      vip_check:
        permission:
          permission: "jobs.vip"
        deny:
          message: "&cOnly VIP players can mine diamonds!"
          sound: "ENTITY_VILLAGER_NO"
      
      tool_check:
        item:
          material: "DIAMOND_PICKAXE"
        deny:
          message: "&cDiamond pickaxe required!"
      
      # Global deny for other failures
      deny:
        message: "&c❌ Requirements not met for diamond mining!"
        cancel-event: true
```

### Location-Based Restrictions

```yaml
deep_diamond_mining:
  target: "DIAMOND_ORE"
  xp: 20.0
  requirements:
    logic: "AND"
    multiple_condition:
      depth_check:
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "16"
        deny:
          message: "&cDiamonds are only found below Y level 16!"
          sound: "BLOCK_NOTE_BLOCK_BASS"
      
      tool_check:
        item:
          material: "IRON_PICKAXE"
        deny:
          message: "&cYou need at least an iron pickaxe!"
          commands:
            - "give {player} iron_pickaxe 1"
            - "tellraw {player} \"Here's a free iron pickaxe!\""
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
    deny:
      message: "&cCrop must be fully grown!"
      sound: "BLOCK_CROP_BREAK"
      cancel-event: true
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  item:
    material: "nexo:special_tool"
    deny:
      message: "&cYou need a special Nexo tool!"
      sound: "ENTITY_VILLAGER_NO"
      commands:
        - "give {player} nexo:special_tool 1"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  placeholder:
    placeholder: "%itemsadder_block_hardness%"
    operator: "less_than"
    value: "10"
    deny:
      message: "&cThis block is too hard to break!"
      sound: "BLOCK_ANVIL_PLACE"
```
{% endtab %}
{% endtabs %}