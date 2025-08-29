package fr.ax_dev.universejobs.menu.config;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.menu.MenuConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestionnaire des slots de jobs dans le menu principal.
 * Permet aux admins de définir quel job apparaît dans quel slot.
 */
public class JobSlotManager {
    
    private final UniverseJobs plugin;
    private final File configFile;
    private MenuConfig menuConfig;
    
    // Map: jobId -> slot number (0-53 pour inventaire 6 lignes)
    private final Map<String, Integer> jobSlots = new ConcurrentHashMap<>();
    
    // Map: slot number -> jobId (pour éviter les conflits)
    private final Map<Integer, String> slotJobs = new ConcurrentHashMap<>();
    
    // Slots disponibles pour auto-placement
    private final Set<Integer> availableSlots = ConcurrentHashMap.newKeySet();
    
    // Slots réservés (ne peuvent pas être utilisés pour les jobs)
    private final Set<Integer> reservedSlots = ConcurrentHashMap.newKeySet();
    
    public JobSlotManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "menus/job-slots.yml");
        
        initializeDefaultSlots();
        // Don't load configuration in constructor - will be done after MenuManager is fully initialized
    }
    
    /**
     * Initialize with MenuConfig reference.
     */
    public void initialize(MenuConfig menuConfig) {
        this.menuConfig = menuConfig;
        loadConfiguration();
    }
    
    /**
     * Initialise les slots disponibles par défaut.
     */
    private void initializeDefaultSlots() {
        // Menu 6x9 = 54 slots (0-53)
        // Réserve les bordures pour la décoration
        for (int i = 0; i < 54; i++) {
            if (isBorderSlot(i)) {
                reservedSlots.add(i);
            } else {
                availableSlots.add(i);
            }
        }
        
        // Réserve aussi les slots du bas pour la navigation
        for (int i = 45; i < 54; i++) {
            reservedSlots.add(i);
            availableSlots.remove(i);
        }
    }
    
    /**
     * Vérifie si un slot est une bordure (première/dernière ligne/colonne).
     */
    private boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        
        // Première ou dernière ligne
        if (row == 0 || row == 5) {
            return true;
        }
        
        // Première ou dernière colonne
        return col == 0 || col == 8;
    }
    
    /**
     * Charge la configuration depuis le main-menu.yml.
     */
    public void loadConfiguration() {
        try {
            // Clear existing configuration
            jobSlots.clear();
            slotJobs.clear();
            
            // Get main menu configuration
            if (menuConfig == null) {
                plugin.getLogger().warning("MenuConfig not initialized in JobSlotManager");
                return;
            }
            var mainMenuConfig = menuConfig.getMainMenuConfig();
            Map<String, Integer> configuredSlots = mainMenuConfig.getJobSlots();
            
            // Load job slots from main menu configuration
            for (Map.Entry<String, Integer> entry : configuredSlots.entrySet()) {
                String jobId = entry.getKey();
                int slot = entry.getValue();
                
                if (isValidSlot(slot) && !reservedSlots.contains(slot)) {
                    setJobSlot(jobId, slot);
                }
            }
            
            plugin.getLogger().info("Loaded job slot configuration from main-menu.yml: " + jobSlots.size() + " jobs configured");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load job slot configuration", e);
        }
    }
    
    /**
     * Crée la configuration par défaut (deprecated - now uses main-menu.yml).
     */
    private void createDefaultConfiguration() {
        // Job slots are now configured in main-menu.yml
        plugin.getLogger().info("Job slots are now configured in main-menu.yml under 'job-slots' section");
    }
    
    /**
     * Définit le slot d'un job spécifique.
     */
    public boolean setJobSlot(String jobId, int slot) {
        if (!isValidSlot(slot)) {
            return false;
        }
        
        if (reservedSlots.contains(slot)) {
            return false;
        }
        
        // Vérifie si le slot est déjà occupé
        String existingJob = slotJobs.get(slot);
        if (existingJob != null && !existingJob.equals(jobId)) {
            return false;
        }
        
        // Supprime l'ancien slot du job si il existait
        Integer oldSlot = jobSlots.get(jobId);
        if (oldSlot != null) {
            slotJobs.remove(oldSlot);
            availableSlots.add(oldSlot);
        }
        
        // Définit le nouveau slot
        jobSlots.put(jobId, slot);
        slotJobs.put(slot, jobId);
        availableSlots.remove(slot);
        
        return true;
    }
    
    /**
     * Supprime le slot configuré d'un job.
     */
    public void removeJobSlot(String jobId) {
        Integer slot = jobSlots.remove(jobId);
        if (slot != null) {
            slotJobs.remove(slot);
            availableSlots.add(slot);
        }
    }
    
    /**
     * Obtient le slot configuré pour un job.
     */
    public OptionalInt getJobSlot(String jobId) {
        Integer slot = jobSlots.get(jobId);
        return slot != null ? OptionalInt.of(slot) : OptionalInt.empty();
    }
    
    /**
     * Obtient le job configuré pour un slot.
     */
    public Optional<String> getSlotJob(int slot) {
        return Optional.ofNullable(slotJobs.get(slot));
    }
    
    /**
     * Calcule les slots pour tous les jobs disponibles.
     * Les jobs avec slots configurés gardent leur position,
     * les autres sont placés automatiquement.
     */
    public Map<String, Integer> calculateJobSlots(Collection<Job> jobs) {
        Map<String, Integer> result = new HashMap<>();
        Set<Integer> usedSlots = new HashSet<>(reservedSlots);
        
        // D'abord, place les jobs avec slots configurés
        for (Job job : jobs) {
            OptionalInt configuredSlot = getJobSlot(job.getId());
            if (configuredSlot.isPresent()) {
                int slot = configuredSlot.getAsInt();
                if (!usedSlots.contains(slot)) {
                    result.put(job.getId(), slot);
                    usedSlots.add(slot);
                }
            }
        }
        
        // Ensuite, place les jobs sans configuration dans les slots disponibles
        List<Integer> availableSlotsList = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            if (!usedSlots.contains(i)) {
                availableSlotsList.add(i);
            }
        }
        Collections.sort(availableSlotsList);
        
        int slotIndex = 0;
        for (Job job : jobs) {
            if (!result.containsKey(job.getId()) && slotIndex < availableSlotsList.size()) {
                result.put(job.getId(), availableSlotsList.get(slotIndex));
                slotIndex++;
            }
        }
        
        return result;
    }
    
    /**
     * Sauvegarde la configuration actuelle (deprecated - now uses main-menu.yml).
     */
    public void saveConfiguration() {
        plugin.getLogger().warning("Job slots configuration should be saved directly in main-menu.yml file");
        plugin.getLogger().info("Current slot configuration: " + jobSlots);
    }
    
    /**
     * Vérifie si un slot est valide.
     */
    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < 54;
    }
    
    /**
     * Ajoute un slot aux slots réservés.
     */
    public void addReservedSlot(int slot) {
        if (isValidSlot(slot)) {
            reservedSlots.add(slot);
            availableSlots.remove(slot);
            
            // Supprime le job de ce slot si il y en a un
            String jobId = slotJobs.remove(slot);
            if (jobId != null) {
                jobSlots.remove(jobId);
            }
        }
    }
    
    /**
     * Supprime un slot des slots réservés.
     */
    public void removeReservedSlot(int slot) {
        if (reservedSlots.remove(slot)) {
            availableSlots.add(slot);
        }
    }
    
    /**
     * Obtient tous les jobs configurés avec leurs slots.
     */
    public Map<String, Integer> getAllJobSlots() {
        return new HashMap<>(jobSlots);
    }
    
    /**
     * Obtient tous les slots réservés.
     */
    public Set<Integer> getReservedSlots() {
        return new HashSet<>(reservedSlots);
    }
    
    /**
     * Obtient les statistiques du manager.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("configured_jobs", jobSlots.size());
        stats.put("reserved_slots", reservedSlots.size());
        stats.put("available_slots", availableSlots.size());
        stats.put("total_slots", 54);
        
        return stats;
    }
    
    /**
     * Recharge la configuration.
     */
    public void reload() {
        initializeDefaultSlots();
        loadConfiguration();
    }
}