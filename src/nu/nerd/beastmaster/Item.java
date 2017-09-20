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
 * There are two distinguished Items, DEFAULT and NOTHING, signifying special
 * drop behaviours.
 * 
 * @see ItemManager
 */
public class Item {
    // ------------------------------------------------------------------------
    /**
     * Special value signifying "drop the vanilla default".
     */
    public static final Item DEFAULT = new Item("DEFAULT");

    /**
     * Special value signifying "drop nothing".
     */
    public static final Item NOTHING = new Item("NOTHING");

    // ------------------------------------------------------------------------
    /**
     * Constructor for special items.
     * 
     * These items cannot be changed and is a built-in, special value.
     * 
     * @param id the ID of this item.
     */
    public Item(String id) {
        _id = id;
        _special = true;
    }

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
     * Return true if this item cannot be changed and is a built-in, special
     * value.
     * 
     * @return true if this item cannot be changed and is a built-in, special
     *         value.
     */
    public boolean isSpecial() {
        return _special;
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
     * If true, this item cannot be changed and is a built-in, special value.
     */
    protected boolean _special;

    /**
     * The dropped item stack; null for special values.
     */
    protected ItemStack _itemStack;
} // class Item