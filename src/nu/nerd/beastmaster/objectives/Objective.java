package nu.nerd.beastmaster.objectives;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.Util;

// ----------------------------------------------------------------------------
/**
 * Represents the state of one quest objective.
 *
 * The player must travel to the objective and click on it before it despawns.
 */
public class Objective {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param objectiveType the type of this Objective.
     * @param loc the location of the objective.
     * @param lifeTicks the number of ticks this objective should live.
     */
    public Objective(ObjectiveType objectiveType, Location loc, int lifeTicks) {
        _objectiveType = objectiveType;
        _location = loc.clone();
        _lifeTicks = lifeTicks;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the objective location.
     *
     * @return the objective location.
     */
    public Location getLocation() {
        return _location;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the objective block.
     *
     * @return the objective block.
     */
    public Block getBlock() {
        return _location.getBlock();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the remaining life time in ticks.
     *
     * @return the remaining life time in ticks.
     */
    public int getLifeInTicks() {
        return _lifeTicks;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the objective is still alive (has not been found or timed
     * out).
     *
     * This method is intended to be called once per tick for each objective in
     * existence. It also updates particle effects around the objective.
     *
     * @return true if the objective is still alive (has not been found or timed
     *         out).
     */
    public boolean isAlive() {
        if (!_objectiveType.isImmortal()) {
            --_lifeTicks;
            if (_lifeTicks <= 0) {
                BeastMaster.PLUGIN.getLogger().info("Objective at " + Util.formatLocation(_location) + " timed out.");
                return false;
            }
        }

        World.Spigot spigot = _location.getWorld().spigot();
        spigot.playEffect(_location,
                          Effect.TILE_BREAK, Material.GLOWSTONE.getId(), 0,
                          _objectiveType.getParticleRadius(),
                          _objectiveType.getParticleRadius(),
                          _objectiveType.getParticleRadius(), 0,
                          _objectiveType.getParticleCount(), 16);

        for (Entity entity : _location.getWorld().getNearbyEntities(_location, 2, 2, 2)) {
            if (entity instanceof Player) {
                spawnLoot((Player) entity);
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Spawn level up sound and loot at the objective location.
     *
     * @param player the player who found the objective.
     */
    public void spawnLoot(Player player) {
        BeastMaster.PLUGIN.getLogger().info("Objective reached by " + player.getName() + " " +
                                            Util.formatLocation(getLocation()));

        World world = getLocation().getWorld();
        world.playSound(getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 3, 1);

        // TODO: drop drops per loot table.
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the objective by turning it into air.
     */
    public void vaporise() {
        getBlock().setType(Material.AIR);
    }

    // ------------------------------------------------------------------------
    /**
     * The type of this objective.
     */
    protected ObjectiveType _objectiveType;

    /**
     * Location of the objective.
     */
    protected Location _location;

    /**
     * The number of ticks this objective should live.
     */
    protected int _lifeTicks;
} // class Objective