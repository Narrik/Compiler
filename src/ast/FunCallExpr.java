package ast;

import java.util.List;

public class FunCallExpr extends Expr {
    public final String name;
    public final List<Expr> params;
    public FunDecl fd; // to be filled in by the name analyser

    public FunCallExpr(String name, List<Expr> params){
        this.name = name;
        this.params = params;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitFunCallExpr(this);
    }
}