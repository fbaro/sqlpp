package it.fb.sqlpp;

import com.facebook.presto.sql.ExpressionFormatter;
import com.facebook.presto.sql.tree.*;
import com.google.common.base.Joiner;
import it.fb.sqlpp.TreeLayout.NodeCode;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class StatementLayout extends DefaultTraversalVisitor<Object, NodeCode> {

    public static String format(int lineWidth, int indentWidth, Statement statement) {
        StatementLayout sl = new StatementLayout();
        return TreeLayout.format(lineWidth, indentWidth, sl.toTree(statement));
    }

    private StatementLayout() {
    }

    private Consumer<NodeCode> toTree(Node node) {
        return nc -> process(node, nc);
    }

    @Override
    protected Object visitIdentifier(Identifier node, NodeCode context) {
        return context.leaf(node.getValue());
    }

    @Override
    protected Object visitSymbolReference(SymbolReference node, NodeCode context) {
        return context.leaf(node.getName());
    }

    @Override
    protected Object visitLiteral(Literal node, NodeCode context) {
        return context.leaf(ExpressionFormatter.formatExpression(node, Optional.empty()));
    }

    @Override
    protected Object visitComparisonExpression(ComparisonExpression node, NodeCode context) {
        return context.child("", "", toTree(node.getLeft()))
                .child(node.getType().getValue(), "", toTree(node.getRight()));
    }

    @Override
    protected Object visitArithmeticBinary(ArithmeticBinaryExpression node, NodeCode context) {
        return context.child("", "", toTree(node.getLeft()))
                .child(node.getType().getValue(), "", toTree(node.getRight()));
    }

    @Override
    protected Object visitIsNullPredicate(IsNullPredicate node, NodeCode context) {
        return context.child("", "IS NULL", toTree(node.getValue()));
    }

    @Override
    protected Object visitIsNotNullPredicate(IsNotNullPredicate node, NodeCode context) {
        return context.child("", "IS NOT NULL", toTree(node.getValue()));
    }

    @Override
    protected Object visitBetweenPredicate(BetweenPredicate node, NodeCode context) {
        return context.child("", "", toTree(node.getValue()))
                .child("BETWEEN", "", toTree(node.getMin()))
                .child("AND", "", toTree(node.getMax()));
    }

    @Override
    protected Object visitExists(ExistsPredicate node, NodeCode context) {
        return context.child("EXISTS (", ")", toTree(node.getSubquery()));
    }

    @Override
    protected Object visitSubqueryExpression(SubqueryExpression node, NodeCode context) {
        return context.child("(", ")", toTree(node.getQuery()));
    }

    @Override
    protected Object visitLogicalBinaryExpression(LogicalBinaryExpression node, NodeCode context) {
        return context.child("", "", toTree(node.getLeft()))
                .child(node.getType().name(), "", toTree(node.getRight()));
    }

    @Override
    protected Void visitSelect(Select node, NodeCode context) {
        List<SelectItem> selectItems = node.getSelectItems();
        int l = selectItems.size();
        for (int i = 0; i < l; i++) {
            SelectItem item = selectItems.get(i);
            context.child("", i == l - 1 ? "" : ",", toTree(item));
        }
        return null;
    }

    @Override
    protected Object visitAllColumns(AllColumns node, NodeCode context) {
        return context.leaf(node.getPrefix()
                .map(qn -> qn.toString() + ".*")
                .orElse("*"));
    }

    @Override
    protected Object visitSingleColumn(SingleColumn node, NodeCode context) {
        return context.child("",
                node.getAlias().map(Identifier::getValue).orElse(""),
                toTree(node.getExpression()));
    }

    @Override
    protected Void visitQuerySpecification(QuerySpecification node, NodeCode context) {
        context.child("SELECT", "", toTree(node.getSelect()));
        if (node.getFrom().isPresent()) {
            context.child("FROM", "", toTree(node.getFrom().get()));
        }
        if (node.getWhere().isPresent()) {
            context.child("WHERE", "", toTree(node.getWhere().get()));
        }
        return null;
    }

    @Override
    protected Object visitAliasedRelation(AliasedRelation node, NodeCode context) {
        return context.child("", node.getAlias().getValue(), toTree(node.getRelation()));
    }

    @Override
    protected Object visitTable(Table node, NodeCode context) {
        return context.leaf(toString(node.getName()));
    }

    @Override
    protected Object visitJoin(Join node, NodeCode context) {
        if (!node.getCriteria().isPresent()) {
            return context.child("", "", toTree(node.getLeft()))
                    .child(toString(node.getType()), "", toTree(node.getRight()));
        } else if (node.getCriteria().get() instanceof JoinOn) {
            return context.child("", "", toTree(node.getLeft()))
                    .child(toString(node.getType()), "", inCtx -> {
                        process(node.getRight(), inCtx);
                        inCtx.child("ON", "", toTree(((JoinOn) node.getCriteria().get()).getExpression()));
                    });
        } else {
            throw new UnsupportedOperationException("Unsupported join criteria: " + node.getCriteria());
        }
    }

    private String toString(QualifiedName qn) {
        return Joiner.on('.').join(qn.getOriginalParts());
    }

    private String toString(Join.Type joinType) {
        switch (joinType) {
            case CROSS:
            case INNER:
            case LEFT:
            case RIGHT:
                return joinType.name() + " JOIN";
            default:
                throw new UnsupportedOperationException("TODO: Other join types");
        }
    }
}
