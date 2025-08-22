package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.bonus.BonusManager;
import fr.ax_dev.universejobs.job.Job;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic bonus command handler to eliminate code duplication.
 */
public abstract class BonusCommandHandler<T, M extends BonusManager<T>> extends JobCommandHandler {
    
    private static final String CMD_REMOVE = "remove";
    private static final String USAGE_PREFIX = "§cUsage: /jobs ";
    
    protected final M bonusManager;
    private final String bonusType;
    private final String permission;
    
    protected BonusCommandHandler(UniverseJobs plugin, M bonusManager, String bonusType, String permission) {
        super(plugin);
        this.bonusManager = bonusManager;
        this.bonusType = bonusType;
        this.permission = permission;
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, permission)) {
            sender.sendMessage(languageManager.getMessage("commands.no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sendBonusHelp(sender);
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (subCommand) {
            case "give" -> handleBonusGive(sender, subArgs);
            case CMD_REMOVE -> handleBonusRemove(sender, subArgs);
            case "list" -> handleBonusList(sender, subArgs);
            case "info" -> handleBonusInfo(sender);
            case "cleanup" -> handleBonusCleanup(sender);
            default -> sendBonusHelp(sender);
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!hasPermission(sender, permission)) {
            return completions;
        }
        
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            List<String> subCommands = Arrays.asList("give", CMD_REMOVE, "list", "info", "cleanup");
            return subCommands.stream()
                    .filter(cmd -> cmd.startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length >= 3) {
            return getBonusTabCompletions(args);
        }
        
        return completions;
    }
    
    private void handleBonusGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(USAGE_PREFIX + bonusType + " give <player|*> <multiplier> <duration> [job] [reason]");
            sender.sendMessage("§7Examples:");
            sender.sendMessage("§7  /jobs " + bonusType + " give * 2.0 3600 - Give all players 2x " + bonusType + " for 1 hour");
            sender.sendMessage("§7  /jobs " + bonusType + " give Player123 1.5 1800 miner Mining Event");
            return;
        }
        
        String target = sanitizeInput(args[1]);
        if (!target.equals("*") && !isValidPlayerName(target)) {
            return;
        }
        
        double multiplier = parseMultiplier(args[2]);
        if (multiplier <= 0) return;
        
        long duration = parseDuration(args[3]);
        if (duration <= 0) return;
        
        String jobId = args.length > 4 ? sanitizeInput(args[4]) : null;
        if (jobId != null && !jobId.equals("*") && !isValidJobId(jobId)) {
            return;
        }
        
        String reason = parseReason(args, 5);
        
        if (jobId != null && !jobId.equals("*")) {
            Job job = jobManager.getJob(jobId);
            if (job == null) return;
        }
        
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        
        if (target.equals("*")) {
            bonusManager.addGlobalBonus(multiplier, duration, reason, senderName);
        } else {
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) return;
            
            if (jobId == null || jobId.equals("*")) {
                bonusManager.addPlayerBonus(targetPlayer.getUniqueId(), multiplier, duration, reason, senderName);
            } else {
                bonusManager.addJobBonus(targetPlayer.getUniqueId(), jobId, multiplier, duration, reason, senderName);
            }
        }
    }
    
    private void handleBonusRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(USAGE_PREFIX + bonusType + " remove <player> [job]");
            return;
        }
        
        String playerName = sanitizeInput(args[1]);
        if (!isValidPlayerName(playerName)) return;
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) return;
        
        String jobId = args.length > 2 ? sanitizeInput(args[2]) : null;
        if (jobId != null && !isValidJobId(jobId)) return;
        
        if (jobId == null) {
            bonusManager.removeAllBonuses(targetPlayer.getUniqueId());
        } else {
            List<T> bonuses = bonusManager.getActiveBonuses(targetPlayer.getUniqueId(), jobId);
            bonuses.forEach(bonusManager::removeBonus);
        }
    }
    
    private void handleBonusList(CommandSender sender, String[] args) {
        String playerName = args.length > 1 ? args[1] : (sender instanceof Player ? sender.getName() : null);
        
        if (playerName == null) {
            sender.sendMessage(USAGE_PREFIX + bonusType + " list [player]");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) return;
        
        List<T> bonuses = bonusManager.getActiveBonuses(targetPlayer.getUniqueId());
        if (bonuses.isEmpty()) return;
    }
    
    private void handleBonusInfo(CommandSender sender) {
    }
    
    private void handleBonusCleanup(CommandSender sender) {
        bonusManager.cleanupExpiredBonuses();
    }
    
    private double parseMultiplier(String input) {
        try {
            String multiplierStr = sanitizeInput(input);
            if (multiplierStr.isEmpty() || multiplierStr.length() > 10) return -1;
            
            double multiplier = Double.parseDouble(multiplierStr);
            return (multiplier > 0.0 && multiplier <= 10.0 && !Double.isNaN(multiplier) && !Double.isInfinite(multiplier)) 
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
    
    private String parseReason(String[] args, int startIndex) {
        if (args.length <= startIndex) return "Admin bonus";
        
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = startIndex; i < args.length && i < startIndex + 5; i++) {
            String part = sanitizeInput(args[i]);
            if (!part.isEmpty()) {
                if (reasonBuilder.length() > 0) reasonBuilder.append(" ");
                reasonBuilder.append(part);
            }
        }
        
        String reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : "Admin bonus";
        return reason.length() > 100 ? reason.substring(0, 100) : reason;
    }
    
    private List<String> getBonusTabCompletions(String[] args) {
        String subCommand = args[1].toLowerCase();
        
        return switch (args.length) {
            case 3 -> getBonusCompletions3Args(subCommand, args[2].toLowerCase());
            case 4 -> getBonusCompletions4Args(subCommand, args[3].toLowerCase());
            case 5 -> getBonusCompletions5Args(subCommand, args[4].toLowerCase());
            case 6 -> getBonusCompletions6Args(subCommand, args[5].toLowerCase());
            default -> new ArrayList<>();
        };
    }
    
    private List<String> getBonusCompletions3Args(String subCommand, String input) {
        List<String> completions = new ArrayList<>();
        
        switch (subCommand) {
            case "give" -> {
                completions.add("*");
                completions.addAll(getFilteredPlayerNames(input));
            }
            case CMD_REMOVE, "list" -> completions.addAll(getFilteredPlayerNames(input));
        }
        return completions;
    }
    
    private List<String> getBonusCompletions4Args(String subCommand, String input) {
        if (!subCommand.equals("give")) return new ArrayList<>();
        
        List<String> multipliers = Arrays.asList("1.25", "1.5", "2.0", "3.0");
        return multipliers.stream()
                .filter(mult -> mult.startsWith(input))
                .collect(Collectors.toList());
    }
    
    private List<String> getBonusCompletions5Args(String subCommand, String input) {
        if (!subCommand.equals("give")) return new ArrayList<>();
        
        List<String> durations = Arrays.asList("300", "600", "1800", "3600", "7200");
        return durations.stream()
                .filter(dur -> dur.startsWith(input))
                .collect(Collectors.toList());
    }
    
    private List<String> getBonusCompletions6Args(String subCommand, String input) {
        if (!subCommand.equals("give")) return new ArrayList<>();
        
        List<String> completions = new ArrayList<>();
        completions.add("*");
        completions.addAll(jobManager.getAllJobs().stream()
                .map(Job::getId)
                .filter(jobId -> jobId.toLowerCase().startsWith(input))
                .collect(Collectors.toList()));
        return completions;
    }
    
    private List<String> getFilteredPlayerNames(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
    
    protected abstract void sendBonusHelp(CommandSender sender);
}