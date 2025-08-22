# PLACEHOLDER

Checks PlaceholderAPI placeholder values using various comparison operators.

## Description

Placeholder requirements use PlaceholderAPI to check dynamic values such as player stats, economy balance, plugin data, and more. This powerful system supports string, numeric, and pattern matching comparisons with extensive operator support.

## Basic Configuration

```yaml
requirements:
  placeholder:
    placeholder: "%player_level%"
    operator: "greater_than"
    value: "10"
    deny:
      message: "&cYou must be level 10 or higher!"
      sound: "ENTITY_VILLAGER_NO"
```

## Supported Operators

### String Operators

| Operator | Aliases | Description | Example |
|----------|---------|-------------|---------|
| `equals` | `=` | Exact string match | `%player_name% = "Steve"` |
| `not_equals` | `!=` | Not equal to | `%player_gamemode% != "creative"` |
| `contains` | - | Contains substring | `%player_world% contains "nether"` |
| `not_contains` | - | Does not contain | `%player_location% not_contains "spawn"` |
| `starts_with` | - | Starts with prefix | `%player_name% starts_with "Admin_"` |
| `ends_with` | - | Ends with suffix | `%player_world% ends_with "_end"` |
| `regex` | - | Regular expression | `%player_name% regex "^[A-Z][a-z]+$"` |

### Numeric Operators

| Operator | Aliases | Description | Example |
|----------|---------|-------------|---------|
| `greater_than` | `>` | Greater than | `%vault_eco_balance% > "1000"` |
| `less_than` | `<` | Less than | `%player_health% < "10"` |
| `greater_equal` | `>=` | Greater or equal | `%UniverseJobs_level_miner% >= "25"` |
| `less_equal` | `<=` | Less or equal | `%player_food_level% <= "5"` |

## Advanced Configuration

### Multiple Placeholder Checks

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    economy_check:
      placeholder:
        placeholder: "%vault_eco_balance%"
        operator: "greater_equal"
        value: "5000"
      deny:
        message: "&cYou need at least $5000!"
        sound: "ENTITY_VILLAGER_NO"
    
    job_level_check:
      placeholder:
        placeholder: "%UniverseJobs_level_miner%"
        operator: "greater_equal"
        value: "50"
      deny:
        message: "&cYou need level 50+ in mining!"
    
    prestige_check:
      placeholder:
        placeholder: "%UniverseJobs_prestige_miner%"
        operator: "greater_than"
        value: "0"
      accept:
        message: "&6Prestige miner bonus!"
        commands:
          - "effect give {player} haste 300 1"
      deny:
        message: "&cPrestige required for this action!"
    
    # Global messages
    accept:
      message: "&a✅ All economic requirements met!"
      commands:
        - "eco take {player} 1000"  # Fee for premium action
    deny:
      message: "&c❌ Economic requirements not satisfied!"
```

### String Pattern Matching

```yaml
requirements:
  logic: "OR"
  multiple_condition:
    admin_access:
      placeholder:
        placeholder: "%player_name%"
        operator: "starts_with"
        value: "Admin_"
      accept:
        message: "&cAdmin access granted!"
        sound: "ENTITY_PLAYER_LEVELUP"
    
    vip_access:
      placeholder:
        placeholder: "%player_displayname%"
        operator: "contains"
        value: "[VIP]"
      accept:
        message: "&6VIP access granted!"
    
    special_name:
      placeholder:
        placeholder: "%player_name%"
        operator: "regex"
        value: "^(Steve|Alex|Notch)$"
      accept:
        message: "&bSpecial player access!"
    
    deny:
      message: "&cSpecial access required!"
```

## Configuration Examples

### Economy-Based Restrictions

```yaml
expensive_mining:
  target: "EMERALD_ORE"
  xp: 100.0
  money: 500.0
  requirements:
    logic: "AND"
    multiple_condition:
      balance_requirement:
        placeholder:
          placeholder: "%vault_eco_balance%"
          operator: "greater_equal"
          value: "10000"
        deny:
          message: "&cYou need at least $10,000 to mine emeralds!"
          sound: "ENTITY_VILLAGER_NO"
      
      transaction_fee:
        placeholder:
          placeholder: "%vault_eco_balance%"
          operator: "greater_equal"
          value: "1000"
        accept:
          message: "&aMining fee of $1000 charged!"
          commands:
            - "eco take {player} 1000"
        deny:
          message: "&cInsufficient funds for mining fee!"
      
      # Global configuration
      accept:
        message: "&a💰 Premium emerald mining unlocked!"
        sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
```

### Job Level Progression

```yaml
master_crafting:
  target: "NETHERITE_INGOT"
  xp: 1000.0
  requirements:
    logic: "AND"
    multiple_condition:
      blacksmith_level:
        placeholder:
          placeholder: "%UniverseJobs_level_blacksmith%"
          operator: "greater_equal"
          value: "75"
        deny:
          message: "&cLevel 75+ blacksmith required!"
      
      total_experience:
        placeholder:
          placeholder: "%UniverseJobs_total_exp%"
          operator: "greater_than"
          value: "1000000"
        deny:
          message: "&cYou need over 1M total job experience!"
      
      prestige_status:
        placeholder:
          placeholder: "%UniverseJobs_prestige_blacksmith%"
          operator: "greater_than"
          value: "2"
        accept:
          message: "&5Master Blacksmith bonus activated!"
          commands:
            - "effect give {player} strength 600 2"
            - "broadcast &5{player} &6is crafting as a Master Blacksmith!"
        deny:
          message: "&cPrestige 3+ required for master crafting!"
      
      accept:
        message: "&6🔥 MASTER CRAFTING UNLOCKED!"
        sound: "BLOCK_ANVIL_USE"
```

### Time and Date Restrictions

```yaml
weekend_bonus:
  target: "DIAMOND"
  xp: 50.0
  requirements:
    logic: "OR"
    multiple_condition:
      saturday_check:
        placeholder:
          placeholder: "%server_time_day_of_week%"
          operator: "equals"
          value: "SATURDAY"
        accept:
          message: "&6Saturday weekend bonus!"
      
      sunday_check:
        placeholder:
          placeholder: "%server_time_day_of_week%"
          operator: "equals"
          value: "SUNDAY"
        accept:
          message: "&6Sunday weekend bonus!"
      
      # Special events
      holiday_check:
        placeholder:
          placeholder: "%server_event_active%"
          operator: "equals"
          value: "holiday_special"
        accept:
          message: "&cHoliday special bonus!"
          commands:
            - "eco give {player} 200"
      
      accept:
        message: "&a🎉 Weekend/Holiday bonus activated!"
        commands:
          - "effect give {player} luck 300 2"
      deny:
        message: "&cWeekend/holiday bonus only available on weekends or during special events!"
```

### Player Stats and Health

```yaml
dangerous_mining:
  target: "OBSIDIAN"
  xp: 25.0
  requirements:
    logic: "AND"
    multiple_condition:
      health_requirement:
        placeholder:
          placeholder: "%player_health%"
          operator: "greater_than"
          value: "15"
        deny:
          message: "&cYou need more than 15 health for dangerous mining!"
          sound: "ENTITY_PLAYER_HURT"
      
      hunger_requirement:
        placeholder:
          placeholder: "%player_food_level%"
          operator: "greater_equal"
          value: "10"
        deny:
          message: "&cYou're too hungry for demanding work!"
          commands:
            - "give {player} bread 5"
            - "tellraw {player} \"Here's some bread to restore your energy!\""
      
      experience_requirement:
        placeholder:
          placeholder: "%player_total_experience%"
          operator: "greater_than"
          value: "100"
        deny:
          message: "&cYou need more experience before attempting this!"
      
      accept:
        message: "&a⚡ Ready for dangerous mining!"
        commands:
          - "effect give {player} resistance 300 1"
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  placeholder:
    placeholder: "%customcrops_crop_stage%"
    operator: "equals"
    value: "4"
    accept:
      message: "&aCrop is fully grown!"
    deny:
      message: "&cCrop must be stage 4!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  placeholder:
    placeholder: "%nexo_durability%"
    operator: "greater_than"
    value: "50"
    accept:
      message: "&6Nexo tool in good condition!"
    deny:
      message: "&cNexo tool durability too low!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  placeholder:
    placeholder: "%itemsadder_fuel_level%"
    operator: "greater_equal"
    value: "25"
    accept:
      message: "&bSufficient ItemsAdder fuel!"
    deny:
      message: "&cNeed more ItemsAdder fuel!"
```
{% endtab %}
{% endtabs %}