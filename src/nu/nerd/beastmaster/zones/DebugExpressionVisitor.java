package nu.nerd.beastmaster.zones;

import java.io.PrintWriter;

import nu.nerd.beastmaster.zones.nodes.AndExpression;
import nu.nerd.beastmaster.zones.nodes.NotExpression;
import nu.nerd.beastmaster.zones.nodes.NumberExpression;
import nu.nerd.beastmaster.zones.nodes.OrExpression;
import nu.nerd.beastmaster.zones.nodes.PredicateExpression;
import nu.nerd.beastmaster.zones.nodes.StringExpression;
import nu.nerd.beastmaster.zones.nodes.XorExpression;

// ----------------------------------------------------------------------------
/**
 * An {@link ExpressionVisitor} that prints an {@link Expression} tree with
 * parentheses to a PrintWriter.
 */
public class DebugExpressionVisitor implements ExpressionVisitor {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param printer the PrintWriter. Note that, since none of the visit()
     *        implementations flush() the PrintWriter, no output will be seen
     *        unless the PrintWriter is flush()ed by the caller.
     */
    public DebugExpressionVisitor(PrintWriter printer) {
        _printer = printer;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.AndExpression)
     */
    @Override
    public void visit(AndExpression node) {
        _printer.print('(');
        node.firstChild().visit(this);
        _printer.print(" & ");
        node.secondChild().visit(this);
        _printer.print(')');
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.OrExpression)
     */
    @Override
    public void visit(OrExpression node) {
        _printer.print('(');
        node.firstChild().visit(this);
        _printer.print(" | ");
        node.secondChild().visit(this);
        _printer.print(')');
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.XorExpression)
     */
    @Override
    public void visit(XorExpression node) {
        _printer.print('(');
        node.firstChild().visit(this);
        _printer.print(" ^ ");
        node.secondChild().visit(this);
        _printer.print(')');
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.NotExpression)
     */
    @Override
    public void visit(NotExpression node) {
        _printer.print('!');
        node.firstChild().visit(this);
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.PredicateExpression)
     */
    @Override
    public void visit(PredicateExpression node) {
        _printer.print(node.getIdent());
        _printer.print("(");
        String sep = "";
        for (int i = 0; i < node.getChildCount(); ++i) {
            _printer.print(sep);
            node.getChild(i).visit(this);
            sep = ",";
        }
        _printer.print(")");
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.NumberExpression)
     */
    @Override
    public void visit(NumberExpression node) {
        _printer.print(node.getNumber());
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.beastmaster.zones.ExpressionVisitor#visit(nu.nerd.beastmaster.zones.nodes.StringExpression)
     */
    @Override
    public void visit(StringExpression node) {
        _printer.print("\"" + node.getText() + "\"");
    }

    // ------------------------------------------------------------------------
    /**
     * Prints the output.
     */
    protected PrintWriter _printer;
} // class DebugExpressionVisitor