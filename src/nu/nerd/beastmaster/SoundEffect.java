package nu.nerd.beastmaster;

import org.bukkit.Location;
import org.bukkit.Sound;

import nu.nerd.beastmaster.mobs.IDataType;

// ----------------------------------------------------------------------------
/**
 * Represents a single sound.
 */
public class SoundEffect implements Comparable<SoundEffect> {
    /**
     * Constructor.
     * 
     * @param sound the Bukkit API Sound type.
     * @param rangeMetres the audible range, or null for default (15m).
     * @param pitch the pitch, [0.5,2.0], or null for default (random).
     */
    public SoundEffect(Sound sound, Double rangeMetres, Double pitch) {
        _sound = sound;
        _rangeMetres = (rangeMetres != null) ? rangeMetres : 15.0;
        _pitch = pitch;
    }

    // ------------------------------------------------------------------------
    /**
     * Play this sound effect at the specified location.
     */
    public void play(Location loc) {
        double pitch = (_pitch != null) ? _pitch : Util.random(0.5, 2.0);
        loc.getWorld().playSound(loc, _sound, (float) _rangeMetres / 15, (float) pitch);
    }

    // ------------------------------------------------------------------------
    /**
     * Format this SoundEffect as a string.
     * 
     * The SoundEffect is formatted the same way it would be input as a mob
     * property to be parsed by its {@link IDataType} implementation.
     * 
     * @return the String.
     */
    @Override
    public String toString() {
        String pitch = (_pitch != null) ? String.format("%3.2f", _pitch) : "random";
        return String.format("%s %3.2f %s", _sound.name(), _rangeMetres, pitch);
    }

    // ------------------------------------------------------------------------
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(SoundEffect other) {
        if (this == other) {
            return 0;
        }
        int soundCmp = _sound.compareTo(other._sound);
        if (soundCmp != 0) {
            return soundCmp;
        }
        int rangeCmp = Double.compare(_rangeMetres, other._rangeMetres);
        if (rangeCmp != 0) {
            return rangeCmp;
        }

        // Treat random pitch as less than a specific pitch.
        if (_pitch == null) {
            return other._pitch == null ? 0 : -1;
        } else {
            return other._pitch == null ? 1 : Double.compare(_pitch, other._pitch);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The Bukkit API Sound type.
     */
    protected Sound _sound;

    /**
     * The audible range in metres.
     */
    protected double _rangeMetres;

    /**
     * The pitch, [0.5,2.0], or null for default (random).
     */
    protected Double _pitch;

} // class SoundEffect