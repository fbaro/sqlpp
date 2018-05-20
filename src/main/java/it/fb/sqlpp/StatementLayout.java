package it.fb.sqlpp;

import com.facebook.presto.sql.ExpressionFormatter;
import com.facebook.presto.sql.parser.ParsingOptions;
import com.facebook.presto.sql.parser.SqlParser;
import com.facebook.presto.sql.tree.*;
import com.google.common.base.Joiner;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public class StatementLayout extends DefaultTraversalVisitor<Tree.Visitor, Tree.Visitor> {

    private static final StatementLayout INSTANCE = new StatementLayout();

    public static String format(int lineWidth, int indentWidth, Statement statement) {
        return TreeLayout.format(lineWidth, indentWidth, INSTANCE.toTree(statement));
    }

    public static String format(int lineWidth, int indentWidth, String statement) {
        Statement parsed = new SqlParser().createStatement(statement, new ParsingOptions());
        return format(lineWidth, indentWidth, parsed);
    }

    static Tree toTree(String statement) {
        Statement parsed = new SqlParser().createStatement(statement, new ParsingOptions());
        return INSTANCE.toTree(parsed);
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
    protected Tree.Visitor visitIdentifier(Identifier node, Tree.Visitor context) {
        return context.leaf(node.getValue());
    }

    @Override
    protected Tree.Visitor visitSymbolReference(SymbolReference node, Tree.Visitor context) {
        return context.leaf(node.getName());
    }

    @Override
    protected Tree.Visitor visitParameter(Parameter node, Tree.Visitor context) {
        return context.leaf("?");
    }

    @Override
    protected Tree.Visitor visitDereferenceExpression(DereferenceExpression node, Tree.Visitor context) {
        //TODO: Che roba e'??
        return context.child("", "." + node.getField().getValue(), toTree(node.getBase()));
    }

    @Override
    protected Tree.Visitor visitLiteral(Literal node, Tree.Visitor context) {
        return context.leaf(ExpressionFormatter.formatExpression(node, Optional.empty()));
    }

    @Override
    protected Tree.Visitor visitComparisonExpression(ComparisonExpression node, Tree.Visitor context) {
        return context.child("", "", toTree(node.getLeft()))
                .child(node.getType().getValue(), "", toTree(node.getRight()));
    }

    private static final class ArithmeticParent {
        final ArithmeticBinaryExpression.Type type;
        final boolean leftMost;
        final Tree.Visitor context;

        ArithmeticParent(ArithmeticBinaryExpression.Type type, boolean leftMost, Tree.Visitor context) {
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
    protected Tree.Visitor visitArithmeticBinary(ArithmeticBinaryExpression node, Tree.Visitor context) {
        AstVisitor<Tree.Visitor, ArithmeticParent> innerVisitor = new AstVisitor<Tree.Visitor, ArithmeticParent>() {
            @Override
            protected Tree.Visitor visitArithmeticBinary(ArithmeticBinaryExpression innerNode, ArithmeticParent context) {
                if (!isCompatible(innerNode.getType(), context.type)) {
                    return context.context.child(context.leftMost ? "(" : context.type.getValue() + " (", ")", toTree(innerNode));
                }
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), new ArithmeticParent(innerNode.getType(), false, context.context));
                return context.context;
            }

            @Override
            protected Tree.Visitor visitExpression(Expression innerNode, ArithmeticParent context) {
                return context.context.child(context.leftMost ? "" : context.type.getValue(), "", toTree(innerNode));
            }

            @Override
            protected Tree.Visitor visitNode(Node node, ArithmeticParent context) {
                throw new IllegalStateException("Did not expect node " + node);
            }
        };
        innerVisitor.process(node.getLeft(), new ArithmeticParent(node.getType(), true, context));
        innerVisitor.process(node.getRight(), new ArithmeticParent(node.getType(), false, context));
        return context;
    }

    @Override
    protected Tree.Visitor visitNotExpression(NotExpression node, Tree.Visitor context) {
        return context.child("NOT (", ")", toTree(node.getValue()));
    }

    @Override
    protected Tree.Visitor visitIsNullPredicate(IsNullPredicate node, Tree.Visitor context) {
        return context.child("", "IS NULL", toTree(node.getValue()));
    }

    @Override
    protected Tree.Visitor visitIsNotNullPredicate(IsNotNullPredicate node, Tree.Visitor context) {
        return context.child("", "IS NOT NULL", toTree(node.getValue()));
    }

    @Override
    protected Tree.Visitor visitBetweenPredicate(BetweenPredicate node, Tree.Visitor context) {
        return context.child("", "", toTree(node.getValue()))
                .child("BETWEEN", "", toTree(node.getMin()))
                .child("AND", "", toTree(node.getMax()));
    }

    @Override
    protected Tree.Visitor visitExists(ExistsPredicate node, Tree.Visitor context) {
        return context.child("EXISTS", "", toTree(node.getSubquery()));
    }

    @Override
    protected Tree.Visitor visitSubqueryExpression(SubqueryExpression node, Tree.Visitor context) {
        return context.child("(", " )", toTree(node.getQuery()));
    }

    @Override
    protected Tree.Visitor visitLogicalBinaryExpression(LogicalBinaryExpression node, Tree.Visitor context) {
        return new AstVisitor<Tree.Visitor, Tree.Visitor>() {
            private int childCount = 0;

            @Override
            protected Tree.Visitor visitLogicalBinaryExpression(LogicalBinaryExpression innerNode, Tree.Visitor context) {
                if (innerNode.getType() != node.getType()) {
                    return context.child(++childCount == 1 ? "(" : node.getType().name() + " (", ")", toTree(innerNode));
                }
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), context);
                return context;
            }

            @Override
            protected Tree.Visitor visitExpression(Expression innerNode, Tree.Visitor context) {
                return context.child(++childCount == 1 ? "" : node.getType().name(), "", toTree(innerNode));
            }

            @Override
            protected Tree.Visitor visitIsNotNullPredicate(IsNotNullPredicate node, Tree.Visitor context) {
                throw new IllegalStateException("Did not expect node " + node);
            }
        }.process(node, context);
    }

    @Override
    protected Tree.Visitor visitInPredicate(InPredicate node, Tree.Visitor context) {
        return context.child("", "", toTree(node.getValue()))
                .child("IN", "", toTree(node.getValueList()));
    }

    @Override
    protected Tree.Visitor visitInListExpression(InListExpression node, Tree.Visitor context) {
        List<Expression> values = node.getValues();
        for (int i = 0, l = values.size(); i < l; i++) {
            Expression e = values.get(i);
            context.child(i == 0 ? "(" : "", i == l - 1 ? " )" : ",", toTree(e));
        }
        return context;
    }

    @Override
    protected Tree.Visitor visitLikePredicate(LikePredicate node, Tree.Visitor context) {
        context.child("", "", toTree(node.getValue()))
                .child("LIKE", "", toTree(node.getPattern()));
        if (node.getEscape() != null) {
            context.child("ESCAPE", "", toTree(node.getEscape()));
        }
        return context;
    }

    @Override
    protected Tree.Visitor visitWhenClause(WhenClause node, Tree.Visitor context) {
        return context.child("", "", toTree(node.getOperand()))
                .child("THEN", "", toTree(node.getResult()));
    }

    @Override
    protected Tree.Visitor visitSearchedCaseExpression(SearchedCaseExpression node, Tree.Visitor context) {
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
    protected Tree.Visitor visitFunctionCall(FunctionCall node, Tree.Visitor context) {
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
    public Tree.Visitor visitWindow(Window node, Tree.Visitor context) {
        if (!node.getPartitionBy().isEmpty()) {
            context.child("PARTITION BY", "", toChildren(node.getPartitionBy(), "", ",", ""));
        }
        if (node.getOrderBy().isPresent()) {
            context.child("ORDER BY", "", toTree(node.getOrderBy().get()));
        }
        return context;
    }

    @Override
    protected Tree.Visitor visitExtract(Extract node, Tree.Visitor context) {
        context.singleChild("EXTRACT(" + node.getField().name() + " FROM", ")",
                toTree(node.getExpression()));
        return null;
    }

    @Override
    protected Tree.Visitor visitNullIfExpression(NullIfExpression node, Tree.Visitor context) {
        return context.child("NULLIF(", ",", toTree(node.getFirst()))
                .child("", ")", toTree(node.getSecond()));
    }

    @Override
    protected Tree.Visitor visitGroupBy(GroupBy node, Tree.Visitor context) {
        toChildren(node.getGroupingElements(), "", ",", "").accept(context);
        return context;
    }

    @Override
    protected Tree.Visitor visitSimpleGroupBy(SimpleGroupBy node, Tree.Visitor context) {
        if (node.getColumnExpressions().size() != 1) {
            throw new UnsupportedOperationException("Unsupported SimpleGroupBy with " + node.getColumnExpressions().size() + " expressions");
        }
        return process(node.getColumnExpressions().get(0), context);
    }

    @Override
    protected Tree.Visitor visitCube(Cube node, Tree.Visitor context) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Tree.Visitor visitRollup(Rollup node, Tree.Visitor context) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Tree.Visitor visitGroupingSets(GroupingSets node, Tree.Visitor context) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Tree.Visitor visitOrderBy(OrderBy node, Tree.Visitor context) {
        toChildren(node.getSortItems(), "", ",", "").accept(context);
        return context;
    }

    @Override
    protected Tree.Visitor visitSortItem(SortItem node, Tree.Visitor context) {
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
    protected Tree.Visitor visitSelect(Select node, Tree.Visitor context) {
        List<SelectItem> selectItems = node.getSelectItems();
        int l = selectItems.size();
        for (int i = 0; i < l; i++) {
            SelectItem item = selectItems.get(i);
            context.child("", i == l - 1 ? "" : ",", toTree(item));
        }
        return context;
    }

    @Override
    protected Tree.Visitor visitAllColumns(AllColumns node, Tree.Visitor context) {
        return context.leaf(node.getPrefix()
                .map(qn -> toString(qn) + ".*")
                .orElse("*"));
    }

    @Override
    protected Tree.Visitor visitSingleColumn(SingleColumn node, Tree.Visitor context) {
        context.singleChild("",
                node.getAlias().map(i -> " AS " + i.getValue()).orElse(""),
                toTree(node.getExpression()));
        return null;
    }

    @Override
    protected Tree.Visitor visitAliasedRelation(AliasedRelation node, Tree.Visitor context) {
        return context.child("", " AS " + node.getAlias().getValue(), toTree(node.getRelation()));
    }

    @Override
    protected Tree.Visitor visitRow(Row node, Tree.Visitor context) {
        toChildren(node.getItems(), "(", ",", ")").accept(context);
        return context;
    }

    @Override
    protected Tree.Visitor visitValues(Values node, Tree.Visitor context) {
        if (node.getRows().size() == 1) {
            context.singleChild("VALUES", "", toTree(node.getRows().get(0)));
        } else {
            context.singleChild("VALUES (", ")", toChildren(node.getRows(), "", ",", ""));
        }
        return null;
    }

    @Override
    protected Tree.Visitor visitQuerySpecification(QuerySpecification node, Tree.Visitor context) {
        context.child("SELECT", "", toTree(node.getSelect()));
        node.getFrom().ifPresent(relation -> context.child("FROM", "", toTree(relation)));
        node.getWhere().ifPresent(where -> context.child("WHERE", "", toTree(where)));
        node.getGroupBy().ifPresent(groupBy -> context.child("GROUP BY", "", toTree(groupBy)));
        node.getHaving().ifPresent(having -> context.child("HAVING", "", toTree(having)));
        node.getOrderBy().ifPresent(orderBy -> context.child("ORDER BY", "", toTree(orderBy)));
        return context;
    }

    @Override
    protected Tree.Visitor visitQuery(Query node, Tree.Visitor context) {
        if (!node.getWith().isPresent() && !node.getOrderBy().isPresent()
                && !node.getLimit().isPresent()) {
            context.singleChild("", "", toTree(node.getQueryBody()));
            return null;
        }
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    protected Tree.Visitor visitInsert(Insert node, Tree.Visitor context) {
        if (node.getColumns().isPresent()) {
            context.child("INSERT INTO " + toString(node.getTarget()) + " (", ")",
                    toChildren(node.getColumns().get(), "", ",", ""));
        } else {
            context.leaf("INSERT INTO " + toString(node.getTarget()));
        }
        return context.child("", "", toTree(node.getQuery()));
    }

    @Override
    protected Tree.Visitor visitDelete(Delete node, Tree.Visitor context) {
        context.child("DELETE FROM", "", toTree(node.getTable()));
        if (node.getWhere().isPresent()) {
            context.child("WHERE", "", toTree(node.getWhere().get()));
        }
        return context;
    }

    @Override
    protected Tree.Visitor visitTable(Table node, Tree.Visitor context) {
        return context.leaf(toString(node.getName()));
    }

    @Override
    protected Tree.Visitor visitTableSubquery(TableSubquery node, Tree.Visitor context) {
        context.singleChild("(", ")", toTree(node.getQuery()));
        return null;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class JoinParent {
        final Join.Type type;
        final Optional<JoinCriteria> criteria;
        final boolean leftMost;
        final Tree.Visitor context;

        JoinParent(Join.Type type, Optional<JoinCriteria> criteria, boolean leftMost, Tree.Visitor context) {
            this.type = type;
            this.criteria = criteria;
            this.leftMost = leftMost;
            this.context = context;
        }
    }

    @Override
    protected Tree.Visitor visitJoin(Join node, Tree.Visitor context) {
        AstVisitor<Tree.Visitor, JoinParent> innerVisitor = new AstVisitor<Tree.Visitor, JoinParent>() {
            @Override
            protected Tree.Visitor visitJoin(Join innerNode, JoinParent context) {
                process(innerNode.getLeft(), context);
                process(innerNode.getRight(), new JoinParent(innerNode.getType(), innerNode.getCriteria(), false, context.context));
                return context.context;
            }

            @Override
            protected Tree.Visitor visitRelation(Relation innerNode, JoinParent context) {
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
            protected Tree.Visitor visitNode(Node innerNode, JoinParent context) {
                throw new IllegalStateException("Did not expect node " + innerNode);
            }
        };
        innerVisitor.process(node.getLeft(), new JoinParent(node.getType(), node.getCriteria(), true, context));
        innerVisitor.process(node.getRight(), new JoinParent(node.getType(), node.getCriteria(), false, context));
        return context;
    }

    @Override
    protected Tree.Visitor visitCommit(Commit node, Tree.Visitor context) {
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
