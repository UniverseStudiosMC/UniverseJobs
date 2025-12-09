# Plugin Integrations

UniverseJobs integrates with many popular plugins to extend functionality.

## Required Plugins

| Plugin | Purpose | Link |
|--------|---------|------|
| Vault | Economy system | [SpigotMC](https://www.spigotmc.org/resources/vault.34315/) |

## Optional Plugins

| Plugin | Purpose |
|--------|---------|
| PlaceholderAPI | Placeholder support |
| Nexo | Custom blocks and items |
| ItemsAdder | Custom blocks and items |
| Oraxen | Custom blocks and items |
| MythicMobs | Custom mobs |
| mcMMO | Skill integration |
| CustomCrops | Custom crops |
| CustomFishing | Custom fishing |
| CraftEngine | Custom crafting |

---

## Vault

**Required for economy features.**

UniverseJobs uses Vault for all money transactions. Any economy plugin that hooks into Vault will work:
- EssentialsX
- CMI
- PlayerPoints
- CoinsEngine
- etc.

### Setup

1. Install Vault
2. Install an economy plugin
3. Restart server

Money rewards will automatically use the configured economy.

---

## PlaceholderAPI

**Recommended for advanced features.**

Enables placeholders in scoreboards, holograms, chat, and more.

### Setup

1. Install PlaceholderAPI
2. Restart server
3. Placeholders auto-register

See [Placeholders](Placeholders) for all available placeholders.

---

## Nexo

Integration for Nexo custom blocks and items.

### Custom Blocks

Use Nexo blocks as targets:

```yaml
actions:
  break:
    - target: "nexo:ruby_ore"
      xp: 75
      money: 15

    - target: "nexo:custom_stone"
      xp: 10
      money: 2
```

### Custom Items

Use Nexo items in rewards:

```yaml
rewards:
  custom_tool:
    items:
      - nexo-id: "custom_pickaxe"
        amount: 1
```

### Conditions

Check for Nexo items in hand:

```yaml
requirements:
  nexo-id: "custom_pickaxe"
```

---

## ItemsAdder

Integration for ItemsAdder custom content.

### Custom Blocks

```yaml
actions:
  break:
    - target: "itemsadder:custom_ore"
      xp: 50
      money: 10
```

### Custom Items

```yaml
rewards:
  ia_reward:
    items:
      - itemsadder-id: "namespace:custom_item"
        amount: 1
```

### Custom Crops

Works with ItemsAdder crops via harvest action:

```yaml
actions:
  harvest:
    - target: "itemsadder:tomato_crop"
      xp: 15
      money: 3
```

### Conditions

```yaml
requirements:
  itemsadder-id: "namespace:custom_tool"
```

---

## Oraxen

Integration for Oraxen custom content.

### Custom Blocks

```yaml
actions:
  break:
    - target: "oraxen:amethyst_ore"
      xp: 60
      money: 12
```

### Custom Items

```yaml
rewards:
  oraxen_reward:
    items:
      - oraxen-id: "custom_sword"
        amount: 1
```

---

## MythicMobs

Integration for MythicMobs custom entities.

### Kill Actions

```yaml
actions:
  kill:
    # Specific MythicMob
    - target: "mythicmobs:SkeletonKing"
      xp: 500
      money: 100

    # MythicMobs with namespace
    - target: "mm:CustomZombie"
      xp: 50
      money: 10

    # All MythicMobs (wildcard)
    - target: "mythicmobs:*"
      xp: 25
      money: 5
```

### Display

MythicMobs targets show a wither skeleton skull icon in menus by default.

---

## mcMMO

Integration with mcMMO skills and abilities.

### Ability Multipliers

Reduce rewards during mcMMO abilities to prevent exploitation:

```yaml
# In job file or config.yml
mcmmo:
  superbreaker:
    money-multiplier: 0.5      # 50% money during Super Breaker
    xp-multiplier: 0.5         # 50% XP during Super Breaker
  gigadrillbreaker:
    money-multiplier: 0.5
    xp-multiplier: 0.5
```

### Supported Abilities

| Ability | Action Type |
|---------|-------------|
| Super Breaker | BREAK |
| Giga Drill Breaker | BREAK |
| Tree Feller | BREAK |
| Green Terra | HARVEST |
| Serrated Strikes | KILL |
| Berserk | BREAK |

---

## CustomCrops

Integration for CustomCrops plugin.

### Harvest Actions

```yaml
actions:
  harvest:
    - target: "customcrops:tomato"
      xp: 20
      money: 5

    - target: "customcrops:corn"
      xp: 15
      money: 3

    - target: "customcrops:*"  # All custom crops
      xp: 10
      money: 2
```

### Events

UniverseJobs listens to CustomCrops harvest events automatically.

---

## CustomFishing

Integration for CustomFishing plugin.

### Fish Actions

```yaml
actions:
  fish:
    - target: "customfishing:golden_fish"
      xp: 100
      money: 25

    - target: "customfishing:rare_catch"
      xp: 200
      money: 50

    - target: "customfishing:*"
      xp: 15
      money: 3
```

---

## CraftEngine

Integration for CraftEngine custom crafting recipes.

### Craft Actions

```yaml
actions:
  craft:
    - target: "craftengine:custom_sword"
      xp: 50
      money: 10

    - target: "craftengine:advanced_tool"
      xp: 75
      money: 20
```

---

## Folia Support

UniverseJobs is fully compatible with Folia servers.

### Automatic Detection

The plugin automatically detects if running on Folia and adjusts:
- All schedulers use region-based scheduling
- Async operations are properly handled
- No configuration needed

### Thread Safety

All operations are thread-safe:
- Player data access uses ReadWriteLock
- Database operations are async
- Cache updates are atomic

---

## Integration Priority

When multiple plugins provide similar features, UniverseJobs checks in order:

1. **Nexo** (if installed)
2. **ItemsAdder** (if installed)
3. **Oraxen** (if installed)
4. **Vanilla Minecraft**

For mobs:
1. **MythicMobs** (if target contains `:`)
2. **Vanilla EntityType**

---

## Adding Custom Integrations

UniverseJobs supports the `CUSTOM` action type for plugin developers:

```yaml
actions:
  custom:
    - target: "myplugin:custom_action"
      xp: 50
      money: 10
```

Developers can fire custom events that UniverseJobs will process. See the API documentation for details.

---

## Troubleshooting

### Plugin Not Detected

1. Check plugin is installed and enabled
2. Check load order (UniverseJobs should load after integration plugins)
3. Check console for errors
4. Restart server

### Custom Items Not Working

1. Verify the item ID is correct
2. Check namespace format (`plugin:item_id`)
3. Test with `/jobs admin reload`

### MythicMobs Not Giving Rewards

1. Ensure target format is `mythicmobs:MobName` or `mm:MobName`
2. Check mob internal name (not display name)
3. Verify mob is actually a MythicMob

### mcMMO Multipliers Not Applying

1. Check mcMMO section in job config
2. Verify ability names are correct
3. Test with debug mode enabled
