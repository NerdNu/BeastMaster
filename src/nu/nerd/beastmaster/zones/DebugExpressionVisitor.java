package nu.nerd.beastmaster.zones;

import nu.nerd.beastmaster.zones.nodes.AndExpression;
import nu.nerd.beastmaster.zones.nodes.NotExpression;
import nu.nerd.beastmaster.zones.nodes.NumberExpression;
import nu.nerd.beastmaster.zones.nodes.OrExpression;
import nu.nerd.beastmaster.zones.nodes.PredicateExpression;
import nu.nerd.beastmaster.zones.nodes.StringExpression;
import nu.nerd.beastmaster.zones.nodes.XorExpression;

// ----------------------------------------------------------------------------
/**
 * An {@link ExpressionVisitor} that appends a String representation of an
 * {@link Expression} tree to a StringBuilder in the context argument of
 * visit().
 */
public class DebugExpressionVisitor implements ExpressionVisitor {
    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.AndExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(AndExpression node, Object context) {
        StringBuilder sb = (StringBuilder) context;
        sb.append('(');
        node.firstChild().visit(this, context);
        sb.append(" & ");
        node.secondChild().visit(this, context);
        sb.append(')');
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.OrExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(OrExpression node, Object context) {
        StringBuilder sb = (StringBuilder) context;
        sb.append('(');
        node.firstChild().visit(this, context);
        sb.append(" | ");
        node.secondChild().visit(this, context);
        sb.append(')');
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.XorExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(XorExpression node, Object context) {
        StringBuilder sb = (StringBuilder) context;
        sb.append('(');
        node.firstChild().visit(this, context);
        sb.append(" ^ ");
        node.secondChild().visit(this, context);
        sb.append(')');
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.NotExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(NotExpression node, Object context) {
        StringBuilder sb = (StringBuilder) context;
        sb.append('!');
        node.firstChild().visit(this, context);
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.PredicateExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(PredicateExpression node, Object context) {
        StringBuilder sb = (StringBuilder) context;
        sb.append(node.getIdent());
        sb.append("(");
        String sep = "";
        for (int i = 0; i < node.getChildCount(); ++i) {
            sb.append(sep);
            node.getChild(i).visit(this, context);
            sep = ",";
        }
        sb.append(")");
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.NumberExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(NumberExpression node, Object context) {
        StringBuilder sb = (StringBuilder) context;
        sb.append(node.getNumber());
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.StringExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(StringExpression node, Object context) {
        StringBuilder sb = (StringBuilder) context;
        sb.append("\"" + node.getText() + "\"");
        return null;
    }
} // class DebugExpressionVisitor