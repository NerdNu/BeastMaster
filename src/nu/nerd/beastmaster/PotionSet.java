package nu.nerd.beastmaster;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

// ----------------------------------------------------------------------------
/**
 * Represents a set of potions that are applied to a player or mob together.
 */
public class PotionSet {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param id the programmatic ID of this DropSet.
     */
    public PotionSet(String id) {
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
     * Randomly apply potions from this set.
     * 
     * Potion probabilities are evaluated independently.
     * 
     * @param entity the affected entity.
     */
    public void apply(LivingEntity entity) {
        for (ProbablePotion potion : _potions.values()) {
            potion.apply(entity);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the collection of all potions in the set, in arbitrary order.
     * 
     * @return the collection of all potions in the set, in arbitrary order.
     */
    public Collection<ProbablePotion> getAllPotions() {
        return _potions.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the collection of all potions in the set, sorted alphabetically by
     * potion effect type.
     * 
     * @return the collection of all potions in the set, sorted alphabetically
     *         by potion effect type.
     */
    public Collection<ProbablePotion> getSortedPotions() {
        return _potions.values().stream().sorted().collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the collection of all PotionEffectType in the set, sorted
     * alphabetically.
     * 
     * @return the collection of all PotionEffectType in the set, sorted
     *         alphabetically.
     */
    public Collection<PotionEffectType> getSortedPotionTypes() {
        return _potions.keySet().stream()
        .sorted((a, b) -> a.getName().compareTo(b.getName()))
        .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the {@link ProbablePotion} of the specified type, or null if not
     * found.
     * 
     * @param type the potion effect type.
     * @return the {@link ProbablePotion} of the specified type, or null if not
     *         found.
     */
    public ProbablePotion getPotion(PotionEffectType type) {
        return _potions.get(type);
    }

    // ------------------------------------------------------------------------
    /**
     * Add the specified potion to this set.
     * 
     * @param potion the potion.
     */
    public void addPotion(ProbablePotion potion) {
        _potions.put(potion.getPotionEffect().getType(), potion);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the specified potion from this set.
     * 
     * @param potion the potion.
     * @return the removed potion.
     */
    public ProbablePotion removePotion(ProbablePotion potion) {
        return _potions.remove(potion.getPotionEffect().getType());
    }

    // ------------------------------------------------------------------------
    /**
     * Remove and return the {@link ProbablePotion} of the specified type, or
     * null if not found.
     * 
     * @param type the potion effect type.
     * @return the {@link ProbablePotion} of the specified type, or null if not
     *         found.
     */
    public ProbablePotion removePotion(PotionEffectType type) {
        return _potions.remove(type);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove all potions.
     */
    public void removeAllPotions() {
        _potions.clear();
    }

    // --------------------------------------------------------------------------
    /**
     * Return true if this potion set is empty.
     * 
     * @return true if this potion set is empty.
     */
    public boolean isEmpty() {
        return _potions.isEmpty();
    }

    // --------------------------------------------------------------------------
    /**
     * Return a description of this
     * 
     * @return
     */
    public String getDescription() {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.YELLOW).append(_id);
        s.append(ChatColor.WHITE).append(":\n");
        s.append(getSortedPotions().stream()
        .map(potion -> "    " + potion.getDescription())
        .collect(Collectors.joining("\n")));
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Load all potions from the specified configuration section.
     * 
     * The name of the specified section is the ID of the potion set. The
     * immediate children of the section are further sections, named after the
     * potion effects in the set.
     * 
     * @param section the section.
     * @param logger the logger for messages.
     */
    public void load(ConfigurationSection section, Logger logger) {
        _id = section.getName();
        removeAllPotions();

        for (String potionEffectType : section.getKeys(false)) {
            ConfigurationSection potionSection = section.getConfigurationSection(potionEffectType);
            ProbablePotion potion = new ProbablePotion();
            if (potion.load(potionSection, logger)) {
                addPotion(potion);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all potions to the specified section.
     * 
     * The potion effect type of the potion is the sub-key.
     * 
     * @param parentSection the parent configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(getId());
        for (ProbablePotion potion : _potions.values()) {
            potion.save(section, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The programmatic ID.
     */
    protected String _id;

    /**
     * Map from potion effect type to corresponding {@link ProbablePotion}.
     * 
     * The set can only contain one potion of a particular potion effect type.
     */
    protected HashMap<PotionEffectType, ProbablePotion> _potions = new HashMap<>();

} // class PotionSet