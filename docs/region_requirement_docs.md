# REGION

Restricts actions to specific WorldGuard regions or excludes certain regions.

## Description

Region requirements integrate with WorldGuard to check if a player is within specific protected regions. This allows precise location control for job activities, perfect for mining claims, VIP areas, event zones, and protected territories.

## Requirements

- **WorldGuard plugin** must be installed and running
- Regions must be properly defined in WorldGuard

## Basic Configuration

```yaml
requirements:
  region:
    regions: 
      - "mining_zone"
      - "vip_area"
    blacklist: false  # false = whitelist (allow only these), true = blacklist (deny these)
    deny:
      message: "&cYou must be in an authorized mining zone!"
      sound: "ENTITY_VILLAGER_NO"
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `regions` | List | [] | List of WorldGuard region names to check |
| `blacklist` | Boolean | false | false = whitelist mode, true = blacklist mode |

## Whitelist vs Blacklist

### Whitelist Mode (blacklist: false)
**Only allows action in specified regions**

```yaml
requirements:
  region:
    regions: 
      - "diamond_mine"
      - "vip_mining_area"
    blacklist: false
    deny:
      message: "&cDiamond mining only in designated areas!"
      commands:
        - "tellraw {player} \"Use /rg info to see available regions!\""
```

### Blacklist Mode (blacklist: true)
**Prevents action in specified regions**

```yaml
requirements:
  region:
    regions: 
      - "spawn"
      - "safe_zone"
      - "market"
    blacklist: true
    deny:
      message: "&cJobs disabled in protected areas!"
      sound: "ENTITY_VILLAGER_NO"
```

## Advanced Configuration

### Multi-Region Mining Setup

```yaml
emerald_mining:
  target: "EMERALD_ORE"
  xp: 100.0
  money: 500.0
  requirements:
    logic: "AND"
    multiple_condition:
      authorized_region:
        region:
          regions:
            - "emerald_mine_1"
            - "emerald_mine_2"
            - "vip_emerald_zone"
          blacklist: false
        deny:
          message: "&cEmerald mining only in authorized zones!"
          commands:
            - "tellraw {player} \"Available zones: emerald_mine_1, emerald_mine_2, vip_emerald_zone\""
      
      not_restricted:
        region:
          regions:
            - "restricted_area"
            - "admin_zone"
          blacklist: true
        deny:
          message: "&cThis area is off-limits for mining!"
      
      accept:
        message: "&a💚 Emerald mining zone confirmed!"
        sound: "BLOCK_NOTE_BLOCK_CHIME"
        commands:
          - "effect give {player} luck 300 1"
```

### VIP Region Access

```yaml
vip_exclusive_mining:
  target: "NETHERITE_SCRAP"
  xp: 500.0
  money: 1000.0
  requirements:
    logic: "AND"
    multiple_condition:
      vip_region:
        region:
          regions:
            - "vip_nether_mine"
            - "exclusive_zone"
          blacklist: false
        accept:
          message: "&6VIP exclusive mining area!"
          commands:
            - "effect give {player} fire_resistance 600 1"
            - "effect give {player} haste 600 2"
        deny:
          message: "&cVIP exclusive area required!"
      
      membership_check:
        permission:
          permission: "jobs.vip.platinum"
        deny:
          message: "&cPlatinum VIP membership required!"
      
      accept:
        message: "&5🔥 EXCLUSIVE: VIP netherite mining!"
        commands:
          - "broadcast &5{player} &6is mining in the VIP exclusive zone!"
```

## Region Categories

### Mining Regions

```yaml
specialized_mining:
  target: "DIAMOND_ORE"
  xp: 50.0
  requirements:
    logic: "OR"
    multiple_condition:
      public_mine:
        region:
          regions:
            - "public_diamond_mine"
            - "community_mine"
        accept:
          message: "&aPublic mining area!"
      
      private_mine:
        region:
          regions:
            - "private_mine_1"
            - "private_mine_2"
        accept:
          message: "&6Private mining area!"
          commands:
            - "eco take {player} 100"  # Mining fee
      
      clan_mine:
        region:
          regions:
            - "clan_territory"
        permission:
          permission: "clan.member"
        accept:
          message: "&9Clan mining territory!"
      
      deny:
        message: "&cNo authorized mining area found!"
```

### Event Regions

```yaml
tournament_fishing:
  target: "TROPICAL_FISH"
  xp: 30.0
  requirements:
    logic: "AND"
    multiple_condition:
      tournament_area:
        region:
          regions:
            - "fishing_tournament"
            - "event_lake"
          blacklist: false
        deny:
          message: "&cTournament fishing only in event areas!"
      
      event_active:
        placeholder:
          placeholder: "%tournament_active%"
          operator: "equals"
          value: "true"
        deny:
          message: "&cNo active fishing tournament!"
      
      registration:
        placeholder:
          placeholder: "%tournament_registered_{player}%"
          operator: "equals"
          value: "true"
        deny:
          message: "&cYou must register for the tournament!"
          commands:
            - "tellraw {player} \"Use /tournament join to register!\""
      
      accept:
        message: "&b🏆 Tournament area fishing!"
        commands:
          - "tournament addpoints {player} 1"
```

### Protection Integration

```yaml
safe_zone_restrictions:
  target: "PLAYER_KILL"
  xp: 100.0
  requirements:
    logic: "AND"
    multiple_condition:
      not_safe:
        region:
          regions:
            - "spawn"
            - "safe_zone"
            - "market"
            - "tutorial"
          blacklist: true
        deny:
          message: "&cPvP disabled in safe zones!"
          cancel-event: true
      
      pvp_area:
        region:
          regions:
            - "pvp_arena"
            - "warzone"
            - "battlefield"
          blacklist: false
        accept:
          message: "&c⚔️ PvP zone confirmed!"
          commands:
            - "effect give {player} strength 300 1"
        deny:
          message: "&cPvP only allowed in designated areas!"
      
      accept:
        message: "&4💀 Combat zone active!"
```

## Region Hierarchies

### Nested Region Support

```yaml
# Parent and child regions
hierarchical_mining:
  target: "GOLD_ORE"
  xp: 20.0
  requirements:
    logic: "OR"
    multiple_condition:
      main_mine:
        region:
          regions:
            - "main_mining_area"  # Parent region
        accept:
          message: "&eMain mining area access!"
      
      sub_areas:
        region:
          regions:
            - "mine_shaft_1"      # Child regions
            - "mine_shaft_2"
            - "gold_vein_alpha"
        accept:
          message: "&6Specialized mining area!"
          commands:
            - "effect give {player} haste 300 1"
      
      deny:
        message: "&cAccess to mining areas required!"
```

### Region Priorities

```yaml
priority_mining:
  target: "ANCIENT_DEBRIS"
  xp: 1000.0
  requirements:
    logic: "OR"
    multiple_condition:
      tier_1_exclusive:
        region:
          regions:
            - "elite_ancient_debris"
        permission:
          permission: "mining.tier1"
        accept:
          message: "&4TIER 1: Elite ancient debris zone!"
          commands:
            - "effect give {player} fire_resistance 1200 1"
            - "effect give {player} haste 1200 3"
      
      tier_2_premium:
        region:
          regions:
            - "premium_ancient_debris"
        permission:
          permission: "mining.tier2"
        accept:
          message: "&6TIER 2: Premium ancient debris zone!"
          commands:
            - "effect give {player} fire_resistance 1200 1"
            - "effect give {player} haste 1200 2"
      
      tier_3_standard:
        region:
          regions:
            - "standard_ancient_debris"
        accept:
          message: "&eStandard ancient debris zone!"
          commands:
            - "effect give {player} fire_resistance 600 1"
      
      deny:
        message: "&cNo access to ancient debris mining zones!"
        cancel-event: true
```

## WorldGuard Integration Examples

### Flag-Based Restrictions

```yaml
# Using WorldGuard flags
flag_based_mining:
  target: "IRON_ORE"
  xp: 10.0
  requirements:
    logic: "AND"
    multiple_condition:
      mining_allowed:
        region:
          regions:
            - "__global__"  # Global region
        placeholder:
          placeholder: "%worldguard_region_flag_mining%"
          operator: "equals"
          value: "allow"
        deny:
          message: "&cMining flag not enabled in this region!"
      
      entry_allowed:
        placeholder:
          placeholder: "%worldguard_can_build%"
          operator: "equals"
          value: "true"
        deny:
          message: "&cYou don't have build permission in this region!"
```

### Member-Only Regions

```yaml
member_region_access:
  target: "DIAMOND_ORE"
  xp: 75.0
  requirements:
    logic: "AND"
    multiple_condition:
      member_region:
        region:
          regions:
            - "member_diamond_mine"
        placeholder:
          placeholder: "%worldguard_region_members%"
          operator: "contains"
          value: "%player_name%"
        accept:
          message: "&aMember region access!"
          commands:
            - "effect give {player} luck 600 2"
        deny:
          message: "&cYou must be a member of this region!"
```

## Practical Examples

### Land Claim Integration

```yaml
claim_mining:
  target: "EMERALD_ORE"
  xp: 150.0
  requirements:
    logic: "OR"
    multiple_condition:
      own_claim:
        placeholder:
          placeholder: "%worldguard_region_owner%"
          operator: "equals"
          value: "%player_name%"
        accept:
          message: "&6Your claim: Full mining rights!"
          commands:
            - "eco give {player} 100"  # Owner bonus
      
      member_claim:
        placeholder:
          placeholder: "%worldguard_region_members%"
          operator: "contains"
          value: "%player_name%"
        accept:
          message: "&aMember claim: Standard mining!"
      
      public_area:
        region:
          regions:
            - "__global__"
          blacklist: false
        placeholder:
          placeholder: "%worldguard_region_name%"
          operator: "equals"
          value: "wilderness"
        accept:
          message: "&7Public wilderness mining!"
      
      deny:
        message: "&cPrivate claim: No mining permission!"
        cancel-event: true
```

## Compatibilities

{% tabs %}
{% tab title="CustomCrops" %}
```yaml
requirements:
  region:
    regions:
      - "farming_region"
      - "crop_zone"
    blacklist: false
    accept:
      message: "&aCustomCrops farming region!"
    deny:
      message: "&cCustomCrops only in farming regions!"
```
{% endtab %}

{% tab title="Nexo" %}
```yaml
requirements:
  region:
    regions:
      - "nexo_blocks_area"
      - "custom_zone"
    blacklist: false
    accept:
      message: "&6Nexo blocks region!"
    deny:
      message: "&cNexo features restricted to special regions!"
```
{% endtab %}

{% tab title="ItemsAdder" %}
```yaml
requirements:
  region:
    regions:
      - "itemsadder_zone"
      - "custom_items_region"
    blacklist: false
    accept:
      message: "&bItemsAdder region access!"
    deny:
      message: "&cItemsAdder features only in designated regions!"
```
{% endtab %}
{% endtabs %}