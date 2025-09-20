package fr.ax_dev.universejobs.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import fr.ax_dev.universejobs.UniverseJobs;

public class NBTItemUtils {

    private static final String NAMESPACE = "universejobs";

    public static ItemStack setStringNBT(ItemStack item, String key, String value) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(NAMESPACE, key);
        container.set(namespacedKey, PersistentDataType.STRING, value);

        item.setItemMeta(meta);
        return item;
    }

    public static String getStringNBT(ItemStack item, String key) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(NAMESPACE, key);

        return container.get(namespacedKey, PersistentDataType.STRING);
    }

    public static ItemStack setIntNBT(ItemStack item, String key, int value) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(NAMESPACE, key);
        container.set(namespacedKey, PersistentDataType.INTEGER, value);

        item.setItemMeta(meta);
        return item;
    }

    public static Integer getIntNBT(ItemStack item, String key) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(NAMESPACE, key);

        return container.get(namespacedKey, PersistentDataType.INTEGER);
    }

    public static boolean hasNBT(ItemStack item, String key) {
        if (item == null) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(NAMESPACE, key);

        return container.has(namespacedKey);
    }

    public static ItemStack removeNBT(ItemStack item, String key) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(NAMESPACE, key);
        container.remove(namespacedKey);

        item.setItemMeta(meta);
        return item;
    }
}