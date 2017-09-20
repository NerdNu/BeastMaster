package nu.nerd.beastmaster.objectives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

// ----------------------------------------------------------------------------
/**
 * Describes a type of objective that can be spawned in a world by the
 * {@link ObjectiveManager}
 */
public class ObjectiveType {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param id the ID of this objective type.
     */
    public ObjectiveType(String id) {
        _id = id;
        _maxCount = 30;
        _minRange = 100;
        _maxRange = 300;
        _minY = 40;
        _maxY = 255;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the ID of this objective type.
     * 
     * @return the ID of this objective type.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * The maximum number of objectives of this type in existence at any time.
     * 
     * @param maxCount the limit.
     */
    public void setMaxCount(int maxCount) {
        _maxCount = maxCount;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the maximum number of objectives of this type in existence at any
     * time.
     * 
     * @return the maximum number of objectives of this type in existence at any
     *         time.
     */
    public int getMaxCount() {
        return _maxCount;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the range of distances of objectives from the drops that spawned
     * them.
     * 
     * @param minRange the minimum range of an objective from the drop that
     *        spawned it.
     * @param maxRange the maximum range of an objective from the drop that
     *        spawned it.
     */
    public void setRange(int minRange, int maxRange) {
        _minRange = minRange;
        _maxRange = maxRange;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the minimum range of an objective from the drop that spawned it.
     * 
     * @return the minimum range of an objective from the drop that spawned it.
     */
    public int getMinRange() {
        return _minRange;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the maximum range of an objective from the drop that spawned it.
     * 
     * @return the maximum range of an objective from the drop that spawned it.
     */
    public int getMaxRange() {
        return _maxRange;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the range of Y coordinates objectives can spawn.
     * 
     * @param minY the minimum Y coordinate.
     * @param maxY the maximum Y coordinate.
     */
    public void setHeight(int minY, int maxY) {
        _minY = minY;
        _maxY = maxY;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the minimum Y coordinate at which an objective can spawn.
     * 
     * @return the minimum Y coordinate at which an objective can spawn.
     */
    public int getMinY() {
        return _minY;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the maximum Y coordinate at which an objective can spawn.
     * 
     * @return the maximum Y coordinate at which an objective can spawn.
     */
    public int getMaxY() {
        return _maxY;
    }

    // ------------------------------------------------------------------------
    /**
     * 
     * @return
     */
    public double getMinPlayerSpeed() {
        // TODO Auto-generated method stub
        return 3.0;
    }

    // ------------------------------------------------------------------------
    /**
     * @return
     */
    public int getExtraTicks() {
        // TODO Auto-generated method stub
        return -1; // Typical allowance 800. <0 => unlimited time.
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this objective lives forever (until claimed).
     * 
     * @return true if this objective lives forever (until claimed).
     */
    public boolean isImmortal() {
        return getExtraTicks() < 0;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a list of possible schematics of this objective, one of which will
     * be selected randomly to use as the objective marker.
     * 
     * TODO: configurability; add methods to add/remove schematics.
     * 
     * @return a list of possible schematics of this objective.
     */
    public ArrayList<String> getSchematics() {
        return _schematics;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the radius of spawned particles around the objective.
     * 
     * @return the radius of spawned particles around the objective.
     */
    public float getParticleRadius() {
        // TODO: configurability.
        return 0.8f;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the number of particles to spawn at the objective.
     * 
     * @return the number of particles to spawn at the objective.
     */
    public int getParticleCount() {
        // TODO: configurability.
        return 25;
    }

    // --------------------------------------------------------------------------
    /**
     * Return a brief, human-readable description of this objective type.
     * 
     * @return a brief, human-readable description of this objective type.
     */
    public String getDescription() {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.YELLOW).append(_id);
        s.append(ChatColor.WHITE).append(" at most ");
        s.append(ChatColor.YELLOW).append(_maxCount);
        s.append(ChatColor.WHITE).append(" instances between ");
        s.append(ChatColor.YELLOW).append(_minRange).append(ChatColor.WHITE).append(" and ");
        s.append(ChatColor.YELLOW).append(_maxRange).append(ChatColor.WHITE).append(" blocks");
        s.append(ChatColor.WHITE).append("\n - at Y ").append(ChatColor.YELLOW).append(_minY);
        s.append(ChatColor.WHITE).append(" to Y ").append(ChatColor.YELLOW).append(_maxY);
        if (isImmortal()) {
            s.append(ChatColor.WHITE).append(", unlimited lifetime");
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Load this objective type from the specified section.
     * 
     * @param section the configuration file section.
     * @return true if successful.
     */
    public boolean load(ConfigurationSection section, Logger logger) {
        _id = section.getName();
        _maxCount = section.getInt("max-count");
        _minRange = section.getInt("min-range");
        _maxRange = section.getInt("max-range");
        _minY = section.getInt("min-y");
        _maxY = section.getInt("max-y");
        _schematics.clear();
        _schematics.addAll(section.getStringList("schematics"));
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this objective type as a child of the specified parent configuration
     * section.
     * 
     * @param parentSection the parent configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(getId());
        section.set("max-count", _maxCount);
        section.set("min-range", _minRange);
        section.set("max-range", _maxRange);
        section.set("min-y", _minY);
        section.set("max-y", _maxY);
        section.set("schematics", _schematics);
    }

    // ------------------------------------------------------------------------
    /**
     * The ID of this objective type.
     */
    protected String _id;

    /**
     * The maximum number of objectives of this type in existence at any time.
     */
    protected int _maxCount;

    /**
     * The minimum range of an objective from the drop that spawned it.
     */
    protected int _minRange;

    /**
     * The maximum range of an objective from the drop that spawned it.
     */
    protected int _maxRange;

    /**
     * Minimum Y at which an objective can spawn.
     */
    protected int _minY;

    /**
     * Maximum Y at which an objective can spawn.
     */
    protected int _maxY;

    /**
     * The list of possible schematics used as marker blocks.
     */
    protected ArrayList<String> _schematics = new ArrayList<>(Arrays.asList("MHF_Chest"));

    // TODO: implement a reference to loot table.

} // class ObjectiveType