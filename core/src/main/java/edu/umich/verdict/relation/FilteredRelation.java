package edu.umich.verdict.relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Optional;

import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.datatypes.SampleParam;
import edu.umich.verdict.datatypes.TableUniqueName;
import edu.umich.verdict.exceptions.VerdictException;
import edu.umich.verdict.relation.condition.CompCond;
import edu.umich.verdict.relation.condition.Cond;
import edu.umich.verdict.relation.condition.CondModifier;
import edu.umich.verdict.relation.expr.ColNameExpr;
import edu.umich.verdict.relation.expr.Expr;
import edu.umich.verdict.relation.expr.ExprModifier;
import edu.umich.verdict.relation.expr.SubqueryExpr;
import edu.umich.verdict.util.VerdictLogger;

public class FilteredRelation extends ExactRelation {

    private ExactRelation source;

    private Cond cond;

    public FilteredRelation(VerdictContext vc, ExactRelation source, Cond cond) {
        super(vc);
        this.source = source;
        this.cond = cond;
        this.alias = source.alias;
    }

    public ExactRelation getSource() {
        return source;
    }

    public Cond getFilter() {
        return cond;
    }

    @Override
    protected String getSourceName() {
        return getAlias();
    }	

    /*
     * Approx
     */

    public ApproxRelation approx() throws VerdictException {
        return null;
    }

    protected ApproxRelation approxWith(Map<TableUniqueName, SampleParam> replace) {
        ApproxRelation a = new ApproxFilteredRelation(vc, source.approxWith(replace), approxPossibleSubqueries(cond));
        a.setAlias(getAlias());
        return a;
    }

    private Cond approxPossibleSubqueries(Cond cond) {
        ApproxSubqueryModifier v = new ApproxSubqueryModifier(vc);
        Cond modified = v.visit(cond);
        return modified;
    }

    @Override
    protected List<ApproxRelation> nBestSamples(Expr elem, int n) throws VerdictException {
        List<ApproxRelation> ofSources = source.nBestSamples(elem, n);
        List<ApproxRelation> filtered = new ArrayList<ApproxRelation>();
        for (ApproxRelation a : ofSources) {
            filtered.add(new ApproxFilteredRelation(vc, a, approxPossibleSubqueries(cond)));
        }
        return filtered;
    }

    protected List<SampleGroup> findSample(Expr elem) {
        return source.findSample(elem);
    }

    /*
     * sql
     */

    public String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * ");

        Pair<Optional<Cond>, ExactRelation> filtersAndNextR = allPrecedingFilters(this);
        String csql = (filtersAndNextR.getLeft().isPresent())? filtersAndNextR.getLeft().get().toString() : "";

        sql.append(String.format(" FROM %s", sourceExpr(source)));
        if (csql.length() > 0) { sql.append(" WHERE "); sql.append(csql); }
        return sql.toString();
    }

    //	@Override
    //	public List<SelectElem> getSelectList() {
    //		return source.getSelectList();
    //	}

    @Override
    public ColNameExpr partitionColumn() {
        ColNameExpr col = source.partitionColumn();
        //		col.setTab(getAliasName());
        return col;
    }

    @Override
    public List<ColNameExpr> accumulateSamplingProbColumns() {
        return source.accumulateSamplingProbColumns();
    }

    @Override
    protected String toStringWithIndent(String indent) {
        StringBuilder s = new StringBuilder(1000);
        s.append(indent);
        s.append(String.format("%s(%s) [%s]\n", this.getClass().getSimpleName(), getAlias(), cond.toString()));
        s.append(source.toStringWithIndent(indent + "  "));
        return s.toString();
    }
}


class ApproxSubqueryModifier extends CondModifier {
    
    private VerdictContext vc;
    
    ExprModifier v2;
    
    public ApproxSubqueryModifier(final VerdictContext vc) {
        v2 = new ExprModifier(vc) {
            public Expr call(Expr expr) {
                if (expr instanceof SubqueryExpr) {
                    Relation r = ((SubqueryExpr) expr).getSubquery();
                    if (r instanceof ExactRelation) {
                        ApproxRelation a = null;
                        try {
                            a = ((ExactRelation) r).approx();
                        } catch (VerdictException e) {
                            VerdictLogger.error(this, e.getMessage());
                            VerdictLogger.error(this, "A subquery is not approximated.");
                            return expr;
                        }
                        return new SubqueryExpr(vc, a);
                    }
                }
                return expr;
            }
        };
    }


    @Override
    public Cond call(Cond cond) {
        if (cond instanceof CompCond) {
            CompCond c = (CompCond) cond;
            return CompCond.from(vc, v2.visit(c.getLeft()), c.getOp(), v2.visit(c.getRight()));
        } else {
            return cond;
        }
    }

}
