package nu.nerd.beastmaster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;

// ----------------------------------------------------------------------------
/**
 * Utility methods.
 */
public class Util {
    // ------------------------------------------------------------------------
    /**
     * Return a string describing a dropped item stack.
     *
     * The string contains the material type name, data value and amount, as
     * well as a list of enchantments. It is used in methods that log drops.
     *
     * @param item the droppped item stack.
     * @return a string describing a dropped item stack.
     */
    public static String getItemDescription(ItemStack item) {
        if (item == null) {
            return "null";
        }

        StringBuilder description = new StringBuilder();
        if (item.getAmount() != 1) {
            description.append(item.getAmount()).append('x');
        }
        description.append(item.getType().name()).append(':').append(item.getDurability());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta instanceof SkullMeta) {
                SkullMeta skullMeta = (SkullMeta) meta;
                if (skullMeta.getOwner() != null) {
                    description.append(" of \"").append(skullMeta.getOwner()).append("\"");
                }
            } else if (meta instanceof SpawnEggMeta) {
                SpawnEggMeta eggMeta = (SpawnEggMeta) meta;
                description.append(" of ").append(eggMeta.getSpawnedType());
            } else if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta bookEnchants = (EnchantmentStorageMeta) meta;
                description.append(" with").append(enchantsToString(bookEnchants.getStoredEnchants()));
            } else if (meta instanceof BookMeta) {
                BookMeta bookMeta = (BookMeta) meta;
                if (bookMeta.getTitle() != null) {
                    description.append(" titled \"").append(bookMeta.getTitle()).append("\"");
                }
                if (bookMeta.getAuthor() != null) {
                    description.append(" by ").append(bookMeta.getAuthor());
                }
            } else if (meta instanceof PotionMeta) {
                PotionMeta potionMeta = (PotionMeta) meta;
                description.append(" of ");
                PotionData data = potionMeta.getBasePotionData();
                description.append(data.getType());
                if (data.isExtended()) {
                    description.append(" extended");
                }
                if (data.isUpgraded()) {
                    description.append(" upgraded");
                }

                List<PotionEffect> effects = potionMeta.getCustomEffects();
                if (effects != null && !effects.isEmpty()) {
                    description.append(" with ");
                    String sep = "";
                    for (PotionEffect effect : potionMeta.getCustomEffects()) {
                        description.append(sep).append(potionToString(effect));
                        sep = "+";
                    }
                }
            }

            if (meta.getDisplayName() != null && !meta.getDisplayName().isEmpty()) {
                description.append(" named \"").append(meta.getDisplayName()).append("\"").append(ChatColor.WHITE);
            }

            List<String> lore = meta.getLore();
            if (lore != null && !lore.isEmpty()) {
                description.append(" lore \"").append(String.join("|", lore)).append("\"").append(ChatColor.WHITE);
            }
        }

        description.append(enchantsToString(item.getEnchantments()));
        return description.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the string description of a potion effect.
     *
     * @param effect the effect.
     * @return the description.
     */
    public static String potionToString(PotionEffect effect) {
        StringBuilder description = new StringBuilder();
        description.append(effect.getType().getName()).append("/");
        description.append(effect.getAmplifier() + 1).append("/");
        description.append(effect.getDuration() / 20.0).append('s');
        return description.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the string description of a set of enchantments.
     *
     * @param enchants map from enchantment type to level, from the Bukkit API.
     * @return the description.
     */
    public static String enchantsToString(Map<Enchantment, Integer> enchants) {
        StringBuilder description = new StringBuilder();
        if (enchants.size() > 0) {
            description.append(" (");
            String sep = "";
            for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                description.append(sep).append(entry.getKey().getName()).append(':').append(entry.getValue());
                sep = ",";
            }
            description.append(')');
        }
        return description.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Format a Location as a string.
     *
     * @param loc the Location.
     * @return a String containing (world, x, y, z).
     */
    public static String formatLocation(Location loc) {
        StringBuilder s = new StringBuilder();
        s.append('(').append(loc.getWorld().getName());
        s.append(", ").append(loc.getBlockX());
        s.append(", ").append(loc.getBlockY());
        s.append(", ").append(loc.getBlockZ());
        return s.append(')').toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Given a sequence of strings, create a string representing the
     * concatenation of all elements in the collection, alternately colour1 then
     * colour2, with the specified separator interposed between consecutive
     * sequence elements.
     * 
     * @param strings the sequence of strings.
     * @param separator the separator, which can include colour codes.
     * @param colour1 the colour of the first element and every second element
     *        after that.
     * @param colour2 the colour of the second element and every second element
     *        after that.
     * @returns a string representing the list, alternating the colours of
     *          elements.
     */
    public static String alternateColours(Collection<String> strings, String separator, ChatColor colour1, ChatColor colour2) {
        StringBuilder result = new StringBuilder();
        int index = 0;
        String sep = "";
        for (String s : strings) {
            ChatColor colour = (index++ & 1) == 0 ? colour1 : colour2;
            result.append(sep).append(colour).append(s);
            sep = separator;
        }
        return result.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if a block of the specified Material could "safely"
     * accommodate a drop.
     * 
     * "Safely" in this instance means that the drop won't disappear in some
     * way. Fire and lava would destroy the block; (parially) solid materials
     * would cause the drop to shoot up into the air; void air would let the
     * drop fall into the void.
     * 
     * @return true if a block of the specified Material would be a good place
     *         for a drop.
     */
    public static boolean canAccomodateDrop(Material material) {
        return material == Material.AIR ||
               material == Material.CAVE_AIR ||
               material == Material.WATER;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random integer in the range [0,values-1].
     *
     * @param values the number of possible values.
     * @return a random integer in the range [0,values-1].
     */
    public static int randomInt(int values) {
        return _random.nextInt(values);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random selection from the specified list of options.
     *
     * @param options a list of possible options to return; must be non-empty.
     * @return a random selection from the list.
     */
    public static <T> T randomChoice(ArrayList<T> options) {
        return options.get(_random.nextInt(options.size()));
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random integer in the range [min,max].
     *
     * @param min the minimum possible value.
     * @param max the maximum possible value.
     * @return a random integer in the range [min,max].
     */
    public static int random(int min, int max) {
        return min + _random.nextInt(max - min + 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random double in the range [min,max].
     *
     * @param min the minimum possible value.
     * @param max the maximum possible value.
     * @return a random double in the range [min,max].
     */
    public static double random(double min, double max) {
        return min + _random.nextDouble() * (max - min);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random double in the range [0.0,1.0).
     *
     * @return a random double in the range [0.0,1.0).
     */
    public static double random() {
        return _random.nextDouble();
    }

    // ------------------------------------------------------------------------
    /**
     * Random number generator.
     */
    protected static Random _random = new Random();
} // class Util