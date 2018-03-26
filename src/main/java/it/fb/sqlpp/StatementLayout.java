package it.fb.sqlpp;

import com.facebook.presto.sql.ExpressionFormatter;
import com.facebook.presto.sql.tree.*;
import com.google.common.base.Joiner;
import it.fb.sqlpp.TreeLayout.NodeCode;

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
    protected NodeCode visitDereferenceExpression(DereferenceExpression node, NodeCode context) {
        //TODO: Che roba e'??
        return context.child("", "." + node.getField().getValue(), toTree(node.getBase()));
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

    private static final class ArithmeticParent {
        public final ArithmeticBinaryExpression.Type type;
        public final boolean leftMost;
        public final NodeCode context;

        ArithmeticParent(ArithmeticBinaryExpression.Type type, boolean leftMost, NodeCode context) {
            this.type = type;
            this.leftMost = leftMost;
            this.context = context;
        }
    }

    private static boolean isCompatible(ArithmeticBinaryExpression.Type type1, ArithmeticBinaryExpression.Type type2) {
        switch (type1) {
            case ADD:
            case SUBTRACT:
                return type2 == ArithmeticBinaryExpression.Type.ADD || type2 == ArithmeticBinaryExpression.Type.SUBTRACT;
            case DIVIDE:
            case MULTIPLY:
                return type2 == ArithmeticBinaryExpression.Type.DIVIDE || type2 == ArithmeticBinaryExpression.Type.MULTIPLY;
            default:
                return false;
        }
    }

    @Override
    protected NodeCode visitArithmeticBinary(ArithmeticBinaryExpression node, NodeCode context) {
        AstVisitor<NodeCode, ArithmeticParent> innerVisitor = new AstVisitor<NodeCode, ArithmeticParent>() {
            @Override
            protected NodeCode visitArithmeticBinary(ArithmeticBinaryExpression innerNode, ArithmeticParent context) {
                if (!isCompatible(innerNode.getType(), context.type)) {
                    return visitNode(innerNode, context);
                }
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), new ArithmeticParent(innerNode.getType(), false, context.context));
                return context.context;
            }

            @Override
            protected NodeCode visitNode(Node innerNode, ArithmeticParent context) {
                return context.context.child(context.leftMost ? "" : context.type.getValue(), "", toTree(innerNode));
            }
        };
        innerVisitor.process(node.getLeft(), new ArithmeticParent(node.getType(), true, context));
        innerVisitor.process(node.getRight(), new ArithmeticParent(node.getType(), false, context));
        return context;
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
        return context.child("(", " )", toTree(node.getQuery()));
    }

    @Override
    protected NodeCode visitLogicalBinaryExpression(LogicalBinaryExpression node, NodeCode context) {
        return new AstVisitor<NodeCode, NodeCode>() {
            private int childCount = 0;

            @Override
            protected NodeCode visitLogicalBinaryExpression(LogicalBinaryExpression innerNode, NodeCode context) {
                if (innerNode.getType() != node.getType()) {
                    return visitNode(innerNode, context);
                }
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), context);
                return context;
            }

            @Override
            protected NodeCode visitNode(Node innerNode, NodeCode context) {
                return context.child(++childCount == 1 ? "" : node.getType().name(), "", toTree(innerNode));
            }
        }.process(node, context);
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
                .map(qn -> toString(qn) + ".*")
                .orElse("*"));
    }

    @Override
    protected NodeCode visitSingleColumn(SingleColumn node, NodeCode context) {
        return context.singleChild("",
                node.getAlias().map(i -> " AS " + i.getValue()).orElse(""),
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
        return context.child("", " AS " + node.getAlias().getValue(), toTree(node.getRelation()));
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

    private static String toString(QualifiedName qn) {
        return Joiner.on('.').join(qn.getOriginalParts());
    }

    private static String toString(Join.Type joinType) {
        switch (joinType) {
            case CROSS:
            case INNER:
            case LEFT:
            case RIGHT:
                return joinType.name() + " JOIN";
            case IMPLICIT:
                return ",";
            default:
                throw new UnsupportedOperationException("TODO: Other join types " + joinType);
        }
    }
}
