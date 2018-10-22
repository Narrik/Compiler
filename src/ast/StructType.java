package ast;

public class StructType implements Type {
    public final String structName;

    public StructType(String structName) {
        this.structName = structName;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructType(this);
    }
}