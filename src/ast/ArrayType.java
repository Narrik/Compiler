package ast;

public class ArrayType implements Type {
    public final Type type;
    public final int elements;

    public ArrayType(Type type, int elements) {
        this.type = type;
        this.elements = elements;
    }

    public boolean equals(Object other){
        if (other instanceof ArrayType){
            Type otherT = ((ArrayType) other).type;
            if (otherT.equals(this.type) && ((ArrayType) other).elements == this.elements) {
                return true;
            }
        }
        if (other instanceof PointerType){
            Type otherT = ((PointerType) other).type;
            if (otherT.equals(this.type)) {
                return true;
            }
        }
        return false;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitArrayType(this);
    }
}
