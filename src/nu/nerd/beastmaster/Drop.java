package nu.nerd.beastmaster;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import nu.nerd.beastmaster.mobs.MobType;
import nu.nerd.beastmaster.objectives.Objective;
import nu.nerd.beastmaster.objectives.ObjectiveType;
import nu.nerd.beastmaster.zones.Zone;

// ----------------------------------------------------------------------------
/**
 * Represents a possible item drop.
 * 
 * TODO: spawn between min and max mobs (if configured), between minR and maxR
 * and minY and maxY, and in accordance with floating (allows feet off the
 * ground). floating false and minY > highest block (at most 255) => surface.
 */
public class Drop implements Cloneable, Comparable<Drop> {
    /**
     * This represents a 100% chance of dropping nothing.
     * 
     * It's used to avoid returning null when selecting a single entry in an
     * empty single-mode DropSet (or a DropSet that is effectively empty after
     * filtering out restricted Drops.
     */
    public static Drop NOTHING = new Drop(DropType.NOTHING, "nothing", 100, 0, 0);

    // ------------------------------------------------------------------------
    /**
     * Constructor for loading from configuration only.
     */
    public Drop() {
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param dropType the type of the drop.
     * @param id the ID of the item or mob; ignored for NOTHING and DEFAULT
     *        drops.
     * @param dropChance the drop chance in the range [0.0, 1.0].
     * @param min the minimum number of drops.
     * @param max the maximum number of drops.
     */
    public Drop(DropType dropType, String id, double dropChance, int min, int max) {
        _dropType = dropType;
        if (dropType.usesId()) {
            if (dropType == DropType.ITEM) {
                // Canonicalise automatically-created vanilla material items.
                Item item = BeastMaster.ITEMS.getItem(id);
                _id = (item == null) ? id : item.getId();
            } else {
                // Currently mobs is the only other case. Canonicalise.
                MobType mobType = BeastMaster.MOBS.getMobType(id);
                _id = (mobType == null) ? id : mobType.getId();
            }
        } else {
            _id = dropType.name();
        }
        if (_dropType == DropType.ITEM) {
            _restricted = true;
        }
        _dropChance = dropChance;
        _min = min;
        _max = max;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random integer between getMinAmount() and getMaxAmount().
     * 
     * @return a random integer between getMinAmount() and getMaxAmount().
     */
    public int randomAmount() {
        return Util.random(getMinAmount(), getMaxAmount());
    }

    // ------------------------------------------------------------------------
    /**
     * Make a new random-sized ItemStack for this drop, which must be of type
     * {@link DropType#ITEM}.
     * 
     * @return the ItemStack.
     * @throws AssertionError if this is not an item Drop.
     */
    public ItemStack randomItemStack() {
        if (getDropType() != DropType.ITEM) {
            throw new AssertionError("requested an ItemStack from a non-item Drop");
        }

        Item item = BeastMaster.ITEMS.getItem(getId());
        if (item == null) {
            return null;
        }

        ItemStack itemStack = item.getItemStack();
        if (itemStack != null) {
            itemStack = itemStack.clone();
            itemStack.setAmount(randomAmount());
        }
        return itemStack;
    }

    // ------------------------------------------------------------------------
    /**
     * Do all actions associated with this drop, including effects and XP.
     * 
     * If the drop is an item that spawns an objective, then check that the
     * objective can be spawned before dropping the item.
     * 
     * @param results records some details about what was dropped.
     * @param trigger a description of the event that triggered the drop, for
     *        logging.
     * @param player the player that triggered the drop, or null.
     * @param loc the Location of the drop.
     * @return true if the default vanilla drop should be dropped.
     */
    public void generate(DropResults results, String trigger, Player player, Location loc) {
        // Invalid mob/item ID or inability to spawn objective makes drop fail.
        boolean dropSucceeded;
        String dropDescription;

        switch (getDropType()) {
        case ITEM: {
            ItemStack itemStack = randomItemStack();
            dropSucceeded = (itemStack != null && trySpawnObjective(itemStack, loc));
            if (dropSucceeded) {
                if (isDirect()) {
                    // PlayerInventory#addItem returns a HashMap detailing items
                    // that failed to add.
                    player.getInventory().addItem(itemStack).values().forEach(i -> doItemDrop(loc, player, i));
                } else {
                    doItemDrop(loc, player, itemStack);
                }
            }
            dropDescription = "ITEM " + getId() + (dropSucceeded ? " x " + itemStack.getAmount() : " (invalid)");
            break;
        }

        case MOB: {
            // TODO: Potentially mobs could spawn in block that comes back.
            // TODO: Actually need to spawn mobs around the event location.

            // Count the number of successful spawns.
            int spawnCount = 0;
            MobType mobType = BeastMaster.MOBS.getMobType(getId());
            LivingEntity livingEntity = null;
            if (mobType != null) {
                for (int i = 0; i < randomAmount(); ++i) {
                    livingEntity = BeastMaster.PLUGIN.spawnMob(loc, mobType, true);
                    if (livingEntity != null) {
                        ++spawnCount;
                        livingEntity.setInvulnerable(isInvulnerable());
                        livingEntity.setGlowing(isGlowing());
                        results.addMob(livingEntity);
                    }
                }
            }
            dropSucceeded = (spawnCount != 0);
            dropDescription = "MOB " + getId() + (dropSucceeded ? " x " + spawnCount : " (invalid)");
            break;
        }

        default: // NOTHING or DEFAULT
            dropDescription = getDropType().toString();
            dropSucceeded = true;
            break;
        }

        if (dropSucceeded) {
            dropExperience(loc);
            playSound(loc);
            if (isLogged()) {
                Logger logger = BeastMaster.PLUGIN.getLogger();
                logger.info(trigger + " @ " + Util.formatLocation(loc) + " --> " + dropDescription);
            }
        }

        // Trigger the default vanilla drop?
        if (getDropType() == DropType.DEFAULT) {
            results.setIncludesVanillaDrop();
        }
    } // generate

    // ------------------------------------------------------------------------
    /**
     * Play the sound of this drop at the specified Location.
     * 
     * @param loc the location.
     */
    public void playSound(Location loc) {
        if (_sound != null) {
            loc.getWorld().playSound(loc, _sound, _soundVolume, _soundPitch);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Drop the experience associated with this drop at the specified Location.
     * 
     * @param loc the location.
     */
    public void dropExperience(Location loc) {
        if (_experience > 0) {
            ExperienceOrb orb = loc.getWorld().spawn(loc, ExperienceOrb.class);
            orb.setExperience(_experience);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Set the type of the drop.
     * 
     * @param dropType the type of the drop.
     */
    public void setDropType(DropType dropType) {
        _dropType = dropType;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the type of the drop.
     * 
     * @return the type of the drop.
     */
    public DropType getDropType() {
        return _dropType;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the probability of this drop, in the range [0.0,1.0].
     * 
     * @param dropChance the drop chance in the range [0.0, 1.0].
     */
    public void setDropChance(double dropChance) {
        _dropChance = dropChance;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the probability of this drop, in the range [0.0,1.0].
     * 
     * @return the probability of this drop, in the range [0.0,1.0].
     */
    public double getDropChance() {
        return _dropChance;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the minimum number of items or mobs dropped together.
     * 
     * @param min the minimum number of items or mobs dropped together.
     */
    public void setMinAmount(int min) {
        _min = min;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the minimum number of items or mobs dropped together.
     * 
     * @return the minimum number of items or mobs dropped together.
     */
    public int getMinAmount() {
        return _min;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the maximum number of items or mobs dropped together.
     * 
     * @param max the maximum number of items or mobs dropped together.
     */
    public void setMaxAmount(int max) {
        _max = max;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the maximum number of items or mobs dropped together.
     * 
     * @return the maximum number of items or mobs dropped together.
     */
    public int getMaxAmount() {
        return _max;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the item or mob ID of this drop.
     * 
     * @return the item or mob ID of this drop.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Set this drop restricted.
     * 
     * Restricted drops require player involvement to occur, usually in the case
     * of a mob being killed. If the mob dies of natural causes, the restricted
     * drops are filtered out.
     * 
     * Only item drops can be set restricted. Item drops are set restricted by
     * default upon creation.
     * 
     * @param restricted whether this drop is restricted.
     */
    public void setRestricted(boolean restricted) {
        _restricted = (restricted && _dropType == DropType.ITEM);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the player must be involved in precipitating the drop for
     * it to occur.
     * 
     * Otherwise, the drop is filtered out. Only item drops can be restricted.
     * Item drops are set restricted upon creation. All other drops default to
     * false for restricted.
     * 
     * @return true if the player must be the cause of the drop for it to occur.
     */
    public boolean isRestricted() {
        return _restricted;
    }

    // ------------------------------------------------------------------------
    /**
     * Specify whether this drop should be logged to console when dropped.
     * 
     * @param logged if true, log this drop.
     */
    public void setLogged(boolean logged) {
        _logged = logged;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this drop is logged to console when dropped.
     * 
     * @return true if this drop is logged to console when dropped.
     */
    public boolean isLogged() {
        return _logged;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the type ID of the objective associated with this drop, or null if
     * this drop does not denote an objective.
     * 
     * @param objectiveType the type ID of the objective.
     */
    public void setObjectiveType(String objectiveType) {
        _objectiveType = objectiveType;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the type ID of the objective associated with this drop, or null if
     * this drop does not denote an objective.
     * 
     * @return the type ID of the objective associated with this drop, or null
     *         if this drop does not denote an objective.
     */
    public String getObjectiveType() {
        return _objectiveType;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the experience to drop.
     * 
     * @param experience the amount of XP; use 0 to not drop an orb.
     */
    public void setExperience(int experience) {
        _experience = experience;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the experience to drop, or 0 to not drop an XP orb.
     * 
     * @return the experience to drop, or 0 to not drop an XP orb.
     */
    public int getExperience() {
        return _experience;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the sound to play when this drop drops.
     * 
     * Note that a sound can be played even when the {@link Item} has a null
     * ItemStack.
     * 
     * @param sound the sound to play, or null to play nothing.
     */
    public void setSound(Sound sound) {
        _sound = sound;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the sound to play, or null for silence.
     * 
     * @return the sound to play, or null for silence.
     */
    public Sound getSound() {
        return _sound;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the sound volume.
     * 
     * @param soundVolume sets the range of the sound to approximately
     *        (15*soundVolume) blocks.
     */

    public void setSoundVolume(float soundVolume) {
        _soundVolume = soundVolume;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the volume of the sound, which is approximately the range of the
     * sound in blocks divided by 15.
     * 
     * @return the volume of the sound, which is approximately the range of the
     *         sound in blocks divided by 15.
     */
    public float getSoundVolume() {
        return _soundVolume;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the pitch of the sound.
     * 
     * @param soundPitch the pitch (playback speed) in the range [0.5,2.0].
     */
    public void setSoundPitch(float soundPitch) {
        _soundPitch = soundPitch;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the sound pitch (playback speed) in the range [0.5,2.0].
     * 
     * @return the sound pitch (playback speed) in the range [0.5,2.0].
     */
    public float getSoundPitch() {
        return _soundPitch;
    }

    // ------------------------------------------------------------------------
    /**
     * Specify whether this drop is invulnerable.
     * 
     * @param invulnerable if true, the drop is invulnerable.
     */
    public void setInvulnerable(boolean invulnerable) {
        _invulnerable = invulnerable;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this drop is invulnerable.
     * 
     * @return true if this drop is invulnerable.
     */
    public boolean isInvulnerable() {
        return _invulnerable;
    }

    // ------------------------------------------------------------------------
    /**
     * Specify whether this drop is glowing.
     * 
     * @param glowing if true, the drop is glowing.
     */
    public void setGlowing(boolean glowing) {
        _glowing = glowing;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this drop is glowing.
     * 
     * @return true if this drop is glowing.
     */
    public boolean isGlowing() {
        return _glowing;
    }

    // ------------------------------------------------------------------------
    /**
     * Specify whether this drop should be put into the player's inventory.
     *
     * @param direct if true, the drop is directly put into the player's
     *        inventory.
     */
    public void setDirect(boolean direct) {
        _directToInventory = direct;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this drop should be directly put into the player's
     * inventory.
     *
     * @return true if this drop should be directly put into the player's
     *         inventory.
     */
    public boolean isDirect() {
        return _directToInventory;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a short description of this drop, suitable for display in-line.
     * 
     * The description does not include a full explanation of the dropped item
     * or mob; only its ID.
     *
     * Example: 10% [1,3] ITEM steak (logged)
     * 
     * @return a short description of this drop, suitable for display in-line.
     */
    public String getShortDescription() {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.WHITE).append(getChanceDescription());
        s.append(ChatColor.YELLOW).append(getCountDescription());
        s.append(ChatColor.GOLD).append(_dropType).append(' ');

        if (_dropType == DropType.ITEM) {
            Item item = BeastMaster.ITEMS.getItem(_id);
            s.append((item == null) ? ChatColor.RED : ChatColor.YELLOW);
            s.append(_id);
        } else if (_dropType == DropType.MOB) {
            MobType mobType = BeastMaster.MOBS.getMobType(_id);
            s.append((mobType == null) ? ChatColor.RED : ChatColor.YELLOW);
            s.append(_id);
        }

        if (_restricted) {
            s.append(ChatColor.RED).append(" restricted");
        }

        if (_logged) {
            s.append(ChatColor.GOLD).append(" (logged)");
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a longer description of the drop ID, item, probability and count.
     * 
     * @return a longer description of the drop.
     */
    public String getLongDescription() {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.WHITE).append(getChanceDescription());
        s.append(ChatColor.YELLOW).append(getCountDescription());
        s.append(ChatColor.GOLD).append(_dropType).append(' ');

        if (_dropType == DropType.ITEM) {
            Item item = BeastMaster.ITEMS.getItem(_id);
            s.append((item == null) ? ChatColor.RED : (item.isImplicit() ? ChatColor.YELLOW : ChatColor.GREEN));
            s.append(_id);

            // Only items can have an associated objective.
            if (_objectiveType != null) {
                s.append(ChatColor.GREEN).append("(objective: ").append(_objectiveType).append(") ");
            }
        } else if (_dropType == DropType.MOB) {
            MobType mobType = BeastMaster.MOBS.getMobType(_id);
            s.append((mobType == null) ? ChatColor.RED : ChatColor.GREEN);
            s.append(_id);
        }

        if (_restricted) {
            s.append(ChatColor.RED).append(" restricted");
        }

        if (_logged) {
            s.append(ChatColor.GOLD).append(" (logged)");
        }

        if (getExperience() > 0 || getSound() != null || isInvulnerable() || isGlowing() || isDirect()) {
            s.append("\n    ");

            if (getExperience() > 0) {
                s.append(' ');
                s.append(ChatColor.GOLD).append("xp ");
                s.append(ChatColor.YELLOW).append(getExperience());
            }

            if (getSound() != null) {
                s.append(' ').append(getSoundDescription());
            }

            if (isGlowing()) {
                s.append(ChatColor.GOLD).append(" glowing");
            }

            if (isInvulnerable()) {
                s.append(ChatColor.GOLD).append(" invulnerable");
            }

            if (isDirect()) {
                s.append(ChatColor.GOLD).append(" direct-to-inv");
            }

        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a description of the chance of this drop.
     * 
     * @return a description of the chance of this drop.
     */
    public String getChanceDescription() {
        StringBuilder s = new StringBuilder();
        if (_dropChance <= 0.01) {
            int tries = (int) Math.round(1.0 / _dropChance);
            s.append("1 in ").append(tries).append(' ');
        } else {
            s.append(_dropChance * 100).append("% ");
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a string representing the number of items or mobs dropped, for
     * presentation to the user.
     * 
     * @return a string representing the number of items or mobs dropped, for
     *         presentation to the user.
     */
    public String getCountDescription() {
        StringBuilder s = new StringBuilder();
        if (_dropType == DropType.ITEM || _dropType == DropType.MOB) {
            if (_min == _max) {
                s.append(_min);
            } else {
                s.append('[').append(_min).append(',').append(_max).append(']');
            }
            s.append(' ');
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a description of the sound of this drop.
     * 
     * @return a description of the sound of this drop.
     */
    public String getSoundDescription() {
        if (getSound() == null) {
            return ChatColor.YELLOW + "none";
        } else {
            StringBuilder s = new StringBuilder();
            s.append(ChatColor.GOLD).append("sound ");
            s.append(ChatColor.YELLOW).append(getSound());
            s.append(ChatColor.GOLD).append(" range ");
            s.append(ChatColor.YELLOW).append(String.format("%1.1fm", getSoundVolume() * 15));
            s.append(ChatColor.GOLD).append(" at ");
            s.append(ChatColor.YELLOW).append(String.format("%1.1f", getSoundPitch()));
            s.append(ChatColor.GOLD).append('x');
            return s.toString();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the long description of this drop.
     * 
     * @return the long description of this drop.
     */
    @Override
    public String toString() {
        return getLongDescription();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a clone of this Drop.
     * 
     * @return a clone of this Drop.
     */
    @Override
    public Drop clone() {
        try {
            return (Drop) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Never.
            return null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Load a custom drop from a configuration file section named after the
     * custom item ID.
     * 
     * @param section the configuration section.
     * @param logger the logger.
     */
    public boolean load(ConfigurationSection section, Logger logger) {
        try {
            // Backwards compatibility: default to ITEM.
            String type = section.getString("type");
            _dropType = DropType.valueOf(type != null ? type : "ITEM");
        } catch (IllegalArgumentException ex) {
            return false;
        }
        _id = section.getName();
        // Need to default restricted to true for ITEMs in old configs.
        _restricted = section.getBoolean("restricted", _dropType == DropType.ITEM);
        _logged = section.getBoolean("logged");
        _dropChance = section.getDouble("chance", 0.0);
        _min = section.getInt("min", 1);
        _max = section.getInt("max", Math.max(1, _min));
        _objectiveType = section.getString("objective");
        _experience = section.getInt("experience");
        String soundId = section.getString("sound");
        try {
            _sound = (soundId != null && soundId.length() > 0) ? Sound.valueOf(soundId)
                                                               : null;
        } catch (IllegalArgumentException ex) {
            _sound = null;
            logger.severe("drop " + _id + " could not load invalid sound " + soundId);
        }
        _soundVolume = (float) section.getDouble("sound-volume");
        _soundPitch = (float) section.getDouble("sound-pitch");
        _invulnerable = section.getBoolean("invulnerable");
        _glowing = section.getBoolean("glowing");
        _directToInventory = section.getBoolean("direct-to-inventory");
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this drop as a child of the specified parent configuration section.
     * 
     * @param parentSection the parent configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(_id);
        section.set("type", _dropType.name());
        section.set("restricted", _restricted);
        section.set("logged", _logged);
        section.set("chance", _dropChance);
        section.set("min", _min);
        section.set("max", _max);
        section.set("objective", _objectiveType);
        section.set("experience", _experience);
        section.set("sound", (_sound != null) ? _sound.toString() : "");
        section.set("sound-volume", _soundVolume);
        section.set("sound-pitch", _soundPitch);
        section.set("invulnerable", _invulnerable);
        section.set("glowing", _glowing);
        section.set("direct-to-inventory", _directToInventory);
    }

    // ------------------------------------------------------------------------
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * 
     *      Compare by DropType enum ordinal first, then case-insensitive ID.
     */
    @Override
    public int compareTo(Drop other) {
        int compareDropType = Integer.compare(getDropType().ordinal(), other.getDropType().ordinal());
        if (compareDropType != 0) {
            return compareDropType;
        }
        return getId().compareToIgnoreCase(other.getId());
    }

    // ------------------------------------------------------------------------
    /**
     * Drops an item naturally near a player with a short delay.
     *
     * @param loc the location.
     * @param player the player.
     * @param itemStack the item.
     */
    protected void doItemDrop(Location loc, Player player, ItemStack itemStack) {
        // To avoid drops occasionally spawning in a block and warping up to the
        // surface, wait for the next tick and check whether the block is
        // actually unobstructed. We don't attempt to save the drop if e.g.
        // a mob is standing in lava, however.
        Bukkit.getScheduler().scheduleSyncDelayedTask(BeastMaster.PLUGIN, () -> {
            Block block = loc.getBlock();
            Location revisedLoc = (block != null &&
                                   !canAccomodateItemDrop(block) &&
                                   player != null) ? player.getLocation()
                                                   : loc;
            org.bukkit.entity.Item item = revisedLoc.getWorld().dropItem(revisedLoc, itemStack);
            item.setInvulnerable(isInvulnerable());
            item.setGlowing(isGlowing());
        }, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified Block can accomodate an item drop without
     * warping it up to the stratosphere.
     * 
     * We only check for some of the more common non-full-block materials.
     * 
     * @param block the Block.
     * @return true if the block is passable or not a "full" block.
     */
    protected static boolean canAccomodateItemDrop(Block block) {
        return block.isPassable() || Util.isNotFullBlock(block.getType());
    }

    // ------------------------------------------------------------------------
    /**
     * If this drop has an accompanying objective, try to spawn it.
     * 
     * @param item the generated dropped item.
     * @return true if the drop is not an objective drop, or if it is and an
     *         objective was successfully spawned. (Return false if an objective
     *         drop failed to spawn an objective.)
     */
    protected boolean trySpawnObjective(ItemStack item, Location dropLoc) {
        String objTypeId = getObjectiveType();
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
     * The type of the drop.
     */
    protected DropType _dropType;

    /**
     * The custom item or mob ID.
     */
    protected String _id;

    /**
     * If true (the default) then the player must be involved in precipitating
     * the drop for it to occur.
     * 
     * Otherwise, the drop is filtered out. Only item drops can be restricted.
     * Item drops are set restricted upon creation. All other drops default to
     * false for restricted.
     */
    protected boolean _restricted;

    /**
     * If true, this drop is logged to console when dropped.
     */
    protected boolean _logged;

    /**
     * Drop chance, [0.0,1.0].
     */
    protected double _dropChance;

    /**
     * Minimum number of items in item stack.
     */
    protected int _min;

    /**
     * Maximum number of items in item stack.
     */
    protected int _max;

    /**
     * The type ID of the objective associated with this item, or null if this
     * drop does not denote an objective.
     */
    protected String _objectiveType;

    /**
     * The amount of experience to drop, or 0 to not drop an XP orb.
     */
    protected int _experience;

    /**
     * The Sound to play when this drop drops, or null to not play anything.
     */
    protected Sound _sound;

    /**
     * The volume to play the sound. The range is about 15*_soundVolume blocks.
     */
    protected float _soundVolume;

    /**
     * The sound pitch (playback speed) in the range [0.5,2.0].
     */
    protected float _soundPitch;

    /**
     * If true, this drop is invulnerable.
     */
    protected boolean _invulnerable;

    /**
     * If true, this drop glows.
     */
    protected boolean _glowing;

    /**
     * If true, this drop will be placed directly in the player's inventory
     * instead of dropping naturally.
     */
    protected boolean _directToInventory;

} // class Drop