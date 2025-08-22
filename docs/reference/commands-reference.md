# 📋 Référence Complète des Commands

JobsAdventure fournit un ensemble complete de Commands pour les players et les administrateurs. Toutes the commands sont regroupées sous the command principale `/jobs`.

## 🎮 Commands players

### `/jobs` - Aide Principale
**Permission** : `jobsadventure.command.jobs` (par défaut : `true`)  
**Usage** : `/jobs`

Affiche l'aide principale avec la liste de toutes the commands disponibles.

```
/jobs help - Affiche cette aide
/jobs list - Affiche tous les jobs disponibles  
/jobs join <job> - Rejoindre a job
/jobs leave <job> - Quitter a job
/jobs info [job] - Informations sur a job
/jobs stats [Player] - Statistics de jobs
/jobs rewards - Afficher et réclamer des récompenses
```

### `/jobs list` - Liste des Jobs
**Permission** : `jobsadventure.command.list` (par défaut : `true`)  
**Usage** : `/jobs list`

Affiche tous les jobs disponibles sur the server avec leurs informations de base.

**Affichage** :
```
=== Jobs Disponibles ===
🛠️ Miner - Extraction de minerais
   Level requis: Aucun
   Permission: jobsadventure.job.miner
   players actifs: 45

🌾 Farmer - Agriculture et élevage  
   Level requis: Aucun
   Permission: jobsadventure.job.farmer
   players actifs: 32

🏹 Hunter - Chasse et combat
   Level requis: Level 10 en Farmer
   Permission: jobsadventure.job.hunter  
   players actifs: 28
```

### `/jobs join <job>` - Rejoindre un Job
**Permission** : `jobsadventure.command.join` (par défaut : `true`)  
**Permission du job** : `jobsadventure.job.<jobid>` (ex: `jobsadventure.job.miner`)  
**Usage** : `/jobs join <nom_du_job>`

Permet à un Player de rejoindre a job spécifique.

**Examples** :
```bash
/jobs join miner    # Rejoindre the job de mineur
/jobs join farmer   # Rejoindre the job de fermier
/jobs join hunter   # Rejoindre the job de chasseur
```

**Vérifications effectuées** :
- ✅ The job existe et est activé
- ✅ The player a la permission pour ce job
- ✅ The player ne possède pas déjà ce job
- ✅ The player n'a pas atteint la limite de jobs simultanés
- ✅ Les prérequis du job sont remplis

### `/jobs leave <job>` - Quitter un Job
**Permission** : `jobsadventure.command.leave` (par défaut : `true`)  
**Usage** : `/jobs leave <nom_du_job>`

Permet à un Player de quitter a job.

**Examples** :
```bash
/jobs leave miner   # Quitter the job de mineur
/jobs leave farmer  # Quitter the job de fermier
```

### `/jobs info [job]` - Informations sur un Job
**Permission** : `jobsadventure.command.info` (par défaut : `true`)  
**Usage** : `/jobs info [nom_du_job]`

Affiche des informations detailed sur a job spécifique ou sur tous les jobs of the player.

### `/jobs stats [Player]` - Statistics
**Permission** : `jobsadventure.command.stats` (par défaut : `true`)  
**Permission autres** : `jobsadventure.command.stats.others` (admins uniquement)  
**Usage** : `/jobs stats [nom_Player]`

Affiche the statistics detailed d'un Player.

### `/jobs rewards` - Système de Récompenses
**Permission** : `jobsadventure.command.rewards` (par défaut : `true`)  
**Usage** : `/jobs rewards`

Ouvre l'interface graphique des récompenses ou affiche the rewards disponibles.

## 🛡️ Commands Admin

### `/jobs admin` - Menu Admin Principal
**Permission** : `jobsadventure.admin` (par défaut : `op`)  
**Usage** : `/jobs admin`

Affiche le menu principal d'administration avec toutes les options disponibles.

### `/jobs admin reload` - Rechargement
**Permission** : `jobsadventure.admin.reload` (par défaut : `op`)  
**Usage** : `/jobs admin reload [config|jobs|rewards|all]`

Recharge les différents composants of the plugin sans redémarrage.

**Options** :
```bash
/jobs admin reload config    # Recharge config.yml uniquement
/jobs admin reload jobs      # Recharge tous les jobs
/jobs admin reload rewards   # Recharge the rewards
/jobs admin reload all       # Recharge tout (par défaut)
```

### `/jobs admin player <Player>` - Gestion Player
**Permission** : `jobsadventure.admin.player` (par défaut : `op`)  
**Usage** : `/jobs admin player <Player> <action> [paramètres]`

Gestion complète des données d'un Player.

**Actions disponibles** :
```bash
# Gestion of jobs
/jobs admin player Steve join miner      # Forcer rejoindre a job
/jobs admin player Steve leave miner     # Forcer quitter a job
/jobs admin player Steve reset miner     # Reset Level/XP d'a job
/jobs admin player Steve reset all       # Reset tous les jobs

# Gestion XP/Levelx
/jobs admin player Steve addxp miner 1000    # Ajouter 1000 XP
/jobs admin player Steve setxp miner 50000   # Définir XP exact
/jobs admin player Steve setlevel miner 50   # Définir Level exact
```

### `/jobs admin bonus` - Gestion Bonus XP
**Permission** : `jobsadventure.admin.bonus` (par défaut : `op`)  
**Usage** : `/jobs admin bonus <action> [paramètres]`

Gestion des bonus d'XP temporaires et permanents.

**Gestion globale** :
```bash
# Bonus Server global
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
/jobs admin debug cache stats          # Statistics du cache
/jobs admin debug cache clear          # Vider le cache

# Base de données
/jobs admin debug database test        # Tester connexion DB
/jobs admin debug database optimize    # Optimiser la base
```

## 🔑 Système de Permissions

### Permissions de Base
```yaml
permissions:
  # Commands players
  jobsadventure.command.jobs: true          # Command /jobs principale
  jobsadventure.command.list: true          # Lister les jobs
  jobsadventure.command.join: true          # Rejoindre a job
  jobsadventure.command.leave: true         # Quitter a job
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
  jobsadventure.admin.stats: op             # Stats Server
  
  # Gestion avancée
  jobsadventure.admin.player: op            # Gestion players
  jobsadventure.admin.job: op               # Gestion jobs
  jobsadventure.admin.bonus: op             # Gestion bonus XP
  jobsadventure.admin.debug: op             # Outils debug
  jobsadventure.admin.cleanup: op           # Nettoyage système
```

## 📝 Alias et Raccourcis

### Alias de Commands
```bash
# Alias disponibles pour /jobs
/j          # Raccourci pour /jobs
/job        # Alias alternatif

# Alias de sous-Commands  
/jobs j     # Raccourci pour /jobs join
/jobs l     # Raccourci pour /jobs leave
/jobs i     # Raccourci pour /jobs info
/jobs s     # Raccourci pour /jobs stats
/jobs r     # Raccourci pour /jobs rewards
```

## 📋 Examples d'Usage

### Scénario Player Débutant
```bash
/jobs list                    # Voir les jobs disponibles
/jobs join miner             # Rejoindre the job mineur
/jobs info miner             # Comprendre the job
# [Miner quelques blocs]
/jobs stats                  # Vérifier les progrès
/jobs rewards                # Voir the rewards disponibles
```

### Scénario Administrateur
```bash
/jobs admin stats            # Vérifier le statut du Server
/jobs admin bonus global set 2.0 1h    # Event XP double pendant 1h
/jobs admin player Steve setlevel miner 25  # Ajuster un Level
/jobs admin reload           # Recharger après changements config
```

## ⚠️ Notes Importantes

1. **Permissions** : Toutes the commands nécessitent les permissions appropriées
2. **Paramètres** : Les paramètres entre `<>` sont obligatoires, ceux entre `[]` sont optionnels
3. **Auto-complétion** : La plupart des Commands supportent l'auto-complétion
4. **Sensibilité à la casse** : Les noms de jobs et players sont sensibles à la casse

## 🔗 Voir Aussi

- [Guide of the player](../player-guide/getting-started.md)
- [Guide de l'Administrateur](../admin-guide/admin-commands.md)
- [Système de Permissions](permissions.md)
- [Dépannage des Commands](../troubleshooting/common-issues.md)
