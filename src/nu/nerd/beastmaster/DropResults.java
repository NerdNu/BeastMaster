package nu.nerd.beastmaster;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.LivingEntity;

// ----------------------------------------------------------------------------
/**
 * Represents the aggregate outcome of dropping one or more {@link Drop}s.
 */
public class DropResults {
    // ------------------------------------------------------------------------
    /**
     * Specify that the vanilla drop should be dropped.
     */
    void setIncludesVanillaDrop() {
        _includesVanillaDrops = true;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the default vanilla drop should be dropped.
     * 
     * @return true if the default vanilla drop should be dropped.
     */
    boolean includesVanillaDrop() {
        return _includesVanillaDrops;
    }

    // ------------------------------------------------------------------------
    /**
     * Add the specified drop to the results.
     * 
     * @param mob the mob.
     */
    public void addMob(LivingEntity mob) {
        _mobs.add(mob);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the mobs that were spawned.
     * 
     * @param the list of mobs.
     */
    public List<LivingEntity> getMobs() {
        return _mobs;
    }

    // ------------------------------------------------------------------------
    /**
     * The list of mobs that were dropped.
     */
    protected ArrayList<LivingEntity> _mobs = new ArrayList<>();

    /**
     * True of the result includes the vanilla drops.
     */
    boolean _includesVanillaDrops;
} // class DropResults