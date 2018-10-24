package ast;

import java.util.ArrayList;
import java.util.List;

public class Block extends Stmt {
    public final List<VarDecl> params;
    public final List<Stmt> stmts;

    public Block(List<VarDecl> params, List<Stmt> stmts) {
        this.params = params;
        this.stmts = stmts;
    }

    public Block(){
        this.params = new ArrayList<>();
        this.stmts = new ArrayList<>();
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitBlock(this);
    }
}