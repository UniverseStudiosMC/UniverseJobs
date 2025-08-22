# PERMISSION

Checks if a player has specific permissions or lacks them.

## Description

Permission requirements verify that a player has (or doesn't have) specific permissions. This integrates with any permission plugin (LuckPerms, PermissionsEx, etc.) and supports both positive and negative permission checks for flexible access control.

## Basic Configuration

```yaml
requirements:
  permission:
    permission: "jobs.mining"
    require: true  # true = must have, false = must NOT have
    deny:
      message: "&cYou need mining permission!"
      sound: "ENTITY_VILLAGER_NO"
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `permission` | String | "" | The permission node to check |
| `require` | Boolean | true | true = must have permission, false = must NOT have permission |

## Advanced Configuration

### VIP Access Control

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    basic_permission:
      permission:
        permission: "jobs.mining"
        require: true
      deny:
        message: "&cYou need basic mining permission!"
    
    vip_access:
      permission:
        permission: "jobs.vip"
        require: true
      accept:
        message: "&6VIP access granted!"
        commands:
          - "effect give {player} haste 300 1"
          - "effect give {player} luck 300 1"
      deny:
        message: "&cVIP membership required for bonus effects!"
    
    # Global messages
    accept:
      message: "&a✅ Full mining access granted!"
    deny:
      message: "&c❌ Insufficient permissions!"
```

### Negative Permission Checks

```yaml
requirements:
  logic: "AND"
  multiple_condition:
    not_banned:
      permission:
        permission: "jobs.banned"
        require: false  # Must NOT have this permission
      deny:
        message: "&cYou are banned from jobs!"
        cancel-event: true
    
    not_in_creative:
      permission:
        permission: "jobs.creative.bypass"
        require: false
      deny:
        message: "&cCreative mode players cannot earn job XP!"
        cancel-event: true
    
    has_access:
      permission:
        permission: "jobs.mining"
        require: true
      deny:
        message: "&cMining permission required!"
    
    accept:
      message: "&aPermission checks passed!"
```

## Permission Hierarchy Examples

### Tiered Access System

```yaml
diamond_mining:
  target: "DIAMOND_ORE"
  xp: 50.0
  money: 200.0
  requirements:
    logic: "OR"
    multiple_condition:
      admin_access:
        permission:
          permission: "jobs.admin"
          require: true
        accept:
          message: "&cAdmin access: Full mining privileges!"
          commands:
            - "effect give {player} haste 600 3"
            - "effect give {player} luck 600 3"
      
      vip_premium:
        permission:
          permission: "jobs.vip.premium"
          require: true
        accept:
          message: "&6Premium VIP: Enhanced mining!"
          commands:
            - "effect give {player} haste 600 2"
            - "effect give {player} luck 600 2"
      
      vip_basic:
        permission:
          permission: "jobs.vip.basic"
          require: true
        accept:
          message: "&aBasic VIP: Standard mining bonus!"
          commands:
            - "effect give {player} haste 600 1"
      
      regular_access:
        logic: "AND"
        permission:
          permission: "jobs.mining.diamond"
          require: true
        placeholder:
          placeholder: "%UniverseJobs_level_miner%"
          operator: "greater_equal"
          value: "50"
        accept:
          message: "&7Regular access: Level 50+ mining!"
      
      deny:
        message: "&cDiamond mining requires: Admin, VIP, or level 50+ with diamond permission!"
        cancel-event: true
```

### Role-Based Restrictions

```yaml
boss_mining:
  target: "NETHERITE_SCRAP"
  xp: 500.0
  requirements:
    logic: "AND"
    multiple_condition:
      leadership_role:
        logic: "OR"
        guild_leader:
          permission:
            permission: "guild.leader"
            require: true
          accept:
            message: "&6Guild Leader bonus!"
        
        party_leader:
          permission:
            permission: "party.leader"
            require: true
          accept:
            message: "&9Party Leader bonus!"
        
        deny:
          message: "&cOnly guild or party leaders can mine netherite!"
      
      not_restricted:
        permission:
          permission: "jobs.restricted"
          require: false
        deny:
          message: "&cYou are restricted from elite mining!"
      
      elite_access:
        permission:
          permission: "jobs.elite"
          require: true
        deny:
          message: "&cElite mining permission required!"
      
      accept:
        message: "&5ELITE: Leadership mining access granted!"
        commands:
          - "broadcast &5{player} &6is mining elite netherite as a leader!"
          - "effect give {player} fire_resistance 1200 1"
```

## Permission Groups

### Staff Permissions

```yaml
staff_mining:
  target: "BEDROCK"
  xp: 1000.0
  requirements:
    logic: "OR"
    multiple_condition:
      owner_access:
        permission:
          permission: "jobs.owner"
          require: true
        accept:
          message: "&4Owner: Unlimited access!"
      
      admin_access:
        permission:
          permission: "jobs.admin"
          require: true
        accept:
          message: "&cAdmin: Full privileges!"
      
      moderator_access:
        permission:
          permission: "jobs.moderator"
          require: true
        accept:
          message: "&6Moderator: Special access!"
      
      deny:
        message: "&cStaff rank required for this action!"
        cancel-event: true
```

### Event Permissions

```yaml
event_fishing:
  target: "TROPICAL_FISH"
  xp: 25.0
  requirements:
    logic: "AND"
    multiple_condition:
      event_participant:
        permission:
          permission: "events.fishing.participant"
          require: true
        deny:
          message: "&cYou must register for the fishing event!"
          commands:
            - "tellraw {player} \"Use /event register fishing to join!\""
      
      not_event_banned:
        permission:
          permission: "events.banned"
          require: false
        deny:
          message: "&cYou are banned from events!"
          cancel-event: true
      
      # Optional VIP bonus
      vip_bonus:
        permission:
          permission: "events.vip"
          require: true
        accept:
          message: "&6VIP Event Bonus!"
          commands:
            - "eco give {player} 100"
            - "effect give {player} luck 300 2"
      
      accept:
        message: "&b🐟 Event fishing bonus activated!"
        sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
```

### Region-Specific Permissions

```yaml
private_mine_access:
  target: "GOLD_ORE"
  xp: 30.0
  requirements:
    logic: "OR"
    multiple_condition:
      mine_owner:
        permission:
          permission: "privatemines.owner"
          require: true
        accept:
          message: "&6Private mine owner bonus!"
          commands:
            - "effect give {player} haste 600 2"
      
      mine_member:
        permission:
          permission: "privatemines.member"
          require: true
        accept:
          message: "&aMine member access!"
          commands:
            - "effect give {player} haste 300 1"
      
      mine_guest:
        logic: "AND"
        permission:
          permission: "privatemines.guest"
          require: true
        placeholder:
          placeholder: "%vault_eco_balance%"
          operator: "greater_than"
          value: "1000"
        accept:
          message: "&7Guest access (fee applied)!"
          commands:
            - "eco take {player} 100"
      
      deny:
        message: "&cPrivate mine access required!"
        commands:
          - "spawn {player}"
```

## Integration Examples

### LuckPerms Integration

```yaml
# Check for specific rank
rank_bonus:
  permission:
    permission: "group.vip"  # LuckPerms group
    require: true
  accept:
    message: "&6VIP rank bonus!"

# Check for temporary permission
temp_access:
  permission:
    permission: "jobs.temporary.bonus"
    require: true
  accept:
    message: "&eTemporary bonus active!"
```

### Custom Permission Nodes

```yaml
# Custom permission structure
custom_access:
  permission:
    permission: "universejobs.mining.tier3"
    require: true
  accept:
    message: "&aTier 3 mining unlocked!"

# Negative checks for restrictions
anti_grief:
  permission:
    permission: "griefprevention.restricted"
    require: false  # Must NOT have this
  deny:
    message: "&cGrief prevention active!"
```

## Common Permission Patterns

```yaml
# Standard job permission
basic_job:
  permission: "jobs.{jobname}"
  require: true

# VIP access
vip_access:
  permission: "jobs.vip"
  require: true

# Admin override
admin_override:
  permission: "jobs.admin"
  require: true

# Banned check
not_banned:
  permission: "jobs.banned"
  require: false

# Region access
region_access:
  permission: "worldguard.region.{regionname}"
  require: true
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  permission:
    permission: "customcrops.harvest.premium"
    require: true
    accept:
      message: "&aCustomCrops premium access!"
    deny:
      message: "&cCustomCrops premium permission required!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  permission:
    permission: "nexo.use.special_blocks"
    require: true
    accept:
      message: "&6Nexo special blocks access!"
    deny:
      message: "&cNexo special blocks permission required!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  permission:
    permission: "itemsadder.recipe.advanced"
    require: true
    accept:
      message: "&bItemsAdder advanced recipes unlocked!"
    deny:
      message: "&cItemsAdder advanced recipe permission required!"
```
{% endtab %}
{% endtabs %}