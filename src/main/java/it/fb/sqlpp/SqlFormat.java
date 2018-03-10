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
                    .map(t -> subtree(leaf(","), t))
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
        protected Tree visitExpression(Expression node, Void context) {
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
        protected Tree visitTable(Table node, Void context) {
            return leaf(Joiner.on('.').join(node.getName().getOriginalParts()));
        }

        @Override
        protected Tree visitQuery(Query node, Void context) {
            return process(node.getQueryBody());
        }

        @Override
        protected Tree visitQuerySpecification(QuerySpecification node, Void context) {
            Tree select = subtree(
                    leaf("SELECT"),
                    process(node.getSelect()));
            ImmutableList<Tree> from = node.getFrom()
                    .map(this::process)
                    .map(t -> subtree(leaf("FROM"), t))
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
}
