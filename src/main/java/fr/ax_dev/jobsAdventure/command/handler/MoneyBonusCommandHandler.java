package fr.ax_dev.jobsAdventure.command.handler;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.bonus.MoneyBonus;
import fr.ax_dev.jobsAdventure.job.Job;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles money bonus commands.
 */
public class MoneyBonusCommandHandler extends JobCommandHandler {
    
    public MoneyBonusCommandHandler(JobsAdventure plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "jobsadventure.admin.moneybonus")) {
            sender.sendMessage(languageManager.getMessage("commands.no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sendMoneyBonusHelp(sender);
            return true;
        }
        
        String moneySubCommand = args[1].toLowerCase();
        String[] moneyArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (moneySubCommand) {
            case "give" -> handleMoneyBonusGive(sender, moneyArgs);
            case "remove" -> handleMoneyBonusRemove(sender, moneyArgs);
            case "list" -> handleMoneyBonusList(sender, moneyArgs);
            case "info" -> handleMoneyBonusInfo(sender);
            case "cleanup" -> handleMoneyBonusCleanup(sender);
            default -> sendMoneyBonusHelp(sender);
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!hasPermission(sender, "jobsadventure.admin.moneybonus")) {
            return completions;
        }
        
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            
            List<String> moneySubCommands = Arrays.asList("give", "remove", "list", "info", "cleanup");
            for (String moneySubCommand : moneySubCommands) {
                if (moneySubCommand.startsWith(input)) {
                    completions.add(moneySubCommand);
                }
            }
        } else if (args.length >= 3) {
            completions.addAll(getMoneyBonusTabCompletions(args));
        }
        
        return completions;
    }
    
    /**
     * Handle the moneybonus give subcommand.
     */
    private void handleMoneyBonusGive(CommandSender sender, String[] args) {
        // /jobs moneybonus give <player|*> <multiplier> <duration> [job] [reason]
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /jobs moneybonus give <player|*> <multiplier> <duration> [job] [reason]");
            sender.sendMessage("§7Examples:");
            sender.sendMessage("§7  /jobs moneybonus give * 2.0 3600 - Give all online players 2x money for 1 hour");
            sender.sendMessage("§7  /jobs moneybonus give Player123 1.5 1800 miner Mining Event");
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
            moneyBonusManager.addGlobalBonus(multiplier, duration, reason, senderName);
        } else {
            // Give to specific player
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                return;
            }
            
            if (jobId == null || jobId.equals("*")) {
                moneyBonusManager.addPlayerBonus(targetPlayer.getUniqueId(), multiplier, duration, reason, senderName);
            } else {
                moneyBonusManager.addJobBonus(targetPlayer.getUniqueId(), jobId, multiplier, duration, reason, senderName);
            }
        }
    }
    
    /**
     * Handle the moneybonus remove subcommand.
     */
    private void handleMoneyBonusRemove(CommandSender sender, String[] args) {
        // /jobs moneybonus remove <player> [job]
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jobs moneybonus remove <player> [job]");
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
            moneyBonusManager.removeAllBonuses(targetPlayer.getUniqueId());
        } else {
            // Remove job-specific bonuses
            List<MoneyBonus> bonuses = moneyBonusManager.getActiveBonuses(targetPlayer.getUniqueId(), jobId);
            for (MoneyBonus bonus : bonuses) {
                moneyBonusManager.removeBonus(bonus);
            }
        }
    }
    
    /**
     * Handle the moneybonus list subcommand.
     */
    private void handleMoneyBonusList(CommandSender sender, String[] args) {
        // /jobs moneybonus list [player]
        String playerName = args.length > 1 ? args[1] : (sender instanceof Player ? sender.getName() : null);
        
        if (playerName == null) {
            sender.sendMessage("§cUsage: /jobs moneybonus list [player]");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            return;
        }
        
        List<MoneyBonus> bonuses = moneyBonusManager.getActiveBonuses(targetPlayer.getUniqueId());
        
        if (bonuses.isEmpty()) {
            return;
        }
    }
    
    /**
     * Handle the moneybonus info subcommand.
     */
    private void handleMoneyBonusInfo(CommandSender sender) {
        moneyBonusManager.getStats();
    }
    
    /**
     * Handle the moneybonus cleanup subcommand.
     */
    private void handleMoneyBonusCleanup(CommandSender sender) {
        moneyBonusManager.cleanupExpiredBonuses();
    }
    
    /**
     * Send money bonus help message.
     */
    private void sendMoneyBonusHelp(CommandSender sender) {
        // Money bonus help messages removed for brevity
    }
    
    /**
     * Get tab completions for Money Bonus commands.
     */
    private List<String> getMoneyBonusTabCompletions(String[] args) {
        List<String> completions = new ArrayList<>();
        String moneySubCommand = args[1].toLowerCase();
        
        if (args.length == 3) {
            String input = args[2].toLowerCase();
            
            switch (moneySubCommand) {
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
            
            if (moneySubCommand.equals("give")) {
                // Multiplier suggestions
                List<String> multipliers = Arrays.asList("1.25", "1.5", "2.0", "3.0");
                completions.addAll(multipliers.stream()
                        .filter(mult -> mult.startsWith(input))
                        .collect(Collectors.toList()));
            } else if (moneySubCommand.equals("remove")) {
                // Job names for remove command
                completions.addAll(jobManager.getAllJobs().stream()
                        .map(Job::getId)
                        .filter(jobId -> jobId.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 5 && moneySubCommand.equals("give")) {
            String input = args[4].toLowerCase();
            // Duration suggestions
            List<String> durations = Arrays.asList("300", "600", "1800", "3600", "7200");
            completions.addAll(durations.stream()
                    .filter(dur -> dur.startsWith(input))
                    .collect(Collectors.toList()));
        } else if (args.length == 6 && moneySubCommand.equals("give")) {
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