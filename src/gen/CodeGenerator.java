package gen;

import ast.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EmptyStackException;
import java.util.Stack;

public class CodeGenerator implements ASTVisitor<Register> {

    /*
     * Simple register allocator.
     */

    // contains all the free temporary registers
    private Stack<Register> freeRegs = new Stack<Register>();

    public CodeGenerator() {
        freeRegs.addAll(Register.tmpRegs);
    }

    private class RegisterAllocationError extends Error {}

    private Register getRegister() {
        try {
            return freeRegs.pop();
        } catch (EmptyStackException ese) {
            throw new RegisterAllocationError(); // no more free registers, bad luck!
        }
    }

    private void freeRegister(Register reg) {
        freeRegs.push(reg);
    }





    private PrintWriter writer; // use this writer to output the assembly instructions


    public void emitProgram(Program program, File outputFile) throws FileNotFoundException {
        writer = new PrintWriter(outputFile);

        visitProgram(program);
        writer.close();
    }

    private final String data = ".data";
    private final String text = ".text";
    private final String syscall = "syscall";
    private int strCount = 0;


    @Override
    public Register visitProgram(Program p) {
        // TODO: to complete
        //writer.print("main: ");
        /*for (StructTypeDecl std : p.structTypeDecls) {
            std.accept(this);
        }
        for (VarDecl vd : p.varDecls) {
            vd.accept(this);
        }*/
        for (FunDecl fd : p.funDecls) {
            fd.accept(this);
        }
        writer.println(text);
        writer.println("li $v0, 10");
        writer.println(syscall);
        return null;
    }

    @Override
    public Register visitStructTypeDecl(StructTypeDecl st) {
        return null;
    }

    @Override
    public Register visitVarDecl(VarDecl vd) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitFunDecl(FunDecl p) {
        // TODO: to complete
        p.type.accept(this);
        for (VarDecl vd : p.params){
            vd.accept(this);
        }
        p.block.accept(this);
        return null;
    }


    // TYPES
    @Override
    public Register visitBaseType(BaseType bt) {
        return null;
    }

    @Override
    public Register visitPointerType(PointerType pt) {
        // TODO: to complete
        return null;
    }


    @Override
    public Register visitStructType(StructType st) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitArrayType(ArrayType at) {
        // TODO: to complete
        return null;
    }


    // EXPRESSIONS
    @Override
    public Register visitIntLiteral(IntLiteral i) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitStrLiteral(StrLiteral s) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitChrLiteral(ChrLiteral c) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitVarExpr(VarExpr v) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitFunCallExpr(FunCallExpr fc) {
        // TODO: to complete solve instanceof
        System.out.println("here");
        if (fc.name.equals("print_s")) {
            System.out.println(fc.params.get(0));
            if (fc.params.get(0) instanceof StrLiteral) {
                String str = ((StrLiteral) fc.params.get(0)).value;
                String curString = "var" + strCount;
                strCount++;
                writer.println(data);
                writer.println(curString + " .asciiz " + "\"" + str + "\"");
                writer.println(text);
                writer.println("la $a0, " + curString);
                writer.println("li $v0, 4");
                writer.println(syscall);
            }
        }
        if (fc.name.equals("print_i")) {
            System.out.println(fc.params.get(0));
            if (fc.params.get(0) instanceof IntLiteral) {
                int intValue = ((IntLiteral) fc.params.get(0)).value;
                writer.println(text);
                writer.println("li $a0, " + intValue);
                writer.println("li $v0, 1");
                writer.println(syscall);
            }
        }
        return null;
    }

    @Override
    public Register visitBinOp(BinOp bo) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitOp(Op op) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitArrayAccessExpr(ArrayAccessExpr aa) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitFieldAccessExpr(FieldAccessExpr fa) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitValueAtExpr(ValueAtExpr va) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitSizeOfExpr(SizeOfExpr so) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitTypecastExpr(TypecastExpr tc) {
        // TODO: to complete
        return null;
    }


    // STATEMENTS
    @Override
    public Register visitExprStmt(ExprStmt e) {
        // TODO: to complete
        e.expr.accept(this);
        return null;
    }

    @Override
    public Register visitWhile(While w) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitIf(If i) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitAssign(Assign a) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitReturn(Return r) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitBlock(Block b) {
        // TODO: to complete
        for (VarDecl vd : b.params){
            vd.accept(this);
        }
        for (Stmt stmts : b.stmts){
            stmts.accept(this);
        }
        return null;
    }
}
