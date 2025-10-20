package fr.ax_dev.universejobs.listener;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.condition.ConditionContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

public class BrewEventListener implements Listener {

    private final UniverseJobs plugin;
    private final ActionProcessor actionProcessor;

    public BrewEventListener(UniverseJobs plugin, ActionProcessor actionProcessor) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!(event.getContents() instanceof BrewerInventory)) {
            return;
        }

        BrewerInventory inventory = (BrewerInventory) event.getContents();
        Player player = findNearbyPlayer(inventory);

        if (player == null) {
            return;
        }

        ItemStack ingredient = inventory.getIngredient();
        if (ingredient == null || ingredient.getType() == Material.AIR) {
            return;
        }

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("BrewEvent: " + player.getName() + " brewing with " + ingredient.getType());
        }

        plugin.getFoliaManager().runLater(() -> {
            for (int i = 0; i < 3; i++) {
                ItemStack potion = inventory.getItem(i);
                if (potion != null && potion.getType() != Material.AIR) {
                    ConditionContext context = new ConditionContext()
                            .set("target", potion.getType().name())
                            .set("potion", potion.getType().name())
                            .set("ingredient", ingredient.getType().name());

                    actionProcessor.processAction(player, ActionType.BREW, event, context);

                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Processed brew: " + potion.getType() + " for " + player.getName());
                    }
                }
            }
        }, 2L);
    }

    private Player findNearbyPlayer(BrewerInventory inventory) {
        if (inventory.getLocation() == null || inventory.getLocation().getWorld() == null) {
            return null;
        }

        return inventory.getLocation().getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(inventory.getLocation()) <= 10)
                .findFirst()
                .orElse(null);
    }
}
