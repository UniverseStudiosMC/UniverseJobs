# 🎭 JobsAdventure API Events

JobsAdventure emits a variety of events that allow third-party Plugins to integrate deeply with the job system. All events inherit from the Bukkit `Event` class and can be listened to via the `@EventHandler` annotation.

## 📋 Complete Event List

### 1. PlayerJobJoinEvent
**Package**: `fr.ax_dev.jobsAdventure.api.events`
**Cancellable**: ❌ No

Triggered when a player successfully joins a job.

#### Details
```java
public class PlayerJobJoinEvent extends Event {
    private final Player player;
    private final String jobId;
    private final Job job;
    private final JoinReason reason;
}
```

#### Available Methods
- `Player getPlayer()` - The player who joined the job
- `String getJobId()` - The ID of the joined job
- `Job getJob()` - The complete Job object
- `JoinReason getReason()` - The reason for joining (COMMAND, API, AUTO)

#### Usage Example
```java
@EventHandler
public void onPlayerJobJoin(PlayerJobJoinEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Give starter kit
    if (event.getReason() == JoinReason.COMMAND) {
        giveStarterKit(player, job.getId());
    }
    
    // Notify other players
    Bukkit.broadcastMessage(
        ChatColor.GREEN + player.getName() + 
        " joined the " + job.getName() + " job!"
    );
    
    // Custom statistics
    incrementJobJoinStats(job.getId());
}
```

### 2. PlayerJobLeaveEvent
**Package**: `fr.ax_dev.jobsAdventure.api.events`
**Cancellable**: ❌ No

Triggered when a player leaves a job.

#### Details
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

#### Available Methods
- `Player getPlayer()` - The player who left the job
- `String getJobId()` - The ID of the left job
- `Job getJob()` - The complete Job object
- `LeaveReason getReason()` - Reason for leaving (COMMAND, API, FORCED)
- `int getFinalLevel()` - The final level reached
- `double getFinalXp()` - The final total XP

#### Usage Example
```java
@EventHandler
public void onPlayerJobLeave(PlayerJobLeaveEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Save statistics
    saveJobStatistics(player, job.getId(), event.getFinalLevel(), event.getFinalXp());
    
    // Give departure reward if high level
    if (event.getFinalLevel() >= 50) {
        giveCompletionReward(player, job.getId());
    }
    
    // Custom message
    player.sendMessage(ChatColor.YELLOW + 
        "You left " + job.getName() + 
        " at level " + event.getFinalLevel() + " !");
}
```

### 3. PlayerXpGainEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Cancellable** : ✅ Yes

Triggered before a player gains XP. Can be cancelled or modified.

#### Details
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

#### Available Methods
- `Player getPlayer()` - The player gaining XP
- `String getJobId()` - The job ID concerned
- `Job getJob()` - The Job object complete
- `double getXp()` - La quantité d'XP to gain
- `void setXp(double xp)` - Modifier la quantité d'XP
- `XpSource getSource()` - XP source (ACTION, COMMAND, BONUS)
- `JobAction getAction()` - L'action qui a généré l'XP (peut être null)
- `boolean isCancelled()` / `void setCancelled(boolean)` - Cancellation management

#### Usage Example
```java
@EventHandler
public void onPlayerXpGain(PlayerXpGainEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Double XP for VIPs
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
        getLogger().warning("Suspicious XP gain: " + player.getName() + 
                          " - " + event.getXp() + " XP en " + job.getId());
    }
}
```

### 4. PlayerLevelUpEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Cancellable** : ❌ No

Déclenché lorsqu'un Player monte de Level dans a job.

#### Details
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

#### Available Methods
- `Player getPlayer()` - The player who leveled up in Level
- `String getJobId()` - The job ID concerned
- `Job getJob()` - The Job object complete
- `int getOldLevel()` - The old Level
- `int getNewLevel()` - The new level
- `double getTotalXp()` - The XP total of the Player
- `boolean isMaxLevel()` - True if the player has reached the max level

#### Usage Example
```java
@EventHandler
public void onPlayerLevelUp(PlayerLevelUpEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    int newLevel = event.getNewLevel();
    
    // Récompenses par paliers
    if (newLevel % 10 == 0) {
        // Tous les 10 Levelx
        int diamonds = newLevel / 10;
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, diamonds));
        player.sendMessage(ChatColor.AQUA + "Récompense de palier: " + diamonds + " diamonds !");
    }
    
    // Titre spécial at level max
    if (event.isMaxLevel()) {
        Bukkit.broadcastMessage(ChatColor.GOLD + "🏆 " + player.getName() + 
                              " a maîtrisé the job " + job.getName() + " ! 🏆");
        
        // Donner un titre spécial
        giveTitle(player, "Master " + job.getName());
    }
    
    // Déverrouiller de nouvelles zones
    unlockAreasForLevel(player, job.getId(), newLevel);
    
    // Statistics
    updatePlayerRanking(player, job.getId(), newLevel);
}
```

### 5. PlayerRewardClaimEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Cancellable** : ✅ Yes

Déclenché lorsqu'un Player tente de réclamer une récompense.

#### Details
```java
public class PlayerRewardClaimEvent extends Cancellable {
    private final Player player;
    private final Reward reward;
    private final ClaimReason reason;
    private boolean cancelled;
    private String cancelReason;
}
```

#### Available Methods
- `Player getPlayer()` - The player claiming the reward
- `Reward getReward()` - La récompense claimed
- `ClaimReason getReason()` - Raison de la réclamation (GUI, COMMAND, AUTO)
- `boolean isCancelled()` / `void setCancelled(boolean)` - Cancellation management
- `String getCancelReason()` / `void setCancelReason(String)` - Cancellation reason

#### Usage Example
```java
@EventHandler
public void onPlayerRewardClaim(PlayerRewardClaimEvent event) {
    Player player = event.getPlayer();
    Reward reward = event.getReward();
    
    // Vérifier the conditions spéciales
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
    getLogger().info(player.getName() + " a réclamé the reward " + reward.getId());
    
    // Compteur custom
    incrementDailyClaimCount(player);
    
    // Notification spéciale pour the rewards rares
    if (reward.getRarity() == RewardRarity.LEGENDARY) {
        Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + 
                              " a obtenu une récompense légendaire !");
    }
}
```

### 6. JobActionEvent
**Package** : `fr.ax_dev.jobsAdventure.api.events`
**Cancellable** : ❌ No

Déclenché après qu'an action de job ait été traitée avec succès.

#### Details
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

#### Available Methods
- `Player getPlayer()` - The player who performed the action
- `Job getJob()` - The job concerned
- `JobAction getAction()` - L'action specifically performed
- `ActionType getActionType()` - The action type (BREAK, PLACE, KILL, etc.)
- `double getXpGained()` - The XP actually gained
- `ConditionContext getContext()` - The action context
- `boolean hasLeveledUp()` - True si The player a monté de Level

#### Usage Example
```java
@EventHandler
public void onJobAction(JobActionEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    
    // Statistics detailed
    ActionStats stats = getActionStats(player);
    stats.incrementAction(job.getId(), event.getActionType());
    
    // Achievements custom
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

## 🔧 Events Customs

You can also create your own events to extend the system :

### Example d'Event Custom
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

### Déclencher l'Event
```java
// Dans votre logique de streak
if (streakCount >= 10) {
    CustomJobStreakEvent streakEvent = new CustomJobStreakEvent(
        player, job, streakCount, streakDuration
    );
    Bukkit.getPluginManager().callEvent(streakEvent);
}
```

## 📊 Priorités d'Events

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
    // Logging uniquement, ne pas modifier the event
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
    // Avoid operations expensive in the events frequent
    if (event.getActionType() == ActionType.BREAK) {
        // Asynchronous processing for operations heavy
        Bukkit.getScheduler().runTaskAsynchronously(Plugin, () -> {
            updateDatabase(event.getPlayer(), event.getJob());
        });
    }
}
```

### 3. Security Checks
```java
@EventHandler
public void onRewardClaim(PlayerRewardClaimEvent event) {
    Player player = event.getPlayer();
    
    // Always check que The player is online
    if (!player.isOnline()) {
        event.setCancelled(true);
        return;
    }
    
    // Check permissions
    if (!player.hasPermission("rewards.claim")) {
        event.setCancelled(true);
        event.setCancelReason("Permission insuffisante");
        return;
    }
}
```

## 🔗 Voir Aussi

- [Introduction à the API](introduction.md) - Concepts de base
- [Intégration Custom](custom-integration.md) - Guide complete d'intégration
- [Examples de Code](code-examples.md) - Examples pratiques
- [Actions Custom](custom-actions.md) - Créer of actions
- [Conditions Custom](custom-conditions.md) - Créer of conditions

---

Les events JobsAdventure offer points of powerful integration to create gaming experiences riches et customs. Utilisez-les to extend the system de jobs according to the needs specific to your Server.
