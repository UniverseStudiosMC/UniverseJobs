# 🔧 API JobsAdventure - Introduction

JobsAdventure fournit une API complète permettant aux développeurs d'intégrer leurs plugins avec le système de jobs. Cette API est conçue pour être simple à utiliser tout en offrant une flexibilité maximale.

## 🎯 Vue d'ensemble de l'API

### Fonctionnalités Principales
- **Gestion des Jobs** : Créer, modifier, activer/désactiver des jobs
- **Données Joueurs** : Accès et modification des niveaux, XP, jobs actifs
- **Système d'Événements** : Écoute et réaction aux événements de jobs
- **Actions Personnalisées** : Création de nouveaux types d'actions
- **Conditions Personnalisées** : Création de nouvelles conditions
- **Récompenses** : Gestion des récompenses et de leur attribution

### Compatibilité
- **Paper/Spigot/Bukkit** : Support complet
- **Folia** : Compatibilité native avec threading régionalisé
- **Java 21+** : Optimisé pour les versions récentes
- **Minecraft 1.19+** : Support des versions récentes

## 🚀 Démarrage Rapide

### 1. Ajout de la Dépendance

#### Maven
```xml
<dependency>
    <groupId>fr.ax_dev</groupId>
    <artifactId>JobsAdventure</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

#### Gradle
```gradle
dependencies {
    compileOnly 'fr.ax_dev:JobsAdventure:1.0-SNAPSHOT'
}
```

### 2. Déclaration dans plugin.yml
```yaml
name: MonPlugin
depend: [JobsAdventure]
# ou pour une dépendance optionnelle :
softdepend: [JobsAdventure]
```

### 3. Accès à l'API
```java
public class MonPlugin extends JavaPlugin {
    
    private JobsAdventure jobsAdventure;
    
    @Override
    public void onEnable() {
        // Vérifier que JobsAdventure est présent
        if (!setupJobsAdventure()) {
            getLogger().severe("JobsAdventure n'est pas installé !");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // L'API est maintenant disponible
        JobManager jobManager = jobsAdventure.getJobManager();
        // ... utiliser l'API
    }
    
    private boolean setupJobsAdventure() {
        Plugin plugin = getServer().getPluginManager().getPlugin("JobsAdventure");
        if (plugin == null || !(plugin instanceof JobsAdventure)) {
            return false;
        }
        
        this.jobsAdventure = (JobsAdventure) plugin;
        return true;
    }
}
```

## 📋 Managers Disponibles

### 1. JobManager
**Accès** : `JobsAdventure.getInstance().getJobManager()`

Gestion complète des jobs et données joueurs :
```java
JobManager jobManager = JobsAdventure.getInstance().getJobManager();

// Gestion des jobs
Job job = jobManager.getJob("miner");
Collection<Job> allJobs = jobManager.getAllJobs();
boolean exists = jobManager.hasJob("farmer");

// Données joueurs
PlayerJobData data = jobManager.getPlayerData(player);
boolean hasJob = jobManager.hasJob(player, "miner");
Set<String> playerJobs = jobManager.getPlayerJobs(player);

// XP et niveaux
jobManager.addXp(player, "miner", 100.0);
double xp = jobManager.getXp(player, "miner");
int level = jobManager.getLevel(player, "miner");
```

### 2. ActionProcessor
**Accès** : `JobsAdventure.getInstance().getActionProcessor()`

Traitement des actions personnalisées :
```java
ActionProcessor processor = JobsAdventure.getInstance().getActionProcessor();

// Traitement manuel d'une action
ConditionContext context = new ConditionContext(player, block, "STONE");
boolean shouldCancel = processor.processAction(player, ActionType.BREAK, event, context);
```

### 3. RewardManager
**Accès** : `JobsAdventure.getInstance().getRewardManager()`

Gestion des récompenses :
```java
RewardManager rewardManager = JobsAdventure.getInstance().getRewardManager();

// Vérifier les récompenses disponibles
List<Reward> available = rewardManager.getAvailableRewards(player);

// Forcer l'attribution d'une récompense
rewardManager.giveReward(player, "daily_mining_bonus");
```

### 4. PlaceholderManager
**Accès** : `JobsAdventure.getInstance().getPlaceholderManager()`

Accès aux placeholders :
```java
PlaceholderManager placeholderManager = JobsAdventure.getInstance().getPlaceholderManager();

// Récupérer une valeur de placeholder
String value = placeholderManager.getPlaceholderValue(player, "jobsadventure_miner_player_level");
```

## 🎭 Événements Disponibles

JobsAdventure émet plusieurs événements que votre plugin peut écouter :

### PlayerJobJoinEvent
Déclenché quand un joueur rejoint un job :
```java
@EventHandler
public void onJobJoin(PlayerJobJoinEvent event) {
    Player player = event.getPlayer();
    String jobId = event.getJobId();
    Job job = event.getJob();
    
    // Logique personnalisée
    player.sendMessage("Bienvenue dans le job " + job.getName() + " !");
}
```

### PlayerJobLeaveEvent
Déclenché quand un joueur quitte un job :
```java
@EventHandler
public void onJobLeave(PlayerJobLeaveEvent event) {
    Player player = event.getPlayer();
    String jobId = event.getJobId();
    
    // Logique personnalisée
    player.sendMessage("Vous avez quitté le job " + jobId);
}
```

### PlayerXpGainEvent
Déclenché quand un joueur gagne de l'XP (annulable) :
```java
@EventHandler
public void onXpGain(PlayerXpGainEvent event) {
    Player player = event.getPlayer();
    String jobId = event.getJobId();
    double xp = event.getXp();
    
    // Modifier l'XP gagné
    if (player.hasPermission("vip.double.xp")) {
        event.setXp(xp * 2);
    }
    
    // Ou annuler complètement
    if (someCondition) {
        event.setCancelled(true);
    }
}
```

### PlayerLevelUpEvent
Déclenché lors d'une montée de niveau :
```java
@EventHandler
public void onLevelUp(PlayerLevelUpEvent event) {
    Player player = event.getPlayer();
    String jobId = event.getJobId();
    int oldLevel = event.getOldLevel();
    int newLevel = event.getNewLevel();
    
    // Récompenses personnalisées
    if (newLevel % 10 == 0) { // Tous les 10 niveaux
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, newLevel / 10));
    }
}
```

### PlayerRewardClaimEvent
Déclenché quand un joueur réclame une récompense :
```java
@EventHandler
public void onRewardClaim(PlayerRewardClaimEvent event) {
    Player player = event.getPlayer();
    Reward reward = event.getReward();
    
    // Logique personnalisée
    getLogger().info(player.getName() + " a réclamé la récompense " + reward.getId());
}
```

### JobActionEvent
Déclenché lors de chaque action de job :
```java
@EventHandler
public void onJobAction(JobActionEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    JobAction action = event.getAction();
    double xpGained = event.getXpGained();
    
    // Statistiques personnalisées
    incrementPlayerStats(player, job.getId(), action.getType());
}
```

## 🛠️ Utilisation Avancée

### 1. Création d'Actions Personnalisées

Vous pouvez créer vos propres types d'actions :

```java
public class MonActionListener implements Listener {
    
    private final JobsAdventure jobsAdventure;
    
    public MonActionListener(JobsAdventure jobsAdventure) {
        this.jobsAdventure = jobsAdventure;
    }
    
    @EventHandler
    public void onCustomEvent(MonEvenementPersonnalise event) {
        Player player = event.getPlayer();
        
        // Créer le contexte
        ConditionContext context = new ConditionContext(
            player,
            null, // Pas de bloc pour cet événement
            "custom_action_" + event.getActionType()
        );
        
        // Traiter l'action
        ActionProcessor processor = jobsAdventure.getActionProcessor();
        processor.processAction(player, ActionType.CUSTOM, event, context);
    }
}
```

### 2. Intégration avec PlaceholderAPI

Utiliser les placeholders JobsAdventure dans vos plugins :

```java
public class MonPlaceholderExtension extends PlaceholderExpansion {
    
    @Override
    public String getIdentifier() {
        return "monplugin";
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.equals("total_mining_xp")) {
            JobManager jobManager = JobsAdventure.getInstance().getJobManager();
            double xp = jobManager.getXp(player, "miner");
            return String.format("%.1f", xp);
        }
        return null;
    }
}
```

### 3. Modification des Courbes XP

Accéder et modifier les courbes XP :

```java
public void modifyXpCurve(String jobId) {
    JobManager jobManager = JobsAdventure.getInstance().getJobManager();
    Job job = jobManager.getJob(jobId);
    
    if (job != null && job.getXpCurve() != null) {
        XpCurve curve = job.getXpCurve();
        
        // Récupérer l'XP requis pour un niveau
        double xpForLevel50 = curve.getXpForLevel(50);
        
        // Calculer le niveau pour une quantité d'XP
        int levelForXp = curve.getLevelForXp(125000.0, job.getMaxLevel());
    }
}
```

## 🔒 Bonnes Pratiques

### 1. Gestion des Erreurs
```java
try {
    JobManager jobManager = JobsAdventure.getInstance().getJobManager();
    if (jobManager != null) {
        // Utiliser l'API
    }
} catch (Exception e) {
    getLogger().warning("Erreur lors de l'accès à l'API JobsAdventure: " + e.getMessage());
}
```

### 2. Vérification de Compatibilité
```java
public boolean isJobsAdventureCompatible() {
    Plugin plugin = Bukkit.getPluginManager().getPlugin("JobsAdventure");
    if (plugin == null) return false;
    
    // Vérifier la version
    String version = plugin.getDescription().getVersion();
    return version.startsWith("1.0") || version.startsWith("1.1");
}
```

### 3. Threading et Folia
```java
public void addXpSafely(Player player, String jobId, double xp) {
    JobsAdventure jobs = JobsAdventure.getInstance();
    
    // Utiliser le manager de compatibilité Folia
    jobs.getFoliaManager().runAsync(() -> {
        jobs.getJobManager().addXp(player, jobId, xp);
    });
}
```

## 📚 Exemples Complets

Voir les fichiers suivants pour des exemples détaillés :
- [Événements](events.md) - Liste complète des événements
- [Intégration Personnalisée](custom-integration.md) - Guide d'intégration
- [Actions Personnalisées](custom-actions.md) - Création d'actions
- [Conditions Personnalisées](custom-conditions.md) - Création de conditions
- [Exemples de Code](code-examples.md) - Exemples pratiques

---

L'API JobsAdventure est conçue pour être puissante et flexible tout en restant simple à utiliser. Elle permet une intégration profonde avec le système de jobs pour créer des expériences de jeu uniques.