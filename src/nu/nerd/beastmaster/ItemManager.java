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
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     * 
     * Adds predefined items.
     */
    public ItemManager() {
        addPredefinedItems();
    }

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
     * If a custom item with the specified ID has not been created and the ID
     * corresponds to a vanilla Material, create a transient (not saved) Item
     * for the Material, on the fly.
     * 
     * @param id the case-insensitive ID of the Item to find.
     * @return the Item with the specified ID, or an automatically generated
     *         Item corresponding to vanilla Materials.
     */
    public Item getItem(String id) {
        Item item = _items.get(id.toLowerCase());
        if (item != null) {
            return item;
        }

        try {
            String upperId = id.toUpperCase();
            Material material = Material.valueOf(upperId);
            return new Item(upperId, new ItemStack(material));
        } catch (IllegalArgumentException ex) {
        }

        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Add an Item, defined by its ID and ItemStack.
     * 
     * @param id the case-insensitive ID.
     * @param itemStack the ItemStack.
     * @throws IllegalArgumentException if the ID is one of the special item
     *         IDs.
     */
    public void addItem(String id, ItemStack itemStack) {
        Item oldItem = getItem(id);
        if (oldItem != null && DropType.isDropType(id)) {
            throw new IllegalArgumentException("can't name an item " + id);
        }

        _items.put(id.toLowerCase(), new Item(id, itemStack));
    }

    // ------------------------------------------------------------------------
    /**
     * Remove and return the Item with the specified ID.
     * 
     * @param id the case-insensitive Item ID.
     * @return the Item, or null if not found.
     */
    public Item removeItem(String id) {
        return _items.remove(id.toLowerCase());
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
        addPredefinedItems();

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
            itemsSection.set(item.getId(), item.getItemStack());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Pre-define some items.
     */
    protected void addPredefinedItems() {
        // Define AIR as a way to clear mob equipment slots.
        addItem("AIR", new ItemStack(Material.AIR, 1));
    }

    // ------------------------------------------------------------------------
    /**
     * Custom items for drops, indexed by lower case ID.
     * 
     * Use a LinkedHashMap to preserve Item definition order when iterating.
     */
    protected HashMap<String, Item> _items = new LinkedHashMap<>();
} // class ItemManager