package edu.umich.verdict.relation.condition;

import java.util.List;
import java.util.Set;

import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.datatypes.TableUniqueName;
import edu.umich.verdict.relation.Relation;
import edu.umich.verdict.relation.expr.ColNameExpr;
import edu.umich.verdict.relation.expr.Expr;
import edu.umich.verdict.relation.expr.SubqueryExpr;

public class CompCond extends Cond {

    private Expr left;

    private Expr right;

    private String compOp;

    public CompCond(Expr left, String compOp, Expr right) {
        this.left = left;
        this.right = right;
        this.compOp = compOp;
    }

    public static CompCond from(VerdictContext vc, Expr left, String compOp, Expr right) {
        return new CompCond(left, compOp, right);
    }

    public static CompCond from(VerdictContext vc, Expr left, String compOp, Relation r) {
        return from(vc, left, compOp, SubqueryExpr.from(vc, r));
    }

    public static CompCond from(VerdictContext vc, String left, String compOp, Relation r) {
        return from(vc, Expr.from(vc, left), compOp, SubqueryExpr.from(vc, r));
    }

    public Expr getLeft() {
        return left;
    }

    public Expr getRight() {
        return right;
    }

    public String getOp() {
        return compOp;
    }

    @Override
    public Cond accept(CondModifier v) {
        return v.call(this);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", left, compOp, right);
    }

    @Override
    public Cond searchForJoinCondition(List<String> joinedTableName, String rightTableName) {
        if (compOp.equals("=")) {
            if (left instanceof ColNameExpr && right instanceof ColNameExpr) {
                // when left side is a new table
                String leftTab = ((ColNameExpr) left).getTab();
                String rightTab = ((ColNameExpr) right).getTab();
                if (joinedTableName.contains(leftTab) && rightTableName.equals(rightTab)
                        || joinedTableName.contains(rightTab) && rightTableName.equals(leftTab)) {
                    return this;
                }
            }
        }
        return null;
    }
    
//    private String searchForTableName(VerdictContext vc, ColNameExpr col, List<TableUniqueName> among) {
//        if (col.getTab() != null) {
//            return col.getTab();
//        } else {
//            for (TableUniqueName t : among) {
//                Set<String> cols = vc.getMeta().getColumns(t);
//                if (cols.contains(col.getCol())) {
//                    return t.
//                }
//            }
//        }
//    }

    @Override
    public boolean equals(Object a) {
        if (a instanceof CompCond) {
            if (((CompCond) a).getLeft().equals(left) && ((CompCond) a).getRight().equals(right) && ((CompCond) a).getOp().equals(compOp)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public Cond remove(Cond j) {
        if (equals(j)) {
            return null;
        } else {
            return this;
        }
    }

    @Override
    public Cond withTableSubstituted(String newTab) {
        return new CompCond(left.withTableSubstituted(newTab), compOp, right.withTableSubstituted(newTab));
    }

    @Override
    public String toSql() {
        return String.format("%s %s %s", left.toSql(), compOp, right.toSql());
    }

    @Override
    public boolean equals(Cond o) {
        if (o instanceof CompCond) {
            return getOp().equals(((CompCond) o).getOp())
                && getLeft().equals(((CompCond) o).getLeft())
                && getRight().equals(((CompCond) o).getRight());
        }
        return false;
    }
}
