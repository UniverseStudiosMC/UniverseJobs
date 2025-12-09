# Conditions

Conditions allow you to restrict when actions can be performed or rewards can be claimed. UniverseJobs supports 7 condition types.

## Condition Types

| Type | Description |
|------|-------------|
| `permission` | Check if player has a permission |
| `world` | Check if player is in a specific world |
| `biome` | Check if player is in a specific biome |
| `time` | Check in-game time |
| `weather` | Check weather conditions |
| `placeholder` | Check PlaceholderAPI values |
| `item` | Check item in player's hand |

## Basic Syntax

Conditions are added to actions using the `requirements` property:

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 50
      money: 10
      requirements:
        world: "world"
        permission: "jobs.mining.diamond"
```

## Permission Condition

Check if the player has a specific permission:

```yaml
requirements:
  permission: "universejobs.premium"
```

Multiple permissions (AND logic):

```yaml
requirements:
  permission:
    - "universejobs.premium"
    - "universejobs.vip"
```

## World Condition

Restrict actions to specific worlds:

```yaml
requirements:
  world: "world"
```

Multiple worlds (OR logic):

```yaml
requirements:
  world:
    - "world"
    - "world_nether"
```

## Biome Condition

Restrict actions to specific biomes:

```yaml
requirements:
  biome: "DESERT"
```

Multiple biomes:

```yaml
requirements:
  biome:
    - "DESERT"
    - "BADLANDS"
    - "SAVANNA"
```

### Common Biome Names

| Biome | Description |
|-------|-------------|
| `PLAINS` | Plains biome |
| `DESERT` | Desert biome |
| `FOREST` | Forest biome |
| `TAIGA` | Taiga biome |
| `OCEAN` | Ocean biome |
| `DEEP_OCEAN` | Deep ocean biome |
| `JUNGLE` | Jungle biome |
| `SWAMP` | Swamp biome |
| `MOUNTAINS` | Mountain biome |
| `NETHER_WASTES` | Nether wastes |
| `THE_END` | The End |

## Time Condition

Restrict actions based on in-game time (0-24000 ticks):

```yaml
requirements:
  time:
    from: 0                         # Sunrise (6:00 AM)
    to: 12000                       # Sunset (6:00 PM)
```

### Time Reference

| Time | Ticks | Description |
|------|-------|-------------|
| 6:00 AM | 0 | Sunrise |
| 12:00 PM | 6000 | Noon |
| 6:00 PM | 12000 | Sunset |
| 12:00 AM | 18000 | Midnight |

Night time example:

```yaml
requirements:
  time:
    from: 13000                     # After sunset
    to: 23000                       # Before sunrise
```

## Weather Condition

Restrict actions based on weather:

```yaml
requirements:
  weather: "RAIN"
```

### Weather Values

| Value | Description |
|-------|-------------|
| `CLEAR` | Clear weather |
| `RAIN` | Raining |
| `THUNDER` | Thunderstorm |

## Placeholder Condition

Use PlaceholderAPI to create complex conditions:

```yaml
requirements:
  placeholder: "%player_level% >= 10"
```

### Operators

| Operator | Description |
|----------|-------------|
| `>` | Greater than |
| `<` | Less than |
| `>=` | Greater than or equal |
| `<=` | Less than or equal |
| `==` | Equal to |
| `!=` | Not equal to |

### Examples

```yaml
# Player level check
requirements:
  placeholder: "%player_level% >= 50"

# Vault balance check
requirements:
  placeholder: "%vault_eco_balance% >= 1000"

# Job level check
requirements:
  placeholder: "%universejobs_miner_level% >= 10"

# mcMMO skill check
requirements:
  placeholder: "%mcmmo_power_level% >= 500"
```

Multiple placeholders:

```yaml
requirements:
  placeholder:
    - "%player_level% >= 10"
    - "%vault_eco_balance% >= 1000"
```

## Item Condition

Check the item in the player's hand:

### Vanilla Items

```yaml
requirements:
  item: "DIAMOND_PICKAXE"
```

### Nexo Items

```yaml
requirements:
  nexo-id: "custom_pickaxe"
```

### ItemsAdder Items

```yaml
requirements:
  itemsadder-id: "namespace:custom_pickaxe"
```

## Combining Conditions

### AND Logic (All must match)

By default, all conditions in `requirements` use AND logic:

```yaml
requirements:
  world: "world"
  permission: "jobs.premium"
  biome: "PLAINS"
  # Player must be in "world" AND have permission AND be in PLAINS
```

### OR Logic (Any must match)

Use `any` wrapper for OR logic:

```yaml
requirements:
  any:
    - world: "world"
    - world: "world_nether"
    # Player must be in "world" OR "world_nether"
```

### Complex Conditions

Combine AND and OR:

```yaml
requirements:
  permission: "jobs.premium"        # Must have this permission
  any:                              # AND be in one of these worlds
    - world: "world"
    - world: "mining_world"
  time:                             # AND be during daytime
    from: 0
    to: 12000
```

## Condition Groups

Create reusable condition groups:

```yaml
requirements:
  group: "daytime_overworld"

# In config.yml, define:
condition-groups:
  daytime_overworld:
    world: "world"
    time:
      from: 0
      to: 12000
    weather: "CLEAR"
```

## Examples

### Premium Mining

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 100
      money: 25
      requirements:
        permission: "jobs.premium"
        world: "mining_world"
```

### Night Hunter

```yaml
actions:
  kill:
    - target: ZOMBIE
      xp: 20
      money: 5
      requirements:
        time:
          from: 13000
          to: 23000
        world: "world"
```

### Desert Farmer

```yaml
actions:
  harvest:
    - target: CACTUS
      xp: 15
      money: 3
      requirements:
        biome:
          - "DESERT"
          - "BADLANDS"
```

### VIP Bonus

```yaml
actions:
  break:
    - target: ANCIENT_DEBRIS
      xp: 200
      money: 50
      requirements:
        permission: "jobs.vip"
        world: "world_nether"
        placeholder: "%universejobs_miner_level% >= 50"
```

### Weather Fisher

```yaml
actions:
  fish:
    - target: COD
      xp: 20
      money: 5
      requirements:
        weather: "RAIN"

    - target: COD
      xp: 10
      money: 2
      # No requirements = always works
```

## Deny Messages

Customize the message shown when conditions aren't met:

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 50
      money: 10
      requirements:
        permission: "jobs.premium"
        deny-message: "<red>You need premium to mine diamonds!"
```
