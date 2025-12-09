# Actions

Actions define what activities give XP and money in a job. UniverseJobs supports 21 different action types.

## Action Types

| Action | Description | Target Type |
|--------|-------------|-------------|
| `BREAK` | Breaking blocks | Material |
| `PLACE` | Placing blocks | Material |
| `KILL` | Killing entities | EntityType |
| `HARVEST` | Harvesting crops | Material |
| `FISH` | Catching fish | Material/Item |
| `CRAFT` | Crafting items | Material |
| `SMELT` | Smelting items | Material |
| `ENCHANT` | Enchanting items | Enchantment |
| `BREW` | Brewing potions | PotionType |
| `TRADE` | Trading with villagers | Material |
| `TAME` | Taming animals | EntityType |
| `BREED` | Breeding animals | EntityType |
| `SHEAR` | Shearing sheep | EntityType |
| `MILK` | Milking cows | EntityType |
| `EAT` | Eating food | Material |
| `POTION` | Drinking potions | PotionType |
| `REPAIR` | Repairing items | Material |
| `EXPLORE` | Exploring chunks | - |
| `BLOCK_INTERACT` | Interacting with blocks | Material |
| `ENTITY_INTERACT` | Interacting with entities | EntityType |
| `CUSTOM` | Custom plugin actions | String |

## Basic Action Structure

```yaml
actions:
  break:                            # Action type (lowercase)
    - target: DIAMOND_ORE           # What to target
      xp: 50                        # XP reward
      money: 10                     # Money reward
```

## Action Properties

### Basic Properties

| Property | Type | Description |
|----------|------|-------------|
| `target` | String | Target material/entity/enchantment |
| `xp` | Number/String | XP reward (number or equation) |
| `money` | Number/String | Money reward (number or equation) |
| `display-name` | String | Custom display name in GUI |
| `display-material` | String | Custom icon material |
| `lore` | List | Custom description lines |
| `action-menu-priority` | Integer | Sort order in GUI (lower = first) |

### Advanced Properties

| Property | Type | Description |
|----------|------|-------------|
| `requirements` | Object | Condition requirements |
| `limits` | Object | Action rate limiting |
| `profession` | String/List | Villager profession filter (TRADE) |
| `color` | String/List | Sheep color filter (SHEAR) |
| `nbt` | String/List | NBT tag filter |
| `potion-type` | String/List | Potion type filter |
| `enchant-level` | String | Enchantment level requirement |
| `age` | String | Crop age requirement |
| `required-tool` | String | Required tool type |
| `blacklist` | List | Materials to exclude |
| `blacklisted-furnaces` | List | Furnace types to exclude (SMELT) |
| `interact-type` | String | Interaction type (RIGHT_CLICK, LEFT_CLICK) |

## Wildcards

Use `*` to match multiple targets:

```yaml
actions:
  break:
    - target: "*_ORE"               # All ores (DIAMOND_ORE, IRON_ORE, etc.)
      xp: 10
      money: 2
    - target: "STONE_*"             # All stone variants
      xp: 5
      money: 1
    - target: "*"                   # Everything
      xp: 1
      money: 0.1
```

## Equations

Use mathematical equations for dynamic rewards:

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: "10 + ({level} * 2)"      # XP increases with level
      money: "5 * {level}"          # Money scales with level
```

Available variables:
- `{level}` - Player's level in this job
- `{xp}` - Player's current XP

## Action Examples

### BREAK - Mining Blocks

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 50
      money: 10
      display-name: "<aqua>Diamond Ore"

    - target: DEEPSLATE_DIAMOND_ORE
      xp: 60
      money: 12

    - target: ANCIENT_DEBRIS
      xp: 100
      money: 25
      requirements:
        world: "world_nether"
```

### PLACE - Placing Blocks

```yaml
actions:
  place:
    - target: WHEAT_SEEDS
      xp: 2
      money: 0.5

    - target: "nexo:custom_seed"    # Nexo custom item
      xp: 5
      money: 1
```

### KILL - Killing Entities

```yaml
actions:
  kill:
    - target: ZOMBIE
      xp: 10
      money: 2

    - target: ENDER_DRAGON
      xp: 5000
      money: 1000

    - target: "mythicmobs:custom_boss"  # MythicMobs
      xp: 500
      money: 100

    - target: PLAYER                # PvP
      xp: 50
      money: 20
```

### HARVEST - Harvesting Crops

```yaml
actions:
  harvest:
    - target: WHEAT
      xp: 5
      money: 1
      age: "7"                      # Only fully grown

    - target: CARROTS
      xp: 5
      money: 1
      age: "7"

    - target: "customcrops:tomato"  # CustomCrops integration
      xp: 10
      money: 3
```

### FISH - Fishing

```yaml
actions:
  fish:
    - target: COD
      xp: 10
      money: 2

    - target: SALMON
      xp: 15
      money: 3

    - target: ENCHANTED_BOOK        # Treasure
      xp: 50
      money: 20

    - target: "customfishing:rare_fish"  # CustomFishing
      xp: 100
      money: 50
```

### CRAFT - Crafting Items

```yaml
actions:
  craft:
    - target: DIAMOND_SWORD
      xp: 30
      money: 5

    - target: DIAMOND_CHESTPLATE
      xp: 80
      money: 15

    - target: "nexo:custom_item"    # Nexo crafting
      xp: 50
      money: 10
```

### SMELT - Smelting Items

```yaml
actions:
  smelt:
    - target: IRON_INGOT
      xp: 10
      money: 2
      blacklisted-furnaces:         # Exclude specific furnaces
        - "BLAST_FURNACE"

    - target: GOLD_INGOT
      xp: 15
      money: 3
```

### ENCHANT - Enchanting

```yaml
actions:
  enchant:
    - target: SHARPNESS
      xp: 20
      money: 5
      enchant-level: "1-5"          # Level range

    - target: EFFICIENCY
      xp: 15
      money: 4
      enchant-level: "5"            # Specific level

    - target: "excellentenchants:tunnel"  # Custom enchants
      xp: 50
      money: 15
```

### BREW - Brewing Potions

```yaml
actions:
  brew:
    - target: SPEED
      xp: 20
      money: 5

    - target: STRENGTH
      xp: 25
      money: 8
      potion-type: "STRENGTH:2"     # Level 2 only
```

### TRADE - Villager Trading

```yaml
actions:
  trade:
    - target: EMERALD
      xp: 10
      money: 0
      profession: "ARMORER"         # Only armorers

    - target: ENCHANTED_BOOK
      xp: 30
      money: 5
      profession:                   # Multiple professions
        - "LIBRARIAN"
        - "CLERIC"
```

### BREED - Breeding Animals

```yaml
actions:
  breed:
    - target: COW
      xp: 15
      money: 3
      display-material: WHEAT       # Show wheat icon

    - target: CHICKEN
      xp: 10
      money: 2
      display-material: WHEAT_SEEDS

    - target: WOLF
      xp: 25
      money: 5
      display-material: COOKED_BEEF
```

### SHEAR - Shearing Sheep

```yaml
actions:
  shear:
    - target: SHEEP
      xp: 5
      money: 1

    - target: SHEEP
      xp: 15
      money: 5
      color: "PINK"                 # Pink sheep only

    - target: SHEEP
      xp: 10
      money: 3
      color:                        # Multiple colors
        - "RED"
        - "BLUE"
```

### EAT - Eating Food

```yaml
actions:
  eat:
    - target: GOLDEN_APPLE
      xp: 50
      money: 10

    - target: COOKED_BEEF
      xp: 5
      money: 1

    - target: "*"                   # Any food
      xp: 2
      money: 0.5
      nbt: "mmoitems:custom_food"   # Custom item only
```

### REPAIR - Repairing Items

```yaml
actions:
  repair:
    - target: DIAMOND_PICKAXE
      xp: 30
      money: 5

    - target: NETHERITE_SWORD
      xp: 50
      money: 15

    - target: "*_CHESTPLATE"        # Any chestplate
      xp: 40
      money: 10
```

### EXPLORE - Chunk Exploration

```yaml
actions:
  explore:
    - target: "*"                   # Any chunk
      xp: 5
      money: 1
      requirements:
        world: "world"              # Overworld only
```

## Action Limits

Prevent farming/exploiting:

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 50
      money: 10
      limits:
        max-action-per-period: 100  # Max 100 per period
        cooldown-minutes: 60        # Reset after 60 minutes
        block-exp: true             # Block XP when limit reached
        block-money: true           # Block money when limit reached
```

## Requirements

Add conditions to actions (see [Conditions](Conditions) page):

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 50
      money: 10
      requirements:
        permission: "jobs.premium"
        world: "world"
        time:
          from: 0
          to: 12000
```

## Custom Display

```yaml
actions:
  break:
    - target: DIAMOND_ORE
      xp: 50
      money: 10
      display-name: "<gradient:#00FFFF:#0080FF>Diamond Mining</gradient>"
      display-material: "DIAMOND:1001"  # Material:CustomModelData
      lore:
        - "<gray>Mine diamond ore"
        - "<gray>to get rich!"
      action-menu-priority: 1       # Show first in menu
```
