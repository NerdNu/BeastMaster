package nu.nerd.beastmaster.zones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropSet;

// ----------------------------------------------------------------------------
/**
 * Defines a hierarchy of 2-D volumes delineated by Zone Specification Language
 * where specific mining drops and mob replacements apply.
 */
public class Zone {
    // ------------------------------------------------------------------------
    /**
     * Default constructor, used for loading from the configuration.
     */
    public Zone() {
        // Default initialisation.
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor for Root Zones.
     *
     * @param world the World where this Zone is the root of the Zone hierarchy.
     */
    public Zone(World world) {
        this(world.getName(), null, null);
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param id         the identifier.
     * @param parent     the parent Zone, or null for a root (world) Zone.
     * @param expression the Expression corresponding to the Zone Specification.
     */
    public Zone(String id, Zone parent, Expression expression) {
        _id = id;
        _parent = parent;
        if (_parent != null) {
            _parent.children().add(this);
        }
        setExpression(expression);
        _inheritsBlocks = true;
        _inheritsReplacements = true;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the programmatic ID of this zone.
     *
     * @return the programmatic ID of this zone.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this Zone is the root of the Zone hierarchy for its World.
     *
     * Root Zones are named after the World they are specific to.
     *
     * @return true if this Zone is the root of the Zone hierarchy for its
     *         World.
     */
    public boolean isRoot() {
        return _parent == null;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the parent of this Zone.
     *
     * @param parent the new parent Zone.
     */
    public void setParent(Zone parent) {
        // In principle, parentless (root zones) cannot have a parent set.
        if (_parent != null) {
            _parent.children().remove(this);
        }
        parent.children().add(this);
        _parent = parent;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the parent of this Zone, or null if this is a root Zone.
     *
     * @return the parent of this Zone, or null if this is a root Zone.
     */
    public Zone getParent() {
        return _parent;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the root of this Zone's hierarchy.
     *
     * @return the root of this Zone's hierarchy.
     */
    public Zone getRoot() {
        return isRoot() ? this : _parent.getRoot();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the World that this Zone belongs to.
     *
     * @return the World that this Zone belongs to.
     */
    public World getWorld() {
        return Bukkit.getWorld(getRoot().getId());
    }

    // ------------------------------------------------------------------------
    /**
     * Mutable access to the children of this Zone.
     *
     * @return the child Zones.
     */
    public List<Zone> children() {
        return _children;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Zone Specification corresponding to this Zone's Expression, or
     * null if this is a root Zone.
     *
     * @return the Zone Specification corresponding to this Zone's Expression,
     *         or null if this is a root Zone.
     */
    public String getSpecification() {
        return _specification;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the Zone Specification Language expression corresponding to this
     * Zone.
     *
     * @param expression the expression.
     */
    public void setExpression(Expression expression) {
        _expression = expression;
        _specification = formatExpression(expression);
    }

    // ------------------------------------------------------------------------
    /**
     * Return this Zone's Expression, or null if this is a root Zone.
     *
     * @return this Zone's Expression, or null if this is a root Zone.
     */
    public Expression getExpression() {
        return _expression;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this Zone contains the specified Location.
     *
     * @param loc the Location.
     * @return true if the Location is in this Zone.
     */
    public boolean contains(Location loc) {
        return (_expression == null) ? getWorld().equals(loc.getWorld())
                                     : (Boolean) _expression.visit(EVALUATOR, loc);
    }

    // ------------------------------------------------------------------------
    /**
     * Specify whether this Zone inherits mining drops from its parent Zone.
     *
     * @param inheritsBlocks True if this Zone inherits mining drops from its
     *                       parent Zone.
     */
    public void setInheritsBlocks(boolean inheritsBlocks) {
        _inheritsBlocks = inheritsBlocks;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this Zone inherits mining drops from its parent Zone.
     *
     * @return true if this Zone inherits mining drops from its parent Zone.
     */
    public boolean getInheritsBlocks() {
        return _inheritsBlocks;
    }

    // ------------------------------------------------------------------------
    /**
     * Specify whether this Zone inherits mob replacements from its parent Zone.
     *
     * @param inheritsReplacements True if this Zone inherits mob replacements
     *                             from its parent Zone.
     */
    public void setInheritsReplacements(boolean inheritsReplacements) {
        _inheritsReplacements = inheritsReplacements;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this Zone inherits mob replacements from its parent Zone.
     *
     * @return true if this Zone inherits mob replacements from its parent Zone.
     */
    public boolean getInheritsReplacements() {
        return _inheritsReplacements;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the {@link DropSet} that will be consulted to determine what to drop
     * when a block is mined where this Zone applies.
     *
     * @param material  the type of the mined block.
     * @param dropSetId the ID of the set of drops to select from. If null,
     *                  remove the entry for the specified Material.
     */
    public void setMiningDropsId(Material material, String dropSetId) {
        if (dropSetId == null) {
            _miningDropsIds.remove(material);
        } else {
            _miningDropsIds.put(material, dropSetId);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the ID of the {@link DropSet} controlling drops when the specified
     * Material is mined.
     *
     * @param material the type of the mined block.
     * @param inherit  if true, mining drops inherited from the parent zone are
     *                 considered.
     * @return the ID of the {@link DropSet} controlling drops when the
     *         specified Material is mined, or null if this Zone does not
     *         override the drops.
     */
    public String getMiningDropsId(Material material, boolean inherit) {
        String id = _miningDropsIds.get(material);
        if (id == null && _parent != null && inherit && _inheritsBlocks) {
            return _parent.getMiningDropsId(material, inherit);
        } else {
            return id;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the {@link DropSet} controlling drops when the specified Material
     * is mined.
     *
     * @param material the type of the mined block.
     * @param inherit  if true, mining drops inherited from the parent zone are
     *                 considered.
     * @return the {@link DropSet} controlling drops when the specified Material
     *         is mined, or null if this Zone does not override the drops.
     */
    public DropSet getMiningDrops(Material material, boolean inherit) {
        return BeastMaster.LOOTS.getDropSet(getMiningDropsId(material, inherit));
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Zone that defines mining drops for a given block material.
     *
     * If this Zone defines drops directly, then return this Zone. Otherwise, if
     * mining drops are inherited, consider ancestor Zones.
     *
     * @param material the material of the broken block.
     * @return the Zone that defines mining drops for the specified block
     *         material, or null if neither this Zone nor its ancestors define
     *         such drops.
     */
    public Zone getMiningDropsDefiningZone(Material material) {
        if (_miningDropsIds.get(material) != null) {
            return this;
        } else {
            return (_parent != null && _inheritsBlocks) ? _parent.getMiningDropsDefiningZone(material)
                                                        : null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the map of Materials to corresponding mining loot tables.
     *
     * @param inherit if true, mining drops inherited from the parent zone are
     *                considered.
     * @return the map of Materials to corresponding mining loot tables.
     */
    public Map<Material, String> getAllMiningDrops(boolean inherit) {
        Map<Material, String> drops = new HashMap<>();
        if (_parent != null && inherit && _inheritsBlocks) {
            drops.putAll(_parent.getAllMiningDrops(inherit));
        }
        drops.putAll(_miningDropsIds);
        return drops;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the ID of the DropSet that defines replacement of newly spawned mobs
     * of the specified EntityType in this zone.
     *
     * @param entityType the EntityType of a newly spawned mob.
     * @param dropSetId  the ID of the DropSet, or null to disable replacement.
     */
    public void setMobReplacementDropSetId(EntityType entityType, String dropSetId) {
        if (dropSetId == null) {
            _mobReplacementDropSetIDs.remove(entityType);
        } else {
            _mobReplacementDropSetIDs.put(entityType, dropSetId);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the ID of the DropSet of replacements for newly spawned mobs of
     * the specified EntityType in this zone.
     *
     * @param entityType the EntityType of a newly spawned mob.
     * @param inherit    if true, mob replacements inherited from the parent
     *                   zone are considered.
     * @return the ID of the DropSet of replacements for newly spawned mobs of
     *         the specified EntityType in this zone.
     */
    public String getMobReplacementDropSetId(EntityType entityType, boolean inherit) {
        String id = _mobReplacementDropSetIDs.get(entityType);
        if (id == null && _parent != null && inherit && _inheritsReplacements) {
            return _parent.getMobReplacementDropSetId(entityType, inherit);
        } else {
            return id;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the DropSet of replacements for newly spawned mobs of the
     * specified EntityType in this zone.
     *
     * @param entityType the EntityType of a newly spawned mob.
     * @param inherit    if true, mob replacements inherited from the parent
     *                   zone are considered.
     * @return the DropSet of replacements for newly spawned mobs of the
     *         specified EntityType in this zone.
     */
    public DropSet getMobReplacementDropSet(EntityType entityType, boolean inherit) {
        return BeastMaster.LOOTS.getDropSet(getMobReplacementDropSetId(entityType, inherit));
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Zone that defines mob replacement for a given EntityType.
     *
     * If this Zone defines drops directly, then return this Zone. Otherwise, if
     * mining drops are inherited, consider ancestor Zones.
     *
     * @param entityType the EntityType of a newly spawned mob.
     * @return the Zone that defines mob replacement for a given EntityType, or
     *         null if neither this Zone nor its ancestors define such
     *         replacements.
     */
    public Zone getMobReplacementDefiningZone(EntityType entityType) {
        if (_mobReplacementDropSetIDs.get(entityType) != null) {
            return this;
        } else {
            return (_parent != null && _inheritsBlocks) ? _parent.getMobReplacementDefiningZone(entityType)
                                                        : null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the map of EntityType to corresponding mob replacement loot table.
     *
     * @param inherit if true, mob replacements inherited from the parent zone
     *                are considered.
     * @return the map of EntityType to corresponding mob replacement loot
     *         table.
     */
    public Map<EntityType, String> getAllReplacedEntityTypes(boolean inherit) {
        Map<EntityType, String> replacements = new HashMap<>();
        if (_parent != null && inherit && _inheritsReplacements) {
            replacements.putAll(_parent.getAllReplacedEntityTypes(inherit));
        }
        replacements.putAll(_mobReplacementDropSetIDs);
        return replacements;
    }

    // ------------------------------------------------------------------------
    /**
     * Specify whether this Zone replaces mobs spawned by spawner blocks.
     *
     * @param replacesSpawnerMobs true if this Zone replaces mobs spawned by
     *                            spawner blocks.
     */
    public void setReplacesSpawnerMobs(boolean replacesSpawnerMobs) {
        _replacesSpawnerMobs = replacesSpawnerMobs;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this Zone replaces mobs spawned by spawner blocks.
     *
     * @return true if this Zone replaces mobs spawned by spawner blocks.
     */
    public boolean replacesSpawnerMobs() {
        return _replacesSpawnerMobs;
    }

    // ------------------------------------------------------------------------
    /**
     * Load the properties of this Zone from the specified configuration
     * section, whose name is the Zone ID.
     *
     * This method doesn't load the parent-child hierarchy information; see
     * {@link #loadHierarchy(ConfigurationSection, Logger)}.
     *
     * @param zoneSection the configuration section.
     * @param logger      the logger.
     * @return true if the zone was loaded successfully.
     */
    public boolean loadProperties(ConfigurationSection zoneSection, Logger logger) {
        _id = zoneSection.getName();

        _specification = null;
        _expression = null;
        String specification = zoneSection.getString("specification");
        if (specification != null && !specification.isEmpty()) {
            try {
                Lexer lexer = new Lexer(specification);
                Parser parser = new Parser(lexer);
                setExpression(parser.parse());

            } catch (ParseError ex) {
                logger.severe("Error loading spec in zone " + _id +
                              " at column " + ex.getToken().getColumn() +
                              ": " + ex.getMessage());
            }
        }

        _inheritsBlocks = zoneSection.getBoolean("inherits-blocks");
        _inheritsReplacements = zoneSection.getBoolean("inherits-replacements");
        _replacesSpawnerMobs = zoneSection.getBoolean("replaces-spawner-mobs");

        _parent = null;
        _children.clear();

        _miningDropsIds.clear();
        ConfigurationSection miningDropsSection = zoneSection.getConfigurationSection("mining-drops");
        if (miningDropsSection != null) {
            for (String materialName : miningDropsSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName);
                    String dropSetId = miningDropsSection.getString(materialName);
                    setMiningDropsId(material, dropSetId);
                } catch (IllegalArgumentException ex) {
                    logger.severe(getId() + " defined mining drops that could not be loaded for unknown material " +
                                  materialName);
                }
            }
        }

        _mobReplacementDropSetIDs.clear();
        ConfigurationSection replacements = zoneSection.getConfigurationSection("replacements");
        if (replacements != null) {
            for (String entityTypeName : replacements.getKeys(false)) {
                try {
                    setMobReplacementDropSetId(EntityType.valueOf(entityTypeName), replacements.getString(entityTypeName));
                } catch (IllegalArgumentException ex) {
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Load the parent and children of this Zone.
     *
     * The parent and children of a Zone cannot be resolved by ID until they
     * have all been registered with the {@link ZoneManager}. So creation of
     * Zones and loading of basic properties happens in
     * {@link Zone#loadProperties(ConfigurationSection, Logger)} before this
     * method loads the hierarchy.
     *
     * @param zoneSection the configuration section.
     * @param logger      the logger.
     * @return true if the zone was loaded successfully.
     */
    public boolean loadHierarchy(ConfigurationSection zoneSection, Logger logger) {
        String parentId = zoneSection.getString("parent");
        if (parentId == null || parentId.isEmpty()) {
            _parent = null;
        } else {
            _parent = BeastMaster.ZONES.getZone(parentId);
            if (_parent == null) {
                // Parent ID, if specified, should be resolvable.
                logger.severe("zone " + getId() + " cannot load parent zone " + parentId);
                return false;
            }
        }

        _children.clear();
        List<String> childrenIds = zoneSection.getStringList("children");
        if (childrenIds != null) {
            for (int i = 0; i < childrenIds.size(); ++i) {
                String childId = childrenIds.get(i);
                Zone child = BeastMaster.ZONES.getZone(childId);
                if (child != null) {
                    _children.add(child);
                } else {
                    logger.severe("zone " + getId() +
                                  " cannot load child zone " + childId +
                                  " at index " + i);
                    return false;
                }
            }
        }

        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this zone as a child of the specified parent configuration section.
     *
     * @param parentSection the parent configuration section.
     * @param logger        the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection zoneSection = parentSection.createSection(getId());
        zoneSection.set("specification", formatExpression(_expression));
        zoneSection.set("inherits-blocks", _inheritsBlocks);
        zoneSection.set("inherits-replacements", _inheritsReplacements);
        zoneSection.set("replaces-spawner-mobs", _replacesSpawnerMobs);

        if (_parent != null) {
            zoneSection.set("parent", _parent.getId());
        }

        List<String> childZoneIds = new ArrayList<>();
        for (int i = 0; i < _children.size(); ++i) {
            childZoneIds.add(_children.get(i).getId());
        }
        zoneSection.set("children", childZoneIds);

        ConfigurationSection miningDropsSection = zoneSection.createSection("mining-drops");
        for (Entry<Material, String> entry : _miningDropsIds.entrySet()) {
            miningDropsSection.set(entry.getKey().name(), entry.getValue());
        }

        ConfigurationSection replacements = zoneSection.createSection("replacements");
        for (Entry<EntityType, String> entry : _mobReplacementDropSetIDs.entrySet()) {
            replacements.set(entry.getKey().name(), entry.getValue());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the human-readable description of the zone.
     *
     * @return the human-readable description of the zone.
     */
    public String getDescription() {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.YELLOW.toString());
        s.append(getId());

        int childCount = children().size();
        if (childCount != 0) {
            s.append(ChatColor.GRAY);
            if (childCount == 1) {
                s.append(" (1 child)");
            } else {
                s.append(" (");
                s.append(childCount);
                s.append(" children)");
            }
        }

        // Only show parent zone and inheritance for non-root zones.
        if (!isRoot()) {
            s.append(ChatColor.WHITE.toString());
            s.append(" in ");
            s.append(ChatColor.YELLOW.toString());
            // Safe if the root zone's World is not loaded:
            s.append(getRoot().getId());

            if (getInheritsReplacements() || getInheritsBlocks()) {
                s.append(ChatColor.WHITE.toString());
                s.append(" inherits ");
                s.append(ChatColor.YELLOW.toString());
                if (getInheritsReplacements()) {
                    s.append("replacements");
                    if (getInheritsBlocks()) {
                        s.append(ChatColor.WHITE.toString());
                        s.append(", ");
                        s.append(ChatColor.YELLOW.toString());
                    }
                }
                if (getInheritsBlocks()) {
                    s.append("blocks");
                }
            }

            if (_replacesSpawnerMobs) {
                s.append(ChatColor.WHITE.toString());
                s.append(" includes");
                s.append(ChatColor.YELLOW.toString());
                s.append(" spawners");
            }

            s.append(ChatColor.WHITE.toString());
            s.append(": ");
            s.append(getSpecification());
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Format the specified Expression as a String.
     *
     * @param expression the Expression.
     * @return the String representation of the Expression, or null if the
     *         Expression is null.
     */
    protected static String formatExpression(Expression expression) {
        if (expression != null) {
            StringBuilder formattedExpression = new StringBuilder();
            expression.visit(FORMAT, formattedExpression);
            return formattedExpression.toString();
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Visitor that evaluates {@link Expression}s.
     */
    protected static EvalExpressionVisitor EVALUATOR = new EvalExpressionVisitor(null);

    /**
     * Visitor that formats {@link Expression}s as Strings.
     */
    protected static FormatExpressionVisitor FORMAT = new FormatExpressionVisitor();

    /**
     * Unique programmatic identifier.
     */
    protected String _id;

    /**
     * The parent of this Zone, or null for root (world) zones.
     */
    protected Zone _parent;

    /**
     * The children of this Zone.
     */
    protected List<Zone> _children = new ArrayList<>();

    /**
     * The text of this Zone's specification; null for root Zones.
     */
    protected String _specification;

    /**
     * The {@link Expression} derived from parsing the specification.
     */
    protected Expression _expression;

    /**
     * Map from mined block type to ID of corresponding {@link DropSet}.
     */
    protected HashMap<Material, String> _miningDropsIds = new HashMap<>();

    /**
     * Map from EntityType to ID of DropSet to replace it with on spawn.
     */
    protected HashMap<EntityType, String> _mobReplacementDropSetIDs = new HashMap<>();

    /**
     * True if this Zone inherits mining drops from its parent Zone.
     */
    protected boolean _inheritsBlocks;

    /**
     * True if this Zone inherits mob replacements from its parent Zone.
     */
    protected boolean _inheritsReplacements;

    /**
     * True if this Zone replaces mobs spawned by spawner blocks.
     */
    protected boolean _replacesSpawnerMobs;

} // class Zone