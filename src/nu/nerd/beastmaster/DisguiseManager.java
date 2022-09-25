package nu.nerd.beastmaster;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.utilities.parser.DisguiseParser;
import nu.nerd.beastmaster.mobs.MobType;

// ----------------------------------------------------------------------------
/**
 * Tracks mob disguises.
 */
public class DisguiseManager {
    // ------------------------------------------------------------------------
    /**
     * Create disguises for all disguised custom mobs in the specified chunk.
     *
     * @param chunk the chunk.
     */
    public void loadDisguises(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity) {
                MobType mobType = BeastMaster.getMobType(entity);
                if (mobType != null) {
                    if (BeastMaster.CONFIG.DEBUG_DISGUISES) {
                        BeastMaster.PLUGIN.debug("Loading " + entity.getType().name() +
                                                 " " + entity.getUniqueId().toString() +
                                                 " with MobType " + mobType.getId() +
                                                 " at " + Util.formatLocation(entity.getLocation()));
                    }
                    String encodedDisguise = (String) mobType.getDerivedProperty("disguise").getValue();
                    createDisguise(entity, chunk.getWorld(), encodedDisguise);
                }
            } else if (entity instanceof Projectile) {
                // TODO: re-disguise projectile. Does this require metadata?
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Re-send disguises of all nearby entities within +/-4 chunks from the
     * player.
     *
     * @param player the player.
     */
    public void sendNearbyDisguises(Player player) {
        Chunk centreChunk = player.getLocation().getChunk();
        World world = centreChunk.getWorld();
        for (int dx = -4; dx <= 4; ++dx) {
            for (int dz = -4; dz <= 4; ++dz) {
                Chunk chunk = world.getChunkAt(centreChunk.getX() + dx, centreChunk.getZ() + dz);
                if (chunk.isLoaded()) {
                    loadDisguises(chunk);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Record an entity's current disguise.
     *
     * @param entity          the entity.
     * @param world           the world where the disguise will apply.
     * @param encodedDisguise the disguise encoded as a string; can be null or
     *                        empty.
     */
    public void createDisguise(Entity entity, World world, String encodedDisguise) {
        if (encodedDisguise == null || encodedDisguise.isEmpty()) {
            return;
        }

        try {
            Disguise disguise = DisguiseParser.parseDisguise(Bukkit.getConsoleSender(), entity, encodedDisguise);
            createDisguise(entity, world, disguise);
        } catch (Throwable ex) {
            MobType mobType = BeastMaster.getMobType(entity);
            String mobTypeId = mobType != null ? mobType.getId() : entity.getType().name();
            Throwable cause = ex.getCause();
            BeastMaster.PLUGIN.getLogger().severe("Error applying disguise \"" + encodedDisguise +
                                                  "\" to " + mobTypeId + ": " +
                                                  (cause != null ? cause.getMessage() : ex.getMessage()));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Record an entity's current disguise.
     *
     * @param entity   the entity.
     * @param world    the world where the disguise will apply.
     * @param disguise the disguise.
     */
    public void createDisguise(Entity entity, World world, Disguise disguise) {
        if (BeastMaster.CONFIG.DEBUG_DISGUISES) {
            MobType mobType = BeastMaster.getMobType(entity);
            String mobTypeId = mobType != null ? mobType.getId() : entity.getType().name();
            BeastMaster.PLUGIN.debug("Sending disguise of " + mobTypeId + " in " + world.getName());
        }
        DisguiseAPI.disguiseToAll(entity, disguise);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the disguise associated with the specified entity.
     *
     * All players are notified of disguise removal in the subsequent tick so
     * that if the entity has died the disguise plugin has a chance to play the
     * death packet as a disguised mob.
     *
     * @param entity the entity.
     * @param world  the world where the disguised applied.
     */
    public void destroyDisguise(Entity entity, World world) {
        Bukkit.getScheduler().runTaskLater(BeastMaster.PLUGIN, () -> {
            DisguiseAPI.undisguiseToAll(entity);
        }, 1);
    }

} // class DisguiseManager