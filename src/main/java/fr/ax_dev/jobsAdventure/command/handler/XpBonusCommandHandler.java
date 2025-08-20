package fr.ax_dev.jobsAdventure.command.handler;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.bonus.XpBonus;
import fr.ax_dev.jobsAdventure.job.Job;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles XP bonus commands.
 */
public class XpBonusCommandHandler extends JobCommandHandler {
    
    public XpBonusCommandHandler(JobsAdventure plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "jobsadventure.admin.xpbonus")) {
            sender.sendMessage(languageManager.getMessage("commands.no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sendXpBonusHelp(sender);
            return true;
        }
        
        String xpSubCommand = args[1].toLowerCase();
        String[] xpArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (xpSubCommand) {
            case "give" -> handleXpBonusGive(sender, xpArgs);
            case "remove" -> handleXpBonusRemove(sender, xpArgs);
            case "list" -> handleXpBonusList(sender, xpArgs);
            case "info" -> handleXpBonusInfo(sender);
            case "cleanup" -> handleXpBonusCleanup(sender);
            default -> sendXpBonusHelp(sender);
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!hasPermission(sender, "jobsadventure.admin.xpbonus")) {
            return completions;
        }
        
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            
            List<String> xpSubCommands = Arrays.asList("give", "remove", "list", "info", "cleanup");
            for (String xpSubCommand : xpSubCommands) {
                if (xpSubCommand.startsWith(input)) {
                    completions.add(xpSubCommand);
                }
            }
        } else if (args.length >= 3) {
            completions.addAll(getXpBonusTabCompletions(args));
        }
        
        return completions;
    }
    
    /**
     * Handle the xpbonus give subcommand.
     */
    private void handleXpBonusGive(CommandSender sender, String[] args) {
        // /jobs xpbonus give <player|*> <multiplier> <duration> [job] [reason]
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /jobs xpbonus give <player|*> <multiplier> <duration> [job] [reason]");
            sender.sendMessage("§7Examples:");
            sender.sendMessage("§7  /jobs xpbonus give * 2.0 3600 - Give all online players 2x XP for 1 hour");
            sender.sendMessage("§7  /jobs xpbonus give Player123 1.5 1800 miner Mining Event");
            return;
        }
        
        // Validate and sanitize target
        String target = sanitizeInput(args[1]);
        if (!target.equals("*") && !isValidPlayerName(target)) {
            return;
        }
        
        // Validate multiplier with strict bounds checking
        double multiplier;
        try {
            String multiplierStr = sanitizeInput(args[2]);
            if (multiplierStr.isEmpty() || multiplierStr.length() > 10) {
                throw new NumberFormatException("Invalid multiplier format");
            }
            multiplier = Double.parseDouble(multiplierStr);
        } catch (NumberFormatException e) {
            return;
        }
        
        // Strict multiplier bounds checking with security in mind
        if (multiplier <= 0.0 || multiplier > 10.0 || Double.isNaN(multiplier) || Double.isInfinite(multiplier)) {
            return;
        }
        
        // Validate duration with strict bounds checking
        long duration;
        try {
            String durationStr = sanitizeInput(args[3]);
            if (durationStr.isEmpty() || durationStr.length() > 10) {
                throw new NumberFormatException("Invalid duration format");
            }
            duration = Long.parseLong(durationStr);
        } catch (NumberFormatException e) {
            return;
        }
        
        // Strict duration bounds checking (1 second to 24 hours)
        if (duration <= 0 || duration > 86400) {
            return;
        }
        
        // Validate job ID if provided
        String jobId = null;
        if (args.length > 4) {
            jobId = sanitizeInput(args[4]);
            if (!jobId.equals("*") && !isValidJobId(jobId)) {
                return;
            }
        }
        
        // Validate and sanitize reason
        String reason = "Admin bonus";
        if (args.length > 5) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 5; i < args.length && i < 10; i++) { // Limit to prevent abuse
                String part = sanitizeInput(args[i]);
                if (!part.isEmpty()) {
                    if (reasonBuilder.length() > 0) reasonBuilder.append(" ");
                    reasonBuilder.append(part);
                }
            }
            reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : "Admin bonus";
            
            // Limit reason length
            if (reason.length() > 100) {
                reason = reason.substring(0, 100);
            }
        }
        
        // Validate job if specified
        if (jobId != null && !jobId.equals("*")) {
            Job job = jobManager.getJob(jobId);
            if (job == null) {
                return;
            }
        }
        
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        
        if (target.equals("*")) {
            // Give to all online players
            bonusManager.addGlobalBonus(multiplier, duration, reason, senderName);
        } else {
            // Give to specific player
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                return;
            }
            
            if (jobId == null || jobId.equals("*")) {
                bonusManager.addPlayerBonus(targetPlayer.getUniqueId(), multiplier, duration, reason, senderName);
            } else {
                bonusManager.addJobBonus(targetPlayer.getUniqueId(), jobId, multiplier, duration, reason, senderName);
            }
        }
    }
    
    /**
     * Handle the xpbonus remove subcommand.
     */
    private void handleXpBonusRemove(CommandSender sender, String[] args) {
        // /jobs xpbonus remove <player> [job]
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jobs xpbonus remove <player> [job]");
            return;
        }
        
        // Validate player name
        String playerName = sanitizeInput(args[1]);
        if (!isValidPlayerName(playerName)) {
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            return;
        }
        
        // Validate job ID if provided
        String jobId = null;
        if (args.length > 2) {
            jobId = sanitizeInput(args[2]);
            if (!isValidJobId(jobId)) {
                return;
            }
        }
        
        if (jobId == null) {
            // Remove all bonuses
            bonusManager.removeAllBonuses(targetPlayer.getUniqueId());
        } else {
            // Remove job-specific bonuses
            List<XpBonus> bonuses = bonusManager.getActiveBonuses(targetPlayer.getUniqueId(), jobId);
            for (XpBonus bonus : bonuses) {
                bonusManager.removeBonus(bonus);
            }
        }
    }
    
    /**
     * Handle the xpbonus list subcommand.
     */
    private void handleXpBonusList(CommandSender sender, String[] args) {
        // /jobs xpbonus list [player]
        String playerName = args.length > 1 ? args[1] : (sender instanceof Player ? sender.getName() : null);
        
        if (playerName == null) {
            sender.sendMessage("§cUsage: /jobs xpbonus list [player]");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            return;
        }
        
        List<XpBonus> bonuses = bonusManager.getActiveBonuses(targetPlayer.getUniqueId());
        
        if (bonuses.isEmpty()) {
            return;
        }
    }
    
    /**
     * Handle the xpbonus info subcommand.
     */
    private void handleXpBonusInfo(CommandSender sender) {
        bonusManager.getStats();
    }
    
    /**
     * Handle the xpbonus cleanup subcommand.
     */
    private void handleXpBonusCleanup(CommandSender sender) {
        bonusManager.cleanupExpiredBonuses();
    }
    
    /**
     * Send XP bonus help message.
     */
    private void sendXpBonusHelp(CommandSender sender) {
        // XP bonus help messages removed for brevity
    }
    
    /**
     * Get tab completions for XP Bonus commands.
     */
    private List<String> getXpBonusTabCompletions(String[] args) {
        List<String> completions = new ArrayList<>();
        String xpSubCommand = args[1].toLowerCase();
        
        if (args.length == 3) {
            String input = args[2].toLowerCase();
            
            switch (xpSubCommand) {
                case "give" -> {
                    // Player names + *
                    completions.add("*");
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                }
                case "remove", "list" -> {
                    // Player names only
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                }
            }
        } else if (args.length == 4) {
            String input = args[3].toLowerCase();
            
            if (xpSubCommand.equals("give")) {
                // Multiplier suggestions
                List<String> multipliers = Arrays.asList("1.25", "1.5", "2.0", "3.0");
                completions.addAll(multipliers.stream()
                        .filter(mult -> mult.startsWith(input))
                        .collect(Collectors.toList()));
            } else if (xpSubCommand.equals("remove")) {
                // Job names for remove command
                completions.addAll(jobManager.getAllJobs().stream()
                        .map(Job::getId)
                        .filter(jobId -> jobId.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 5 && xpSubCommand.equals("give")) {
            String input = args[4].toLowerCase();
            // Duration suggestions
            List<String> durations = Arrays.asList("300", "600", "1800", "3600", "7200");
            completions.addAll(durations.stream()
                    .filter(dur -> dur.startsWith(input))
                    .collect(Collectors.toList()));
        } else if (args.length == 6 && xpSubCommand.equals("give")) {
            String input = args[5].toLowerCase();
            // Job names
            completions.add("*");
            completions.addAll(jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .filter(jobId -> jobId.toLowerCase().startsWith(input))
                    .collect(Collectors.toList()));
        }
        
        return completions;
    }
}