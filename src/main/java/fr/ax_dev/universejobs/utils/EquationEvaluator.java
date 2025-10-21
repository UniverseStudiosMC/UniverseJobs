package fr.ax_dev.universejobs.utils;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EquationEvaluator {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    private static final Pattern INTERNAL_PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");
    private static final long PAPI_CACHE_DURATION = 5000L;

    private static final java.util.Set<String> DYNAMIC_PLACEHOLDERS = java.util.Set.of(
        "level", "xp", "total_xp", "current_xp", "needed_xp",
        "job_count",
        "player_level", "player_exp", "player_total_exp",
        "player_health", "player_max_health", "player_food",
        "x", "y", "z"
    );

    private final UniverseJobs plugin;
    private final Map<String, CachedValue> papiCache;
    private final Map<String, Double> internalCache;

    public EquationEvaluator(UniverseJobs plugin) {
        this.plugin = plugin;
        this.papiCache = new ConcurrentHashMap<>();
        this.internalCache = new ConcurrentHashMap<>();
    }

    public double evaluate(String equation, Player player, Job job, String jobId) {
        if (equation == null || equation.isEmpty()) {
            return 0.0;
        }

        try {
            double value = Double.parseDouble(equation);
            return value;
        } catch (NumberFormatException ignored) {
        }

        String processed = processPlaceholders(equation, player, job, jobId);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Equation after placeholder processing: " + processed);
        }

        double result = evaluateExpression(processed);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Equation result: " + result);
        }

        return result;
    }

    private String processPlaceholders(String input, Player player, Job job, String jobId) {
        String result = input;

        result = processInternalPlaceholders(result, player, job, jobId);

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            result = processPlaceholderAPI(result, player);
        }

        return result;
    }

    private String processInternalPlaceholders(String input, Player player, Job job, String jobId) {
        Matcher matcher = INTERNAL_PLACEHOLDER_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            String cacheKey = player.getUniqueId() + ":" + jobId + ":" + placeholder;

            if (!DYNAMIC_PLACEHOLDERS.contains(placeholder)) {
                Double cachedValue = internalCache.get(cacheKey);
                if (cachedValue != null) {
                    matcher.appendReplacement(sb, String.valueOf(cachedValue));
                    continue;
                }
            }

            double value = getInternalPlaceholderValue(placeholder, player, job, jobId);

            if (!DYNAMIC_PLACEHOLDERS.contains(placeholder)) {
                internalCache.put(cacheKey, value);
            }

            matcher.appendReplacement(sb, String.valueOf(value));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private double getInternalPlaceholderValue(String placeholder, Player player, Job job, String jobId) {
        PlayerJobData data = plugin.getJobManager().getPlayerData(player.getUniqueId());

        return switch (placeholder) {
            case "level" -> data.getLevel(jobId);
            case "xp" -> data.getXp(jobId);
            case "total_xp" -> data.getXp(jobId);
            case "current_xp" -> {
                double[] progress = data.getXpProgress(jobId);
                yield progress[0];
            }
            case "needed_xp" -> {
                double[] progress = data.getXpProgress(jobId);
                yield progress[1];
            }
            case "max_level" -> job != null ? job.getMaxLevel() : 100;
            case "job_count" -> data.getJobs().size();
            case "player_level" -> player.getLevel();
            case "player_exp" -> player.getExp();
            case "player_total_exp" -> player.getTotalExperience();
            case "player_health" -> player.getHealth();
            case "player_max_health" -> player.getMaxHealth();
            case "player_food" -> player.getFoodLevel();
            case "x" -> player.getLocation().getX();
            case "y" -> player.getLocation().getY();
            case "z" -> player.getLocation().getZ();
            default -> 0.0;
        };
    }

    private String processPlaceholderAPI(String input, Player player) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String fullPlaceholder = matcher.group(0);
            String cacheKey = player.getUniqueId() + ":" + fullPlaceholder;

            CachedValue cached = papiCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                matcher.appendReplacement(sb, cached.value);
                continue;
            }

            String value = PlaceholderAPI.setPlaceholders(player, fullPlaceholder);
            papiCache.put(cacheKey, new CachedValue(value, System.currentTimeMillis()));
            matcher.appendReplacement(sb, value);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private double evaluateExpression(String expression) {
        try {
            expression = expression.replaceAll("\\s+", "");

            return parseExpression(expression);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to evaluate expression: " + expression + " - " + e.getMessage());
            return 0.0;
        }
    }

    private double parseExpression(String expr) {
        return parseAddSubtract(expr);
    }

    private double parseAddSubtract(String expr) {
        int level = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') level++;
            else if (c == '(') level--;
            else if (level == 0 && (c == '+' || c == '-')) {
                if (i == 0) {
                    continue;
                }
                String left = expr.substring(0, i);
                String right = expr.substring(i + 1);
                if (left.isEmpty() || right.isEmpty()) {
                    continue;
                }
                double leftVal = parseAddSubtract(left);
                double rightVal = parseMultiplyDivide(right);
                return c == '+' ? leftVal + rightVal : leftVal - rightVal;
            }
        }
        return parseMultiplyDivide(expr);
    }

    private double parseMultiplyDivide(String expr) {
        int level = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') level++;
            else if (c == '(') level--;
            else if (level == 0 && (c == '*' || c == '/')) {
                String left = expr.substring(0, i);
                String right = expr.substring(i + 1);
                double leftVal = parseMultiplyDivide(left);
                double rightVal = parsePower(right);
                return c == '*' ? leftVal * rightVal : leftVal / rightVal;
            }
        }
        return parsePower(expr);
    }

    private double parsePower(String expr) {
        int level = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') level++;
            else if (c == '(') level--;
            else if (level == 0 && c == '^') {
                String left = expr.substring(0, i);
                String right = expr.substring(i + 1);
                double leftVal = parseUnary(left);
                double rightVal = parsePower(right);
                return Math.pow(leftVal, rightVal);
            }
        }
        return parseUnary(expr);
    }

    private double parseUnary(String expr) {
        if (expr.startsWith("-")) {
            return -parseParentheses(expr.substring(1));
        }
        if (expr.startsWith("+")) {
            return parseParentheses(expr.substring(1));
        }
        return parseParentheses(expr);
    }

    private double parseParentheses(String expr) {
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return parseExpression(expr.substring(1, expr.length() - 1));
        }
        return parseNumber(expr);
    }

    private double parseNumber(String expr) {
        return Double.parseDouble(expr);
    }

    public void clearCache(UUID playerId) {
        String prefix = playerId.toString() + ":";
        papiCache.keySet().removeIf(key -> key.startsWith(prefix));
        internalCache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void clearInternalCache() {
        internalCache.clear();
    }

    public void clearExpiredPapiCache() {
        papiCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static class CachedValue {
        final String value;
        final long timestamp;

        CachedValue(String value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > PAPI_CACHE_DURATION;
        }
    }
}
