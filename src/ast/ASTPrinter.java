package ast;

import java.io.PrintWriter;

public class ASTPrinter implements ASTVisitor<Void> {

    private PrintWriter writer;

    public ASTPrinter(PrintWriter writer) {
            this.writer = writer;
    }

    //Types
    @Override
    public Void visitBaseType(BaseType bt){
        writer.print(""+bt);
        return null;
    }

    @Override
    public Void visitPointerType(PointerType pt){
        writer.print("PointerType(");
        pt.type.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitStructType(StructType st){
        writer.print("StructType(");
        writer.print(st.structName);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitArrayType(ArrayType at){
        writer.print("ArrayType(");
        at.type.accept(this);
        writer.print(","+at.elements);
        writer.print(")");
        return null;
    }

    //Expressions
    @Override
    public Void visitIntLiteral(IntLiteral i) {
        writer.print("IntLiteral(");
        writer.print(i.value);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitStrLiteral(StrLiteral s) {
        writer.print("StrLiteral(");
        writer.print(s.value);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitChrLiteral(ChrLiteral c) {
        writer.print("IntLiteral(");
        writer.print(c.value);
        writer.print(")");
        return null;
    }


    @Override
    public Void visitVarExpr(VarExpr v) {
        writer.print("VarExpr(");
        writer.print(v.name);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr f) {
        writer.print("FunCallExpr(");
        writer.print(f.name);
        for (Expr param: f.params) {
            writer.print(",");
            param.accept(this);
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitBinOp(BinOp bo) {
        writer.print("BinOp(");
        bo.lhs.accept(this);
        writer.print(",");
        bo.op.accept(this);
        writer.print(",");
        bo.rhs.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitOp(Op op){
        writer.print(""+op);
        return null;
    }

    @Override
    public Void visitArrayAccessExpr(ArrayAccessExpr aa) {
        writer.print("ArrayAccessExpr(");
        aa.array.accept(this);
        writer.print(",");
        aa.index.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitFieldAccessExpr(FieldAccessExpr fa) {
        writer.print("FieldAccessExpr(");
        fa.structure.accept(this);
        writer.print(",");
        writer.print(fa.field);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitValueAtExpr(ValueAtExpr va) {
        writer.print("ValueAtExpr(");
        va.expr.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitSizeOfExpr(SizeOfExpr so) {
        writer.print("SizeOfExpr(");
        so.type.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitTypecastExpr(TypecastExpr t) {
        writer.print("TypecastExpr(");
        t.type.accept(this);
        writer.print(",");
        t.expr.accept(this);
        writer.print(")");
        return null;
    }

    //Stmt

    @Override
    public Void visitExprStmt(ExprStmt es) {
        writer.print("ExprStmt(");
        es.expr.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitWhile(While w) {
        writer.print("While(");
        w.expr.accept(this);
        writer.print(",");
        w.stmt.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitIf(If i) {
        writer.print("If(");
        i.expr.accept(this);
        writer.print(",");
        i.stmt1.accept(this);
        if (i.stmt2 != null){
            writer.print(",");
            i.stmt2.accept(this);
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitAssign(Assign a) {
        writer.print("Assign(");
        a.expr1.accept(this);
        writer.print(",");
        a.expr2.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitReturn(Return r) {
        writer.print("Return(");
        if (r.expr != null){
            r.expr.accept(this);
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitBlock(Block b) {
        // more complicated than others since commas must match
        writer.print("Block(");
        if(!b.params.isEmpty()) {
            b.params.get(0).accept(this);
            for (int i = 1; i < b.params.size(); i++) {
                writer.print(",");
                b.params.get(i).accept(this);
            }
        }
        if(!b.stmts.isEmpty()) {
            if (!b.params.isEmpty()){
                writer.print(",");
            }
            b.stmts.get(0).accept(this);
            for (int i = 1; i < b.stmts.size(); i++) {
                writer.print(",");
                b.stmts.get(i).accept(this);
            }
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitProgram(Program p) {
        writer.print("Program(");
        String delimiter = "";
        for (StructTypeDecl std : p.structTypeDecls) {
            writer.print(delimiter);
            delimiter = ",";
            std.accept(this);
        }
        for (VarDecl vd : p.varDecls) {
            writer.print(delimiter);
            delimiter = ",";
            vd.accept(this);
        }
        for (FunDecl fd : p.funDecls) {
            writer.print(delimiter);
            delimiter = ",";
            fd.accept(this);
        }
        writer.print(")");
        writer.flush();
        return null;
    }

    @Override
    public Void visitStructTypeDecl(StructTypeDecl st) {
        writer.print("StructTypeDecl(");
        st.structType.accept(this);
        for (VarDecl vd : st.params) {
            writer.print(",");
            vd.accept(this);
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl vd){
        writer.print("VarDecl(");
        vd.type.accept(this);
        writer.print(","+vd.varName);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl fd) {
        writer.print("FunDecl(");
        fd.type.accept(this);
        writer.print(","+fd.name+",");
        for (VarDecl vd : fd.params) {
            vd.accept(this);
            writer.print(",");
        }
        fd.block.accept(this);
        writer.print(")");
        return null;
    }
}
