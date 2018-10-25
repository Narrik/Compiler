package sem;

import ast.*;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckVisitor extends BaseSemanticVisitor<Type> {

    List<StructTypeDecl> structTypeDeclList = new ArrayList<>();

	@Override
	public Type visitBaseType(BaseType bt) {
		return bt;
	}

    @Override
    public Type visitPointerType(PointerType pt) {
	    return pt;
    }

    @Override
    public Type visitStructType(StructType sy) {
        return sy;
    }

    @Override
    public Type visitArrayType(ArrayType at) {
        return at;
    }

    @Override
    public Type visitIntLiteral(IntLiteral i) {
	    i.type = BaseType.INT;
        return i.type;
    }

    @Override
    public Type visitStrLiteral(StrLiteral s) {
	    s.type = new ArrayType(BaseType.CHAR,s.value.length()+1);
        return s.type;
    }

    @Override
    public Type visitChrLiteral(ChrLiteral c) {
	    c.type = BaseType.CHAR;
        return c.type;
    }

    @Override
    public Type visitVarExpr(VarExpr v) {
	    v.type = v.vd.type;
	    return v.type;
    }

    @Override
    public Type visitFunCallExpr(FunCallExpr f) {
	    List<VarDecl> params = f.fd.params;
	    List<Expr> args = f.params;
	    // Check for size
	    if (params.size() != args.size()){
	        error("Function called with a different amount of arguments than it was declared");
	        return f.type;
        }
        // Check for types being the same
        for (int i =0; i<params.size(); i++){
            VarDecl param = params.get(i);
            Expr arg = args.get(i);
            Type paramT = param.type;
            Type argT = arg.accept(this);
            if (!argT.equals(paramT)) {
                error("Function arguments don't match in types with their declared types " + paramT + " expected, " + argT + " found.");
            }
        }
        f.type = f.fd.type;
        return f.type;
    }

    @Override
    public Type visitBinOp(BinOp bo) {
        Type lhsT = bo.lhs.accept(this);
        Type rhsT = bo.rhs.accept(this);
        if (bo.op == Op.ADD || bo.op == Op.SUB || bo.op == Op.MUL ||
            bo.op == Op.DIV || bo.op == Op.MOD || bo.op == Op.OR ||
            bo.op == Op.AND || bo.op == Op.GT || bo.op == Op.LT ||
            bo.op == Op.GE || bo.op == Op.LE){
            // Check if both sides are of type int
            if (lhsT == BaseType.INT && rhsT == BaseType.INT){
                bo.type = BaseType.INT;
                return bo.type;
            } else {
                error("Cannot perform "+bo.op+" between type "+lhsT+" and type "+rhsT);
                return BaseType.INT;
            }
            // bo.op == NE | EQ
        } else {
            // Check if both sides are not of type StructType, ArrayType or void
            if (lhsT instanceof StructType || lhsT instanceof ArrayType || lhsT == BaseType.VOID ||
                rhsT instanceof StructType || rhsT instanceof ArrayType || rhsT == BaseType.VOID){
                error("Cannot perform "+bo.op+" between type "+lhsT+" and type "+rhsT);
                return BaseType.INT;
            }
            bo.type = BaseType.INT;
            return bo.type;
        }
    }

    @Override
    public Type visitOp(Op op) {
        return null;
    }

    @Override
    public Type visitArrayAccessExpr(ArrayAccessExpr aa) {
	    // Check if index is of type int
        Type indexT = aa.index.accept(this);
        if (!(indexT == BaseType.INT)){
            error("Cannot access an array at "+aa.index+" position");
        }
        // Check if array we are trying to access has been declared as an array
        Type arrayT = aa.array.accept(this);
        if (arrayT instanceof ArrayType) {
            ArrayType array = ((ArrayType) aa.array.type);
            if (array.type == BaseType.INT || array.type == BaseType.CHAR || array.type == BaseType.VOID) {
                aa.type = array.type;
                return aa.type;
            }
        }
        if (arrayT instanceof PointerType){
            PointerType array = ((PointerType) aa.array.type);
            if (array.type == BaseType.INT || array.type == BaseType.CHAR || array.type == BaseType.VOID){
                aa.type = array.type;
                return aa.type;
            }
        }
        return BaseType.VOID;
    }

    @Override
    public Type visitFieldAccessExpr(FieldAccessExpr fa) {
	    Type faT = fa.structure.accept(this);
        if (faT instanceof StructType){

        }
        return null;
    }

    @Override
    public Type visitValueAtExpr(ValueAtExpr va) {
	    Type vaT = va.expr.accept(this);
        if (vaT instanceof PointerType){
        PointerType pointer = ((PointerType) vaT);
        if (pointer.type == BaseType.INT || pointer.type == BaseType.CHAR || pointer.type == BaseType.VOID){
            va.type = pointer.type;
            return va.type;
        }
    }
        return BaseType.VOID;
    }

    @Override
    public Type visitSizeOfExpr(SizeOfExpr so) {
        return BaseType.INT;
    }

    @Override
    public Type visitTypecastExpr(TypecastExpr t) {
	    Type tT = t.expr.accept(this);
        if (tT == BaseType.CHAR && t.type == BaseType.INT){
            t.expr.type = BaseType.INT;
            return t.expr.type;
        }
        if (tT instanceof ArrayType && t.type instanceof PointerType){
            t.expr.type = new PointerType(((PointerType) t.type).type);
            return t.expr.type;
        }
        if (tT instanceof PointerType && t.type instanceof PointerType){
            t.expr.type = new PointerType(((PointerType) t.type).type);
            return t.expr.type;
        }
        return BaseType.VOID;
    }

    @Override
    public Type visitExprStmt(ExprStmt e) {
        e.expr.accept(this);
        return null;
    }

    @Override
    public Type visitWhile(While w) {
        Type wT = w.expr.accept(this);
        if(! (wT == BaseType.INT)){
            error("While expression must be an int");
        }
        return null;
    }

    @Override
    public Type visitIf(If i) {
        Type iT = i.expr.accept(this);
        if(!(iT == BaseType.INT)){
            error("If expression must be an int");
        }
        return null;
    }

    @Override
    public Type visitAssign(Assign a) {
	    Type aT1 = a.expr1.accept(this);
        Type aT2 = a.expr2.accept(this);
        return null;
    }

    @Override
    public Type visitReturn(Return r) {
        // To be completed...
        return null;
    }

	@Override
	public Type visitBlock(Block b) {
		for (VarDecl vd : b.params){
		    vd.accept(this);
        }
        for (Stmt stmts : b.stmts){
            stmts.accept(this);
        }
		return null;
	}

	@Override
	public Type visitProgram(Program p) {
	    try{
            for (StructTypeDecl std : p.structTypeDecls) {
                std.accept(this);
            }
            for (VarDecl vd : p.varDecls) {
                vd.accept(this);
            }
            for (FunDecl fd : p.funDecls) {
                fd.accept(this);
            }
        } catch (NullPointerException e){
	        System.out.println("Null pointer exception");
	        e.printStackTrace();
        }
		return null;

	}

    @Override
    public Type visitStructTypeDecl(StructTypeDecl st) {
        structTypeDeclList.add(st);
	    for (VarDecl vd : st.params){
	        vd.accept(this);
        }
        return null;
    }

    @Override
    public Type visitFunDecl(FunDecl p) {
        p.type.accept(this);
        for (VarDecl vd : p.params){
            vd.accept(this);
        }
        p.block.accept(this);
        return null;
    }

	@Override
	public Type visitVarDecl(VarDecl vd) {
		if (vd.type == BaseType.VOID){
		    error("Cannot declare a variable with type VOID");
        }
		return null;
	}

}
