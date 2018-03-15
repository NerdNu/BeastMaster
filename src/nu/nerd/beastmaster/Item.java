package nu.nerd.beastmaster;

import org.bukkit.inventory.ItemStack;

// ----------------------------------------------------------------------------
/**
 * Represents a dropped item.
 * 
 * Items are immutable references to an ItemStack, with an immutable ID. To
 * change the association between ID and ItemStack, you must add a new Item to
 * the ItemManager.
 * 
 * @see ItemManager
 */
public class Item {
    // ------------------------------------------------------------------------
    /**
     * Constructor for regular items.
     * 
     * @param id the ID of this item.
     * @param itemStack the ItemStack.
     */
    public Item(String id, ItemStack itemStack) {
        _id = id;
        _itemStack = itemStack;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the ID of this item.
     * 
     * @return the ID of this item.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the ItemStack.
     * 
     * If the ItemStack is non-null, it will have a single item. It will be
     * cloned and the amount changed before being dropped.
     * 
     * @return the ItemStack.
     */
    public ItemStack getItemStack() {
        return _itemStack;
    }

    // ------------------------------------------------------------------------
    /**
     * The ID of this item.
     */
    protected String _id;

    /**
     * The dropped item stack; null for special values.
     */
    protected ItemStack _itemStack;
} // class Item