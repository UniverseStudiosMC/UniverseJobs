# 🔧 API JobsAdventure - Introduction

JobsAdventure provides a API complète allowing développeurs d'intégrer leurs Plugins avec the system de jobs. Cette API is designed pour être simple à utiliser while offering flexibility maximum.

## 🎯 Vue d'ensemble of the API

### Fonctionnalités Principales
- **Gestion des Jobs** : Créer, modifier, activer/désactiver of jobs
- **Données players** : Accès et modification des Levelx, XP, jobs actifs
- **Système d'Events** : Écoute et réaction aux events de jobs
- **Actions Custom** : Création de nouveaux types d'actions
- **Conditions Custom** : Création de nouvelthe conditions
- **Récompenses** : Gestion des récompenses et de leur attribution

### Compatibilité
- **Paper/Spigot/Bukkit** : Support complete
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

### 2. Déclaration dans Plugin.yml
```yaml
name: MonPlugin
depend: [JobsAdventure]
# ou pour une dépendance optionnelle :
softdepend: [JobsAdventure]
```

### 3. Accès à the API
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
        // ... utiliser the API
    }
    
    private boolean setupJobsAdventure() {
        Plugin Plugin = getServer().getPluginManager().getPlugin("JobsAdventure");
        if (Plugin == null || !(Plugin instanceof JobsAdventure)) {
            return false;
        }
        
        this.jobsAdventure = (JobsAdventure) Plugin;
        return true;
    }
}
```

## 📋 Managers Disponibles

### 1. JobManager
**Accès** : `JobsAdventure.getInstance().getJobManager()`

Gestion complète of jobs et données players :
```java
JobManager jobManager = JobsAdventure.getInstance().getJobManager();

// Gestion of jobs
Job job = jobManager.getJob("miner");
Collection<Job> allJobs = jobManager.getAllJobs();
boolean exists = jobManager.hasJob("farmer");

// Données players
PlayerJobData data = jobManager.getPlayerData(player);
boolean hasJob = jobManager.hasJob(player, "miner");
Set<String> playerJobs = jobManager.getPlayerJobs(player);

// XP et Levelx
jobManager.addXp(player, "miner", 100.0);
double xp = jobManager.getXp(player, "miner");
int level = jobManager.getLevel(player, "miner");
```

### 2. ActionProcessor
**Accès** : `JobsAdventure.getInstance().getActionProcessor()`

Traitement of actions customs :
```java
ActionProcessor processor = JobsAdventure.getInstance().getActionProcessor();

// Traitement manuel d'an action
ConditionContext context = new ConditionContext(player, block, "STONE");
boolean shouldCancel = processor.processAction(player, ActionType.BREAK, event, context);
```

### 3. RewardManager
**Accès** : `JobsAdventure.getInstance().getRewardManager()`

Gestion des récompenses :
```java
RewardManager rewardManager = JobsAdventure.getInstance().getRewardManager();

// Vérifier the rewards disponibles
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

## 🎭 Events Disponibles

JobsAdventure émet plusieurs events que votre Plugin peut écouter :

### PlayerJobJoinEvent
Déclenché quand un Player rejoint a job :
```java
@EventHandler
public void onJobJoin(PlayerJobJoinEvent event) {
    Player player = event.getPlayer();
    String jobId = event.getJobId();
    Job job = event.getJob();
    
    // Logique custom
    player.sendMessage("Bienvenue dans the job " + job.getName() + " !");
}
```

### PlayerJobLeaveEvent
Déclenché quand un Player quitte a job :
```java
@EventHandler
public void onJobLeave(PlayerJobLeaveEvent event) {
    Player player = event.getPlayer();
    String jobId = event.getJobId();
    
    // Logique custom
    player.sendMessage("Vous avez quitté the job " + jobId);
}
```

### PlayerXpGainEvent
Déclenché quand un Player gagne de l'XP (annulable) :
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
Déclenché lors d'une montée de Level :
```java
@EventHandler
public void onLevelUp(PlayerLevelUpEvent event) {
    Player player = event.getPlayer();
    String jobId = event.getJobId();
    int oldLevel = event.getOldLevel();
    int newLevel = event.getNewLevel();
    
    // Récompenses customs
    if (newLevel % 10 == 0) { // Tous les 10 Levelx
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, newLevel / 10));
    }
}
```

### PlayerRewardClaimEvent
Déclenché quand un Player réclame une récompense :
```java
@EventHandler
public void onRewardClaim(PlayerRewardClaimEvent event) {
    Player player = event.getPlayer();
    Reward reward = event.getReward();
    
    // Logique custom
    getLogger().info(player.getName() + " a réclamé the reward " + reward.getId());
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
    
    // Statistics customs
    incrementPlayerStats(player, job.getId(), action.getType());
}
```

## 🛠️ Usage Avancée

### 1. Création d'Actions Custom

You can create your own types d'actions :

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
            null, // Pas de bloc pour cet event
            "custom_action_" + event.getActionType()
        );
        
        // Traiter the action
        ActionProcessor processor = jobsAdventure.getActionProcessor();
        processor.processAction(player, ActionType.CUSTOM, event, context);
    }
}
```

### 2. Intégration avec PlaceholderAPI

Utiliser les placeholders JobsAdventure dans vos Plugins :

```java
public class MonPlaceholderExtension extends PlaceholderExpansion {
    
    @Override
    public String getIdentifier() {
        return "monPlugin";
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
        
        // Récupérer l'XP requis pour un Level
        double xpForLevel50 = curve.getXpForLevel(50);
        
        // Calculer le Level pour une quantité d'XP
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
        // Utiliser the API
    }
} catch (Exception e) {
    getLogger().warning("Erreur lors de l'accès à the API JobsAdventure: " + e.getMessage());
}
```

### 2. Vérification de Compatibilité
```java
public boolean isJobsAdventureCompatible() {
    Plugin Plugin = Bukkit.getPluginManager().getPlugin("JobsAdventure");
    if (Plugin == null) return false;
    
    // Vérifier la version
    String version = Plugin.getDescription().getVersion();
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

## 📚 Examples Complets

Voir les fichiers suivants pour of examples détaillés :
- [Events](events.md) - Liste complète of events
- [Intégration Custom](custom-integration.md) - Guide d'intégration
- [Actions Custom](custom-actions.md) - Création d'actions
- [Conditions Custom](custom-conditions.md) - Création de conditions
- [Examples de Code](code-examples.md) - Examples pratiques

---

L'API JobsAdventure is designed pour être puissante et flexible tout en restant simple à utiliser. Elle permet une intégration profonde avec the system de jobs to create gaming experiences uniques.
