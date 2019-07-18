package nu.nerd.beastmaster;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;

// ----------------------------------------------------------------------------
/**
 * A potion effect and the independent probability of it being applied.
 * 
 * @see PotionSet
 */
public class ProbablePotion implements Comparable<ProbablePotion> {
    // ------------------------------------------------------------------------
    /**
     * Default constructor for deserialisation.
     */
    public ProbablePotion() {
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param potionEffect the potion effect.
     */
    public ProbablePotion(PotionEffect potionEffect) {
        this(potionEffect, 1.0);
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param potionEffect the potion effect.
     * @param chance the chance the potion is applied, in the range [0.0,1.0].
     */
    public ProbablePotion(PotionEffect potionEffect, double chance) {
        _potionEffect = potionEffect;
        _chance = chance;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the potion effect.
     * 
     * @return the potion effect.
     */
    public PotionEffect getPotionEffect() {
        return _potionEffect;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the chnace of the potion being applied, in the range [0.0,1.0].
     * 
     * @return the chnace of the potion being applied, in the range [0.0,1.0].
     */
    public double getChance() {
        return _chance;
    }

    // ------------------------------------------------------------------------
    /**
     * Apply this effect to a living entity.
     * 
     * @param entity the entity.
     */
    public void apply(LivingEntity entity) {
        if (Math.random() < _chance) {
            entity.addPotionEffect(_potionEffect, true);
        }
    }

    // --------------------------------------------------------------------------
    /**
     * Return a description of this potion.
     * 
     * @return a description of this potion.
     */
    public String getDescription() {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.YELLOW).append(String.format("%.3f", _chance * 100)).append(ChatColor.WHITE).append("% ");
        if (_potionEffect.getDuration() == Integer.MAX_VALUE) {
            s.append(ChatColor.YELLOW).append("inf ");
        } else {
            s.append(ChatColor.YELLOW).append(String.format("%.3g", _potionEffect.getDuration() / 20.0)).append(ChatColor.WHITE).append("s ");
        }
        s.append(ChatColor.YELLOW).append(_potionEffect.getType().getName()).append(' ').append(_potionEffect.getAmplifier() + 1);

        if (_potionEffect.hasParticles()) {
            s.append(ChatColor.WHITE).append(" particles");
        }
        if (_potionEffect.isAmbient()) {
            s.append(ChatColor.WHITE).append(" ambient");
        }
        if (_potionEffect.hasIcon()) {
            s.append(ChatColor.WHITE).append(" icon");
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Load a this instance from a configuration file section named after the
     * potion effect type ID.
     * 
     * @param section the configuration section.
     * @param logger the logger.
     */
    public boolean load(ConfigurationSection section, Logger logger) {
        _potionEffect = (PotionEffect) section.get("potion");
        _chance = section.getDouble("chance");
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this instance as a child of the specified parent configuration
     * section.
     * 
     * Format:
     * 
     * <pre>
     * potions: # Parent section.
     *   BLINDNESS:
     *     potion: # Bukkit serialised PotionEffect.
     *     chance: 1.0
     * </pre>
     * 
     * @param parentSection the parent configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(_potionEffect.getType().getName());
        section.set("potion", _potionEffect);
        section.set("chance", _chance);
    }

    // ------------------------------------------------------------------------
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ProbablePotion other) {
        return _potionEffect.getType().getName().compareTo(other._potionEffect.getType().getName());
    }

    // ------------------------------------------------------------------------
    /**
     * The potion effect.
     */
    PotionEffect _potionEffect;

    /**
     * Chance of this potion being applied, in the range [0.0,1.0].
     */
    double _chance;

} // class ProbablePotion