# 📋 Référence complète des commandes

Cette page contient toutes les commandes disponibles dans JobsAdventure.

## 🎮 Commandes pour les joueurs

### Commandes de base

#### `/jobs` ou `/job`
**Description** : Commande principale du plugin  
**Permission** : `jobsadventure.use` (par défaut)  
**Utilisation** : `/jobs <sous-commande>`

#### `/jobs help`
**Description** : Affiche l'aide des commandes  
**Permission** : `jobsadventure.use`  
**Exemple** :
```
/jobs help
```

### Gestion des métiers

#### `/jobs list`
**Description** : Affiche tous les métiers disponibles  
**Permission** : `jobsadventure.use`  
**Exemple** :
```
/jobs list
```
**Sortie** :
```
=== Métiers Disponibles ===
✓ Miner - Extraction de ressources souterraines
✗ Farmer - Agriculture et élevage
✗ Hunter - Combat et survie
```

#### `/jobs join <métier>`
**Description** : Rejoindre un métier  
**Permission** : `jobsadventure.use` + `jobsadventure.job.<métier>`  
**Exemples** :
```
/jobs join miner
/jobs join farmer
/jobs join hunter
```

#### `/jobs leave <métier>`
**Description** : Quitter un métier  
**Permission** : `jobsadventure.use`  
**Exemples** :
```
/jobs leave miner
/jobs leave farmer
```

### Informations et statistiques

#### `/jobs info [métier|joueur]`
**Description** : Affiche des informations détaillées  
**Permission** : `jobsadventure.use`  
**Exemples** :
```
/jobs info miner          # Info sur le métier Miner
/jobs info PlayerName      # Info sur un joueur
/jobs info                 # Info sur vos métiers
```

#### `/jobs stats [joueur]`
**Description** : Affiche les statistiques des métiers  
**Permission** : `jobsadventure.use`  
**Exemples** :
```
/jobs stats               # Vos statistiques
/jobs stats PlayerName    # Stats d'un autre joueur
```

#### `/jobs top <métier>`
**Description** : Affiche le classement d'un métier  
**Permission** : `jobsadventure.use`  
**Exemples** :
```
/jobs top miner
/jobs top farmer
```

### Système de récompenses

#### `/jobs rewards`
**Description** : Commandes du système de récompenses  
**Permission** : `jobsadventure.rewards.use`  
**Sous-commandes** :

##### `/jobs rewards list`
**Description** : Liste les métiers avec des récompenses
```
/jobs rewards list
```

##### `/jobs rewards open <métier>`
**Description** : Ouvre l'interface des récompenses d'un métier
```
/jobs rewards open miner
/jobs rewards open farmer
```

##### `/jobs rewards claim <métier> <récompense>`
**Description** : Réclame une récompense spécifique
```
/jobs rewards claim miner starter_bonus
/jobs rewards claim farmer daily_bonus
```

##### `/jobs rewards info <métier> <récompense>`
**Description** : Affiche les détails d'une récompense
```
/jobs rewards info miner tool_upgrade
```

## ⚔️ Commandes d'administration

### Gestion générale

#### `/jobs reload`
**Description** : Recharge toutes les configurations  
**Permission** : `jobsadventure.admin`  
**Exemple** :
```
/jobs reload
```

#### `/jobs debug [on|off]`
**Description** : Active/désactive le mode debug  
**Permission** : `jobsadventure.admin`  
**Exemples** :
```
/jobs debug on
/jobs debug off
```

### Gestion des joueurs

#### `/jobs admin player <joueur> join <métier>`
**Description** : Force un joueur à rejoindre un métier  
**Permission** : `jobsadventure.admin`  
**Exemple** :
```
/jobs admin player Steve join miner
```

#### `/jobs admin player <joueur> leave <métier>`
**Description** : Force un joueur à quitter un métier  
**Permission** : `jobsadventure.admin`  
**Exemple** :
```
/jobs admin player Steve leave miner
```

#### `/jobs admin player <joueur> setlevel <métier> <niveau>`
**Description** : Définit le niveau d'un joueur dans un métier  
**Permission** : `jobsadventure.admin`  
**Exemple** :
```
/jobs admin player Steve setlevel miner 50
```

#### `/jobs admin player <joueur> addxp <métier> <xp>`
**Description** : Ajoute de l'XP à un joueur  
**Permission** : `jobsadventure.admin`  
**Exemple** :
```
/jobs admin player Steve addxp miner 1000
```

#### `/jobs admin player <joueur> reset [métier]`
**Description** : Remet à zéro les données d'un joueur  
**Permission** : `jobsadventure.admin`  
**Exemples** :
```
/jobs admin player Steve reset          # Tous les métiers
/jobs admin player Steve reset miner    # Métier spécifique
```

### Système de bonus XP

#### `/jobs xpbonus <multiplicateur> <durée>`
**Description** : Donne un bonus XP global  
**Permission** : `jobsadventure.admin.xpbonus`  
**Paramètres** :
- `multiplicateur` : 0.1 à 10.0 (ex: 2.0 = +100%)
- `durée` : en secondes (max 86400 = 24h)

**Exemples** :
```
/jobs xpbonus 2.0 3600        # +100% XP pendant 1 heure
/jobs xpbonus 1.5 1800        # +50% XP pendant 30 minutes
```

#### `/jobs xpbonus <joueur> <multiplicateur> <durée>`
**Description** : Donne un bonus XP à un joueur  
**Permission** : `jobsadventure.admin.xpbonus`  
**Exemple** :
```
/jobs xpbonus Steve 3.0 600   # +200% XP pour Steve pendant 10 min
```

#### `/jobs xpbonus <joueur> <métier> <multiplicateur> <durée>`
**Description** : Donne un bonus XP spécifique à un métier  
**Permission** : `jobsadventure.admin.xpbonus`  
**Exemple** :
```
/jobs xpbonus Steve miner 2.5 1200  # +150% XP mining pour Steve pendant 20 min
```

### Gestion des récompenses

#### `/jobs admin rewards give <joueur> <métier> <récompense>`
**Description** : Donne une récompense à un joueur  
**Permission** : `jobsadventure.rewards.admin`  
**Exemple** :
```
/jobs admin rewards give Steve miner starter_bonus
```

#### `/jobs admin rewards reset <joueur> [métier] [récompense]`
**Description** : Remet à zéro les récompenses d'un joueur  
**Permission** : `jobsadventure.rewards.admin`  
**Exemples** :
```
/jobs admin rewards reset Steve                    # Toutes les récompenses
/jobs admin rewards reset Steve miner              # Toutes les récompenses miner
/jobs admin rewards reset Steve miner daily_bonus  # Récompense spécifique
```

### Statistiques et monitoring

#### `/jobs admin stats`
**Description** : Affiche les statistiques du serveur  
**Permission** : `jobsadventure.admin`  
**Sortie** :
```
=== Statistiques JobsAdventure ===
Joueurs actifs: 45
Métiers actifs: 3
Actions traitées (dernière heure): 2,847
Performance moyenne: 0.8ms
Mémoire utilisée: 42MB
```

#### `/jobs admin performance`
**Description** : Affiche les métriques de performance  
**Permission** : `jobsadventure.admin`  

#### `/jobs admin database [save|load|optimize]`
**Description** : Gestion de la base de données  
**Permission** : `jobsadventure.admin`  
**Exemples** :
```
/jobs admin database save      # Sauvegarde forcée
/jobs admin database load      # Rechargement des données
/jobs admin database optimize  # Optimisation de la DB
```

## 🔧 Commandes par alias

JobsAdventure supporte plusieurs alias pour faciliter l'utilisation :

| Commande complète | Alias |
|:---|:---|
| `/jobs` | `/job` |
| `/jobs join` | `/job j` |
| `/jobs leave` | `/job l` |
| `/jobs info` | `/job i` |
| `/jobs stats` | `/job s` |
| `/jobs list` | `/job ls` |
| `/jobs rewards` | `/job r` |

## 📝 Exemples d'utilisation courante

### Scénario joueur débutant
```bash
/jobs list                    # Voir les métiers disponibles
/jobs join miner             # Rejoindre le métier de mineur
/jobs info miner             # Comprendre le métier
# [Miner quelques blocs]
/jobs stats                  # Voir les progrès
/jobs rewards open miner     # Voir les récompenses disponibles
```

### Scénario administrateur
```bash
/jobs admin stats            # Vérifier l'état du serveur
/jobs xpbonus 2.0 3600      # Événement XP double pendant 1h
/jobs admin player Steve setlevel miner 25  # Ajuster un niveau
/jobs reload                 # Recharger après modifications config
```

## ⚠️ Notes importantes

1. **Permissions** : Toutes les commandes nécessitent les permissions appropriées
2. **Paramètres** : Les paramètres entre `<>` sont obligatoires, ceux entre `[]` sont optionnels
3. **Autocomplétion** : La plupart des commandes supportent l'autocomplétion avec Tab
4. **Sensibilité à la casse** : Les noms de métiers et de joueurs sont sensibles à la casse

## 🔗 Voir aussi

- [Liste des permissions](permissions.md)
- [Guide d'administration](../admin-guide/admin-commands.md)
- [Guide du joueur](../player-guide/commands.md)
- [Dépannage des commandes](../troubleshooting/common-issues.md)