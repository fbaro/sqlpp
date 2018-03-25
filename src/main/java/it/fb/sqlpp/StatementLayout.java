package it.fb.sqlpp;

import com.facebook.presto.sql.ExpressionFormatter;
import com.facebook.presto.sql.tree.*;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import it.fb.sqlpp.TreeLayout.NodeCode;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class StatementLayout extends DefaultTraversalVisitor<NodeCode, NodeCode> {

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
    protected NodeCode visitIdentifier(Identifier node, NodeCode context) {
        return context.leaf(node.getValue());
    }

    @Override
    protected NodeCode visitSymbolReference(SymbolReference node, NodeCode context) {
        return context.leaf(node.getName());
    }

    @Override
    protected NodeCode visitLiteral(Literal node, NodeCode context) {
        return context.leaf(ExpressionFormatter.formatExpression(node, Optional.empty()));
    }

    @Override
    protected NodeCode visitComparisonExpression(ComparisonExpression node, NodeCode context) {
        return context.child("", "", toTree(node.getLeft()))
                .child(node.getType().getValue(), "", toTree(node.getRight()));
    }

    @Override
    protected NodeCode visitArithmeticBinary(ArithmeticBinaryExpression node, NodeCode context) {
        return context.child("", "", toTree(node.getLeft()))
                .child(node.getType().getValue(), "", toTree(node.getRight()));
    }

    @Override
    protected NodeCode visitIsNullPredicate(IsNullPredicate node, NodeCode context) {
        return context.child("", "IS NULL", toTree(node.getValue()));
    }

    @Override
    protected NodeCode visitIsNotNullPredicate(IsNotNullPredicate node, NodeCode context) {
        return context.child("", "IS NOT NULL", toTree(node.getValue()));
    }

    @Override
    protected NodeCode visitBetweenPredicate(BetweenPredicate node, NodeCode context) {
        return context.child("", "", toTree(node.getValue()))
                .child("BETWEEN", "", toTree(node.getMin()))
                .child("AND", "", toTree(node.getMax()));
    }

    @Override
    protected NodeCode visitExists(ExistsPredicate node, NodeCode context) {
        return context.child("EXISTS (", ")", toTree(node.getSubquery()));
    }

    @Override
    protected NodeCode visitSubqueryExpression(SubqueryExpression node, NodeCode context) {
        return context.child("(", ")", toTree(node.getQuery()));
    }

    @Override
    protected NodeCode visitLogicalBinaryExpression(LogicalBinaryExpression node, NodeCode context) {
        Iterator<? extends Expression> merged = mergeBinaryExpressions(node);
        context.child("", "", toTree(merged.next()));
        while (merged.hasNext()) {
            context.child(node.getType().name(), "", toTree(merged.next()));
        }
        return context;
    }

    private Iterator<? extends Expression> mergeBinaryExpressions(LogicalBinaryExpression node) {
        Iterator<? extends Expression> leftIt = Iterators.singletonIterator(node.getLeft());
        Iterator<? extends Expression> rightIt = Iterators.singletonIterator(node.getRight());
        if ((node.getLeft() instanceof LogicalBinaryExpression)
            && (((LogicalBinaryExpression) node.getLeft()).getType() == node.getType())) {
            leftIt = mergeBinaryExpressions((LogicalBinaryExpression) (node.getLeft()));
        }
        if ((node.getRight() instanceof LogicalBinaryExpression)
            && (((LogicalBinaryExpression) node.getRight()).getType() == node.getType())) {
            rightIt = mergeBinaryExpressions((LogicalBinaryExpression) (node.getRight()));
        }
        return Iterators.concat(leftIt, rightIt);
    }

    @Override
    protected NodeCode visitSelect(Select node, NodeCode context) {
        List<SelectItem> selectItems = node.getSelectItems();
        int l = selectItems.size();
        for (int i = 0; i < l; i++) {
            SelectItem item = selectItems.get(i);
            context.child("", i == l - 1 ? "" : ",", toTree(item));
        }
        return context;
    }

    @Override
    protected NodeCode visitAllColumns(AllColumns node, NodeCode context) {
        return context.leaf(node.getPrefix()
                .map(qn -> qn.toString() + ".*")
                .orElse("*"));
    }

    @Override
    protected NodeCode visitSingleColumn(SingleColumn node, NodeCode context) {
        return context.child("",
                node.getAlias().map(Identifier::getValue).orElse(""),
                toTree(node.getExpression()));
    }

    @Override
    protected NodeCode visitQuerySpecification(QuerySpecification node, NodeCode context) {
        context.child("SELECT", "", toTree(node.getSelect()));
        if (node.getFrom().isPresent()) {
            context.child("FROM", "", toTree(node.getFrom().get()));
        }
        if (node.getWhere().isPresent()) {
            context.child("WHERE", "", toTree(node.getWhere().get()));
        }
        return context;
    }

    @Override
    protected NodeCode visitAliasedRelation(AliasedRelation node, NodeCode context) {
        return context.child("", node.getAlias().getValue(), toTree(node.getRelation()));
    }

    @Override
    protected NodeCode visitTable(Table node, NodeCode context) {
        return context.leaf(toString(node.getName()));
    }

    @Override
    protected NodeCode visitJoin(Join node, NodeCode context) {
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
