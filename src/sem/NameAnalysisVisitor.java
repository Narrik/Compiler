package sem;

import ast.*;

import java.util.ArrayList;
import java.util.List;

public class NameAnalysisVisitor extends BaseSemanticVisitor<Void> {

    Scope scope;
	NameAnalysisVisitor(Scope scope){
	    this.scope =scope;
    }

    private List<FunDecl> builtInFun = new ArrayList<FunDecl>() {{
        // void print_s(char* s);
        add(new FunDecl(BaseType.VOID,"print_s",new ArrayList<VarDecl>(){{
            add(new VarDecl(new PointerType(BaseType.CHAR), "s"));
        }}, new Block()));
        // void print_i(int i);
        add(new FunDecl(BaseType.VOID,"print_i",new ArrayList<VarDecl>(){{
            add(new VarDecl(BaseType.INT, "i"));
        }}, new Block()));
        // void print_c(char c);
        add(new FunDecl(BaseType.VOID,"print_c",new ArrayList<VarDecl>(){{
            add(new VarDecl(BaseType.CHAR, "c"));
        }}, new Block()));
        // char read_c();
        add(new FunDecl(BaseType.CHAR,"read_c",new ArrayList<VarDecl>(), new Block()));
        // int read_i();
        add(new FunDecl(BaseType.INT,"read_i",new ArrayList<VarDecl>(), new Block()));
        // void mcmalloc(int size);
        add(new FunDecl(BaseType.VOID,"mcmalloc ",new ArrayList<VarDecl>(){{
            add(new VarDecl(BaseType.INT, "size"));
        }}, new Block()));
    }};


    @Override
    public Void visitProgram(Program p) {
        p.funDecls.addAll(0, builtInFun);
        for (StructTypeDecl std : p.structTypeDecls) {
            std.accept(this);
        }
        for (VarDecl vd : p.varDecls) {
            vd.accept(this);
        }
        for (FunDecl fd : p.funDecls) {
            fd.accept(this);
        }
        return null;
    }

    @Override
    public Void visitStructTypeDecl(StructTypeDecl sts) {
        Symbol s = scope.lookupCurrent(sts.structType.structName);
        if(s != null){
            error("Struct "+sts.structType.structName+" has already been declared");
        } else {
            Scope oldScope = scope;
            scope = new Scope(oldScope);
            for (VarDecl vd : sts.params){
                vd.accept(this);
            }
            scope = oldScope;
            scope.put(new StructTypeSymbol(sts));
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl vd) {
        Symbol s = scope.lookupCurrent(vd.varName);
        if(s != null){
            error("Variable "+vd.varName+" has already been declared");
        } else {
            scope.put(new VarSymbol(vd));
        }
        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl p) {
        Symbol s = scope.lookupCurrent(p.name);
        if(s != null){
            error("Function "+p.name+" has already been declared");
        } else {
            scope.put(new FunSymbol(p));
            p.block.params.addAll(0,p.params);
            p.block.accept(this);
        }
        return null;
    }



    @Override
    public Void visitVarExpr(VarExpr v) {
        Symbol s = scope.lookup(v.name);
        if (s == null){
            error("Cannot use a variable before it's been declared");
        } else if (!s.isVar()){
            error("Expected variable, found function");
        } else {
            v.vd = ((VarSymbol) s).vd;
        }
        return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr f) {
        Symbol s = scope.lookup(f.name);
        if (s == null){
            error("Cannot use a function before it's been declared");
        } else if (!s.isFun()){
            error("Expected function, found variable");
        } else {
            f.fd = ((FunSymbol) s).fd;
            for (Expr param : f.params){
                param.accept(this);
            }
        }
        return null;
    }

    @Override
    public Void visitBinOp(BinOp bo) {
        bo.lhs.accept(this);
        bo.rhs.accept(this);
        return null;
    }

    @Override
    public Void visitOp(Op op) {
        return null;
    }

    @Override
    public Void visitArrayAccessExpr(ArrayAccessExpr aa) {
        aa.index.accept(this);
        aa.array.accept(this);
        return null;
    }

    @Override
    public Void visitFieldAccessExpr(FieldAccessExpr fa) {
        fa.structure.accept(this);
        return null;
    }

    @Override
    public Void visitValueAtExpr(ValueAtExpr va) {
        va.expr.accept(this);
        return null;
    }

    @Override
    public Void visitSizeOfExpr(SizeOfExpr so) {
        return null;
    }

    @Override
    public Void visitTypecastExpr(TypecastExpr t) {
        t.expr.accept(this);
        return null;
    }


	@Override
	public Void visitBlock(Block b) {
        Scope oldScope = scope;
        scope = new Scope(oldScope);
        for (VarDecl vd : b.params){
            vd.accept(this);
        }
        for(Stmt stmt : b.stmts){
            stmt.accept(this);
        }
        scope = oldScope;
		return null;
	}

    @Override
    public Void visitExprStmt(ExprStmt e) {
        e.expr.accept(this);
        return null;
    }

    @Override
    public Void visitWhile(While w) {
        w.expr.accept(this);
        w.stmt.accept(this);
        return null;
    }

    @Override
    public Void visitIf(If i) {
        i.expr.accept(this);
        i.stmt1.accept(this);
        if (i.stmt2 != null){
            i.stmt2.accept(this);
        }
        return null;
    }

    @Override
    public Void visitAssign(Assign a) {
        a.expr1.accept(this);
        a.expr2.accept(this);
        return null;
    }

    @Override
    public Void visitReturn(Return r) {
        if (r.expr != null){
            r.expr.accept(this);
        }
        return null;
    }

    @Override
    public Void visitBaseType(BaseType bt) {
        return null;
    }

    @Override
    public Void visitPointerType(PointerType pt) {
        return null;
    }

    @Override
    public Void visitStructType(StructType st) {
        return null;
    }

    @Override
    public Void visitArrayType(ArrayType at) {
        return null;
    }

    @Override
    public Void visitIntLiteral(IntLiteral i) {
        return null;
    }

    @Override
    public Void visitStrLiteral(StrLiteral s) {
        return null;
    }

    @Override
    public Void visitChrLiteral(ChrLiteral c) {
        return null;
    }
}
