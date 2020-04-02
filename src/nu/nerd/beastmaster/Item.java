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
     * @param implicit if true, this Item was implicitly defined as a reference
     *        to an ItemStack of 1 item of a vanilla Material.
     */
    public Item(String id, ItemStack itemStack, boolean implicit) {
        _id = id;
        _itemStack = itemStack;
        _implicit = implicit;
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
     * Return true if this Item was implicitly defined as a reference to an
     * ItemStack of 1 item of a vanilla Material.
     * 
     * @return true if this Item was implicitly defined as a reference to an
     *         ItemStack of 1 item of a vanilla Material.
     */
    public boolean isImplicit() {
        return _implicit;
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

    /**
     * If true, this Item was implicitly defined as a reference to an ItemStack
     * of 1 item of a vanilla Material.
     */
    boolean _implicit;
} // class Item