# WEATHER

Restricts actions based on current weather conditions in the player's world.

## Description

Weather requirements check the current weather in the player's world to allow or restrict job actions. Perfect for creating weather-dependent activities like rain fishing bonuses, clear weather farming, or thunderstorm special events.

## Basic Configuration

```yaml
requirements:
  weather:
    weather: "RAIN"
    accept:
      message: "&3Rain bonus activated!"
      sound: "WEATHER_RAIN"
    deny:
      message: "&cThis action requires rainy weather!"
      sound: "ENTITY_VILLAGER_NO"
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `weather` | String | "CLEAR" | Required weather type |

## Weather Types

| Weather | Description | Conditions |
|---------|-------------|------------|
| **CLEAR** | Clear skies | No rain or storm |
| **RAIN** | Rainy weather | Rain but no thunder |
| **THUNDER** | Thunderstorm | Rain with thunder/lightning |

## Advanced Configuration

### Weather-Specific Bonuses

```yaml
rain_fishing:
  target: "COD"
  xp: 15.0
  requirements:
    logic: "AND"
    multiple_condition:
      rainy_weather:
        weather:
          weather: "RAIN"
        accept:
          message: "&3Rain fishing bonus!"
          commands:
            - "effect give {player} luck 300 2"
        deny:
          message: "&cRain fishing bonus only during rain!"
      
      outdoor_fishing:
        placeholder:
          placeholder: "%player_light_level%"
          operator: "greater_than"
          value: "7"
        deny:
          message: "&cRain bonus requires outdoor fishing!"
      
      accept:
        message: "&b🌧️ Perfect rainy fishing conditions!"
        commands:
          - "eco give {player} 50"

thunder_storm_mining:
  target: "REDSTONE_ORE"
  xp: 50.0
  requirements:
    weather:
      weather: "THUNDER"
    accept:
      message: "&4⚡ Thunderstorm energy boost!"
      commands:
        - "effect give {player} haste 600 3"
        - "effect give {player} strength 300 1"
    deny:
      message: "&cThunderstorm power required for enhanced redstone mining!"
```

### Multi-Weather Activities

```yaml
farming_weather:
  target: "WHEAT"
  xp: 5.0
  requirements:
    logic: "OR"
    multiple_condition:
      clear_farming:
        weather:
          weather: "CLEAR"
        accept:
          message: "&eClear weather farming!"
          commands:
            - "eco give {player} 25"
      
      rain_farming:
        weather:
          weather: "RAIN"
        accept:
          message: "&3Rain-blessed farming!"
          commands:
            - "eco give {player} 50"
            - "effect give {player} saturation 300 1"
      
      deny:
        message: "&cNo farming during thunderstorms!"
        cancel-event: true
```

## Weather-Based Job Examples

### Storm Chasing

```yaml
lightning_rod_crafting:
  target: "LIGHTNING_ROD"
  xp: 100.0
  money: 200.0
  requirements:
    logic: "AND"
    multiple_condition:
      thunderstorm:
        weather:
          weather: "THUNDER"
        deny:
          message: "&cLightning rods can only be crafted during thunderstorms!"
      
      high_altitude:
        placeholder:
          placeholder: "%player_y%"
          operator: "greater_than"
          value: "100"
        deny:
          message: "&cCraft lightning rods at high altitude for better conductivity!"
      
      copper_materials:
        item:
          material: "COPPER_INGOT"
        deny:
          message: "&cYou need copper ingots in hand!"
      
      accept:
        message: "&4⚡ STORM CRAFTING: Lightning rod forged!"
        commands:
          - "effect give {player} fire_resistance 600 1"
          - "broadcast &4{player} &6is crafting lightning rods in the storm!"
```

### Seasonal Weather Jobs

```yaml
snow_collection:
  target: "SNOW_BLOCK"
  xp: 3.0
  requirements:
    logic: "AND"
    multiple_condition:
      snowy_weather:
        weather:
          weather: "RAIN"  # In cold biomes, rain becomes snow
        biome:
          biomes: 
            - "SNOWY_TUNDRA"
            - "SNOWY_MOUNTAINS"
            - "ICE_SPIKES"
        accept:
          message: "&fSnowy weather collection!"
          commands:
            - "effect give {player} slowness 0 0"  # Remove slowness
        deny:
          message: "&cSnow collection requires snowy weather in cold biomes!"
      
      winter_gear:
        item:
          material: 
            - "LEATHER_BOOTS"
            - "IRON_BOOTS"
            - "DIAMOND_BOOTS"
        deny:
          message: "&cWinter boots required for snow collection!"
      
      accept:
        message: "&f❄️ Winter collection bonus!"
        commands:
          - "eco give {player} 15"
```

### Atmospheric Crafting

```yaml
storm_brewing:
  target: "SPLASH_POTION"
  xp: 25.0
  requirements:
    logic: "AND"
    multiple_condition:
      storm_power:
        weather:
          weather: "THUNDER"
        accept:
          message: "&5Storm-powered brewing!"
          commands:
            - "effect give {player} strength 300 1"
        deny:
          message: "&cPowerful potions require thunderstorm energy!"
      
      brewing_stand:
        placeholder:
          placeholder: "%player_location_block%"
          operator: "contains"
          value: "brewing_stand"
        deny:
          message: "&cYou must be at a brewing stand!"
      
      outdoor_brewing:
        placeholder:
          placeholder: "%player_light_level%"
          operator: "less_than"
          value: "15"
        accept:
          message: "&8Storm clouds enhance brewing!"
        deny:
          message: "&cStorm brewing must be done under open sky!"
      
      accept:
        message: "&4⚗️ STORM BREWING: Potent potions created!"
        commands:
          - "eco give {player} 75"
          - "give {player} experience_bottle 3"
```

## Weather-Dependent Professions

### Weather Wizard

```yaml
weather_magic:
  target: "NETHER_STAR"
  xp: 500.0
  requirements:
    logic: "OR"
    multiple_condition:
      clear_magic:
        weather:
          weather: "CLEAR"
        time:
          min: 6000   # Day time
          max: 18000
        accept:
          message: "&eSolar magic amplification!"
          commands:
            - "effect give {player} regeneration 600 2"
      
      storm_magic:
        weather:
          weather: "THUNDER"
        accept:
          message: "&4Storm magic amplification!"
          commands:
            - "effect give {player} strength 600 2"
            - "effect give {player} speed 600 1"
      
      rain_magic:
        weather:
          weather: "RAIN"
        accept:
          message: "&3Hydro magic amplification!"
          commands:
            - "effect give {player} water_breathing 600 1"
            - "effect give {player} conduit_power 600 1"
      
      deny:
        message: "&cWeather magic requires specific atmospheric conditions!"
```

### Storm Photographer

```yaml
lightning_photography:
  target: "ITEM_FRAME"  # Representing photo capture
  xp: 75.0
  requirements:
    logic: "AND"
    multiple_condition:
      thunderstorm:
        weather:
          weather: "THUNDER"
        deny:
          message: "&cLightning photography requires active thunderstorm!"
      
      camera_equipment:
        item:
          material: "SPYGLASS"
        deny:
          message: "&cYou need a spyglass as camera equipment!"
      
      high_vantage:
        placeholder:
          placeholder: "%player_y%"
          operator: "greater_than"
          value: "80"
        deny:
          message: "&cFind higher ground for better lightning shots!"
      
      timing:
        time:
          min: 12000  # After noon
          max: 6000   # Before dawn
        deny:
          message: "&cLightning photography best during dark hours!"
      
      accept:
        message: "&4📸 LIGHTNING SHOT CAPTURED!"
        commands:
          - "broadcast &6{player} &ecaptured an amazing lightning photograph!"
          - "eco give {player} 500"
          - "give {player} painting 1"
```

## Weather Integration

### Climate-Based Farming

```yaml
climate_agriculture:
  target: "CARROT"
  xp: 4.0
  requirements:
    logic: "AND"
    multiple_condition:
      optimal_weather:
        logic: "OR"
        rainy_season:
          weather:
            weather: "RAIN"
          accept:
            message: "&3Rain-fed agriculture!"
            commands:
              - "eco give {player} 40"
        
        irrigation_system:
          weather:
            weather: "CLEAR"
          placeholder:
            placeholder: "%player_water_nearby%"
            operator: "equals"
            value: "true"
          accept:
            message: "&aIrrigated farming!"
            commands:
              - "eco give {player} 25"
        
        deny:
          message: "&cCrops need water: rain or irrigation!"
      
      growing_season:
        biome:
          biomes:
            - "PLAINS"
            - "FOREST"
            - "RIVER"
        deny:
          message: "&cThis crop doesn't grow well in this biome!"
      
      accept:
        message: "&2🌱 Optimal growing conditions!"
```

### Weather Prediction Jobs

```yaml
meteorology:
  target: "BAROMETER"  # Custom item
  xp: 30.0
  requirements:
    logic: "AND"
    multiple_condition:
      weather_station:
        region:
          regions:
            - "weather_tower"
            - "observatory"
        deny:
          message: "&cWeather monitoring requires weather station access!"
      
      equipment:
        item:
          material: "COMPASS"
        deny:
          message: "&cYou need meteorological equipment (compass)!"
      
      weather_change:
        logic: "OR"
        storm_approaching:
          weather:
            weather: "THUNDER"
          accept:
            message: "&4Storm detected: High priority reading!"
            commands:
              - "eco give {player} 100"
        
        rain_detected:
          weather:
            weather: "RAIN"
          accept:
            message: "&3Rain detected: Standard reading!"
            commands:
              - "eco give {player} 50"
        
        clear_monitoring:
          weather:
            weather: "CLEAR"
          accept:
            message: "&eClear skies: Routine monitoring!"
            commands:
              - "eco give {player} 25"
      
      accept:
        message: "&6🌤️ Weather data recorded!"
        commands:
          - "broadcast Weather update by {player}: %world_weather%"
```

## Practical Weather Applications

### Emergency Response

```yaml
storm_rescue:
  target: "PLAYER_RESCUE"  # Custom event
  xp: 200.0
  requirements:
    logic: "AND"
    multiple_condition:
      dangerous_weather:
        weather:
          weather: "THUNDER"
        deny:
          message: "&cStorm rescue only during active thunderstorms!"
      
      rescue_gear:
        item:
          material: "ELYTRA"
        deny:
          message: "&cRescue operations require flight gear (elytra)!"
      
      emergency_permission:
        permission:
          permission: "jobs.emergency.responder"
        deny:
          message: "&cEmergency responder certification required!"
      
      accept:
        message: "&c🚨 STORM RESCUE OPERATION!"
        commands:
          - "effect give {player} fire_resistance 1200 1"
          - "effect give {player} water_breathing 1200 1"
          - "broadcast &c{player} &eis conducting storm rescue operations!"
```

### Agricultural Science

```yaml
weather_research:
  target: "WHEAT_SEEDS"
  xp: 8.0
  requirements:
    logic: "OR"
    multiple_condition:
      drought_study:
        weather:
          weather: "CLEAR"
        time:
          min: 8000   # Extended clear period
          max: 16000
        accept:
          message: "&eDrought resistance research!"
          commands:
            - "eco give {player} 60"
      
      flood_resistance:
        weather:
          weather: "RAIN"
        accept:
          message: "&3Flood tolerance research!"
          commands:
            - "eco give {player} 80"
      
      storm_survival:
        weather:
          weather: "THUNDER"
        accept:
          message: "&4Storm survival research!"
          commands:
            - "eco give {player} 120"
            - "effect give {player} resistance 300 1"
      
      accept:
        message: "&2🔬 Agricultural research data collected!"
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  weather:
    weather: "RAIN"
    accept:
      message: "&aCustomCrops love the rain!"
      commands:
        - "effect give {player} saturation 300 1"
    deny:
      message: "&cCustomCrops grow better in rain!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  weather:
    weather: "THUNDER"
    accept:
      message: "&6Nexo blocks charged by lightning!"
      commands:
        - "effect give {player} strength 300 2"
    deny:
      message: "&cNexo energy requires thunderstorm power!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  weather:
    weather: "CLEAR"
    accept:
      message: "&bItemsAdder solar panels active!"
      commands:
        - "eco give {player} 75"
    deny:
      message: "&cItemsAdder solar technology needs clear skies!"
```
{% endtab %}
{% endtabs %}