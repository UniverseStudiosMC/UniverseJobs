# 🔧 Système de Conditions

The system de conditions de JobsAdventure permet de créer des règles complexes pour contrôler quand the actions donnent de l'XP et quand the rewards peuvent être claimeds. Il utilise une logique AND/OR flexible avec support des groupes imbriqués.

## 🎯 Vue d'ensemble

### Fonctionnalités Principales
- **Logique AND/OR** : Combinaison flexible of conditions
- **Groupes imbriqués** : Structure hiérarchique de conditions complexes
- **Types multiples** : 9 types de conditions différents
- **Actions conditionnelles** : Messages et Commands selon le résultat
- **Performance optimisée** : Évaluation rapide et cache intelligent

### Usage
Les conditions peuvent être utilisées dans :
- **Actions de jobs** : Contrôler quand l'XP est accordé
- **Récompenses** : Définir les prérequis pour réclamation
- **Commands dynamiques** : Exécution conditionnelle
- **Messages adaptatifs** : Affichage selon le contexte

## 📋 Types de Conditions

### 1. Permission (`permission`)
Vérifie si un Player possède une permission spécifique.

```yaml
permission:
  permission: "vip.mining"     # Permission à vérifier
  require: true                # true = doit avoir, false = ne doit pas avoir
  accept:
    message: "&aVous êtes VIP !"
    sound: "ENTITY_PLAYER_LEVELUP"
  deny:
    message: "&cVous devez être VIP pour cette action !"
    sound: "ENTITY_VILLAGER_NO"
```

**Cas d'usage** :
- Réserver certaines actions aux VIP
- Empêcher l'XP pour les players bannis
- Créer des zones spéciales selon les rangs

### 2. Placeholder (`placeholder`)
Utilise PlaceholderAPI pour évaluer of conditions dynamiques.

```yaml
placeholder:
  placeholder: "%player_level%"     # Placeholder à évaluer
  operator: "greater_than"          # Opérateur de comparaison
  value: "25"                      # Valeur de référence
  accept:
    message: "&aVous avez le Level requis !"
  deny:
    message: "&cVous devez être Level 25+ !"
```

**Opérateurs disponibles** :
- `equals` : Égal à
- `not_equals` : Différent de
- `greater_than` : Supérieur à
- `greater_than_or_equal` : Supérieur ou égal à
- `less_than` : Inférieur à
- `less_than_or_equal` : Inférieur ou égal à
- `contains` : Contient (pour texte)
- `starts_with` : Commence par
- `ends_with` : Finit par

**Examples avancés** :
```yaml
# Vérifier l'argent of the player
placeholder:
  placeholder: "%vault_eco_balance%"
  operator: "greater_than_or_equal"
  value: "1000"

# Vérifier un autre job
placeholder:
  placeholder: "%jobsadventure_farmer_player_level%"
  operator: "greater_than"
  value: "10"

# Vérifier une région
placeholder:
  placeholder: "%worldguard_region_name%"
  operator: "equals"
  value: "mining_zone"
```

### 3. Item (`item`)
Vérifie l'item en main of the player. Supporte maintenant les matériaux multiples !

```yaml
item:
  # Support des matériaux multiples (OU logique)
  material: 
    - "IRON_PICKAXE"
    - "DIAMOND_PICKAXE"
    - "NETHERITE_PICKAXE"
  
  # OU format traditionnel (toujours supporté)
  # material: "DIAMOND_PICKAXE"
  
  name: "&bPioche de Maître"        # Nom affiché (optionnel)
  lore:                             # Lore requis (optionnel)
    - "&7Pioche spéciale"
    - "&7pour le mining avancé"
  nbt: '{Enchantments:[{id:"efficiency",lvl:5}]}'  # NBT requis (optionnel)
  amount:                           # Quantité requise (optionnel)
    min: 1
    max: 64
  accept:
    message: "&aBonne pioche !"
  deny:
    message: "&cVous devez avoir une pioche en fer ou mieux !"
```

**Options avancées** :
```yaml
item:
  # Support des items custom
  material: "nexo:mythril_pickaxe"
  
  # Vérification des enchantements
  enchantments:
    efficiency: 3
    unbreaking: 2
    
  # Vérification de la durabilité
  durability:
    min: 50        # Minimum 50% de durabilité
    max: 100
    
  # Items MultiBrooks/ItemsAdder
  custom_id: "SPECIAL_TOOL"
```

### 4. World (`world`)
Limite the actions à certains mondes.

```yaml
world:
  worlds:                           # Liste des mondes autorisés
    - "world"
    - "mining_world"
    - "resources"
  blacklist: false                  # true = mondes interdits, false = mondes autorisés
  accept:
    message: "&aBon monde pour miner !"
  deny:
    message: "&cVous ne pouvez pas miner ici !"
```

**Examples** :
```yaml
# Autoriser seulement le monde normal
world:
  worlds: ["world"]
  blacklist: false

# Interdire le créatif et le lobby
world:
  worlds: ["creative", "lobby"]
  blacklist: true

# Support des patterns
world:
  worlds: ["mining_*", "resource_*"]  # Tous les mondes commençant par...
  blacklist: false
```

### 5. Time (`time`)
Conditions basées sur l'heure du jeu.

```yaml
time:
  min: 6000                         # Heure minimale (6h du matin)
  max: 18000                        # Heure maximum (18h)
  accept:
    message: "&aIl fait jour, bon moment pour miner !"
  deny:
    message: "&cTrop dangereux de miner la nuit !"
```

**Heures Minecraft** :
- `0` : Minuit
- `6000` : 6h du matin (lever du soleil)
- `12000` : Midi
- `18000` : 18h (coucher du soleil)
- `23999` : Fin de la journée

**Examples avancés** :
```yaml
# Seulement la nuit
time:
  min: 18000
  max: 6000        # Supporte le passage minuit

# Happy hour (bonus XP)
time:
  min: 19000       # 19h à 21h
  max: 21000
  accept:
    message: "&6⭐ Happy Hour ! XP doublé !"
    commands:
      - "jobs admin bonus player %player% set all 2.0 2h"
```

### 6. Weather (`weather`)
Conditions basées sur la météo.

```yaml
weather:
  weather: "CLEAR"                  # Type de météo requis
  accept:
    message: "&aBeau temps pour travailler !"
  deny:
    message: "&cPas possible sous la pluie !"
```

**Types de météo** :
- `CLEAR` : Temps clair
- `RAIN` : Pluie
- `STORM` : Orage

**Examples** :
```yaml
# Farming seulement sous la pluie
weather:
  weather: "RAIN"
  accept:
    message: "&aLa pluie aide les cultures !"

# Mining pas pendant les orages
weather:
  weather: "STORM"
  require: false
  deny:
    message: "&cTrop dangereux de miner pendant l'orage !"
```

### 7. Biome (`biome`)
Conditions basées sur le biome.

```yaml
biome:
  biomes:                           # Liste des biomes autorisés
    - "DESERT"
    - "DESERT_HILLS"
    - "BADLANDS"
  blacklist: false                  # true = biomes interdits
  accept:
    message: "&eBon biome pour trouver de l'or !"
  deny:
    message: "&cPas d'or dans ce biome !"
```

**Biomes courants** :
- Montagnes : `MOUNTAINS`, `MOUNTAIN_EDGE`
- Déserts : `DESERT`, `DESERT_HILLS`, `BADLANDS`
- Océans : `OCEAN`, `DEEP_OCEAN`
- Forêts : `FOREST`, `BIRCH_FOREST`, `DARK_FOREST`

### 8. Region (`region`)
Intégration WorldGuard pour les régions. *(Requiert WorldGuard)*

```yaml
region:
  regions:                          # Liste des régions autorisées
    - "mining_zone"
    - "vip_area"
  blacklist: false
  accept:
    message: "&aZone de mining autorisée !"
  deny:
    message: "&cVous devez être dans une zone de mining !"
```

### 9. Custom (`custom`)
Conditions customs via the API.

```yaml
custom:
  type: "mon_Plugin_condition"      # Type de condition custom
  parameters:                       # Paramètres spécifiques
    min_level: 25
    required_achievement: "master_miner"
  accept:
    message: "&aCondition custom remplie !"
  deny:
    message: "&cCondition custom non remplie !"
```

## 🌟 Structure Multiple Condition

La structure `multiple_condition` permet de définir plusieurs conditions nommées avec des messages d'accept/deny globaux et individuels pour une flexibilité maximale.

### Syntaxe de Base
```yaml
requirements:
  logic: "AND"
  multiple_condition:
    # Conditions individuelles avec leurs propres deny messages
    permission:
      permission: "jobsadventure.mine.iron"
      require: true
      deny:
        message: "&cVous n'avez pas la permission de miner le fer !"
    
    placeholder:
      placeholder: "%player_level%"
      operator: "greater_than"
      value: "10"
      deny:
        message: "&cVous devez être niveau 10+ pour miner du fer !"
        sound: "ENTITY_VILLAGER_NO"
    
    item:
      material: 
        - "IRON_PICKAXE"
        - "DIAMOND_PICKAXE"
      deny:
        message: "&cVous avez besoin d'une pioche en fer ou mieux !"
        sound: "BLOCK_ANVIL_PLACE"
        cancelEvent: true
    
    # Messages globaux (optionnels)
    accept:
      message: "&a✅ Toutes les conditions remplies !"
      sound: "ENTITY_PLAYER_LEVELUP"
    
    deny:
      message: "&c❌ Conditions non remplies"
      sound: "ENTITY_VILLAGER_NO"
      cancelEvent: true
```

### Avantages de Multiple Condition

1. **Messages Spécifiques** : Chaque condition peut avoir son propre message d'erreur
2. **Flexibilité** : Combine la logique AND/OR avec des messages personnalisés
3. **Lisibilité** : Structure claire et organisée
4. **Performance** : Évaluation optimisée avec court-circuit

### Hiérarchie des Messages

L'ordre de priorité pour les messages est le suivant :

1. **Message individuel** de la condition qui échoue (le plus spécifique)
2. **Message global deny** (si pas de message individuel)
3. **Message par défaut** du système

```yaml
multiple_condition:
  permission:
    permission: "vip.mining"
    require: true
    deny:
      message: "&cMessage spécifique VIP"  # Priorité 1
  
  placeholder:
    placeholder: "%player_level%"
    operator: "greater_than"
    value: "25"
    # Pas de deny message individuel
  
  # Message global pour les conditions sans message spécifique
  deny:
    message: "&cMessage global par défaut"   # Priorité 2 pour placeholder
    sound: "ENTITY_VILLAGER_NO"
```

### Support des Matériaux Multiples

La condition `item` supporte maintenant plusieurs matériaux avec logique OR :

```yaml
multiple_condition:
  tool_check:
    material: 
      - "IRON_PICKAXE"      # OU
      - "DIAMOND_PICKAXE"   # OU
      - "NETHERITE_PICKAXE" # OU
      # Le joueur doit avoir UN de ces outils
    deny:
      message: "&cVous avez besoin d'une pioche en fer ou mieux !"
  
  # Backward compatible - fonctionne toujours
  single_tool:
    material: "DIAMOND_SWORD"
```

### Exemple Complexe : Système de Mining Avancé

```yaml
coal_ore:
  target: "COAL_ORE"
  xp: 5.0
  name: "Coal Mining"
  description: "Mining coal ore with advanced requirements"
  requirements:
    logic: "AND"
    multiple_condition:
      # Vérification des permissions
      mining_permission:
        permission: "jobsadventure.mine.coal"
        require: true
        deny:
          message: "&c🚫 Permission manquante pour miner le charbon !"
          sound: "ENTITY_VILLAGER_NO"
      
      # Niveau minimum requis
      level_check:
        placeholder: "%jobsadventure_miner_player_level%"
        operator: "greater_than_or_equal"
        value: "5"
        deny:
          message: "&c📊 Niveau mineur 5+ requis (actuel: %jobsadventure_miner_player_level%)"
          sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
      
      # Vérification de l'outil
      tool_requirement:
        material: 
          - "STONE_PICKAXE"
          - "IRON_PICKAXE"
          - "DIAMOND_PICKAXE"
          - "NETHERITE_PICKAXE"
        deny:
          message: "&c⛏️ Pioche requise (pierre, fer, diamant ou netherite) !"
          sound: "BLOCK_ANVIL_PLACE"
          cancelEvent: true
      
      # Vérification de l'argent (coût par action)
      money_check:
        placeholder: "%vault_eco_balance%"
        operator: "greater_than_or_equal"
        value: "10"
        deny:
          message: "&c💰 10$ requis par bloc miné (solde: %vault_eco_balance%$)"
          sound: "ENTITY_VILLAGER_NO"
      
      # Zone autorisée
      location_check:
        world:
          worlds: ["mining_world", "world"]
          blacklist: false
        deny:
          message: "&c🌍 Mining autorisé uniquement dans mining_world !"
      
      # Messages de succès
      accept:
        message: "&a✅ Mining réussi ! Charbon obtenu !"
        sound: "ENTITY_PLAYER_LEVELUP"
        commands:
          - "eco take %player% 10"  # Prélever le coût
          - "give %player% coal 1"  # Donner le charbon bonus
      
      # Message d'échec global (si pas de message spécifique)
      deny:
        message: "&c❌ Impossible de miner - vérifiez les conditions"
        sound: "ENTITY_VILLAGER_NO"
        cancelEvent: true
```

## 🔗 Logique AND/OR

### Logique Simple
```yaml
requirements:
  logic: "AND"                      # Toutes the conditions doivent être vraies
  permission:
    permission: "vip.mining"
    require: true
  time:
    min: 6000
    max: 18000
```

```yaml
requirements:
  logic: "OR"                       # Une seule condition doit être vraie
  world:
    worlds: ["mining_world"]
  permission:
    permission: "mining.anywhere"
```

### Groupes Imbriqués
```yaml
requirements:
  logic: "AND"
  
  # Condition simple au Level principal
  permission:
    permission: "jobs.mining"
    require: true
  
  # Groupe de conditions avec logique OR
  groups:
    time_or_vip:
      logic: "OR"
      time:
        min: 6000
        max: 18000
      permission:
        permission: "vip.anytime"
        require: true
    
    # Autre groupe avec logique AND
    location_requirements:
      logic: "AND"
      world:
        worlds: ["mining_world"]
      biome:
        biomes: ["MOUNTAINS", "DESERT"]
```

### Example Complexe : Mine VIP
```yaml
requirements:
  logic: "AND"
  
  # Doit avoir l'outil approprié
  item:
    material: "DIAMOND_PICKAXE"
    enchantments:
      efficiency: 3
  
  # Conditions alternatives pour l'accès
  groups:
    access_methods:
      logic: "OR"
      
      # Méthode 1: VIP Gold
      vip_gold:
        logic: "AND"
        permission:
          permission: "vip.gold"
          require: true
        placeholder:
          placeholder: "%vault_eco_balance%"
          operator: "greater_than"
          value: "10000"
      
      # Méthode 2: Level élevé + temps spécial
      high_level:
        logic: "AND"
        placeholder:
          placeholder: "%jobsadventure_miner_player_level%"
          operator: "greater_than"
          value: "50"
        time:
          min: 20000
          max: 6000
      
      # Méthode 3: Admin override
      admin_access:
        permission:
          permission: "admin.mining"
          require: true
  
  # Conditions de localisation
  location:
    logic: "AND"
    region:
      regions: ["vip_mine"]
    biome:
      biomes: ["MOUNTAINS"]
```

## ⚡ Actions Conditionnelles

### Messages et Sons
```yaml
permission:
  permission: "vip.bonus"
  require: true
  accept:
    message: "&a✅ Bonus VIP activé !"
    sound: "ENTITY_PLAYER_LEVELUP"
    title:
      title: "&6Bonus VIP"
      subtitle: "&eXP multiplié par 2 !"
      duration: 60  # ticks
  deny:
    message: "&c❌ Vous devez être VIP !"
    sound: "ENTITY_VILLAGER_NO"
    actionbar: "&cUpgrade vers VIP pour plus de bonus !"
```

### Commands Conditionnelles
```yaml
placeholder:
  placeholder: "%jobsadventure_miner_player_level%"
  operator: "equals"
  value: "100"
  accept:
    message: "&6🎉 Level maximum atteint !"
    commands:
      - "broadcast %player% a atteint the max level en Mining !"
      - "give %player% diamond_block 10"
      - "titles send %player% title:&6MAÎTRE subtitle:&eMining_Level_100 fadeIn:20 stay:60 fadeOut:20"
      - "jobs admin player %player% reset miner"  # Reset pour prestige
```

### Effets et Particules
```yaml
time:
  min: 0
  max: 6000
  accept:
    message: "&9Bonus nocturne activé !"
    effects:
      - "NIGHT_VISION:30:1"      # Effet:durée:Level
      - "SPEED:30:1"
    particles:
      - "ENCHANTMENT_TABLE:10"   # Type:quantité
      - "VILLAGER_HAPPY:5"
```

## 🎯 Cas d'Usage Avancés

### Système de Prestige
```yaml
# Action spéciale pour prestige
prestige_action:
  target: "BEDROCK"
  xp: 0
  requirements:
    logic: "AND"
    placeholder:
      placeholder: "%jobsadventure_miner_player_level%"
      operator: "equals"
      value: "100"
    item:
      material: "NETHER_STAR"
      name: "&6Étoile de Prestige"
    accept:
      message: "&6⭐ Prestige accompli ! Vous recommencez au Level 1 avec des bonus !"
      commands:
        - "jobs admin player %player% reset miner"
        - "jobs admin player %player% setlevel miner 1"
        - "permission add %player% prestige.miner.1"
        - "give %player% diamond_pickaxe{Enchantments:[{id:efficiency,lvl:10},{id:unbreaking,lvl:5}]} 1"
```

### Events Temporaires
```yaml
# Event spécial week-end
weekend_bonus:
  target: "*"
  xp: 25.0
  requirements:
    logic: "AND"
    placeholder:
      placeholder: "%server_time_day_of_week%"
      operator: "equals"
      value: "SATURDAY,SUNDAY"
    time:
      min: 14000   # Après-midi
      max: 22000
    accept:
      message: "&6🎉 Bonus week-end ! +25 XP supplémentaire !"
      title:
        title: "&6WEEK-END BONUS"
        subtitle: "&e+25 XP"
```

### Système de Quêtes Intégré
```yaml
# Quête: Miner 1000 blocs de pierre
quest_stone_mining:
  target: "STONE"
  xp: 1.0
  requirements:
    logic: "AND"
    placeholder:
      placeholder: "%quests_player_quest_progress_stone_collector%"
      operator: "less_than"
      value: "1000"
    permission:
      permission: "quest.stone_collector.active"
      require: true
    accept:
      commands:
        - "quests progress %player% stone_collector 1"
      message: "&7⛏️ Progression quête: %quests_player_quest_progress_stone_collector%/1000"
    deny:
      message: "&cQuête terminée ou non active !"
```

### Mine à Péage
```yaml
# Système de paiement par action
toll_mining:
  target: "DIAMOND_ORE"
  xp: 50.0
  requirements:
    logic: "AND"
    placeholder:
      placeholder: "%vault_eco_balance%"
      operator: "greater_than_or_equal"
      value: "100"
    region:
      regions: ["diamond_mine"]
    accept:
      message: "&b💎 Diamant miné ! -$100 de frais"
      commands:
        - "eco take %player% 100"
    deny:
      message: "&cPas assez d'argent ! ($100 requis par diamant)"
```

## 🔧 Configuration et Debug

### Debug des Conditions
```yaml
# Activer le debug pour voir l'évaluation
debug:
  conditions: true
  
# Logs détaillés
requirements:
  debug: true  # Active le debug pour ce groupe
  logic: "AND"
  # ... conditions ...
```

### Optimisation Performance
```yaml
# Ordre d'évaluation optimisé (conditions rapides en premier)
requirements:
  logic: "AND"
  
  # 1. Conditions rapides (permission, cache)
  permission:
    permission: "mining.allowed"
  
  # 2. Conditions moyennes (placeholder cache)
  placeholder:
    placeholder: "%player_level%"
    operator: "greater_than"
    value: "10"
  
  # 3. Conditions lentes (calculs, API)
  custom:
    type: "complex_calculation"
```

### Messages d'Erreur
```yaml
permission:
  permission: "invalid.permission"
  require: true
  accept:
    message: "&aOK"
  deny:
    message: "&cErreur: %error%"  # Affiche l'erreur si debug activé
  error:
    message: "&4Erreur système: %error_details%"
```

## 🔗 Voir Aussi

- [Configuration des Jobs](../jobs-Configuration/creating-jobs.md)
- [Système de Récompenses](../rewards/reward-conditions.md)
- [Placeholders](../reference/placeholders.md)
- [Intégrations](../integrations/)
- [API pour Développeurs](../api/custom-conditions.md)

---

The system de conditions JobsAdventure offre flexibility maximum to create mécaniques de jeu complexes et engageantes, adaptées aux besoins specific to your Server.
