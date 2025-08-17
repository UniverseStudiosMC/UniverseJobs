# 📝 Exemples de configuration complète

Cette page présente des exemples complets de configuration de métiers pour vous inspirer et vous aider à créer vos propres métiers personnalisés.

## 🏗️ Structure de base d'un métier

Chaque métier est défini dans un fichier YAML dans le dossier `/plugins/JobsAdventure/jobs/`. Voici la structure de base :

```yaml
# Informations de base
name: "NomDuMétier"
description: "Description du métier"
enabled: true
max-level: 100
permission: "jobsadventure.job.nomdumétier"
icon: "MATERIAL_ICON"

# Système de récompenses
rewards: "nom_fichier_récompenses"
gui-reward: "nom_fichier_gui_récompenses"

# Courbe d'expérience
xp-equation: "100 * Math.pow(level, 1.8)"

# Messages XP
xp-message:
  type: "bossbar"
  text: "+{exp} XP - {job}"
  bossbar:
    color: "green"
    style: "segmented_0"
    duration: 60

# Description du métier
lore:
  - "&7Description ligne 1"
  - "&7Description ligne 2"

# Actions qui donnent de l'XP
actions:
  TYPE_ACTION:
    nom_action:
      target: "CIBLE"
      xp: 10.0
      name: "Nom de l'action"
      description: "Description de l'action"
      # Conditions optionnelles
      requirements:
        # ...
```

## ⛏️ Exemple complet : Métier de Mineur

```yaml
# /plugins/JobsAdventure/jobs/miner.yml
name: "Mineur"
description: "Maître de l'extraction souterraine"
enabled: true
max-level: 100
permission: "jobsadventure.job.miner"
icon: "DIAMOND_PICKAXE"

rewards: "miner_rewards"
gui-reward: "miner_gui"

# Courbe d'XP progressive
xp-equation: "100 * Math.pow(level, 1.8)"

# Messages avec barre de boss colorée
xp-message:
  type: "bossbar"
  text: "&6⛏ +{exp} XP Mining &7({level})"
  bossbar:
    color: "yellow"
    style: "segmented_10"
    duration: 80
    show-progress: true

lore:
  - "&7Creusez dans les profondeurs de la terre"
  - "&7Découvrez des minerais précieux"
  - "&7Bonus XP pour les matériaux rares"
  - "&7Compatible avec tous les plugins de blocs personnalisés"

actions:
  # Minage de base
  BREAK:
    # Pierre de base
    stone:
      target: "STONE"
      xp: 1.0
      name: "Extraction de pierre"
      description: "Minage basique de pierre"
    
    # Minerais communs
    coal_ore:
      target: "COAL_ORE"
      xp: 5.0
      name: "Extraction de charbon"
      description: "Minage de minerai de charbon"
      requirements:
        logic: "AND"
        item:
          material: "IRON_PICKAXE"
          deny:
            message: "&cUne pioche en fer minimum est requise !"
            sound: "BLOCK_ANVIL_PLACE"
    
    iron_ore:
      target: "IRON_ORE"
      xp: 12.0
      name: "Extraction de fer"
      description: "Minage de minerai de fer"
      requirements:
        logic: "AND"
        item:
          material: "IRON_PICKAXE"
        placeholder:
          placeholder: "%jobsadventure_miner_player_level%"
          operator: "greater_than"
          value: "10"
          deny:
            message: "&cNiveau 10 requis en minage !"
    
    # Minerais précieux
    gold_ore:
      target: "GOLD_ORE"
      xp: 25.0
      name: "Extraction d'or"
      description: "Minage de minerai d'or précieux"
      requirements:
        logic: "AND"
        item:
          material: "DIAMOND_PICKAXE"
        world:
          worlds: ["world", "mining_world"]
          blacklist: false
          deny:
            message: "&cL'or ne peut être miné que dans certains mondes !"
    
    diamond_ore:
      target: "DIAMOND_ORE"
      xp: 50.0
      name: "Extraction de diamant"
      description: "Minage du précieux diamant"
      message:
        type: "BOSSBAR"
        style: "segment_0"
        color: "BLUE"
        duration: 100
        message: "&b💎 DIAMANT TROUVÉ ! +50 XP"
      sound: "ENTITY_PLAYER_LEVELUP"
      requirements:
        logic: "AND"
        item:
          material: "DIAMOND_PICKAXE"
        time:
          min: 13000  # Nuit seulement
          max: 23000
          deny:
            message: "&cLes diamants sont plus faciles à trouver la nuit !"
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "16"
          deny:
            message: "&cLes diamants se trouvent sous Y=16 !"
    
    # Minerais Nether
    ancient_debris:
      target: "ANCIENT_DEBRIS"
      xp: 100.0
      name: "Extraction de débris antique"
      description: "Minage du rare débris antique"
      commands:
        - "broadcast &6{player} &ea trouvé des débris antiques !"
      requirements:
        logic: "AND"
        item:
          material: "NETHERITE_PICKAXE"
        world:
          worlds: ["world_nether"]
          blacklist: false
    
    # Blocs personnalisés Nexo
    mythril_ore:
      target: "nexo:mythril_ore"
      xp: 75.0
      name: "Extraction de mythril"
      description: "Minage du légendaire mythril"
      requirements:
        logic: "AND"
        item:
          mmoitems:
            type: "TOOL"
            id: "MYTHRIL_PICKAXE"
          deny:
            message: "&cSeule une pioche en mythril peut extraire ce minerai !"
  
  # Combat souterrain
  KILL:
    cave_spider:
      target: "CAVE_SPIDER"
      xp: 8.0
      name: "Élimination d'araignée des cavernes"
      description: "Combat dans les mines"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "50"
          deny:
            message: "&cBonus souterrain uniquement !"
    
    zombie:
      target: "ZOMBIE"
      xp: 5.0
      name: "Élimination de zombie mineur"
      description: "Nettoyer les mines des morts-vivants"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%player_y%"
          operator: "less_than"
          value: "60"
    
    # Boss MythicMobs
    cave_guardian:
      target: "MYTHICMOB:CaveGuardian"
      xp: 200.0
      name: "Défaite du Gardien des Cavernes"
      description: "Vaincre le redoutable gardien"
      commands:
        - "broadcast &6&l{player} &ea vaincu le Gardien des Cavernes !"
        - "give {player} diamond 10"
      requirements:
        logic: "AND"
        permission:
          permission: "jobs.boss.cave"
          require: true
```

## 🌾 Exemple complet : Métier de Fermier

```yaml
# /plugins/JobsAdventure/jobs/farmer.yml
name: "Fermier"
description: "Maître de l'agriculture et de l'élevage"
enabled: true
max-level: 75
permission: "jobsadventure.job.farmer"
icon: "GOLDEN_HOE"

rewards: "farmer_rewards"
gui-reward: "farmer_gui"

xp-equation: "80 * Math.pow(level, 1.6) + level * 15"

xp-message:
  type: "actionbar"
  text: "&a🌾 +{exp} XP Agriculture &7[{level}]"
  actionbar:
    duration: 100

lore:
  - "&7Cultivez la terre et élevez des animaux"
  - "&7Maîtrisez l'art de l'agriculture"
  - "&7Bonus saisonniers et multiplicateurs"
  - "&7Intégration CustomCrops complète"

actions:
  # Agriculture de base
  BREAK:
    wheat:
      target: "WHEAT"
      xp: 3.0
      name: "Récolte de blé"
      description: "Récolter du blé mature"
      requirements:
        logic: "AND"
        item:
          material: "HOE"
    
    carrots:
      target: "CARROTS"
      xp: 3.5
      name: "Récolte de carottes"
      description: "Récolter des carottes matures"
    
    potatoes:
      target: "POTATOES"
      xp: 3.5
      name: "Récolte de pommes de terre"
      description: "Récolter des pommes de terre matures"
    
    # CustomCrops
    tomato:
      target: "customcrops:tomato_stage_3"
      xp: 8.0
      name: "Récolte de tomates"
      description: "Récolter des tomates CustomCrops matures"
      message:
        type: "CHAT"
        message: "&a🍅 Tomate récoltée ! +8 XP"
    
    corn:
      target: "customcrops:corn_stage_4"
      xp: 12.0
      name: "Récolte de maïs"
      description: "Récolter du maïs géant"
  
  # Plantation
  PLACE:
    wheat_seeds:
      target: "WHEAT_SEEDS"
      xp: 1.0
      name: "Plantation de blé"
      description: "Planter des graines de blé"
    
    custom_tomato_seeds:
      target: "customcrops:tomato_seeds"
      xp: 2.0
      name: "Plantation de tomates"
      description: "Planter des graines de tomates"
  
  # Élevage
  KILL:
    cow:
      target: "COW"
      xp: 8.0
      name: "Abattage de vache"
      description: "Élever et abattre du bétail"
      requirements:
        logic: "AND"
        item:
          material: "SWORD"
    
    pig:
      target: "PIG"
      xp: 6.0
      name: "Abattage de cochon"
      description: "Élever et abattre des cochons"
    
    chicken:
      target: "CHICKEN"
      xp: 4.0
      name: "Abattage de poule"
      description: "Élever et abattre de la volaille"
  
  # Soins aux animaux
  INTERACT:
    milk_cow:
      target: "COW"
      xp: 2.0
      name: "Traite de vache"
      description: "Traire une vache avec un seau"
      requirements:
        logic: "AND"
        item:
          material: "BUCKET"
  
  # Artisanat agricole
  CRAFT:
    bread:
      target: "BREAD"
      xp: 2.0
      name: "Fabrication de pain"
      description: "Préparer du pain frais"
    
    cake:
      target: "CAKE"
      xp: 10.0
      name: "Préparation de gâteau"
      description: "Créer un délicieux gâteau"
```

## 🏹 Exemple complet : Métier de Chasseur

```yaml
# /plugins/JobsAdventure/jobs/hunter.yml
name: "Chasseur"
description: "Maître de la chasse et du combat"
enabled: true
max-level: 80
permission: "jobsadventure.job.hunter"
icon: "BOW"

rewards: "hunter_rewards"
gui-reward: "hunter_gui"

xp-equation: "120 * Math.pow(level, 1.7)"

xp-message:
  type: "bossbar"
  text: "&c🏹 +{exp} XP Chasse &7| Niveau {level}"
  bossbar:
    color: "red"
    style: "segmented_6"
    duration: 60

lore:
  - "&7Traquez et chassez les créatures sauvages"
  - "&7Maîtrisez l'art du combat et de la survie"
  - "&7Bonus pour les créatures rares et dangereuses"
  - "&7Intégration MythicMobs avancée"

actions:
  # Chasse de base
  KILL:
    zombie:
      target: "ZOMBIE"
      xp: 5.0
      name: "Élimination de zombie"
      description: "Chasser les morts-vivants"
    
    skeleton:
      target: "SKELETON"
      xp: 6.0
      name: "Élimination de squelette"
      description: "Combattre les archers squelettes"
    
    creeper:
      target: "CREEPER"
      xp: 8.0
      name: "Élimination de creeper"
      description: "Défuser la menace explosive"
      requirements:
        logic: "AND"
        item:
          material: "BOW"
          deny:
            message: "&cUtilisez un arc pour chasser les creepers en sécurité !"
    
    spider:
      target: "SPIDER"
      xp: 4.0
      name: "Élimination d'araignée"
      description: "Chasser les araignées"
    
    # Créatures hostiles avancées
    enderman:
      target: "ENDERMAN"
      xp: 15.0
      name: "Élimination d'Enderman"
      description: "Affronter les téléporteurs de l'End"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%jobsadventure_hunter_player_level%"
          operator: "greater_than"
          value: "20"
          deny:
            message: "&cNiveau 20 en chasse requis pour affronter les Endermen !"
    
    wither_skeleton:
      target: "WITHER_SKELETON"
      xp: 25.0
      name: "Élimination de squelette du Wither"
      description: "Combattre dans le Nether"
      requirements:
        logic: "AND"
        world:
          worlds: ["world_nether"]
          blacklist: false
    
    # Animaux sauvages
    wolf:
      target: "WOLF"
      xp: 10.0
      name: "Chasse au loup"
      description: "Chasser les loups sauvages"
      requirements:
        logic: "AND"
        weather:
          weather: "CLEAR"
          deny:
            message: "&cLes loups sont plus agressifs par temps clair !"
    
    # Boss MythicMobs
    forest_guardian:
      target: "MYTHICMOB:ForestGuardian"
      xp: 150.0
      name: "Défaite du Gardien de la Forêt"
      description: "Vaincre le protecteur de la nature"
      commands:
        - "broadcast &a&l{player} &ea vaincu le Gardien de la Forêt !"
        - "give {player} emerald 15"
      sound: "ENTITY_ENDER_DRAGON_DEATH"
    
    ancient_beast:
      target: "MYTHICMOB:AncientBeast"
      xp: 300.0
      name: "Défaite de la Bête Antique"
      description: "Affronter la créature légendaire"
      requirements:
        logic: "AND"
        permission:
          permission: "jobs.boss.ancient"
        groups:
          group1:
            logic: "OR"
            item:
              mmoitems:
                type: "SWORD"
                id: "LEGENDARY_BLADE"
            item:
              mmoitems:
                type: "BOW"
                id: "MYTHIC_BOW"
  
  # Apprivoisement
  TAME:
    wolf_taming:
      target: "WOLF"
      xp: 20.0
      name: "Apprivoisement de loup"
      description: "Dompter un loup sauvage"
    
    horse_taming:
      target: "HORSE"
      xp: 25.0
      name: "Apprivoisement de cheval"
      description: "Dompter un cheval sauvage"
  
  # Pêche spécialisée (CustomFishing)
  FISH:
    rare_fish:
      target: "customfishing:golden_trout"
      xp: 30.0
      name: "Pêche de truite dorée"
      description: "Attraper une truite rare"
      requirements:
        logic: "AND"
        time:
          min: 6000   # Début de journée
          max: 12000  # Midi
        biome:
          biomes: ["RIVER", "FOREST"]
          blacklist: false
```

## 🔧 Exemple : Métier d'Artisan

```yaml
# /plugins/JobsAdventure/jobs/crafter.yml
name: "Artisan"
description: "Maître de l'artisanat et de la création"
enabled: true
max-level: 60
permission: "jobsadventure.job.crafter"
icon: "CRAFTING_TABLE"

rewards: "crafter_rewards"
gui-reward: "crafter_gui"

xp-equation: "60 * Math.pow(level, 1.5) + level * 10"

xp-message:
  type: "chat"
  text: "&6🔨 +{exp} XP Artisanat &7({job} Niv.{level})"

lore:
  - "&7Créez et fabriquez des objets utiles"
  - "&7Maîtrisez tous les arts de l'artisanat"
  - "&7Bonus pour les objets complexes"
  - "&7Compatible MMOItems et objets personnalisés"

actions:
  # Artisanat de base
  CRAFT:
    wooden_tools:
      target: "WOODEN_PICKAXE,WOODEN_AXE,WOODEN_SHOVEL,WOODEN_SWORD"
      xp: 2.0
      name: "Fabrication d'outils en bois"
      description: "Créer des outils basiques"
    
    stone_tools:
      target: "STONE_PICKAXE,STONE_AXE,STONE_SHOVEL,STONE_SWORD"
      xp: 4.0
      name: "Fabrication d'outils en pierre"
      description: "Créer des outils améliorés"
    
    iron_tools:
      target: "IRON_PICKAXE,IRON_AXE,IRON_SHOVEL,IRON_SWORD"
      xp: 8.0
      name: "Fabrication d'outils en fer"
      description: "Créer des outils de qualité"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%jobsadventure_crafter_player_level%"
          operator: "greater_than"
          value: "15"
    
    diamond_tools:
      target: "DIAMOND_PICKAXE,DIAMOND_AXE,DIAMOND_SHOVEL,DIAMOND_SWORD"
      xp: 20.0
      name: "Fabrication d'outils en diamant"
      description: "Créer des outils de maître"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%jobsadventure_crafter_player_level%"
          operator: "greater_than"
          value: "35"
  
  # Enchantement
  ENCHANT:
    basic_enchant:
      target: "ANY"
      xp: 5.0
      name: "Enchantement basique"
      description: "Enchanter des objets"
    
    high_level_enchant:
      target: "ANY"
      xp: 15.0
      name: "Enchantement avancé"
      description: "Enchantements de haut niveau"
      requirements:
        logic: "AND"
        placeholder:
          placeholder: "%player_level%"
          operator: "greater_than"
          value: "30"
  
  # Alchimie
  BREW:
    healing_potion:
      target: "POTION_HEALING"
      xp: 10.0
      name: "Préparation de potion de soin"
      description: "Brasser des potions curatives"
    
    strength_potion:
      target: "POTION_STRENGTH"
      xp: 15.0
      name: "Préparation de potion de force"
      description: "Brasser des potions de combat"
  
  # Fusion
  SMELT:
    iron_ingot:
      target: "IRON_INGOT"
      xp: 3.0
      name: "Fusion de fer"
      description: "Transformer le minerai en lingot"
    
    gold_ingot:
      target: "GOLD_INGOT"
      xp: 5.0
      name: "Fusion d'or"
      description: "Raffiner l'or précieux"
```

## 🎯 Conseils de configuration

### Équilibrage des courbes XP
```yaml
# Progression lente et régulière
xp-equation: "100 * Math.pow(level, 1.2)"

# Progression rapide au début, plus lente ensuite
xp-equation: "50 * Math.pow(level, 1.8) + level * 25"

# Progression très difficile pour métiers de prestige
xp-equation: "200 * Math.pow(level, 2.5)"
```

### Conditions complexes
```yaml
requirements:
  logic: "AND"
  # Doit avoir la permission ET être dans le bon monde
  permission:
    permission: "vip.mining"
  world:
    worlds: ["mining_world"]
  # ET avoir un certain niveau OU un objet spécial
  groups:
    group1:
      logic: "OR"
      placeholder:
        placeholder: "%jobsadventure_miner_player_level%"
        operator: "greater_than"
        value: "50"
      item:
        mmoitems:
          type: "TOOL"
          id: "MASTER_PICKAXE"
```

### Messages dynamiques
```yaml
# Messages différents selon le niveau
message:
  type: "BOSSBAR"
  color: "GREEN"
  duration: 60
  # Utilisation de placeholders
  message: "&a+{exp} XP {job} &7| Total: %jobsadventure_{job}_player_xp%"
```

## 📁 Organisation des fichiers

```
/plugins/JobsAdventure/
├── jobs/
│   ├── miner.yml
│   ├── farmer.yml
│   ├── hunter.yml
│   ├── crafter.yml
│   └── custom_job.yml
├── rewards/
│   ├── miner_rewards.yml
│   ├── farmer_rewards.yml
│   └── hunter_rewards.yml
├── gui/
│   ├── miner_gui.yml
│   └── farmer_gui.yml
└── xp-curves/
    ├── linear.yml
    ├── steep.yml
    └── custom.yml
```

## 🔗 Voir aussi

- [Créer un nouveau métier](creating-jobs.md)
- [Système de conditions](conditions-system.md)
- [Courbes d'expérience](xp-curves.md)
- [Configuration des récompenses](../rewards/reward-configuration.md)
- [Intégrations de plugins](../integrations/placeholderapi.md)