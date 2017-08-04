package nu.nerd.beastmaster.objectives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;

import net.md_5.bungee.api.ChatColor;
import nu.nerd.beastmaster.DropSet;

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
        return 2;
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

    // ------------------------------------------------------------------------
    /**
     * Return the set of drops to be dropped by objectives of this type.
     * 
     * @return the set of drops to be dropped by objectives of this type.
     */
    public DropSet getDropSet() {
        return _drops;
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
        if (getExtraTicks() < 0) {
            s.append(ChatColor.YELLOW).append("\n- unlimited lifetime");
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
        _schematics.clear();
        _schematics.addAll(section.getStringList("schematics"));
        _drops.load(section.getConfigurationSection("drops"), logger);
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
        section.set("schematics", _schematics);
        ConfigurationSection dropsSection = section.createSection("drops");
        _drops.save(dropsSection, logger);
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
     * The list of possible schematics used as marker blocks.
     */
    protected ArrayList<String> _schematics = new ArrayList<>(Arrays.asList("MHF_Chest"));

    /**
     * The set of drops to be dropped by objectives of this type.
     */
    protected DropSet _drops = new DropSet();

} // class ObjectiveType