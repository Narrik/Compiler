package sem;

import ast.VarDecl;

public class VarSymbol extends Symbol {
    VarDecl vd;

    public VarSymbol(VarDecl vd){
        super(vd.varName);
        this.vd = vd;
    }

}
