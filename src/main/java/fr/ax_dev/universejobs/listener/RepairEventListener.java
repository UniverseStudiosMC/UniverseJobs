package fr.ax_dev.universejobs.listener;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.condition.ConditionContext;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RepairEventListener implements Listener {

    private final UniverseJobs plugin;
    private final ActionProcessor actionProcessor;
    private final Map<UUID, ItemStack> pendingRepairs = new HashMap<>();

    public RepairEventListener(UniverseJobs plugin, ActionProcessor actionProcessor) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getResult() == null) {
            return;
        }

        if (!(event.getView().getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getView().getPlayer();
        AnvilInventory inventory = event.getInventory();
        ItemStack firstItem = inventory.getItem(0);
        ItemStack secondItem = inventory.getItem(1);

        if (firstItem == null) {
            return;
        }

        if (firstItem.getType().getMaxDurability() > 0 && secondItem != null) {
            boolean isRepair = false;
            ItemStack result = event.getResult();

            if (result != null && result.getType() == firstItem.getType()) {
                if (firstItem.getType() == secondItem.getType()) {
                    isRepair = true;
                } else {
                    String secondItemName = secondItem.getType().name();
                    if (secondItemName.endsWith("_INGOT") ||
                        secondItemName.equals("DIAMOND") ||
                        secondItemName.equals("NETHERITE_INGOT") ||
                        secondItemName.equals("NETHERITE_SCRAP")) {
                        isRepair = true;
                    }
                }
            }

            if (isRepair) {
                pendingRepairs.put(player.getUniqueId(), result.clone());
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Pending repair for " + player.getName() + ": " + result.getType());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getInventory() == null) return;

        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        if (event.getSlot() != 2) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null) {
            return;
        }

        ItemStack pending = pendingRepairs.get(player.getUniqueId());
        if (pending == null) {
            return;
        }

        if (!result.isSimilar(pending)) {
            return;
        }

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("RepairEvent: " + player.getName() + " repaired " + result.getType());
        }

        ConditionContext context = new ConditionContext()
                .set("target", result.getType().name())
                .set("item", result.getType().name());

        actionProcessor.processAction(player, ActionType.REPAIR, event, context);

        pendingRepairs.remove(player.getUniqueId());
    }
}
