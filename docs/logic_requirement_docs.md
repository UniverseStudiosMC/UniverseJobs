# LOGIC

Controls how multiple requirements are combined using AND/OR logical operations.

## Description

Logic requirements define how multiple conditions are evaluated together. Using **AND** logic requires all conditions to be met, while **OR** logic requires only one condition to be met. This allows for complex conditional structures and flexible access control.

## Basic Configuration

```yaml
requirements:
  logic: "AND"  # or "OR"
  permission:
    permission: "jobs.mining"
  world:
    worlds: ["mining_world"]
```

## Logic Types

### AND Logic

**ALL conditions must be true** for the requirement to pass.

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    vip_check:
      permission:
        permission: "jobs.vip"
    level_check:
      placeholder:
        placeholder: "%player_level%"
        operator: "greater_than"
        value: "25"
    tool_check:
      item:
        material: "DIAMOND_PICKAXE"
  # All three conditions must be met
```

### OR Logic

**ANY condition can be true** for the requirement to pass.

```yaml
requirements:
  logic: "OR"
  multiple_condition:
    vip_access:
      permission:
        permission: "jobs.vip"
    admin_access:
      permission:
        permission: "admin.mining"
    event_access:
      placeholder:
        placeholder: "%server_event%"
        operator: "equals"
        value: "double_xp"
  # Only one condition needs to be met
```

## Advanced Configuration

### Mixed Logic with Individual Messages

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    permission_check:
      permission:
        permission: "jobs.mining"
      deny:
        message: "&cYou need mining permission!"
    
    access_methods:
      logic: "OR"  # Nested OR within AND
      vip_method:
        permission:
          permission: "jobs.vip"
        accept:
          message: "&6VIP access granted!"
      
      level_method:
        placeholder:
          placeholder: "%player_level%"
          operator: "greater_than"
          value: "50"
        accept:
          message: "&aHigh level access granted!"
    
    # Global messages
    accept:
      message: "&a✅ All requirements met!"
      sound: "ENTITY_PLAYER_LEVELUP"
    deny:
      message: "&c❌ Requirements not met!"
      sound: "ENTITY_VILLAGER_NO"
```

### Complex Nested Groups

```yaml
requirements:
  logic: "AND"
  
  # Basic requirement
  permission:
    permission: "jobs.mining"
  
  # Complex access control
  multiple_condition:
    location_and_time:
      logic: "AND"
      world:
        worlds: ["mining_world"]
      time:
        start: "6000"   # Day time
        end: "18000"
    
    player_qualification:
      logic: "OR"
      vip_player:
        logic: "AND"
        permission:
          permission: "jobs.vip"
        placeholder:
          placeholder: "%vault_balance%"
          operator: "greater_than"
          value: "1000"
      
      experienced_player:
        logic: "AND"
        placeholder:
          placeholder: "%UniverseJobs_level_miner%"
          operator: "greater_than"
          value: "75"
        item:
          material: "NETHERITE_PICKAXE"
```

## Logic Behavior

| Logic Type | Behavior | Short-Circuit | Result |
|------------|----------|---------------|---------|
| **AND** | All conditions must be true | Stops on first failure | Deny from first failed condition |
| **OR** | Any condition can be true | Stops on first success | Accept from first successful condition |

### Performance Optimization

- **Short-circuit evaluation**: Logic operations stop as soon as the result is determined
- **AND logic**: Stops checking when the first condition fails
- **OR logic**: Stops checking when the first condition succeeds

## Examples

### VIP or Experience-Based Access

```yaml
diamond_mining:
  target: "DIAMOND_ORE"
  xp: 50.0
  money: 200.0
  requirements:
    logic: "OR"  # Either VIP OR experienced
    multiple_condition:
      vip_access:
        permission:
          permission: "jobs.vip.diamond"
        accept:
          message: "&6VIP diamond mining access!"
          commands:
            - "effect give {player} haste 300 1"
      
      experienced_access:
        logic: "AND"
        placeholder:
          placeholder: "%UniverseJobs_level_miner%"
          operator: "greater_equal"
          value: "50"
        item:
          material: ["DIAMOND_PICKAXE", "NETHERITE_PICKAXE"]
        accept:
          message: "&aExperienced miner access!"
      
      # Global messages
      accept:
        message: "&a💎 Diamond mining unlocked!"
        sound: "BLOCK_NOTE_BLOCK_CHIME"
      deny:
        message: "&cInsufficient qualifications for diamond mining!"
        cancel-event: true
```

### Time and Location Restrictions

```yaml
night_fishing_bonus:
  target: "SALMON"
  xp: 15.0
  requirements:
    logic: "AND"  # All conditions must be met
    multiple_condition:
      time_requirement:
        time:
          start: "13000"  # Night time
          end: "23000"
        deny:
          message: "&cThis bonus is only available at night!"
      
      location_requirement:
        logic: "OR"  # Any of these locations
        ocean_biome:
          biome:
            biomes: ["OCEAN", "DEEP_OCEAN"]
        special_region:
          region:
            region: "fishing_zone"
        deny:
          message: "&cYou must be in ocean or fishing zone!"
      
      weather_bonus:
        weather:
          weather: "RAIN"
        accept:
          message: "&3Rainy night fishing! Double bonus!"
          commands:
            - "eco give {player} 50"
      
      # Global configuration
      accept:
        message: "&9🌙 Perfect night fishing conditions!"
        sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
```

### Multi-Tier Access System

```yaml
exclusive_mining:
  target: "ANCIENT_DEBRIS"
  xp: 500.0
  money: 1000.0
  requirements:
    logic: "AND"
    multiple_condition:
      basic_access:
        permission:
          permission: "jobs.mining.advanced"
        deny:
          message: "&cAdvanced mining permission required!"
      
      tier_access:
        logic: "OR"  # Multiple ways to gain access
        tier_1_vip:
          permission:
            permission: "vip.platinum"
          accept:
            message: "&bPlatinum VIP access!"
        
        tier_2_experienced:
          logic: "AND"
          placeholder:
            placeholder: "%UniverseJobs_level_miner%"
            operator: "greater_equal"
            value: "100"
          placeholder2:
            placeholder: "%vault_balance%"
            operator: "greater_than"
            value: "10000"
          accept:
            message: "&6Master miner access!"
        
        tier_3_special:
          item:
            material: "NETHERITE_PICKAXE"
            enchantments:
              - "efficiency:5"
              - "unbreaking:3"
          accept:
            message: "&5Legendary tool access!"
      
      # Global accept/deny
      accept:
        message: "&c🔥 EXCLUSIVE: Ancient debris mining unlocked!"
        commands:
          - "broadcast &6{player} &cis now mining ancient debris!"
          - "effect give {player} fire_resistance 600 1"
      deny:
        message: "&c🚫 Exclusive mining access denied!"
        cancel-event: true
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  logic: "AND"
  multiple_condition:
    crop_ready:
      placeholder:
        placeholder: "%customcrops_stage%"
        operator: "equals"
        value: "4"
    tool_check:
      item:
        material: "customcrops:special_hoe"
  accept:
    message: "&aPerfect CustomCrops harvest!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  logic: "OR"
  multiple_condition:
    nexo_tool:
      item:
        material: "nexo:master_pickaxe"
    nexo_permission:
      permission:
        permission: "nexo.vip"
  accept:
    message: "&6Nexo bonus activated!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  logic: "AND"
  multiple_condition:
    ia_block:
      placeholder:
        placeholder: "%itemsadder_block_type%"
        operator: "equals"
        value: "special_ore"
    ia_tool:
      item:
        material: "itemsadder:mining_tool"
  accept:
    message: "&bItemsAdder mining bonus!"
```
{% endtab %}
{% endtabs %}