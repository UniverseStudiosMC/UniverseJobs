package fr.ax_dev.universejobs.cache;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.action.JobAction;
import fr.ax_dev.universejobs.action.ActionType;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Cache ultra-rapide de toutes les configurations.
 * Charge TOUT au démarrage pour éviter les lookups répétés.
 */
public class ConfigurationCache {
    
    private final UniverseJobs plugin;
    
    // Configuration flags (les plus utilisées)
    private volatile boolean debugEnabled;
    private volatile boolean showXpGain;
    private volatile int maxCachedPlayers;
    
    // Job action lookups précalculées
    private final Map<ActionType, Map<String, Set<JobAction>>> jobActionsByMaterial = new ConcurrentHashMap<>();
    private final Map<ActionType, Map<String, Set<JobAction>>> jobActionsByEntity = new ConcurrentHashMap<>();
    private final Map<ActionType, Map<String, Set<JobAction>>> actionTypeLookup = new ConcurrentHashMap<>();
    
    // Pattern matching précalculé
    private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> expandedWildcards = new ConcurrentHashMap<>();
    
    // Target validation rapide
    private final Map<String, Boolean> targetValidationCache = new ConcurrentHashMap<>();
    
    // Permission cache patterns
    private final Set<String> validPermissionNodes = ConcurrentHashMap.newKeySet();
    
    // Message templates précalculées
    private final Map<String, String> messageTemplates = new ConcurrentHashMap<>();
    
    public ConfigurationCache(UniverseJobs plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Charge TOUTES les configurations en mémoire au démarrage.
     * Une seule fois = performance maximale après.
     */
    public void loadAllConfigurations() {
        plugin.getLogger().info("Loading configuration cache...");
        long startTime = System.currentTimeMillis();
        
        // 1. Config flags basiques
        loadConfigurationFlags();
        
        // 2. Jobs et actions
        loadJobActionMappings();
        
        // 3. Patterns et wildcards
        loadPatternMappings();
        
        // 4. Permission nodes
        loadPermissionNodes();
        
        // 5. Message templates
        loadMessageTemplates();
        
        long loadTime = System.currentTimeMillis() - startTime;
        plugin.getLogger().info("Configuration cache loaded in " + loadTime + "ms");
        plugin.getLogger().info("Cache size: " + getCacheStats());
    }
    
    /**
     * Cache des flags de configuration les plus utilisées.
     */
    private void loadConfigurationFlags() {
        debugEnabled = plugin.getConfig().getBoolean("debug", false);
        showXpGain = plugin.getConfig().getBoolean("messages.show-xp-gain", true);
        maxCachedPlayers = plugin.getConfig().getInt("performance.max-cached-players", 1000);
    }
    
    /**
     * Précalcule TOUS les mappings job->action par material/entity.
     */
    private void loadJobActionMappings() {
        for (Job job : plugin.getJobManager().getAllJobs()) {
            if (!job.isEnabled()) continue;
            
            for (ActionType actionType : ActionType.values()) {
                List<JobAction> actions = job.getActions(actionType);
                if (actions.isEmpty()) continue;
                
                // Index par type d'action
                actionTypeLookup.computeIfAbsent(actionType, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(job.getId(), k -> new HashSet<>())
                    .addAll(actions);
                
                // Index par target pour lookup ultra-rapide
                for (JobAction action : actions) {
                    String target = action.getTarget();
                    if (target == null) continue;
                    
                    // Material-based actions (BREAK, PLACE, etc.)
                    if (isMaterialAction(actionType)) {
                        Set<String> materials = expandTarget(target, getAllMaterials());
                        for (String material : materials) {
                            jobActionsByMaterial.computeIfAbsent(actionType, k -> new ConcurrentHashMap<>())
                                .computeIfAbsent(material, k -> new HashSet<>())
                                .add(action);
                        }
                    }
                    
                    // Entity-based actions (KILL, BREED, etc.)
                    if (isEntityAction(actionType)) {
                        Set<String> entities = expandTarget(target, getAllEntities());
                        for (String entity : entities) {
                            jobActionsByEntity.computeIfAbsent(actionType, k -> new ConcurrentHashMap<>())
                                .computeIfAbsent(entity, k -> new HashSet<>())
                                .add(action);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Précalcule tous les patterns et wildcards.
     */
    private void loadPatternMappings() {
        // Collecte tous les targets utilisés
        Set<String> allTargets = new HashSet<>();
        for (Job job : plugin.getJobManager().getAllJobs()) {
            for (ActionType actionType : ActionType.values()) {
                for (JobAction action : job.getActions(actionType)) {
                    if (action.getTarget() != null) {
                        allTargets.add(action.getTarget());
                    }
                }
            }
        }
        
        // Compile tous les patterns (case-insensitive)
        for (String target : allTargets) {
            if (target.contains("*") || target.contains("?")) {
                compiledPatterns.put(target, Pattern.compile(
                    target.replace("*", ".*").replace("?", "."),
                    Pattern.CASE_INSENSITIVE
                ));
            }
        }
        
        // Précalcule les expansions de wildcards
        for (String target : allTargets) {
            if (target.contains("*")) {
                expandedWildcards.put(target, expandWildcard(target));
            }
        }
    }
    
    /**
     * Cache tous les permission nodes valides.
     */
    private void loadPermissionNodes() {
        // Wildcard permissions
        validPermissionNodes.add("*");
        validPermissionNodes.add("universejobs.*");
        validPermissionNodes.add("universejobs.multiplier.*");
        validPermissionNodes.add("universejobs.multiplier.money.*");
        validPermissionNodes.add("universejobs.multiplier.exp.*");

        // Multiplier permissions
        for (int i = 1; i <= 10; i++) {
            validPermissionNodes.add("universejobs.multiplier.money." + i);
            validPermissionNodes.add("universejobs.multiplier.exp." + i);
        }
        
        // Job permissions
        for (Job job : plugin.getJobManager().getAllJobs()) {
            if (job.getPermission() != null) {
                validPermissionNodes.add(job.getPermission());
            }
        }
    }
    
    /**
     * Précalcule tous les templates de messages.
     */
    private void loadMessageTemplates() {
        // Load message templates from config
        if (plugin.getConfig().contains("messages")) {
            for (String key : plugin.getConfig().getConfigurationSection("messages").getKeys(true)) {
                Object value = plugin.getConfig().get("messages." + key);
                if (value instanceof String) {
                    messageTemplates.put(key, (String) value);
                }
            }
        }
    }
    
    // ========== GETTERS ULTRA-RAPIDES ==========
    
    public boolean isDebugEnabled() { return debugEnabled; }
    public boolean isShowXpGain() { return showXpGain; }
    public int getMaxCachedPlayers() { return maxCachedPlayers; }
    
    /**
     * Lookup instantané des actions par material et ActionType (case-insensitive).
     */
    public Set<JobAction> getActionsForMaterial(ActionType actionType, String material) {
        Map<String, Set<JobAction>> materialMap = jobActionsByMaterial.get(actionType);
        if (materialMap == null) return Collections.emptySet();
        
        // First try exact match (for performance)
        Set<JobAction> exactMatch = materialMap.get(material);
        if (exactMatch != null && !exactMatch.isEmpty()) {
            return exactMatch;
        }
        
        // If no exact match, try case-insensitive search
        for (Map.Entry<String, Set<JobAction>> entry : materialMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(material)) {
                return entry.getValue();
            }
        }
        
        return Collections.emptySet();
    }
    
    /**
     * Lookup instantané des actions par entity et ActionType (case-insensitive).
     */
    public Set<JobAction> getActionsForEntity(ActionType actionType, String entity) {
        Map<String, Set<JobAction>> entityMap = jobActionsByEntity.get(actionType);
        if (entityMap == null) return Collections.emptySet();
        
        // First try exact match (for performance)
        Set<JobAction> exactMatch = entityMap.get(entity);
        if (exactMatch != null && !exactMatch.isEmpty()) {
            return exactMatch;
        }
        
        // If no exact match, try case-insensitive search
        for (Map.Entry<String, Set<JobAction>> entry : entityMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(entity)) {
                return entry.getValue();
            }
        }
        
        return Collections.emptySet();
    }
    
    /**
     * Validation de target ultra-rapide avec cache.
     */
    public boolean isValidTarget(String actionTarget, String contextTarget) {
        String cacheKey = actionTarget + ":" + contextTarget;
        return targetValidationCache.computeIfAbsent(cacheKey, key -> {
            // Direct match (case-insensitive)
            if (actionTarget.equalsIgnoreCase(contextTarget)) return true;
            
            // Pattern match
            Pattern pattern = compiledPatterns.get(actionTarget);
            if (pattern != null) {
                return pattern.matcher(contextTarget).matches();
            }
            
            // Wildcard match (case-insensitive)
            Set<String> expanded = expandedWildcards.get(actionTarget);
            if (expanded != null) {
                // Check if any expanded target matches case-insensitively
                return expanded.stream().anyMatch(target -> target.equalsIgnoreCase(contextTarget));
            }
            
            return false;
        });
    }
    
    /**
     * Check si un permission node est valide (avec cache).
     */
    public boolean isValidPermissionNode(String permission) {
        return validPermissionNodes.contains(permission);
    }
    
    /**
     * Get message template par clé.
     */
    public String getMessageTemplate(String key) {
        return messageTemplates.get(key);
    }
    
    // ========== HELPER METHODS ==========
    
    private boolean isMaterialAction(ActionType type) {
        return type == ActionType.BREAK || type == ActionType.PLACE || 
               type == ActionType.CRAFT || type == ActionType.SMELT ||
               type == ActionType.EAT || type == ActionType.POTION;
    }
    
    private boolean isEntityAction(ActionType type) {
        return type == ActionType.KILL || type == ActionType.BREED ||
               type == ActionType.TAME || type == ActionType.SHEAR ||
               type == ActionType.ENTITY_INTERACT || type == ActionType.TRADE;
    }
    
    private Set<String> expandTarget(String target, Set<String> allPossible) {
        if (!target.contains("*")) {
            // For non-wildcard targets, find case-insensitive matches
            return allPossible.stream()
                .filter(s -> s.equalsIgnoreCase(target))
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        }
        
        // Case-insensitive pattern matching for wildcards
        Pattern pattern = Pattern.compile(target.replace("*", ".*"), Pattern.CASE_INSENSITIVE);
        return allPossible.stream()
            .filter(s -> pattern.matcher(s).matches())
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    private Set<String> expandWildcard(String wildcard) {
        Pattern pattern = compiledPatterns.get(wildcard);
        if (pattern == null) return Collections.emptySet();
        
        Set<String> result = new HashSet<>();
        
        // Check against all materials (case-insensitive)
        for (Material material : Material.values()) {
            if (pattern.matcher(material.name()).matches()) {
                result.add(material.name());
            }
        }
        
        // Check against all entities (case-insensitive)
        for (EntityType entity : EntityType.values()) {
            if (pattern.matcher(entity.name()).matches()) {
                result.add(entity.name());
            }
        }
        
        return result;
    }
    
    private Set<String> getAllMaterials() {
        Set<String> materials = new HashSet<>();
        for (Material mat : Material.values()) {
            materials.add(mat.name());
        }
        return materials;
    }
    
    private Set<String> getAllEntities() {
        Set<String> entities = new HashSet<>();
        for (EntityType entity : EntityType.values()) {
            entities.add(entity.name());
        }
        return entities;
    }
    
    /**
     * Rechargement du cache (après reload de config).
     */
    public void reload() {
        clearCaches();
        loadAllConfigurations();
    }
    
    private void clearCaches() {
        jobActionsByMaterial.clear();
        jobActionsByEntity.clear();
        actionTypeLookup.clear();
        compiledPatterns.clear();
        expandedWildcards.clear();
        targetValidationCache.clear();
        validPermissionNodes.clear();
        messageTemplates.clear();
    }
    
    /**
     * Obtient la liste des matériaux mis en cache (pour debug).
     */
    public Set<String> getCachedMaterials() {
        Set<String> allMaterials = new HashSet<>();
        for (Map<String, Set<JobAction>> materialMap : jobActionsByMaterial.values()) {
            allMaterials.addAll(materialMap.keySet());
        }
        return allMaterials;
    }
    
    /**
     * Statistiques du cache pour monitoring.
     */
    public String getCacheStats() {
        int materialActions = 0;
        for (Map<String, Set<JobAction>> materialMap : jobActionsByMaterial.values()) {
            materialActions += materialMap.size();
        }
        
        int entityActions = 0;
        for (Map<String, Set<JobAction>> entityMap : jobActionsByEntity.values()) {
            entityActions += entityMap.size();
        }
        
        return String.format(
            "Materials: %d, Entities: %d, Patterns: %d, Validations: %d, Permissions: %d, Messages: %d",
            materialActions,
            entityActions,
            compiledPatterns.size(),
            targetValidationCache.size(),
            validPermissionNodes.size(),
            messageTemplates.size()
        );
    }
}