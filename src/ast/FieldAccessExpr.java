package ast;

public class FieldAccessExpr extends Expr{
    public final Expr structure;
    public final String field;

    public FieldAccessExpr(Expr structure, String field){
        this.structure = structure;
        this.field = field;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitFieldAccessExpr(this);
    }
}
