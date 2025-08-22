# WORLD

Restricts actions to specific worlds or excludes certain worlds.

## Description

World requirements check which world the player is currently in. This allows you to enable jobs only in specific worlds (whitelist mode) or disable them in certain worlds (blacklist mode). Perfect for controlling where job activities can take place.

## Basic Configuration

```yaml
requirements:
  world:
    worlds: 
      - "mining_world"
      - "survival_world"
    blacklist: false  # false = whitelist (allow only these), true = blacklist (deny these)
    deny:
      message: "&cThis action is not allowed in this world!"
      sound: "ENTITY_VILLAGER_NO"
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `worlds` | List | [] | List of world names to check |
| `blacklist` | Boolean | false | false = whitelist mode, true = blacklist mode |

## Whitelist vs Blacklist

### Whitelist Mode (blacklist: false)
**Only allows action in specified worlds**

```yaml
requirements:
  world:
    worlds: 
      - "mining_world"
      - "resource_world"
    blacklist: false
    deny:
      message: "&cMining only allowed in mining or resource worlds!"
      commands:
        - "spawn {player}"
```

### Blacklist Mode (blacklist: true)
**Prevents action in specified worlds**

```yaml
requirements:
  world:
    worlds: 
      - "spawn"
      - "lobby"
      - "creative_world"
    blacklist: true
    deny:
      message: "&cJobs are disabled in spawn/lobby areas!"
      sound: "ENTITY_VILLAGER_NO"
```

## Advanced Configuration

### Multi-World Job Restrictions

```yaml
diamond_mining:
  target: "DIAMOND_ORE"
  xp: 50.0
  money: 200.0
  requirements:
    logic: "AND"
    multiple_condition:
      mining_worlds:
        world:
          worlds:
            - "mining_world"
            - "deep_mines"
            - "nether_mines"
          blacklist: false
        deny:
          message: "&cDiamond mining only allowed in designated mining worlds!"
          commands:
            - "tellraw {player} \"Use /warp mining to go to mining world!\""
      
      not_protected:
        world:
          worlds:
            - "protected_area"
            - "spawn_region"
          blacklist: true
        deny:
          message: "&cMining forbidden in protected areas!"
      
      accept:
        message: "&a💎 Diamond mining area confirmed!"
        sound: "BLOCK_NOTE_BLOCK_CHIME"
```

### Dimension-Specific Jobs

```yaml
nether_harvesting:
  target: "NETHER_WART"
  xp: 15.0
  requirements:
    logic: "AND"
    multiple_condition:
      nether_dimension:
        world:
          worlds:
            - "world_nether"
            - "custom_nether"
          blacklist: false
        deny:
          message: "&cNether wart can only be harvested in the Nether!"
          commands:
            - "tellraw {player} \"Use /nether to travel to the Nether dimension!\""
      
      not_fortress:
        placeholder:
          placeholder: "%player_location_structure%"
          operator: "not_equals"
          value: "nether_fortress"
        accept:
          message: "&eSafe nether harvesting area!"
        deny:
          message: "&cAvoid harvesting near fortress structures!"
      
      accept:
        message: "&c🔥 Nether harvesting bonus!"
        commands:
          - "effect give {player} fire_resistance 300 1"
```

## World Categories

### Survival Worlds

```yaml
survival_mining:
  target: "IRON_ORE"
  xp: 10.0
  requirements:
    world:
      worlds:
        - "world"           # Main survival world
        - "survival_world"  # Additional survival
        - "hardcore_world"  # Hardcore mode
      blacklist: false
    deny:
      message: "&cSurvival mining only in designated survival worlds!"
```

### Creative/Staff Exclusions

```yaml
no_creative_jobs:
  target: "STONE"
  xp: 1.0
  requirements:
    world:
      worlds:
        - "creative"        # Creative world
        - "build_world"     # Building world
        - "staff_world"     # Staff testing
      blacklist: true
    deny:
      message: "&cJobs disabled in creative/staff worlds!"
      cancel-event: true
```

### Event Worlds

```yaml
event_fishing:
  target: "SALMON"
  xp: 25.0
  requirements:
    logic: "AND"
    multiple_condition:
      event_world:
        world:
          worlds:
            - "fishing_tournament"
            - "event_island"
          blacklist: false
        deny:
          message: "&cEvent fishing only in tournament worlds!"
      
      event_active:
        placeholder:
          placeholder: "%server_event_active%"
          operator: "equals"
          value: "true"
        deny:
          message: "&cNo active fishing event!"
      
      accept:
        message: "&b🏆 Tournament fishing area!"
        commands:
          - "broadcast {player} is participating in the fishing tournament!"
```

### Mining World Tiers

```yaml
tier_3_mining:
  target: "ANCIENT_DEBRIS"
  xp: 500.0
  requirements:
    logic: "AND"
    multiple_condition:
      elite_world:
        world:
          worlds:
            - "elite_mining"
            - "deep_nether"
            - "hardcore_nether"
          blacklist: false
        deny:
          message: "&cAncient debris mining only in elite worlds!"
          commands:
            - "tellraw {player} \"Requires access to elite mining worlds!\""
      
      not_beginner:
        world:
          worlds:
            - "beginner_world"
            - "tutorial_world"
          blacklist: true
        deny:
          message: "&cAncient debris not available in beginner areas!"
      
      access_level:
        placeholder:
          placeholder: "%UniverseJobs_level_miner%"
          operator: "greater_equal"
          value: "75"
        deny:
          message: "&cLevel 75+ required for elite mining worlds!"
      
      accept:
        message: "&4🔥 ELITE MINING ZONE ACCESSED!"
        commands:
          - "effect give {player} fire_resistance 1200 1"
          - "broadcast &4{player} &6entered the elite mining zone!"
```

## World Integration Examples

### Multiverse Integration

```yaml
multiverse_worlds:
  world:
    worlds:
      - "survival_normal"    # Multiverse world
      - "survival_amplified" # Multiverse world
      - "mining_void"        # Void mining world
    blacklist: false
  deny:
    message: "&cThis job only works in Multiverse survival worlds!"
```

### Custom World Names

```yaml
custom_worlds:
  world:
    worlds:
      - "MyServer_Mining"
      - "CustomWorld_Resources"
      - "Event_FishingContest"
    blacklist: false
  deny:
    message: "&cAction only available in custom server worlds!"
```

### Hub/Network Restrictions

```yaml
network_restriction:
  world:
    worlds:
      - "Hub"
      - "Lobby"
      - "MainLobby"
      - "NetworkHub"
    blacklist: true  # Prevent in these worlds
  deny:
    message: "&cJobs disabled in lobby/hub areas!"
    commands:
      - "tellraw {player} \"Use /server survival to access jobs!\""
```

## Practical Examples

### Resource World Reset Protection

```yaml
temp_resource_mining:
  target: "DIAMOND_ORE"
  xp: 30.0
  requirements:
    logic: "AND"
    multiple_condition:
      resource_world:
        world:
          worlds:
            - "resource_world"
            - "temp_mining"
          blacklist: false
        accept:
          message: "&eResource world mining!"
          commands:
            - "tellraw {player} \"&cWarning: This world resets monthly!\""
        deny:
          message: "&cDiamonds only available in resource worlds!"
      
      world_age:
        placeholder:
          placeholder: "%world_age_days%"
          operator: "less_than"
          value: "25"  # Less than 25 days old
        accept:
          message: "&aFresh resource world!"
        deny:
          message: "&cResource world too old, reset coming soon!"
```

### PvP Zone Jobs

```yaml
pvp_combat_job:
  target: "PLAYER_KILL"
  xp: 100.0
  requirements:
    logic: "AND"
    multiple_condition:
      pvp_world:
        world:
          worlds:
            - "pvp_arena"
            - "warzone"
            - "factions_world"
          blacklist: false
        deny:
          message: "&cPvP jobs only in designated PvP worlds!"
      
      not_safe_zone:
        world:
          worlds:
            - "safe_spawn"
            - "neutral_zone"
          blacklist: true
        deny:
          message: "&cPvP disabled in safe zones!"
          cancel-event: true
      
      accept:
        message: "&c⚔️ PvP combat zone active!"
        commands:
          - "effect give {player} strength 60 1"
```

## World Aliases

For easier configuration, you can define world categories:

```yaml
# Survival category
survival_category:
  worlds:
    - "world"
    - "survival"
    - "survival_nether"
    - "survival_end"

# Creative category  
creative_category:
  worlds:
    - "creative"
    - "build"
    - "plots"

# Event category
event_category:
  worlds:
    - "event_arena"
    - "tournament_world"
    - "special_events"
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  world:
    worlds:
      - "farming_world"
      - "agriculture_zone"
    blacklist: false
    accept:
      message: "&aCustomCrops farming world!"
    deny:
      message: "&cCustomCrops only in farming worlds!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  world:
    worlds:
      - "nexo_world"
      - "custom_blocks_world"
    blacklist: false
    accept:
      message: "&6Nexo blocks world!"
    deny:
      message: "&cNexo features only in custom worlds!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  world:
    worlds:
      - "itemsadder_world"
      - "custom_items_zone"
    blacklist: false
    accept:
      message: "&bItemsAdder world access!"
    deny:
      message: "&cItemsAdder features restricted to special worlds!"
```
{% endtab %}
{% endtabs %}