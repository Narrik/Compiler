package ast;

public class StructType implements Type {
    public final String structName;

    public StructType(String structName) {
        this.structName = structName;
    }

    public boolean equals(Object other){
        if (other instanceof StructType){
            if (((StructType) other).structName.equals(this.structName)) {
                return true;
            }
        }
        return false;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructType(this);
    }
}