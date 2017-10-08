package nu.nerd.beastmaster;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
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
        invalidateWeightedSelection();
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
        invalidateWeightedSelection();
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
     * Generate randomly selected drops and their accompanying objectives,
     * experience orbs and sound effects.
     * 
     * @param trigger a description of the event that triggered the drop, for
     *        logging.
     * @param player the player that triggered the drop, or null.
     * @param loc the Location where items will be dropped.
     * @return true if the vanilla default drops should also be dropped by the
     *         caller.
     */
    public boolean generateRandomDrops(String trigger, Player player, Location loc) {
        if (isSingle()) {
            cacheWeightedSelection();
            Drop drop = _selectionCache.choose();
            return generateOneDrop(trigger, player, loc, drop);

        } else {
            // An uninitialised drop table (no drops) drops vanilla items.
            boolean dropDefault = _drops.isEmpty() ? true : false;
            for (Drop drop : _drops.values()) {
                if (Math.random() < drop.getDropChance()) {
                    dropDefault |= generateOneDrop(trigger, player, loc, drop);
                }
            }
            return dropDefault;
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

        _drops.clear();
        invalidateWeightedSelection();

        ConfigurationSection allDropsSection = section.getConfigurationSection("drops");
        if (allDropsSection != null) {
            for (String itemId : allDropsSection.getKeys(false)) {
                Drop drop = new Drop(itemId, 0, 0, 0);
                ConfigurationSection dropSection = allDropsSection.getConfigurationSection(itemId);
                if (drop.load(dropSection, logger)) {
                    _drops.put(itemId, drop);
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
     * Return a description of this set of drops that can be presented to the
     * user.
     * 
     * @return a description of this set of drops that can be presented to the
     *         user.
     */
    public String getDescription() {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.YELLOW).append(_id);
        s.append(ChatColor.WHITE).append(": ");
        s.append(ChatColor.YELLOW).append(isSingle() ? "single" : "multiple");
        s.append(ChatColor.WHITE).append(" {");
        s.append(getAllDrops().stream().map(drop -> drop.getShortDescription())
        .collect(Collectors.joining(ChatColor.WHITE + ", ")));
        s.append(ChatColor.WHITE).append("}");
        return s.toString();
    }

    // --------------------------------------------------------------------------
    /**
     * Do all actions associated with a {@link Drop}, including effects and XP.
     * 
     * @param trigger a description of the event that triggered the drop, for
     *        logging.
     * @param player the player that triggered the drop, or null.
     * @param loc the Location of the drop.
     * @param drop describes the drop.
     * @return true if the default vanilla drop should be dropped.
     */
    protected boolean generateOneDrop(String trigger, Player player, Location loc, Drop drop) {
        Item item = BeastMaster.ITEMS.getItem(drop.getItemId());
        if (item == Item.NOTHING) {
            return false;
        } else if (item == Item.DEFAULT) {
            return true;
        } else {
            ItemStack itemStack = drop.generate();
            boolean hasItemStack = (itemStack != null && trySpawnObjective(drop, itemStack, loc));
            if (hasItemStack) {
                if (itemStack != null && trySpawnObjective(drop, itemStack, loc)) {
                    // To avoid drops occasionally spawning in a block and
                    // warping up to the surface, wait for the next tick and
                    // check whether the block is actually air. If not air,
                    // spawn the drop at the player's feet.
                    Bukkit.getScheduler().scheduleSyncDelayedTask(BeastMaster.PLUGIN, () -> {
                        Block locBlock = loc.getBlock();
                        Location revisedLoc = (locBlock != null && locBlock.getType() != Material.AIR &&
                                               player != null) ? player.getLocation() : loc;
                        revisedLoc.getWorld().dropItemNaturally(revisedLoc, itemStack);
                    }, 1);
                }
            }

            // Play effects and drop XP if the ItemStack dropped or if no
            // ItemStack is expected.
            if (hasItemStack || itemStack == null) {
                if (drop.getExperience() > 0) {
                    ExperienceOrb orb = loc.getWorld().spawn(loc, ExperienceOrb.class);
                    orb.setExperience(drop.getExperience());
                }

                if (drop.getSound() != null) {
                    loc.getWorld().playSound(loc, drop.getSound(), drop.getSoundVolume(), drop.getSoundPitch());
                }

                if (drop.isLogged()) {
                    Logger logger = BeastMaster.PLUGIN.getLogger();
                    String count = (itemStack != null) ? " x " + itemStack.getAmount() : "";
                    logger.info(trigger + " @ " + Util.formatLocation(loc) + " --> " + drop.getItemId() + count);
                }
            }
            return false;
        }
    } // doDrop

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
     * Map from item ID to drop.
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