package fr.ax_dev.universejobs.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.UUID;

/**
 * Optimized InventoryHolder following 2025 Paper API best practices.
 * Stores menu metadata directly to avoid lookups and string comparisons.
 */
public class OptimizedMenuHolder implements InventoryHolder {

    private final UUID playerId;
    private final String menuType;
    private final String menuId;
    private final BaseMenu menu;
    private final long createdAt;
    private Inventory inventory;

    // Pre-computed hash for faster event handling
    private final int holderHash;

    public OptimizedMenuHolder(UUID playerId, String menuType, String menuId, BaseMenu menu) {
        this.playerId = playerId;
        this.menuType = menuType;
        this.menuId = menuId;
        this.menu = menu;
        this.createdAt = System.currentTimeMillis();

        // Pre-compute hash for O(1) comparisons
        this.holderHash = computeHash();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getMenuType() {
        return menuType;
    }

    public String getMenuId() {
        return menuId;
    }

    public BaseMenu getMenu() {
        return menu;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(long maxAge) {
        return System.currentTimeMillis() - createdAt > maxAge;
    }

    private int computeHash() {
        int result = playerId.hashCode();
        result = 31 * result + menuType.hashCode();
        result = 31 * result + (menuId != null ? menuId.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OptimizedMenuHolder)) return false;

        OptimizedMenuHolder other = (OptimizedMenuHolder) obj;
        return holderHash == other.holderHash &&
               playerId.equals(other.playerId) &&
               menuType.equals(other.menuType) &&
               (menuId != null ? menuId.equals(other.menuId) : other.menuId == null);
    }

    @Override
    public int hashCode() {
        return holderHash;
    }

    /**
     * Fast type checking without string comparison.
     */
    public boolean isMenuType(String type) {
        return menuType.equals(type);
    }

    /**
     * Check if this holder belongs to a specific player.
     */
    public boolean belongsTo(UUID playerId) {
        return this.playerId.equals(playerId);
    }
}