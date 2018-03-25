package it.fb.sqlpp;

import com.facebook.presto.sql.ExpressionFormatter;
import com.facebook.presto.sql.parser.ParsingOptions;
import com.facebook.presto.sql.parser.SqlParser;
import com.facebook.presto.sql.tree.*;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.fb.sqlpp.Tree.leaf;
import static it.fb.sqlpp.Tree.solidSubtree;
import static it.fb.sqlpp.Tree.subtree;

public final class SqlFormat {

    private SqlFormat() {
    }

    public static String format(String sql, int lineWidth, int indentSize) {
        Tree sqlTree = toTree(sql);
        return TreeFormat.format(sqlTree, lineWidth, indentSize);
    }

    static Tree toTree(String sql) {
        Statement parsedSql = new SqlParser().createStatement(sql, new ParsingOptions(ParsingOptions.DecimalLiteralTreatment.AS_DECIMAL));
        return parsedSql.accept(ToTreeVisitor.INSTANCE, null);
    }

    private static final class ToTreeVisitor extends DefaultTraversalVisitor<Tree, Void> {

        static final ToTreeVisitor INSTANCE = new ToTreeVisitor();

        @Override
        protected Tree visitSelect(Select node, Void context) {
            Tree firstChild = process(node.getChildren().get(0));
            List<Tree> otherChildren = node.getChildren()
                    .stream()
                    .skip(1)
                    .map(this::process)
                    .map(t -> solidSubtree(leaf(","), t))
                    .collect(Collectors.toList());
            return otherChildren.isEmpty() ? firstChild :
                    subtree(Iterables.concat(ImmutableList.of(firstChild), otherChildren));
        }

        @Override
        protected Tree visitAllColumns(AllColumns node, Void context) {
            return leaf(node.getPrefix()
                    .map(QualifiedName::toString)
                    .map(s -> s + ".*")
                    .orElse("*"));
        }

        @Override
        protected Tree visitSingleColumn(SingleColumn node, Void context) {
            return process(node.getExpression());
        }

        @Override
        protected Tree visitSymbolReference(SymbolReference node, Void context) {
            return leaf(node.getName());
        }

        @Override
        protected Tree visitLiteral(Literal node, Void context) {
            return leaf(ExpressionFormatter.formatExpression(node, Optional.empty()));
        }

        @Override
        protected Tree visitComparisonExpression(ComparisonExpression node, Void context) {
            return subtree(
                    process(node.getLeft()),
                    subtree(
                            leaf(node.getType().getValue()),
                            process(node.getRight())));
        }

        @Override
        protected Tree visitArithmeticBinary(ArithmeticBinaryExpression node, Void context) {
            return subtree(
                    process(node.getLeft()),
                    subtree(
                            leaf(node.getType().getValue()),
                            process(node.getRight())));
        }

        @Override
        protected Tree visitIsNullPredicate(IsNullPredicate node, Void context) {
            return subtree(process(node.getValue()), leaf("IS NULL"));
        }

        @Override
        protected Tree visitIsNotNullPredicate(IsNotNullPredicate node, Void context) {
            return subtree(process(node.getValue()), leaf("IS NOT NULL"));
        }

        @Override
        protected Tree visitBetweenPredicate(BetweenPredicate node, Void context) {
            return subtree(process(node.getValue()), leaf("BETWEEN"), process(node.getMin()), leaf("AND"), process(node.getMax()));
        }

        @Override
        protected Tree visitExists(ExistsPredicate node, Void context) {
            return subtree(leaf("EXISTS"), leaf("("), process(node.getSubquery()), leaf(")"));
        }

        @Override
        protected Tree visitSubqueryExpression(SubqueryExpression node, Void context) {
            return subtree(leaf("("), process(node.getQuery()), leaf(")"));
        }

        @Override
        protected Tree visitLogicalBinaryExpression(LogicalBinaryExpression node, Void context) {
            return subtree(process(node.getLeft()), subtree(leaf(node.getType().name()), process(node.getRight())));
        }

        @Override
        protected Tree visitIdentifier(Identifier node, Void context) {
            return leaf(node.getValue());
        }

        @Override
        protected Tree visitAliasedRelation(AliasedRelation node, Void context) {
            return subtree(process(node.getRelation()), process(node.getAlias()));
        }

        @Override
        protected Tree visitTable(Table node, Void context) {
            return leaf(Joiner.on('.').join(node.getName().getOriginalParts()));
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

        @Override
        protected Tree visitJoin(Join node, Void context) {
            if (!node.getCriteria().isPresent()) {
                return subtree(
                        process(node.getLeft()),
                        solidSubtree(
                                leaf(toString(node.getType())),
                                process(node.getRight())
                        ));
            } else if (node.getCriteria().get() instanceof JoinOn) {
                return subtree(
                        process(node.getLeft()),
                        subtree(
                                solidSubtree(
                                        leaf(toString(node.getType())),
                                        process(node.getRight())
                                ),
                                solidSubtree(
                                        leaf("ON"),
                                        process(((JoinOn) node.getCriteria().get()).getExpression())
                                )
                        ));
            } else {
                throw new UnsupportedOperationException("TODO join" + node.getCriteria().get());
            }
        }

        @Override
        protected Tree visitQuery(Query node, Void context) {
            // TODO
            return process(node.getQueryBody());
        }

        @Override
        protected Tree visitQuerySpecification(QuerySpecification node, Void context) {
            Tree select = addOlderSibling("SELECT", process(node.getSelect()));
            ImmutableList<Tree> from = node.getFrom()
                    .map(this::process)
                    .map(t -> addOlderSibling("FROM", t))
                    .map(ImmutableList::of)
                    .orElse(ImmutableList.of());
            ImmutableList<Tree> where = node.getWhere()
                    .map(this::process)
                    .map(t -> subtree(leaf("WHERE"), t))
                    .map(ImmutableList::of)
                    .orElse(ImmutableList.of());
            ImmutableList<Tree> groupBy = node.getGroupBy()
                    .map(this::process)
                    .map(t -> subtree(leaf("GROUP BY"), t))
                    .map(ImmutableList::of)
                    .orElse(ImmutableList.of());
            ImmutableList<Tree> having = node.getHaving()
                    .map(this::process)
                    .map(t -> subtree(leaf("HAVING"), t))
                    .map(ImmutableList::of)
                    .orElse(ImmutableList.of());
            ImmutableList<Tree> orderBy = node.getOrderBy()
                    .map(this::process)
                    .map(t -> subtree(leaf("ORDER BY"), t))
                    .map(ImmutableList::of)
                    .orElse(ImmutableList.of());
            return Tree.<Tree>subtree(Iterables.concat(
                    ImmutableList.of(select),
                    from, where, groupBy, having, orderBy));
        }
    }

    private static Tree addOlderSibling(String text, Tree subtree) {
        Tree newNode = leaf(text);
        if (subtree.isLeaf()) {
            return subtree(newNode, subtree);
        } else {
            return subtree.add(0, newNode);
        }
    }
}
