package ast;

public class PointerType implements Type {
    public final Type type;

    public PointerType(Type type) {
        this.type = type;
    }


    public boolean equals(Object other){
       if (other instanceof PointerType){
           Type otherT = ((PointerType) other).type;
           if (otherT.equals(this.type)) {
               return true;
           }
       }
        if (other instanceof ArrayType){
            Type otherT = ((ArrayType) other).type;
            if (otherT.equals(this.type)) {
                return true;
            }
        }
       return false;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitPointerType(this);
    }
}
