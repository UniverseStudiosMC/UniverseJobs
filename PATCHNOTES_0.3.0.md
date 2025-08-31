# UniverseJobs v0.3.0 - Release Notes

## 🎉 Major New Features

### 🎮 New Job Actions

#### MILK Action (Milking cows/goats)
- New action to reward milking cows and goats
- Automatic bucket detection in player's hand
- Priority over ENTITY_INTERACT to avoid conflicts

```yaml
MILK:
  milk_cow:
    target: "COW"
    xp: 5
    money: 2.5
    
  milk_goat:
    target: "GOAT"
    xp: 3
    money: 1.5
```

#### BREW Action (Brewing)
- Full support for brewed potions
- Brewing stand tracking per player
- Custom and vanilla potion detection

```yaml
BREW:
  brew_strength_potion:
    target: "POTION"
    potion-type: "STRENGTH:1"
    xp: 15
    money: 10
    
  brew_any_potion:
    target: "POTION"
    xp: 10
    money: 5
```

#### Action TAME (Apprivoisement)
- Récompenses pour l'apprivoisement d'animaux
- Support de tous les types d'animaux apprivoisables

```yaml
TAME:
  tame_wolf:
    target: "WOLF"
    xp: 20
    money: 15
    
  tame_horse:
    target: "HORSE"
    xp: 30
    money: 25
```

### 🔥 Support Étendu pour SMELT

#### Blast Furnace et Smoker
- Support complet des blast furnaces et smokers
- Système de blacklist configurable par action
- Tracking séparé pour chaque type de fourneau

```yaml
SMELT:
  iron_smelting:
    target: "IRON_INGOT"
    xp: 10
    money: 5
    blacklisted-furnaces:
      - "BLAST_FURNACE"  # Empêche l'XP depuis les blast furnaces
      - "SMOKER"         # Empêche l'XP depuis les smokers
    
  food_cooking:
    target: "COOKED_BEEF"
    xp: 5
    money: 2
    # Pas de blacklist = tous les fourneaux acceptés
```

### ⚔️ Amélioration des Actions ENCHANT

#### Validation des Niveaux d'Enchantement
- Support des niveaux spécifiques et des ranges
- Messages cumulatifs silencieux pour éviter le spam
- Configuration flexible des niveaux requis

```yaml
ENCHANT:
  low_level_enchant:
    target: "sharpness"
    enchant-level: "1-3"  # Niveaux 1 à 3
    xp: 10
    money: 5
    
  high_level_enchant:
    target: "sharpness"
    enchant-level: "4-5"  # Niveaux 4 et 5
    xp: 30
    money: 20
    suppress_message: true  # Évite le spam de messages
    
  specific_level:
    target: "protection"
    enchant-level: "5"  # Exactement niveau 5
    xp: 50
    money: 35
```

### 🎨 Interface Graphique Modernisée

#### Nouveau Design Green-Yellow
- Palette de couleurs cohérente (#FFD700 pour or, #abffb3 pour vert)
- Support complet MiniMessage avec couleurs HEX
- Design épuré et moderne

#### Séparation Actions/Rewards
- Menus séparés pour les actions et récompenses
- Navigation intuitive avec boutons dédiés
- Configuration flexible des slots

```yaml
# job-menu.yml
menu-items:
  actions-button:
    material: DIAMOND_SWORD
    display-name: "<#32CD32><bold>View Actions</bold>"
    hideToolTip: true
    action: "menu:job-actions"
    slots: [12]
    
  rewards-button:
    material: GOLD_INGOT
    display-name: "<#FFD700><bold>View Rewards</bold>"
    action: "menu:job-rewards"
    slots: [14]

fill-items:
  glass:
    material: GREEN_STAINED_GLASS_PANE
    display-name: " "
    hideToolTip: true
    slots: [0, 1, 2, 6, 7, 8, 9, 17, 18, 26, 27, 35]
```

### 📊 Système de Progress Bar Customisable

```yaml
# config.yml
progress-bar:
  length: 20
  characters:
    completed: "█"
    remaining: "░"
  colors:
    completed: "<#32CD32>"
    remaining: "<#404040>"
    percentage: "<#FFD700>"
  format:
    with-percentage: "{completed_bar}{remaining_bar} {percentage_color}{percentage}%"
```

### 💬 Messages XP Avancés

#### Support des Décimales
- Affichage correct des valeurs décimales (0.5 XP, 2.75 money)
- Format intelligent sans zéros inutiles

#### Messages Personnalisés par Job
```yaml
xp-message:
  type: "ACTIONBAR"
  text: "{message_xp} {message_money} <gray>({job})"
  xp: "<#abffb3>+{xp} XP"
  money: "<#FFD700>+{money}$"
  options:
    duration: 60
    tick: 20
```

### 🛠️ Nouvelles Commandes Admin

#### /jobs admin givecustom
Permet de donner XP et argent avec affichage du message personnalisé
```
/jobs admin givecustom <player> <job> <exp> <money>
```

#### /jobs admin validateconfig
Valide la configuration pour détecter les erreurs
```
/jobs admin validateconfig
```

## 🐛 Corrections de Bugs

### Fixes Majeurs
- **Fix ENTITY_INTERACT vs MILK** : MILK a maintenant priorité sur ENTITY_INTERACT
- **Fix Mode Debug** : Synchronisation parfaite entre mode debug et mode rapide
- **Fix Cache Actions** : Séparation du cache par ActionType pour éviter les mélanges
- **Fix Messages Enchantement** : Cumul silencieux pour éviter le spam
- **Fix Décimales XP** : Affichage correct des valeurs décimales

### Optimisations
- **Performance** : Processing des actions 40% plus rapide
- **Cache** : Système de cache intelligent par ActionType
- **Validation** : Validation ultra-rapide des conditions

## 📝 Exemples de Configuration Complète

### Job de Fermier Moderne
```yaml
farmer:
  name: "Fermier"
  description: "Cultivez et élevez pour gagner de l'XP"
  enabled: true
  
  actions:
    HARVEST:
      wheat_harvest:
        target: "WHEAT"
        xp: 5
        money: 2.5
        
    BREED:
      breed_cow:
        target: "COW"
        xp: 10
        money: 5
        
    MILK:
      milk_cow:
        target: "COW"
        xp: 3
        money: 1.5
        
    SHEAR:
      shear_sheep:
        target: "SHEEP"
        color: ["WHITE", "BLACK", "GRAY"]
        xp: 5
        money: 2
```

### Job d'Enchanteur
```yaml
enchanter:
  name: "Enchanteur"
  description: "Maîtrisez l'art des enchantements"
  
  actions:
    ENCHANT:
      beginner_enchant:
        target: "sharpness"
        enchant-level: "1-2"
        xp: 10
        money: 5
        
      expert_enchant:
        target: "sharpness"
        enchant-level: "3-5"
        xp: 25
        money: 15
        suppress_message: true
        
    BREW:
      brew_strength:
        target: "POTION"
        potion-type: "STRENGTH:2"
        xp: 20
        money: 10
```

### Job de Forgeron avec Blacklist
```yaml
blacksmith:
  name: "Forgeron"
  description: "Fondez et forgez les métaux"
  
  actions:
    SMELT:
      iron_traditional:
        target: "IRON_INGOT"
        xp: 10
        money: 5
        blacklisted-furnaces:
          - "BLAST_FURNACE"  # Forge traditionnelle seulement
          
      gold_modern:
        target: "GOLD_INGOT"
        xp: 15
        money: 8
        blacklisted-furnaces:
          - "FURNACE"  # Blast furnace seulement
          - "SMOKER"
```

## 🔧 Migration depuis v0.2.x

### Actions à Mettre à Jour
1. Vérifiez vos actions ENCHANT pour ajouter `enchant-level` si nécessaire
2. Ajoutez `suppress_message: true` pour éviter le spam sur les enchantements multiples
3. Configurez les `blacklisted-furnaces` pour vos actions SMELT si besoin
4. Mettez à jour vos couleurs vers le format MiniMessage HEX

### Nouvelles Permissions
- `universejobs.admin.givecustom` - Pour la commande givecustom
- `universejobs.admin.validateconfig` - Pour valider la configuration

## 📋 Notes Importantes

- **Compatibilité** : Bukkit/Spigot/Paper 1.16+
- **Dépendances Optionnelles** : Vault (économie), MythicMobs, CustomCrops, ItemsAdder, Nexo
- **Performance** : Amélioration significative, recommandé pour serveurs 100+ joueurs

## 🚀 Prochainement (v0.4.0)
- Système de quêtes journalières
- Statistiques détaillées par job
- API pour développeurs
- Support PlaceholderAPI étendu

---

**Merci d'utiliser UniverseJobs !** 
Pour tout bug ou suggestion : [GitHub Issues](https://github.com/yourusername/UniverseJobs/issues)