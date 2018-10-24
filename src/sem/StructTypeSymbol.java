package sem;

import ast.StructTypeDecl;

public class StructTypeSymbol extends Symbol {
    StructTypeDecl st;

    public StructTypeSymbol(StructTypeDecl st){
        super(st.structType.structName);
        this.st = st;
    }
}
