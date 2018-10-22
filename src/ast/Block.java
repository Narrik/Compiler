package ast;

import java.util.List;

public class Block extends Stmt {
    public final List<VarDecl> params;
    public final List<Stmt> stmts;

    public Block(List<VarDecl> params, List<Stmt> stmts) {
        this.params = params;
        this.stmts = stmts;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitBlock(this);
    }
}