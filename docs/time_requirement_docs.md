# TIME

Restricts actions to specific in-game time periods (day/night cycles).

## Description

Time requirements check the current in-game time to allow or restrict job actions during specific periods. Perfect for creating day/night specific jobs, seasonal events, or time-based bonuses. Uses Minecraft's 24000-tick time system.

## Basic Configuration

```yaml
requirements:
  time:
    min: 6000   # Day starts (6:00 AM)
    max: 18000  # Day ends (6:00 PM)
    deny:
      message: "&cThis action is only available during daytime!"
      sound: "ENTITY_VILLAGER_NO"
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `min` | Long | 0 | Minimum time (in ticks, 0-23999) |
| `max` | Long | 24000 | Maximum time (in ticks, 0-23999) |

## Minecraft Time System

| Time Period | Ticks | Description |
|-------------|-------|-------------|
| **Dawn** | 0-1000 | Early morning |
| **Day** | 1000-12000 | Full daylight |
| **Dusk** | 12000-13000 | Evening twilight |
| **Night** | 13000-23000 | Full darkness |
| **Late Night** | 23000-24000 | Pre-dawn |

### Common Time Ranges

```yaml
# Day time (6 AM to 6 PM)
day_time:
  min: 6000
  max: 18000

# Night time (6 PM to 6 AM) - wrapping range
night_time:
  min: 18000
  max: 6000

# Full day (dawn to dusk)
full_daylight:
  min: 1000
  max: 12000

# Full night (dusk to dawn)
full_darkness:
  min: 13000
  max: 23000

# Morning hours (6 AM to 12 PM)
morning:
  min: 6000
  max: 12000

# Afternoon (12 PM to 6 PM)
afternoon:
  min: 12000
  max: 18000
```

## Advanced Configuration

### Day/Night Specific Jobs

```yaml
day_farming:
  target: "WHEAT"
  xp: 5.0
  requirements:
    time:
      min: 6000   # 6 AM
      max: 18000  # 6 PM
    accept:
      message: "&aOptimal farming time!"
      commands:
        - "effect give {player} saturation 300 1"
    deny:
      message: "&cFarming is more effective during daylight hours!"

night_fishing:
  target: "SALMON"
  xp: 15.0
  requirements:
    time:
      min: 18000  # 6 PM
      max: 6000   # 6 AM (wrapping)
    accept:
      message: "&9Night fishing bonus!"
      commands:
        - "effect give {player} luck 300 2"
    deny:
      message: "&cNight fishing bonus only available after 6 PM!"
```

### Multi-Period Bonuses

```yaml
mining_schedule:
  target: "IRON_ORE"
  xp: 10.0
  requirements:
    logic: "OR"
    multiple_condition:
      morning_shift:
        time:
          min: 6000   # 6 AM
          max: 12000  # 12 PM
        accept:
          message: "&eMorning shift bonus!"
          commands:
            - "eco give {player} 50"
      
      evening_shift:
        time:
          min: 18000  # 6 PM
          max: 24000  # 12 AM
        accept:
          message: "&5Evening shift bonus!"
          commands:
            - "eco give {player} 75"
      
      night_shift:
        time:
          min: 0      # 12 AM
          max: 6000   # 6 AM
        accept:
          message: "&8Night shift premium!"
          commands:
            - "eco give {player} 100"
            - "effect give {player} night_vision 600 1"
      
      deny:
        message: "&cNo shift bonus during midday (12 PM - 6 PM)"
```

## Seasonal and Event Timing

### Special Event Hours

```yaml
happy_hour_mining:
  target: "DIAMOND_ORE"
  xp: 100.0
  money: 200.0
  requirements:
    logic: "AND"
    multiple_condition:
      happy_hour:
        logic: "OR"
        morning_rush:
          time:
            min: 8000   # 8 AM
            max: 10000  # 10 AM
        evening_rush:
          time:
            min: 20000  # 8 PM
            max: 22000  # 10 PM
        accept:
          message: "&6HAPPY HOUR: Double XP time!"
          commands:
            - "effect give {player} haste 600 2"
        deny:
          message: "&cHappy hour: 8-10 AM or 8-10 PM only!"
      
      weekend_check:
        placeholder:
          placeholder: "%server_day_of_week%"
          operator: "contains"
          value: "SATURDAY|SUNDAY"
        accept:
          message: "&aWeekend bonus active!"
        deny:
          message: "&cHappy hour only on weekends!"
      
      accept:
        message: "&a🎉 WEEKEND HAPPY HOUR ACTIVATED!"
        commands:
          - "broadcast &6{player} &ais mining during weekend happy hour!"
```

### Vampire/Nocturnal Jobs

```yaml
vampire_mining:
  target: "REDSTONE_ORE"
  xp: 25.0
  requirements:
    logic: "AND"
    multiple_condition:
      deep_night:
        time:
          min: 18000  # 6 PM
          max: 6000   # 6 AM
        deny:
          message: "&cVampire activities only during night!"
      
      underground:
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "40"
        deny:
          message: "&cVampire mining requires deep underground!"
      
      no_sunlight:
        placeholder:
          placeholder: "%player_light_level%"
          operator: "less_than"
          value: "8"
        deny:
          message: "&cToo much light for vampire activities!"
      
      accept:
        message: "&4🧛 Vampire mining bonus!"
        commands:
          - "effect give {player} night_vision 600 1"
          - "effect give {player} strength 600 1"
          - "effect give {player} speed 600 1"
```

## Time-Based Restrictions

### Solar-Powered Activities

```yaml
solar_crafting:
  target: "GLASS"
  xp: 8.0
  requirements:
    logic: "AND"
    multiple_condition:
      daylight:
        time:
          min: 6000   # 6 AM
          max: 18000  # 6 PM
        deny:
          message: "&cSolar crafting requires daylight!"
      
      clear_sky:
        weather:
          weather: "CLEAR"
        deny:
          message: "&cSolar crafting needs clear weather!"
      
      outdoor:
        placeholder:
          placeholder: "%player_light_level%"
          operator: "greater_than"
          value: "12"
        deny:
          message: "&cSolar crafting must be done outdoors!"
      
      accept:
        message: "&e☀️ Solar power activated!"
        commands:
          - "effect give {player} regeneration 300 1"
```

### Timed Challenges

```yaml
speed_mining_challenge:
  target: "COAL_ORE"
  xp: 5.0
  requirements:
    logic: "AND"
    multiple_condition:
      challenge_window:
        time:
          min: 12000  # 12 PM
          max: 12100  # ~5 minutes window
        accept:
          message: "&6SPEED CHALLENGE: Active!"
          commands:
            - "effect give {player} haste 300 3"
            - "title {player} title \"&6SPEED MINING\""
            - "title {player} subtitle \"&eYou have 5 minutes!\""
        deny:
          message: "&cSpeed challenge: 12:00-12:05 PM only!"
      
      participation:
        placeholder:
          placeholder: "%challenge_participant_{player}%"
          operator: "equals"
          value: "true"
        deny:
          message: "&cRegister for speed challenge first!"
          commands:
            - "tellraw {player} \"Use /challenge register to join!\""
      
      accept:
        message: "&a⚡ SPEED MINING CHALLENGE!"
        commands:
          - "challenge addpoints {player} 1"
```

## Practical Examples

### Restaurant/Food Jobs

```yaml
breakfast_cooking:
  target: "BREAD"
  xp: 3.0
  requirements:
    time:
      min: 6000   # 6 AM
      max: 10000  # 10 AM
    accept:
      message: "&eBreakfast service!"
      commands:
        - "eco give {player} 25"
    deny:
      message: "&cBreakfast service: 6-10 AM only!"

lunch_cooking:
  target: "COOKED_BEEF"
  xp: 5.0
  requirements:
    time:
      min: 11000  # 11 AM
      max: 14000  # 2 PM
    accept:
      message: "&6Lunch service!"
      commands:
        - "eco give {player} 40"
    deny:
      message: "&cLunch service: 11 AM - 2 PM only!"

dinner_cooking:
  target: "COOKED_SALMON"
  xp: 8.0
  requirements:
    time:
      min: 17000  # 5 PM
      max: 21000  # 9 PM
    accept:
      message: "&5Dinner service!"
      commands:
        - "eco give {player} 60"
    deny:
      message: "&cDinner service: 5-9 PM only!"
```

### Security/Guard Jobs

```yaml
night_watch:
  target: "MONSTER_KILL"
  xp: 20.0
  requirements:
    logic: "AND"
    multiple_condition:
      night_shift:
        time:
          min: 18000  # 6 PM
          max: 6000   # 6 AM
        deny:
          message: "&cNight watch duty: 6 PM - 6 AM only!"
      
      patrol_area:
        region:
          regions:
            - "city_walls"
            - "guard_tower"
        deny:
          message: "&cPatrol duty requires staying in guard areas!"
      
      accept:
        message: "&8🛡️ Night watch duty!"
        commands:
          - "effect give {player} night_vision 600 1"
          - "effect give {player} speed 600 1"
          - "eco give {player} 100"
```

### Market/Trading Hours

```yaml
market_trading:
  target: "EMERALD"
  xp: 15.0
  requirements:
    logic: "AND"
    multiple_condition:
      market_hours:
        time:
          min: 8000   # 8 AM
          max: 20000  # 8 PM
        deny:
          message: "&cMarket closed! Trading hours: 8 AM - 8 PM"
      
      market_region:
        region:
          regions:
            - "market_district"
            - "trading_post"
        deny:
          message: "&cTrading only allowed in market areas!"
      
      trader_license:
        permission:
          permission: "jobs.trader"
        deny:
          message: "&cTrading license required!"
      
      accept:
        message: "&2📈 Market trading active!"
        commands:
          - "effect give {player} luck 600 1"
```

## Time Calculations

### Real-World Time Conversion

```yaml
# Minecraft day = 20 minutes real time
# 1 Minecraft tick = 0.05 seconds real time
# Time conversions:

real_time_events:
  # Every 2 hours real time (6 MC days)
  bi_hourly:
    min: 0
    max: 2000
  
  # Every hour real time (3 MC days)  
  hourly:
    min: 0
    max: 1000
  
  # Every 30 minutes real time (1.5 MC days)
  half_hourly:
    min: 0
    max: 500
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  time:
    min: 6000   # Day time
    max: 18000
    accept:
      message: "&aCustomCrops growing time!"
    deny:
      message: "&cCustomCrops grow better during daylight!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  time:
    min: 18000  # Night time
    max: 6000
    accept:
      message: "&6Nexo blocks glow at night!"
    deny:
      message: "&cNexo special effects only at night!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  time:
    min: 12000  # Noon
    max: 14000
    accept:
      message: "&bItemsAdder solar charging!"
    deny:
      message: "&cItemsAdder solar items need midday sun!"
```
{% endtab %}
{% endtabs %}