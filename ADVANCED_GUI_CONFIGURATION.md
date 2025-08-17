# 🎨 Configuration Avancée des GUIs - JobsAdventure

## 📋 Vue d'ensemble

Le système de GUI de récompenses permet une personnalisation complète de l'interface, incluant :
- Slots personnalisés pour les récompenses
- Items de décoration et de navigation
- Layouts complètement flexibles
- Remplissage automatique des slots vides
- Navigation multi-pages personnalisée

## ⚙️ Structure de Configuration Complète

```yaml
gui:
  title: "&6&lMon GUI Personnalisé"
  size: 54  # 9, 18, 27, 36, 45, ou 54

  # Slots dédiés aux récompenses
  reward-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25]

  # Remplissage automatique
  fill:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    name: " "
    slots: [0, 1, 2, 3, 4, 5, 6, 7, 8]  # Slots spécifiques, ou vide pour tous

  # Items personnalisés
  items:
    mon_item:
      material: "DIAMOND"
      name: "&bItem Personnalisé"
      lore:
        - "&7Description de l'item"
      slots: [4, 40]  # Placer aux slots 4 et 40
      glowing: true
      custom-model-data: 12345
      enchantments:
        sharpness: 5

  # Navigation personnalisée
  navigation:
    previous-page:
      material: "ARROW"
      name: "&e⬅ Page Précédente"
      slots: [45]
    next-page:
      material: "ARROW"  
      name: "&ePage Suivante ➡"
      slots: [53]
    close:
      material: "BARRIER"
      name: "&cFermer"
      slots: [49]
    refresh:
      material: "CLOCK"
      name: "&aActualiser"
      slots: [47]
    info:
      material: "PAPER"
      name: "&bInformations"
      slots: [51]
```

## 🎯 Configuration des Slots

### Slots de récompenses
```yaml
# Définit exactement où placer les récompenses
reward-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34]

# Exemples de layouts :
# Ligne horizontale : [10, 11, 12, 13, 14, 15, 16]
# Carré 3x3 : [10, 11, 12, 19, 20, 21, 28, 29, 30]
# Layout en L : [9, 18, 27, 28, 29, 30, 31, 32]
```

### Calcul des slots
```
Slot 0  = Rangée 1, Colonne 1
Slot 8  = Rangée 1, Colonne 9
Slot 9  = Rangée 2, Colonne 1
Slot 17 = Rangée 2, Colonne 9

Formule: slot = (rangée - 1) * 9 + (colonne - 1)
```

## 🎨 Items Personnalisés

### Item basique
```yaml
items:
  decoration:
    material: "DIAMOND_SWORD"
    name: "&6Épée Décorative"
    lore:
      - "&7Item purement décoratif"
      - "&7Ne fait rien au clic"
    slots: [4]
```

### Item avec effets visuels
```yaml
items:
  glowing_item:
    material: "EMERALD"
    name: "&aItem Brillant"
    slots: [13]
    glowing: true  # Effet d'enchantement
    enchantments:
      sharpness: 5
      fire_aspect: 2
```

### Item avec action (futur)
```yaml
items:
  action_item:
    material: "CLOCK"
    name: "&eItem d'Action"
    slots: [22]
    action: "refresh"  # Action spéciale (à implémenter)
```

## 🧭 Navigation Personnalisée

### Configuration complète
```yaml
navigation:
  previous-page:
    material: "SPECTRAL_ARROW"
    name: "&e⬅ &6Page Précédente"
    lore:
      - "&7Retour à la page précédente"
      - "&7Raccourci: &eMAJ + Clic"
    slots: [45, 36]  # Multiple slots possibles
    glowing: true

  next-page:
    material: "SPECTRAL_ARROW"
    name: "&6Page Suivante &e➡"
    lore:
      - "&7Aller à la page suivante"
      - "&7Raccourci: &eClic droit"
    slots: [53, 44]

  close:
    material: "RED_CONCRETE"
    name: "&c✖ &4Fermer le GUI"
    lore:
      - "&7Ferme le GUI des récompenses"
      - "&cToutes les modifications sont sauvées"
    slots: [49]
    custom-model-data: 1001

  refresh:
    material: "LIME_CONCRETE"
    name: "&a🔄 &2Actualiser"
    lore:
      - "&7Actualise le statut des récompenses"
      - "&7Vérifie les nouveaux déblocages"
    slots: [47]

  info:
    material: "KNOWLEDGE_BOOK"
    name: "&b📚 &3Guide des Récompenses"
    lore:
      - "&7Explications des statuts :"
      - "&a⬢ Vert = Récupérable"
      - "&c⬢ Rouge = Bloqué"  
      - "&8⬢ Gris = Déjà récupéré"
      - ""
      - "&eInfo supplémentaire ajoutée automatiquement"
    slots: [51]
```

## 🎨 Remplissage et Décoration

### Remplissage automatique
```yaml
fill:
  enabled: true
  material: "BLACK_STAINED_GLASS_PANE"
  name: " "  # Nom vide pour cacher
  # Sans 'slots' = remplit tous les slots vides
```

### Remplissage de slots spécifiques
```yaml
fill:
  enabled: true
  material: "BLUE_STAINED_GLASS_PANE"
  name: "&9═"
  slots: [0, 1, 2, 3, 4, 5, 6, 7, 8]  # Seulement la première rangée
```

### Bordures et motifs
```yaml
# Bordure complète
fill:
  enabled: true
  material: "GRAY_STAINED_GLASS_PANE"
  name: " "
  slots: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53]

# Motif en damier
items:
  pattern_white:
    material: "WHITE_STAINED_GLASS_PANE"
    name: " "
    slots: [1, 3, 5, 7, 10, 12, 14, 16]
  pattern_black:
    material: "BLACK_STAINED_GLASS_PANE"
    name: " "
    slots: [0, 2, 4, 6, 8, 9, 11, 13, 15, 17]
```

## 🎨 Layouts Prédéfinis

### Layout "Vitrine" (centre)
```yaml
reward-slots: [13, 21, 22, 23, 29, 30, 31]
```

### Layout "Galerie" (lignes)
```yaml
reward-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25]
```

### Layout "Cercle"
```yaml
reward-slots: [13, 12, 14, 21, 23, 30, 31, 32]
```

### Layout "Tout l'espace"
```yaml
reward-slots: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35]
navigation:
  previous-page:
    slots: [45]
  next-page:
    slots: [53]
  close:
    slots: [49]
```

## 🔧 Exemples Complets

### GUI Minimaliste
```yaml
gui:
  title: "&7Récompenses"
  size: 27
  reward-slots: [10, 11, 12, 13, 14, 15, 16]
  fill:
    enabled: true
    material: "LIGHT_GRAY_STAINED_GLASS_PANE"
    name: " "
  navigation:
    close:
      material: "BARRIER"
      name: "&cFermer"
      slots: [22]
```

### GUI Élégant
```yaml
gui:
  title: "&6&l✦ &e&lRécompenses Royales &6&l✦"
  size: 54
  reward-slots: [19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34]
  
  fill:
    enabled: true
    material: "YELLOW_STAINED_GLASS_PANE"
    name: " "
    slots: [0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53]
  
  items:
    crown:
      material: "GOLDEN_HELMET"
      name: "&6&l👑 RÉCOMPENSES ROYALES"
      lore:
        - "&7Récupérez vos récompenses"
        - "&7dignes d'un roi !"
      slots: [4]
      glowing: true
    
    royal_guard_left:
      material: "IRON_SWORD"
      name: "&7⚔ Garde Royal"
      slots: [10, 11, 12]
    
    royal_guard_right:
      material: "IRON_SWORD"
      name: "&7⚔ Garde Royal"
      slots: [14, 15, 16]
  
  navigation:
    previous-page:
      material: "GOLDEN_HORSE_ARMOR"
      name: "&6⬅ Page Précédente"
      slots: [37]
    next-page:
      material: "GOLDEN_HORSE_ARMOR"
      name: "&6Page Suivante ➡"
      slots: [43]
    close:
      material: "RED_BED"
      name: "&cQuitter le Château"
      slots: [49]
```

## 🎮 Variables Disponibles

Dans les noms et lores, vous pouvez utiliser :
- `{job}` - Nom du métier
- `{player}` - Nom du joueur
- `{level}` - Niveau actuel (si implémenté)
- `{xp}` - XP actuelle (si implémenté)

## 🚀 Utilisation

1. **Créez votre fichier** de récompenses avec configuration GUI
2. **Liez-le** dans votre métier avec `gui-reward: "nom_fichier"`
3. **Rechargez** avec `/jobs rewards reload`
4. **Testez** avec `/jobs rewards open votre_metier`

Le système détecte automatiquement la configuration et utilise votre layout personnalisé !

## 💡 Tips et Astuces

- **Testez d'abord** avec un GUI simple avant d'ajouter la complexité
- **Utilisez des couleurs cohérentes** pour une meilleure expérience
- **Laissez de l'espace** entre les éléments pour la lisibilité
- **Groupez les récompenses** par niveau ou type
- **Utilisez la symétrie** pour un aspect professionnel
- **Testez sur différentes résolutions** d'écran

Le système est maintenant totalement flexible et permet de créer des interfaces uniques pour chaque métier ! 🎨