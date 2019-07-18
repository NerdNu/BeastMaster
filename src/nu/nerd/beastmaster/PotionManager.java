package nu.nerd.beastmaster;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

// ----------------------------------------------------------------------------
/**
 * Manages a collection of all known {@link PotionSet}s.
 * 
 * Potion set IDs are compared case-insensitively.
 */
public class PotionManager {
    // ------------------------------------------------------------------------
    /**
     * Return the {@link PotionSet} with the specified ID, or null if not found.
     * 
     * @return the {@link PotionSet} with the specified ID, or null if not
     *         found.
     */
    public PotionSet getPotionSet(String id) {
        return id != null ? _idToPotions.get(id.toLowerCase()) : null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all {@link PotionSets}.
     * 
     * @return a collection of all {@link PotionSets}.
     */
    public Collection<PotionSet> getPotionSets() {
        return _idToPotions.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all {@link PotionSets}, sorted by case-insensitive
     * ID.
     * 
     * @return a collection of all {@link PotionSets}, sorted by
     *         case-insensitive ID.
     */
    public Collection<PotionSet> getSortedPotionSets() {
        return _idToPotions.values().stream()
        .sorted((a, b) -> a.getId().compareToIgnoreCase(b.getId()))
        .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------
    /**
     * Add a new {@link PotionSet}.
     * 
     * The set should not be previously registered.
     * 
     * @param potions the set of potions.
     */
    public void addPotionSet(PotionSet potions) {
        _idToPotions.put(potions.getId().toLowerCase(), potions);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link PotionSet}.
     * 
     * @param id the ID of the potion set to remove.
     */
    public void removePotionSet(String id) {
        _idToPotions.remove(id.toLowerCase());
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link PotionSet}.
     * 
     * @param potions the potions set to remove.
     */
    public void removePotionSet(PotionSet potions) {
        removePotionSet(potions.getId());
    }

    // ------------------------------------------------------------------------
    /**
     * Load all the potion sets from the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void load(FileConfiguration config, Logger logger) {
        _idToPotions.clear();

        ConfigurationSection potionsSection = config.getConfigurationSection("potions");
        if (potionsSection == null) {
            potionsSection = config.createSection("potions");
        }

        for (String id : potionsSection.getKeys(false)) {
            ConfigurationSection section = potionsSection.getConfigurationSection(id);
            PotionSet potions = new PotionSet(id);
            potions.load(section, logger);
            addPotionSet(potions);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all the loot tables in the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void save(FileConfiguration config, Logger logger) {
        // Create section empty to remove deleted entries.
        ConfigurationSection potionsSection = config.createSection("potions");
        for (PotionSet potions : _idToPotions.values()) {
            potions.save(potionsSection, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from {@link PotionSet} lower-case ID to instance.
     */
    protected HashMap<String, PotionSet> _idToPotions = new HashMap<>();

} // class PotionManager