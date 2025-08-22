# ITEM

Checks the item in the player's main hand against various criteria.

## Description

Item requirements verify that the player is holding a specific item in their main hand. This system supports material checking, custom model data, NBT data, MMOItems integration, and multiple material options with OR logic.

## Basic Configuration

```yaml
requirements:
  item:
    material: "DIAMOND_PICKAXE"
    deny:
      message: "&cYou need a diamond pickaxe!"
      sound: "BLOCK_ANVIL_PLACE"
```

## Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `material` | String/List | Material name(s) - accepts single material or list |
| `custom-model-data` | Integer | Custom model data value for resource packs |
| `nbt.key` | String | NBT key to check |
| `nbt.value` | String | Required NBT value |
| `mmoitems.type` | String | MMOItems item type |
| `mmoitems.id` | String | MMOItems item ID |

## Advanced Configuration

### Multiple Material Options

```yaml
requirements:
  item:
    material: 
      - "DIAMOND_PICKAXE"
      - "NETHERITE_PICKAXE"
      - "IRON_PICKAXE"
    deny:
      message: "&cYou need at least an iron pickaxe!"
      sound: "BLOCK_ANVIL_PLACE"
```

### Custom Model Data Support

```yaml
requirements:
  item:
    material: "DIAMOND_SWORD"
    custom-model-data: 12345
    accept:
      message: "&6Special sword equipped!"
      commands:
        - "effect give {player} strength 300 1"
    deny:
      message: "&cYou need the special diamond sword (ID: 12345)!"
```

### NBT Data Checking

```yaml
requirements:
  item:
    material: "STICK"
    nbt:
      key: "special_wand"
      value: "fire_wand"
    accept:
      message: "&cFire wand equipped!"
      sound: "ENTITY_BLAZE_SHOOT"
    deny:
      message: "&cYou need a fire wand!"
```

### MMOItems Integration

```yaml
requirements:
  item:
    mmoitems:
      type: "SWORD"
      id: "LEGENDARY_BLADE"
    accept:
      message: "&5Legendary blade equipped!"
      commands:
        - "effect give {player} strength 600 2"
        - "effect give {player} resistance 600 1"
    deny:
      message: "&cYou need the Legendary Blade MMOItem!"
```

## Complex Examples

### Tier-Based Tool Requirements

```yaml
diamond_mining:
  target: "DIAMOND_ORE"
  xp: 50.0
  money: 200.0
  requirements:
    logic: "OR"
    multiple_condition:
      premium_tool:
        item:
          material: "NETHERITE_PICKAXE"
        accept:
          message: "&5Netherite pickaxe: Maximum efficiency!"
          commands:
            - "effect give {player} haste 300 2"
      
      standard_tool:
        item:
          material: "DIAMOND_PICKAXE"
        accept:
          message: "&bDiamond pickaxe: Good choice!"
          commands:
            - "effect give {player} haste 300 1"
      
      enchanted_iron:
        item:
          material: "IRON_PICKAXE"
          nbt:
            key: "Enchantments"
            value: "fortune"
        accept:
          message: "&aEnchanted iron pickaxe works!"
      
      deny:
        message: "&cYou need at least an iron pickaxe (enchanted) or better!"
        cancel-event: true
```

### Special Tool with Custom Properties

```yaml
master_crafting:
  target: "NETHERITE_INGOT"
  xp: 500.0
  requirements:
    logic: "AND"
    multiple_condition:
      master_hammer:
        item:
          material: "DIAMOND_AXE"
          custom-model-data: 9001
          nbt:
            key: "master_tool"
            value: "true"
        accept:
          message: "&6Master Hammer equipped!"
          sound: "BLOCK_ANVIL_USE"
          commands:
            - "effect give {player} strength 600 3"
        deny:
          message: "&cYou need the Master Hammer (custom tool)!"
      
      furnace_access:
        placeholder:
          placeholder: "%player_location_block%"
          operator: "contains"
          value: "furnace"
        deny:
          message: "&cYou must be near a furnace!"
      
      accept:
        message: "&a🔥 Master crafting station ready!"
        commands:
          - "title {player} title \"&6Master Crafter\""
          - "title {player} subtitle \"&eForging legendary items...\""
```

### MMOItems Weapon Requirements

```yaml
boss_fight_action:
  target: "WITHER"
  xp: 1000.0
  requirements:
    logic: "OR"
    multiple_condition:
      legendary_weapon:
        item:
          mmoitems:
            type: "SWORD"
            id: "DRAGON_SLAYER"
        accept:
          message: "&4Dragon Slayer sword: Maximum damage!"
          commands:
            - "effect give {player} strength 1200 3"
            - "effect give {player} fire_resistance 1200 1"
      
      mythic_weapon:
        item:
          mmoitems:
            type: "BOW"
            id: "VOID_ARCHER"
        accept:
          message: "&5Void Archer bow: Piercing shots!"
          commands:
            - "effect give {player} strength 1200 2"
            - "effect give {player} speed 1200 2"
      
      enchanted_netherite:
        item:
          material: "NETHERITE_SWORD"
          nbt:
            key: "Enchantments"
            value: "sharpness"
        accept:
          message: "&8Enchanted netherite: Adequate power!"
          commands:
            - "effect give {player} strength 1200 1"
      
      deny:
        message: "&cYou need a legendary weapon to fight bosses!"
        cancel-event: true
```

### Tool Durability Checking

```yaml
precision_mining:
  target: "EMERALD_ORE"
  xp: 100.0
  requirements:
    logic: "AND"
    multiple_condition:
      tool_check:
        item:
          material: 
            - "DIAMOND_PICKAXE"
            - "NETHERITE_PICKAXE"
        deny:
          message: "&cPrecision mining requires diamond or netherite pickaxe!"
      
      tool_condition:
        placeholder:
          placeholder: "%item_durability%"
          operator: "greater_than"
          value: "50"
        accept:
          message: "&aTool in excellent condition!"
        deny:
          message: "&cYour tool is too damaged for precision work!"
          commands:
            - "tellraw {player} \"Repair your tool at an anvil!\""
      
      enchantment_check:
        item:
          nbt:
            key: "Enchantments"
            value: "fortune"
        accept:
          message: "&bFortune enchantment detected: Bonus yield!"
          commands:
            - "eco give {player} 50"
      
      accept:
        message: "&a💎 Precision mining mode activated!"
        commands:
          - "effect give {player} haste 300 1"
          - "effect give {player} luck 300 2"
```

### Resource Pack Integration

```yaml
custom_furniture_craft:
  target: "OAK_PLANKS"
  xp: 10.0
  requirements:
    logic: "AND"
    multiple_condition:
      custom_hammer:
        item:
          material: "WOODEN_AXE"
          custom-model-data: 7001
        accept:
          message: "&eCrafting hammer ready!"
        deny:
          message: "&cYou need a crafting hammer (resource pack item)!"
      
      workbench_area:
        placeholder:
          placeholder: "%player_location_block%"
          operator: "contains"
          value: "crafting_table"
        deny:
          message: "&cYou must be near a crafting table!"
      
      materials_check:
        placeholder:
          placeholder: "%player_inventory_oak_log%"
          operator: "greater_equal"
          value: "4"
        deny:
          message: "&cYou need at least 4 oak logs!"
          commands:
            - "tellraw {player} \"Gather more oak logs first!\""
      
      accept:
        message: "&a🔨 Custom furniture crafting unlocked!"
        sound: "BLOCK_WOOD_PLACE"
        commands:
          - "take {player} oak_log 4"
          - "give {player} custom_table 1"
```

## Item Categories

### Common Tool Materials

```yaml
# Basic tools
basic_tools:
  material: ["WOODEN_PICKAXE", "STONE_PICKAXE", "IRON_PICKAXE"]

# Advanced tools  
advanced_tools:
  material: ["DIAMOND_PICKAXE", "NETHERITE_PICKAXE"]

# All pickaxes
any_pickaxe:
  material: 
    - "WOODEN_PICKAXE"
    - "STONE_PICKAXE" 
    - "IRON_PICKAXE"
    - "GOLDEN_PICKAXE"
    - "DIAMOND_PICKAXE"
    - "NETHERITE_PICKAXE"
```

### Special Items

```yaml
# Magic items (using NBT)
magic_wand:
  material: "STICK"
  nbt:
    key: "magic_type"
    value: "fire"

# Resource pack items
custom_armor:
  material: "LEATHER_CHESTPLATE"
  custom-model-data: 5000

# MMOItems weapons
legendary_sword:
  mmoitems:
    type: "SWORD"
    id: "EXCALIBUR"
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  item:
    material: "customcrops:special_hoe"
    accept:
      message: "&aCustomCrops special hoe equipped!"
    deny:
      message: "&cYou need a CustomCrops special hoe!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  item:
    material: "nexo:master_pickaxe"
    accept:
      message: "&6Nexo master pickaxe equipped!"
      commands:
        - "effect give {player} haste 300 3"
    deny:
      message: "&cYou need a Nexo master pickaxe!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  item:
    material: "itemsadder:ruby_pickaxe"
    accept:
      message: "&cRuby pickaxe equipped!"
      sound: "BLOCK_NOTE_BLOCK_CHIME"
    deny:
      message: "&cYou need an ItemsAdder ruby pickaxe!"
```
{% endtab %}
{% endtabs %}