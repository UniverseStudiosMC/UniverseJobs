package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.bonus.MoneyBonus;
import fr.ax_dev.universejobs.bonus.XpBonus;
import fr.ax_dev.universejobs.job.Job;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified boost command handler for both XP and Money bonuses.
 * Replaces XpBonusCommandHandler and MoneyBonusCommandHandler.
 */
public class BoostCommandHandler extends JobCommandHandler {
    
    private static final String CMD_XP = "xp";
    private static final String CMD_MONEY = "money";
    private static final String CMD_GIVE = "give";
    private static final String CMD_REMOVE = "remove";
    private static final String CMD_LIST = "list";
    private static final String CMD_INFO = "info";
    private static final String CMD_CLEANUP = "cleanup";
    
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
        
        String boostType = args[2].toLowerCase();
        if (!boostType.equals(CMD_XP) && !boostType.equals(CMD_MONEY)) {
            sendBoostHelp(sender);
            return true;
        }
        
        if (args.length < 4) {
            sendBoostHelp(sender);
            return true;
        }
        
        String action = args[3].toLowerCase();
        String[] actionArgs = Arrays.copyOfRange(args, 3, args.length);
        
        switch (action) {
            case CMD_GIVE -> handleBoostGive(sender, boostType, actionArgs);
            case CMD_REMOVE -> handleBoostRemove(sender, boostType, actionArgs);
            case CMD_LIST -> handleBoostList(sender, boostType, actionArgs);
            case CMD_INFO -> handleBoostInfo(sender, boostType);
            case CMD_CLEANUP -> handleBoostCleanup(sender, boostType);
            default -> sendBoostHelp(sender);
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!hasPermission(sender, PERM_BOOST_ADMIN)) {
            return completions;
        }
        
        if (args.length == 3) {
            return Arrays.asList(CMD_XP, CMD_MONEY).stream()
                    .filter(type -> type.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 4) {
            return Arrays.asList(CMD_GIVE, CMD_REMOVE, CMD_LIST, CMD_INFO, CMD_CLEANUP).stream()
                    .filter(cmd -> cmd.startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length >= 5) {
            String boostType = args[2].toLowerCase();
            String action = args[3].toLowerCase();
            
            if (boostType.equals(CMD_XP) || boostType.equals(CMD_MONEY)) {
                return getBoostTabCompletions(action, args);
            }
        }
        
        return completions;
    }
    
    private void handleBoostGive(CommandSender sender, String boostType, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /jobs admin boost " + boostType + " give <player|*> <multiplier> <duration> [job] [reason]");
            sender.sendMessage("§7Examples:");
            sender.sendMessage("§7  /jobs admin boost " + boostType + " give * 2.0 3600");
            sender.sendMessage("§7  /jobs admin boost " + boostType + " give Player123 1.5 1800 miner Event");
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
        
        int reasonStartIndex = (jobId != null) ? 5 : 4;
        String reason = parseReason(args, reasonStartIndex);
        
        if (jobId != null && !jobId.equals("*")) {
            Job job = jobManager.getJob(jobId);
            if (job == null) return;
        }
        
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        
        if (target.equals("*")) {
            if (boostType.equals(CMD_XP)) {
                plugin.getBonusManager().addGlobalBonus(multiplier, duration, reason, senderName);
            } else {
                plugin.getMoneyBonusManager().addGlobalBonus(multiplier, duration, reason, senderName);
            }
        } else {
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) return;
            
            if (jobId == null || jobId.equals("*")) {
                if (boostType.equals(CMD_XP)) {
                    plugin.getBonusManager().addPlayerBonus(targetPlayer.getUniqueId(), multiplier, duration, reason, senderName);
                } else {
                    plugin.getMoneyBonusManager().addPlayerBonus(targetPlayer.getUniqueId(), multiplier, duration, reason, senderName);
                }
            } else {
                if (boostType.equals(CMD_XP)) {
                    plugin.getBonusManager().addJobBonus(targetPlayer.getUniqueId(), jobId, multiplier, duration, reason, senderName);
                } else {
                    plugin.getMoneyBonusManager().addJobBonus(targetPlayer.getUniqueId(), jobId, multiplier, duration, reason, senderName);
                }
            }
        }
    }
    
    private void handleBoostRemove(CommandSender sender, String boostType, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jobs admin boost " + boostType + " remove <player> [job]");
            return;
        }
        
        String playerName = sanitizeInput(args[1]);
        if (!isValidPlayerName(playerName)) return;
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) return;
        
        String jobId = args.length > 2 ? sanitizeInput(args[2]) : null;
        if (jobId != null && !isValidJobId(jobId)) return;
        
        if (jobId == null) {
            if (boostType.equals(CMD_XP)) {
                plugin.getBonusManager().removeAllBonuses(targetPlayer.getUniqueId());
            } else {
                plugin.getMoneyBonusManager().removeAllBonuses(targetPlayer.getUniqueId());
            }
        } else {
            if (boostType.equals(CMD_XP)) {
                List<XpBonus> bonuses = plugin.getBonusManager().getActiveBonuses(targetPlayer.getUniqueId(), jobId);
                bonuses.forEach(plugin.getBonusManager()::removeBonus);
            } else {
                List<MoneyBonus> bonuses = plugin.getMoneyBonusManager().getActiveBonuses(targetPlayer.getUniqueId(), jobId);
                bonuses.forEach(plugin.getMoneyBonusManager()::removeBonus);
            }
        }
    }
    
    private void handleBoostList(CommandSender sender, String boostType, String[] args) {
        String playerName = args.length > 1 ? args[1] : (sender instanceof Player ? sender.getName() : null);
        
        if (playerName == null) {
            sender.sendMessage("§cUsage: /jobs admin boost " + boostType + " list [player]");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) return;
        
        if (boostType.equals(CMD_XP)) {
            List<XpBonus> bonuses = plugin.getBonusManager().getActiveBonuses(targetPlayer.getUniqueId());
            if (bonuses.isEmpty()) {
                sender.sendMessage("§7No active XP bonuses for " + targetPlayer.getName());
                return;
            }
        } else {
            List<MoneyBonus> bonuses = plugin.getMoneyBonusManager().getActiveBonuses(targetPlayer.getUniqueId());
            if (bonuses.isEmpty()) {
                sender.sendMessage("§7No active money bonuses for " + targetPlayer.getName());
                return;
            }
        }
    }
    
    private void handleBoostInfo(CommandSender sender, String boostType) {
        sender.sendMessage("§6=== " + boostType.toUpperCase() + " Boost Information ===");
        sender.sendMessage("§7Active boost system for " + boostType + " rewards");
    }
    
    private void handleBoostCleanup(CommandSender sender, String boostType) {
        if (boostType.equals(CMD_XP)) {
            plugin.getBonusManager().cleanupExpiredBonuses();
        } else {
            plugin.getMoneyBonusManager().cleanupExpiredBonuses();
        }
        
        sender.sendMessage("§aExpired " + boostType + " bonuses cleaned up!");
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
        if (args.length <= startIndex) return "Admin boost";
        
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = startIndex; i < args.length && i < startIndex + 5; i++) {
            String part = sanitizeInput(args[i]);
            if (!part.isEmpty()) {
                if (reasonBuilder.length() > 0) reasonBuilder.append(" ");
                reasonBuilder.append(part);
            }
        }
        
        String reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : "Admin boost";
        return reason.length() > 100 ? reason.substring(0, 100) : reason;
    }
    
    private List<String> getBoostTabCompletions(String action, String[] args) {
        return switch (args.length) {
            case 5 -> getBoostCompletions5Args(action, args[4].toLowerCase());
            case 6 -> getBoostCompletions6Args(action, args[5].toLowerCase());
            case 7 -> getBoostCompletions7Args(action, args[6].toLowerCase());
            case 8 -> getBoostCompletions8Args(action, args[7].toLowerCase());
            default -> new ArrayList<>();
        };
    }
    
    private List<String> getBoostCompletions5Args(String action, String input) {
        List<String> completions = new ArrayList<>();
        
        switch (action) {
            case CMD_GIVE -> {
                completions.add("*");
                completions.addAll(getFilteredPlayerNames(input));
            }
            case CMD_REMOVE, CMD_LIST -> completions.addAll(getFilteredPlayerNames(input));
        }
        return completions;
    }
    
    private List<String> getBoostCompletions6Args(String action, String input) {
        if (!action.equals(CMD_GIVE)) return new ArrayList<>();
        
        List<String> multipliers = Arrays.asList("1.25", "1.5", "2.0", "3.0");
        return multipliers.stream()
                .filter(mult -> mult.startsWith(input))
                .collect(Collectors.toList());
    }
    
    private List<String> getBoostCompletions7Args(String action, String input) {
        if (!action.equals(CMD_GIVE)) return new ArrayList<>();
        
        List<String> durations = Arrays.asList("300", "600", "1800", "3600", "7200");
        return durations.stream()
                .filter(dur -> dur.startsWith(input))
                .collect(Collectors.toList());
    }
    
    private List<String> getBoostCompletions8Args(String action, String input) {
        if (!action.equals(CMD_GIVE)) return new ArrayList<>();
        
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
    
    private void sendBoostHelp(CommandSender sender) {
        sender.sendMessage("§6=== Boost Commands ===");
        sender.sendMessage("§e/jobs admin boost <xp|money> <action> [args...]");
        sender.sendMessage("");
        sender.sendMessage("§6Actions:");
        sender.sendMessage("§e  give <player|*> <multiplier> <duration> [job] [reason]");
        sender.sendMessage("§e  remove <player> [job]");
        sender.sendMessage("§e  list [player]");
        sender.sendMessage("§e  info");
        sender.sendMessage("§e  cleanup");
        sender.sendMessage("");
        sender.sendMessage("§7Examples:");
        sender.sendMessage("§7  /jobs admin boost xp give * 2.0 3600");
        sender.sendMessage("§7  /jobs admin boost money give Player123 1.5 1800 miner");
    }
}