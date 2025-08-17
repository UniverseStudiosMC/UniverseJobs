# ⭐ Fonctionnalités principales

JobsAdventure offre un éventail complet de fonctionnalités avancées pour créer l'expérience de métiers parfaite sur votre serveur Minecraft.

## 🏢 Système de métiers avancé

### Types d'actions supportés (15+)
JobsAdventure reconnaît et récompense une large variété d'actions :

| Type d'action | Description | Exemples |
|:---|:---|:---|
| **BREAK** | Casser des blocs | Minage, déforestation, excavation |
| **PLACE** | Placer des blocs | Construction, agriculture |
| **KILL** | Tuer des entités | Combat, chasse, nettoyage |
| **FISH** | Pêcher des poissons | Pêche normale et CustomFishing |
| **INTERACT** | Interagir avec des objets | Récolte, artisanat |
| **CRAFT** | Fabriquer des objets | Artisanat, forge |
| **SMELT** | Faire fondre des objets | Métallurgie, cuisine |
| **BREW** | Créer des potions | Alchimie |
| **ENCHANT** | Enchanter des objets | Magie |
| **TAME** | Apprivoiser des animaux | Dressage |
| **SHEAR** | Tondre des animaux | Élevage |
| **MILK** | Traire des animaux | Production laitière |
| **CUSTOM** | Actions personnalisées | Via API |
| **MYTHICMOB** | Créatures MythicMobs | `MYTHICMOB:CreatureName` |
| **PLUGIN** | Intégrations plugins | CustomCrops, Nexo, etc. |

### Courbes d'expérience flexibles
Deux options pour définir la progression :

#### 1. Formules mathématiques personnalisées
```yaml
xp-equation: "100 * Math.pow(level, 1.8)"
```
Fonctions supportées :
- `Math.pow(level, 2)` - Progression exponentielle
- `Math.sqrt(level * 500)` - Progression racine carrée
- `Math.log(level + 1)` - Progression logarithmique
- Combinaisons complexes possibles

#### 2. Fichiers de courbes prédéfinis
```yaml
xp-curve: "steep"  # Utilise /xp-curves/steep.yml
```

### Messages XP personnalisables
Trois modes d'affichage pour les gains d'XP :

#### Chat classique
```yaml
xp-message:
  type: "chat"
  text: "&e+{exp} XP {job} &7(Total: {total_xp})"
```

#### Barre d'action
```yaml
xp-message:
  type: "actionbar"
  text: "&6+{exp} XP &7| &e{job} Level {level}"
  duration: 60  # ticks (3 secondes)
```

#### Barre de boss
```yaml
xp-message:
  type: "bossbar"
  text: "+{exp} EXP - {job}"
  color: "green"
  style: "segmented_10"
  duration: 80
  show-progress: true  # Affiche le progrès du niveau
```

## 🎁 Système de récompenses intelligent

### Types de récompenses multiples

#### Récompenses d'objets
```yaml
items:
  tool:
    material: DIAMOND_PICKAXE
    amount: 1
    display-name: "&bPioche de Maître Mineur"
    enchantments:
      efficiency: 3
      unbreaking: 2
    nbt: '{Custom: 1b}'  # NBT personnalisé
```

#### Récompenses économiques
```yaml
economy-reward: 500.0  # Via Vault
```

#### Récompenses de commandes
```yaml
commands:
  - "give {player} diamond 5"
  - "broadcast {player} a atteint le niveau 50 !"
  - "tellraw {player} {\"text\":\"Félicitations !\", \"color\":\"gold\"}"
```

### Système de conditions avancé
Logique AND/OR complexe pour les prérequis :

```yaml
requirements:
  logic: "AND"
  permission:
    permission: "vip.level1"
    require: true
  placeholder:
    placeholder: "%player_level%"
    operator: "greater_than"
    value: "25"
  item:
    material: "IRON_PICKAXE"
    accept:
      message: "&aRécompense réclamée !"
      sound: "ENTITY_PLAYER_LEVELUP"
    deny:
      message: "&cVous avez besoin d'une pioche en fer !"
      sound: "ENTITY_VILLAGER_NO"
```

### Interface graphique interactive
- **Navigation par pages** pour de nombreuses récompenses
- **Aperçu en temps réel** des prérequis
- **Feedback visuel** pour les récompenses disponibles/réclamées
- **Cooldowns affichés** avec temps restant

### Système de cooldown flexible
```yaml
cooldown-hours: 24        # Cooldown de 24 heures
cooldown-days: 7          # Cooldown de 7 jours
repeatable: true          # Peut être réclamée plusieurs fois
```

## 🔗 Intégrations de plugins

### PlaceholderAPI (60+ placeholders)

#### Informations joueur
```
%jobsadventure_miner_player_level%       # Niveau mining du joueur
%jobsadventure_farmer_player_xp%         # XP farming du joueur
%jobsglobal_player_totaljobs%            # Nombre total de métiers
%jobsglobal_player_totallevels%          # Somme de tous les niveaux
%jobsglobal_player_rank%                 # Rang global du joueur
```

#### Classements
```
%jobsadventure_miner_leaderboard_1_name%    # Nom du top 1 mineur
%jobsadventure_miner_leaderboard_1_level%   # Niveau du top 1 mineur
%jobsglobal_totalxp_1_displayname%          # Top 1 XP global
%jobsglobal_totallevels_5_name%             # 5ème joueur par niveaux
```

### MythicMobs
Intégration complète pour les créatures personnalisées :
```yaml
actions:
  KILL:
    dragon_boss:
      target: "MYTHICMOB:AncientDragon"
      xp: 1000.0
      name: "Dragon Slaying"
      requirements:
        permission:
          permission: "jobs.boss.dragon"
```

### CustomCrops
Support complet de l'agriculture avancée :
```yaml
actions:
  BREAK:
    pineapple_harvest:
      target: "customcrops:pineapple_stage_3"
      xp: 15.0
  PLACE:
    pineapple_plant:
      target: "customcrops:pineapple_seed"
      xp: 2.0
```

### CustomFishing
Pêche personnalisée avec créatures rares :
```yaml
actions:
  FISH:
    legendary_fish:
      target: "customfishing:golden_salmon"
      xp: 50.0
      requirements:
        time:
          min: 13000  # Seulement la nuit
          max: 23000
```

### Nexo & ItemsAdder
Objets et blocs personnalisés :
```yaml
actions:
  BREAK:
    custom_ore:
      target: "nexo:mythril_ore"
      xp: 25.0
  PLACE:
    custom_block:
      target: "itemsadder:decorative_stone"
      xp: 5.0
```

### MMOItems
Outils et armes spécialisés :
```yaml
requirements:
  item:
    mmoitems:
      type: "TOOL"
      id: "MASTER_PICKAXE"
    deny:
      message: "&cVous avez besoin de la Pioche de Maître !"
```

## 🛡️ Système anti-triche avancé

### Protection NBT des blocs
- **Marquage automatique** des blocs placés par les joueurs
- **Distinction intelligente** entre blocs naturels et artificiels
- **Nettoyage automatique** des données NBT obsolètes
- **Performance optimisée** sans impact sur le gameplay

### Détection d'exploits
- **Cooldowns adaptatifs** pour prévenir le spam
- **Détection de patterns** suspects d'activité
- **Validation stricte** de toutes les actions
- **Logs détaillés** pour audit

### Système de validation
```yaml
anti-exploit:
  block-tracking: true
  cooldown-ms: 100        # Cooldown entre actions
  max-actions-per-second: 10
  suspicious-threshold: 50
```

## ⚡ Performance et compatibilité

### Compatibilité Folia
- **Threading régionalisé** pour performance maximale
- **Opérations asynchrones** pour éviter les lags
- **Synchronisation intelligente** entre régions
- **Scaling horizontal** pour gros serveurs

### Optimisations avancées
- **Cache multi-niveaux** pour accès rapides
- **Compression de données** pour économiser l'espace
- **Pool de connexions** pour la base de données
- **Batch operations** pour réduire les I/O

### Monitoring en temps réel
```
Performance Metrics:
⚡ Action Processing:    < 1ms average
💾 Memory Usage:        < 50MB for 1000 players
🔄 Database Queries:    Batched & async
🧵 Thread Safety:       100% concurrent-safe
📈 Scalability:         Tested up to 5000 players
```

## 🎨 Personnalisation complète

### Messages multilingues
Support complet pour :
- **Français** (fr_FR) - Traduction complète
- **Anglais** (en_US) - Langue par défaut
- **Extensible** - Ajout facile de nouvelles langues

### Interface graphique
```yaml
gui:
  title: "&6Métiers de {player}"
  size: 54  # Inventaire 6 lignes
  items:
    job_item:
      material: "DIAMOND_PICKAXE"
      display-name: "&e{job_name}"
      lore:
        - "&7Niveau: &a{level}"
        - "&7XP: &b{current_xp}/{required_xp}"
```

### Sons et effets
```yaml
sounds:
  level-up: "ENTITY_PLAYER_LEVELUP"
  xp-gain: "ENTITY_EXPERIENCE_ORB_PICKUP"
  reward-claim: "ENTITY_VILLAGER_YES"
effects:
  level-up:
    - "FIREWORK"
    - "PARTICLE:FLAME:50"
```

## 🔧 API développeur

### Événements complets
```java
// Événements disponibles
PlayerJobJoinEvent       - Rejoindre un métier
PlayerJobLeaveEvent      - Quitter un métier
PlayerXpGainEvent        - Gain d'XP (cancellable)
PlayerLevelUpEvent       - Montée de niveau
PlayerRewardClaimEvent   - Réclamation de récompense
JobActionEvent           - Action de métier exécutée
```

### Intégration facile
```java
// Exemple d'utilisation de l'API
JobManager jobManager = JobsAdventure.getInstance().getJobManager();
Player player = Bukkit.getPlayer("Steve");

// Ajouter de l'XP
jobManager.addXp(player, "miner", 100.0);

// Vérifier le niveau
int level = jobManager.getLevel(player, "miner");

// Forcer montée de niveau
jobManager.setLevel(player, "miner", 50);
```

### Extensions personnalisées
- **Actions personnalisées** via l'API
- **Conditions personnalisées** pour les récompenses
- **Intégrations tierces** simplifiées
- **Hooks de données** pour synchronisation

## 📊 Système de données

### Stockage hybride
**Fichiers YAML** (par défaut) :
- Installation simple
- Idéal pour petits/moyens serveurs
- Sauvegarde facile

**Base de données MySQL** (optionnel) :
- Performance maximale
- Idéal pour gros serveurs/réseaux
- Synchronisation cross-serveur

### Compression intelligente
- **Compression automatique** des gros fichiers
- **Seuil configurable** pour l'activation
- **Économie d'espace** significative
- **Performance préservée**

### Cache avancé
```yaml
cache:
  max-entries: 1000      # Nombre max d'entrées
  max-memory-mb: 256     # Mémoire maximale
  cleanup-interval: 30   # Nettoyage en minutes
```

## 🔗 Voir aussi

- [Compatibilité et prérequis](compatibility.md)
- [Installation rapide](../installation/quick-start.md)
- [Configuration des métiers](../jobs-configuration/creating-jobs.md)
- [API développeur](../api/introduction.md)