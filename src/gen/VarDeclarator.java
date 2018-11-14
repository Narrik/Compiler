package gen;

import ast.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

public class VarDeclarator implements ASTVisitor<Register> {


    private PrintWriter writer; // use this writer to output the assembly instructions
    private int fpOffset;
    public List<StructTypeDecl> structTypeDeclList = new ArrayList<>();

    public int addVarDecls(FunDecl fd, int fpOffset, PrintWriter writer, List<StructTypeDecl> structTypeDeclList){
        this.writer = writer;
        this.fpOffset = fpOffset;
        this.structTypeDeclList = structTypeDeclList;
        visitFunDecl(fd);
        return this.fpOffset;
    }

    // Implement Type
    public Register visitBaseType(BaseType bt){ return null; }
    public Register visitPointerType(PointerType pt){ return null; }
    public Register visitStructType(StructType st){ return null; }
    public Register visitArrayType(ArrayType at){ return null; }

    // Extends Expr (except Op)
    public Register visitIntLiteral(IntLiteral i){ return null; }
    public Register visitStrLiteral(StrLiteral s){ return null; }
    public Register visitChrLiteral(ChrLiteral c){ return null; }
    public Register visitVarExpr(VarExpr v){ return null; }
    public Register visitFunCallExpr(FunCallExpr f){ return null; }
    public Register visitBinOp(BinOp bo){ return null; }
    public Register visitOp(Op op){ return null; }
    public Register visitArrayAccessExpr(ArrayAccessExpr aa){ return null; }
    public Register visitFieldAccessExpr(FieldAccessExpr fa){ return null; }
    public Register visitValueAtExpr(ValueAtExpr va){ return null; }
    public Register visitSizeOfExpr(SizeOfExpr so){ return null; }
    public Register visitTypecastExpr(TypecastExpr t){ return null; }

    // Extends Stmt
    public Register visitExprStmt(ExprStmt es){ return null; }

    @Override
    public Register visitWhile(While w){
        w.stmt.accept(this);
        return null;
    }

    @Override
    public Register visitIf(If i){
        i.stmt1.accept(this);
        if (i.stmt2 != null) {
            i.stmt2.accept(this);
        }
        return null; }

    public Register visitAssign(Assign a){ return null; }
    public Register visitReturn(Return r){ return null; }

    @Override
    public Register visitBlock(Block b){
        for (VarDecl vd : b.params){
            vd.accept(this);
        }
        for (Stmt stmts : b.stmts){
            stmts.accept(this);
        }
        return null;
    }

    @Override
    public Register visitProgram(Program p){ return null; }

    @Override
    public Register visitStructTypeDecl(StructTypeDecl st){ return null; }

    @Override
    public Register visitVarDecl(VarDecl vd) {
        // if variable is an int, char or pointer
        if (vd.type == BaseType.INT || vd.type == BaseType.CHAR|| vd.type instanceof PointerType ){
            writer.println("addi $sp, $sp, -4");
            vd.varLoc = "-"+fpOffset+"($fp)";
            fpOffset += 4;

            // if variable is an array type
        } else if (vd.type instanceof ArrayType){
            int elements = (((ArrayType) vd.type).elements)*4;
            // if we have an array of chars, each char only requires 1 byte, but need to word align
            if (((ArrayType) vd.type).type == BaseType.CHAR){
                elements = ((ArrayType) vd.type).elements;
                elements += 4-(elements % 4);
            }
            writer.println("addi $sp, $sp, -"+elements);
            vd.varLoc = "-"+fpOffset+"($fp)";
            fpOffset += elements;

        } else if (vd.type instanceof StructType){
            for (StructTypeDecl s : structTypeDeclList) {
                if (s.structType.structName.equals(((StructType) vd.type).structName)) {
                    for (VarDecl svd : s.params) {
                        writer.println("addi $sp, $sp, -4");
                        svd.varLoc = "-"+fpOffset+"($fp)";
                        fpOffset += 4;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Register visitFunDecl(FunDecl p){
        p.block.accept(this);
        return null;
    }

}
