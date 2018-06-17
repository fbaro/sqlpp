package it.fb.sqlpp;

import com.facebook.presto.sql.parser.*;
import com.google.common.base.Preconditions;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public class StatementLayout2 extends SqlBaseBaseVisitor<Tree> {

    private static final StatementLayout2 INSTANCE = new StatementLayout2();

    public static String format(int lineWidth, int indentWidth, String statement) {
        SqlBaseParser.SingleStatementContext parsed = invokeParser(statement);
        return format(lineWidth, indentWidth, parsed);
    }

    static Tree toTree(String statement) {
        SqlBaseParser.SingleStatementContext parsed = invokeParser(statement);
        return INSTANCE.toTree(parsed);
    }

    private static SqlBaseParser.SingleStatementContext invokeParser(String sql) {
        try {
            SqlBaseLexer lexer = new SqlBaseLexer(new CaseInsensitiveStream(new ANTLRInputStream(sql)));
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            SqlBaseParser parser = new SqlBaseParser(tokenStream);

//            lexer.removeErrorListeners(); // TODO
//            lexer.addErrorListener(ERROR_LISTENER);

//            parser.removeErrorListeners();
//            parser.addErrorListener(ERROR_LISTENER);

            SqlBaseParser.SingleStatementContext tree;
            try {
                // first, try parsing with potentially faster SLL mode
                parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
                return parser.singleStatement();
            } catch (ParseCancellationException ex) {
                // if we fail, parse with LL mode
                tokenStream.reset(); // rewind input stream
                parser.reset();

                return parser.singleStatement();
            }
        } catch (StackOverflowError e) {
            throw new ParsingException("Statement is too large (stack overflow while parsing)");
        }
    }

    private static String format(int lineWidth, int indentWidth, SqlBaseParser.SingleStatementContext statement) {
        return TreeLayout.format(lineWidth, indentWidth, INSTANCE.toTree(statement));
    }

    protected StatementLayout2() {
    }

    protected Tree toTree(ParserRuleContext node) {
        return node.accept(StatementLayout2.this);
    }

    protected Tree toChildren(List<? extends ParserRuleContext> nodes, String opening, String joiner, String closing) {
        if (nodes.isEmpty()) {
            return nc -> {
            };
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

    protected Tree toChildren2(List<? extends ParserRuleContext> nodes, String joiner) {
        if (nodes.isEmpty()) {
            return nc -> {
            };
        } else if (nodes.size() == 1) {
            return nc -> nc.singleChild("", "", toTree(nodes.get(0)));
        } else {
            return nc -> {
                for (int i = 0; i < nodes.size(); i++) {
                    nc.child(i == 0 ? "" : joiner,
                            "",
                            toTree(nodes.get(i)));
                }
            };
        }
    }

    @Override
    protected Tree defaultResult() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Tree visitSingleStatement(SqlBaseParser.SingleStatementContext ctx) {
        return toTree(ctx.statement());
    }

    @Override
    public Tree visitStatementDefault(SqlBaseParser.StatementDefaultContext ctx) {
        return toTree(ctx.query());
    }

    @Override
    public Tree visitQuery(SqlBaseParser.QueryContext ctx) {
        if (ctx.with() != null) {
            throw new UnsupportedOperationException("TODO");
        }
        return toTree(ctx.queryNoWith());
    }

    @Override
    public Tree visitQueryNoWith(SqlBaseParser.QueryNoWithContext ctx) {
        if (ctx.sortItem().isEmpty() && ctx.limit == null) {
            return toTree(ctx.queryTerm());
        }
        return nc -> {
            toTree(ctx.queryTerm()).accept(nc);
            if (!ctx.sortItem().isEmpty()) {
                nc.child("ORDER BY", "", toChildren(ctx.sortItem(), "", ",", ""));
            }
            if (ctx.limit != null) {
                nc.child("LIMIT", "", nc2 -> nc2.leaf(ctx.limit.getText()));
            }
        };
    }

    @Override
    public Tree visitQueryTermDefault(SqlBaseParser.QueryTermDefaultContext ctx) {
        return toTree(ctx.queryPrimary());
    }

    @Override
    public Tree visitQueryPrimaryDefault(SqlBaseParser.QueryPrimaryDefaultContext ctx) {
        return toTree(ctx.querySpecification());
    }

    @Override
    public Tree visitQuerySpecification(SqlBaseParser.QuerySpecificationContext ctx) {
        return nc -> {
            nc.child("SELECT", "", toChildren(ctx.selectItem(), "", ",", ""));
            if (!ctx.relation().isEmpty()) {
                nc.child("FROM", "", toChildren2(ctx.relation(), ","));
            }
            if (ctx.where != null) {
                nc.child("WHERE", "", toTree(ctx.where));
            }
            if (ctx.groupBy() != null) {
                nc.child("GROUP BY", "", toTree(ctx.groupBy()));
            }
            if (ctx.having != null) {
                nc.child("HAVING", "", toTree(ctx.having));
            }
        };
    }

    @Override
    public Tree visitSelectAll(SqlBaseParser.SelectAllContext ctx) {
        if (ctx.qualifiedName() != null) {
            return nc -> nc.leaf(ctx.qualifiedName().getText() + ".*");
        }
        return nc -> nc.leaf("*");
    }

    @Override
    public Tree visitSelectSingle(SqlBaseParser.SelectSingleContext ctx) {
        return nc -> nc.singleChild("",
                Optional.ofNullable(ctx.identifier())
                        .map(v -> " AS " + v.getText())
                        .orElse(""),
                toTree(ctx.expression()));
    }

    @Override
    public Tree visitRelationDefault(SqlBaseParser.RelationDefaultContext ctx) {
        return toTree(ctx.sampledRelation());
    }

    @Override
    public Tree visitSampledRelation(SqlBaseParser.SampledRelationContext ctx) {
        Preconditions.checkArgument(ctx.sampleType() == null); // TODO
        return toTree(ctx.aliasedRelation());
    }

    @Override
    public Tree visitAliasedRelation(SqlBaseParser.AliasedRelationContext ctx) {
        if (ctx.identifier() == null) {
            return toTree(ctx.relationPrimary());
        } else {
            return nc -> nc.singleChild("", " AS " + ctx.identifier().getText(), toTree(ctx.relationPrimary()));
        }
    }

    @Override
    public Tree visitTableName(SqlBaseParser.TableNameContext ctx) {
        return nc -> nc.leaf(ctx.qualifiedName().getText());
    }

    @Override
    public Tree visitPredicated(SqlBaseParser.PredicatedContext ctx) {
        if (ctx.predicate() == null) {
            return toTree(ctx.valueExpression);
        } else {
            return toTree(ctx.predicate()); // The valueExpression is retrieved from within the predicate
        }
    }

    @Override
    public Tree visitComparison(SqlBaseParser.ComparisonContext ctx) {
        return nc -> nc.child("", "", toTree(ctx.value))
                .child(ctx.comparisonOperator().getText(), "", toTree(ctx.right));
    }

    @Override
    public Tree visitValueExpressionDefault(SqlBaseParser.ValueExpressionDefaultContext ctx) {
        return toTree(ctx.primaryExpression());
    }

    @Override
    public Tree visitColumnReference(SqlBaseParser.ColumnReferenceContext ctx) {
        return toTree(ctx.identifier());
    }

    @Override
    public Tree visitUnquotedIdentifier(SqlBaseParser.UnquotedIdentifierContext ctx) {
        return nc -> nc.leaf(ctx.getText());
    }

    @Override
    public Tree visitQuotedIdentifier(SqlBaseParser.QuotedIdentifierContext ctx) {
        return nc -> nc.leaf(ctx.getText());
    }

    @Override
    public Tree visitBackQuotedIdentifier(SqlBaseParser.BackQuotedIdentifierContext ctx) {
        return nc -> nc.leaf(ctx.getText());
    }

    @Override
    public Tree visitDigitIdentifier(SqlBaseParser.DigitIdentifierContext ctx) {
        return nc -> nc.leaf(ctx.getText());
    }

    @Override
    public Tree visitGroupBy(SqlBaseParser.GroupByContext ctx) {
        Preconditions.checkArgument(ctx.setQuantifier() == null);
        return toChildren(ctx.groupingElement(), "", ",", "");
    }

    @Override
    public Tree visitSingleGroupingSet(SqlBaseParser.SingleGroupingSetContext ctx) {
        return toTree(ctx.groupingExpressions());
    }

    @Override
    public Tree visitGroupingExpressions(SqlBaseParser.GroupingExpressionsContext ctx) {
        return toChildren(ctx.expression(), "", ",", "");
    }

    @Override
    public Tree visitExpression(SqlBaseParser.ExpressionContext ctx) {
        return toTree(ctx.booleanExpression());
    }

    @Override
    public Tree visitNumericLiteral(SqlBaseParser.NumericLiteralContext ctx) {
        return nc -> nc.leaf(ctx.getText());
    }

    @Override
    public Tree visitSortItem(SqlBaseParser.SortItemContext ctx) {
        String pl = ctx.ordering == null ? "" : " " + ctx.ordering.getText();
        String postLabel = ctx.nullOrdering == null ? pl : pl + " NULLS " + ctx.nullOrdering.getText();
        return nc -> nc.singleChild("", postLabel, toTree(ctx.expression()));
    }

    @Override
    public Tree visitJoinRelation(SqlBaseParser.JoinRelationContext ctx) {
        if (ctx.CROSS() != null) {
            throw new UnsupportedOperationException("TODO");
        } else if (ctx.NATURAL() != null) {
            throw new UnsupportedOperationException("TODO");
        } else {
            return nc -> {
                toTree(ctx.left).appendTo(nc);
                nc.child(ctx.joinType().getText() + " JOIN", "", nc2 -> {
                    toTree(ctx.rightRelation).accept(nc2);
                    nc2.child("", "", toTree(ctx.joinCriteria()));
                });
            };
        }
    }

    @Override
    public Tree visitJoinCriteria(SqlBaseParser.JoinCriteriaContext ctx) {
        if (ctx.ON() != null) {
            return nc -> nc.singleChild("ON", "", toTree(ctx.booleanExpression()));
        } else {
            return nc -> nc.singleChild("USING", "", toChildren(ctx.identifier(), "(", ",", ")"));
        }
    }

    @Override
    public Tree visitLogicalBinary(SqlBaseParser.LogicalBinaryContext ctx) {
        return nc -> {
            if (ctx.left instanceof SqlBaseParser.LogicalBinaryContext) {
                toTree(ctx.left).appendTo(nc);
            } else {
                nc.child("", "", toTree(ctx.left));
            }
            if (ctx.right instanceof SqlBaseParser.LogicalBinaryContext) {
                toTree(ctx.right).appendTo(ctx.operator.getText(), nc);
            } else {
                nc.child(ctx.operator.getText(), "", toTree(ctx.right));
            }
        };
    }

    @Override
    public Tree visitBooleanDefault(SqlBaseParser.BooleanDefaultContext ctx) {
        return toTree(ctx.predicated());
    }

    @Override
    public Tree visitArithmeticBinary(SqlBaseParser.ArithmeticBinaryContext ctx) {
        return nc -> {
            toTree(ctx.left).appendTo(nc);
            nc.child(ctx.operator.getText(), "", toTree(ctx.right));
        };
    }

    @Override
    public Tree visitDereference(SqlBaseParser.DereferenceContext ctx) {
        return nc -> nc.singleChild("", "." + ctx.identifier().getText(), toTree(ctx.primaryExpression()));
    }

    @Override
    public Tree visitStringLiteral(SqlBaseParser.StringLiteralContext ctx) {
        return nc -> nc.leaf(ctx.getText());
    }

    @Override
    public Tree visitLike(SqlBaseParser.LikeContext ctx) {
        return nc -> {
            nc.child("", "", toTree(ctx.value));
            nc.child("LIKE", "", toTree(ctx.pattern));
            if (ctx.escape != null) {
                nc.child("ESCAPE", "", toTree(ctx.escape));
            }
        };
    }

    @Override
    public Tree visitInList(SqlBaseParser.InListContext ctx) {
        return nc -> {
            nc.child("", "", toTree(ctx.value));
            nc.child(ctx.NOT() == null ? "IN" : "NOT IN", "",
                    toChildren(ctx.expression(), "(", ",", " )"));
        };
    }

    @Override
    public Tree visitSearchedCase(SqlBaseParser.SearchedCaseContext ctx) {
        return nc -> {
            List<SqlBaseParser.WhenClauseContext> whenClauses = ctx.whenClause();
            for (int i = 0; i < whenClauses.size(); i++) {
                nc.child(i == 0 ? "CASE WHEN" : "WHEN", "", toTree(whenClauses.get(i)));
            }
            if (ctx.elseExpression != null) {
                nc.child("ELSE", "", toTree(ctx.elseExpression));
            }
            nc.leaf("END");
        };
    }

    @Override
    public Tree visitWhenClause(SqlBaseParser.WhenClauseContext ctx) {
        return nc -> nc.child("", "", toTree(ctx.condition))
                .child("THEN", "", toTree(ctx.result));
    }

    @Override
    public Tree visitFunctionCall(SqlBaseParser.FunctionCallContext ctx) {
        if (!ctx.sortItem().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        }
        String opening = ctx.qualifiedName().getText() + "(";
        if (ctx.over() == null) {
            if (ctx.ASTERISK() != null) {
                return nc -> nc.leaf(opening + "*)");
            } else if (ctx.expression().isEmpty()) {
                return nc -> nc.leaf(opening + ")");
            } else {
                return nc -> toChildren(ctx.expression(), opening, ",", ")")
                        .appendTo(nc);
            }
        } else {
            return nc -> {
                if (ctx.ASTERISK() != null) {
                    nc.leaf(opening + "*)");
                } else if (ctx.expression().isEmpty()) {
                    nc.leaf(opening + ")");
                } else {
                    nc.child(opening, ")", toChildren(ctx.expression(), "", ",", ""));
                }
                nc.child("OVER(", ")", toTree(ctx.over()));
            };
        }
    }

    @Override
    public Tree visitOver(SqlBaseParser.OverContext ctx) {
        return nc -> {
            nc.child("PARTITION BY", "", toChildren(ctx.expression(), "", ",", ""));
            if (!ctx.sortItem().isEmpty()) {
                nc.child("ORDER BY", "", toChildren(ctx.sortItem(), "", ",", ""));
            }
            if (ctx.windowFrame() != null) {
                toTree(ctx.windowFrame()).appendTo(nc);
            }
        };
    }

    @Override
    public Tree visitWindowFrame(SqlBaseParser.WindowFrameContext ctx) {
        if (ctx.BETWEEN() == null) {
            return nc -> nc.singleChild(ctx.frameType.getText(), "", toTree(ctx.frameBound(0)));
        } else {
            return nc -> nc.singleChild(ctx.frameType.getText(), "", toChildren(ctx.frameBound(),
                    "BETWEEN", "AND", ""));
        }
    }

    @Override
    public Tree visitParenthesizedExpression(SqlBaseParser.ParenthesizedExpressionContext ctx) {
        return nc -> nc.singleChild("(", ")", toTree(ctx.expression()));
    }

    @Override
    public Tree visitSubqueryExpression(SqlBaseParser.SubqueryExpressionContext ctx) {
        return nc ->  nc.singleChild("(", " )", toTree(ctx.query()));
    }

    @Override
    public Tree visitInsertInto(SqlBaseParser.InsertIntoContext ctx) {
        return nc -> {
            if (ctx.columnAliases() == null) {
                nc.leaf("INSERT INTO " + ctx.qualifiedName().getText());
            } else {
                nc.child("INSERT INTO " + ctx.qualifiedName().getText() + " (", ")",
                        toChildren(ctx.columnAliases().identifier(), "", ",", ""));
            }
            nc.child("", "", toTree(ctx.query()));
        };
    }

    @Override
    public Tree visitInlineTable(SqlBaseParser.InlineTableContext ctx) {
        return nc -> nc.singleChild("VALUES", "", toChildren(ctx.expression(), "", ",", ""));
    }

    @Override
    public Tree visitRowConstructor(SqlBaseParser.RowConstructorContext ctx) {
        return toChildren(ctx.expression(), ctx.ROW() == null ? "(" : "ROW (", ",", ")");
    }

    @Override
    public Tree visitDelete(SqlBaseParser.DeleteContext ctx) {
        return nc -> {
            nc.leaf("DELETE FROM " + ctx.qualifiedName().getText());
            if (ctx.booleanExpression() != null) {
                nc.child("WHERE", "", toTree(ctx.booleanExpression()));
            }
        };
    }

    @Override
    public Tree visitBetween(SqlBaseParser.BetweenContext ctx) {
        return nc -> {
            nc.child("", "", toTree(ctx.value));
            nc.child(ctx.NOT() == null ? "BETWEEN" : "NOT BETWEEN", "", toTree(ctx.valueExpression(0)));
            nc.child("AND","", toTree(ctx.valueExpression(1)));
        };
    }

    @Override
    public Tree visitInSubquery(SqlBaseParser.InSubqueryContext ctx) {
        return nc -> {
            nc.child("", "", toTree(ctx.value));
            nc.child(ctx.NOT() == null ? "IN (" : "NOT IN (", " )",
                    toTree(ctx.query()));
        };
    }

    @Override
    public Tree visitParameter(SqlBaseParser.ParameterContext ctx) {
        return nc -> nc.leaf("?");
    }

    @Override
    public Tree visitExists(SqlBaseParser.ExistsContext ctx) {
        return nc -> nc.singleChild("EXISTS (", " )", toTree(ctx.query()));
    }
}
