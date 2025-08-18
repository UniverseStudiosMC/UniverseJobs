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
# ===================================
# MINER JOB CONFIGURATION
# ===================================
# This job rewards players for mining blocks and fighting underground creatures
# Configure XP rewards, level up actions, and mining-related activities

# Basic job information
name: "Miner"
description: "Extract valuable resources from the depths of the earth"
enabled: true
max-level: 100
permission: "jobsadventure.job.miner"
icon: "DIAMOND_PICKAXE"

# XP progression curve
xp-equation: "100 * Math.pow(level, 1.8)"

# XP message display
xp-message:
  type: "actionbar"
  text: "&e+{exp} XP &8| &6{job} &7Level {level}"
  actionbar:
    duration: 60

# Job description and lore
lore:
  - "&7Dig deep and discover precious ores!"
  - "&7Level up by mining stones and ores"
  - "&7Unlock better tools and rewards"

# ===================================
# JOB ACTIONS - WHAT GIVES XP
# ===================================
actions:
  BREAK:
    # Basic materials
    stone:
      target: "STONE"
      xp: 1.0
      name: "Stone Mining"
    
    coal_ore:
      target: "COAL_ORE"
      xp: 3.0
      name: "Coal Mining"
    
    iron_ore:
      target: "IRON_ORE"
      xp: 5.0
      name: "Iron Mining"
      requirements:
        logic: "AND"
        item:
          material: "IRON_PICKAXE"
          deny:
            message: "&cYou need at least an iron pickaxe!"
    
    gold_ore:
      target: "GOLD_ORE"
      xp: 8.0
      name: "Gold Mining"
    
    diamond_ore:
      target: "DIAMOND_ORE"
      xp: 15.0
      name: "Diamond Mining"
      requirements:
        logic: "AND"
        item:
          material: "DIAMOND_PICKAXE"
          deny:
            message: "&cYou need a diamond pickaxe for this!"
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "16"
          deny:
            message: "&cDiamonds are only found at Y level 16 or below!"
    
    ancient_debris:
      target: "ANCIENT_DEBRIS"
      xp: 25.0
      name: "Ancient Debris Mining"
      requirements:
        logic: "AND"
        item:
          material: "NETHERITE_PICKAXE"
          deny:
            message: "&cAncient debris requires a netherite pickaxe!"

  KILL:
    # Underground creatures
    zombie:
      target: "ZOMBIE"
      xp: 2.0
      name: "Cave Zombie Elimination"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "50"
          deny:
            message: "&cThis bonus only applies underground!"
    
    skeleton:
      target: "SKELETON"
      xp: 2.5
      name: "Cave Skeleton Elimination"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "50"

# ===================================
# LEVEL UP ACTIONS - REWARDS & EFFECTS
# ===================================
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
  
  # Tool rewards at specific levels
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
  
  legendary_miner:
    type: "command"
    levels: [100]
    commands:
      - "broadcast &6&l🌟 {player} &7has achieved &eLEGENDARY MINER &7status! &6&l🌟"
      - "give {player} netherite_ingot 5"
      - "give {player} diamond_block 3"
```

## 🌾 Complete Example: Farmer Job

```yaml
# ===================================
# FARMER JOB CONFIGURATION  
# ===================================
# This job rewards players for farming, breeding animals, and food production
# Configure crop growing, animal care, and agricultural activities

# Basic job information
name: "Farmer"
description: "Cultivate the land and raise animals for sustenance"
enabled: true
max-level: 75
permission: "jobsadventure.job.farmer"
icon: "GOLDEN_HOE"

# XP progression curve (gentler than miner)
xp-curve: "gentle"

# XP message display
xp-message:
  type: "bossbar"
  text: "&a+{exp} EXP &7| &2{job} &7Level {level}"
  bossbar:
    color: "green"
    style: "segmented_10"
    duration: 60
    show-progress: true

# Job description and lore
lore:
  - "&7Grow crops and care for animals"
  - "&7Master the art of sustainable farming"
  - "&7Feed the world with your harvest"

# ===================================
# JOB ACTIONS - WHAT GIVES XP
# ===================================
actions:
  BREAK:
    # Crop harvesting
    wheat:
      target: "WHEAT"
      xp: 2.0
      name: "Wheat Harvesting"
    
    carrots:
      target: "CARROTS"
      xp: 2.5
      name: "Carrot Harvesting"
    
    potatoes:
      target: "POTATOES"
      xp: 2.5
      name: "Potato Harvesting"
    
    beetroots:
      target: "BEETROOTS"
      xp: 3.0
      name: "Beetroot Harvesting"
    
    pumpkin:
      target: "PUMPKIN"
      xp: 4.0
      name: "Pumpkin Harvesting"
    
    melon:
      target: "MELON"
      xp: 4.0
      name: "Melon Harvesting"
    
    sugar_cane:
      target: "SUGAR_CANE"
      xp: 1.5
      name: "Sugar Cane Harvesting"

  PLACE:
    # Planting crops
    wheat_seeds:
      target: "WHEAT_SEEDS"
      xp: 1.0
      name: "Wheat Planting"
    
    carrot_planting:
      target: "CARROTS"
      xp: 1.0
      name: "Carrot Planting"
    
    potato_planting:
      target: "POTATOES"
      xp: 1.0
      name: "Potato Planting"

  BREED:
    # Animal breeding
    cow:
      target: "COW"
      xp: 8.0
      name: "Cow Breeding"
      requirements:
        logic: "AND"
        item:
          material: "WHEAT"
          deny:
            message: "&cYou need wheat to breed cows!"
    
    pig:
      target: "PIG"
      xp: 6.0
      name: "Pig Breeding"
      requirements:
        logic: "AND"
        item:
          material: "CARROT"
          deny:
            message: "&cYou need carrots to breed pigs!"
    
    chicken:
      target: "CHICKEN"
      xp: 4.0
      name: "Chicken Breeding"
      requirements:
        logic: "AND"
        item:
          material: "WHEAT_SEEDS"
          deny:
            message: "&cYou need seeds to breed chickens!"
    
    sheep:
      target: "SHEEP"
      xp: 5.0
      name: "Sheep Breeding"
      requirements:
        logic: "AND"
        item:
          material: "WHEAT"
          deny:
            message: "&cYou need wheat to breed sheep!"

  MILK:
    # Animal care
    cow_milk:
      target: "COW"
      xp: 2.0
      name: "Cow Milking"

  SHEAR:
    # Wool collection
    sheep_shear:
      target: "SHEEP"
      xp: 3.0
      name: "Sheep Shearing"
      requirements:
        logic: "AND"
        item:
          material: "SHEARS"
          deny:
            message: "&cYou need shears to properly shear sheep!"

  FISH:
    # Basic fishing
    general_fishing:
      target: "COD"
      xp: 3.0
      name: "Fishing"
    
    salmon_fishing:
      target: "SALMON"
      xp: 4.0
      name: "Salmon Fishing"

# ===================================
# LEVEL UP ACTIONS - REWARDS & EFFECTS
# ===================================
levelup-actions:
  # Welcome message for new farmers
  welcome_farmer:
    type: "message"
    levels: [1]
    messages:
      - "&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
      - "&a&l🌾 WELCOME TO THE FARMER JOB! 🌾"
      - "&7You are now a level &e{level} &7farmer!"
      - "&7Grow crops and care for animals!"
      - "&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
  
  # Basic level up effects
  level_up_sound:
    type: "sound"
    min-level: 1
    sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
  
  level_up_title:
    type: "title"
    min-level: 2
    title: "&a&lLEVEL UP!"
    subtitle: "&7Farmer Level &e{level}"
    fade-in: 10
    stay: 50
    fade-out: 15
  
  level_up_particles:
    type: "particle"
    min-level: 1
    particle: "VILLAGER_HAPPY"
    count: 20
  
  # Starter supplies
  starter_seeds:
    type: "command"
    levels: [3]
    commands:
      - "give {player} wheat_seeds 32"
      - "give {player} carrot 16"
      - "give {player} potato 16"
      - "tellraw {player} {\"text\":\"🌱 Starter farming supplies! Plant these to begin your agricultural journey.\",\"color\":\"green\"}"
  
  # Tool upgrades
  iron_hoe_reward:
    type: "command"
    levels: [10]
    commands:
      - "give {player} iron_hoe 1"
      - "tellraw {player} {\"text\":\"🔧 Iron Hoe unlocked! More efficient farming awaits.\",\"color\":\"gray\"}"
  
  diamond_hoe_reward:
    type: "command"
    levels: [30]
    commands:
      - "give {player} diamond_hoe 1"
      - "tellraw {player} {\"text\":\"💎 Diamond Hoe acquired! The ultimate farming tool.\",\"color\":\"aqua\"}"
  
  # Animal starter pack
  animal_starter:
    type: "command"
    levels: [15]
    commands:
      - "give {player} cow_spawn_egg 2"
      - "give {player} pig_spawn_egg 2"
      - "give {player} chicken_spawn_egg 2"
      - "tellraw {player} {\"text\":\"🐄 Animal starter pack! Begin your livestock operations.\",\"color\":\"yellow\"}"
  
  # Food rewards every 5 levels
  food_rewards:
    type: "command"
    min-level: 5
    level-interval: 5
    commands:
      - "give {player} bread 16"
      - "give {player} cooked_beef 8"
      - "tellraw {player} {\"text\":\"🍞 Nutritious food reward for level {level}!\",\"color\":\"gold\"}"
  
  # Major milestones
  milestone_broadcast:
    type: "broadcast"
    levels: [20, 40, 60, 75]
    messages:
      - "&a🌾 {player} &7has reached level &e{level} &7in the Farmer job! 🌾"
  
  # Master farmer achievement
  master_farmer:
    type: "command"
    levels: [50]
    commands:
      - "broadcast &a🏆 {player} &7has become a &2Master Farmer&7! 🏆"
      - "give {player} golden_apple 5"
      - "give {player} enchanted_golden_apple 1"
      - "give {player} emerald 10"
  
  # Legendary farmer
  legendary_farmer:
    type: "command"
    levels: [75]
    commands:
      - "broadcast &a&l🌟 {player} &7has achieved &2LEGENDARY FARMER &7status! &a&l🌟"
      - "give {player} totem_of_undying 1"
      - "give {player} diamond 15"
```

## 🏹 Complete Example: Hunter Job

```yaml
# ===================================
# HUNTER JOB CONFIGURATION
# ===================================
# This job rewards players for hunting creatures and combat activities
# Configure mob hunting, archery skills, and survival challenges

# Basic job information
name: "Hunter"
description: "Master of the wild and expert tracker"
enabled: true
max-level: 80
permission: "jobsadventure.job.hunter"
icon: "BOW"

# XP progression curve (moderate difficulty)
xp-equation: "120 * Math.pow(level, 1.6)"

# XP message display
xp-message:
  type: "chat"
  text: "&c+{exp} XP &8| &4{job} &7Level {level} &8(&e{total_xp}&8)"

# Job description and lore
lore:
  - "&7Track and hunt dangerous creatures"
  - "&7Master archery and survival skills"
  - "&7Protect others from monster threats"

# ===================================
# JOB ACTIONS - WHAT GIVES XP
# ===================================
actions:
  KILL:
    # Basic hostile mobs
    zombie:
      target: "ZOMBIE"
      xp: 3.0
      name: "Zombie Hunting"
    
    skeleton:
      target: "SKELETON"
      xp: 4.0
      name: "Skeleton Hunting"
    
    spider:
      target: "SPIDER"
      xp: 2.5
      name: "Spider Hunting"
    
    # Dangerous mobs with higher XP
    creeper:
      target: "CREEPER"
      xp: 8.0
      name: "Creeper Elimination"
      requirements:
        logic: "AND"
        item:
          material: "BOW"
          deny:
            message: "&cUse a bow to safely eliminate creepers!"
    
    enderman:
      target: "ENDERMAN"
      xp: 12.0
      name: "Enderman Hunting"
    
    witch:
      target: "WITCH"
      xp: 10.0
      name: "Witch Hunting"
    
    # Nether creatures
    blaze:
      target: "BLAZE"
      xp: 15.0
      name: "Blaze Hunting"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%player_world%"
          operator: "equals"
          value: "world_nether"
          deny:
            message: "&cBlazes can only be hunted in the Nether!"
    
    ghast:
      target: "GHAST"
      xp: 20.0
      name: "Ghast Hunting"
    
    wither_skeleton:
      target: "WITHER_SKELETON"
      xp: 18.0
      name: "Wither Skeleton Hunting"
    
    # Boss creatures
    ender_dragon:
      target: "ENDER_DRAGON"
      xp: 500.0
      name: "Dragon Slaying"
    
    wither:
      target: "WITHER"
      xp: 300.0
      name: "Wither Slaying"
    
    # Animals (lower XP, for food hunting)
    cow:
      target: "COW"
      xp: 1.0
      name: "Cattle Hunting"
    
    pig:
      target: "PIG"
      xp: 1.0
      name: "Pig Hunting"
    
    chicken:
      target: "CHICKEN"
      xp: 0.5
      name: "Chicken Hunting"
    
    rabbit:
      target: "RABBIT"
      xp: 1.5
      name: "Rabbit Hunting"

  FISH:
    # Fishing for survival
    cod:
      target: "COD"
      xp: 2.0
      name: "Cod Fishing"
    
    salmon:
      target: "SALMON"
      xp: 2.5
      name: "Salmon Fishing"
    
    tropical_fish:
      target: "TROPICAL_FISH"
      xp: 3.0
      name: "Tropical Fish Fishing"

# ===================================
# LEVEL UP ACTIONS - REWARDS & EFFECTS
# ===================================
levelup-actions:
  # Welcome message for new hunters
  welcome_hunter:
    type: "message"
    levels: [1]
    messages:
      - "&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
      - "&c&l🏹 WELCOME TO THE HUNTER JOB! 🏹"
      - "&7You are now a level &e{level} &7hunter!"
      - "&7Hunt creatures and master survival!"
      - "&8&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
  
  # Basic level up effects
  level_up_sound:
    type: "sound"
    min-level: 1
    sound: "ENTITY_ARROW_HIT_PLAYER"
  
  level_up_title:
    type: "title"
    min-level: 2
    title: "&c&lLEVEL UP!"
    subtitle: "&7Hunter Level &e{level}"
    fade-in: 10
    stay: 50
    fade-out: 20
  
  level_up_particles:
    type: "particle"
    min-level: 1
    particle: "CRIT"
    count: 30
  
  # Weapon progression
  bow_upgrade:
    type: "command"
    levels: [5]
    commands:
      - "give {player} bow 1"
      - "give {player} arrow 64"
      - "tellraw {player} {\"text\":\"🏹 Hunter's Bow and arrows! Ready for the hunt.\",\"color\":\"red\"}"
  
  crossbow_upgrade:
    type: "command"
    levels: [20]
    commands:
      - "give {player} crossbow 1"
      - "tellraw {player} {\"text\":\"🎯 Crossbow unlocked! Precise and powerful.\",\"color\":\"dark_red\"}"
  
  trident_reward:
    type: "command"
    levels: [40]
    commands:
      - "give {player} trident 1"
      - "tellraw {player} {\"text\":\"🔱 Legendary Trident! A weapon of the seas.\",\"color\":\"aqua\"}"
  
  # Survival supplies every 10 levels
  survival_kit:
    type: "command"
    min-level: 10
    level-interval: 10
    commands:
      - "give {player} cooked_beef 16"
      - "give {player} potion{Potion:\"minecraft:healing\"} 3"
      - "tellraw {player} {\"text\":\"🥩 Survival supplies for level {level}! Stay nourished on your hunts.\",\"color\":\"gold\"}"
  
  # Armor upgrades
  leather_armor:
    type: "command"
    levels: [8]
    commands:
      - "give {player} leather_helmet 1"
      - "give {player} leather_chestplate 1"
      - "give {player} leather_leggings 1"
      - "give {player} leather_boots 1"
      - "tellraw {player} {\"text\":\"🧥 Hunter's Leather Armor! Basic protection for your adventures.\",\"color\":\"brown\"}"
  
  iron_armor:
    type: "command"
    levels: [25]
    commands:
      - "give {player} iron_helmet 1"
      - "give {player} iron_chestplate 1"
      - "give {player} iron_leggings 1"
      - "give {player} iron_boots 1"
      - "tellraw {player} {\"text\":\"⚔️ Iron Armor set! Enhanced protection for dangerous hunts.\",\"color\":\"gray\"}"
  
  # Major milestones
  milestone_broadcast:
    type: "broadcast"
    levels: [15, 30, 50, 70, 80]
    messages:
      - "&c🏹 {player} &7has reached level &e{level} &7in the Hunter job! 🏹"
  
  # Special achievements
  expert_hunter:
    type: "command"
    levels: [35]
    commands:
      - "broadcast &c🎯 {player} &7has become an &4Expert Hunter&7! 🎯"
      - "give {player} diamond 8"
      - "give {player} enchanted_book 1"
  
  master_hunter:
    type: "command"
    levels: [60]
    commands:
      - "broadcast &c🏆 {player} &7has become a &4Master Hunter&7! 🏆"
      - "give {player} netherite_ingot 2"
      - "give {player} totem_of_undying 1"
  
  # Legendary achievement
  legendary_hunter:
    type: "command"
    levels: [80]
    commands:
      - "broadcast &c&l🌟 {player} &7has achieved &4LEGENDARY HUNTER &7status! &c&l🌟"
      - "give {player} netherite_sword 1"
      - "give {player} elytra 1"
      - "give {player} diamond_block 5"
```

## 🎯 Level Up Actions System

JobsAdventure now features a powerful level up actions system that allows 100% customizable rewards and effects when players level up. Here's how it works:

### Basic Structure
```yaml
levelup-actions:
  action_name:
    type: "action_type"
    levels: [1, 5, 10]  # Specific levels
    # OR
    min-level: 5
    level-interval: 10  # Every 10 levels starting from 5
    # Action-specific configuration
```

### Available Action Types

**Message Actions** - Send formatted messages
```yaml
welcome_message:
  type: "message"
  levels: [1]
  messages:
    - "&6Welcome to the {job} job!"
    - "&7You are now level &e{level}&7!"
```

**Command Actions** - Execute any server commands
```yaml
tool_reward:
  type: "command"
  levels: [5, 10, 15]
  commands:
    - "give {player} iron_pickaxe 1"
    - "tellraw {player} {\"text\":\"🎁 Tool reward!\",\"color\":\"gold\"}"
```

**Sound & Visual Effects**
```yaml
level_sound:
  type: "sound"
  min-level: 1
  sound: "ENTITY_PLAYER_LEVELUP"

level_title:
  type: "title"
  min-level: 2
  title: "&6&lLEVEL UP!"
  subtitle: "&7{job} Level &e{level}"

level_particles:
  type: "particle"
  min-level: 1
  particle: "FLAME"
  count: 25

announcements:
  type: "broadcast"
  levels: [25, 50, 75, 100]
  messages:
    - "&6{player} &7reached level &e{level} &7in {job}!"
```

### Placeholder Support
All actions support these placeholders:
- `{player}` - Player name
- `{job}` - Job display name
- `{level}` or `{newlevel}` - New level reached
- `{oldlevel}` - Previous level
- `{totalxp}` - Total XP in the job
- `{xpgained}` - XP that triggered the level up

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