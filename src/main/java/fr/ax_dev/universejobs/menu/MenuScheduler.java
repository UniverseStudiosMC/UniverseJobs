package fr.ax_dev.universejobs.menu;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.Bukkit;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Ultra-optimized scheduler dedicated to menu operations.
 * Implements 2025 best practices for minimal thread overhead.
 */
public class MenuScheduler {

    private final UniverseJobs plugin;
    private final ScheduledExecutorService asyncExecutor;
    private final ExecutorService fastExecutor;

    // Performance counters
    private final AtomicLong tasksExecuted = new AtomicLong(0);
    private final AtomicInteger activeAsyncTasks = new AtomicInteger(0);

    // Thread pools optimized for menu operations
    private static final int ASYNC_CORE_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors() / 4);
    private static final int FAST_CORE_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() / 8);

    public MenuScheduler(UniverseJobs plugin) {
        this.plugin = plugin;

        // Async executor for heavy operations (data loading, calculations)
        this.asyncExecutor = Executors.newScheduledThreadPool(ASYNC_CORE_THREADS, r -> {
            Thread thread = new Thread(r);
            thread.setName("UniverseJobs-MenuAsync-" + thread.getId());
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY); // Don't interfere with main thread
            return thread;
        });

        // Fast executor for quick operations (cache updates, cleanup)
        this.fastExecutor = Executors.newFixedThreadPool(FAST_CORE_THREADS, r -> {
            Thread thread = new Thread(r);
            thread.setName("UniverseJobs-MenuFast-" + thread.getId());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
    }

    /**
     * Execute task asynchronously with minimal overhead.
     */
    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(() -> {
            activeAsyncTasks.incrementAndGet();
            try {
                task.run();
                tasksExecuted.incrementAndGet();
            } finally {
                activeAsyncTasks.decrementAndGet();
            }
        }, asyncExecutor);
    }

    /**
     * Execute task asynchronously and return result.
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            activeAsyncTasks.incrementAndGet();
            try {
                T result = supplier.get();
                tasksExecuted.incrementAndGet();
                return result;
            } finally {
                activeAsyncTasks.decrementAndGet();
            }
        }, asyncExecutor);
    }

    /**
     * Execute task on main thread with minimal delay.
     */
    public void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            plugin.getFoliaManager().runNextTick(task);
        }
    }

    /**
     * Execute task on main thread and return result.
     */
    public <T> CompletableFuture<T> supplySync(Supplier<T> supplier) {
        if (Bukkit.isPrimaryThread()) {
            return CompletableFuture.completedFuture(supplier.get());
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getFoliaManager().runNextTick(() -> {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Fast execution for lightweight operations.
     */
    public void runFast(Runnable task) {
        fastExecutor.execute(() -> {
            try {
                task.run();
                tasksExecuted.incrementAndGet();
            } catch (Exception e) {
                plugin.getLogger().warning("Fast task failed: " + e.getMessage());
            }
        });
    }

    /**
     * Schedule periodic task with optimal timing.
     */
    public ScheduledFuture<?> scheduleRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return asyncExecutor.scheduleAtFixedRate(() -> {
            activeAsyncTasks.incrementAndGet();
            try {
                task.run();
                tasksExecuted.incrementAndGet();
            } catch (Exception e) {
                plugin.getLogger().warning("Scheduled task failed: " + e.getMessage());
            } finally {
                activeAsyncTasks.decrementAndGet();
            }
        }, initialDelay, period, unit);
    }

    /**
     * Batch execute multiple tasks for efficiency.
     */
    public CompletableFuture<Void> runBatch(Runnable... tasks) {
        return CompletableFuture.runAsync(() -> {
            activeAsyncTasks.incrementAndGet();
            try {
                for (Runnable task : tasks) {
                    task.run();
                }
                tasksExecuted.addAndGet(tasks.length);
            } finally {
                activeAsyncTasks.decrementAndGet();
            }
        }, asyncExecutor);
    }

    /**
     * Execute with timeout for safety.
     */
    public CompletableFuture<Void> runAsyncWithTimeout(Runnable task, long timeout, TimeUnit unit) {
        return runAsync(task).orTimeout(timeout, unit);
    }

    /**
     * Get performance statistics.
     */
    public MenuSchedulerStats getStats() {
        return new MenuSchedulerStats(
            tasksExecuted.get(),
            activeAsyncTasks.get(),
            ((ThreadPoolExecutor) asyncExecutor).getActiveCount(),
            ((ThreadPoolExecutor) fastExecutor).getActiveCount()
        );
    }

    /**
     * Shutdown scheduler gracefully.
     */
    public void shutdown() {
        asyncExecutor.shutdown();
        fastExecutor.shutdown();

        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            if (!fastExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                fastExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            fastExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class MenuSchedulerStats {
        public final long totalTasksExecuted;
        public final int activeAsyncTasks;
        public final int activeAsyncThreads;
        public final int activeFastThreads;

        public MenuSchedulerStats(long totalTasksExecuted, int activeAsyncTasks, int activeAsyncThreads, int activeFastThreads) {
            this.totalTasksExecuted = totalTasksExecuted;
            this.activeAsyncTasks = activeAsyncTasks;
            this.activeAsyncThreads = activeAsyncThreads;
            this.activeFastThreads = activeFastThreads;
        }

        @Override
        public String toString() {
            return String.format("MenuScheduler{tasks=%d, async=%d, threads=%d/%d}",
                totalTasksExecuted, activeAsyncTasks, activeAsyncThreads, activeFastThreads);
        }
    }
}