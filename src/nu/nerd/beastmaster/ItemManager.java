package nu.nerd.beastmaster;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

// ----------------------------------------------------------------------------
/**
 * Manages the mapping from item name to the corresponding Item.
 * 
 * Items hold an immutable reference to an ItemStack.
 */
public class ItemManager {
    /**
     * An ItemStack signifying that nothing should be dropped.
     */
    public static final ItemStack NOTHING = new ItemStack(Material.AIR);

    /**
     * An ItemStack signifying that the default vanilla drop should be dropped.
     */
    public static final ItemStack DEFAULT = null;

    // ------------------------------------------------------------------------
    /**
     * Return the set of all defined Items.
     * 
     * @return the set of all defined Items.
     */
    public Collection<Item> getAllItems() {
        return _items.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Item with the specified ID.
     * 
     * @param id the ID of the Item to find.
     * @return the Item with the specified ID.
     */
    public Item getItem(String id) {
        return _items.get(id);
    }

    // ------------------------------------------------------------------------
    /**
     * Add an Item, defined by its ID and ItemStack.
     * 
     * @param id the ID.
     * @param itemStack the ItemStack.
     * @throws IllegalArgumentException if the ID is one of the special item
     *         IDs.
     */
    public void addItem(String id, ItemStack itemStack) {
        Item oldItem = getItem(id);
        if (oldItem != null && oldItem.isSpecial()) {
            throw new IllegalArgumentException("can't alter special item " + id);
        }

        _items.put(id, new Item(id, itemStack));
    }

    // ------------------------------------------------------------------------
    /**
     * Remove and return the Item with the specified ID.
     * 
     * @param id the Item ID.
     * @return the Item, or null if not found.
     */
    public Item removeItem(String id) {
        return _items.remove(id);
    }

    // ------------------------------------------------------------------------
    /**
     * Load all the items from the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void load(FileConfiguration config, Logger logger) {
        _items.clear();
        _items.put(Item.DEFAULT.getId(), Item.DEFAULT);
        _items.put(Item.NOTHING.getId(), Item.NOTHING);

        ConfigurationSection items = config.getConfigurationSection("items");
        for (String itemId : items.getKeys(false)) {
            try {
                addItem(itemId, items.getItemStack(itemId));
            } catch (IllegalArgumentException ex) {
                logger.severe("cannot configurespecial item " + itemId);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all the items in the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void save(FileConfiguration config, Logger logger) {
        // Create empty section to remove old items.
        ConfigurationSection itemsSection = config.createSection("items");
        for (Item item : _items.values()) {
            if (!item.isSpecial()) {
                itemsSection.set(item.getId(), item.getItemStack());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Custom items for drops.
     * 
     * Use a LinkedHashMap to preserve Item definition order when iterating.
     */
    protected HashMap<String, Item> _items = new LinkedHashMap<>();
} // class ItemManager