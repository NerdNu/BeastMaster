package nu.nerd.beastmaster;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

// ----------------------------------------------------------------------------
/**
 * Manages a collection of elements with associated probability of selection
 * weights and allows random selection of elements.
 * 
 * The probability of choosing a particular element is its weight divided by the
 * sum of all weights.
 */
public class WeightedSelection<E> {
    // --------------------------------------------------------------------------
    /**
     * Main program for a bit of interactive testing.
     */
    public static void main(String[] args) {
        WeightedSelection<String> w = new WeightedSelection<String>();
        w.addChoice("A", 1);
        w.addChoice("B", 2);
        // w.addChoice("C", 4);
        // w.addChoice("D", 8);
        System.out.println(w);
        System.out.println(w.removeChoice("A"));
        System.out.println(w);
    }

    // --------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public WeightedSelection() {
        this(new Random());
    }

    // --------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param random the random number generator to use.
     */
    public WeightedSelection(Random random) {
        _random = random;
    }

    // --------------------------------------------------------------------------
    /**
     * Remove all choices.
     */
    public void clear() {
        _total = 0;
        _choices.clear();
    }

    // --------------------------------------------------------------------------
    /**
     * Add a choice.
     * 
     * @param choice the chosen object.
     * @param weight its probability weight; this must be greater than 0, or the
     *        choice is not added.
     */
    public void addChoice(E choice, double weight) {
        if (weight > 0) {
            _total += weight;
            _choices.put(_total, choice);
        }
    }

    // --------------------------------------------------------------------------
    /**
     * Remove a choice.
     * 
     * @param choice the choice to remove.
     * @return the removed choice, or null if not found.
     */
    public E removeChoice(E choice) {
        E removed = null;

        // Build up new field values to swap in.
        // All choices after the removed choice need their weights adjusted
        // down by the difference between the removed key and the key before.
        // We can't find a choice by key, so we need to iterate over all
        // elements. Therefore this method isn't drastically inefficient
        // compared to the fastest implementation.
        double newTotal = 0;
        TreeMap<Double, E> newChoices = new TreeMap<Double, E>();

        double keyBefore = 0;
        for (Entry<Double, E> entry : _choices.entrySet()) {
            // The original weight is the difference between successive keys.
            double weight = entry.getKey() - keyBefore;
            keyBefore = entry.getKey();

            if (entry.getValue().equals(choice)) {
                removed = entry.getValue();
            } else {
                newTotal += weight;
                newChoices.put(newTotal, entry.getValue());
            }
        }

        _total = newTotal;
        _choices = newChoices;
        return removed;
    }

    // --------------------------------------------------------------------------
    /**
     * Return a randomly selected element, or null if there is nothing to
     * choose.
     * 
     * @return a randomly selected element, or null if there is nothing to
     *         choose.
     */
    public E choose() {
        double value = _random.nextDouble() * _total;

        // Using floorEntry() is more correct here in the sense that it gives
        // the lower choice right on the boundary, and Random.nextDouble()
        // returns[0.0,1.0). But floorEntry() makes it much harder to extract
        // weights and thresholds from the entries.
        Entry<Double, E> entry = _choices.ceilingEntry(value);
        return (entry != null) ? entry.getValue() : null;
    }

    // --------------------------------------------------------------------------
    /**
     * Return the entrySet() of the underlying collection (mainly for
     * debugging).
     * 
     * @rReturn the entrySet() of the underlying collection
     */
    public Set<Entry<Double, E>> entrySet() {
        return _choices.entrySet();
    }

    // --------------------------------------------------------------------------
    /**
     * Return all choices and their weights as a string, for debugging.
     * 
     * @return all choices and their weights as a string, for debugging.
     */
    @Override
    public String toString() {
        return _choices.entrySet().stream()
        .map(e -> e.getKey() + ": " + e.getValue())
        .collect(Collectors.joining(", "));
    }

    // --------------------------------------------------------------------------
    /**
     * Return the sum of all of the probability weights.
     * 
     * @return the sum of all of the probability weights.
     */
    public double getTotalWeight() {
        return _total;
    }

    // --------------------------------------------------------------------------
    /**
     * The random number generator.
     */
    protected Random _random;

    /**
     * Sum of all of the weights of all choices.
     */
    protected double _total = 0;

    /**
     * Sum of all of the weights of all choices.
     */
    protected NavigableMap<Double, E> _choices = new TreeMap<Double, E>();
} // class WeightedSelection
