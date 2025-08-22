# BIOME

Restricts actions to specific biomes or excludes certain biomes.

## Description

Biome requirements check the current biome where the player is located to allow or restrict job actions. Perfect for creating biome-specific professions, environmental jobs, or location-based activities that make ecological sense.

## Basic Configuration

```yaml
requirements:
  biome:
    biomes: 
      - "FOREST"
      - "DARK_FOREST"
      - "BIRCH_FOREST"
    blacklist: false  # false = whitelist (allow only these), true = blacklist (deny these)
    deny:
      message: "&cThis action requires a forest biome!"
      sound: "ENTITY_VILLAGER_NO"
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `biomes` | List | [] | List of Minecraft biome names |
| `blacklist` | Boolean | false | false = whitelist mode, true = blacklist mode |

## Common Biome Categories

### Forest Biomes
```yaml
forest_biomes:
  biomes:
    - "FOREST"
    - "DARK_FOREST"
    - "BIRCH_FOREST"
    - "TAIGA"
    - "OLD_GROWTH_BIRCH_FOREST"
    - "OLD_GROWTH_PINE_TAIGA"
    - "OLD_GROWTH_SPRUCE_TAIGA"
```

### Ocean Biomes
```yaml
ocean_biomes:
  biomes:
    - "OCEAN"
    - "DEEP_OCEAN"
    - "WARM_OCEAN"
    - "LUKEWARM_OCEAN"
    - "COLD_OCEAN"
    - "FROZEN_OCEAN"
    - "DEEP_LUKEWARM_OCEAN"
    - "DEEP_COLD_OCEAN"
    - "DEEP_FROZEN_OCEAN"
```

### Mountain Biomes
```yaml
mountain_biomes:
  biomes:
    - "MOUNTAINS"
    - "MOUNTAIN_EDGE"
    - "SNOWY_MOUNTAINS"
    - "MODIFIED_GRAVELLY_MOUNTAINS"
    - "SHATTERED_SAVANNA_PLATEAU"
    - "WINDSWEPT_HILLS"
    - "WINDSWEPT_GRAVELLY_HILLS"
    - "WINDSWEPT_FOREST"
```

### Desert Biomes
```yaml
desert_biomes:
  biomes:
    - "DESERT"
    - "DESERT_HILLS"
    - "DESERT_LAKES"
    - "BADLANDS"
    - "BADLANDS_PLATEAU"
    - "MODIFIED_BADLANDS_PLATEAU"
    - "ERODED_BADLANDS"
```

## Advanced Configuration

### Biome-Specific Professions

```yaml
marine_biology:
  target: "TROPICAL_FISH"
  xp: 20.0
  requirements:
    logic: "AND"
    multiple_condition:
      ocean_biome:
        biome:
          biomes:
            - "WARM_OCEAN"
            - "LUKEWARM_OCEAN"
            - "OCEAN"
          blacklist: false
        accept:
          message: "&bMarine research area!"
          commands:
            - "effect give {player} water_breathing 600 1"
            - "effect give {player} conduit_power 300 1"
        deny:
          message: "&cMarine biology requires ocean biomes!"
      
      research_equipment:
        item:
          material: "SPYGLASS"
        deny:
          message: "&cYou need research equipment (spyglass)!"
      
      underwater:
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "62"  # Below sea level
        deny:
          message: "&cMarine research requires underwater exploration!"
      
      accept:
        message: "&3🐠 Marine biology research active!"
        commands:
          - "eco give {player} 100"
          - "broadcast {player} discovered marine life in %player_biome%!"

forestry_job:
  target: "OAK_LOG"
  xp: 8.0
  requirements:
    logic: "AND"
    multiple_condition:
      forest_biome:
        biome:
          biomes:
            - "FOREST"
            - "DARK_FOREST"
            - "BIRCH_FOREST"
            - "TAIGA"
          blacklist: false
        deny:
          message: "&cForestry work requires forest biomes!"
      
      sustainable_tools:
        item:
          material: ["IRON_AXE", "DIAMOND_AXE", "NETHERITE_AXE"]
        deny:
          message: "&cSustainable forestry requires proper tools!"
      
      conservation_permit:
        permission:
          permission: "jobs.forestry.licensed"
        accept:
          message: "&2Licensed forestry operation!"
        deny:
          message: "&cForestry license required for tree harvesting!"
      
      accept:
        message: "&2🌲 Sustainable forestry active!"
        commands:
          - "eco give {player} 50"
          - "give {player} oak_sapling 2"  # Replanting materials
```

### Climate-Based Agriculture

```yaml
tropical_farming:
  target: "MELON"
  xp: 6.0
  requirements:
    logic: "AND"
    multiple_condition:
      tropical_climate:
        biome:
          biomes:
            - "JUNGLE"
            - "JUNGLE_HILLS"
            - "MODIFIED_JUNGLE"
            - "JUNGLE_EDGE"
            - "MODIFIED_JUNGLE_EDGE"
            - "BAMBOO_JUNGLE"
            - "BAMBOO_JUNGLE_HILLS"
          blacklist: false
        accept:
          message: "&aTropical climate perfect for melons!"
          commands:
            - "effect give {player} saturation 300 1"
        deny:
          message: "&cMelons thrive in tropical jungle biomes!"
      
      humidity_check:
        weather:
          weather: "RAIN"
        accept:
          message: "&3High humidity boost!"
          commands:
            - "eco give {player} 25"
        # No deny - rain is optional bonus
      
      farming_tools:
        item:
          material: ["IRON_HOE", "DIAMOND_HOE", "NETHERITE_HOE"]
        deny:
          message: "&cTropical farming requires advanced hoes!"
      
      accept:
        message: "&6🍈 Tropical farming expertise!"
        commands:
          - "eco give {player} 75"

arctic_survival:
  target: "ICE"
  xp: 12.0
  requirements:
    logic: "AND"
    multiple_condition:
      arctic_biome:
        biome:
          biomes:
            - "SNOWY_TUNDRA"
            - "ICE_SPIKES"
            - "SNOWY_MOUNTAINS"
            - "FROZEN_OCEAN"
            - "SNOWY_BEACH"
            - "SNOWY_TAIGA"
          blacklist: false
        deny:
          message: "&cArctic survival requires cold biomes!"
      
      winter_gear:
        item:
          material: ["LEATHER_BOOTS", "IRON_BOOTS", "DIAMOND_BOOTS"]
        deny:
          message: "&cWinter boots required for arctic conditions!"
      
      cold_resistance:
        placeholder:
          placeholder: "%player_temperature%"
          operator: "less_than"
          value: "0.5"  # Cold temperature
        accept:
          message: "&fAdapted to arctic conditions!"
          commands:
            - "effect give {player} resistance 600 1"
        deny:
          message: "&cYou need better cold adaptation!"
      
      accept:
        message: "&f❄️ Arctic survival expertise!"
        commands:
          - "eco give {player} 100"
          - "effect give {player} fire_resistance 300 1"  # Warm from within
```

## Biome Ecology Jobs

### Cave Exploration

```yaml
spelunking:
  target: "STONE"
  xp: 3.0
  requirements:
    logic: "AND"
    multiple_condition:
      underground:
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "50"
        deny:
          message: "&cSpelunking requires underground exploration!"
      
      cave_biome:
        biome:
          biomes:
            - "DRIPSTONE_CAVES"
            - "LUSH_CAVES"
          blacklist: false
        accept:
          message: "&8Special cave biome discovered!"
          commands:
            - "eco give {player} 50"
            - "effect give {player} night_vision 600 1"
        # No deny - works in any underground area
      
      spelunking_gear:
        item:
          material: "TORCH"
        deny:
          message: "&cCave exploration requires lighting (torch)!"
      
      accept:
        message: "&8🕳️ Cave exploration active!"
        commands:
          - "eco give {player} 25"
```

### Desert Archaeology

```yaml
archaeology:
  target: "SUSPICIOUS_SAND"
  xp: 50.0
  requirements:
    logic: "AND"
    multiple_condition:
      desert_biome:
        biome:
          biomes:
            - "DESERT"
            - "DESERT_HILLS"
            - "BADLANDS"
            - "ERODED_BADLANDS"
          blacklist: false
        deny:
          message: "&cArchaeology requires arid desert biomes!"
      
      excavation_tools:
        item:
          material: "BRUSH"
        deny:
          message: "&cArchaeological excavation requires a brush!"
      
      heat_protection:
        time:
          min: 6000   # Day time
          max: 18000
        placeholder:
          placeholder: "%player_light_level%"
          operator: "greater_than"
          value: "12"
        accept:
          message: "&eOptimal excavation lighting!"
        deny:
          message: "&cDesert archaeology requires daylight for visibility!"
      
      research_permit:
        permission:
          permission: "jobs.archaeology.licensed"
        deny:
          message: "&cArchaeological research permit required!"
      
      accept:
        message: "&6🏺 Archaeological expedition active!"
        commands:
          - "effect give {player} fire_resistance 600 1"
          - "eco give {player} 200"
          - "broadcast {player} is conducting archaeological research!"
```

## Environmental Science

### Ecosystem Monitoring

```yaml
biodiversity_study:
  target: "GRASS_BLOCK"
  xp: 4.0
  requirements:
    logic: "OR"
    multiple_condition:
      grassland_study:
        biome:
          biomes:
            - "PLAINS"
            - "SUNFLOWER_PLAINS"
            - "SAVANNA"
            - "SAVANNA_PLATEAU"
          blacklist: false
        accept:
          message: "&2Grassland ecosystem study!"
          commands:
            - "eco give {player} 30"
      
      wetland_study:
        biome:
          biomes:
            - "SWAMP"
            - "SWAMP_HILLS"
            - "RIVER"
          blacklist: false
        accept:
          message: "&6Wetland ecosystem study!"
          commands:
            - "eco give {player} 45"
            - "effect give {player} water_breathing 300 1"
      
      mountain_study:
        biome:
          biomes:
            - "MOUNTAINS"
            - "WINDSWEPT_HILLS"
            - "WINDSWEPT_FOREST"
          blacklist: false
        accept:
          message: "&7Mountain ecosystem study!"
          commands:
            - "eco give {player} 60"
            - "effect give {player} slow_falling 300 1"
      
      accept:
        message: "&a🌿 Ecosystem research data collected!"
        commands:
          - "broadcast {player} completed ecosystem research in %player_biome%"
```

### Climate Research

```yaml
climate_monitoring:
  target: "THERMOMETER"  # Custom item
  xp: 25.0
  requirements:
    logic: "OR"
    multiple_condition:
      hot_climate:
        biome:
          biomes:
            - "DESERT"
            - "BADLANDS"
            - "NETHER_WASTES"
            - "CRIMSON_FOREST"
          blacklist: false
        accept:
          message: "&cHot climate data collection!"
          commands:
            - "eco give {player} 75"
            - "effect give {player} fire_resistance 300 1"
      
      cold_climate:
        biome:
          biomes:
            - "SNOWY_TUNDRA"
            - "ICE_SPIKES"
            - "FROZEN_OCEAN"
          blacklist: false
        accept:
          message: "&fCold climate data collection!"
          commands:
            - "eco give {player} 75"
            - "effect give {player} resistance 300 1"
      
      temperate_climate:
        biome:
          biomes:
            - "PLAINS"
            - "FOREST"
            - "RIVER"
          blacklist: false
        accept:
          message: "&aTemperate climate data collection!"
          commands:
            - "eco give {player} 50"
      
      accept:
        message: "&6🌡️ Climate research completed!"
```

## Biome Restrictions

### Protected Areas

```yaml
conservation_area:
  target: "FLOWER"
  xp: 2.0
  requirements:
    logic: "AND"
    multiple_condition:
      protected_biome:
        biome:
          biomes:
            - "FLOWER_FOREST"
            - "SUNFLOWER_PLAINS"
            - "CHERRY_GROVE"
          blacklist: false
        deny:
          message: "&cFlower collection only in designated flower biomes!"
      
      conservation_permit:
        permission:
          permission: "jobs.conservation.licensed"
        deny:
          message: "&cConservation permit required!"
      
      sustainable_collection:
        placeholder:
          placeholder: "%flowers_collected_today%"
          operator: "less_than"
          value: "50"
        deny:
          message: "&cDaily flower collection limit reached!"
          commands:
            - "tellraw {player} \"Conservation limit: 50 flowers per day\""
      
      accept:
        message: "&d🌸 Sustainable flower collection!"
        commands:
          - "eco give {player} 20"
          - "placeholderapi setvalue flowers_collected_today +1"
```

### Biome-Specific Restrictions

```yaml
environmental_protection:
  target: "DIAMOND_ORE"
  xp: 50.0
  requirements:
    logic: "AND"
    multiple_condition:
      not_protected:
        biome:
          biomes:
            - "CHERRY_GROVE"
            - "LUSH_CAVES"
            - "FLOWER_FOREST"
          blacklist: true  # NOT allowed in these biomes
        deny:
          message: "&cMining prohibited in protected environmental areas!"
          cancel-event: true
      
      mining_zone:
        biome:
          biomes:
            - "MOUNTAINS"
            - "WINDSWEPT_HILLS"
            - "DRIPSTONE_CAVES"
          blacklist: false
        deny:
          message: "&cDiamond mining only allowed in designated mountain/cave areas!"
      
      environmental_permit:
        permission:
          permission: "jobs.mining.environmental"
        deny:
          message: "&cEnvironmental impact permit required!"
      
      accept:
        message: "&b💎 Environmentally responsible mining!"
        commands:
          - "eco give {player} 100"
          - "broadcast {player} is mining responsibly in %player_biome%"
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  biome:
    biomes:
      - "PLAINS"
      - "FOREST"
    blacklist: false
    accept:
      message: "&aCustomCrops thrive in this biome!"
    deny:
      message: "&cCustomCrops need suitable growing biomes!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  biome:
    biomes:
      - "MOUNTAINS"
      - "DRIPSTONE_CAVES"
    blacklist: false
    accept:
      message: "&6Nexo ore veins in mountainous areas!"
    deny:
      message: "&cNexo ores only spawn in specific biomes!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  biome:
    biomes:
      - "NETHER_WASTES"
      - "CRIMSON_FOREST"
    blacklist: false
    accept:
      message: "&cItemsAdder nether materials!"
    deny:
      message: "&cItemsAdder special materials only in the Nether!"
```
{% endtab %}
{% endtabs %}