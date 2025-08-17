# ⚔️ Guide d'administration - Commandes essentielles

Ce guide présente toutes les commandes d'administration pour gérer efficacement JobsAdventure sur votre serveur.

## 🔑 Permissions administrateur

Avant tout, assurez-vous d'avoir les bonnes permissions :

| Permission | Description |
|:---|:---|
| `jobsadventure.admin` | Accès aux commandes d'administration de base |
| `jobsadventure.admin.xpbonus` | Gestion des bonus XP |
| `jobsadventure.rewards.admin` | Administration des récompenses |

## 🔧 Gestion générale du plugin

### Recharger la configuration
```
/jobs reload
```
**Utilisation :** Après avoir modifié des fichiers de configuration
**Effet :** Recharge tous les fichiers sans redémarrer le serveur

### Mode debug
```
/jobs debug on
/jobs debug off
```
**Utilisation :** Pour diagnostiquer des problèmes
**Effet :** Active/désactive les logs détaillés dans la console

### Statistiques du serveur
```
/jobs admin stats
```
**Sortie exemple :**
```
=== Statistiques JobsAdventure ===
Joueurs actifs: 42
Métiers actifs: 3
Actions traitées (dernière heure): 1,847
Performance moyenne: 0.7ms
Mémoire utilisée: 38MB
Cache hits: 94.2%
```

### Métriques de performance
```
/jobs admin performance
```
Affiche des métriques détaillées pour optimiser les performances.

## 👥 Gestion des joueurs

### Forcer un joueur à rejoindre un métier
```
/jobs admin player <joueur> join <métier>
```
**Exemples :**
```
/jobs admin player Steve join miner
/jobs admin player Alice join farmer
```

### Forcer un joueur à quitter un métier
```
/jobs admin player <joueur> leave <métier>
```
**Exemple :**
```
/jobs admin player Steve leave miner
```

### Modifier le niveau d'un joueur
```
/jobs admin player <joueur> setlevel <métier> <niveau>
```
**Exemples :**
```
/jobs admin player Steve setlevel miner 50
/jobs admin player Alice setlevel farmer 25
```

### Ajouter de l'XP à un joueur
```
/jobs admin player <joueur> addxp <métier> <xp>
```
**Exemples :**
```
/jobs admin player Steve addxp miner 1000
/jobs admin player Alice addxp farmer 500
```

### Retirer de l'XP à un joueur
```
/jobs admin player <joueur> removexp <métier> <xp>
```
**Exemple :**
```
/jobs admin player Steve removexp miner 200
```

### Réinitialiser un joueur
```
/jobs admin player <joueur> reset [métier]
```
**Exemples :**
```
/jobs admin player Steve reset           # Tous les métiers
/jobs admin player Steve reset miner     # Métier spécifique
```

## 🚀 Système de bonus XP

### Bonus XP global pour tous les joueurs
```
/jobs xpbonus <multiplicateur> <durée>
```
**Paramètres :**
- `multiplicateur` : 0.1 à 10.0 (ex: 2.0 = +100% XP)
- `durée` : en secondes (max 86400 = 24h)

**Exemples :**
```
/jobs xpbonus 2.0 3600        # Double XP pendant 1 heure
/jobs xpbonus 1.5 7200        # +50% XP pendant 2 heures
/jobs xpbonus 3.0 1800        # Triple XP pendant 30 minutes
```

### Bonus XP pour un joueur spécifique
```
/jobs xpbonus <joueur> <multiplicateur> <durée>
```
**Exemples :**
```
/jobs xpbonus VIP_Player 2.5 3600    # +150% XP pour un VIP
/jobs xpbonus NewPlayer 1.2 86400     # +20% XP pour un débutant (24h)
```

### Bonus XP pour un métier spécifique
```
/jobs xpbonus <joueur> <métier> <multiplicateur> <durée>
```
**Exemples :**
```
/jobs xpbonus Steve miner 3.0 1800    # Triple XP mining pour Steve (30 min)
/jobs xpbonus Alice farmer 2.0 7200   # Double XP farming pour Alice (2h)
```

### Scénarios d'utilisation courants

#### Événement weekend
```
/jobs xpbonus 2.0 172800    # Double XP pendant tout le weekend (48h)
```

#### Bonus de bienvenue
```
/jobs xpbonus NouveauJoueur 1.5 604800    # +50% XP pendant 1 semaine
```

#### Événement métier spécialisé
```
# Semaine du mining - bonus pour tous les mineurs
/jobs xpbonus JoueurA miner 2.0 604800
/jobs xpbonus JoueurB miner 2.0 604800
# etc...
```

## 🎁 Gestion des récompenses

### Donner une récompense à un joueur
```
/jobs admin rewards give <joueur> <métier> <récompense>
```
**Exemples :**
```
/jobs admin rewards give Steve miner starter_bonus
/jobs admin rewards give Alice farmer daily_bonus
```

### Réinitialiser les récompenses
```
/jobs admin rewards reset <joueur> [métier] [récompense]
```
**Exemples :**
```
/jobs admin rewards reset Steve                     # Toutes les récompenses
/jobs admin rewards reset Steve miner               # Toutes les récompenses miner
/jobs admin rewards reset Steve miner daily_bonus   # Récompense spécifique
```

### Forcer l'éligibilité d'une récompense
```
/jobs admin rewards unlock <joueur> <métier> <récompense>
```
Utile pour débloquer des récompenses spéciales lors d'événements.

## 💾 Gestion des données

### Sauvegarde forcée
```
/jobs admin database save
```
Force la sauvegarde de toutes les données joueurs.

### Rechargement des données
```
/jobs admin database load
```
Recharge toutes les données depuis la source (fichiers/DB).

### Optimisation de la base de données
```
/jobs admin database optimize
```
Optimise les performances de la base de données (MySQL uniquement).

### Migration des données
```
/jobs admin migrate file-to-database
/jobs admin migrate database-to-file
```
Migre les données entre fichiers et base de données.

## 📊 Surveillance et monitoring

### Voir les joueurs actifs par métier
```
/jobs admin list players <métier>
```
**Exemple :**
```
/jobs admin list players miner
```

### Statistiques détaillées d'un joueur
```
/jobs admin info <joueur>
```
Affiche toutes les informations administrateur sur un joueur.

### Log des actions récentes
```
/jobs admin log [joueur] [métier] [heures]
```
**Exemples :**
```
/jobs admin log                      # Toutes les actions (dernière heure)
/jobs admin log Steve                # Actions de Steve (dernière heure)
/jobs admin log Steve miner 24       # Actions mining de Steve (24h)
```

### Alertes de performance
```
/jobs admin alerts
```
Affiche les alertes de performance et recommandations d'optimisation.

## 🔧 Maintenance et diagnostic

### Test de performance
```
/jobs admin benchmark
```
Lance un test de performance pour diagnostiquer les problèmes.

### Nettoyage du cache
```
/jobs admin cache clear
/jobs admin cache info
```
Gère le cache interne du plugin.

### Vérification de l'intégrité
```
/jobs admin check integrity
```
Vérifie l'intégrité des données et configurations.

### Export des données
```
/jobs admin export <format> [fichier]
```
**Formats supportés :** `csv`, `json`, `yaml`
**Exemples :**
```
/jobs admin export csv player_stats.csv
/jobs admin export json backup.json
```

## 🎯 Scénarios d'administration courants

### Nouveau serveur - Configuration initiale
```bash
# 1. Vérifier l'installation
/jobs admin stats

# 2. Configurer un événement de lancement
/jobs xpbonus 2.0 604800    # Double XP pendant 1 semaine

# 3. Créer des comptes test
/jobs admin player TestPlayer join miner
/jobs admin player TestPlayer setlevel miner 10
```

### Événement spécial - Weekend double XP
```bash
# Vendredi soir
/jobs xpbonus 2.0 172800
/broadcast &6[Événement] &eDouble XP activé pour le weekend !

# Dimanche soir - vérification
/jobs admin stats    # Voir l'impact de l'événement
```

### Problème de joueur - Réinitialisation
```bash
# Enquête
/jobs admin info ProblematicPlayer
/jobs admin log ProblematicPlayer 48

# Réinitialisation si nécessaire
/jobs admin player ProblematicPlayer reset
/jobs admin rewards reset ProblematicPlayer
```

### Maintenance serveur - Sauvegarde
```bash
# Avant maintenance
/jobs admin database save
/jobs admin export json backup_$(date).json

# Après maintenance
/jobs admin check integrity
/jobs reload
```

## ⚠️ Bonnes pratiques

### Sauvegardes régulières
- Configurez des **sauvegardes automatiques** toutes les heures
- Testez la **restauration** régulièrement
- Gardez des **archives** de plusieurs jours

### Monitoring continu
- Vérifiez `/jobs admin stats` quotidiennement
- Surveillez les **alertes de performance**
- Attention aux **pics d'utilisation** inhabituels

### Gestion des événements
- **Planifiez** les bonus XP à l'avance
- **Communiquez** les événements aux joueurs
- **Surveillez** l'impact sur l'économie

### Sécurité
- **Limitez** les permissions admin
- **Loggez** toutes les actions administrateur
- **Vérifiez** régulièrement les accès

## 🆘 Dépannage rapide

### Performance dégradée
```bash
/jobs admin performance      # Identifier les goulots
/jobs admin cache clear      # Nettoyer le cache
/jobs admin database optimize # Optimiser la DB
```

### Données corrompues
```bash
/jobs admin check integrity  # Diagnostic
/jobs admin database load    # Rechargement
# Si nécessaire : restaurer depuis sauvegarde
```

### Plugin ne répond plus
```bash
/jobs debug on              # Activer les logs
/jobs reload                # Recharger
# Vérifier la console pour les erreurs
```

## 📚 Voir aussi

- [Gestion des joueurs](player-management.md)
- [Système de bonus XP](xp-bonus-system.md)
- [Surveillance et débogage](monitoring-debugging.md)
- [Référence des commandes](../reference/commands-reference.md)
- [Dépannage](../troubleshooting/common-issues.md)