package sem;

import ast.*;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckVisitor extends BaseSemanticVisitor<Type> {

    public List<StructTypeDecl> structTypeDeclList = new ArrayList<>();
    public Type curFunType = BaseType.VOID;

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
                for (StructTypeDecl s : structTypeDeclList) {
                    if(s.structType.structName.equals(((StructType) faT).structName)){
                        for (VarDecl vd : s.params){
                            if(fa.field.equals(vd.varName)){
                                return vd.type;
                            }
                        }
                        error("Struct "+s.structType.structName+" has no "+fa.field+" field");
                        return null;
                    }
                }
                error("Struct "+((StructType) faT).structName+" hasn't been declared");
                return null;
            }
        error("Cannot do a field access on a "+faT);
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
        if (!aT1.equals(aT2)){
            error("Cannot assign "+aT2+" to expression of type "+aT1);
        }
        if (aT1 instanceof ArrayType || aT1 == BaseType.VOID){
            error("Assign expressions cannot be of type void or ArrayType");
        }
        if (a.expr1 instanceof VarExpr || a.expr1 instanceof FieldAccessExpr ||
            a.expr1 instanceof ArrayAccessExpr || a.expr1 instanceof ValueAtExpr){
            return null;
        } else {
            error("Left hand side expression of an assignment statement must be one of the following: VarExpr, FieldAccessExpr, ArrayAccessExpr or ValueAtExpr");
            return null;
        }
    }

    @Override
    public Type visitReturn(Return r) {
        if (r.expr == null){
            if (curFunType == BaseType.VOID){
                return null;
            } else {
                error("Cannot return null for a function with type "+curFunType);
                return null;
            }
        } else {
            Type rT = r.expr.accept(this);
            if (rT.equals(curFunType)){
                return null;
            } else {
                error("Return type "+rT+" and function type  "+curFunType+" don't match");
                return null;
            }
        }
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
	    curFunType = p.type;
        p.type.accept(this);
        for (VarDecl vd : p.params){
            vd.accept(this);
        }
        p.block.accept(this);
        return null;
    }

	@Override
	public Type visitVarDecl(VarDecl vd) {
	    if (vd.type instanceof StructType) {
            for (StructTypeDecl s : structTypeDeclList) {
                if(s.structType.structName.equals(((StructType) vd.type).structName)){
                    return null;
                }
            }
            error("Struct "+((StructType) vd.type).structName+" hasn't been declared");
        }
		if (vd.type == BaseType.VOID){
		    error("Cannot declare a variable with type VOID");
        }
		return null;
	}

}
