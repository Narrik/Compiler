package ast;

public class ArrayType implements Type {
    public final Type type;
    public final int elements;

    public ArrayType(Type type, int elements) {
        this.type = type;
        this.elements = elements;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitArrayType(this);
    }
}
