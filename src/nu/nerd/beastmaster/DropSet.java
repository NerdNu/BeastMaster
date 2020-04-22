package nu.nerd.beastmaster;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

// ----------------------------------------------------------------------------
/**
 * Represents a set of drops.
 */
public class DropSet {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param id the programmatic ID of this DropSet.
     */
    public DropSet(String id) {
        _id = id;
    }

    // ------------------------------------------------------------------------
    /**
     * Create a copy of another DropSet, with a different ID.
     * 
     * @param id the ID of the new DropSet.
     * @param other the DropSet whose drops and other state are copied.
     */
    public DropSet(String id, DropSet other) {
        _id = id;
        _single = other._single;
        for (Drop drop : other.getAllDrops()) {
            addDrop(drop.clone());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the programmatic ID of this DropSet.
     * 
     * @return the programmatic ID of this DropSet.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Specify whether this {@link DropSet} can drop only a single ItemStack, or
     * multiple.
     * 
     * @param single whether a single {@link Drop} is dropped.
     */
    public void setSingle(boolean single) {
        _single = single;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if only a single {@link Drop} can be dropped.
     * 
     * @return true if only a single {@link Drop} can be dropped.
     */
    public boolean isSingle() {
        return _single;
    }

    // ------------------------------------------------------------------------
    /**
     * Invalidate the cached {@link WeightedSelection} used to determine the
     * drop when this {@link DropSet#isSingle()}.
     */
    public void invalidateWeightedSelection() {
        _selectionCache = null;
    }

    // ------------------------------------------------------------------------
    /**
     * Remove all drops.
     */
    public void removeAllDrops() {
        _drops.clear();
        invalidateWeightedSelection();
    }

    // ------------------------------------------------------------------------
    /**
     * Add or replace the drop with the item ID of the specified Drop.
     * 
     * @param drop the drop.
     */
    public void addDrop(Drop drop) {
        invalidateWeightedSelection();
        _drops.put(drop.getId().toLowerCase(), drop);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the drop with the specified item ID.
     * 
     * @param itemId the item ID.
     * @return the removed drop.
     */
    public Drop removeDrop(String itemId) {
        invalidateWeightedSelection();
        return _drops.remove(itemId.toLowerCase());
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the drop with the same item ID as the specified drop from the
     * drops of this mob.
     * 
     * @param drop the drop.
     * @return the removed drop.
     */
    public Drop removeDrop(Drop drop) {
        return removeDrop(drop.getId());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the drop with the specified item ID, or null if not found.
     * 
     * @param itemId the item ID.
     * @return the drop with the specified item ID, or null if not found.
     */
    public Drop getDrop(String itemId) {
        return _drops.get(itemId.toLowerCase());
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all drops of this mob type.
     * 
     * @return a collection of all drops of this mob type.
     */
    public Collection<Drop> getAllDrops() {
        return _drops.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a map from drop to corresponding weight (for the single selection
     * case).
     * 
     * @return a map from drop to corresponding weight
     */
    public Map<Drop, Double> getDropWeights() {
        cacheWeightedSelection();

        Map<Drop, Double> weights = new TreeMap<>();
        double previousKey = 0;
        for (Entry<Double, Drop> entry : _selectionCache.entrySet()) {
            double weight = entry.getKey() - previousKey;
            previousKey = entry.getKey();
            weights.put(entry.getValue(), weight);
        }
        return weights;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the total of all spawn weights in the zone.
     * 
     * @return the total of all spawn weights in the zone.
     */
    public double getTotalWeight() {
        cacheWeightedSelection();
        return _selectionCache.getTotalWeight();
    }

    // ------------------------------------------------------------------------
    /**
     * Select one drop, as if this DropSet {@link #isSingle()}.
     * 
     * @return the {@link Drop}.
     */
    public Drop chooseOneDrop() {
        cacheWeightedSelection();
        return _selectionCache.choose();
    }

    // ------------------------------------------------------------------------
    /**
     * Generate randomly selected drops and their accompanying objectives,
     * experience orbs and sound effects.
     * 
     * @param trigger a description of the event that triggered the drop, for
     *        logging.
     * @param player the player that triggered the drop, or null.
     * @param loc the Location where items will be dropped.
     */
    public void generateRandomDrops(DropResults results, String trigger, Player player, Location loc) {
        if (isSingle()) {
            chooseOneDrop().generate(results, trigger, player, loc);

        } else {
            // An uninitialised drop table (no drops) drops vanilla items.
            // Gotcha: a call to a nested empty loot table would also be
            // affected by this. I don't think it's worth the effort to stop.
            if (_drops.isEmpty()) {
                results.setIncludesVanillaDrop();
            }

            for (Drop drop : _drops.values()) {
                if (Math.random() < drop.getDropChance()) {
                    drop.generate(results, trigger, player, loc);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Load all properties and drops from the specified configuration section,
     * with each key being an item ID.
     * 
     * @param section the section.
     * @param logger the logger for messages.
     */
    public void load(ConfigurationSection section, Logger logger) {
        _id = section.getName();
        _single = section.getBoolean("single");
        removeAllDrops();

        ConfigurationSection allDropsSection = section.getConfigurationSection("drops");
        if (allDropsSection != null) {
            for (String id : allDropsSection.getKeys(false)) {
                ConfigurationSection dropSection = allDropsSection.getConfigurationSection(id);
                Drop drop = new Drop();
                if (drop.load(dropSection, logger)) {
                    addDrop(drop);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all properties and drops to the specified section, with each item
     * corresponding to one key.
     * 
     * @param parentSection the parent configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(getId());
        section.set("single", _single);

        ConfigurationSection allDropsSection = section.createSection("drops");
        for (Drop drop : _drops.values()) {
            drop.save(allDropsSection, logger);
        }
    }

    // --------------------------------------------------------------------------
    /**
     * Return a short description of this set of drops that can be presented to
     * the user.
     * 
     * @return a short description of this set of drops that can be presented to
     *         the user.
     */
    public String getShortDescription() {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.YELLOW).append(_id);
        s.append(ChatColor.WHITE).append(": ");
        s.append(ChatColor.YELLOW).append(isSingle() ? "single" : "multiple");
        return s.toString();
    }

    // --------------------------------------------------------------------------
    /**
     * Return a description of this set of drops that can be presented to the
     * user.
     * 
     * @return a description of this set of drops that can be presented to the
     *         user.
     */
    public String getDescription() {
        StringBuilder s = new StringBuilder();
        s.append(getShortDescription());
        s.append(ChatColor.WHITE).append(" {");
        s.append(getAllDrops().stream().map(drop -> drop.getShortDescription())
        .collect(Collectors.joining(ChatColor.WHITE + ", ")));
        s.append(ChatColor.WHITE).append("}");
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Cache a {@link WeightedSelection} for use when this
     * {@link DropSet#isSingle()}.
     */
    protected void cacheWeightedSelection() {
        if (_selectionCache == null) {
            _selectionCache = new WeightedSelection<Drop>();
            for (Drop drop : _drops.values()) {
                _selectionCache.addChoice(drop, drop.getDropChance());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The programmatic ID.
     */
    protected String _id;

    /**
     * Map from lower case {@link Drop#getId()} to drop.
     */
    protected HashMap<String, Drop> _drops = new HashMap<>();

    /**
     * If _single is true (single {@link Drop} only) this member caches a
     * {@link WeightedSelection} computed from all drops. The cache is
     * invalidated by adding or removing a {@link Drop}, or explicitly calling
     * {@link #invalidateWeightedSelection()} in the case where a {@link Drop}s
     * chance is altered.
     */
    protected WeightedSelection<Drop> _selectionCache;

    /**
     * If true, only a single {@link Drop} can be selected and drop chances are
     * treated as weights in a {@link WeightedSelection}. If false, multiple
     * {@link Drop}s can be dropped, independent of one another.
     */
    protected boolean _single;

} // class DropSet