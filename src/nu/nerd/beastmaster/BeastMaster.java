package nu.nerd.beastmaster;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import net.sothatsit.blockstore.BlockStoreApi;
import nu.nerd.beastmaster.commands.BeastItemExecutor;
import nu.nerd.beastmaster.commands.BeastLootExecutor;
import nu.nerd.beastmaster.commands.BeastMasterExecutor;
import nu.nerd.beastmaster.commands.BeastMobExecutor;
import nu.nerd.beastmaster.commands.BeastObjectiveExecutor;
import nu.nerd.beastmaster.commands.BeastPotionExecutor;
import nu.nerd.beastmaster.commands.BeastZoneExecutor;
import nu.nerd.beastmaster.commands.ExecutorBase;
import nu.nerd.beastmaster.mobs.MobProperty;
import nu.nerd.beastmaster.mobs.MobType;
import nu.nerd.beastmaster.mobs.MobTypeManager;
import nu.nerd.beastmaster.objectives.Objective;
import nu.nerd.beastmaster.objectives.ObjectiveManager;
import nu.nerd.beastmaster.objectives.ObjectiveTypeManager;
import nu.nerd.beastmaster.zones.Zone;
import nu.nerd.beastmaster.zones.ZoneManager;
import nu.nerd.entitymeta.EntityMeta;

// ----------------------------------------------------------------------------
/**
 * Plugin, command handling and event handler class.
 */
public class BeastMaster extends JavaPlugin implements Listener {
    // ------------------------------------------------------------------------
    /**
     * Configuration wrapper instance.
     */
    public static final Configuration CONFIG = new Configuration();

    /**
     * This plugin, accessible as, effectively, a singleton.
     */
    public static BeastMaster PLUGIN;

    /**
     * Zone manager as a singleton.
     */
    public static final ZoneManager ZONES = new ZoneManager();

    /**
     * Item manager as a singleton.
     */
    public static final ItemManager ITEMS = new ItemManager();

    /**
     * Mob type manager as a singleton.
     */
    public static final MobTypeManager MOBS = new MobTypeManager();

    /**
     * Loot table manager as a singleton.
     */
    public static final LootManager LOOTS = new LootManager();

    /**
     * Manages all objectives.
     */
    public static final ObjectiveManager OBJECTIVES = new ObjectiveManager();

    /**
     * Manages all objective type.
     */
    public static final ObjectiveTypeManager OBJECTIVE_TYPES = new ObjectiveTypeManager();

    /**
     * Manages all potion sets.
     */
    public static final PotionManager POTIONS = new PotionManager();

    /**
     * Keeps track of all disguised mobs.
     */
    public static final DisguiseManager DISGUISES = new DisguiseManager();

    // ------------------------------------------------------------------------
    /**
     * Log a debug message.
     *
     * @param message the message.
     */
    public void debug(String message) {
        getLogger().info("[DEBUG] " + message);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;
        saveDefaultConfig();
        CONFIG.reload(false);

        addCommandExecutor(new BeastMasterExecutor());
        addCommandExecutor(new BeastZoneExecutor());
        addCommandExecutor(new BeastItemExecutor());
        addCommandExecutor(new BeastLootExecutor());
        addCommandExecutor(new BeastMobExecutor());
        addCommandExecutor(new BeastObjectiveExecutor());
        addCommandExecutor(new BeastPotionExecutor());

        getServer().getPluginManager().registerEvents(this, this);

        // Every tick, do particle effects for objectives.
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                OBJECTIVES.tickAll();
            }
        }, 1, 1);

        OBJECTIVES.extractSchematics();

        // Since we can't rely on ChunkLoadEvent or WorldLoadEvent to tell us
        // when chunks containing disguised mobs load at startup, let's
        // process all loaded chunks here.
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                DISGUISES.loadDisguises(chunk);
            }
        }
    } // onEnable

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        OBJECTIVES.removeAll();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the MobType of the specified entity.
     *
     * @param entity the entity.
     * @return the MobType, or null if the entity is not living, or has no
     *         custom MobType. Note that generally vanilla mobs do have one even
     *         if it has not custom properties.
     */
    public static MobType getMobType(Entity entity) {
        if (entity instanceof LivingEntity) {
            String mobTypeId = (String) EntityMeta.api().get(entity, BeastMaster.PLUGIN, "mob-type");
            return MOBS.getMobType(mobTypeId);
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Spawn a mob of the specified custom mob type.
     *
     * @param loc         the Location where the mob spawns.
     * @param mobType     the custom mob type.
     * @param checkCanFit if true, the available space at the location is
     *                    checked to see if it can accommodate the mob, and if
     *                    not, the mob is removed.
     * @return the new LivingEntity, or null if it could not fit or an invalid
     *         type was specified.
     */
    public LivingEntity spawnMob(Location loc, MobType mobType, boolean checkCanFit) {
        // Maximum mob passenger stack height is two more than this, i.e. 10.
        if (_spawnMobRecursion > 8) {
            return null;
        }
        ++_spawnMobRecursion;

        MobProperty entityTypeProperty = mobType.getDerivedProperty("entity-type");
        EntityType entityType = (EntityType) entityTypeProperty.getValue();
        LivingEntity livingEntity = null;
        if (entityType == null) {
            getLogger().info("Mob type " + mobType.getId() + " cannot spawn at " + Util.formatLocation(loc) + ": no entity type.");
        } else {
            // When _spawningMobType is non-null, we know that custom
            // (plugin-generated) mob spawns originate from this plugin.
            // World.spawnEntity() calls into onCreatureSpawn().
            _spawningMobType = mobType;
            _spawningCheckCanFit = checkCanFit;
            livingEntity = (LivingEntity) loc.getWorld().spawnEntity(loc, entityType);

            // Check if removed because it can't fit.
            if (!livingEntity.isValid()) {
                livingEntity = null;
            }
            _spawningMobType = null;
        }

        --_spawnMobRecursion;
        return livingEntity;
    }

    // ------------------------------------------------------------------------
    /**
     * Spawn multiple mobs according to a mob property that is either a DropSet
     * ID or MobType ID.
     *
     * @param loc         the location to spawn the mob(s).
     * @param lootOrMobId the DropSet or MobType ID.
     * @param checkCanFit whether to check if the mobs can fit.
     * @param results     DropResults recording whether vanilla drops happened.
     * @param trigger     the trigger string to log for logged {@link Drop}s.
     * @return a list of the spawned mobs.
     */
    public List<LivingEntity> spawnMultipleMobs(Location loc, String lootOrMobId, boolean checkCanFit, DropResults results, String trigger) {
        DropSet drops = BeastMaster.LOOTS.getDropSet(lootOrMobId);
        if (drops != null) {
            drops.generateRandomDrops(results, trigger, null, loc, true);
            return results.getMobs();
        } else {
            List<LivingEntity> mobs = new ArrayList<>();
            MobType supportMobType = BeastMaster.MOBS.getMobType(lootOrMobId);
            if (supportMobType != null) {
                mobs.add(spawnMob(loc, supportMobType, checkCanFit));
            }
            return mobs;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When a world is loaded, ensure that a top-level zone for that world
     * exists.
     */
    @EventHandler(ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent event) {
        getLogger().info("onWorldLoad: " + event.getWorld().getName());
        World world = event.getWorld();
        if (ZONES.getZone(world.getName()) == null) {
            ZONES.addZone(new Zone(world));
        }

        // ChunkLoadEvent is not raised for pre-loaded spawn chunks.
        for (Chunk chunk : world.getLoadedChunks()) {
            Bukkit.getScheduler().runTaskLater(this, () -> DISGUISES.loadDisguises(chunk), 1);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If a player breaks an objective block, do treasure drops and stop that
     * the particle effects.
     *
     * Handle players breaking ore blocks by consulting the most specific loot
     * table for the applicable Zone/Condition and block type.
     *
     * Don't drop special items for player-placed blocks.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        long start = System.nanoTime();
        boolean placed = BlockStoreApi.isPlaced(block);
        if (CONFIG.DEBUG_BLOCKSTORE) {
            float elapsedMs = (System.nanoTime() - start) * 1.0e-6f;
            if (elapsedMs > 10.0f) {
                debug("BlockStoreApi.isPlaced() took: " + String.format("%3.2f", elapsedMs));
            }
        }

        if (placed) {
            return;
        }

        handleBlockBreakCustomDrops(event, block);

        Objective objective = OBJECTIVES.getObjective(block);
        if (objective != null) {
            // Prevent the objective break from being logged by LogBlock.
            event.setCancelled(true);

            OBJECTIVES.removeObjective(objective);
            objective.spawnLoot(event.getPlayer());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When a mob spawns, perform zone-appropriate replacement with custom mob
     * types.
     *
     * Mobs that are not replaced are customised according to their EntityType.
     *
     * All mobs that go through this process end up with persistent metadata
     * value "mob-type" set to the ID of their MobType. Note, however, that
     * CUSTOM spawns from other plugins will not have the "mob-type" metadata.
     *
     * All mobs are tagged with their spawn reason as metadata. I would like to
     * tag slimes that spawn by splitting according to whether the original
     * slime came from a spawner, but there's no easy way to find the parent
     * slime.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onCreatureSpawn(CreatureSpawnEvent event) {
        replaceNetherSkeletonSpawn(event);

        LivingEntity entity = event.getEntity();

        // Tag spawn reason. Replacement mobs will have SpawnReason.CUSTOM.
        EntityMeta.api().set(entity, this, "spawn-reason", event.getSpawnReason().toString());

        switch (event.getSpawnReason()) {
        case CUSTOM:
            // Plugin driven spawns.
            if (_spawningMobType != null) {
                if (!_spawningCheckCanFit || canFit(entity)) {
                    _spawningMobType.configureMob(entity);
                } else {
                    entity.remove();
                }
            }
            break;

        case NATURAL:
        case REINFORCEMENTS:
        case INFECTION:
        case VILLAGE_INVASION:
        case VILLAGE_DEFENSE:
        case SILVERFISH_BLOCK:
        case ENDER_PEARL:
        case SPAWNER:
            // Vanilla spawns.
            Location loc = event.getLocation();
            Zone zone = ZONES.getZone(loc);
            if (zone != null && (zone.replacesSpawnerMobs() || event.getSpawnReason() != SpawnReason.SPAWNER)) {
                DropSet replacement = zone.getMobReplacementDropSet(entity.getType(), true);
                if (replacement != null) {
                    Drop drop = replacement.chooseOneDrop(true);
                    switch (drop.getDropType()) {
                    case DEFAULT:
                        // Don't change anything.
                        break;
                    case NOTHING:
                        entity.remove();
                        break;
                    case MOB:
                    case ITEM:
                        entity.remove();
                        DropResults results = new DropResults();
                        drop.generate(results, "Mob replacement of " + entity.getType() + " in " + zone.getId(),
                                      null, entity.getLocation());
                        break;
                    }
                } else {
                    MobType vanillaMobType = MOBS.getMobType(entity.getType());
                    if (vanillaMobType != null) {
                        vanillaMobType.configureMob(entity);
                    }
                }
            }
            break;

        case EGG:
        case SPAWNER_EGG:
        case BUILD_SNOWMAN:
        case BUILD_IRONGOLEM:
        case BUILD_WITHER:
        case DEFAULT: // For the EnderDragon.
        case SLIME_SPLIT: // For slimes.
            MobType vanillaMobType = MOBS.getMobType(entity.getType());
            if (vanillaMobType != null) {
                vanillaMobType.configureMob(entity);
            }
            break;

        default:
            break;
        }
    } // onCreatureSpawn

    // ------------------------------------------------------------------------
    /**
     * When a mob launches projectiles, replace them according to
     * "projectile-mobs" first. Those that aren't turned into mobs are then
     * disguised according to "projectile-disguise".
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    protected void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof LivingEntity) || shooter instanceof Player) {
            return;
        }

        // Get the shooter's target.
        // EnderDragon doesn't have getTarget() because it doesn't
        // inherit from Mob (nor from Flying). :/
        LivingEntity target = null;
        if (shooter instanceof Mob) {
            target = ((Mob) shooter).getTarget();
        }

        // Turn projectiles into mobs, if configured.
        LivingEntity shootingMob = (LivingEntity) shooter;
        MobType shootingMobType = getMobType(shootingMob);
        if (shootingMobType == null) {
            return;
        }
        MobProperty projectileMobsProperty = shootingMobType.getDerivedProperty("projectile-mobs");

        // Need to record if projectile removed. isValid() is not true until
        // this event returns.
        Location projectileLocation = projectile.getLocation();
        boolean projectileRemoved = false;
        if (projectileMobsProperty.getValue() != null) {
            // DropSet or MobType ID:
            String id = (String) projectileMobsProperty.getValue();
            DropResults results = new DropResults();
            List<LivingEntity> projectileMobs = spawnMultipleMobs(projectileLocation, id, false, results,
                                                                  shootingMobType.getId() + " projectile-mobs");
            for (LivingEntity mob : projectileMobs) {
                // Launch the mob with the projectile's velocity.
                mob.setVelocity(projectile.getVelocity());

                // Target the mob at the shooter's target.
                if (target != null && mob instanceof Mob) {
                    ((Mob) mob).setTarget(target);
                }
            }

            // To have the vanilla drop means not removing the projectile.
            // Really requires drop spread to avoid hitting spawned mobs.
            if (!results.includesVanillaDrop()) {
                event.setCancelled(true);
                projectileRemoved = true;
            }
        }

        // If the projectile was removed, we can't disguise it etc.
        if (!projectileRemoved) {
            String projectileDisguise = (String) shootingMobType.getDerivedProperty("projectile-disguise").getValue();
            BeastMaster.DISGUISES.createDisguise(projectile, projectile.getWorld(), projectileDisguise);

            SoundEffect sound = (SoundEffect) shootingMobType.getDerivedProperty("projectile-launch-sound").getValue();
            if (sound != null) {
                sound.play(projectileLocation);
            }
        }
    } // onProjectileLaunch

    // ------------------------------------------------------------------------
    /**
     * For projectiles fired by a custom mob, remove the projectile on impact
     * when "projectile-removed" is true.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof LivingEntity) || shooter instanceof Player) {
            return;
        }

        LivingEntity shootingMob = (LivingEntity) shooter;
        MobType shootingMobType = getMobType(shootingMob);
        if (shootingMobType != null) {
            Boolean removed = (Boolean) shootingMobType.getDerivedProperty("projectile-removed").getValue();
            if (removed != null && removed) {
                projectile.remove();
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When a mob is damaged, play the `projectile-hurt-sound`, or the
     * `melee-hurt-sound` for all other damage types.
     *
     * Note: onEntityDamange() is called after onEntityDamageByEntity().
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        // If the entity would die, don't summon support and don't play the hurt
        // sound. Leave a silence for the death sound.
        LivingEntity damagedLiving = (LivingEntity) entity;
        double finalHealth = damagedLiving.getHealth() - event.getFinalDamage();
        if (finalHealth <= 0.0) {
            return;
        }

        MobType mobType = getMobType(entity);
        if (mobType != null) {
            Location mobLocation = entity.getLocation();

            // Support mobs.
            String supportId = (String) mobType.getProperty("support-mobs").getValue();
            if (supportId != null) {
                Double healthThreshold = (Double) mobType.getProperty("support-health").getValue();
                boolean healthLow = (healthThreshold == null || finalHealth <= healthThreshold);
                Double prevHealth = (Double) EntityMeta.api().get(entity, this, "support-health");
                Double healthStep = (Double) mobType.getProperty("support-health-step").getValue();
                Double supportPercent = (Double) mobType.getProperty("support-percent").getValue();

                if (healthLow && (prevHealth == null ||
                                  healthStep == null ||
                                  prevHealth - finalHealth >= healthStep)
                    && (supportPercent == null ||
                        Math.random() * 100 < supportPercent)) {

                    // TODO: spawning needs to do better at looking for a
                    // spawnable location. Really need to do spawn conditions
                    // argument to spawning functions.
                    Location supportLocation = mobLocation.clone().add(0, 1, 0);

                    // Summon support mobs targeting same target as summoner.
                    DropResults results = new DropResults();
                    List<LivingEntity> supportMobs = spawnMultipleMobs(supportLocation, supportId, false, results,
                                                                       mobType.getId() + " support-mobs");
                    if (damagedLiving instanceof Mob) {
                        for (LivingEntity mob : supportMobs) {
                            if (mob instanceof Mob) {
                                ((Mob) mob).setTarget(((Mob) damagedLiving).getTarget());
                            }
                        }
                    }
                    // TODO: Spread them out?

                    // Record the mob health when support mobs were last
                    // spawned.
                    EntityMeta.api().set(entity, this, "support-health", finalHealth);
                }
            }

            DamageCause cause = event.getCause();
            if (cause == DamageCause.PROJECTILE) {
                Double immunityPercent = (Double) mobType.getDerivedProperty("projectile-immunity-percent").getValue();
                boolean immuneToProjectile = (immunityPercent != null && Math.random() * 100 < immunityPercent);
                if (immuneToProjectile) {
                    event.setCancelled(true);
                    SoundEffect immunitySound = (SoundEffect) mobType.getDerivedProperty("projectile-immunity-sound").getValue();
                    if (immunitySound != null) {
                        Bukkit.getScheduler().runTaskLater(this, () -> immunitySound.play(mobLocation), 1);
                    }
                    return;
                }
            }

            // Play hurt sounds after projectile immunity checks.
            String propertyName = (cause == DamageCause.PROJECTILE) ? "projectile-hurt-sound" : "melee-hurt-sound";
            SoundEffect hurtSound = (SoundEffect) mobType.getDerivedProperty(propertyName).getValue();
            if (hurtSound != null) {
                Bukkit.getScheduler().runTaskLater(this, () -> hurtSound.play(mobLocation), 1);
            }

            // Impart hurt-potions effects on the mob when hurt.
            String potionSetId = (String) mobType.getDerivedProperty("hurt-potions").getValue();
            PotionSet potionSet = POTIONS.getPotionSet(potionSetId);
            if (potionSet != null) {
                potionSet.apply(damagedLiving);
            }

            // Don't teleport if the damage is low to allow for slight falls.
            if (event.getFinalDamage() <= 3.0) {
                return;
            }

            // The mob has been hurt. Teleport away per random chance.
            Double hurtTeleportPercent = (Double) mobType.getDerivedProperty("hurt-teleport-percent").getValue();
            if (hurtTeleportPercent != null && Math.random() * 100 < hurtTeleportPercent) {
                // Find a location up to 10 blocks up and up to 15 blocks away.
                Location oldLoc = mobLocation;
                double range = Util.random(5.0, 15.0);
                double angle = Util.random() * 2.0 * Math.PI;
                Location newLoc = oldLoc.clone().add(range * Math.cos(angle), 0, range * Math.sin(angle));

                // Look back at the old location.
                Location diff = newLoc.clone();
                diff.subtract(oldLoc);
                newLoc.setDirection(diff.getDirection());

                // Find an initial safe destination. Might be floating.
                newLoc.add(0, 10, 0);
                boolean safe = false;
                for (int i = 0; i < 10; ++i) {
                    if (Util.isPassable3x3x3(newLoc)) {
                        safe = true;
                        break;
                    }
                    newLoc.add(0, -1, 0);
                }

                if (!safe) {
                    // Nowhere safe. Try again next damage.
                    return;
                }

                // newLoc is a valid teleport destination, but maybe floating.
                // Try to lower the location to the ground.
                for (int i = 0; i < 10; ++i) {
                    Location tryLoc = newLoc.clone().add(0, -1, 0);
                    if (Util.isPassable3x3x3(tryLoc)) {
                        newLoc = tryLoc;
                    } else {
                        break;
                    }
                }

                // Needs to be final to keep the compiler happy.
                final Location destination = newLoc;
                if (destination != null) {
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        entity.teleport(destination);
                        Util.doTeleportEffects(mobType, destination);
                    }, 1);
                }
            }
        }
    } // onEntityDamage

    // ------------------------------------------------------------------------
    /**
     * Tag mobs hurt by players with the time stamp of the damage event to
     * facilitate custom dropped XP.
     *
     * Also, apply attack-potions {@link PotionSet} to the victim, where
     * configured on the attacker.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damagedEntity = event.getEntity();
        LivingEntity attackingMob = null;
        Player attackingPlayer = null;

        if (event.getDamager() instanceof Player) {
            attackingPlayer = (Player) event.getDamager();
            EntityMeta.api().set(damagedEntity, this, DAMAGED_BY_PLAYER_NAME, attackingPlayer.getName());
            EntityMeta.api().set(damagedEntity, this, DAMAGED_BY_PLAYER_TIME, damagedEntity.getWorld().getFullTime());
        } else if (event.getDamager() instanceof LivingEntity) {
            attackingMob = (LivingEntity) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attackingPlayer = (Player) projectile.getShooter();
                EntityMeta.api().set(damagedEntity, this, DAMAGED_BY_PLAYER_NAME, attackingPlayer.getName());
                EntityMeta.api().set(damagedEntity, this, DAMAGED_BY_PLAYER_TIME, damagedEntity.getWorld().getFullTime());
            } else if (projectile.getShooter() instanceof LivingEntity) {
                attackingMob = (LivingEntity) projectile.getShooter();
            }
        }

        // Should damaged mobs immediately switch target to the damager?
        MobType damagedMobType = getMobType(damagedEntity);
        if (damagedMobType != null && damagedEntity instanceof Mob) {
            Boolean targetDamager = (Boolean) damagedMobType.getDerivedProperty("target-damager").getValue();
            if (targetDamager != null && targetDamager) {
                Mob damagedMob = (Mob) damagedEntity;
                if (attackingPlayer != null) {
                    damagedMob.setTarget(attackingPlayer);
                } else if (attackingMob != null) {
                    damagedMob.setTarget(attackingMob);
                }
            }
        }

        // What to do if the attacker is a mob.
        if (attackingMob != null) {
            MobType attackingMobType = getMobType(attackingMob);
            if (attackingMobType != null) {
                // Mob attacking mob?
                if (damagedMobType != null && attackingMobType.isFriendlyTo(damagedMobType)) {
                    event.setCancelled(true);
                    return;
                }

                // Apply attackingMob's attack-potions, if set.
                String potionSetId = (String) attackingMobType.getDerivedProperty("attack-potions").getValue();
                PotionSet potionSet = POTIONS.getPotionSet(potionSetId);
                if (potionSet != null) {
                    potionSet.apply((LivingEntity) damagedEntity);
                }

                // Play the melee-attack-sound.
                SoundEffect sound = (SoundEffect) attackingMobType.getDerivedProperty("melee-attack-sound").getValue();
                if (sound != null) {
                    sound.play(damagedEntity.getLocation());
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handle entity death of custom mobs by replacing drops.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        // Note: Ghasts and Slimes are not Monsters... Players and ArmorStands
        // are LivingEntities. #currentyear
        MobType mobType = getMobType(entity);
        if (mobType != null) {
            Location loc = entity.getLocation();

            // If the mob has been damaged by a player recently, work out that
            // player's name.
            String victoriousPlayerName = null;
            Long damageTime = (Long) EntityMeta.api().get(entity, this, DAMAGED_BY_PLAYER_TIME);
            boolean damagedByPlayer = damageTime != null &&
                                      loc.getWorld().getFullTime() - damageTime < PLAYER_DAMAGE_TICKS;

            if (damagedByPlayer) {
                MobProperty experience = mobType.getDerivedProperty("experience");
                if (experience.getValue() != null) {
                    event.setDroppedExp((Integer) experience.getValue());
                }

                victoriousPlayerName = (String) EntityMeta.api().get(entity, this, DAMAGED_BY_PLAYER_NAME);
            }

            DropSet drops = mobType.getDrops();
            if (drops != null) {
                StringBuilder trigger = new StringBuilder();

                Player victoriousPlayer = (victoriousPlayerName != null) ? Bukkit.getPlayerExact(victoriousPlayerName) : null;
                trigger.append((victoriousPlayer != null) ? victoriousPlayer.getName() : "<environment>");
                trigger.append(" killed ");
                trigger.append(mobType.getId());

                DropResults results = new DropResults();
                drops.generateRandomDrops(results, trigger.toString(), victoriousPlayer, entity.getLocation(), damagedByPlayer);
                if (!results.includesVanillaDrop()) {
                    event.getDrops().clear();
                }
            }

            SoundEffect deathSound = (SoundEffect) mobType.getDerivedProperty("death-sound").getValue();
            if (deathSound != null) {
                // Mysteriously doesn't work unless delayed 2 ticks. Disguises?
                Bukkit.getScheduler().runTaskLater(this, () -> deathSound.play(loc), 2);
            }

            if (CONFIG.DEBUG_EQUIPMENT_DROPS) {
                // If the entity has a MobType, it's a LivingEntity.
                LivingEntity mob = (LivingEntity) entity;
                EntityEquipment equipment = mob.getEquipment();
                debug(String.format("%s equipment drop %% (B,L,C,H), (M,O): (%.3f,%.3f,%.3f,%.3f), (%.3f,%.3f)",
                                    mobType.getId(),
                                    100 * equipment.getBootsDropChance(), 100 * equipment.getLeggingsDropChance(),
                                    100 * equipment.getChestplateDropChance(), 100 * equipment.getHelmetDropChance(),
                                    100 * equipment.getItemInMainHandDropChance(), 100 * equipment.getItemInOffHandDropChance()));
                debug(mobType.getId() + " event drops: " + event.getDrops().stream().map(Util::getItemDescription).collect(Collectors.joining(", ")));
            }
        }

    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * Prevent slimes with "slime-can-split" false from splitting.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    protected void onSlimeSplit(SlimeSplitEvent event) {
        Slime slime = event.getEntity();
        MobType mobType = getMobType(slime);
        if (mobType == null) {
            return;
        }

        Boolean canSplit = (Boolean) mobType.getDerivedProperty("slime-can-split").getValue();
        if (canSplit != null && !canSplit) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prevent a mob from targeting any other mob whose "groups" set includes a
     * name that is in the targeting mob's "friend-groups".
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    protected void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        Entity targetter = event.getEntity();
        LivingEntity target = event.getTarget();
        if (!(targetter instanceof LivingEntity)) {
            return;
        }

        // If the targeting mob is not friendly to any groups, no action needed.
        MobType mobType = getMobType(targetter);
        if (mobType == null) {
            return;
        }

        if (mobType.isFriendlyTo(getMobType(target))) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Play the teleport-sound, if configured for the mob.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onEntityTeleport(EntityTeleportEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity) || entity instanceof Player) {
            return;
        }

        MobType mobType = getMobType(entity);
        if (mobType != null) {
            SoundEffect sound = (SoundEffect) mobType.getDerivedProperty("teleport-sound").getValue();
            if (sound != null) {
                // Mysteriously doesn't work unless delayed 1 tick. Disguises?
                Bukkit.getScheduler().runTaskLater(this, () -> sound.play(event.getFrom()), 1);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When the player joins, send them all pertinent disguises in the world.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onPlayerJoin(PlayerJoinEvent event) {
        if (BeastMaster.CONFIG.DEBUG_DISGUISES) {
            debug("onPlayerJoin()");
        }
        DISGUISES.sendAllDisguises(event.getPlayer().getWorld(), event.getPlayer());
    }

    // ------------------------------------------------------------------------
    /**
     * When the player respawns, refresh disguises.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> DISGUISES.sendAllDisguises(event.getPlayer().getWorld(), event.getPlayer()), 1);
    }

    // ------------------------------------------------------------------------
    /**
     * When the player changes world, send disguises for the new world.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onPlayerTeleport(PlayerTeleportEvent event) {
        World fromWorld = event.getFrom().getWorld();
        World toWorld = event.getTo().getWorld();
        if (!fromWorld.equals(toWorld)) {
            Bukkit.getScheduler().runTaskLater(this, () -> DISGUISES.sendAllDisguises(toWorld, event.getPlayer()), 1);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When loading a chunk, apply disguises if not null/empty.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onChunkLoad(ChunkLoadEvent event) {
        DISGUISES.loadDisguises(event.getChunk());
    }

    // ------------------------------------------------------------------------
    /**
     * When unloading a chunk, clear disguises of unloaded mobs.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity) {
                MobType mobType = getMobType(entity);
                if (mobType != null) {
                    String encodedDisguise = (String) mobType.getDerivedProperty("disguise").getValue();
                    if (encodedDisguise != null && !encodedDisguise.isEmpty()) {
                        DISGUISES.destroyDisguise(entity, event.getWorld());
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handle block break in a zone where that block type should drop custom
     * drops.
     *
     * Only survival mode players should trigger drops.
     *
     * @param event the BlockBreakEvent.
     * @param block the broken block.
     */
    protected void handleBlockBreakCustomDrops(BlockBreakEvent event, Block block) {
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        Zone zone = ZONES.getZone(loc);
        if (zone == null) {
            return;
        }

        DropSet drops = zone.getMiningDrops(block.getType(), true);
        if (drops == null) {
            return;
        }

        StringBuilder trigger = new StringBuilder();
        trigger.append(event.getPlayer().getName());
        trigger.append(" broke ");
        trigger.append(block.getType());

        DropResults results = new DropResults();
        drops.generateRandomDrops(results, trigger.toString(), event.getPlayer(), loc, true);
        event.setDropItems(results.includesVanillaDrop());
    }

    // ------------------------------------------------------------------------
    /**
     * In the plains biome in the nether environment, replace the configured
     * percentage of Skeletons with WitherSkeletons.
     *
     * This code dates back to PvE Rev 19 when vanilla Minecraft separated
     * wither skeletons from regular skeltons, breaking wither spawning in
     * nether plains biomes. It will eventually be obsoleted by more general
     * BeastMaster mechanisms.
     */
    protected void replaceNetherSkeletonSpawn(CreatureSpawnEvent event) {
        // Old PvE Rev 19 code path to make Wither Skeletons spawn in nether
        // plains biomes following removal from vanilla.
        Location loc = event.getLocation();
        World world = loc.getWorld();
        if (world.getEnvironment() == Environment.NETHER &&
            loc.getBlock().getBiome() == Biome.PLAINS &&
            event.getEntityType() == EntityType.SKELETON &&
            Math.random() < CONFIG.CHANCE_WITHER_SKELETON) {
            if (CONFIG.DEBUG_REPLACE) {
                debug(String.format("Replacing skeleton at (%d, %d, %d, %s) with wither skeleton.",
                                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName()));
            }
            event.getEntity().remove();
            world.spawnEntity(loc, EntityType.WITHER_SKELETON);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified entity can fit at its spawn location.
     *
     * This check is important when replacing mobs, because the replacement may
     * be larger than the mob it replaced and may suffocate. Mobs that don't fit
     * are simply removed.
     *
     * @return true if the specified entity can fit at its spawn location.
     */
    protected boolean canFit(Entity entity) {
        int height = (int) Math.ceil(entity.getHeight());
        int width = (int) Math.ceil(entity.getWidth());

        // For 1x1 mobs, assume that they are spawning in the block just broken.
        // Just let them spawn, even though the block is still there until the
        // end of the tick.
        if (width == 1 && height == 1) {
            return true;
        }

        Block feetBlock = entity.getLocation().getBlock();
        for (int y = height - 1; y >= 0; --y) {
            for (int x = -width / 2; x <= width / 2; ++x) {
                for (int z = -width / 2; z <= width / 2; ++z) {
                    Block block = feetBlock.getRelative(x, y, z);
                    if (block != null && !block.isPassable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Add the specified CommandExecutor and set it as its own TabCompleter.
     *
     * @param executor the CommandExecutor.
     */
    protected void addCommandExecutor(ExecutorBase executor) {
        PluginCommand command = getCommand(executor.getName());
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    // ------------------------------------------------------------------------
    /**
     * Persistent metadata key used to record name of player that damaged a mob.
     */
    protected static final String DAMAGED_BY_PLAYER_NAME = "damage-player";

    /**
     * Persistent metadata key used to record the full world time when a mob was
     * damaged by a player.
     */
    protected static final String DAMAGED_BY_PLAYER_TIME = "damage-time";

    /**
     * Time in ticks (1/20ths of a second) for which player attack damage
     * "sticks" to a mob. The time between the last player damage on a mob and
     * its death must be less than this for it to drop special stuff.
     */
    protected static final int PLAYER_DAMAGE_TICKS = 100;

    /**
     * When spawning a custom mob via
     * {@link #spawnMob(Location, EntityType, MobType)} we need to record the
     * MobType of the custom mob so that the entity can be configured
     * accordingly. BeastMaster.spawnMob() calls World.spawnEntity() which
     * triggers a function call to onCreatureSpawn(), which then consults this
     * field only in the event of SpawnReason.CUSTOM. This field is then cleared
     * to null to prevent BeastMaster from attempting custom configuration of
     * mobs spawned by other plugins.
     */
    protected MobType _spawningMobType;

    /**
     * If true, and if _spawningMobType is non-null, then a size check is made
     * on mobs with SpawnReason.CUSTOM and those that don't fit the available
     * space at their spawn location are removed.
     *
     * The size check is there to gracefully handle replacement of naturally
     * spawned mobs with different size mobs. However, when spawnMob() is called
     * because of the /beast-mob spawn or /beast-mob statue commands, the size
     * check should be suppressed to avoid the confusion of nothing spawning as
     * a result.
     */
    protected boolean _spawningCheckCanFit;

    /**
     * The number of {@link #spawnMob(Location, MobType, boolean)} calls
     * currently on the call stack.
     *
     * {@link #spawnMob(Location, MobType, boolean)} triggers a
     * {@link CreatureSpawnEvent} that can result in more
     * {@link #spawnMob(Location, MobType, boolean)} calls due to mob
     * replacement or the passenger property of mobs. If a mob type is defined
     * to spawn itself as a passenger then that would result in an infinite
     * recursion of {@link #spawnMob(Location, MobType, boolean)} calls that
     * would crash the server with a stack overflow. To prevent that, we keep
     * track of the number of calls with this variable.
     */
    protected int _spawnMobRecursion;
} // class BeastMaster