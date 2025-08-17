# 🎭 Événements de l'API JobsAdventure

JobsAdventure émet une variété d'événements permettant aux plugins tiers de s'intégrer profondément avec le système de jobs. Tous les événements héritent de la classe Bukkit `Event` et peuvent être écoutés via l'annotation `@EventHandler`.

## 📋 Liste Complète des Événements

### 1. PlayerJobJoinEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Annulable** : ❌ Non

Déclenché lorsqu'un joueur rejoint un job avec succès.

#### Détails
```java
public class PlayerJobJoinEvent extends Event {
    private final Player player;
    private final String jobId;
    private final Job job;
    private final JoinReason reason;
}
```

#### Méthodes Disponibles
- `Player getPlayer()` - Le joueur qui a rejoint le job
- `String getJobId()` - L'ID du job rejoint  
- `Job getJob()` - L'objet Job complet
- `JoinReason getReason()` - La raison du join (COMMAND, API, AUTO)

#### Exemple d'Usage
```java
@EventHandler
public void onPlayerJobJoin(PlayerJobJoinEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Donner un kit de démarrage
    if (event.getReason() == JoinReason.COMMAND) {
        giveStarterKit(player, job.getId());
    }
    
    // Notifier les autres joueurs
    Bukkit.broadcastMessage(
        ChatColor.GREEN + player.getName() + 
        " a rejoint le job " + job.getName() + " !"
    );
    
    // Statistiques personnalisées
    incrementJobJoinStats(job.getId());
}
```

### 2. PlayerJobLeaveEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Annulable** : ❌ Non

Déclenché lorsqu'un joueur quitte un job.

#### Détails
```java
public class PlayerJobLeaveEvent extends Event {
    private final Player player;
    private final String jobId;
    private final Job job;
    private final LeaveReason reason;
    private final int finalLevel;
    private final double finalXp;
}
```

#### Méthodes Disponibles
- `Player getPlayer()` - Le joueur qui a quitté le job
- `String getJobId()` - L'ID du job quitté
- `Job getJob()` - L'objet Job complet
- `LeaveReason getReason()` - Raison du départ (COMMAND, API, FORCED)
- `int getFinalLevel()` - Le niveau final atteint
- `double getFinalXp()` - L'XP total final

#### Exemple d'Usage
```java
@EventHandler
public void onPlayerJobLeave(PlayerJobLeaveEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Sauvegarder les statistiques
    saveJobStatistics(player, job.getId(), event.getFinalLevel(), event.getFinalXp());
    
    // Donner une récompense de départ si niveau élevé
    if (event.getFinalLevel() >= 50) {
        giveCompletionReward(player, job.getId());
    }
    
    // Message personnalisé
    player.sendMessage(ChatColor.YELLOW + 
        "Vous avez quitté " + job.getName() + 
        " au niveau " + event.getFinalLevel() + " !");
}
```

### 3. PlayerXpGainEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Annulable** : ✅ Oui

Déclenché avant qu'un joueur ne gagne de l'XP. Peut être annulé ou modifié.

#### Détails
```java
public class PlayerXpGainEvent extends Cancellable {
    private final Player player;
    private final String jobId;
    private final Job job;
    private double xp;
    private final XpSource source;
    private final JobAction action;
    private boolean cancelled;
}
```

#### Méthodes Disponibles
- `Player getPlayer()` - Le joueur gagnant l'XP
- `String getJobId()` - L'ID du job concerné
- `Job getJob()` - L'objet Job complet
- `double getXp()` - La quantité d'XP à gagner
- `void setXp(double xp)` - Modifier la quantité d'XP
- `XpSource getSource()` - Source de l'XP (ACTION, COMMAND, BONUS)
- `JobAction getAction()` - L'action qui a généré l'XP (peut être null)
- `boolean isCancelled()` / `void setCancelled(boolean)` - Gestion d'annulation

#### Exemple d'Usage
```java
@EventHandler
public void onPlayerXpGain(PlayerXpGainEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Double XP pour les VIP
    if (player.hasPermission("vip.double.xp")) {
        event.setXp(event.getXp() * 2);
    }
    
    // Bonus d'XP selon l'heure
    Calendar cal = Calendar.getInstance();
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    if (hour >= 18 && hour <= 22) { // Happy hour
        event.setXp(event.getXp() * 1.5);
        player.sendMessage(ChatColor.GOLD + "Bonus Happy Hour +50% XP !");
    }
    
    // Annuler XP dans certains mondes
    if (event.getPlayer().getWorld().getName().equals("creative")) {
        event.setCancelled(true);
        return;
    }
    
    // Log pour anti-triche
    if (event.getXp() > 1000) {
        getLogger().warning("Gain XP suspect: " + player.getName() + 
                          " - " + event.getXp() + " XP en " + job.getId());
    }
}
```

### 4. PlayerLevelUpEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Annulable** : ❌ Non

Déclenché lorsqu'un joueur monte de niveau dans un job.

#### Détails
```java
public class PlayerLevelUpEvent extends Event {
    private final Player player;
    private final String jobId;
    private final Job job;
    private final int oldLevel;
    private final int newLevel;
    private final double totalXp;
    private final boolean isMaxLevel;
}
```

#### Méthodes Disponibles
- `Player getPlayer()` - Le joueur qui a monté de niveau
- `String getJobId()` - L'ID du job concerné
- `Job getJob()` - L'objet Job complet
- `int getOldLevel()` - L'ancien niveau
- `int getNewLevel()` - Le nouveau niveau
- `double getTotalXp()` - L'XP total du joueur
- `boolean isMaxLevel()` - True si le joueur a atteint le niveau max

#### Exemple d'Usage
```java
@EventHandler
public void onPlayerLevelUp(PlayerLevelUpEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    int newLevel = event.getNewLevel();
    
    // Récompenses par paliers
    if (newLevel % 10 == 0) {
        // Tous les 10 niveaux
        int diamonds = newLevel / 10;
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, diamonds));
        player.sendMessage(ChatColor.AQUA + "Récompense de palier: " + diamonds + " diamants !");
    }
    
    // Titre spécial au niveau max
    if (event.isMaxLevel()) {
        Bukkit.broadcastMessage(ChatColor.GOLD + "🏆 " + player.getName() + 
                              " a maîtrisé le job " + job.getName() + " ! 🏆");
        
        // Donner un titre spécial
        giveTitle(player, "Master " + job.getName());
    }
    
    // Déverrouiller de nouvelles zones
    unlockAreasForLevel(player, job.getId(), newLevel);
    
    // Statistiques
    updatePlayerRanking(player, job.getId(), newLevel);
}
```

### 5. PlayerRewardClaimEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Annulable** : ✅ Oui

Déclenché lorsqu'un joueur tente de réclamer une récompense.

#### Détails
```java
public class PlayerRewardClaimEvent extends Cancellable {
    private final Player player;
    private final Reward reward;
    private final ClaimReason reason;
    private boolean cancelled;
    private String cancelReason;
}
```

#### Méthodes Disponibles
- `Player getPlayer()` - Le joueur réclamant la récompense
- `Reward getReward()` - La récompense réclamée
- `ClaimReason getReason()` - Raison de la réclamation (GUI, COMMAND, AUTO)
- `boolean isCancelled()` / `void setCancelled(boolean)` - Gestion d'annulation
- `String getCancelReason()` / `void setCancelReason(String)` - Raison d'annulation

#### Exemple d'Usage
```java
@EventHandler
public void onPlayerRewardClaim(PlayerRewardClaimEvent event) {
    Player player = event.getPlayer();
    Reward reward = event.getReward();
    
    // Vérifier les conditions spéciales
    if (reward.getId().startsWith("vip_") && !player.hasPermission("vip.rewards")) {
        event.setCancelled(true);
        event.setCancelReason("Vous devez être VIP pour cette récompense !");
        return;
    }
    
    // Limite de récompenses par jour
    int dailyClaims = getDailyClaimCount(player);
    if (dailyClaims >= 5) {
        event.setCancelled(true);
        event.setCancelReason("Limite quotidienne de récompenses atteinte !");
        return;
    }
    
    // Log pour audit
    getLogger().info(player.getName() + " a réclamé la récompense " + reward.getId());
    
    // Compteur personnalisé
    incrementDailyClaimCount(player);
    
    // Notification spéciale pour les récompenses rares
    if (reward.getRarity() == RewardRarity.LEGENDARY) {
        Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + 
                              " a obtenu une récompense légendaire !");
    }
}
```

### 6. JobActionEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Annulable** : ❌ Non

Déclenché après qu'une action de job ait été traitée avec succès.

#### Détails
```java
public class JobActionEvent extends Event {
    private final Player player;
    private final Job job;
    private final JobAction action;
    private final ActionType actionType;
    private final double xpGained;
    private final ConditionContext context;
    private final boolean leveledUp;
}
```

#### Méthodes Disponibles
- `Player getPlayer()` - Le joueur ayant effectué l'action
- `Job getJob()` - Le job concerné
- `JobAction getAction()` - L'action spécifique effectuée
- `ActionType getActionType()` - Le type d'action (BREAK, PLACE, KILL, etc.)
- `double getXpGained()` - L'XP réellement gagné
- `ConditionContext getContext()` - Le contexte de l'action
- `boolean hasLeveledUp()` - True si le joueur a monté de niveau

#### Exemple d'Usage
```java
@EventHandler
public void onJobAction(JobActionEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Statistiques détaillées
    ActionStats stats = getActionStats(player);
    stats.incrementAction(job.getId(), event.getActionType());
    
    // Achievements personnalisés
    checkCustomAchievements(player, job, event.getActionType());
    
    // Particules visuelles pour certaines actions
    if (event.getActionType() == ActionType.BREAK && event.getXpGained() > 10) {
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 5);
    }
    
    // Integration avec économie
    if (event.hasLeveledUp()) {
        double bonus = event.getJob().getLevel() * 10.0;
        EconomyProvider.deposit(player, bonus);
        player.sendMessage(ChatColor.GREEN + "Bonus économique: $" + bonus);
    }
}
```

## 🔧 Événements Personnalisés

Vous pouvez également créer vos propres événements pour étendre le système :

### Exemple d'Événement Personnalisé
```java
public class CustomJobStreakEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final Job job;
    private final int streakCount;
    private final long streakDuration;
    
    public CustomJobStreakEvent(Player player, Job job, int streakCount, long streakDuration) {
        this.player = player;
        this.job = job;
        this.streakCount = streakCount;
        this.streakDuration = streakDuration;
    }
    
    // Getters...
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
```

### Déclencher l'Événement
```java
// Dans votre logique de streak
if (streakCount >= 10) {
    CustomJobStreakEvent streakEvent = new CustomJobStreakEvent(
        player, job, streakCount, streakDuration
    );
    Bukkit.getPluginManager().callEvent(streakEvent);
}
```

## 📊 Priorités d'Événements

Utilisez les priorités pour contrôler l'ordre d'exécution :

```java
@EventHandler(priority = EventPriority.HIGHEST)
public void onXpGainHighest(PlayerXpGainEvent event) {
    // Exécuté en dernier, peut annuler après toutes les modifications
}

@EventHandler(priority = EventPriority.LOWEST)
public void onXpGainLowest(PlayerXpGainEvent event) {
    // Exécuté en premier, modifications de base
}

@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
public void onXpGainMonitor(PlayerXpGainEvent event) {
    // Logging uniquement, ne pas modifier l'événement
    logXpGain(event.getPlayer(), event.getJob(), event.getXp());
}
```

## 🛡️ Bonnes Pratiques

### 1. Gestion des Erreurs
```java
@EventHandler
public void onPlayerLevelUp(PlayerLevelUpEvent event) {
    try {
        // Votre logique ici
        processLevelUp(event);
    } catch (Exception e) {
        getLogger().log(Level.SEVERE, "Erreur dans le traitement du level up", e);
        // Ne pas laisser l'erreur se propager
    }
}
```

### 2. Performance
```java
@EventHandler
public void onJobAction(JobActionEvent event) {
    // Éviter les opérations coûteuses dans les événements fréquents
    if (event.getActionType() == ActionType.BREAK) {
        // Traitement asynchrone pour les opérations lourdes
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            updateDatabase(event.getPlayer(), event.getJob());
        });
    }
}
```

### 3. Vérifications de Sécurité
```java
@EventHandler
public void onRewardClaim(PlayerRewardClaimEvent event) {
    Player player = event.getPlayer();
    
    // Toujours vérifier que le joueur est en ligne
    if (!player.isOnline()) {
        event.setCancelled(true);
        return;
    }
    
    // Vérifier les permissions
    if (!player.hasPermission("rewards.claim")) {
        event.setCancelled(true);
        event.setCancelReason("Permission insuffisante");
        return;
    }
}
```

## 🔗 Voir Aussi

- [Introduction à l'API](introduction.md) - Concepts de base
- [Intégration Personnalisée](custom-integration.md) - Guide complet d'intégration
- [Exemples de Code](code-examples.md) - Exemples pratiques
- [Actions Personnalisées](custom-actions.md) - Créer des actions
- [Conditions Personnalisées](custom-conditions.md) - Créer des conditions

---

Les événements JobsAdventure offrent des points d'intégration puissants pour créer des expériences de jeu riches et personnalisées. Utilisez-les pour étendre le système de jobs selon les besoins spécifiques de votre serveur.