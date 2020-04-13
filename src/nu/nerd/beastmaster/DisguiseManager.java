package nu.nerd.beastmaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * Tracks mob disguises on a per-world basis.
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

            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Record an entity's current disguise.
     * 
     * @param entity the entity.
     * @param world the world where the disguise will apply.
     * @param encodedDisguise the disguise encoded as a string; can be null or
     *        empty.
     */
    public void createDisguise(Entity entity, World world, String encodedDisguise) {
        if (encodedDisguise == null || encodedDisguise.isEmpty()) {
            return;
        }

        try {
            Disguise disguise = DisguiseParser.parseDisguise(Bukkit.getConsoleSender(), entity, encodedDisguise);
            createDisguise(entity, world, disguise);
        } catch (Exception ex) {
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
     * @param entity the entity.
     * @param world the world where the disguise will apply.
     * @param disguise the disguise.
     */
    public void createDisguise(Entity entity, World world, Disguise disguise) {
        getWorldDisguises(world).put(entity, disguise);
        if (BeastMaster.CONFIG.DEBUG_DISGUISES) {
            BeastMaster.PLUGIN.debug("Sending disguise in " + world.getName() + " to: " +
                                     world.getPlayers().stream()
                                     .map(Player::getName).collect(Collectors.joining(", ")));
        }
        DisguiseAPI.disguiseToPlayers(entity, disguise, world.getPlayers());
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
     * @param world the world where the disguised applied.
     */
    public void destroyDisguise(Entity entity, World world) {
        Disguise disguise = getWorldDisguises(world).remove(entity);
        if (disguise != null) {
            Bukkit.getScheduler().runTaskLater(BeastMaster.PLUGIN, () -> {
                disguise.stopDisguise();
            }, 1);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Move an entity's disguise from one world to another.
     * 
     * This method only needs to be called if the fromWorld and toWorld args are
     * different worlds.
     * 
     * @param entity the entity.
     * @param fromWorld the world where the entity used to be.
     * @param toWorld the new world where the entity exists.
     */
    public void teleportDisguise(Entity entity, World fromWorld, World toWorld) {
        if (fromWorld.equals(toWorld)) {
            // Nothing to be done when teleporting within the same world.
            return;
        }

        // We cannot call destroyDisguise(); it would call
        // DisguiseAPI.undisguiseToAll() next tick.
        Disguise disguise = getWorldDisguises(fromWorld).remove(entity);
        disguise.stopDisguise();
        getWorldDisguises(toWorld).put(entity, disguise);
        DisguiseAPI.disguiseToPlayers(entity, disguise, toWorld.getPlayers());
    }

    // ------------------------------------------------------------------------
    /**
     * Send the specified player the disguises of all disguised entities in the
     * specified world.
     */
    public void sendAllDisguises(World world, Player player) {
        // After iterating, remove invalid entities and re-index entities that
        // have moved between worlds.
        ArrayList<Entity> invalidEntities = new ArrayList<>();
        ArrayList<Entity> teleportedEntities = new ArrayList<>();

        UUID worldUuid = world.getUID();
        HashMap<Entity, Disguise> worldDisguises = getWorldDisguises(world);
        for (Map.Entry<Entity, Disguise> entry : worldDisguises.entrySet()) {
            Entity entity = entry.getKey();
            Disguise disguise = entry.getValue();

            if (entity.isValid()) {
                if (entity.getWorld().getUID().equals(worldUuid)) {
                    DisguiseAPI.disguiseToPlayers(entity, disguise, player);
                } else {
                    teleportedEntities.add(entity);
                }
            } else {
                invalidEntities.add(entity);
            }
        }

        for (Entity entity : invalidEntities) {
            destroyDisguise(entity, entity.getWorld());
        }

        for (Entity entity : teleportedEntities) {
            teleportDisguise(entity, world, entity.getWorld());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return a map from Entity to Disguise in a specified world.
     * 
     * @param world the world.
     * @return the map.
     */
    protected HashMap<Entity, Disguise> getWorldDisguises(World world) {
        UUID worldUuid = world.getUID();
        HashMap<Entity, Disguise> disguises = _worldToEntityToDisguise.get(worldUuid);
        if (disguises == null) {
            disguises = new HashMap<Entity, Disguise>();
            _worldToEntityToDisguise.put(worldUuid, disguises);
        }
        return disguises;
    }

    // ------------------------------------------------------------------------
    /**
     * A map from World UUID to a map from Entity to Disguise.
     */
    protected HashMap<UUID, HashMap<Entity, Disguise>> _worldToEntityToDisguise = new HashMap<>();

} // class DisguiseManager