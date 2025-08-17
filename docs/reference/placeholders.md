# 📊 Placeholders JobsAdventure

JobsAdventure s'intègre parfaitement avec PlaceholderAPI pour fournir plus de 60 placeholders différents, permettant d'afficher des informations dynamiques sur les jobs, niveaux, classements et statistiques dans votre serveur.

## 🎯 Vue d'ensemble

### Fonctionnalités Principales
- **Informations joueurs** : Niveaux, XP, progression dans chaque job
- **Classements** : Leaderboards par job et globaux
- **Statistiques serveur** : Informations sur l'activité des jobs
- **Cache intelligent** : Optimisation automatique pour les performances
- **Mise à jour temps réel** : Données toujours à jour

### Configuration Requise
- **PlaceholderAPI** : Plugin requis (téléchargeable sur Spigot)
- **JobsAdventure** : Version 1.0+ avec placeholders activés
- **Permissions** : Aucune permission spéciale requise

## 📋 Placeholders par Job

Tous les placeholders suivants utilisent le format : `%jobsadventure_<job>_<type>_<info>%`

### Informations Joueur

#### Niveaux et XP
```
%jobsadventure_miner_player_level%          # Niveau du joueur en mining
%jobsadventure_farmer_player_level%         # Niveau du joueur en farming
%jobsadventure_hunter_player_level%         # Niveau du joueur en hunting

%jobsadventure_miner_player_xp%             # XP total du joueur en mining
%jobsadventure_farmer_player_xp%            # XP total du joueur en farming
%jobsadventure_hunter_player_xp%            # XP total du joueur en hunting
```

#### Progression et Statistiques
```
%jobsadventure_miner_player_xp_current%     # XP actuel dans le niveau
%jobsadventure_miner_player_xp_required%    # XP requis pour niveau suivant
%jobsadventure_miner_player_xp_remaining%   # XP restant pour niveau suivant
%jobsadventure_miner_player_progress%       # Progression en % (0-100)

%jobsadventure_miner_player_rank%           # Rang du joueur dans ce job
%jobsadventure_miner_player_actions%        # Nombre d'actions effectuées
%jobsadventure_miner_player_playtime%       # Temps de jeu dans ce job
```

#### Formatage et Affichage
```
%jobsadventure_miner_player_level_formatted%    # Niveau avec formatage coloré
%jobsadventure_miner_player_xp_formatted%       # XP avec séparateurs (125,430)
%jobsadventure_miner_player_progress_bar%       # Barre de progression ████████░░
%jobsadventure_miner_player_status%             # Statut (Actif/Inactif)
```

### Informations sur les Jobs

#### Statistiques Générales
```
%jobsadventure_miner_job_name%              # Nom affiché du job
%jobsadventure_miner_job_description%       # Description du job
%jobsadventure_miner_job_max_level%         # Niveau maximum
%jobsadventure_miner_job_enabled%           # Statut activé/désactivé

%jobsadventure_miner_job_total_players%     # Nombre total de joueurs
%jobsadventure_miner_job_active_players%    # Joueurs actifs (en ligne)
%jobsadventure_miner_job_actions_today%     # Actions effectuées aujourd'hui
%jobsadventure_miner_job_actions_total%     # Total d'actions depuis création
```

#### Performance et Statistiques
```
%jobsadventure_miner_job_avg_level%         # Niveau moyen des joueurs
%jobsadventure_miner_job_top_level%         # Niveau le plus élevé
%jobsadventure_miner_job_total_xp%          # XP total de tous les joueurs
%jobsadventure_miner_job_popularity%        # Popularité (rang parmi jobs)
```

## 🏆 Placeholders de Classement

### Classements par Job
Format : `%jobsadventure_<job>_leaderboard_<position>_<info>%`

#### Informations des Joueurs au Classement
```
# Top 1 du mining
%jobsadventure_miner_leaderboard_1_name%         # Nom du joueur #1
%jobsadventure_miner_leaderboard_1_displayname%  # Nom d'affichage #1
%jobsadventure_miner_leaderboard_1_level%        # Niveau du joueur #1
%jobsadventure_miner_leaderboard_1_xp%           # XP du joueur #1
%jobsadventure_miner_leaderboard_1_actions%      # Actions du joueur #1

# Top 2 du mining
%jobsadventure_miner_leaderboard_2_name%         # Nom du joueur #2
%jobsadventure_miner_leaderboard_2_level%        # Niveau du joueur #2
# ... et ainsi de suite jusqu'à la position 10
```

#### Formatage du Classement
```
%jobsadventure_miner_leaderboard_1_formatted%    # Format complet: "1. Steve (Lvl 45)"
%jobsadventure_miner_leaderboard_1_xp_formatted% # XP formaté: "125,430 XP"
%jobsadventure_miner_leaderboard_1_badge%        # Badge de rang: 🥇🥈🥉
```

### Classements Globaux
Format : `%jobsglobal_<type>_<position>_<info>%`

#### Classement par Niveau Total
```
%jobsglobal_totallevels_1_name%           # Joueur #1 en niveaux totaux
%jobsglobal_totallevels_1_displayname%    # Nom d'affichage #1
%jobsglobal_totallevels_1_levels%         # Niveaux totaux du #1
%jobsglobal_totallevels_1_jobs%           # Nombre de jobs actifs #1

%jobsglobal_totallevels_2_name%           # Joueur #2 en niveaux totaux
# ... jusqu'à la position 10
```

#### Classement par XP Total
```
%jobsglobal_totalxp_1_name%               # Joueur #1 en XP total
%jobsglobal_totalxp_1_displayname%        # Nom d'affichage #1  
%jobsglobal_totalxp_1_xp%                 # XP total du #1
%jobsglobal_totalxp_1_xp_formatted%       # XP formaté du #1

%jobsglobal_totalxp_2_name%               # Joueur #2 en XP total
# ... jusqu'à la position 10
```

#### Classement par Nombre de Jobs
```
%jobsglobal_totaljobs_1_name%             # Joueur #1 en nombre de jobs
%jobsglobal_totaljobs_1_jobs%             # Nombre de jobs du #1
%jobsglobal_totaljobs_1_jobs_list%        # Liste des jobs du #1
```

## 🌐 Placeholders Globaux du Joueur

Format : `%jobsglobal_player_<stat>%`

### Statistiques Générales
```
%jobsglobal_player_totaljobs%             # Nombre total de jobs du joueur
%jobsglobal_player_totallevels%           # Somme de tous les niveaux
%jobsglobal_player_totalxp%               # Somme de toute l'XP
%jobsglobal_player_totalxp_formatted%     # XP total formaté

%jobsglobal_player_rank%                  # Rang global du joueur
%jobsglobal_player_rank_levels%           # Rang par niveaux totaux
%jobsglobal_player_rank_xp%               # Rang par XP total
%jobsglobal_player_rank_jobs%             # Rang par nombre de jobs
```

### Activité et Performance
```
%jobsglobal_player_actions_total%         # Actions totales tous jobs
%jobsglobal_player_actions_today%         # Actions aujourd'hui
%jobsglobal_player_playtime%              # Temps de jeu total
%jobsglobal_player_first_join%            # Date de première connexion

%jobsglobal_player_most_active_job%       # Job le plus actif
%jobsglobal_player_highest_level_job%     # Job avec niveau le plus élevé
%jobsglobal_player_favorite_job%          # Job préféré (le plus utilisé)
```

### Progression et Objectifs
```
%jobsglobal_player_avg_level%             # Niveau moyen tous jobs
%jobsglobal_player_completion%            # Pourcentage de completion global
%jobsglobal_player_next_milestone%        # Prochain objectif important
%jobsglobal_player_achievements%          # Nombre d'achievements débloqués
```

## 📊 Placeholders de Serveur

Format : `%jobsadventure_server_<stat>%`

### Statistiques Générales
```
%jobsadventure_server_total_players%      # Nombre total de joueurs enregistrés
%jobsadventure_server_active_players%     # Joueurs actifs dans les jobs
%jobsadventure_server_total_jobs%         # Nombre de jobs configurés
%jobsadventure_server_enabled_jobs%       # Nombre de jobs activés

%jobsadventure_server_actions_today%      # Actions totales aujourd'hui
%jobsadventure_server_actions_total%      # Actions totales depuis création
%jobsadventure_server_uptime%             # Temps depuis démarrage plugin
```

### Performance et Activité
```
%jobsadventure_server_performance%        # Performance moyenne (ms)
%jobsadventure_server_cache_hit_rate%     # Taux de succès du cache
%jobsadventure_server_memory_usage%       # Utilisation mémoire (MB)
%jobsadventure_server_database_status%    # Statut de la base de données

%jobsadventure_server_most_popular_job%   # Job le plus populaire
%jobsadventure_server_peak_players%       # Pic de joueurs simultanés
%jobsadventure_server_version%            # Version du plugin
```

## 🎨 Placeholders de Formatage

### Barres de Progression
```
%jobsadventure_miner_player_progress_bar_10%     # Barre 10 caractères ████████░░
%jobsadventure_miner_player_progress_bar_20%     # Barre 20 caractères
%jobsadventure_miner_player_progress_bar_custom% # Barre personnalisable
```

### Couleurs et Styles
```
%jobsadventure_miner_player_level_colored%       # Niveau avec couleur selon rang
%jobsadventure_miner_player_status_badge%        # Badge de statut ✅❌⏸️
%jobsadventure_miner_job_icon%                   # Icône du job 🛠️🌾🏹
```

### Formatage Numérique
```
%jobsadventure_miner_player_xp_compact%          # XP en format compact (125k)
%jobsadventure_miner_player_xp_percentage%       # XP en pourcentage du max
%jobsadventure_miner_player_level_roman%         # Niveau en chiffres romains
```

## ⚙️ Configuration et Utilisation

### Exemples d'Utilisation dans Plugins

#### Scoreboard (avec ScoreboardManager)
```yaml
# scoreboard.yml
title: "&6&lJobsAdventure"
lines:
  - "&7Niveau Mining: &e%jobsadventure_miner_player_level%"
  - "&7XP: &a%jobsadventure_miner_player_xp_formatted%"
  - "&7Progression: %jobsadventure_miner_player_progress_bar%"
  - ""
  - "&7Rang: &6#%jobsadventure_miner_player_rank%"
  - "&7Top 1: &b%jobsadventure_miner_leaderboard_1_name%"
```

#### Chat (avec ChatManager)
```yaml
# chat-format.yml
format: "&7[&e%jobsglobal_player_totallevels%&7] %player_displayname%: %message%"
join-message: "&e%player_name% &7rejoint le serveur (Rang: &6#%jobsglobal_player_rank%&7)"
```

#### TAB List (avec TAB Plugin)
```yaml
# tab.yml
header:
  - "&6&lJobsAdventure Server"
  - "&7Total joueurs: &e%jobsadventure_server_total_players%"

footer:
  - "&7Votre rang global: &6#%jobsglobal_player_rank%"
  - "&7Niveaux totaux: &e%jobsglobal_player_totallevels%"
```

#### Hologrammes (avec HolographicDisplays)
```yaml
# Hologramme classement
lines:
  - "&6&l🏆 TOP MINERS 🏆"
  - "&e1. %jobsadventure_miner_leaderboard_1_formatted%"
  - "&e2. %jobsadventure_miner_leaderboard_2_formatted%"
  - "&e3. %jobsadventure_miner_leaderboard_3_formatted%"
  - ""
  - "&7Mise à jour automatique"
```

### Placeholders Conditionnels

Certains placeholders retournent des valeurs spéciales :

#### Valeurs par Défaut
```
- Joueur inexistant: "Unknown"
- Position vide: "None"
- Job inactif: "Inactive" 
- Erreur: "Error"
- Pas de données: "N/A"
```

#### Gestion des Erreurs
```
- Job invalide: "Invalid Job"
- Position invalide: "Invalid Position"
- Permission insuffisante: "No Permission"
- Plugin désactivé: "Disabled"
```

## 🔧 Cache et Performance

### Système de Cache Intelligent
- **Cache automatique** : 30 secondes pour les classements
- **Mise à jour temps réel** : Données joueur instantanées
- **Optimisation mémoire** : Nettoyage automatique des données inutilisées
- **Performance** : < 1ms pour la plupart des placeholders

### Commandes de Gestion du Cache
```bash
/jobs admin placeholder cache clear         # Vider tout le cache
/jobs admin placeholder cache clear miner   # Vider cache pour un job
/jobs admin placeholder cache stats         # Statistiques du cache
```

## 🎯 Cas d'Usage Avancés

### Système de Récompenses Automatiques
```yaml
# Avec ConditionalCommands
conditions:
  - placeholder: "%jobsadventure_miner_player_level%"
    value: "50"
    commands:
      - "give %player% diamond_pickaxe{Enchantments:[{id:efficiency,lvl:5}]}"
      - "broadcast &6%player% a atteint le niveau 50 en Mining!"
```

### Interface Web Dynamique
```php
<?php
// Via PlaceholderAPI Web Hook
$top_miners = [
    get_placeholder("jobsadventure_miner_leaderboard_1_name"),
    get_placeholder("jobsadventure_miner_leaderboard_2_name"),
    get_placeholder("jobsadventure_miner_leaderboard_3_name")
];
?>
```

### Système de Rangs Automatique
```yaml
# Avec LuckPerms
tracks:
  mining:
    - novice: "level >= 1"
    - apprentice: "level >= 10" 
    - expert: "level >= 25"
    - master: "level >= 50"
    - grandmaster: "level >= 100"
```

## 🔗 Voir Aussi

- [Configuration PlaceholderAPI](../integrations/placeholderapi.md)
- [Intégration avec Autres Plugins](../integrations/)
- [Guide des Classements](leaderboards.md)
- [API pour Développeurs](../api/introduction.md)

---

Les placeholders JobsAdventure offrent une intégration profonde avec l'écosystème PlaceholderAPI, permettant de créer des expériences utilisateur riches et dynamiques sur votre serveur Minecraft.