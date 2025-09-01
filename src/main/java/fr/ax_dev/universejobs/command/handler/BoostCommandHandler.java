package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.action.JobAction;
import fr.ax_dev.universejobs.bonus.MoneyBonus;
import fr.ax_dev.universejobs.bonus.XpBonus;
import fr.ax_dev.universejobs.job.Job;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        
        return switch (args.length) {
            case 3 -> Arrays.asList(CMD_GIVE, CMD_REMOVE, CMD_INFO).stream()
                    .filter(action -> action.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            case 4 -> getTypeCompletions(args);
            case 5 -> getPlayerCompletions(args);
            case 6 -> getJobCompletions(args);
            case 7 -> getActionTypeCompletions(args);
            case 8 -> getActionIdCompletions(args);
            case 9 -> getMultiplierCompletions(args);
            case 10 -> getDurationCompletions(args);
            default -> new ArrayList<>();
        };
    }
    
    private List<String> getTypeCompletions(String[] args) {
        String action = args[2].toLowerCase();
        if (action.equals(CMD_GIVE) || action.equals(CMD_REMOVE)) {
            return Arrays.asList(TYPE_XP, TYPE_MONEY).stream()
                    .filter(type -> type.startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    private List<String> getPlayerCompletions(String[] args) {
        String action = args[2].toLowerCase();
        if (action.equals(CMD_GIVE)) {
            List<String> players = new ArrayList<>();
            players.add("*");
            players.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[4].toLowerCase()))
                    .collect(Collectors.toList()));
            return players;
        }
        return new ArrayList<>();
    }
    
    private List<String> getMultiplierCompletions(String[] args) {
        String action = args[2].toLowerCase();
        if (action.equals(CMD_GIVE)) {
            return Arrays.asList("1.25", "1.5", "2.0", "2.5", "3.0").stream()
                    .filter(mult -> mult.startsWith(args[5]))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    private List<String> getDurationCompletions(String[] args) {
        String action = args[2].toLowerCase();
        if (action.equals(CMD_GIVE)) {
            return Arrays.asList("300", "600", "1800", "3600", "7200").stream()
                    .filter(dur -> dur.startsWith(args[6]))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    private List<String> getJobCompletions(String[] args) {
        String action = args[2].toLowerCase();
        if (action.equals(CMD_GIVE)) {
            List<String> jobs = new ArrayList<>();
            jobs.add("*");
            jobs.addAll(plugin.getJobManager().getJobs().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[5].toLowerCase()))
                    .collect(Collectors.toList()));
            return jobs;
        }
        return new ArrayList<>();
    }
    
    private List<String> getActionTypeCompletions(String[] args) {
        String action = args[2].toLowerCase();
        if (action.equals(CMD_GIVE) && args.length > 5) {
            String jobId = args[5];
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
                    .filter(act -> act.toLowerCase().startsWith(args[6].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    private List<String> getActionIdCompletions(String[] args) {
        String action = args[2].toLowerCase();
        if (action.equals(CMD_GIVE) && args.length > 6) {
            String actionType = args[6].toUpperCase();
            String jobId = args.length > 5 ? args[5] : "*";
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
                    .filter(id -> id.toLowerCase().startsWith(args[7].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    private List<String> getJobActionIds(Job job, String actionType) {
        List<String> actionIds = new ArrayList<>();
        
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
    
    private void handleGiveBoost(CommandSender sender, String[] args) {
        // /jobs admin boost give <xp|money> <player/*> <job/*> <action_type> <id_in_action> <multiplier> <duration>
        if (args.length < 10) {
            sender.sendMessage("§cUsage: /jobs admin boost give <xp|money> <player/*> <job/*> <action_type> <id_in_action> <multiplier> <duration>");
            sender.sendMessage("§7Examples:");
            sender.sendMessage("§7  /jobs admin boost give xp * miner BREAK simple_break 2.0 3600");
            sender.sendMessage("§7  /jobs admin boost give money Player123 * KILL simple_kill 1.5 1800");
            return;
        }
        
        String boostType = args[3].toLowerCase();
        if (!boostType.equals(TYPE_XP) && !boostType.equals(TYPE_MONEY)) {
            sender.sendMessage("§cInvalid boost type! Use: xp or money");
            return;
        }
        
        String target = sanitizeInput(args[4]);
        if (!target.equals("*") && !isValidPlayerName(target)) {
            sender.sendMessage("§cInvalid player name!");
            return;
        }
        
        String jobId = sanitizeInput(args[5]);
        if (!jobId.equals("*") && !isValidJobId(jobId)) {
            sender.sendMessage("§cInvalid job ID!");
            return;
        }
        
        String actionType = sanitizeInput(args[6]);
        if (!actionType.equals("*") && !isValidActionType(actionType)) {
            sender.sendMessage("§cInvalid action type!");
            return;
        }
        
        String actionId = sanitizeInput(args[7]);
        if (!actionId.equals("*") && !isValidActionId(actionType, actionId)) {
            sender.sendMessage("§cInvalid action ID for type " + actionType + "!");
            return;
        }
        
        double multiplier = parseMultiplier(args[8]);
        if (multiplier <= 0) {
            sender.sendMessage("§cInvalid multiplier! Must be between 0.1 and 10.0");
            return;
        }
        
        long duration = parseDuration(args[9]);
        if (duration <= 0) {
            sender.sendMessage("§cInvalid duration! Must be between 1 and 86400 seconds");
            return;
        }
        
        String reason = "Admin boost";
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        
        applyBoost(boostType, target, multiplier, duration, jobId, actionType, actionId, reason, senderName);
        
        String targetDisplay = target.equals("*") ? "all players" : target;
        String jobDisplay = jobId.equals("*") ? "all jobs" : jobId;
        String actionDisplay = actionType.equals("*") ? "all actions" : actionType;
        String actionIdDisplay = actionId.equals("*") ? "" : " (" + actionId + ")";
        sender.sendMessage("§aApplied " + boostType + " boost x" + multiplier + " for " + duration + "s to " + targetDisplay + " (" + jobDisplay + " - " + actionDisplay + actionIdDisplay + ")");
    }
    
    private void handleRemoveBoost(CommandSender sender, String[] args) {
        // /jobs admin boost remove <xp|money> <player> [job]
        if (args.length < 5) {
            sender.sendMessage("§cUsage: /jobs admin boost remove <xp|money> <player> [job]");
            return;
        }
        
        String boostType = args[3].toLowerCase();
        if (!boostType.equals(TYPE_XP) && !boostType.equals(TYPE_MONEY)) {
            sender.sendMessage("§cInvalid boost type! Use: xp or money");
            return;
        }
        
        String playerName = sanitizeInput(args[4]);
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        String jobId = args.length > 5 ? sanitizeInput(args[5]) : null;
        
        removeBoost(boostType, targetPlayer, jobId);
        
        String jobDisplay = (jobId == null || jobId.equals("*")) ? "all jobs" : jobId;
        sender.sendMessage("§aRemoved " + boostType + " boosts from " + playerName + " (" + jobDisplay + ")");
    }
    
    private void handleInfoBoost(CommandSender sender, String[] args) {
        sender.sendMessage("§6=== Boost System Information ===");
        sender.sendMessage("§7Active boost system for XP and money rewards");
        sender.sendMessage("§7Types: §exp§7, §emoney");
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
        if (target.equals("*")) {
            // Global boost
            if (boostType.equals(TYPE_XP)) {
                plugin.getBonusManager().addGlobalBonus(multiplier, duration, reason, senderName);
            } else if (boostType.equals(TYPE_MONEY)) {
                plugin.getMoneyBonusManager().addGlobalBonus(multiplier, duration, reason, senderName);
            }
        } else {
            // Player boost
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) return;
            
            if (jobId == null || jobId.equals("*")) {
                // All jobs
                if (boostType.equals(TYPE_XP)) {
                    plugin.getBonusManager().addPlayerBonus(targetPlayer.getUniqueId(), multiplier, duration, reason, senderName);
                } else if (boostType.equals(TYPE_MONEY)) {
                    plugin.getMoneyBonusManager().addPlayerBonus(targetPlayer.getUniqueId(), multiplier, duration, reason, senderName);
                }
            } else {
                // Specific job
                if (boostType.equals(TYPE_XP)) {
                    plugin.getBonusManager().addJobBonus(targetPlayer.getUniqueId(), jobId, multiplier, duration, reason, senderName);
                } else if (boostType.equals(TYPE_MONEY)) {
                    plugin.getMoneyBonusManager().addJobBonus(targetPlayer.getUniqueId(), jobId, multiplier, duration, reason, senderName);
                }
            }
        }
    }
    
    private void removeBoost(String boostType, Player targetPlayer, String jobId) {
        if (jobId == null || jobId.equals("*")) {
            // Remove all bonuses
            if (boostType.equals(TYPE_XP)) {
                plugin.getBonusManager().removeAllBonuses(targetPlayer.getUniqueId());
            } else if (boostType.equals(TYPE_MONEY)) {
                plugin.getMoneyBonusManager().removeAllBonuses(targetPlayer.getUniqueId());
            }
        } else {
            // Remove job-specific bonuses
            if (boostType.equals(TYPE_XP)) {
                List<XpBonus> bonuses = plugin.getBonusManager().getActiveBonuses(targetPlayer.getUniqueId(), jobId);
                bonuses.forEach(plugin.getBonusManager()::removeBonus);
            } else if (boostType.equals(TYPE_MONEY)) {
                List<MoneyBonus> bonuses = plugin.getMoneyBonusManager().getActiveBonuses(targetPlayer.getUniqueId(), jobId);
                bonuses.forEach(plugin.getMoneyBonusManager()::removeBonus);
            }
        }
    }
    
    private double parseMultiplier(String input) {
        try {
            String multiplierStr = sanitizeInput(input);
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
            String durationStr = sanitizeInput(input);
            if (durationStr.isEmpty() || durationStr.length() > 10) return -1;
            
            long duration = Long.parseLong(durationStr);
            return (duration > 0 && duration <= 86400) ? duration : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private void sendBoostHelp(CommandSender sender) {
        sender.sendMessage("§6=== Boost Commands ===");
        sender.sendMessage("§e/jobs admin boost <action> <type> <args...>");
        sender.sendMessage("");
        sender.sendMessage("§6Actions:");
        sender.sendMessage("§e  give <xp|money> <player/*> <job/*> <action_type> <id_in_action> <multiplier> <duration>");
        sender.sendMessage("§e  remove <xp|money> <player> [job]");
        sender.sendMessage("§e  info");
        sender.sendMessage("");
        sender.sendMessage("§7Examples:");
        sender.sendMessage("§7  /jobs admin boost give xp * miner BREAK simple_break 2.0 3600");
        sender.sendMessage("§7  /jobs admin boost give money Player123 * KILL simple_kill 1.5 1800");
        sender.sendMessage("§7  /jobs admin boost remove money Player123");
    }
}