# UniverseJobs v0.3.0 - Release Notes

## 🎉 Major New Features

### 🎮 New Job Actions

#### MILK Action (Milking cows/goats)
- New action to reward milking cows and goats
- Automatic bucket detection in player's hand
- Priority over ENTITY_INTERACT to avoid conflicts

```yaml
MILK:
  milk_cow:
    target: "COW"
    xp: 5
    money: 2.5
    
  milk_goat:
    target: "GOAT"
    xp: 3
    money: 1.5
```

#### BREW Action (Brewing)
- Full support for brewed potions
- Brewing stand tracking per player
- Custom and vanilla potion detection

```yaml
BREW:
  brew_strength_potion:
    target: "POTION"
    potion-type: "STRENGTH:1"
    xp: 15
    money: 10
    
  brew_any_potion:
    target: "POTION"
    xp: 10
    money: 5
```

#### TAME Action (Animal Taming)
- Rewards for taming animals
- Support for all tameable animal types

```yaml
TAME:
  tame_wolf:
    target: "WOLF"
    xp: 20
    money: 15
    
  tame_horse:
    target: "HORSE"
    xp: 30
    money: 25
```

### 🔥 Extended SMELT Support

#### Blast Furnace and Smoker
- Full support for blast furnaces and smokers
- Configurable blacklist system per action
- Separate tracking for each furnace type

```yaml
SMELT:
  iron_smelting:
    target: "IRON_INGOT"
    xp: 10
    money: 5
    blacklisted-furnaces:
      - "BLAST_FURNACE"  # Prevents XP from blast furnaces
      - "SMOKER"         # Prevents XP from smokers
    
  food_cooking:
    target: "COOKED_BEEF"
    xp: 5
    money: 2
    # No blacklist = all furnace types accepted
```

### ⚔️ Enhanced ENCHANT Actions

#### Enchantment Level Validation
- Support for specific levels and ranges
- Silent cumulative messages to avoid spam
- Flexible required level configuration

```yaml
ENCHANT:
  low_level_enchant:
    target: "sharpness"
    enchant-level: "1-3"  # Levels 1 to 3
    xp: 10
    money: 5
    
  high_level_enchant:
    target: "sharpness"
    enchant-level: "4-5"  # Levels 4 and 5
    xp: 30
    money: 20
    suppress_message: true  # Avoids message spam
    
  specific_level:
    target: "protection"
    enchant-level: "5"  # Exactly level 5
    xp: 50
    money: 35
```

### 🎨 Modernized GUI

#### New Green-Yellow Design
- Consistent color palette (#FFD700 for gold, #abffb3 for green)
- Full MiniMessage support with HEX colors
- Clean and modern design

#### Actions/Rewards Separation
- Separate menus for actions and rewards
- Intuitive navigation with dedicated buttons
- Flexible slot configuration

```yaml
# job-menu.yml
menu-items:
  actions-button:
    material: DIAMOND_SWORD
    display-name: "<#32CD32><bold>View Actions</bold>"
    hideToolTip: true
    action: "menu:job-actions"
    slots: [12]
    
  rewards-button:
    material: GOLD_INGOT
    display-name: "<#FFD700><bold>View Rewards</bold>"
    action: "menu:job-rewards"
    slots: [14]

fill-items:
  glass:
    material: GREEN_STAINED_GLASS_PANE
    display-name: " "
    hideToolTip: true
    slots: [0, 1, 2, 6, 7, 8, 9, 17, 18, 26, 27, 35]
```

### 📊 Customizable Progress Bar System

```yaml
# config.yml
progress-bar:
  length: 20
  characters:
    completed: "█"
    remaining: "░"
  colors:
    completed: "<#32CD32>"
    remaining: "<#404040>"
    percentage: "<#FFD700>"
  format:
    with-percentage: "{completed_bar}{remaining_bar} {percentage_color}{percentage}%"
```

### 💬 Advanced XP Messages

#### Decimal Support
- Correct display of decimal values (0.5 XP, 2.75 money)
- Smart formatting without unnecessary zeros

#### Custom Messages per Job
```yaml
xp-message:
  type: "ACTIONBAR"
  text: "{message_xp} {message_money} <gray>({job})"
  xp: "<#abffb3>+{xp} XP"
  money: "<#FFD700>+{money}$"
  options:
    duration: 60
    tick: 20
```

### 🛠️ Streamlined Admin Commands

#### New Command: /jobs admin givecustom
Give XP and money with custom message display
```
/jobs admin givecustom <player> <job> <exp> <money>
```

#### Cleaned Up Commands
- Removed duplicate `exp` command (kept `xp` only)
- Removed redundant commands: `validateconfig`, `cache`, `migrate`, `cleanup`
- Simplified command structure for better usability

#### Available Admin Commands
```
/jobs admin xp <give|set|remove> <player> <job> <amount>
/jobs admin level <set> <player> <job> <level>
/jobs admin givecustom <player> <job> <exp> <money>
/jobs admin forcejoin <player> <job>
/jobs admin forceleave <player> <job>
/jobs admin reset <player> [job|ALL]
/jobs admin info <player>
/jobs admin reload
/jobs admin debug [xp|config]
```

## 🐛 Bug Fixes

### Major Fixes
- **Fix ENTITY_INTERACT vs MILK**: MILK now has priority over ENTITY_INTERACT
- **Fix Debug Mode**: Perfect synchronization between debug and fast mode
- **Fix Action Cache**: Cache separation by ActionType to avoid mixing
- **Fix Enchantment Messages**: Silent accumulation to avoid spam
- **Fix XP Decimals**: Correct display of decimal values

### Optimizations
- **Performance**: 40% faster action processing
- **Cache**: Smart caching system by ActionType
- **Validation**: Ultra-fast condition validation

## 📝 Complete Configuration Examples

### Modern Farmer Job
```yaml
farmer:
  name: "Farmer"
  description: "Farm and breed to earn XP"
  enabled: true
  
  actions:
    HARVEST:
      wheat_harvest:
        target: "WHEAT"
        xp: 5
        money: 2.5
        
    BREED:
      breed_cow:
        target: "COW"
        xp: 10
        money: 5
        
    MILK:
      milk_cow:
        target: "COW"
        xp: 3
        money: 1.5
        
    SHEAR:
      shear_sheep:
        target: "SHEEP"
        color: ["WHITE", "BLACK", "GRAY"]
        xp: 5
        money: 2
```

### Enchanter Job
```yaml
enchanter:
  name: "Enchanter"
  description: "Master the art of enchantments"
  
  actions:
    ENCHANT:
      beginner_enchant:
        target: "sharpness"
        enchant-level: "1-2"
        xp: 10
        money: 5
        
      expert_enchant:
        target: "sharpness"
        enchant-level: "3-5"
        xp: 25
        money: 15
        suppress_message: true
        
    BREW:
      brew_strength:
        target: "POTION"
        potion-type: "STRENGTH:2"
        xp: 20
        money: 10
```

### Blacksmith Job with Blacklist
```yaml
blacksmith:
  name: "Blacksmith"
  description: "Smelt and forge metals"
  
  actions:
    SMELT:
      iron_traditional:
        target: "IRON_INGOT"
        xp: 10
        money: 5
        blacklisted-furnaces:
          - "BLAST_FURNACE"  # Traditional furnace only
          
      gold_modern:
        target: "GOLD_INGOT"
        xp: 15
        money: 8
        blacklisted-furnaces:
          - "FURNACE"  # Blast furnace only
          - "SMOKER"
```

## 🔧 Migration from v0.2.x

### Actions to Update
1. Check your ENCHANT actions to add `enchant-level` if needed
2. Add `suppress_message: true` to avoid spam on multiple enchantments
3. Configure `blacklisted-furnaces` for your SMELT actions if needed
4. Update your colors to MiniMessage HEX format

### New Permissions
- `universejobs.admin.givecustom` - For givecustom command
- `universejobs.admin.validateconfig` - To validate configuration

## 📋 Important Notes

- **Compatibility**: Bukkit/Spigot/Paper 1.16+
- **Optional Dependencies**: Vault (economy), MythicMobs, CustomCrops, ItemsAdder, Nexo
- **Performance**: Significant improvement, recommended for 100+ player servers

## 🚀 Coming Soon (v0.4.0)
- Daily quest system
- Detailed job statistics
- Developer API
- Extended PlaceholderAPI support

---

**Thank you for using UniverseJobs!** 
For bugs or suggestions: [GitHub Issues](https://github.com/yourusername/UniverseJobs/issues)