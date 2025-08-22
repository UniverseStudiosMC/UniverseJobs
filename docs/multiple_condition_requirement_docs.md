# MULTIPLE_CONDITION

Organizes multiple named requirements with individual messages and global fallbacks.

## Description

Multiple_condition provides a structured way to define several requirements with custom names, individual accept/deny messages, and global fallback messages. This system works with logic requirements (AND/OR) to create complex, well-organized conditional structures with clear feedback.

## Basic Configuration

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    permission_check:
      permission:
        permission: "jobs.mining"
      deny:
        message: "&cMining permission required!"
    
    level_check:
      placeholder:
        placeholder: "%player_level%"
        operator: "greater_than"
        value: "10"
      deny:
        message: "&cYou must be level 10+!"
```

## Advanced Configuration

### Individual Messages with Global Fallbacks

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    tool_requirement:
      item:
        material: "DIAMOND_PICKAXE"
      accept:
        message: "&aPerfect tool equipped!"
        sound: "BLOCK_NOTE_BLOCK_PLING"
      deny:
        message: "&cDiamond pickaxe required!"
        sound: "BLOCK_ANVIL_PLACE"
    
    vip_requirement:
      permission:
        permission: "jobs.vip"
      accept:
        message: "&6VIP access confirmed!"
        commands:
          - "effect give {player} haste 300 1"
      deny:
        message: "&cVIP membership required!"
    
    level_requirement:
      placeholder:
        placeholder: "%UniverseJobs_level_miner%"
        operator: "greater_equal"
        value: "25"
      deny:
        message: "&cLevel 25+ in mining required!"
    
    # Global messages (used when individual conditions don't specify)
    accept:
      message: "&a✅ All mining requirements met!"
      sound: "ENTITY_PLAYER_LEVELUP"
      commands:
        - "eco give {player} 100"
        - "broadcast {player} unlocked advanced mining!"
    
    deny:
      message: "&c❌ Mining requirements not satisfied!"
      sound: "ENTITY_VILLAGER_NO"
      cancel-event: true
```

### Complex Nested Structure

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    basic_access:
      permission:
        permission: "jobs.mining"
      deny:
        message: "&cBasic mining permission required!"
    
    location_and_time:
      logic: "AND"
      world:
        worlds: ["mining_world"]
      time:
        start: "6000"
        end: "18000"
      deny:
        message: "&cMining only allowed in mining world during day!"
    
    qualification_method:
      logic: "OR"  # Either VIP OR experienced
      vip_access:
        permission:
          permission: "jobs.vip"
        accept:
          message: "&6VIP qualification met!"
      
      experience_access:
        logic: "AND"
        placeholder:
          placeholder: "%UniverseJobs_level_miner%"
          operator: "greater_than"
          value: "50"
        item:
          material: "NETHERITE_PICKAXE"
        accept:
          message: "&aExperience qualification met!"
      
      deny:
        message: "&cEither VIP or level 50+ with netherite pickaxe required!"
    
    # Global configuration
    accept:
      message: "&a🎯 All advanced mining requirements satisfied!"
      commands:
        - "title {player} title \"&6Advanced Miner\""
        - "title {player} subtitle \"&eYou can now mine rare ores!\""
    deny:
      message: "&c🚫 Advanced mining access denied!"
      cancel-event: true
```

## Message Priority System

| Priority | Message Source | Description |
|----------|----------------|-------------|
| **1 (Highest)** | Individual condition deny | Specific message for the failing condition |
| **2 (Medium)** | Global deny | Fallback message in multiple_condition |
| **3 (Lowest)** | System default | Built-in plugin message |

### Example of Message Priority

```yaml
multiple_condition:
  tool_check:
    item:
      material: "DIAMOND_PICKAXE"
    deny:
      message: "&cDiamond pickaxe needed!"  # Priority 1 - shown if this condition fails
  
  level_check:
    placeholder:
      placeholder: "%player_level%"
      operator: "greater_than"
      value: "25"
    # No individual deny message
  
  # Global deny message
  deny:
    message: "&cGeneral requirements not met!"  # Priority 2 - shown if level_check fails
```

## Configuration Options

| Option | Type | Level | Description |
|--------|------|-------|-------------|
| `logic` | String | Global | "AND" or "OR" for condition evaluation |
| `accept` | Object | Individual/Global | Actions when condition succeeds |
| `deny` | Object | Individual/Global | Actions when condition fails |
| `message` | String/Object | Accept/Deny | Message to display |
| `sound` | String | Accept/Deny | Sound effect to play |
| `commands` | List | Accept/Deny | Console commands to execute |
| `cancel-event` | Boolean | Accept/Deny | Whether to cancel the event |

## Examples

### VIP Diamond Mining System

```yaml
mine_diamond_vip:
  target: "DIAMOND_ORE"
  xp: 50.0
  money: 200.0
  requirements:
    logic: "AND"
    multiple_condition:
      permission_tier:
        permission:
          permission: "jobs.mining.diamond"
        deny:
          message: "&cDiamond mining permission required!"
          sound: "ENTITY_VILLAGER_NO"
      
      player_qualification:
        logic: "OR"
        vip_method:
          permission:
            permission: "jobs.vip"
          accept:
            message: "&6VIP diamond access!"
            commands:
              - "effect give {player} luck 300 1"
        
        experience_method:
          logic: "AND"
          placeholder:
            placeholder: "%UniverseJobs_level_miner%"
            operator: "greater_equal"
            value: "75"
          item:
            material: "NETHERITE_PICKAXE"
          accept:
            message: "&aExpert miner diamond access!"
        
        deny:
          message: "&cEither VIP or level 75+ with netherite pickaxe required!"
      
      location_requirement:
        world:
          worlds: ["mining_world"]
        deny:
          message: "&cDiamond mining only allowed in mining world!"
          commands:
            - "spawn {player}"
      
      # Global messages
      accept:
        message: "&a💎 Diamond mining unlocked! Happy mining!"
        sound: "BLOCK_NOTE_BLOCK_CHIME"
        commands:
          - "broadcast &6{player} &ais now mining diamonds!"
      deny:
        message: "&c❌ Diamond mining requirements not met!"
        cancel-event: true
```

### Event-Based Fishing Tournament

```yaml
tournament_fishing:
  target: "SALMON"
  xp: 25.0
  money: 150.0
  requirements:
    logic: "AND"
    multiple_condition:
      event_active:
        placeholder:
          placeholder: "%server_event%"
          operator: "equals"
          value: "fishing_tournament"
        deny:
          message: "&cFishing tournament is not active!"
          sound: "ENTITY_VILLAGER_NO"
      
      registration_check:
        placeholder:
          placeholder: "%tournament_registered_{player}%"
          operator: "equals"
          value: "true"
        deny:
          message: "&cYou must register for the tournament first!"
          commands:
            - "tellraw {player} \"Use /tournament register to join!\""
      
      time_window:
        time:
          start: "12000"  # Tournament hours
          end: "16000"
        deny:
          message: "&cTournament fishing only allowed 12:00-16:00!"
      
      location_requirement:
        logic: "OR"
        ocean_fishing:
          biome:
            biomes: ["OCEAN", "DEEP_OCEAN"]
        river_fishing:
          biome:
            biomes: ["RIVER"]
        tournament_area:
          region:
            region: "tournament_zone"
        deny:
          message: "&cTournament fishing only in water biomes or tournament zone!"
      
      # Global tournament messages
      accept:
        message: "&b🐟 Tournament fishing bonus! Good luck!"
        sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
        commands:
          - "tournament addpoints {player} 1"
          - "actionbar {player} \"&bTournament Points: %tournament_points_{player}%\""
      deny:
        message: "&c🚫 Cannot participate in tournament fishing!"
```

### Multi-Tier Job Access

```yaml
exclusive_job_action:
  target: "ANCIENT_DEBRIS"
  xp: 1000.0
  money: 5000.0
  requirements:
    logic: "AND"
    multiple_condition:
      basic_qualification:
        permission:
          permission: "jobs.mining.advanced"
        deny:
          message: "&cAdvanced mining permission required!"
      
      tier_access:
        logic: "OR"
        platinum_vip:
          permission:
            permission: "vip.platinum"
          accept:
            message: "&bPlatinum VIP: Ancient debris access!"
            commands:
              - "effect give {player} fire_resistance 600 1"
              - "effect give {player} haste 600 2"
        
        master_miner:
          logic: "AND"
          placeholder:
            placeholder: "%UniverseJobs_level_miner%"
            operator: "greater_equal"
            value: "100"
          placeholder2:
            placeholder: "%UniverseJobs_prestige_miner%"
            operator: "greater_equal"
            value: "3"
          accept:
            message: "&6Master Miner: Ancient debris access!"
            commands:
              - "effect give {player} fire_resistance 600 1"
        
        legendary_tool:
          item:
            material: "NETHERITE_PICKAXE"
            enchantments:
              - "efficiency:5"
              - "unbreaking:3"
              - "fortune:3"
          accept:
            message: "&5Legendary Tool: Ancient debris access!"
        
        deny:
          message: "&cRequires: Platinum VIP, Master Miner (Level 100, Prestige 3), or Legendary Tool!"
      
      nether_location:
        world:
          worlds: ["world_nether"]
        deny:
          message: "&cAncient debris can only be mined in the Nether!"
          commands:
            - "tellraw {player} \"Use /nether to travel to the Nether!\""
      
      # Elite mining messages
      accept:
        message: "&c🔥 ELITE: Ancient debris mining unlocked!"
        sound: "ENTITY_ENDER_DRAGON_GROWL"
        commands:
          - "broadcast &c&l[ELITE] &6{player} &cis now mining ancient debris!"
          - "title {player} title \"&c&lELITE MINER\""
          - "title {player} subtitle \"&6Ancient Debris Access Granted\""
      deny:
        message: "&c🚫 ELITE MINING ACCESS DENIED!"
        cancel-event: true
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
multiple_condition:
  crop_stage:
    placeholder:
      placeholder: "%customcrops_stage%"
      operator: "equals"
      value: "4"
    deny:
      message: "&cCrop must be fully grown!"
  
  special_tool:
    item:
      material: "customcrops:harvester"
    accept:
      message: "&aSpecial harvester bonus!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
multiple_condition:
  nexo_block:
    placeholder:
      placeholder: "%nexo_block_type%"
      operator: "equals"
      value: "special_ore"
    deny:
      message: "&cThis is not a Nexo special ore!"
  
  nexo_tool:
    item:
      material: "nexo:mining_drill"
    accept:
      message: "&6Nexo mining drill bonus!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
multiple_condition:
  ia_material:
    placeholder:
      placeholder: "%itemsadder_material%"
      operator: "equals"
      value: "rare_crystal"
    deny:
      message: "&cThis is not a rare crystal!"
  
  ia_permission:
    permission:
      permission: "itemsadder.vip"
    accept:
      message: "&bItemsAdder VIP bonus!"
```
{% endtab %}
{% endtabs %}