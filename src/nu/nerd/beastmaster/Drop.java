package nu.nerd.beastmaster;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

// ----------------------------------------------------------------------------
/**
 * Represents a possible item drop.
 */
public class Drop {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param itemId the ID of the custom item.
     * @param dropChance the drop chance in the range [0.0, 1.0].
     * @param min the minimum number of drops.
     * @param max the maximum number of drops.
     */
    public Drop(String itemId, double dropChance, int min, int max) {
        _itemId = itemId;
        _dropChance = dropChance;
        _min = min;
        _max = max;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the item ID of this drop.
     * 
     * @return the item ID of this drop.
     */
    public String getItemId() {
        return _itemId;
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
     * Load a custom drop from a configuration file section named after the
     * custom item ID.
     * 
     * @param section the configuration section.
     * @param logger the logger.
     */
    public boolean load(ConfigurationSection section, Logger logger) {
        _itemId = section.getName();
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
            logger.severe("drop " + _itemId + " could not load invalid sound " + soundId);
        }
        _soundVolume = (float) section.getDouble("sound-volume");
        _soundPitch = (float) section.getDouble("sound-pitch");
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
        ConfigurationSection section = parentSection.createSection(_itemId);
        section.set("logged", _logged);
        section.set("chance", _dropChance);
        section.set("min", _min);
        section.set("max", _max);
        section.set("objective", _objectiveType);
        section.set("experience", _experience);
        section.set("sound", (_sound != null) ? _sound.toString() : "");
        section.set("sound-volume", _soundVolume);
        section.set("sound-pitch", _soundPitch);
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
     * Generate a new ItemStack by selecting a random number of items within the
     * configured range.
     *
     * @return the ItemStack.
     * @throws IllegalArgumentException if the dropped item has one of the
     *         special item IDs.
     */
    public ItemStack generate() {
        Item item = BeastMaster.ITEMS.getItem(_itemId);
        if (item == null) {
            return null;
        }

        if (item.isSpecial()) {
            throw new IllegalArgumentException("can't drop special item " + item.getId());
        }

        ItemStack result = item.getItemStack();
        if (result != null) {
            result = result.clone();
            result.setAmount(Util.random(_min, _max));
        }
        return result;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a short description of this drop, suitable for display in-line.
     * 
     * The description does not include a full explanation of the dropped item;
     * only it's ID.
     * 
     * @return a short description of this drop, suitable for display in-line.
     */
    public String getShortDescription() {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.WHITE).append(_dropChance * 100).append("% ");

        Item item = BeastMaster.ITEMS.getItem(_itemId);
        if (item == null) {
            s.append(ChatColor.RED).append(_itemId);
        } else if (item.isSpecial()) {
            s.append(ChatColor.YELLOW).append(_itemId);
        } else {
            if (_min == _max) {
                s.append(_min);
            } else {
                s.append('[').append(_min).append(',').append(_max).append(']');
            }
            s.append(' ');
            ItemStack itemStack = item.getItemStack();
            if (itemStack == null) {
                s.append(ChatColor.RED).append("nothing");
            } else {
                s.append(ChatColor.YELLOW).append(_itemId);
            }
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
        s.append(ChatColor.YELLOW).append(_itemId).append(": ");
        if (_objectiveType != null) {
            s.append(ChatColor.GREEN).append("(objective: ").append(_objectiveType).append(") ");
        }
        s.append(ChatColor.WHITE).append(_dropChance * 100).append("% ");

        Item item = BeastMaster.ITEMS.getItem(_itemId);
        if (item == null) {
            s.append(ChatColor.RED).append(_itemId);
        } else if (item.isSpecial()) {
            s.append(ChatColor.YELLOW).append(_itemId);
        } else {
            if (_min == _max) {
                s.append(_min);
            } else {
                s.append('[').append(_min).append(',').append(_max).append(']');
            }
            s.append(' ');

            ItemStack itemStack = item.getItemStack();
            s.append((itemStack == null) ? ChatColor.RED + "nothing"
                                         : ChatColor.WHITE + Util.getItemDescription(itemStack));
        }

        if (_logged) {
            s.append(ChatColor.GOLD).append(" (logged)");
        }
        return s.toString();
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
     * The custom item ID.
     */
    protected String _itemId;

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
} // class Drop