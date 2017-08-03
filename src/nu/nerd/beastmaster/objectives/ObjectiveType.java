package nu.nerd.beastmaster.objectives;

import java.util.ArrayList;

import nu.nerd.beastmaster.DropSet;

// ----------------------------------------------------------------------------
/**
 * Describes a type of objective that can be spawned in a world by the
 * {@link ObjectiveManager}
 */
public class ObjectiveType {
    // ------------------------------------------------------------------------
    /**
     * @return
     */
    public int getMaxObjectives() {
        // TODO Auto-generated method stub
        return 0;
    }

    // --------------------------------------------------------------------------
    /**
     * @return
     */
    public int getMinRange() {
        // TODO Auto-generated method stub
        return 0;
    }

    // --------------------------------------------------------------------------
    /**
     * @return
     */
    public int getMaxRange() {
        // TODO Auto-generated method stub
        return 0;
    }

    // --------------------------------------------------------------------------
    /**
     * @return
     */
    public double getMinPlayerSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    // --------------------------------------------------------------------------
    /**
     * @return
     */
    public int getExtraTicks() {
        // TODO Auto-generated method stub
        return 0;
    }

    // --------------------------------------------------------------------------
    /**
     * @return
     */
    public ArrayList<String> getSchematics() {
        // TODO Auto-generated method stub
        return null;
    }

    // --------------------------------------------------------------------------
    /**
     * @return
     */
    public float getParticleRadius() {
        // TODO Auto-generated method stub
        return 0;
    }

    // --------------------------------------------------------------------------
    /**
     * @return
     */
    public int getParticleCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    // --------------------------------------------------------------------------
    /**
     * TODO: doc
     */
    public DropSet getDropSet() {
        return _drops;
    }

    // --------------------------------------------------------------------------

    protected DropSet _drops = new DropSet();
} // class ObjectiveType