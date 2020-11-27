package nu.nerd.beastmaster.zones;

import org.bukkit.Location;

import nu.nerd.beastmaster.zones.nodes.AndExpression;
import nu.nerd.beastmaster.zones.nodes.NotExpression;
import nu.nerd.beastmaster.zones.nodes.NumberExpression;
import nu.nerd.beastmaster.zones.nodes.OrExpression;
import nu.nerd.beastmaster.zones.nodes.PredicateExpression;
import nu.nerd.beastmaster.zones.nodes.StringExpression;
import nu.nerd.beastmaster.zones.nodes.XorExpression;

// ----------------------------------------------------------------------------
/**
 * An ExpressionVisitor implementation that evaluates the nodes of an Expression
 * tree.
 *
 * This implementation uses short-circuit evaluation of Boolean AND and OR.
 *
 * The context of the visit() method should be the Location to test.
 */
public class EvalExpressionVisitor implements ExpressionVisitor {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param trace if not null, is used to record a log of evaluation.
     */
    public EvalExpressionVisitor(StringBuilder trace) {
        _trace = trace;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.AndExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(AndExpression node, Object context) {
        if (_trace != null) {
            Boolean left = (Boolean) node.firstChild().visit(this, context);
            showBoolean(_trace, left);
            Boolean right = (Boolean) node.secondChild().visit(this, context);
            showBoolean(_trace, right);
            _trace.append("&");
            return left && right;
        }

        Boolean left = (Boolean) node.firstChild().visit(this, context);
        if (!left) {
            return false;
        }
        Boolean right = (Boolean) node.secondChild().visit(this, context);
        return right;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.OrExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(OrExpression node, Object context) {
        if (_trace != null) {
            Boolean left = (Boolean) node.firstChild().visit(this, context);
            showBoolean(_trace, left);
            Boolean right = (Boolean) node.secondChild().visit(this, context);
            showBoolean(_trace, right);
            _trace.append("|");
            return left || right;
        }

        Boolean left = (Boolean) node.firstChild().visit(this, context);
        if (left) {
            return true;
        }
        Boolean right = (Boolean) node.secondChild().visit(this, context);
        return right;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.XorExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(XorExpression node, Object context) {
        Boolean left = (Boolean) node.firstChild().visit(this, context);
        if (_trace != null) {
            showBoolean(_trace, left);
        }
        Boolean right = (Boolean) node.secondChild().visit(this, context);
        if (_trace != null) {
            showBoolean(_trace, right);
            _trace.append("^");

        }
        return left ^ right;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.NotExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(NotExpression node, Object context) {
        Boolean child = (Boolean) node.firstChild().visit(this, context);
        if (_trace != null) {
            showBoolean(_trace, child);
            _trace.append(" !");
        }
        return !child;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.PredicateExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(PredicateExpression node, Object context) {
        if (_trace != null) {
            _trace.append(node.getIdent());
            _trace.append("(");
            String sep = "";
            for (int i = 0; i < node.getChildCount(); ++i) {
                _trace.append(sep);
                node.getChild(i).visit(this, context);
                sep = ",";
            }
            _trace.append(")");
        }

        return node.matches((Location) context);
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.NumberExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(NumberExpression node, Object context) {
        if (_trace != null) {
            _trace.append(node.getNumber());
        }
        return node.getNumber();
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.StringExpression,
     *      java.lang.Object)
     */
    @Override
    public Object visit(StringExpression node, Object context) {
        if (_trace != null) {
            _trace.append(node.getText());
        }
        return node.getText();
    }

    // ------------------------------------------------------------------------
    /**
     * Append the value of a boolean expression to the trace.
     *
     * @param trace used to log a record of the evaluation.
     * @param b     the boolean value.
     */
    public void showBoolean(StringBuilder trace, boolean b) {
        trace.append(b ? " => T, " : " => F, ");
    }

    // ------------------------------------------------------------------------
    /**
     * If not null, records a log of evaluation.
     */
    protected StringBuilder _trace = new StringBuilder();
} // class EvalExpressionVisitor