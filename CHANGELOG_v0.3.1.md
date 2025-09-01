# 🚀 UniverseJobs v0.3.1 - Changelog

## ✨ **Nouveautés majeures**

### 🎮 **Système GUI pour la gestion des boosts**
- **GUI interactif** pour `/jobs admin boost info` remplaçant l'affichage texte
- **Auto-actualisation** toutes les secondes
- **Clic droit** pour supprimer les boosts directement depuis le GUI
- **Tri automatique** par ordre de lancement des boosts
- **100% configurable** via `menus/boost-manager.yml`

### 🔢 **Système de modes de calcul des bonus**
Trois modes de calcul disponibles dans `config.yml` :

1. **ADDITIVE** : `2.5x + 2.5x = 5.0x` (addition)
2. **MULTIPLICATIVE** : `2.5x × 2.5x = 6.25x` (multiplication - défaut actuel)  
3. **HIGHEST** : `3.0x, 2.5x = 3.0x` (seul le meilleur boost)

### 🏗️ **Architecture InventoryHolder**
- **Sécurité renforcée** contre les exploits d'inventaire
- **Gestion centralisée** des GUIs dans MenuManager
- **Pattern uniforme** avec les autres menus du plugin

---

## 🎯 **Fonctionnalités du GUI Boost Manager**

### 📋 **Interface utilisateur**
- **Experience bottles** pour les boosts XP
- **Gold ingots** pour les boosts Money
- **Items de navigation** : fermer, actualiser, informations
- **Items décoratifs** personnalisables
- **Effets sonores** configurables

### ⚙️ **Configuration complète**
```yaml
# Exemple de configuration dans menus/boost-manager.yml
title: "<gold><b>Boost Manager</b></gold>"
size: 54

xp-boosts:
  slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25]
  item:
    material: EXPERIENCE_BOTTLE
    display-name: "<white>XP Boost <gold>{boost_id}</gold></white>"
    glow: false

money-boosts:
  slots: [28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43]
  item:
    material: GOLD_INGOT
    display-name: "<white>Money Boost <gold>{boost_id}</gold></white>"
```

### 🎨 **Variables disponibles**
- `{boost_id}` : ID unique du boost
- `{player_name}` : Nom du joueur ou "All Players"
- `{job_info}` : Job ciblé ou "All Jobs"
- `{action_info}` : Action ciblée ou "All Actions"
- `{multiplier}` : Multiplicateur (ex: 2.5)
- `{remaining_time}` : Temps restant formaté
- `{xp_count}` : Nombre de boosts XP actifs
- `{money_count}` : Nombre de boosts Money actifs
- `{total_count}` : Total des boosts actifs

---

## 🛠️ **Configuration des modes de calcul**

### 📝 **Dans config.yml**
```yaml
jobs:
  # Boost Calculation Mode
  # ADDITIVE: Multipliers are added together (2.5x + 2.5x = 5.0x)
  # MULTIPLICATIVE: Multipliers are multiplied (2.5x * 2.5x = 6.25x)
  # HIGHEST: Only the highest multiplier is used (3.0x, 2.5x = 3.0x)
  boost-calculation-mode: MULTIPLICATIVE
```

### 💡 **Exemples de calculs**

#### Mode ADDITIVE
```
Boost 1: 1.5x (+0.5)
Boost 2: 2.0x (+1.0)
Boost 3: 1.3x (+0.3)
Total: 1.0 + 0.5 + 1.0 + 0.3 = 2.8x
```

#### Mode MULTIPLICATIVE (défaut)
```
Boost 1: 1.5x
Boost 2: 2.0x
Boost 3: 1.3x
Total: 1.5 × 2.0 × 1.3 = 3.9x
```

#### Mode HIGHEST
```
Boost 1: 1.5x
Boost 2: 2.0x ← Utilisé
Boost 3: 1.3x
Total: 2.0x (le plus élevé)
```

---

## 🎵 **Système de sons configurable**

```yaml
sounds:
  open:
    enabled: true
    sound: ENTITY_EXPERIENCE_ORB_PICKUP
    volume: 0.5
    pitch: 1.0
    
  remove-boost:
    enabled: true
    sound: ENTITY_EXPERIENCE_ORB_PICKUP
    volume: 0.7
    pitch: 1.5
    
  close:
    enabled: true
    sound: UI_BUTTON_CLICK
    volume: 0.5
    pitch: 1.0
```

---

## 🌍 **Système de langues intégré**

### 📁 **Messages dans languages/en_US.yml**
```yaml
# Boost GUI messages
boost-gui:
  removed: "<green>Removed {type} boost: <gold>{boost_id}</gold></green>"
  no-permission: "<red>You don't have permission to do that!</red>"
  refresh-clicked: "<aqua>Refreshing boost list...</aqua>"
```

---

## 🚪 **Exemples d'utilisation**

### 🎮 **Pour les joueurs**
```bash
# Ouvrir le GUI de gestion des boosts (admin)
/jobs admin boost info
```

### 👑 **Pour les administrateurs**

#### Donner des boosts
```bash
# Boost XP global pour tous les joueurs
/jobs admin boost give xp * * * 2.0 3600

# Boost Money spécifique pour un joueur et job
/jobs admin boost give money Player123 miner * 1.5 1800

# Boost XP pour une action spécifique
/jobs admin boost give xp * * BREAK stone 2.5 7200
```

#### Supprimer des boosts
```bash
# Via commande
/jobs admin boost remove xp_boost_001

# Via GUI : Clic droit sur l'item du boost
```

#### Changer le mode de calcul
```yaml
# Dans config.yml
jobs:
  boost-calculation-mode: ADDITIVE  # ou MULTIPLICATIVE ou HIGHEST
```

---

## 🔧 **Personnalisation avancée**

### 🎨 **Items de navigation personnalisés**
```yaml
navigation:
  close:
    enabled: true
    slots: [49]
    material: BARRIER
    display-name: "<red>Close</red>"
    glow: false
    
  refresh:
    enabled: true
    slots: [45]
    material: CLOCK
    display-name: "<aqua>Refresh</aqua>"
    glow: true
    
  info:
    enabled: true
    slots: [53]
    material: BOOK
    display-name: "<yellow>Information</yellow>"
    lore:
      - "<gray>Active XP Boosts: <green>{xp_count}</green>"
      - "<gray>Active Money Boosts: <gold>{money_count}</gold>"
      - "<gray>Total Boosts: <white>{total_count}</white>"
```

### 🎭 **Items décoratifs**
```yaml
custom-items:
  separator-xp:
    enabled: true
    slots: [9, 18, 27, 36]
    material: BLACK_STAINED_GLASS_PANE
    display-name: "<dark_gray>▼ XP Boosts ▼</dark_gray>"
    
  separator-money:
    enabled: true
    slots: [17, 26, 35, 44]
    material: BLACK_STAINED_GLASS_PANE
    display-name: "<dark_gray>▼ Money Boosts ▼</dark_gray>"
```

---

## 🔒 **Permissions**

| Permission | Description |
|------------|-------------|
| `universejobs.admin.boost` | Accès complet au système de boosts (GUI + commandes) |
| `universejobs.admin.boost.*` | Accès à toutes les fonctionnalités admin |

---

## 🐛 **Corrections et améliorations**

### ✅ **Sécurité**
- **Protection anti-exploit** via InventoryHolder
- **Validation des clics** sécurisée
- **Gestion des permissions** unifiée

### 🚀 **Performance**
- **Auto-actualisation optimisée** (configurable)
- **Gestion mémoire améliorée** pour les GUIs
- **Cleanup automatique** des tâches

### 🎯 **UX/UI**
- **Interface intuitive** avec icônes visuelles
- **Feedback sonore** pour chaque action
- **Messages informatifs** contextuels

---

## 📈 **Migration depuis v0.3.0**

### 🔄 **Automatique**
- Les fichiers de configuration sont **automatiquement générés**
- **Aucune perte de données** des boosts existants
- **Compatibilité ascendante** totale

### ⚙️ **Configuration manuelle optionnelle**
1. **Modifier le mode de calcul** dans `config.yml`
2. **Personnaliser le GUI** dans `menus/boost-manager.yml`
3. **Adapter les messages** dans `languages/`

---

## 🎉 **Conclusion**

La version **0.3.1** apporte une **révolution** dans la gestion des boosts avec :

- 🎮 **Interface graphique moderne** et interactive
- 🔢 **Flexibilité de calcul** sans précédent
- 🎨 **Customisation totale** de l'expérience
- 🔒 **Sécurité renforcée** et architecture solide

**Compatible** avec toutes les versions Minecraft **1.17+** et **optimisé** pour les serveurs haute performance !

---

*🤖 Généré avec [Claude Code](https://claude.ai/code) - UniverseJobs v0.3.1*