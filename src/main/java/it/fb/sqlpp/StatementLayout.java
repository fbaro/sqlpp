package it.fb.sqlpp;

import com.facebook.presto.sql.ExpressionFormatter;
import com.facebook.presto.sql.parser.ParsingOptions;
import com.facebook.presto.sql.parser.SqlParser;
import com.facebook.presto.sql.tree.*;
import com.google.common.base.Joiner;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public class StatementLayout extends DefaultTraversalVisitor<TreeVisitor, TreeVisitor> {

    private static final StatementLayout INSTANCE = new StatementLayout();

    public static String format(int lineWidth, int indentWidth, Statement statement) {
        return TreeLayout.format(lineWidth, indentWidth, INSTANCE.toTree(statement));
    }

    public static String format(int lineWidth, int indentWidth, String statement) {
        Statement parsed = new SqlParser().createStatement(statement, new ParsingOptions());
        return format(lineWidth, indentWidth, parsed);
    }

    protected StatementLayout() {
    }

    protected Tree toTree(Node node) {
        return nc -> process(node, nc);
    }

    protected Tree toChildren(List<? extends Node> nodes, String opening, String joiner, String closing) {
        if (nodes.isEmpty()) {
            return nc -> {};
        } else if (nodes.size() == 1) {
            return nc -> nc.singleChild(opening, closing, toTree(nodes.get(0)));
        } else {
            return nc -> {
                for (int i = 0; i < nodes.size(); i++) {
                    nc.child(i == 0 ? opening : "",
                            i == nodes.size() - 1 ? closing : joiner,
                            toTree(nodes.get(i)));
                }
            };
        }
    }

    @Override
    protected TreeVisitor visitIdentifier(Identifier node, TreeVisitor context) {
        return context.leaf(node.getValue());
    }

    @Override
    protected TreeVisitor visitSymbolReference(SymbolReference node, TreeVisitor context) {
        return context.leaf(node.getName());
    }

    @Override
    protected TreeVisitor visitParameter(Parameter node, TreeVisitor context) {
        return context.leaf("?");
    }

    @Override
    protected TreeVisitor visitDereferenceExpression(DereferenceExpression node, TreeVisitor context) {
        //TODO: Che roba e'??
        return context.child("", "." + node.getField().getValue(), toTree(node.getBase()));
    }

    @Override
    protected TreeVisitor visitLiteral(Literal node, TreeVisitor context) {
        return context.leaf(ExpressionFormatter.formatExpression(node, Optional.empty()));
    }

    @Override
    protected TreeVisitor visitComparisonExpression(ComparisonExpression node, TreeVisitor context) {
        return context.child("", "", toTree(node.getLeft()))
                .child(node.getType().getValue(), "", toTree(node.getRight()));
    }

    private static final class ArithmeticParent {
        final ArithmeticBinaryExpression.Type type;
        final boolean leftMost;
        final TreeVisitor context;

        ArithmeticParent(ArithmeticBinaryExpression.Type type, boolean leftMost, TreeVisitor context) {
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
    protected TreeVisitor visitArithmeticBinary(ArithmeticBinaryExpression node, TreeVisitor context) {
        AstVisitor<TreeVisitor, ArithmeticParent> innerVisitor = new AstVisitor<TreeVisitor, ArithmeticParent>() {
            @Override
            protected TreeVisitor visitArithmeticBinary(ArithmeticBinaryExpression innerNode, ArithmeticParent context) {
                if (!isCompatible(innerNode.getType(), context.type)) {
                    return context.context.child(context.leftMost ? "(" : context.type.getValue() + " (", ")", toTree(innerNode));
                }
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), new ArithmeticParent(innerNode.getType(), false, context.context));
                return context.context;
            }

            @Override
            protected TreeVisitor visitExpression(Expression innerNode, ArithmeticParent context) {
                return context.context.child(context.leftMost ? "" : context.type.getValue(), "", toTree(innerNode));
            }

            @Override
            protected TreeVisitor visitNode(Node node, ArithmeticParent context) {
                throw new IllegalStateException("Did not expect node " + node);
            }
        };
        innerVisitor.process(node.getLeft(), new ArithmeticParent(node.getType(), true, context));
        innerVisitor.process(node.getRight(), new ArithmeticParent(node.getType(), false, context));
        return context;
    }

    @Override
    protected TreeVisitor visitNotExpression(NotExpression node, TreeVisitor context) {
        return context.child("NOT (", ")", toTree(node.getValue()));
    }

    @Override
    protected TreeVisitor visitIsNullPredicate(IsNullPredicate node, TreeVisitor context) {
        return context.child("", "IS NULL", toTree(node.getValue()));
    }

    @Override
    protected TreeVisitor visitIsNotNullPredicate(IsNotNullPredicate node, TreeVisitor context) {
        return context.child("", "IS NOT NULL", toTree(node.getValue()));
    }

    @Override
    protected TreeVisitor visitBetweenPredicate(BetweenPredicate node, TreeVisitor context) {
        return context.child("", "", toTree(node.getValue()))
                .child("BETWEEN", "", toTree(node.getMin()))
                .child("AND", "", toTree(node.getMax()));
    }

    @Override
    protected TreeVisitor visitExists(ExistsPredicate node, TreeVisitor context) {
        return context.child("EXISTS", "", toTree(node.getSubquery()));
    }

    @Override
    protected TreeVisitor visitSubqueryExpression(SubqueryExpression node, TreeVisitor context) {
        return context.child("(", " )", toTree(node.getQuery()));
    }

    @Override
    protected TreeVisitor visitLogicalBinaryExpression(LogicalBinaryExpression node, TreeVisitor context) {
        return new AstVisitor<TreeVisitor, TreeVisitor>() {
            private int childCount = 0;

            @Override
            protected TreeVisitor visitLogicalBinaryExpression(LogicalBinaryExpression innerNode, TreeVisitor context) {
                if (innerNode.getType() != node.getType()) {
                    return context.child(++childCount == 1 ? "(" : node.getType().name() + " (", ")", toTree(innerNode));
                }
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), context);
                return context;
            }

            @Override
            protected TreeVisitor visitExpression(Expression innerNode, TreeVisitor context) {
                return context.child(++childCount == 1 ? "" : node.getType().name(), "", toTree(innerNode));
            }

            @Override
            protected TreeVisitor visitIsNotNullPredicate(IsNotNullPredicate node, TreeVisitor context) {
                throw new IllegalStateException("Did not expect node " + node);
            }
        }.process(node, context);
    }

    @Override
    protected TreeVisitor visitInPredicate(InPredicate node, TreeVisitor context) {
        return context.child("", "", toTree(node.getValue()))
                .child("IN", "", toTree(node.getValueList()));
    }

    @Override
    protected TreeVisitor visitInListExpression(InListExpression node, TreeVisitor context) {
        List<Expression> values = node.getValues();
        for (int i = 0, l = values.size(); i < l; i++) {
            Expression e = values.get(i);
            context.child(i == 0 ? "(" : "", i == l - 1 ? " )" : ",", toTree(e));
        }
        return context;
    }

    @Override
    protected TreeVisitor visitLikePredicate(LikePredicate node, TreeVisitor context) {
        context.child("", "", toTree(node.getValue()))
                .child("LIKE", "", toTree(node.getPattern()));
        if (node.getEscape() != null) {
            context.child("ESCAPE", "", toTree(node.getEscape()));
        }
        return context;
    }

    @Override
    protected TreeVisitor visitWhenClause(WhenClause node, TreeVisitor context) {
        return context.child("", "", toTree(node.getOperand()))
                .child("THEN", "", toTree(node.getResult()));
    }

    @Override
    protected TreeVisitor visitSearchedCaseExpression(SearchedCaseExpression node, TreeVisitor context) {
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
    protected TreeVisitor visitFunctionCall(FunctionCall node, TreeVisitor context) {
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
    public TreeVisitor visitWindow(Window node, TreeVisitor context) {
        if (!node.getPartitionBy().isEmpty()) {
            context.child("PARTITION BY", "", toChildren(node.getPartitionBy(), "", ",", ""));
        }
        if (node.getOrderBy().isPresent()) {
            context.child("ORDER BY", "", toTree(node.getOrderBy().get()));
        }
        return context;
    }

    @Override
    protected TreeVisitor visitExtract(Extract node, TreeVisitor context) {
        return context.singleChild("EXTRACT(" + node.getField().name() + " FROM", ")",
                toTree(node.getExpression()));
    }

    @Override
    protected TreeVisitor visitNullIfExpression(NullIfExpression node, TreeVisitor context) {
        return context.child("NULLIF(", ",", toTree(node.getFirst()))
                .child("", ")", toTree(node.getSecond()));
    }

    @Override
    protected TreeVisitor visitGroupBy(GroupBy node, TreeVisitor context) {
        toChildren(node.getGroupingElements(), "", ",", "").accept(context);
        return context;
    }

    @Override
    protected TreeVisitor visitSimpleGroupBy(SimpleGroupBy node, TreeVisitor context) {
        if (node.getColumnExpressions().size() != 1) {
            throw new UnsupportedOperationException("Unsupported SimpleGroupBy with " + node.getColumnExpressions().size() + " expressions");
        }
        return process(node.getColumnExpressions().get(0), context);
    }

    @Override
    protected TreeVisitor visitCube(Cube node, TreeVisitor context) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected TreeVisitor visitRollup(Rollup node, TreeVisitor context) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected TreeVisitor visitGroupingSets(GroupingSets node, TreeVisitor context) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected TreeVisitor visitOrderBy(OrderBy node, TreeVisitor context) {
        toChildren(node.getSortItems(), "", ",", "").accept(context);
        return context;
    }

    @Override
    protected TreeVisitor visitSortItem(SortItem node, TreeVisitor context) {
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
    protected TreeVisitor visitSelect(Select node, TreeVisitor context) {
        List<SelectItem> selectItems = node.getSelectItems();
        int l = selectItems.size();
        for (int i = 0; i < l; i++) {
            SelectItem item = selectItems.get(i);
            context.child("", i == l - 1 ? "" : ",", toTree(item));
        }
        return context;
    }

    @Override
    protected TreeVisitor visitAllColumns(AllColumns node, TreeVisitor context) {
        return context.leaf(node.getPrefix()
                .map(qn -> toString(qn) + ".*")
                .orElse("*"));
    }

    @Override
    protected TreeVisitor visitSingleColumn(SingleColumn node, TreeVisitor context) {
        return context.singleChild("",
                node.getAlias().map(i -> " AS " + i.getValue()).orElse(""),
                toTree(node.getExpression()));
    }

    @Override
    protected TreeVisitor visitAliasedRelation(AliasedRelation node, TreeVisitor context) {
        return context.child("", " AS " + node.getAlias().getValue(), toTree(node.getRelation()));
    }

    @Override
    protected TreeVisitor visitRow(Row node, TreeVisitor context) {
        toChildren(node.getItems(), "(", ",", ")").accept(context);
        return context;
    }

    @Override
    protected TreeVisitor visitValues(Values node, TreeVisitor context) {
        if (node.getRows().size() == 1) {
            return context.singleChild("VALUES", "", toTree(node.getRows().get(0)));
        } else {
            return context.singleChild("VALUES (", ")", toChildren(node.getRows(), "", ",", ""));
        }
    }

    @Override
    protected TreeVisitor visitQuerySpecification(QuerySpecification node, TreeVisitor context) {
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
    protected TreeVisitor visitQuery(Query node, TreeVisitor context) {
        if (!node.getWith().isPresent() && !node.getOrderBy().isPresent()
                && !node.getLimit().isPresent()) {
            return context.singleChild("", "", toTree(node.getQueryBody()));
        }
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    protected TreeVisitor visitInsert(Insert node, TreeVisitor context) {
        if (node.getColumns().isPresent()) {
            context.child("INSERT INTO " + toString(node.getTarget()) + " (", ")",
                    toChildren(node.getColumns().get(), "", ",", ""));
        } else {
            context.leaf("INSERT INTO " + toString(node.getTarget()));
        }
        return context.child("", "", toTree(node.getQuery()));
    }

    @Override
    protected TreeVisitor visitDelete(Delete node, TreeVisitor context) {
        context.child("DELETE FROM", "", toTree(node.getTable()));
        if (node.getWhere().isPresent()) {
            context.child("WHERE", "", toTree(node.getWhere().get()));
        }
        return context;
    }

    @Override
    protected TreeVisitor visitTable(Table node, TreeVisitor context) {
        return context.leaf(toString(node.getName()));
    }

    @Override
    protected TreeVisitor visitTableSubquery(TableSubquery node, TreeVisitor context) {
        return context.singleChild("(", ")", toTree(node.getQuery()));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class JoinParent {
        final Join.Type type;
        final Optional<JoinCriteria> criteria;
        final boolean leftMost;
        final TreeVisitor context;

        JoinParent(Join.Type type, Optional<JoinCriteria> criteria, boolean leftMost, TreeVisitor context) {
            this.type = type;
            this.criteria = criteria;
            this.leftMost = leftMost;
            this.context = context;
        }
    }

    @Override
    protected TreeVisitor visitJoin(Join node, TreeVisitor context) {
        AstVisitor<TreeVisitor, JoinParent> innerVisitor = new AstVisitor<TreeVisitor, JoinParent>() {
            @Override
            protected TreeVisitor visitJoin(Join innerNode, JoinParent context) {
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), new JoinParent(innerNode.getType(), innerNode.getCriteria(), false, context.context));
                return context.context;
            }

            @Override
            protected TreeVisitor visitRelation(Relation innerNode, JoinParent context) {
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
            protected TreeVisitor visitNode(Node innerNode, JoinParent context) {
                throw new IllegalStateException("Did not expect node " + innerNode);
            }
        };
        innerVisitor.process(node.getLeft(), new JoinParent(node.getType(), node.getCriteria(), true, context));
        innerVisitor.process(node.getRight(), new JoinParent(node.getType(), node.getCriteria(), false, context));
        return context;
    }

    @Override
    protected TreeVisitor visitCommit(Commit node, TreeVisitor context) {
        return context.leaf("COMMIT");
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
