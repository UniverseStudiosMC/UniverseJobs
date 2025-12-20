package fr.ax_dev.universejobs.placeholder;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.job.PlayerJobData;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class SelectJobPlaceholder {

    private final UniverseJobs plugin;
    private final JobManager jobManager;

    public SelectJobPlaceholder(final UniverseJobs plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        PlayerJobData data;
        try {
            data = jobManager.getPlayerData(player.getUniqueId());
        } catch (Exception e) {
            return "";
        }

        if (data == null || data.isLoading()) {
            return "";
        }

        String clean = params;

        if (clean.startsWith("select_")) {
            clean = clean.substring("select_".length());
        }

        Args args = Args.parse(clean);

        List<JobContext> jobs = buildContexts(plugin, data);

        // active filter
        if (args.filterActive()) {
            jobs.removeIf(j -> !j.isActive());
        }

        // apply sorters
        applySorters(jobs, args);

        // position required
        if (!args.hasPosition()) {
            return args.getDefaultValue();
        }

        int pos = args.getPosition();
        if (pos < 1 || pos > jobs.size()) {
            return args.getDefaultValue();
        }

        JobContext selected = jobs.get(pos - 1);
        return JobValueResolver.resolve(selected, args);
    }

    private static List<JobContext> buildContexts(UniverseJobs plugin, PlayerJobData data) {
        List<JobContext> list = new ArrayList<>();

        for (Job job : plugin.getJobManager().getAllJobs()) {
            list.add(new JobContext(job, data));
        }

        return list;
    }

    private static void applySorters(List<JobContext> jobs, Args args) {
        Comparator<JobContext> comparator = null;

        for (String raw : args.getSorters()) {
            boolean desc = raw.endsWith("_desc") || raw.endsWith(":desc");

            String key = raw
                    .replace("_desc", "")
                    .replace("_asc", "")
                    .replace(":desc", "")
                    .replace(":asc", "");

            JobSorter sorter = SorterRegistry.get(key);
            if (sorter == null) continue;

            Comparator<JobContext> next = sorter.comparator(desc);

            comparator = (comparator == null)
                    ? next
                    : comparator.thenComparing(next);
        }

        if (comparator != null) {
            jobs.sort(comparator);
        }
    }

    public static final class Args {

        private final Map<String, String> raw;

        private Args(Map<String, String> raw) {
            this.raw = raw;
        }

        /* ---------- factory ---------- */

        public static Args parse(String params) {
            Map<String, String> map = new HashMap<>();

            if (params == null || params.isBlank()) {
                return new Args(map);
            }

            String[] parts = params.contains(";")
                    ? params.split(";")
                    : params.split("_");

            for (String part : parts) {
                if (part.isBlank()) continue;

                int idx = part.indexOf('=');
                if (idx <= 0 || idx == part.length() - 1) continue;

                String key = part.substring(0, idx)
                        .trim()
                        .toLowerCase(Locale.ROOT);

                String value = part.substring(idx + 1).trim();

                if (key.isEmpty() || value.isEmpty()) continue;

                map.put(key, value);
            }

            return new Args(map);
        }

        /* ---------- raw ---------- */

        public boolean has(String key) {
            return raw.containsKey(key.toLowerCase());
        }

        public String get(String key) {
            return raw.get(key.toLowerCase());
        }

        /* ---------- typed ---------- */

        public String getString(String key, String def) {
            return raw.getOrDefault(key.toLowerCase(), def);
        }

        public int getInt(String key, int def) {
            try {
                return Integer.parseInt(raw.get(key.toLowerCase()));
            } catch (Exception ignored) {
                return def;
            }
        }

        public boolean getBoolean(String key, boolean def) {
            String v = raw.get(key.toLowerCase());
            if (v == null) return def;
            return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equals("1");
        }

        public List<String> getList(String key) {
            String v = raw.get(key.toLowerCase());
            if (v == null || v.isBlank()) return List.of();

            return Arrays.stream(v.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        /* ---------- semantic helpers ---------- */

        public boolean hasPosition() {
            return has("position");
        }

        public int getPosition() {
            return getInt("position", -1);
        }

        public boolean filterActive() {
            return getBoolean("active", false);
        }

        public String getDefaultValue() {
            return getString("default", "");
        }

        public String getValueType() {
            return getString("value", "name");
        }

        public List<String> getSorters() {
            return getList("sort");
        }
    }

    public record JobContext(
            Job job,
            PlayerJobData data
    ) {

        public String id() { return job.getId(); }
        public String name() { return job.getName(); }
        public String prefix() { return job.getPrefix(); }
        public String suffix() { return job.getSuffix(); }

        public boolean isActive() {
            return job.isEnabled() && data.hasJob(job.getId());
        }

        public int level() { return data.getLevel(job.getId()); }
        public double totalXp() { return data.getXp(job.getId()); }

        public double[] xpProgress() { return data.getXpProgress(job.getId()); }
        public double xpInLevel() { return xpProgress()[0]; }
        public double xpToNext() { return xpProgress()[1]; }

        public boolean isMaxLevel() {
            return level() >= data.getMaxLevel(job.getId());
        }

        public int maxLevel() { return data.getMaxLevel(job.getId()); }
        public boolean hasXpCurve() { return job.getXpCurve() != null; }
    }

    public interface JobSorter {
        Comparator<JobContext> comparator(boolean descending);
    }

    public static class LevelSorter implements JobSorter {
        @Override
        public Comparator<JobContext> comparator(boolean desc) {
            Comparator<JobContext> c = Comparator.comparingInt(JobContext::level);
            return desc ? c.reversed() : c;
        }
    }

    public static class XpSorter implements JobSorter {
        @Override
        public Comparator<JobContext> comparator(boolean desc) {
            Comparator<JobContext> c = Comparator.comparingDouble(JobContext::totalXp);
            return desc ? c.reversed() : c;
        }
    }

    public static class NameSorter implements JobSorter {
        @Override
        public Comparator<JobContext> comparator(boolean desc) {
            Comparator<JobContext> c = Comparator.comparing(JobContext::name, String.CASE_INSENSITIVE_ORDER);
            return desc ? c.reversed() : c;
        }
    }

    public static class XpPercentSorter implements JobSorter {

        @Override
        public Comparator<JobContext> comparator(boolean desc) {
            Comparator<JobContext> c = Comparator.comparingDouble(this::progress);
            return desc ? c.reversed() : c;
        }

        private double progress(JobContext ctx) {
            if (ctx.isMaxLevel()) return 1.0;

            double in = ctx.xpInLevel();
            double toNext = ctx.xpToNext();

            if (toNext <= 0) return 1.0;
            return in / (in + toNext);
        }
    }

    public static class ActiveSorter implements JobSorter {
        @Override
        public Comparator<JobContext> comparator(boolean desc) {
            Comparator<JobContext> c = Comparator.comparing(JobContext::isActive);
            return desc ? c.reversed() : c;
        }
    }

    public static class MaxLevelSorter implements JobSorter {
        @Override
        public Comparator<JobContext> comparator(boolean desc) {
            Comparator<JobContext> c = Comparator.comparing(JobContext::isMaxLevel);
            return desc ? c.reversed() : c;
        }
    }

    public static class IdSorter implements JobSorter {
        @Override
        public Comparator<JobContext> comparator(boolean desc) {
            Comparator<JobContext> c = Comparator.comparing(JobContext::id, String.CASE_INSENSITIVE_ORDER);
            return desc ? c.reversed() : c;
        }
    }

    public static class SorterRegistry {

        private static final Map<String, JobSorter> SORTERS = new HashMap<>();
        private static final Map<String, String> ALIASES = new HashMap<>();

        static {
            register("level", new LevelSorter(), "lvl");
            register("xp", new XpSorter(), "experience");
            register("xp_percent", new XpPercentSorter(),
                    "percent", "progress", "xp_progress");
            register("name", new NameSorter(), "jobname");
            register("active", new ActiveSorter(), "enabled");
            register("maxlevel", new MaxLevelSorter(), "cap");
            register("id", new IdSorter());
        }

        private static void register(String key, JobSorter sorter, String... aliases) {
            SORTERS.put(key, sorter);
            ALIASES.put(key, key);

            for (String alias : aliases) {
                ALIASES.put(alias.toLowerCase(), key);
            }
        }

        public static JobSorter get(String key) {
            if (key == null) return null;

            String normalized = ALIASES.get(key.toLowerCase());
            if (normalized == null) return null;

            return SORTERS.get(normalized);
        }
    }

    public enum ValueKey {
        ID,
        NAME,
        PREFIX,
        SUFFIX,
        LEVEL,
        MAXLEVEL,
        XP,
        XP_IN_LEVEL,
        XP_TO_NEXT,
        XP_PERCENT
    }

    public static final class JobValueResolver {

        private static final Map<String, ValueKey> VALUES = new HashMap<>();

        static {
            register(ValueKey.ID, "id");
            register(ValueKey.NAME, "name");
            register(ValueKey.PREFIX, "prefix");
            register(ValueKey.SUFFIX, "suffix");
            register(ValueKey.LEVEL, "level", "lvl");
            register(ValueKey.MAXLEVEL, "maxlevel", "cap");
            register(ValueKey.XP, "xp", "experience");
            register(ValueKey.XP_IN_LEVEL, "xpinlevel", "currentxp");
            register(ValueKey.XP_TO_NEXT, "xptonext", "remainingxp");
            register(ValueKey.XP_PERCENT, "progress", "percent", "xp_percent");
        }

        private static void register(ValueKey key, String... names) {
            for (String name : names) {
                VALUES.put(name.toLowerCase(), key);
            }
        }

        public static String resolve(JobContext job, Args args) {
            ValueKey key = VALUES.get(args.getValueType().toLowerCase());
            if (key == null) {
                return args.getDefaultValue();
            }

            return switch (key) {
                case ID -> job.id();
                case NAME -> job.name();
                case PREFIX -> job.prefix();
                case SUFFIX -> job.suffix();
                case LEVEL -> String.valueOf(job.level());
                case MAXLEVEL -> String.valueOf(job.maxLevel());
                case XP -> String.valueOf((long) job.totalXp());
                case XP_IN_LEVEL -> String.valueOf((long) job.xpInLevel());
                case XP_TO_NEXT -> String.valueOf((long) job.xpToNext());
                case XP_PERCENT -> {
                    if (job.isMaxLevel()) yield "100";
                    double in = job.xpInLevel();
                    double toNext = job.xpToNext();
                    if (toNext <= 0) yield "100";
                    yield String.format("%.2f", (in / (in + toNext)) * 100);
                }
            };
        }
    }

}
