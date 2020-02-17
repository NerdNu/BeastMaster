package nu.nerd.beastmaster.mobs;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.Drop;
import nu.nerd.beastmaster.DropSet;
import nu.nerd.beastmaster.DropType;
import nu.nerd.beastmaster.Item;
import nu.nerd.beastmaster.PotionSet;
import nu.nerd.entitymeta.EntityMeta;

// ----------------------------------------------------------------------------
/**
 * Represents a custom mob type.
 */
public class MobType {
    // ------------------------------------------------------------------------
    /**
     * Return the set of property names that are immutable for predefined Mob
     * Types.
     * 
     * @return the set of property names that are immutable for predefined Mob
     *         Types.
     */
    public static Set<String> getImmutablePredefinedPropertyNames() {
        return IMMUTABLE_PREDEFINED_PROPERTIES;
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor for loading.
     */
    public MobType() {
        this(null, null, false);
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor for custom mob types.
     * 
     * @param id the programmatic ID of this mob type.
     * @param id the programmatic ID of the parent mob type.
     */
    public MobType(String id, String parentTypeId) {
        this(id, null, false);
        setParentTypeId(parentTypeId);
    }

    // ------------------------------------------------------------------------
    /**
     * General purpose constructor.
     * 
     * @param id the programmatic ID of this mob type.
     * @param entityType the EntityType of the underlying vanilla mob.
     * @param predefined true if this mob type can be changed.
     */
    public MobType(String id, EntityType entityType, boolean predefined) {
        _id = id;
        _predefined = predefined;

        addProperties();
        getProperty("entity-type").setValue(entityType);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the programmatic ID of this mob type.
     * 
     * @return the programmatic ID of this mob type.
     */
    public String getId() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this mob type is predefined.
     * 
     * Predefined mob types correspond to the vanilla mob types. They cannot
     * haver their "entity-type" or "parent-type" property changed.
     * 
     * @return true if this mob type is predefined.
     */
    public boolean isPredefined() {
        return _predefined;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the parent mob type ID.
     * 
     * @param parentTypeId the parent type ID.
     */
    public void setParentTypeId(String parentTypeId) {
        getProperty("parent-type").setValue(parentTypeId);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the parent mob type ID, or null if unset.
     * 
     * @return the parent mob type ID, or null if unset.
     */
    public String getParentTypeId() {
        return (String) getProperty("parent-type").getValue();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the parent mob type, or null if unset or invalid.
     * 
     * @return the parent mob type, or null if unset or invalid.
     */
    public MobType getParentType() {
        return BeastMaster.MOBS.getMobType(getParentTypeId());
    }

    // ------------------------------------------------------------------------
    /**
     * Set the drops loot table ID.
     * 
     * @param dropsId the drops loot table ID.
     */
    public void setDropsId(String dropsId) {
        getProperty("drops").setValue(dropsId);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the ID of the DropSet consulted when this mob dies.
     * 
     * @return the ID of the DropSet consulted when this mob dies.
     */
    public String getDropsId() {
        return (String) getProperty("drops").getValue();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the DropSet consulted when this mob dies.
     * 
     * @return the DropSet consulted when this mob dies.
     */
    public DropSet getDrops() {
        String dropsId = getDropsId();
        return dropsId != null ? BeastMaster.LOOTS.getDropSet(dropsId) : null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all properties that this mob type can override.
     * 
     * @return a collection of all properties that this mob type can override.
     */
    public Collection<MobProperty> getAllProperties() {
        return _properties.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the set of all property IDs.
     * 
     * @return the set of all property IDs.
     */
    public static Set<String> getAllPropertyIds() {
        // All mobs have the same properties. Choose zombie, arbitarily.
        return BeastMaster.MOBS.getMobType("zombie")._properties.keySet();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the property of this mob type with the specified ID.
     * 
     * Note that this method does not consider property values inherited from
     * the parent type.
     * 
     * @param id the property ID.
     * @return the property.
     */
    public MobProperty getProperty(String id) {
        return _properties.get(id);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the property with the specified ID derived by considering
     * inherited property values as well as the properties overridden by this
     * mob type.
     * 
     * Properties that have a null value ({@link MobProperty#getValue()}) do not
     * override whatever was inherited from the ancestor mob types.
     * 
     * @param id the property ID.
     * @return the {@link MobProperty} instance that has a non-null value
     *         belonging to the most-derived mob type in the hierarchy, or the
     *         root ancestor of the hierarchy if no mob type overrides that
     *         property. Return null if there is no property with the specified
     *         ID. The return value will always be non-null if the property ID
     *         is valid.
     */
    public MobProperty getDerivedProperty(String id) {
        MobProperty property = getProperty(id);
        if (property == null) {
            return null;
        }

        for (;;) {
            if (property.getValue() != null) {
                // Overridden property belonging to most-derived mob type.
                return property;
            }
            MobType owner = property.getMobType();
            MobType parent = owner.getParentType();
            if (parent == null) {
                // Root ancestor when not overridden.
                return property;
            } else {
                property = parent.getProperty(id);
                // Invariant: all mob types have the same property IDs.
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Load this mob type from the specified section.
     * 
     * @param section the configuration file section.
     * @return true if successful.
     */
    public boolean load(ConfigurationSection section, Logger logger) {
        _id = section.getName();
        ConfigurationSection propertiesSection = section.getConfigurationSection("properties");
        if (propertiesSection != null) {
            for (MobProperty property : getAllProperties()) {
                property.load(propertiesSection, logger);
            }
        } else {
            logger.warning("Mob type " + _id + " overrides no properties.");
        }

        // TODO: check properties on load, e.g. verify drops table existence.
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this mob type as a child of the specified parent configuration
     * section.
     * 
     * @param parentSection the parent configuration section.
     * @param logger the logger.
     */
    public void save(ConfigurationSection parentSection, Logger logger) {
        ConfigurationSection section = parentSection.createSection(getId());
        ConfigurationSection propertiesSection = section.createSection("properties");
        for (MobProperty property : getAllProperties()) {
            property.save(propertiesSection, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Configure a mob according to this mob type.
     * 
     * @param mob the mob.
     */
    public void configureMob(LivingEntity mob) {
        EntityMeta.api().set(mob, BeastMaster.PLUGIN, "mob-type", getId());

        for (String propertyId : getAllPropertyIds()) {
            getDerivedProperty(propertyId).configureMob(mob, BeastMaster.PLUGIN.getLogger());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return a short string description of this type.
     * 
     * This is a short (single line) description with just the basic details.
     * Immutable (built-in) mob types have their ID shown in green.
     * 
     * @return a short string description of this type.
     */
    public String getShortDescription() {
        StringBuilder desc = new StringBuilder();
        if (!isPredefined()) {
            desc.append(ChatColor.WHITE).append("id: ");
            desc.append(ChatColor.YELLOW).append(getId());
            desc.append(ChatColor.WHITE).append(", parent-type: ");
            desc.append(getParentType() != null ? ChatColor.GREEN : ChatColor.RED);
            desc.append(getParentTypeId());
        } else {
            desc.append(ChatColor.YELLOW).append(getId());
        }
        // TODO: Add properties.
        return desc.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * This method is called to check that the mob type is mutable before an
     * attempt is made to change its properties.
     * 
     * @throws AssertionException if the mob type is not mutable.
     */
    protected void checkMutable() {
        if (!isPredefined()) {
            throw new AssertionError("This mob type is not mutable.");
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Add the specified property.
     * 
     * @param property the property.
     */
    protected void addProperty(MobProperty property) {
        _properties.put(property.getId(), property);
        property.setMobType(this);
    }

    // ------------------------------------------------------------------------
    /**
     * Add standard properties of this mob type.
     */
    protected void addProperties() {
        // TODO: Many of these need get/set/range implementations.
        addProperty(new MobProperty("parent-type", DataType.STRING, null));
        addProperty(new MobProperty("entity-type", DataType.ENTITY_TYPE, null));
        addProperty(new MobProperty("drops", DataType.LOOT, null));

        // Note: configureMob() lambdas only called if property value non-null.
        addProperty(new MobProperty("health", DataType.DOUBLE,
            (mob, logger) -> {
                AttributeInstance attribute = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                attribute.setBaseValue((Double) getDerivedProperty("health").getValue());
                mob.setHealth(attribute.getBaseValue());
            }));
        addProperty(new MobProperty("experience", DataType.INTEGER, null));
        addProperty(new MobProperty("follow-range", DataType.DOUBLE,
            (mob, logger) -> {
                AttributeInstance attribute = mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
                attribute.setBaseValue((Double) getDerivedProperty("follow-range").getValue());
            }));
        addProperty(new MobProperty("attack-damage", DataType.DOUBLE,
            (mob, logger) -> {
                AttributeInstance attribute = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                attribute.setBaseValue((Double) getDerivedProperty("attack-damage").getValue());
            }));
        addProperty(new MobProperty("attack-speed", DataType.DOUBLE,
            (mob, logger) -> {
                AttributeInstance attribute = mob.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
                attribute.setBaseValue((Double) getDerivedProperty("attack-speed").getValue());
            }));
        addProperty(new MobProperty("speed", DataType.DOUBLE,
            (mob, logger) -> {
                AttributeInstance attribute = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                attribute.setBaseValue((Double) getDerivedProperty("speed").getValue());
            }));
        addProperty(new MobProperty("flying-speed", DataType.DOUBLE,
            (mob, logger) -> {
                AttributeInstance attribute = mob.getAttribute(Attribute.GENERIC_FLYING_SPEED);
                if (attribute != null) {
                    attribute.setBaseValue((Double) getDerivedProperty("flying-speed").getValue());
                }
            }));
        addProperty(new MobProperty("pick-up-percent", DataType.DOUBLE,
            (mob, logger) -> {
                mob.setCanPickupItems(Math.random() * 100 < (Double) getDerivedProperty("pick-up-percent").getValue());
            }));
        addProperty(new MobProperty("baby-percent", DataType.DOUBLE,
            (mob, logger) -> {
                boolean isBaby = (Math.random() * 100 < (Double) getDerivedProperty("baby-percent").getValue());
                if (mob instanceof Ageable) {
                    if (isBaby) {
                        ((Ageable) mob).setBaby();
                    } else {
                        ((Ageable) mob).setAdult();
                    }
                } else if (mob instanceof Zombie) {
                    ((Zombie) mob).setBaby(isBaby);
                }
            }));
        addProperty(new MobProperty("charged-percent", DataType.DOUBLE,
            (mob, logger) -> {
                if (mob instanceof Creeper) {
                    ((Creeper) mob).setPowered(Math.random() * 100 < (Double) getDerivedProperty("charged-percent").getValue());
                }
            }));
        addProperty(new MobProperty("size", DataType.INTEGER,
            (mob, logger) -> {
                if (mob instanceof Phantom) {
                    ((Phantom) mob).setSize((Integer) getDerivedProperty("size").getValue());
                } else if (mob instanceof Slime) {
                    ((Slime) mob).setSize((Integer) getDerivedProperty("size").getValue());
                }
            }));
        addProperty(new MobProperty("glowing", DataType.BOOLEAN,
            (mob, logger) -> {
                mob.setGlowing((Boolean) getDerivedProperty("glowing").getValue());
            }));
        addProperty(new MobProperty("name", DataType.STRING,
            (mob, logger) -> {
                mob.setCustomName(ChatColor.translateAlternateColorCodes('&', (String) getDerivedProperty("name").getValue()));
            }));
        addProperty(new MobProperty("show-name-plate", DataType.BOOLEAN,
            (mob, logger) -> {
                mob.setCustomNameVisible((Boolean) getDerivedProperty("show-name-plate").getValue());
            }));
        addProperty(new MobProperty("breath-seconds", DataType.INTEGER,
            (mob, logger) -> {
                int ticks = 20 * (Integer) getDerivedProperty("breath-seconds").getValue();
                mob.setMaximumAir(ticks);
                mob.setRemainingAir(ticks);
            }));
        addProperty(new MobProperty("helmet", DataType.LOOT_OR_ITEM,
            (mob, logger) -> {
                String id = (String) getDerivedProperty("helmet").getValue();
                ItemStack itemStack = getEquipmentItem(id);
                if (itemStack != null) {
                    mob.getEquipment().setHelmet(itemStack);
                }
            }));
        addProperty(new MobProperty("helmet-drop-percent", DataType.DOUBLE,
            (mob, logger) -> {
                double percent = (Double) getDerivedProperty("helmet-drop-percent").getValue();
                mob.getEquipment().setHelmetDropChance((float) percent / 100);
            }));
        addProperty(new MobProperty("chest-plate", DataType.LOOT_OR_ITEM,
            (mob, logger) -> {
                String id = (String) getDerivedProperty("chest-plate").getValue();
                ItemStack itemStack = getEquipmentItem(id);
                if (itemStack != null) {
                    mob.getEquipment().setChestplate(itemStack);
                }
            }));
        addProperty(new MobProperty("chest-plate-drop-percent", DataType.DOUBLE,
            (mob, logger) -> {
                double percent = (Double) getDerivedProperty("chest-plate-drop-percent").getValue();
                mob.getEquipment().setChestplateDropChance((float) percent / 100);
            }));
        addProperty(new MobProperty("leggings", DataType.LOOT_OR_ITEM,
            (mob, logger) -> {
                String id = (String) getDerivedProperty("leggings").getValue();
                ItemStack itemStack = getEquipmentItem(id);
                if (itemStack != null) {
                    mob.getEquipment().setLeggings(itemStack);
                }
            }));
        addProperty(new MobProperty("leggings-drop-percent", DataType.DOUBLE,
            (mob, logger) -> {
                double percent = (Double) getDerivedProperty("leggings-drop-percent").getValue();
                mob.getEquipment().setLeggingsDropChance((float) percent / 100);
            }));
        addProperty(new MobProperty("boots", DataType.LOOT_OR_ITEM,
            (mob, logger) -> {
                String id = (String) getDerivedProperty("boots").getValue();
                ItemStack itemStack = getEquipmentItem(id);
                if (itemStack != null) {
                    mob.getEquipment().setBoots(itemStack);
                }
            }));
        addProperty(new MobProperty("boots-drop-percent", DataType.DOUBLE,
            (mob, logger) -> {
                double percent = (Double) getDerivedProperty("boots-drop-percent").getValue();
                mob.getEquipment().setBootsDropChance((float) percent / 100);
            }));
        addProperty(new MobProperty("main-hand", DataType.LOOT_OR_ITEM,
            (mob, logger) -> {
                String id = (String) getDerivedProperty("main-hand").getValue();
                ItemStack itemStack = getEquipmentItem(id);
                if (itemStack != null) {
                    mob.getEquipment().setItemInMainHand(itemStack);
                }
            }));
        addProperty(new MobProperty("main-hand-drop-percent", DataType.DOUBLE,
            (mob, logger) -> {
                double percent = (Double) getDerivedProperty("main-hand-drop-percent").getValue();
                mob.getEquipment().setItemInMainHandDropChance((float) percent / 100);
            }));
        addProperty(new MobProperty("off-hand", DataType.LOOT_OR_ITEM,
            (mob, logger) -> {
                String id = (String) getDerivedProperty("off-hand").getValue();
                ItemStack itemStack = getEquipmentItem(id);
                if (itemStack != null) {
                    mob.getEquipment().setItemInOffHand(itemStack);
                }
            }));
        addProperty(new MobProperty("off-hand-drop-percent", DataType.DOUBLE,
            (mob, logger) -> {
                double percent = (Double) getDerivedProperty("off-hand-drop-percent").getValue();
                mob.getEquipment().setItemInOffHandDropChance((float) percent / 100);
            }));
        // Added after custom name => will clear PersistenceRequired NBT.
        addProperty(new MobProperty("can-despawn", DataType.BOOLEAN,
            (mob, logger) -> {
                boolean canDespawn = (Boolean) getDerivedProperty("can-despawn").getValue();
                mob.setRemoveWhenFarAway(canDespawn);
            }));
        addProperty(new MobProperty("potion-buffs", DataType.POTION_SET,
            (mob, logger) -> {
                String potionSetId = (String) getDerivedProperty("potion-buffs").getValue();
                PotionSet potionSet = BeastMaster.POTIONS.getPotionSet(potionSetId);
                if (potionSet != null) {
                    potionSet.apply(mob);
                }
            }));
        addProperty(new MobProperty("attack-potions", DataType.POTION_SET, (mob, logger) -> {
        }));
        addProperty(new MobProperty("disguise", DataType.DISGUISE,
            (mob, logger) -> {
                String encodedDisguise = (String) getDerivedProperty("disguise").getValue();
                BeastMaster.DISGUISES.createDisguise(mob, mob.getWorld(), encodedDisguise);
            }));
        addProperty(new MobProperty("passenger", DataType.LOOT_OR_MOB,
            (mob, logger) -> {
                // If passenger-percent is unset but passenger is, the chance is
                // implicitly 100%.
                MobProperty percent = getDerivedProperty("passenger-percent");
                boolean hasPassenger = (percent.getValue() == null) ? true
                                                                    : (Math.random() * 100 < (Double) percent.getValue());
                if (!hasPassenger) {
                    return;
                }

                // The passenger property may be a loot table or a mob type.
                String id = (String) getDerivedProperty("passenger").getValue();
                DropSet drops = BeastMaster.LOOTS.getDropSet(id);
                MobType mobType = null;
                if (drops != null) {
                    Drop drop = drops.chooseOneDrop();
                    if (drop != null) {
                        if (drop.getDropType() == DropType.MOB) {
                            mobType = BeastMaster.MOBS.getMobType(drop.getId());
                        }
                    }
                } else {
                    mobType = BeastMaster.MOBS.getMobType(id);
                }

                if (mobType != null) {
                    LivingEntity passenger = BeastMaster.PLUGIN.spawnMob(mob.getLocation(), mobType, false);
                    if (passenger != null) {
                        mob.addPassenger(passenger);
                    }
                }
            }));
        addProperty(new MobProperty("passenger-percent", DataType.DOUBLE, null));

        // TODO: list of escort mobs? Or separate that?
        // TODO: particle effects tracking mob, projectiles, attack hit points.
    }

    // ------------------------------------------------------------------------
    /**
     * Return an equipment ItemStack to apply to a mob in
     * {@link #configureMob(LivingEntity)}.
     * 
     * Mob properties corresponding to equipment items (helmet, chest-plate,
     * leggings, boots, main-hand, off-hand) are Strings that are interpreted as
     * either the ID of a {@link DropSet} or the ID of an {@link Item}. This
     * method attempts to look up the DropSet first, and if one with the
     * specified ID doesn't exist, the ID is interpreted as that of an Item.
     * 
     * @param id the ID of the DropSet or Item to generate.
     * @return the equipment as an ItemStack, or null if the equipment should
     *         not change (be default).
     */
    protected static ItemStack getEquipmentItem(String id) {
        DropSet drops = BeastMaster.LOOTS.getDropSet(id);
        if (drops != null) {
            Drop drop = drops.chooseOneDrop();
            if (drop != null) {
                if (drop.getDropType() == DropType.NOTHING) {
                    return new ItemStack(Material.AIR);
                } else if (drop.getDropType() == DropType.ITEM) {
                    return drop.randomItemStack();
                }
            }
            return null;
        } else {
            Item item = BeastMaster.ITEMS.getItem(id);
            return (item != null) ? item.getItemStack().clone() : null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The set of property names that are immutable for predefined Mob Types.
     */
    protected static HashSet<String> IMMUTABLE_PREDEFINED_PROPERTIES = new HashSet<>();
    static {
        IMMUTABLE_PREDEFINED_PROPERTIES.addAll(Arrays.asList("parent-type", "entity-type"));
    }

    /**
     * The ID of this mob type.
     */
    protected String _id;

    /**
     * True if this mob type is predefined (corresponds to a vanilla mob).
     */
    protected boolean _predefined;

    /**
     * Map from property ID to {@link MobProperty} instance.
     * 
     * Properties are enumerated in the order they were added by
     * {@link #addProperties()}.
     */
    protected LinkedHashMap<String, MobProperty> _properties = new LinkedHashMap<>();
} // class MobType