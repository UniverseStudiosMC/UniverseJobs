package fr.ax_dev.universejobs.protection;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.compatibility.FoliaCompatibilityManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;


public class BlockProtectionManager implements Listener {

    private static final String BLOCK_PREFIX = "block_";

    private final UniverseJobs plugin;
    private final FoliaCompatibilityManager folia;
    private boolean enabled;
    private boolean nexoEnabled;
    private Set<String> blacklist;


    public BlockProtectionManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.folia = plugin.getFoliaManager();

        loadConfiguration();
        checkNexoCompatibility();


        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    private void loadConfiguration() {
        this.enabled = plugin.getConfig().getBoolean("block-protection.enabled", true);
        this.blacklist = new HashSet<>(plugin.getConfig().getStringList("block-protection.blacklist"));
    }


    public void reloadConfig() {
        loadConfiguration();
        checkNexoCompatibility();
    }


    private void checkNexoCompatibility() {
        this.nexoEnabled = plugin.getServer().getPluginManager().isPluginEnabled("Nexo");
        if (nexoEnabled && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Nexo plugin detected - custom blocks will be tracked for anti-exploit protection");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void recordBlockPlacement(Player player, Block block) {
        if (!enabled) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Block protection disabled - not tracking block placement by " + player.getName() + " at " + block.getLocation());
            }
            return;
        }

        try {
            if (!folia.isOwnedByCurrentRegion(player)) {
                folia.runAtEntity(player, () -> recordBlockPlacement(player, block));
                return;
            }

            String playerId = player.getUniqueId().toString();
            String playerName = player.getName();

            if (!folia.isOwnedByCurrentRegion(block.getLocation())) {
                folia.runAtLocation(block.getLocation(), () -> recordBlockPlacementOwned(playerName, playerId, block));
                return;
            }

            recordBlockPlacementOwned(playerName, playerId, block);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to mark block as player-placed", e);
        }
    }

    private void recordBlockPlacementOwned(String playerName, String playerId, Block block) {
        NamespacedKey blockKey = getBlockKey(block);
        String playerData = playerId;

        if (nexoEnabled) {
            String nexoBlockId = getNexoBlockId(block);
            if (nexoBlockId != null) {
                playerData += "|NEXO:" + nexoBlockId;
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Detected Nexo custom block: " + nexoBlockId + " at " + block.getLocation());
                }
            }
        }

        block.getChunk().getPersistentDataContainer().set(blockKey, PersistentDataType.STRING, playerData);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("TRACKED block placement by " + playerName + " at " + block.getLocation());
        }
    }


    public boolean isPlayerPlacedBlock(Block block) {
        if (!enabled) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Block protection disabled - allowing XP for block at " + block.getLocation());
            }
            return false;
        }

        if (!folia.isOwnedByCurrentRegion(block.getLocation())) {
            plugin.getLogger().warning("BlockProtectionManager.isPlayerPlacedBlock called off-region for " + block.getLocation() + " - defaulting to protected=true");
            return true;
        }

        String blockType = block.getType().name();
        if (blacklist != null && blacklist.contains(blockType)) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Block type " + blockType + " is blacklisted - allowing XP");
            }
            return false;
        }

        try {
            NamespacedKey blockKey = new NamespacedKey(plugin, BLOCK_PREFIX + block.getX() + "_" + block.getY() + "_" + block.getZ());
            String placedBy = block.getChunk().getPersistentDataContainer().get(blockKey, PersistentDataType.STRING);

            if (placedBy != null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Block at " + block.getLocation() + " was placed by: " + placedBy + " - blocking XP");
                }
                return true;
            } else {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Block at " + block.getLocation() + " is natural - allowing XP");
                }
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check if block is player-placed", e);
            return false;
        }
    }


    private String getBlockPlayerData(Block block) {
        try {
            NamespacedKey blockKey = getBlockKey(block);
            return block.getChunk().getPersistentDataContainer().get(blockKey, PersistentDataType.STRING);
        } catch (Exception e) {
            return null;
        }
    }


    private void setBlockPlayerData(Block block, String playerData) {
        try {
            NamespacedKey blockKey = getBlockKey(block);
            block.getChunk().getPersistentDataContainer().set(blockKey, PersistentDataType.STRING, playerData);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set block player data", e);
        }
    }


    public void removeTrackedBlock(Block block) {
        if (!enabled) return;

        try {
            if (!folia.isOwnedByCurrentRegion(block.getLocation())) {
                folia.runAtLocation(block.getLocation(), () -> removeTrackedBlock(block));
                return;
            }

            NamespacedKey blockKey = getBlockKey(block);
            block.getChunk().getPersistentDataContainer().remove(blockKey);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Removed player-placed tag from block at " + block.getLocation());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove block tracking", e);
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!enabled) return;

        try {
            BlockFace direction = event.getDirection();
            List<Block> blocks = event.getBlocks();


            for (int i = blocks.size() - 1; i >= 0; i--) {
                Block movedBlock = blocks.get(i);

                Location oldLoc = movedBlock.getLocation();
                Location newLoc = oldLoc.clone().add(direction.getModX(), direction.getModY(), direction.getModZ());
                transferBlockTracking(oldLoc, newLoc, "pushed");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling piston extend event", e);
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!enabled) return;

        try {
            BlockFace direction = event.getDirection();
            List<Block> blocks = event.getBlocks();


            for (int i = blocks.size() - 1; i >= 0; i--) {
                Block movedBlock = blocks.get(i);

                Location oldLoc = movedBlock.getLocation();
                Location newLoc = oldLoc.clone().add(direction.getModX(), direction.getModY(), direction.getModZ());
                transferBlockTracking(oldLoc, newLoc, "retracted");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling piston retract event", e);
        }
    }

    private NamespacedKey getBlockKey(Block block) {
        return new NamespacedKey(plugin, BLOCK_PREFIX + block.getX() + "_" + block.getY() + "_" + block.getZ());
    }

    private void transferBlockTracking(Location oldLoc, Location newLoc, String action) {
        if (!folia.isOwnedByCurrentRegion(oldLoc)) {
            folia.runAtLocation(oldLoc, () -> transferBlockTracking(oldLoc, newLoc, action));
            return;
        }

        Block oldBlock = oldLoc.getBlock();
        String playerData = getBlockPlayerData(oldBlock);
        if (playerData == null) {
            return;
        }

        NamespacedKey oldKey = getBlockKey(oldBlock);
        oldBlock.getChunk().getPersistentDataContainer().remove(oldKey);

        if (folia.isOwnedByCurrentRegion(newLoc)) {
            Block newBlock = newLoc.getBlock();
            setBlockPlayerData(newBlock, playerData);
        } else {
            folia.runAtLocation(newLoc, () -> setBlockPlayerData(newLoc.getBlock(), playerData));
        }

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Piston " + action + " tracked block from " + oldLoc + " to " + newLoc);
        }
    }

    private String getNexoBlockId(Block block) {
        if (!nexoEnabled) return null;

        try {
            CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(block.getLocation());
            return mechanic != null ? mechanic.getItemID() : null;
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Failed to check Nexo block ID: " + e.getMessage());
            }
            return null;
        }
    }
}
