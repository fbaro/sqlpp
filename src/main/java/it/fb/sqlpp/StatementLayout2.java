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
        Preconditions.checkArgument(ctx.sortItem().isEmpty()); // TODO
        Preconditions.checkArgument(ctx.limit == null); // TODO
        return nc -> nc.singleChild("", "", toTree(ctx.queryTerm()));
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
        };
    }

    @Override
    public Tree visitSelectAll(SqlBaseParser.SelectAllContext ctx) {
        if (ctx.qualifiedName() != null) {
            throw new UnsupportedOperationException("TODO");
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
        Preconditions.checkArgument(ctx.identifier() == null); // TODO
        return toTree(ctx.relationPrimary());
    }

    @Override
    public Tree visitTableName(SqlBaseParser.TableNameContext ctx) {
        return nc -> nc.leaf(ctx.qualifiedName().getText());
    }
}