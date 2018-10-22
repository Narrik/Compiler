package ast;

import java.util.List;

public class StructTypeDecl implements ASTNode {
    public final StructType structType;
    public final List<VarDecl> params;

    public StructTypeDecl(StructType structType, List<VarDecl> params) {
        this.structType = structType;
        this.params = params;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructTypeDecl(this);
    }

}
