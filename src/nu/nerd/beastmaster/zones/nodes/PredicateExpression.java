package nu.nerd.beastmaster.zones.nodes;

import java.util.ArrayList;
import java.util.List;

import nu.nerd.beastmaster.zones.Expression;
import nu.nerd.beastmaster.zones.ExpressionVisitor;

// ----------------------------------------------------------------------------
/**
 * A Zone Specification sub-expression that represents a predicate (function)
 * call.
 */
public class PredicateExpression extends Expression {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param ident the identifier of this predicate.
     */
    public PredicateExpression(String ident) {
        _ident = ident;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the identifier of this predicate.
     * 
     * @return the identifier of this predicate.
     */
    public String getIdent() {
        return _ident;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.Expression#visit(nu.nerd.beastmaster.zones.ExpressionVisitor,
     *      java.lang.Object)
     */
    @Override
    public Object visit(ExpressionVisitor visitor, Object context) {
        return visitor.visit(this, context);
    }

    // ------------------------------------------------------------------------
    /**
     * Predicate arguments from child nodes, cached and exposed for efficiency.
     */
    public List<Object> args = new ArrayList<>();

    // ------------------------------------------------------------------------

    /**
     * The identifier of this predicate.
     */
    protected String _ident;
} // class PredicateExpression