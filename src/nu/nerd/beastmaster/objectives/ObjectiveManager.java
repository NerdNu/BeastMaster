package nu.nerd.beastmaster.objectives;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.DataException;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.Util;
import nu.nerd.beastmaster.zones.Zone;

// ----------------------------------------------------------------------------
/**
 * Manages the collection of all {@link Objective}s in the world.
 */
public class ObjectiveManager {
    // ------------------------------------------------------------------------
    /**
     * Extract schematics that are built into the plugin JAR if the schematics/
     * subdirectory of the plugin directory does not exist.
     */
    public void extractSchematics() {
        File schematicsDir = new File(BeastMaster.PLUGIN.getDataFolder(), "schematics");
        if (!schematicsDir.isDirectory()) {
            schematicsDir.mkdirs();

            for (String name : SCHEMATICS) {
                String baseName = name + ".schematic";
                File toWrite = new File(schematicsDir, baseName);
                if (!toWrite.exists()) {
                    try (InputStream in = getClass().getResourceAsStream("/schematics/" + baseName)) {
                        if (in == null) {
                            BeastMaster.PLUGIN.getLogger().severe("Unable to find configured schematic: " +
                                                                  baseName + " in plugin JAR.");
                        } else {
                            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(toWrite, false));
                            byte[] buf = new byte[4096];
                            int totalLen = 0;
                            int readLen;
                            while ((readLen = in.read(buf)) > 0) {
                                out.write(buf, 0, readLen);
                                totalLen += readLen;
                            }
                            out.close();
                            BeastMaster.PLUGIN.getLogger().info("Wrote schematic: " + baseName +
                                                                " (" + totalLen + " bytes)");
                        }
                    } catch (IOException ex) {
                        BeastMaster.PLUGIN.getLogger().severe("Error writing schematic: " + baseName);
                    }
                }
            }
        }
    } // extractSchematics

    // ------------------------------------------------------------------------
    /**
     * If the maximum number of extant {@link Objective}s has not been exceeded,
     * then calculate a random location within the configured range limits of
     * the mob death point, and spawn an Objective there.
     *
     * This method should only be called if the death location was in the
     * affected world.
     *
     * The objective will not be spawned if it might collide with an entity
     * (e.g. a painting or item frame) - thus breaking it, if it would be above
     * the height limit of the world, if the maximum number of objectives
     * currently exists in the world, or if the objective would be over the
     * world border.
     *
     * @param objectiveType the objective type.
     * @param zone the zone whose limits will be obeyed when placing the
     *        objective.
     * @param deathLocation the location where a drop triggered the objective's
     *        spawning.
     * @return a new Objective if fewer have been spawned than the limit, a
     *         reference to the oldest objective still in the world old if the
     *         maximum have been spawned, or null if no objective could be
     *         spawned.
     */
    public Objective spawnObjective(ObjectiveType objectiveType, Zone zone, Location deathLocation) {
        if (_blockToObjective.size() >= objectiveType.getMaxCount()) {
            return (_objectives.isEmpty()) ? null : Util.randomChoice(_objectives);
        }

        // Try a few times to spawn an objective.
        for (int i = 0; i < 3; ++i) {
            Objective objective = spawnNewObjective(objectiveType, zone, deathLocation);
            if (objective != null) {
                return objective;
            }
        }
        return null;
    } // spawnObjective

    // ------------------------------------------------------------------------
    /**
     * Tick all objectives in the world, updating particle effects and removing
     * those that were found or timed out.
     */
    public void tickAll() {
        Iterator<Entry<Block, Objective>> it = _blockToObjective.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, Objective> entry = it.next();
            Objective objective = entry.getValue();
            if (!objective.isAlive()) {
                objective.vaporise();
                _objectives.remove(objective);
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If the block marks an objective, return the corresponding
     * {@link Objective} instance.
     *
     * @param block the block that might be an objective.
     * @return the {@link Objective} or null if there is none at the specified
     *         Block.
     */
    public Objective getObjective(Block block) {
        if (block.getType() == Material.LEGACY_SKULL) { // TODO
            Objective objective = _blockToObjective.get(block);
            if (objective != null) {
                return objective;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link Objective} from the the world, thus cancelling the
     * particle effects.
     *
     * @param objective the Objective.
     */
    public void removeObjective(Objective objective) {
        objective.vaporise();
        _objectives.remove(objective);
        _blockToObjective.remove(objective.getBlock());
    }

    // ------------------------------------------------------------------------
    /**
     * Remove all the pots.
     */
    public void removeAll() {
        for (Objective objective : _objectives) {
            objective.vaporise();
        }
        _objectives.clear();
        _blockToObjective.clear();
    }

    // ------------------------------------------------------------------------
    /**
     * Set the specified block to a skull with a Christmas present skin.
     * 
     * The following code does not work on Spigot 1.10.2, despite it being in
     * excess of the best recommendations found online regarding how to achieve
     * the desired effect:
     * 
     * <pre>
     * Block objectiveBlock = objLocation.getBlock();
     * objectiveBlock.setType(Material.SKULL);
     * objectiveBlock.setData((byte) 1);
     * 
     * String skullOwner = Util.randomChoice(BeastMaster.CONFIG.OBJECTIVE_SKINS);
     * Skull skull = (Skull) objectiveBlock.getState();
     * skull.setSkullType(SkullType.PLAYER);
     * skull.setRotation(BlockFace.SOUTH_SOUTH_EAST);
     * skull.setOwner(skullOwner);
     * skull.update(true);
     * 
     * for (Player player : objLocation.getWorld().getPlayers()) {
     *     player.sendBlockChange(objLocation, Material.SKULL, (byte) 1);
     * }
     * </pre>
     * 
     * The code above does set the block correctly and it can be seen if you
     * relog. However, if you don't relog, the client sees the objective marker
     * as just a rotated, regular skeleton skull. Despite trying several
     * different API calls I have not been able to get the client to see the
     * updated block.
     * 
     * I know that WorldEdit is able to paste skulls correctly. I suspect that
     * it is doing some kind of NMS magic. Rather than reverse engineer that and
     * add an NMS dependency, this method simply delegates the task to
     * WorldEdit.
     * 
     * @param objectiveType the objective type.
     * @param loc the Location to mark as an objective.
     */
    protected void markObjective(ObjectiveType objectiveType, Location loc) {
        String fileName = Util.randomChoice(objectiveType.getSchematics()) + ".schematic";
        File schematicsDir = new File(BeastMaster.PLUGIN.getDataFolder(), "schematics");
        File presentSchematicFile = new File(schematicsDir, fileName);
        try {
            pasteSchematic(presentSchematicFile, loc, new Vector(), false, false);
        } catch (Exception ex) {
            BeastMaster.PLUGIN.getLogger().warning("Unable to paste objective marker: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Paste a schematic to a specified location, with specified position
     * offset.
     * 
     * @param file the schematic file to load.
     * @param loc the location to paste the reference point.
     * @param offset the relative position adjustment to apply to the location.
     * @param copyAir if true, air in the schematic is written into the world;
     *        otherwise, the block at that location is unchanged.
     * @param copyEntities if true, paste entities from the schematic into the
     *        world.
     */
    protected void pasteSchematic(File file, Location loc, Vector offset, boolean copyAir, boolean copyEntities)
    throws DataException, IOException, MaxChangedBlocksException {
        WorldEdit we = getWorldEdit();
        EditSession es = we.getEditSessionFactory().getEditSession(new BukkitWorld(loc.getWorld()), -1);

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            return;
        }
        try (Closer closer = Closer.create()) {
            FileInputStream fileInputStream = closer.register(new FileInputStream(file));
            BufferedInputStream bufferedInputSteam = closer.register(new BufferedInputStream(fileInputStream));
            ClipboardReader reader = closer.register(format.getReader(bufferedInputSteam));
            Clipboard clipboard = reader.read();

            ClipboardHolder holder = new ClipboardHolder(clipboard);
            LocalSession localSession = new LocalSession();

            AffineTransform transform = new AffineTransform();
            transform.rotateY(90 * Util.randomInt(4));
            holder.setTransform(transform);

            localSession.setClipboard(holder);

            try {
                localSession.getClipboard().createPaste(es)
                .to(toWEVector(loc.clone().add(offset)))
                .ignoreAirBlocks(!copyAir)
                .build();
            } catch (EmptyClipboardException e) {
                e.printStackTrace();
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Convert a Bukkit Location into a WorldEdit Vector.
     * 
     * @param loc the location.
     * @return the Vector.
     */
    protected BlockVector3 toWEVector(Location loc) {
        return BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    // ------------------------------------------------------------------------
    /**
     * Return a reference to the WorldEdit plugin.
     *
     * @return a reference to the WorldEdit plugin.
     */
    protected WorldEdit getWorldEdit() {
        WorldEditPlugin plugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        return plugin.getWorldEdit();
    }

    // ------------------------------------------------------------------------
    /**
     * Try to spawn a new objective, subject to the constraints listed in the
     * JavaDoc of {@link #spawnObjective(ObjectiveType, Zone, Location)}.
     * 
     * @param objectiveType the objective type.
     * @param zone the zone whose limits will be obeyed when placing the
     *        objective.
     * @param dropLocation the location where a drop triggered the objective's
     *        spawning.
     * @return a new objective, or null if it could not be spawned.
     */
    protected Objective spawnNewObjective(ObjectiveType objectiveType, Zone zone, Location dropLocation) {
        double angleRadians = 2 * Math.PI * Math.random();
        double range = Util.random(objectiveType.getMinRange(), objectiveType.getMaxRange());
        double dX = range * Math.cos(angleRadians);
        double dZ = range * Math.sin(angleRadians);

        World world = dropLocation.getWorld();
        Block floorBlock = world.getHighestBlockAt((int) (dropLocation.getX() + dX),
                                                   (int) (dropLocation.getZ() + dZ));
        Location highestLocation = floorBlock.getLocation();

        // TODO: Objectives are officially borked.
        int minX = 0; // zone.getCentreX() - zone.getRadius();
        int maxX = 0; // zone.getCentreX() + zone.getRadius();
        int minZ = 0; // zone.getCentreZ() - zone.getRadius();
        int maxZ = 0; // zone.getCentreZ() + zone.getRadius();
        int x = highestLocation.getBlockX();
        int highestY = highestLocation.getBlockY();
        int z = highestLocation.getBlockZ();

        if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
            // Select a block in the vertical range of the objective type. No
            // higher than the highest block.
            int minY = Math.min(objectiveType.getMinY(), highestY);
            int maxY = Math.min(objectiveType.getMaxY(), highestY);
            int startY = Util.random(minY, maxY);

            // Scan for an eligible location between startY and maxY. Must be
            // air and no entities (item frames, paintings) to break.
            for (int y = startY; y < maxY; ++y) {
                Block objBlock = world.getBlockAt(x, y, z);
                if (objBlock.getType() == Material.AIR &&
                    world.getNearbyEntities(objBlock.getLocation(), 2, 2, 2).isEmpty()) {

                    // While the chosen location is above the minimum Y and it
                    // is floating in the air, look for a horizontal surface
                    // below to use instead.
                    int floorY = getFloorY(objectiveType.getMinY(), world, x, y, z);
                    objBlock = world.getBlockAt(x, floorY, z);
                    Location objLocation = objBlock.getLocation();

                    BeastMaster.PLUGIN.getLogger().info("Objective spawned at " + Util.formatLocation(objLocation));
                    double distance = objLocation.distance(dropLocation);
                    BeastMaster.PLUGIN.getLogger().info("Distance " + distance);
                    int travelTicks = 20 * (int) (distance / objectiveType.getMinPlayerSpeed());
                    BeastMaster.PLUGIN.getLogger().info("Travel ticks " + travelTicks);
                    int lifeInTicks = objectiveType.getExtraTicks() + travelTicks;
                    BeastMaster.PLUGIN.getLogger().info("Life in ticks " + lifeInTicks);

                    Objective objective = new Objective(objectiveType, objLocation, lifeInTicks);
                    _blockToObjective.put(objBlock, objective);
                    _objectives.add(objective);
                    markObjective(objectiveType, objLocation);
                    return objective;
                }
            } // for
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a floor below (x,y,z) that the objective could sit on, provided it
     * is empty of blocks and entities and at least the minimum Y for the
     * objective type.
     * 
     * @param minY the minimum Y.
     * @param world the world.
     * @param x the X coordinate.
     * @param y the starting Y coordinate for the search; the maximum return
     *        value.
     * @param z the Z coordinate.
     * @return the Y coordinate of the first empty spot below y and above minY
     *         if there is a floor to sit on, or minY if it is all empty space.
     */
    protected int getFloorY(int minY, World world, int x, int y, int z) {
        while (y > minY) {
            Block block = world.getBlockAt(x, y - 1, z);
            if (block.getType() == Material.AIR &&
                world.getNearbyEntities(block.getLocation(), 2, 2, 2).isEmpty()) {
                --y;
            } else {
                break;
            }
        }
        return y;
    }

    // ------------------------------------------------------------------------
    /**
     * A list of schematic file base names built into the plugin JAR.
     */
    protected static String[] SCHEMATICS = {
        "SeerPotion", "Hannah4848", "CruXXx",
        "MHF_Present1", "MHF_Present2", "MHF_Chest"
    };

    /**
     * Objectives listed in the order they were created, to facilitate
     * recycling.
     */
    protected ArrayList<Objective> _objectives = new ArrayList<>();

    /**
     * Map from Block to Objective at that block.
     */
    protected HashMap<Block, Objective> _blockToObjective = new HashMap<Block, Objective>();

} // class ObjectiveManager