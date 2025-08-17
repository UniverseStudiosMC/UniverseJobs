# 📋 Référence Complète des Commandes

JobsAdventure fournit un ensemble complet de commandes pour les joueurs et les administrateurs. Toutes les commandes sont regroupées sous la commande principale `/jobs`.

## 🎮 Commandes Joueurs

### `/jobs` - Aide Principale
**Permission** : `jobsadventure.command.jobs` (par défaut : `true`)  
**Usage** : `/jobs`

Affiche l'aide principale avec la liste de toutes les commandes disponibles.

```
/jobs help - Affiche cette aide
/jobs list - Affiche tous les jobs disponibles  
/jobs join <job> - Rejoindre un job
/jobs leave <job> - Quitter un job
/jobs info [job] - Informations sur un job
/jobs stats [joueur] - Statistiques de jobs
/jobs rewards - Afficher et réclamer des récompenses
```

### `/jobs list` - Liste des Jobs
**Permission** : `jobsadventure.command.list` (par défaut : `true`)  
**Usage** : `/jobs list`

Affiche tous les jobs disponibles sur le serveur avec leurs informations de base.

**Affichage** :
```
=== Jobs Disponibles ===
🛠️ Miner - Extraction de minerais
   Niveau requis: Aucun
   Permission: jobsadventure.job.miner
   Joueurs actifs: 45

🌾 Farmer - Agriculture et élevage  
   Niveau requis: Aucun
   Permission: jobsadventure.job.farmer
   Joueurs actifs: 32

🏹 Hunter - Chasse et combat
   Niveau requis: Niveau 10 en Farmer
   Permission: jobsadventure.job.hunter  
   Joueurs actifs: 28
```

### `/jobs join <job>` - Rejoindre un Job
**Permission** : `jobsadventure.command.join` (par défaut : `true`)  
**Permission du job** : `jobsadventure.job.<jobid>` (ex: `jobsadventure.job.miner`)  
**Usage** : `/jobs join <nom_du_job>`

Permet à un joueur de rejoindre un job spécifique.

**Exemples** :
```bash
/jobs join miner    # Rejoindre le job de mineur
/jobs join farmer   # Rejoindre le job de fermier
/jobs join hunter   # Rejoindre le job de chasseur
```

**Vérifications effectuées** :
- ✅ Le job existe et est activé
- ✅ Le joueur a la permission pour ce job
- ✅ Le joueur ne possède pas déjà ce job
- ✅ Le joueur n'a pas atteint la limite de jobs simultanés
- ✅ Les prérequis du job sont remplis

### `/jobs leave <job>` - Quitter un Job
**Permission** : `jobsadventure.command.leave` (par défaut : `true`)  
**Usage** : `/jobs leave <nom_du_job>`

Permet à un joueur de quitter un job.

**Exemples** :
```bash
/jobs leave miner   # Quitter le job de mineur
/jobs leave farmer  # Quitter le job de fermier
```

### `/jobs info [job]` - Informations sur un Job
**Permission** : `jobsadventure.command.info` (par défaut : `true`)  
**Usage** : `/jobs info [nom_du_job]`

Affiche des informations détaillées sur un job spécifique ou sur tous les jobs du joueur.

### `/jobs stats [joueur]` - Statistiques
**Permission** : `jobsadventure.command.stats` (par défaut : `true`)  
**Permission autres** : `jobsadventure.command.stats.others` (admins uniquement)  
**Usage** : `/jobs stats [nom_joueur]`

Affiche les statistiques détaillées d'un joueur.

### `/jobs rewards` - Système de Récompenses
**Permission** : `jobsadventure.command.rewards` (par défaut : `true`)  
**Usage** : `/jobs rewards`

Ouvre l'interface graphique des récompenses ou affiche les récompenses disponibles.

## 🛡️ Commandes Admin

### `/jobs admin` - Menu Admin Principal
**Permission** : `jobsadventure.admin` (par défaut : `op`)  
**Usage** : `/jobs admin`

Affiche le menu principal d'administration avec toutes les options disponibles.

### `/jobs admin reload` - Rechargement
**Permission** : `jobsadventure.admin.reload` (par défaut : `op`)  
**Usage** : `/jobs admin reload [config|jobs|rewards|all]`

Recharge les différents composants du plugin sans redémarrage.

**Options** :
```bash
/jobs admin reload config    # Recharge config.yml uniquement
/jobs admin reload jobs      # Recharge tous les jobs
/jobs admin reload rewards   # Recharge les récompenses
/jobs admin reload all       # Recharge tout (par défaut)
```

### `/jobs admin player <joueur>` - Gestion Joueur
**Permission** : `jobsadventure.admin.player` (par défaut : `op`)  
**Usage** : `/jobs admin player <joueur> <action> [paramètres]`

Gestion complète des données d'un joueur.

**Actions disponibles** :
```bash
# Gestion des jobs
/jobs admin player Steve join miner      # Forcer rejoindre un job
/jobs admin player Steve leave miner     # Forcer quitter un job
/jobs admin player Steve reset miner     # Reset niveau/XP d'un job
/jobs admin player Steve reset all       # Reset tous les jobs

# Gestion XP/Niveaux
/jobs admin player Steve addxp miner 1000    # Ajouter 1000 XP
/jobs admin player Steve setxp miner 50000   # Définir XP exact
/jobs admin player Steve setlevel miner 50   # Définir niveau exact
```

### `/jobs admin bonus` - Gestion Bonus XP
**Permission** : `jobsadventure.admin.bonus` (par défaut : `op`)  
**Usage** : `/jobs admin bonus <action> [paramètres]`

Gestion des bonus d'XP temporaires et permanents.

**Gestion globale** :
```bash
# Bonus serveur global
/jobs admin bonus global set 2.0 1h     # Bonus x2 pendant 1h sur tous les jobs
/jobs admin bonus global remove         # Supprimer bonus global

# Bonus par job
/jobs admin bonus job miner set 1.5 30m   # Bonus x1.5 sur mining pendant 30m
/jobs admin bonus job farmer set 3.0 2h   # Bonus x3 sur farming pendant 2h
```

### `/jobs admin debug` - Outils Debug
**Permission** : `jobsadventure.admin.debug` (par défaut : `op`)  
**Usage** : `/jobs admin debug <tool> [paramètres]`

Outils avancés de débogage et diagnostic.

**Outils disponibles** :
```bash
# Monitoring performance
/jobs admin debug performance start     # Démarrer profiling performance
/jobs admin debug performance stop      # Arrêter et afficher rapport

# Cache et mémoire
/jobs admin debug cache stats          # Statistiques du cache
/jobs admin debug cache clear          # Vider le cache

# Base de données
/jobs admin debug database test        # Tester connexion DB
/jobs admin debug database optimize    # Optimiser la base
```

## 🔑 Système de Permissions

### Permissions de Base
```yaml
permissions:
  # Commandes joueurs
  jobsadventure.command.jobs: true          # Commande /jobs principale
  jobsadventure.command.list: true          # Lister les jobs
  jobsadventure.command.join: true          # Rejoindre un job
  jobsadventure.command.leave: true         # Quitter un job
  jobsadventure.command.info: true          # Info sur les jobs
  jobsadventure.command.stats: true         # Ses propres stats
  jobsadventure.command.rewards: true       # Accès aux récompenses
  
  # Permissions par job
  jobsadventure.job.miner: true             # Accès au job miner
  jobsadventure.job.farmer: true            # Accès au job farmer
  jobsadventure.job.hunter: true            # Accès au job hunter
  
  # Multiplicateurs XP (exclusifs)
  jobsadventure.multiplier.2: false         # Bonus XP x2
  jobsadventure.multiplier.3: false         # Bonus XP x3
  jobsadventure.multiplier.5: false         # Bonus XP x5
```

### Permissions Administrateur
```yaml
  # Administration de base
  jobsadventure.admin: op                   # Accès admin général
  jobsadventure.admin.reload: op            # Rechargement config
  jobsadventure.admin.stats: op             # Stats serveur
  
  # Gestion avancée
  jobsadventure.admin.player: op            # Gestion joueurs
  jobsadventure.admin.job: op               # Gestion jobs
  jobsadventure.admin.bonus: op             # Gestion bonus XP
  jobsadventure.admin.debug: op             # Outils debug
  jobsadventure.admin.cleanup: op           # Nettoyage système
```

## 📝 Alias et Raccourcis

### Alias de Commandes
```bash
# Alias disponibles pour /jobs
/j          # Raccourci pour /jobs
/job        # Alias alternatif

# Alias de sous-commandes  
/jobs j     # Raccourci pour /jobs join
/jobs l     # Raccourci pour /jobs leave
/jobs i     # Raccourci pour /jobs info
/jobs s     # Raccourci pour /jobs stats
/jobs r     # Raccourci pour /jobs rewards
```

## 📋 Exemples d'Usage

### Scénario Joueur Débutant
```bash
/jobs list                    # Voir les jobs disponibles
/jobs join miner             # Rejoindre le job mineur
/jobs info miner             # Comprendre le job
# [Miner quelques blocs]
/jobs stats                  # Vérifier les progrès
/jobs rewards                # Voir les récompenses disponibles
```

### Scénario Administrateur
```bash
/jobs admin stats            # Vérifier le statut du serveur
/jobs admin bonus global set 2.0 1h    # Événement XP double pendant 1h
/jobs admin player Steve setlevel miner 25  # Ajuster un niveau
/jobs admin reload           # Recharger après changements config
```

## ⚠️ Notes Importantes

1. **Permissions** : Toutes les commandes nécessitent les permissions appropriées
2. **Paramètres** : Les paramètres entre `<>` sont obligatoires, ceux entre `[]` sont optionnels
3. **Auto-complétion** : La plupart des commandes supportent l'auto-complétion
4. **Sensibilité à la casse** : Les noms de jobs et joueurs sont sensibles à la casse

## 🔗 Voir Aussi

- [Guide du Joueur](../player-guide/getting-started.md)
- [Guide de l'Administrateur](../admin-guide/admin-commands.md)
- [Système de Permissions](permissions.md)
- [Dépannage des Commandes](../troubleshooting/common-issues.md)