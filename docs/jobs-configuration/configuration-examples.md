# 📝 Complete Configuration Examples

This page presents complete job configuration examples to inspire you and help you create your own custom jobs.

## 🏗️ Basic Job Structure

Each job is defined in a YAML file in the `/plugins/JobsAdventure/jobs/` folder. Here's the basic structure:

```yaml
# Basic information
name: "JobName"
description: "Job description"
enabled: true
max-level: 100
permission: "jobsadventure.job.jobname"
icon: "MATERIAL_ICON"

# Reward system
rewards: "reward_file_name"
gui-reward: "gui_reward_file_name"

# Experience curve
xp-equation: "100 * Math.pow(level, 1.8)"

# XP messages
xp-message:
  type: "bossbar"
  text: "+{exp} XP - {job}"
  bossbar:
    color: "green"
    style: "segmented_0"
    duration: 60

# Job description
lore:
  - "&7Description line 1"
  - "&7Description line 2"

# Actions that give XP
actions:
  ACTION_TYPE:
    action_name:
      target: "TARGET"
      xp: 10.0
      name: "Action name"
      description: "Action description"
      # Optional conditions
      requirements:
        # ...
```

## ⛏️ Complete Example: Miner Job

```yaml
# /plugins/JobsAdventure/jobs/miner.yml
name: "Miner"
description: "Master of underground extraction"
enabled: true
max-level: 100
permission: "jobsadventure.job.miner"
icon: "DIAMOND_PICKAXE"

rewards: "miner_rewards"
gui-reward: "miner_gui"

# Progressive XP curve
xp-equation: "100 * Math.pow(level, 1.8)"

# Messages with colored boss bar
xp-message:
  type: "bossbar"
  text: "&6⛏ +{exp} XP Mining &7({level})"
  bossbar:
    color: "yellow"
    style: "segmented_10"
    duration: 80
    show-progress: true

lore:
  - "&7Dig deep into the earth"
  - "&7Discover precious ores"
  - "&7Bonus XP for rare materials"
  - "&7Compatible with all custom block plugins"

actions:
  # Basic mining
  BREAK:
    # Basic stone
    stone:
      target: "STONE"
      xp: 1.0
      name: "Stone Extraction"
      description: "Basic stone mining"
    
    # Common ores
    coal_ore:
      target: "COAL_ORE"
      xp: 5.0
      name: "Coal Extraction"
      description: "Coal ore mining"
      requirements:
        logic: "AND"
        item:
          material: "IRON_PICKAXE"
          deny:
            message: "&cAn iron pickaxe or better is required!"
            sound: "BLOCK_ANVIL_PLACE"
    
    iron_ore:
      target: "IRON_ORE"
      xp: 12.0
      name: "Iron Extraction"
      description: "Iron ore mining"
      requirements:
        logic: "AND"
        item:
          material: "IRON_PICKAXE"
        placeholder:
          placeholder: "%jobsadventure_miner_player_level%"
          operator: "greater_than"
          value: "10"
          deny:
            message: "&cLevel 10 required in mining!"
    
    # Precious ores
    gold_ore:
      target: "GOLD_ORE"
      xp: 25.0
      name: "Gold Extraction"
      description: "Precious gold ore mining"
      requirements:
        logic: "AND"
        item:
          material: "DIAMOND_PICKAXE"
        world:
          worlds: ["world", "mining_world"]
          blacklist: false
          deny:
            message: "&cGold can only be mined in certain worlds!"
    
    diamond_ore:
      target: "DIAMOND_ORE"
      xp: 50.0
      name: "Diamond Extraction"
      description: "Mining precious diamonds"
      message:
        type: "BOSSBAR"
        style: "segment_0"
        color: "BLUE"
        duration: 100
        message: "&b💎 DIAMOND FOUND! +50 XP"
      sound: "ENTITY_PLAYER_LEVELUP"
      requirements:
        logic: "AND"
        item:
          material: "DIAMOND_PICKAXE"
        time:
          min: 13000  # Night only
          max: 23000
          deny:
            message: "&cDiamonds are easier to find at night!"
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "16"
          deny:
            message: "&cDiamonds are found below Y=16!"
    
    # Nether ores
    ancient_debris:
      target: "ANCIENT_DEBRIS"
      xp: 100.0
      name: "Ancient Debris Extraction"
      description: "Mining rare ancient debris"
      commands:
        - "broadcast &6{player} &efound ancient debris!"
      requirements:
        logic: "AND"
        item:
          material: "NETHERITE_PICKAXE"
        world:
          worlds: ["world_nether"]
          blacklist: false
    
    # Custom Nexo blocks
    mythril_ore:
      target: "nexo:mythril_ore"
      xp: 75.0
      name: "Mythril Extraction"
      description: "Mining legendary mythril"
      requirements:
        logic: "AND"
        item:
          mmoitems:
            type: "TOOL"
            id: "MYTHRIL_PICKAXE"
          deny:
            message: "&cOnly a mythril pickaxe can extract this ore!"
  
  # Underground combat
  KILL:
    cave_spider:
      target: "CAVE_SPIDER"
      xp: 8.0
      name: "Cave Spider Elimination"
      description: "Combat in mines"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "50"
          deny:
            message: "&cUnderground bonus only!"
    
    zombie:
      target: "ZOMBIE"
      xp: 5.0
      name: "Miner Zombie Elimination"
      description: "Clearing mines of undead"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "60"
    
    # MythicMobs boss
    cave_guardian:
      target: "MYTHICMOB:CaveGuardian"
      xp: 200.0
      name: "Cave Guardian Defeat"
      description: "Defeating the formidable guardian"
      commands:
        - "broadcast &6&l{player} &edefeated the Cave Guardian!"
        - "give {player} diamond 10"
      requirements:
        logic: "AND"
        permission:
          permission: "jobs.boss.cave"
          require: true
```

## 🌾 Complete Example: Farmer Job

```yaml
# /plugins/JobsAdventure/jobs/farmer.yml
name: "Farmer"
description: "Master of agriculture and livestock"
enabled: true
max-level: 75
permission: "jobsadventure.job.farmer"
icon: "GOLDEN_HOE"

rewards: "farmer_rewards"
gui-reward: "farmer_gui"

xp-equation: "80 * Math.pow(level, 1.6) + level * 15"

xp-message:
  type: "actionbar"
  text: "&a🌾 +{exp} XP Agriculture &7[{level}]"
  actionbar:
    duration: 100

lore:
  - "&7Cultivate the land and raise animals"
  - "&7Master the art of agriculture"
  - "&7Seasonal bonuses and multipliers"
  - "&7Complete CustomCrops integration"

actions:
  # Basic agriculture
  BREAK:
    wheat:
      target: "WHEAT"
      xp: 3.0
      name: "Wheat Harvest"
      description: "Harvesting mature wheat"
      requirements:
        logic: "AND"
        item:
          material: "HOE"
    
    carrots:
      target: "CARROTS"
      xp: 3.5
      name: "Carrot Harvest"
      description: "Harvesting mature carrots"
    
    potatoes:
      target: "POTATOES"
      xp: 3.5
      name: "Potato Harvest"
      description: "Harvesting mature potatoes"
    
    # CustomCrops
    tomato:
      target: "customcrops:tomato_stage_3"
      xp: 8.0
      name: "Tomato Harvest"
      description: "Harvesting mature CustomCrops tomatoes"
      message:
        type: "CHAT"
        message: "&a🍅 Tomato harvested! +8 XP"
    
    corn:
      target: "customcrops:corn_stage_4"
      xp: 12.0
      name: "Corn Harvest"
      description: "Harvesting giant corn"
  
  # Planting
  PLACE:
    wheat_seeds:
      target: "WHEAT_SEEDS"
      xp: 1.0
      name: "Wheat Planting"
      description: "Planting wheat seeds"
    
    custom_tomato_seeds:
      target: "customcrops:tomato_seeds"
      xp: 2.0
      name: "Tomato Planting"
      description: "Planting tomato seeds"
  
  # Livestock
  KILL:
    cow:
      target: "COW"
      xp: 8.0
      name: "Cattle Slaughter"
      description: "Raising and slaughtering cattle"
      requirements:
        logic: "AND"
        item:
          material: "SWORD"
    
    pig:
      target: "PIG"
      xp: 6.0
      name: "Pig Slaughter"
      description: "Raising and slaughtering pigs"
    
    chicken:
      target: "CHICKEN"
      xp: 4.0
      name: "Poultry Slaughter"
      description: "Raising and slaughtering poultry"
  
  # Animal care
  INTERACT:
    milk_cow:
      target: "COW"
      xp: 2.0
      name: "Cow Milking"
      description: "Milking a cow with a bucket"
      requirements:
        logic: "AND"
        item:
          material: "BUCKET"
  
  # Agricultural crafting
  CRAFT:
    bread:
      target: "BREAD"
      xp: 2.0
      name: "Bread Making"
      description: "Preparing fresh bread"
    
    cake:
      target: "CAKE"
      xp: 10.0
      name: "Cake Preparation"
      description: "Creating a delicious cake"
```

## 🏹 Complete Example: Hunter Job

```yaml
# /plugins/JobsAdventure/jobs/hunter.yml
name: "Hunter"
description: "Master of hunting and combat"
enabled: true
max-level: 80
permission: "jobsadventure.job.hunter"
icon: "BOW"

rewards: "hunter_rewards"
gui-reward: "hunter_gui"

xp-equation: "120 * Math.pow(level, 1.7)"

xp-message:
  type: "bossbar"
  text: "&c🏹 +{exp} XP Hunting &7| Level {level}"
  bossbar:
    color: "red"
    style: "segmented_6"
    duration: 60

lore:
  - "&7Track and hunt wild creatures"
  - "&7Master the art of combat and survival"
  - "&7Bonuses for rare and dangerous creatures"
  - "&7Advanced MythicMobs integration"

actions:
  # Basic hunting
  KILL:
    zombie:
      target: "ZOMBIE"
      xp: 5.0
      name: "Zombie Elimination"
      description: "Hunting the undead"
    
    skeleton:
      target: "SKELETON"
      xp: 6.0
      name: "Skeleton Elimination"
      description: "Fighting skeleton archers"
    
    creeper:
      target: "CREEPER"
      xp: 8.0
      name: "Creeper Elimination"
      description: "Defusing explosive threats"
      requirements:
        logic: "AND"
        item:
          material: "BOW"
          deny:
            message: "&cUse a bow to hunt creepers safely!"
    
    spider:
      target: "SPIDER"
      xp: 4.0
      name: "Spider Elimination"
      description: "Hunting spiders"
    
    # Advanced hostile creatures
    enderman:
      target: "ENDERMAN"
      xp: 15.0
      name: "Enderman Elimination"
      description: "Facing End teleporters"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%jobsadventure_hunter_player_level%"
          operator: "greater_than"
          value: "20"
          deny:
            message: "&cLevel 20 in hunting required to face Endermen!"
    
    wither_skeleton:
      target: "WITHER_SKELETON"
      xp: 25.0
      name: "Wither Skeleton Elimination"
      description: "Fighting in the Nether"
      requirements:
        logic: "AND"
        world:
          worlds: ["world_nether"]
          blacklist: false
    
    # Wild animals
    wolf:
      target: "WOLF"
      xp: 10.0
      name: "Wolf Hunting"
      description: "Hunting wild wolves"
      requirements:
        logic: "AND"
        weather:
          weather: "CLEAR"
          deny:
            message: "&cWolves are more aggressive in clear weather!"
    
    # MythicMobs bosses
    forest_guardian:
      target: "MYTHICMOB:ForestGuardian"
      xp: 150.0
      name: "Forest Guardian Defeat"
      description: "Defeating nature's protector"
      commands:
        - "broadcast &a&l{player} &edefeated the Forest Guardian!"
        - "give {player} emerald 15"
      sound: "ENTITY_ENDER_DRAGON_DEATH"
    
    ancient_beast:
      target: "MYTHICMOB:AncientBeast"
      xp: 300.0
      name: "Ancient Beast Defeat"
      description: "Facing the legendary creature"
      requirements:
        logic: "AND"
        permission:
          permission: "jobs.boss.ancient"
        groups:
          group1:
            logic: "OR"
            item:
              mmoitems:
                type: "SWORD"
                id: "LEGENDARY_BLADE"
            item:
              mmoitems:
                type: "BOW"
                id: "MYTHIC_BOW"
  
  # Taming
  TAME:
    wolf_taming:
      target: "WOLF"
      xp: 20.0
      name: "Wolf Taming"
      description: "Taming a wild wolf"
    
    horse_taming:
      target: "HORSE"
      xp: 25.0
      name: "Horse Taming"
      description: "Taming a wild horse"
  
  # Specialized fishing (CustomFishing)
  FISH:
    rare_fish:
      target: "customfishing:golden_trout"
      xp: 30.0
      name: "Golden Trout Fishing"
      description: "Catching a rare trout"
      requirements:
        logic: "AND"
        time:
          min: 6000   # Early day
          max: 12000  # Noon
        biome:
          biomes: ["RIVER", "FOREST"]
          blacklist: false
```

## 🔧 Example: Crafter Job

```yaml
# /plugins/JobsAdventure/jobs/crafter.yml
name: "Crafter"
description: "Master of crafting and creation"
enabled: true
max-level: 60
permission: "jobsadventure.job.crafter"
icon: "CRAFTING_TABLE"

rewards: "crafter_rewards"
gui-reward: "crafter_gui"

xp-equation: "60 * Math.pow(level, 1.5) + level * 10"

xp-message:
  type: "chat"
  text: "&6🔨 +{exp} XP Crafting &7({job} Lvl.{level})"

lore:
  - "&7Create and craft useful items"
  - "&7Master all crafting arts"
  - "&7Bonuses for complex items"
  - "&7Compatible with MMOItems and custom items"

actions:
  # Basic crafting
  CRAFT:
    wooden_tools:
      target: "WOODEN_PICKAXE,WOODEN_AXE,WOODEN_SHOVEL,WOODEN_SWORD"
      xp: 2.0
      name: "Wooden Tool Crafting"
      description: "Creating basic tools"
    
    stone_tools:
      target: "STONE_PICKAXE,STONE_AXE,STONE_SHOVEL,STONE_SWORD"
      xp: 4.0
      name: "Stone Tool Crafting"
      description: "Creating improved tools"
    
    iron_tools:
      target: "IRON_PICKAXE,IRON_AXE,IRON_SHOVEL,IRON_SWORD"
      xp: 8.0
      name: "Iron Tool Crafting"
      description: "Creating quality tools"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%jobsadventure_crafter_player_level%"
          operator: "greater_than"
          value: "15"
    
    diamond_tools:
      target: "DIAMOND_PICKAXE,DIAMOND_AXE,DIAMOND_SHOVEL,DIAMOND_SWORD"
      xp: 20.0
      name: "Diamond Tool Crafting"
      description: "Creating master tools"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%jobsadventure_crafter_player_level%"
          operator: "greater_than"
          value: "35"
  
  # Enchanting
  ENCHANT:
    basic_enchant:
      target: "ANY"
      xp: 5.0
      name: "Basic Enchanting"
      description: "Enchanting items"
    
    high_level_enchant:
      target: "ANY"
      xp: 15.0
      name: "Advanced Enchanting"
      description: "High-level enchantments"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%player_level%"
          operator: "greater_than"
          value: "30"
  
  # Alchemy
  BREW:
    healing_potion:
      target: "POTION_HEALING"
      xp: 10.0
      name: "Healing Potion Preparation"
      description: "Brewing healing potions"
    
    strength_potion:
      target: "POTION_STRENGTH"
      xp: 15.0
      name: "Strength Potion Preparation"
      description: "Brewing combat potions"
  
  # Smelting
  SMELT:
    iron_ingot:
      target: "IRON_INGOT"
      xp: 3.0
      name: "Iron Smelting"
      description: "Transforming ore into ingot"
    
    gold_ingot:
      target: "GOLD_INGOT"
      xp: 5.0
      name: "Gold Smelting"
      description: "Refining precious gold"
```

## 🎯 Configuration Tips

### XP Curve Balancing
```yaml
# Slow and steady progression
xp-equation: "100 * Math.pow(level, 1.2)"

# Fast start, slower later
xp-equation: "50 * Math.pow(level, 1.8) + level * 25"

# Very difficult for prestige jobs
xp-equation: "200 * Math.pow(level, 2.5)"
```

### Complex Conditions
```yaml
requirements:
  logic: "AND"
  # Must have permission AND be in right world
  permission:
    permission: "vip.mining"
  world:
    worlds: ["mining_world"]
  # AND have certain level OR special item
  groups:
    group1:
      logic: "OR"
      placeholder:
        placeholder: "%jobsadventure_miner_player_level%"
        operator: "greater_than"
        value: "50"
      item:
        mmoitems:
          type: "TOOL"
          id: "MASTER_PICKAXE"
```

### Dynamic Messages
```yaml
# Different messages based on level
message:
  type: "BOSSBAR"
  color: "GREEN"
  duration: 60
  # Using placeholders
  message: "&a+{exp} XP {job} &7| Total: %jobsadventure_{job}_player_xp%"
```

## 📁 File Organization

```
/plugins/JobsAdventure/
├── jobs/
│   ├── miner.yml
│   ├── farmer.yml
│   ├── hunter.yml
│   ├── crafter.yml
│   └── custom_job.yml
├── rewards/
│   ├── miner_rewards.yml
│   ├── farmer_rewards.yml
│   └── hunter_rewards.yml
├── gui/
│   ├── miner_gui.yml
│   └── farmer_gui.yml
└── xp-curves/
    ├── linear.yml
    ├── steep.yml
    └── custom.yml
```

## 🔗 See Also

- [Creating New Jobs](creating-jobs.md)
- [Conditions System](conditions-system.md)
- [Experience Curves](xp-curves.md)
- [Reward Configuration](../rewards/reward-configuration.md)
- [Plugin Integrations](../integrations/placeholderapi.md)