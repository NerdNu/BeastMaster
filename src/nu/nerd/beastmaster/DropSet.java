package nu.nerd.beastmaster;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import nu.nerd.beastmaster.objectives.Objective;
import nu.nerd.beastmaster.objectives.ObjectiveType;
import nu.nerd.beastmaster.zones.Zone;

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
     * Return the programmatic ID of this DropSet.
     * 
     * @return the programmatic ID of this DropSet.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Remove all drops.
     */
    public void clear() {
        _drops.clear();
    }

    // ------------------------------------------------------------------------
    /**
     * Add or replace the drop with the item ID of the specified Drop.
     * 
     * @param drop the drop.
     */
    public void addDrop(Drop drop) {
        _drops.put(drop.getItemId(), drop);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the drop with the specified item ID.
     * 
     * @param itemId the item ID.
     * @return the removed drop.
     */
    public Drop removeDrop(String itemId) {
        return _drops.remove(itemId);
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
        return removeDrop(drop.getItemId());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the drop with the specified item ID, or null if not found.
     * 
     * @param itemId the item ID.
     * @return the drop with the specified item ID, or null if not found.
     */
    public Drop getDrop(String itemId) {
        return _drops.get(itemId);
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
     * Process all drops and randomly drop them at the specified Location.
     * 
     * @param loc the Location.
     */
    public void drop(Location loc) {
        for (Drop drop : _drops.values()) {
            if (Math.random() < drop.getDropChance()) {
                ItemStack item = drop.generate();
                if (item != null && trySpawnObjective(drop, item, loc)) {
                    loc.getWorld().dropItemNaturally(loc, item);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Load all drops from the specified configuration section, with each key
     * being an item ID.
     * 
     * @param section the section.
     * @param logger the logger for messages.
     */
    public void load(ConfigurationSection section, Logger logger) {
        _id = section.getName();
        _drops.clear();
        if (section != null) {
            for (String itemId : section.getKeys(false)) {
                Drop drop = new Drop(itemId, 0, 0, 0);
                ConfigurationSection dropSection = section.getConfigurationSection(itemId);
                if (drop.load(dropSection, logger)) {
                    _drops.put(itemId, drop);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all drops to the specified section, with each item corresponding to
     * one key.
     * 
     * @param parentSection the parent configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(getId());
        for (Drop drop : _drops.values()) {
            drop.save(section, logger);
        }
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
        s.append(ChatColor.YELLOW).append(_id);
        s.append(ChatColor.WHITE).append(": [");
        s.append(getAllDrops().stream().map(drop -> drop.getShortDescription())
        .collect(Collectors.joining(ChatColor.WHITE + ", ")));
        s.append(ChatColor.WHITE).append("]");
        return s.toString();
    }

    // --------------------------------------------------------------------------
    /**
     * If a drop has an accompanying objective, try to spawn it.
     * 
     * @param drop the drop.
     * @param item the generated dropped item.
     * @return true if the drop is not an objective drop, or if it is and an
     *         objective was successfully spawned. (Return false if an objective
     *         drop failed to spawn an objective.)
     */
    protected boolean trySpawnObjective(Drop drop, ItemStack item, Location dropLoc) {
        String objTypeId = drop.getObjectiveType();
        if (objTypeId == null) {
            return true;
        }

        ObjectiveType objType = BeastMaster.OBJECTIVE_TYPES.getObjectiveType(objTypeId);
        if (objType == null) {
            return false;
        }
        Zone zone = BeastMaster.ZONES.getZone(dropLoc);
        if (zone == null) {
            return false;
        }
        Objective obj = BeastMaster.OBJECTIVES.spawnObjective(objType, zone, dropLoc);
        if (obj != null) {
            substituteObjectiveText(item, obj.getLocation());
            return true;
        } else {
            return false;
        }
    }

    // --------------------------------------------------------------------------
    /**
     * Substitute formatting parameters into the text of dropped items that are
     * for objectives.
     * 
     * Text substitution is performed on lore and book page text. The
     * substitution parameters are:
     * 
     * @param item the dropped item.
     * @param loc the location to format into text.
     */
    protected void substituteObjectiveText(ItemStack item, Location loc) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BookMeta) {
            BookMeta bookMeta = (BookMeta) meta;
            ArrayList<String> newPages = new ArrayList<>();
            for (String page : bookMeta.getPages()) {
                newPages.add(MessageFormat.format(page,
                                                  loc.getBlockX(),
                                                  loc.getBlockY(),
                                                  loc.getBlockZ()));
            }
            bookMeta.setPages(newPages);
        }

        List<String> lore = meta.getLore();
        if (lore != null && !lore.isEmpty()) {
            ArrayList<String> newLore = new ArrayList<>();
            for (String line : lore) {
                newLore.add(MessageFormat.format(line,
                                                 loc.getBlockX(),
                                                 loc.getBlockY(),
                                                 loc.getBlockZ()));
            }
            meta.setLore(newLore);
        }
        item.setItemMeta(meta);
    }

    // ------------------------------------------------------------------------
    /**
     * The programmatic ID.
     */
    protected String _id;

    /**
     * Map from item ID to drop.
     */
    protected HashMap<String, Drop> _drops = new HashMap<>();

} // class DropSet