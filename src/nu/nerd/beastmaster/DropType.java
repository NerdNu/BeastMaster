package nu.nerd.beastmaster;

// ----------------------------------------------------------------------------
/**
 * Distinguishes the various types of actions that can be performed by a
 * {@link DropAction} when a {@link Drop} is selected in a {@link DropSet}.
 */
public enum DropType {
    /**
     * Signifies that nothing should be dropped.
     */
    NOTHING(false),

    /**
     * Signifies that the default drop appropriate to the context should occur.
     * 
     * If the drop was triggered by a block break event, then the default items
     * will be dropped. If the drop was triggered by a mob spawn event, the mob
     * will spawn unchanged.
     */
    DEFAULT(false),

    /**
     * Signifies that the {@link Drop}'s ID is that of an item to be dropped.
     */
    ITEM(true),

    /**
     * Signifies that the {@link Drop}'s ID is that of a mob to be spawned.
     */
    MOB(true);

    // ------------------------------------------------------------------------
    /**
     * Return true if this drop type needs an associated ID (for the item or mob
     * type).
     * 
     * @return true if this drop type needs an associated ID (for the item or
     *         mob type).
     */
    public boolean usesId() {
        return _usesId;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified ID is reserved, and so cannot be used for
     * items or mobs.
     * 
     * @eturn true if the specified ID is reserved.
     */
    public static boolean isDropType(String id) {
        for (DropType type : values()) {
            if (type.name().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param usesId true if this drop type needs an associated ID (for the item
     *        or mob type).
     */
    private DropType(boolean usesId) {
        _usesId = usesId;
    }

    // ------------------------------------------------------------------------
    /**
     * True if this drop type needs an associated ID (for the item or mob type).
     */
    private boolean _usesId;
} // class DropType