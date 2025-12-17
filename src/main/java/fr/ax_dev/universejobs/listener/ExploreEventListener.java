package fr.ax_dev.universejobs.listener;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.condition.ConditionContext;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExploreEventListener implements Listener {

    private final UniverseJobs plugin;
    private final ActionProcessor actionProcessor;
    private final Map<Long, Integer> chunkExplorationCount = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastChunkMove = new ConcurrentHashMap<>();
    private static final int MAX_EXPLORATIONS = 5;
    private static final long CHUNK_COOLDOWN_MS = 1000;

    public ExploreEventListener(UniverseJobs plugin, ActionProcessor actionProcessor) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }

        Player player = event.getPlayer();
        Chunk chunk = event.getTo().getChunk();
        long chunkKey = getChunkKey(chunk);

        long currentTime = System.currentTimeMillis();
        Long lastMove = lastChunkMove.get(player.getUniqueId());
        if (lastMove != null && (currentTime - lastMove) < CHUNK_COOLDOWN_MS) {
            return;
        }

        lastChunkMove.put(player.getUniqueId(), currentTime);

        int explorationCount = chunkExplorationCount.getOrDefault(chunkKey, 0);

        if (explorationCount >= MAX_EXPLORATIONS) {
            return;
        }

        explorationCount++;
        chunkExplorationCount.put(chunkKey, explorationCount);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("ExploreEvent: " + player.getName() + " exploring chunk " +
                    chunk.getX() + "," + chunk.getZ() + " (count: " + explorationCount + ")");
        }

        ConditionContext context = new ConditionContext()
                .set("target", String.valueOf(explorationCount))
                .set("exploration_count", String.valueOf(explorationCount))
                .set("chunk_x", String.valueOf(chunk.getX()))
                .set("chunk_z", String.valueOf(chunk.getZ()))
                .set("world", chunk.getWorld().getName());

        actionProcessor.processAction(player, ActionType.EXPLORE, event, context);
    }

    private long getChunkKey(Chunk chunk) {
        return ((long) chunk.getX() << 32) | (chunk.getZ() & 0xFFFFFFFFL);
    }

    public void clearExplorationData() {
        chunkExplorationCount.clear();
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Cleared exploration data (server restart/reload)");
        }
    }
}
