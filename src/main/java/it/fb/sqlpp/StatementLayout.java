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

    private Consumer<NodeCode> toChildren(List<? extends Node> nodes, String opening, String joiner, String closing) {
        return nc -> {
            for (int i = 0; i < nodes.size(); i++) {
                nc.child(i == 0 ? opening : "",
                        i == nodes.size() - 1 ? closing : joiner,
                        toTree(nodes.get(i)));
            }
        };
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
                    return context.context.child(context.leftMost ? "(" : context.type.getValue() + "(", ")", toTree(innerNode));
                }
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), new ArithmeticParent(innerNode.getType(), false, context.context));
                return context.context;
            }

            @Override
            protected NodeCode visitExpression(Expression innerNode, ArithmeticParent context) {
                return context.context.child(context.leftMost ? "" : context.type.getValue(), "", toTree(innerNode));
            }

            @Override
            protected NodeCode visitNode(Node node, ArithmeticParent context) {
                throw new IllegalStateException("Did not expect node " + node);
            }
        };
        innerVisitor.process(node.getLeft(), new ArithmeticParent(node.getType(), true, context));
        innerVisitor.process(node.getRight(), new ArithmeticParent(node.getType(), false, context));
        return context;
    }

    @Override
    protected NodeCode visitNotExpression(NotExpression node, NodeCode context) {
        return context.child("NOT (", ")", toTree(node.getValue()));
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
                    return context.child(++childCount == 1 ? "(" : node.getType().name() + "(", ")", toTree(innerNode));
                }
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), context);
                return context;
            }

            @Override
            protected NodeCode visitExpression(Expression innerNode, NodeCode context) {
                return context.child(++childCount == 1 ? "" : node.getType().name(), "", toTree(innerNode));
            }

            @Override
            protected NodeCode visitIsNotNullPredicate(IsNotNullPredicate node, NodeCode context) {
                throw new IllegalStateException("Did not expect node " + node);
            }
        }.process(node, context);
    }

    @Override
    protected NodeCode visitInPredicate(InPredicate node, NodeCode context) {
        return context.child("", "", toTree(node.getValue()))
                .child("IN", "", toTree(node.getValueList()));
    }

    @Override
    protected NodeCode visitInListExpression(InListExpression node, NodeCode context) {
        List<Expression> values = node.getValues();
        for (int i = 0, l = values.size(); i < l; i++) {
            Expression e = values.get(i);
            context.child(i == 0 ? "(" : "", i == l - 1 ? " )" : ",", toTree(e));
        }
        return context;
    }

    @Override
    protected NodeCode visitLikePredicate(LikePredicate node, NodeCode context) {
        context.child("", "", toTree(node.getValue()))
                .child("LIKE", "", toTree(node.getPattern()));
        if (node.getEscape() != null) {
            context.child("ESCAPE", "", toTree(node.getEscape()));
        }
        return context;
    }

    @Override
    protected NodeCode visitWhenClause(WhenClause node, NodeCode context) {
        return context.child("", "", toTree(node.getOperand()))
                .child("THEN", "", toTree(node.getResult()));
    }

    @Override
    protected NodeCode visitSearchedCaseExpression(SearchedCaseExpression node, NodeCode context) {
        List<WhenClause> whenClauses = node.getWhenClauses();
        for (int i = 0; i < whenClauses.size(); i++) {
            WhenClause wc = whenClauses.get(i);
            context.child(i == 0 ? "CASE WHEN" : "WHEN", "", toTree(wc));
        }
        node.getDefaultValue()
            .map(StatementLayout.this::toTree)
            .ifPresent(els -> context.child("ELSE", "", els));
        return context.leaf("END");
    }

    @Override
    protected NodeCode visitFunctionCall(FunctionCall node, NodeCode context) {
        if (node.getFilter().isPresent() || node.getOrderBy().isPresent()) {
            throw new UnsupportedOperationException("TODO");
        }

        String opening = toString(node.getName()) + "(" + (node.isDistinct() ? "DISTINCT" : "");
        List<Expression> arguments = node.getArguments();

        if (!node.getWindow().isPresent()) {
            if (arguments.isEmpty()) {
                context.leaf(opening + ")");
            } else {
                toChildren(arguments, opening, ",", ")").accept(context);
            }
            return context;
        }

        if (arguments.isEmpty()) {
            context.leaf(opening + ")");
        } else {
            context.child(opening, "", toChildren(arguments, "", ",", ")"));
        }
        return context.child("OVER(", ")", toTree(node.getWindow().get()));
    }

    @Override
    public NodeCode visitWindow(Window node, NodeCode context) {
        if (!node.getPartitionBy().isEmpty()) {
            context.child("PARTITION BY", "", toChildren(node.getPartitionBy(), "", ",", ""));
        }
        if (node.getOrderBy().isPresent()) {
            context.child("ORDER BY", "", toTree(node.getOrderBy().get()));
        }
        return context;
    }

    @Override
    protected NodeCode visitOrderBy(OrderBy node, NodeCode context) {
        toChildren(node.getSortItems(), "", ",", "").accept(context);
        return context;
    }

    @Override
    protected NodeCode visitSortItem(SortItem node, NodeCode context) {
        StringBuilder builder = new StringBuilder();
        switch (node.getOrdering()) {
            case ASCENDING:
                break;
            case DESCENDING:
                builder.append(" DESC");
                break;
            default:
                throw new UnsupportedOperationException("unknown ordering: " + node.getOrdering());
        }

        switch (node.getNullOrdering()) {
            case FIRST:
                builder.append(" NULLS FIRST");
                break;
            case LAST:
                builder.append(" NULLS LAST");
                break;
            case UNDEFINED:
                // no op
                break;
            default:
                throw new UnsupportedOperationException("unknown null ordering: " + node.getNullOrdering());
        }
        return context.child("", builder.toString(), toTree(node.getSortKey()));
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
        if (node.getGroupBy().isPresent()) {
            context.child("GROUP BY", "", toTree(node.getGroupBy().get()));
        }
        if (node.getHaving().isPresent()) {
            context.child("HAVING", "", toTree(node.getHaving().get()));
        }
        if (node.getOrderBy().isPresent()) {
            context.child("ORDER BY", "", toTree(node.getOrderBy().get()));
        }
        return context;
    }

    @Override
    protected NodeCode visitQuery(Query node, NodeCode context) {
        if (!node.getWith().isPresent() && !node.getOrderBy().isPresent()
                && !node.getLimit().isPresent()) {
            return context.singleChild("", "", toTree(node.getQueryBody()));
        }
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    protected NodeCode visitAliasedRelation(AliasedRelation node, NodeCode context) {
        return context.child("", " AS " + node.getAlias().getValue(), toTree(node.getRelation()));
    }

    @Override
    protected NodeCode visitTable(Table node, NodeCode context) {
        return context.leaf(toString(node.getName()));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class JoinParent {
        public final Join.Type type;
        public final Optional<JoinCriteria> criteria;
        public final boolean leftMost;
        public final NodeCode context;

        JoinParent(Join.Type type, Optional<JoinCriteria> criteria, boolean leftMost, NodeCode context) {
            this.type = type;
            this.criteria = criteria;
            this.leftMost = leftMost;
            this.context = context;
        }
    }

    @Override
    protected NodeCode visitJoin(Join node, NodeCode context) {
        AstVisitor<NodeCode, JoinParent> innerVisitor = new AstVisitor<NodeCode, JoinParent>() {
            @Override
            protected NodeCode visitJoin(Join innerNode, JoinParent context) {
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), new JoinParent(innerNode.getType(), innerNode.getCriteria(), false, context.context));
                return context.context;
            }

            @Override
            protected NodeCode visitRelation(Relation innerNode, JoinParent context) {
                String preLabel = context.leftMost ? "" : StatementLayout.toString(context.type);
                if (context.leftMost || !context.criteria.isPresent()) {
                    return context.context.child(preLabel, "", toTree(innerNode));
                } else if (context.criteria.get() instanceof JoinOn) {
                    return context.context.child(preLabel, "", inCtx -> {
                        StatementLayout.this.process(innerNode, inCtx);
                        inCtx.child("ON", "", toTree(((JoinOn) context.criteria.get()).getExpression()));
                    });
                } else {
                    throw new UnsupportedOperationException("Unsupported join criteria: " + node.getCriteria());
                }
            }

            @Override
            protected NodeCode visitNode(Node innerNode, JoinParent context) {
                throw new IllegalStateException("Did not expect node " + innerNode);
            }
        };
        innerVisitor.process(node.getLeft(), new JoinParent(node.getType(), node.getCriteria(), true, context));
        innerVisitor.process(node.getRight(), new JoinParent(node.getType(), node.getCriteria(), false, context));
        return context;
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
