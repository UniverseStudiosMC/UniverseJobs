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
  BREAK:                              # Action type (UPPERCASE)
    break_diamond_ore:                # Unique action key
      target: "DIAMOND_ORE"           # What to target
      display-name: "Diamond Ore"     # Display name in GUI
      xp: 50                          # XP reward
      money: 10                       # Money reward
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
  BREAK:
    break_any_ore:
      target: "*_ORE"
      display-name: "Any Ore"
      xp: 10
      money: 2
    break_stone_variants:
      target: "STONE_*"
      display-name: "Stone Variants"
      xp: 5
      money: 1
    break_anything:
      target: "*"
      display-name: "Any Block"
      xp: 1
      money: 0.1
```

## Equations

Use mathematical equations for dynamic rewards:

```yaml
actions:
  BREAK:
    break_diamond_ore:
      target: "DIAMOND_ORE"
      display-name: "Diamond Ore"
      xp: "10 + ({level} * 2)"
      money: "5 + {level} * 0.5"
```

Available variables:
- `{level}` - Player's level in this job
- `{xp}` - Player's current XP

## Action Examples

### BREAK - Mining Blocks

```yaml
actions:
  BREAK:
    break_diamond_ore:
      target: "DIAMOND_ORE"
      display-name: "<aqua>Diamond Ore"
      xp: 50
      money: 10

    break_deepslate_diamond_ore:
      target: "DEEPSLATE_DIAMOND_ORE"
      display-name: "Deepslate Diamond Ore"
      xp: 60
      money: 12

    break_ancient_debris:
      target: "ANCIENT_DEBRIS"
      display-name: "Ancient Debris"
      xp: 100
      money: 25
      requirements:
        world: "world_nether"
```

### PLACE - Placing Blocks

```yaml
actions:
  PLACE:
    place_wheat_seeds:
      target: "WHEAT_SEEDS"
      display-name: "Wheat Seeds"
      xp: 2
      money: 0.5

    place_custom_seed:
      target: "nexo:custom_seed"
      display-name: "Custom Seed"
      xp: 5
      money: 1
```

### KILL - Killing Entities

```yaml
actions:
  KILL:
    kill_zombie:
      target: "ZOMBIE"
      display-name: "Zombie"
      xp: 10
      money: 2

    kill_ender_dragon:
      target: "ENDER_DRAGON"
      display-name: "Ender Dragon"
      xp: 5000
      money: 1000

    kill_mythic_boss:
      target: "mythicmobs:custom_boss"
      display-name: "Custom Boss"
      xp: 500
      money: 100

    kill_player:
      target: "PLAYER"
      display-name: "Player Kill"
      xp: 50
      money: 20
```

### HARVEST - Harvesting Crops

```yaml
actions:
  HARVEST:
    harvest_wheat:
      target: "WHEAT"
      display-name: "Wheat"
      xp: 5
      money: 1
      age: "7"

    harvest_carrots:
      target: "CARROTS"
      display-name: "Carrots"
      xp: 5
      money: 1
      age: "7"

    harvest_tomato:
      target: "customcrops:tomato"
      display-name: "Tomato"
      xp: 10
      money: 3
```

### FISH - Fishing

```yaml
actions:
  FISH:
    fish_cod:
      target: "COD"
      display-name: "Cod"
      xp: 10
      money: 2

    fish_salmon:
      target: "SALMON"
      display-name: "Salmon"
      xp: 15
      money: 3

    fish_book:
      target: "ENCHANTED_BOOK"
      display-name: "Enchanted Book"
      xp: 50
      money: 20

    fish_rare:
      target: "customfishing:rare_fish"
      display-name: "Rare Fish"
      xp: 100
      money: 50
```

### CRAFT - Crafting Items

```yaml
actions:
  CRAFT:
    craft_diamond_sword:
      target: "DIAMOND_SWORD"
      display-name: "Diamond Sword"
      xp: 30
      money: 5

    craft_diamond_chestplate:
      target: "DIAMOND_CHESTPLATE"
      display-name: "Diamond Chestplate"
      xp: 80
      money: 15

    craft_custom_item:
      target: "nexo:custom_item"
      display-name: "Custom Item"
      xp: 50
      money: 10
```

### SMELT - Smelting Items

```yaml
actions:
  SMELT:
    smelt_iron:
      target: "IRON_INGOT"
      display-name: "Iron Ingot"
      xp: 10
      money: 2
      blacklisted-furnaces:
        - "BLAST_FURNACE"

    smelt_gold:
      target: "GOLD_INGOT"
      display-name: "Gold Ingot"
      xp: 15
      money: 3
```

### ENCHANT - Enchanting

```yaml
actions:
  ENCHANT:
    enchant_sharpness:
      target: "SHARPNESS"
      display-name: "Sharpness"
      xp: 20
      money: 5
      enchant-level: "1-5"

    enchant_efficiency:
      target: "EFFICIENCY"
      display-name: "Efficiency V"
      xp: 15
      money: 4
      enchant-level: "5"

    enchant_tunnel:
      target: "excellentenchants:tunnel"
      display-name: "Tunnel"
      xp: 50
      money: 15
```

### BREW - Brewing Potions

```yaml
actions:
  BREW:
    brew_speed:
      target: "SPEED"
      display-name: "Speed Potion"
      xp: 20
      money: 5

    brew_strength:
      target: "STRENGTH"
      display-name: "Strength II"
      xp: 25
      money: 8
      potion-type: "STRENGTH:2"
```

### TRADE - Villager Trading

```yaml
actions:
  TRADE:
    trade_emerald_armorer:
      target: "EMERALD"
      display-name: "Armorer Trade"
      xp: 10
      money: 0
      profession: "ARMORER"

    trade_book:
      target: "ENCHANTED_BOOK"
      display-name: "Book Trade"
      xp: 30
      money: 5
      profession:
        - "LIBRARIAN"
        - "CLERIC"
```

### BREED - Breeding Animals

```yaml
actions:
  BREED:
    breed_cow:
      target: "COW"
      display-name: "Cow"
      display-material: "WHEAT"
      xp: 15
      money: 3

    breed_chicken:
      target: "CHICKEN"
      display-name: "Chicken"
      display-material: "WHEAT_SEEDS"
      xp: 10
      money: 2

    breed_wolf:
      target: "WOLF"
      display-name: "Wolf"
      display-material: "COOKED_BEEF"
      xp: 25
      money: 5
```

### SHEAR - Shearing Sheep

```yaml
actions:
  SHEAR:
    shear_sheep:
      target: "SHEEP"
      display-name: "Sheep"
      xp: 5
      money: 1

    shear_pink_sheep:
      target: "SHEEP"
      display-name: "Pink Sheep"
      xp: 15
      money: 5
      color: "PINK"

    shear_colored_sheep:
      target: "SHEEP"
      display-name: "Colored Sheep"
      xp: 10
      money: 3
      color:
        - "RED"
        - "BLUE"
```

### EAT - Eating Food

```yaml
actions:
  EAT:
    eat_golden_apple:
      target: "GOLDEN_APPLE"
      display-name: "Golden Apple"
      xp: 50
      money: 10

    eat_cooked_beef:
      target: "COOKED_BEEF"
      display-name: "Cooked Beef"
      xp: 5
      money: 1

    eat_custom_food:
      target: "*"
      display-name: "Custom Food"
      xp: 2
      money: 0.5
      nbt: "mmoitems:custom_food"
```

### REPAIR - Repairing Items

```yaml
actions:
  REPAIR:
    repair_diamond_pickaxe:
      target: "DIAMOND_PICKAXE"
      display-name: "Diamond Pickaxe"
      xp: 30
      money: 5

    repair_netherite_sword:
      target: "NETHERITE_SWORD"
      display-name: "Netherite Sword"
      xp: 50
      money: 15

    repair_any_chestplate:
      target: "*_CHESTPLATE"
      display-name: "Any Chestplate"
      xp: 40
      money: 10
```

### EXPLORE - Chunk Exploration

```yaml
actions:
  EXPLORE:
    explore_overworld:
      target: "*"
      display-name: "Explore Chunk"
      xp: 5
      money: 1
      requirements:
        world: "world"
```

## Action Limits

Prevent farming/exploiting by limiting how many times a player can perform an action.

### Basic Configuration

```yaml
actions:
  BREAK:
    break_diamond_ore:
      target: "DIAMOND_ORE"
      display-name: "Diamond Ore"
      xp: 50
      money: 10
      limits:
        max-actions-per-period: 100    # Max actions before cooldown
        cooldown-minutes: 60           # Cooldown duration in minutes
        block-exp: true                # Block XP when limit reached
        block-money: true              # Block money when limit reached
```

### Limit Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `max-actions-per-period` | Integer | - | Maximum actions allowed before cooldown |
| `cooldown-minutes` | Integer | 60 | Cooldown duration in minutes |
| `block-exp` | Boolean | false | Whether to block XP when limit reached |
| `block-money` | Boolean | false | Whether to block money when limit reached |
| `message` | Object | - | Message configuration (see below) |

### Limit Messages

Configure a message to inform players when they reach their action limit:

```yaml
actions:
  BREAK:
    break_diamond_ore:
      target: "DIAMOND_ORE"
      xp: 50
      money: 10
      limits:
        max-actions-per-period: 100
        cooldown-minutes: 60
        block-exp: true
        block-money: true
        message:
          enabled: true
          type: actionbar
          text: "&cLimit reached! {actions}/{max} - Wait {time}"
```

### Message Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | true | Enable/disable the limit message |
| `type` | String | actionbar | Message display type |
| `text` | String | `&cLimit reached! Wait {time} to continue.` | The message text |

### Message Types

| Type | Description |
|------|-------------|
| `chat` | Sends message in player's chat |
| `actionbar` | Displays above hotbar (default) |
| `bossbar` | Displays as a red boss bar at top of screen |

### Message Placeholders

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{actions}` | Number of actions performed | `100` |
| `{max}` | Maximum actions allowed | `100` |
| `{time}` | Formatted cooldown time remaining | `1h 0m`, `30m 15s`, `45s` |

### Complete Example

```yaml
actions:
  BREAK:
    break_diamond_ore:
      target: "DIAMOND_ORE"
      display-name: "Diamond Ore"
      xp: 50
      money: 10
      limits:
        max-actions-per-period: 100
        cooldown-minutes: 60
        block-exp: false
        block-money: false
        message:
          enabled: true
          type: bossbar
          text: "&c&lMining Limit! &7{actions}/{max} diamonds mined. Wait &e{time}&7."

    break_any_ore:
      target: "*_ORE"
      xp: 10
      money: 2
      limits:
        max-actions-per-period: 500
        cooldown-minutes: 120
        message:
          enabled: true
          type: actionbar
          text: "&eOre limit reached! Cooldown: {time}"
```

## Requirements

Add conditions to actions (see [Conditions](Conditions) page):

```yaml
actions:
  BREAK:
    break_diamond_ore:
      target: "DIAMOND_ORE"
      display-name: "Diamond Ore"
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
  BREAK:
    break_diamond_ore:
      target: "DIAMOND_ORE"
      display-name: "<gradient:#00FFFF:#0080FF>Diamond Mining</gradient>"
      display-material: "DIAMOND:1001"
      action-menu-priority: 1
      lore:
        - "<gray>Mine diamond ore"
        - "<gray>to get rich!"
      xp: 50
      money: 10
```
