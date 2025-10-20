package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.action.JobAction;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.utils.MessageUtils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unified boost command handler for both XP and Money bonuses.
 * Command structure: /jobs admin boost <action> <type> <args...>
 */
public class BoostCommandHandler extends JobCommandHandler {
    
    private static final String CMD_GIVE = "give";
    private static final String CMD_REMOVE = "remove";
    private static final String CMD_INFO = "info";
    
    private static final String TYPE_XP = "xp";
    private static final String TYPE_MONEY = "money";
    
    private static final String PERM_BOOST_ADMIN = "universejobs.admin.boost";
    
    public BoostCommandHandler(UniverseJobs plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_BOOST_ADMIN)) {
            sender.sendMessage(languageManager.getMessage("commands.no-permission"));
            return true;
        }
        
        if (args.length < 3) {
            sendBoostHelp(sender);
            return true;
        }
        
        String action = args[2].toLowerCase();
        
        switch (action) {
            case CMD_GIVE -> handleGiveBoost(sender, args);
            case CMD_REMOVE -> handleRemoveBoost(sender, args);
            case CMD_INFO -> handleInfoBoost(sender, args);
            default -> sendBoostHelp(sender);
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_BOOST_ADMIN)) {
            return new ArrayList<>();
        }
        
        // Déterminer l'offset selon le contexte d'appel
        final int offset = (args.length >= 3 && "boost".equals(args[2])) ? 1 : 0;
        
        int adjustedLength = args.length - offset;
        
        String action = args.length > 2 + offset ? args[2 + offset].toLowerCase() : "";
        
        return switch (adjustedLength) {
            case 3 -> Arrays.asList(CMD_GIVE, CMD_REMOVE, CMD_INFO).stream()
                    .filter(cmd -> cmd.startsWith(action))
                    .collect(Collectors.toList());
            case 4 -> {
                if (CMD_REMOVE.equals(action)) {
                    yield getActiveBoostIds(args, offset);
                } else if (CMD_GIVE.equals(action)) {
                    yield getTypeCompletions(args, offset);
                }
                yield new ArrayList<>();
            }
            case 5 -> getPlayerCompletions(args, offset);
            case 6 -> getJobCompletions(args, offset);
            case 7 -> getActionTypeCompletions(args, offset);
            case 8 -> getActionIdOrMultiplierCompletions(args, offset);
            case 9 -> getMultiplierOrDurationCompletions(args, offset);
            case 10 -> getDurationCompletions(args, offset);
            default -> new ArrayList<>();
        };
    }
    
    private List<String> getTypeCompletions(String[] args, int offset) {
        String action = args[2 + offset].toLowerCase();
        if (action.equals(CMD_GIVE) || action.equals(CMD_REMOVE)) {
            return Arrays.asList(TYPE_XP, TYPE_MONEY).stream()
                    .filter(type -> type.startsWith(args[3 + offset].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    private List<String> getPlayerCompletions(String[] args, int offset) {
        String action = args[2 + offset].toLowerCase();
        if (action.equals(CMD_GIVE)) {
            List<String> players = new ArrayList<>();
            players.add("*");
            players.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[4 + offset].toLowerCase()))
                    .collect(Collectors.toList()));
            return players;
        }
        return new ArrayList<>();
    }
    
    private List<String> getDurationCompletions(String[] args, int offset) {
        String action = args[2 + offset].toLowerCase();
        if (action.equals(CMD_GIVE)) {
            int durationArgIndex;
            
            // Déterminer l'index de la duration selon si action_type est * ou non
            if (args.length > 6 + offset && args[6 + offset].equals("*")) {
                // Si action_type est *, la duration est à l'index 8
                durationArgIndex = 8 + offset;
            } else {
                // Si action_type n'est pas *, la duration est à l'index 9
                durationArgIndex = 9 + offset;
            }
            
            if (args.length > durationArgIndex) {
                return Arrays.asList("300", "600", "1800", "3600", "7200").stream()
                        .filter(dur -> dur.startsWith(args[durationArgIndex]))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
    
    private List<String> getJobCompletions(String[] args, int offset) {
        String action = args[2 + offset].toLowerCase();
        if (action.equals(CMD_GIVE)) {
            List<String> jobs = new ArrayList<>();
            jobs.add("*");
            jobs.addAll(plugin.getJobManager().getJobs().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[5 + offset].toLowerCase()))
                    .collect(Collectors.toList()));
            return jobs;
        }
        return new ArrayList<>();
    }
    
    private List<String> getActionTypeCompletions(String[] args, int offset) {
        String action = args[2 + offset].toLowerCase();
        if (action.equals(CMD_GIVE) && args.length > 5 + offset) {
            String jobId = args[5 + offset];
            List<String> actionTypes = new ArrayList<>();
            actionTypes.add("*");
            
            if (jobId.equals("*")) {
                // Récupérer tous les ActionTypes de tous les jobs
                Set<String> allActionTypes = new HashSet<>();
                for (Job job : plugin.getJobManager().getJobs().values()) {
                    for (ActionType actionType : job.getActions().keySet()) {
                        allActionTypes.add(actionType.name());
                    }
                }
                actionTypes.addAll(allActionTypes);
            } else {
                // Récupérer les ActionTypes du job spécifique
                Job job = plugin.getJobManager().getJob(jobId);
                if (job != null) {
                    for (ActionType actionType : job.getActions().keySet()) {
                        actionTypes.add(actionType.name());
                    }
                }
            }
            
            return actionTypes.stream()
                    .distinct()
                    .sorted()
                    .filter(act -> act.toLowerCase().startsWith(args[6 + offset].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    private List<String> getActionIdOrMultiplierCompletions(String[] args, int offset) {
        String action = args[2 + offset].toLowerCase();
        if (action.equals(CMD_GIVE) && args.length > 6 + offset) {
            String actionType = args[6 + offset];
            
            if (actionType.equals("*")) {
                // Si action_type est *, alors args[7] est le multiplier
                return Arrays.asList("1.0", "1.5", "2.0", "2.5", "3.0").stream()
                        .filter(mult -> mult.startsWith(args[7 + offset]))
                        .collect(Collectors.toList());
            } else {
                // Sinon, c'est l'id_in_action
                return getActionIdCompletions(args, offset);
            }
        }
        return new ArrayList<>();
    }
    
    private List<String> getMultiplierOrDurationCompletions(String[] args, int offset) {
        String action = args[2 + offset].toLowerCase();
        if (action.equals(CMD_GIVE) && args.length > 6 + offset) {
            String actionType = args[6 + offset];
            
            if (actionType.equals("*")) {
                // Si action_type est *, alors args[8] est la duration
                return Arrays.asList("300", "600", "1800", "3600", "7200").stream()
                        .filter(dur -> dur.startsWith(args[8 + offset]))
                        .collect(Collectors.toList());
            } else {
                // Sinon, c'est le multiplier
                return Arrays.asList("1.0", "1.5", "2.0", "2.5", "3.0").stream()
                        .filter(mult -> mult.startsWith(args[8 + offset]))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
    
    private List<String> getActionIdCompletions(String[] args, int offset) {
        String action = args[2 + offset].toLowerCase();
        if (action.equals(CMD_GIVE) && args.length > 6 + offset) {
            String actionType = args[6 + offset].toUpperCase();
            String jobId = args.length > 5 + offset ? args[5 + offset] : "*";
            List<String> ids = new ArrayList<>();
            ids.add("*");
            
            if (jobId.equals("*")) {
                for (Job job : plugin.getJobManager().getJobs().values()) {
                    ids.addAll(getJobActionIds(job, actionType));
                }
            } else {
                Job job = plugin.getJobManager().getJob(jobId);
                if (job != null) {
                    ids.addAll(getJobActionIds(job, actionType));
                }
            }
            
            return ids.stream()
                    .distinct()
                    .filter(id -> id.toLowerCase().startsWith(args[7 + offset].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    private List<String> getJobActionIds(Job job, String actionType) {
        List<String> actionIds = new ArrayList<>();
        
        try {
            // Essayer d'accéder à la configuration via le job manager
            File jobFile = new File(plugin.getDataFolder(), "jobs/" + job.getId() + ".yml");
            if (jobFile.exists()) {
                org.bukkit.configuration.file.YamlConfiguration jobConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(jobFile);
                org.bukkit.configuration.ConfigurationSection actionSection = jobConfig.getConfigurationSection("actions." + actionType);
                if (actionSection != null) {
                    actionIds.addAll(actionSection.getKeys(false));
                    return actionIds;
                }
            }
        } catch (Exception e) {
            // En cas d'erreur, continuer avec le fallback
        }
        
        // Fallback : utiliser getName() depuis les JobAction
        try {
            ActionType enumActionType = ActionType.valueOf(actionType.toUpperCase());
            List<JobAction> actions = job.getActions(enumActionType);
            if (actions != null) {
                for (JobAction action : actions) {
                    actionIds.add(action.getName());
                }
            }
        } catch (IllegalArgumentException e) {
            // ActionType inconnu, ignoré
        }
        
        return actionIds;
    }
    
    private List<String> getActiveBoostIds(String[] args, int offset) {
        List<String> allBoosts = new ArrayList<>();
        
        // Ajouter tous les boosts XP actifs
        allBoosts.addAll(plugin.getBonusManager().getAllActiveBoostIds());
        
        // Ajouter tous les boosts Money actifs
        allBoosts.addAll(plugin.getMoneyBonusManager().getAllActiveBoostIds());
        
        String input = args.length > 3 + offset ? args[3 + offset].toLowerCase() : "";
        return allBoosts.stream()
                .filter(boostId -> boostId.toLowerCase().startsWith(input))
                .sorted()
                .collect(Collectors.toList());
    }
    
    private void handleGiveBoost(CommandSender sender, String[] args) {
        // /jobs admin boost give <xp|money> <player/*> <job/*> <action_type> [id_in_action] <multiplier> <duration>
        if (args.length < 9) {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.boost-usage-general"));
            return;
        }
        
        String boostType = args[3].toLowerCase();
        if (!boostType.equals(TYPE_XP) && !boostType.equals(TYPE_MONEY)) {
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-invalid-type"));
            return;
        }

        String target = sanitizeInputWithWildcard(args[4]);
        if (!target.equals("*") && !isValidPlayerName(target)) {
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-invalid-player"));
            return;
        }

        String jobId = sanitizeInputWithWildcard(args[5]);
        if (!jobId.equals("*") && !isValidJobId(jobId)) {
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-invalid-job"));
            return;
        }

        String actionType = sanitizeInputWithWildcard(args[6]);
        if (!actionType.equals("*") && !isValidActionType(actionType)) {
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-invalid-action-type"));
            return;
        }
        
        String actionId = "*";
        double multiplier;
        long duration;
        
        if (actionType.equals("*")) {
            // Si action_type est *, pas besoin d'id_in_action
            multiplier = parseMultiplier(args[7]);
            if (multiplier <= 0) {
                sender.sendMessage(languageManager.getMessage("commands.admin.boost-invalid-multiplier"));
                return;
            }

            duration = parseDuration(args[8]);
            if (duration <= 0) {
                sender.sendMessage(languageManager.getMessage("commands.admin.boost-invalid-duration"));
                return;
            }
        } else {
            // Action type spécifique, id_in_action requis
            if (args.length < 10) {
                MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.boost-action-id-required"));
                MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.boost-usage-with-action"));
                return;
            }
            
            actionId = sanitizeInputWithWildcard(args[7]);
            if (!actionId.equals("*") && !isValidActionId(actionType, actionId)) {
                sender.sendMessage(languageManager.getMessage("commands.admin.boost-invalid-action-id", "type", actionType));
                return;
            }

            multiplier = parseMultiplier(args[8]);
            if (multiplier <= 0) {
                sender.sendMessage(languageManager.getMessage("commands.admin.boost-invalid-multiplier"));
                return;
            }

            duration = parseDuration(args[9]);
            if (duration <= 0) {
                sender.sendMessage(languageManager.getMessage("commands.admin.boost-invalid-duration"));
                return;
            }
        }
        
        String reason = languageManager.getMessage("commands.admin.boost-reason-admin");
        String senderName = sender instanceof Player ? sender.getName() :
                languageManager.getMessage("commands.admin.boost-sender-console");
        
        applyBoost(boostType, target, multiplier, duration, jobId, actionType, actionId, reason, senderName);
        
        String targetDisplay = target.equals("*") ? languageManager.getMessage("commands.admin.boost-info-all-players") : target;
        String jobDisplay = jobId.equals("*") ? languageManager.getMessage("commands.admin.boost-info-all-jobs") : jobId;
        String actionDisplay = actionType.equals("*") ? languageManager.getMessage("commands.admin.boost-info-all-actions") : actionType;
        String actionIdDisplay = actionId.equals("*") ? "" : " (" + actionId + ")";
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.boost-applied",
                "type", boostType,
                "multiplier", String.valueOf(multiplier),
                "duration", String.valueOf(duration),
                "target", targetDisplay,
                "job", jobDisplay,
                "action", actionDisplay,
                "action_id", actionIdDisplay));
    }
    
    private void handleRemoveBoost(CommandSender sender, String[] args) {
        // /jobs admin boost remove <boostId>
        if (args.length < 4) {
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-remove-usage"));
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-remove-info-tip"));
            return;
        }
        
        String boostId = sanitizeInput(args[3]);
        
        // Essayer de supprimer des deux types de boost
        boolean removedXp = plugin.getBonusManager().removeBoostById(boostId);
        boolean removedMoney = plugin.getMoneyBonusManager().removeBoostById(boostId);
        
        if (removedXp || removedMoney) {
            String type = removedXp
                    ? languageManager.getMessage("commands.admin.boost-type-xp")
                    : languageManager.getMessage("commands.admin.boost-type-money");
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-remove-success", "type", type, "id", boostId));
        } else {
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-remove-not-found", "id", boostId));
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-remove-info-tip"));
        }
    }
    
    private void handleInfoBoost(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            // Ouvrir le GUI pour les joueurs
            plugin.getBoostManagerGui().openGui(player);
        } else {
            // Affichage texte pour la console
            sender.sendMessage(languageManager.getMessage("commands.admin.boost-info-header"));

            List<String> xpBoosts = plugin.getBonusManager().getAllActiveBoostIds();
            List<String> moneyBoosts = plugin.getMoneyBonusManager().getAllActiveBoostIds();

            if (xpBoosts.isEmpty() && moneyBoosts.isEmpty()) {
                sender.sendMessage(languageManager.getMessage("commands.admin.boost-info-none"));
                return;
            }

            if (!xpBoosts.isEmpty()) {
                sender.sendMessage(languageManager.getMessage("commands.admin.boost-info-xp-header"));
                for (String boostId : xpBoosts) {
                    var boost = plugin.getBonusManager().getBoostById(boostId);
                    if (boost != null && boost.isActive()) {
                        String playerName;
                        if (boost.isGlobal()) {
                            playerName = languageManager.getMessage("commands.admin.boost-info-all-players");
                        } else {
                            Player player = Bukkit.getPlayer(boost.getPlayerId());
                            playerName = player != null ? player.getName() : languageManager.getMessage("commands.admin.boost-info-unknown-player");
                        }

                        String jobInfo = boost.getJobId() == null ? languageManager.getMessage("commands.admin.boost-info-all-jobs") : boost.getJobId();
                        String actionInfo = formatActionInfo(boost.getActionType(), boost.getActionId());
                        sender.sendMessage(languageManager.getMessage("commands.admin.boost-info-entry",
                                "id", boostId,
                                "player", playerName,
                                "multiplier", String.valueOf(boost.getMultiplier()),
                                "job", jobInfo,
                                "action", actionInfo,
                                "time", boost.getRemainingTimeFormatted()));
                    }
                }
            }

            if (!moneyBoosts.isEmpty()) {
                sender.sendMessage(languageManager.getMessage("commands.admin.boost-info-money-header"));
                for (String boostId : moneyBoosts) {
                    var boost = plugin.getMoneyBonusManager().getBoostById(boostId);
                    if (boost != null && boost.isActive()) {
                        String playerName;
                        if (boost.isGlobal()) {
                            playerName = languageManager.getMessage("commands.admin.boost-info-all-players");
                        } else {
                            Player player = Bukkit.getPlayer(boost.getPlayerId());
                            playerName = player != null ? player.getName() : languageManager.getMessage("commands.admin.boost-info-unknown-player");
                        }

                        String jobInfo = boost.getJobId() == null ? languageManager.getMessage("commands.admin.boost-info-all-jobs") : boost.getJobId();
                        String actionInfo = formatActionInfo(boost.getActionType(), boost.getActionId());
                        sender.sendMessage(languageManager.getMessage("commands.admin.boost-info-entry",
                                "id", boostId,
                                "player", playerName,
                                "multiplier", String.valueOf(boost.getMultiplier()),
                                "job", jobInfo,
                                "action", actionInfo,
                                "time", boost.getRemainingTimeFormatted()));
                    }
                }
            }

            sender.sendMessage(languageManager.getMessage("commands.admin.boost-info-remove-tip"));
        }
    }
    
    private String formatActionInfo(String actionType, String actionId) {
        if (actionType == null) {
            return languageManager.getMessage("commands.admin.boost-info-all-actions");
        }
        
        if (actionId == null) {
            return actionType;
        }
        
        return actionType + ":" + actionId;
    }
    
    private boolean isValidActionType(String actionType) {
        try {
            ActionType.valueOf(actionType.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private boolean isValidActionId(String actionType, String actionId) {
        if (actionId.equals("*")) return true;
        
        try {
            ActionType enumActionType = ActionType.valueOf(actionType.toUpperCase());
            for (Job job : plugin.getJobManager().getJobs().values()) {
                List<JobAction> actions = job.getActions(enumActionType);
                if (actions != null) {
                    for (JobAction action : actions) {
                        if (action.getName().equals(actionId)) {
                            return true;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            // ActionType inconnu
        }
        
        return false;
    }
    
    private void applyBoost(String boostType, String target, double multiplier, long duration, String jobId, String actionType, String actionId, String reason, String senderName) {
        String finalActionType = actionType.equals("*") ? null : actionType;
        String finalActionId = actionId.equals("*") ? null : actionId;
        String finalJobId = jobId.equals("*") ? null : jobId;
        
        if (target.equals("*")) {
            // Global boost pour tous les joueurs connectés
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (boostType.equals(TYPE_XP)) {
                    plugin.getBonusManager().addActionBonus(player.getUniqueId(), finalJobId, finalActionType, finalActionId, multiplier, duration, reason, senderName, true);
                } else if (boostType.equals(TYPE_MONEY)) {
                    plugin.getMoneyBonusManager().addActionBonus(player.getUniqueId(), finalJobId, finalActionType, finalActionId, multiplier, duration, reason, senderName, true);
                }
            }
        } else {
            // Player boost
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) return;
            
            if (boostType.equals(TYPE_XP)) {
                plugin.getBonusManager().addActionBonus(targetPlayer.getUniqueId(), finalJobId, finalActionType, finalActionId, multiplier, duration, reason, senderName, false);
            } else if (boostType.equals(TYPE_MONEY)) {
                plugin.getMoneyBonusManager().addActionBonus(targetPlayer.getUniqueId(), finalJobId, finalActionType, finalActionId, multiplier, duration, reason, senderName, false);
            }
        }
    }
    
    private double parseMultiplier(String input) {
        try {
            String multiplierStr = sanitizeNumber(input);
            if (multiplierStr.isEmpty() || multiplierStr.length() > 10) return -1;
            
            double multiplier = Double.parseDouble(multiplierStr);
            return (multiplier > 0.1 && multiplier <= 10.0 && !Double.isNaN(multiplier) && !Double.isInfinite(multiplier)) 
                    ? multiplier : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private long parseDuration(String input) {
        try {
            String durationStr = sanitizeNumber(input);
            if (durationStr.isEmpty() || durationStr.length() > 10) return -1;
            
            long duration = Long.parseLong(durationStr);
            return (duration > 0 && duration <= 86400) ? duration : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private void sendBoostHelp(CommandSender sender) {
        for (String line : languageManager.getMessageList("commands.admin.boost-help")) {
            sender.sendMessage(line);
        }
    }
}