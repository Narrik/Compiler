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


    private String printChar(char c){
        StringBuilder sb = new StringBuilder();
        switch (c){
            case('\t'): sb.append("\\"); sb.append("t"); break;
            case('\b'): sb.append("\\"); sb.append("b"); break;
            case('\n'): sb.append("\\"); sb.append("n"); break;
            case('\r'): sb.append("\\"); sb.append("r"); break;
            case('\f'): sb.append("\\"); sb.append("f"); break;
            case('\''): sb.append("\\"); sb.append("'"); break;
            case('\"'): sb.append("\\"); sb.append("\""); break;
            case('\\'): sb.append("\\"); sb.append("\\"); break;
            case('\0'): sb.append("\\"); sb.append("0"); break;
            default: sb.append(c);
        }
        return sb.toString();
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
    private int funCount = 0;
    private int fpOffset = 0;
    private int trueCount = 0;
    private int endCount = 0;
    private int elseCount = 0;


    @Override
    public Register visitProgram(Program p) {
        for (StructTypeDecl std : p.structTypeDecls) {
            std.accept(this);
        }
        writer.println(data);
        for (VarDecl vd : p.varDecls) {

            // if variable is an int, char or pointer
            if (vd.type == BaseType.INT || vd.type == BaseType.CHAR|| vd.type instanceof PointerType ){
                writer.println(vd.varName + ": .space 4");
                vd.varLoc = vd.varName;

            // if variable is an array type
            } else if (vd.type instanceof ArrayType){
                int elements = (((ArrayType) vd.type).elements)*4;
                // if we have an array of chars, each char only requires 1 byte, but need to word align
                if (((ArrayType) vd.type).type == BaseType.CHAR){
                    elements = ((ArrayType) vd.type).elements;
                    elements += 4-(elements % 4);
                }
                writer.println(vd.varName + ": .space "+elements);
                vd.varLoc = vd.varName;

            } else if (vd.type instanceof StructType){
                //TODO: STRUCTS
            }
        }
        for (FunDecl fd : p.funDecls) {
            fd.accept(this);
        }
        return null;
    }

    @Override
    public Register visitStructTypeDecl(StructTypeDecl st) {
        return null;
    }

    @Override
    public Register visitVarDecl(VarDecl vd) {
        // TODO: to complete
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
            //TODO: STRUCTS
        }
        return null;
    }

    @Override
    public Register visitFunDecl(FunDecl p) {
        // TODO: to complete
        // exclude built in functions
        if (!(p.name.equals("print_s") || p.name.equals("print_i") || p.name.equals("print_c") ||
              p.name.equals("read_c") || p.name.equals("read_i") || p.name.equals("mcmalloc"))){
            // main cannot be shadowed and doesn't need numbering
            if (p.name.equals("main")){
                writer.print("main:");
            } else {
                writer.print(p.name + funCount + ":");
            }
            funCount++;
            writer.println(text);
            writer.println("move $fp, $sp");
            p.block.accept(this);
        }
        return null;
    }


    // TYPES
    @Override
    public Register visitBaseType(BaseType bt) { return null; }

    @Override
    public Register visitPointerType(PointerType pt) { return null; }


    @Override
    public Register visitStructType(StructType st) { return null; }

    @Override
    public Register visitArrayType(ArrayType at) { return null; }


    // EXPRESSIONS
    @Override
    public Register visitIntLiteral(IntLiteral i) {
        Register intLiteral = getRegister();
        writer.println("li "+intLiteral+", "+i.value);
        return intLiteral;
    }

    @Override
    public Register visitStrLiteral(StrLiteral s) {
        Register strLiteral = getRegister();
        writer.println(data);
        String curString = "var" + strCount;
        strCount++;
        writer.println(curString + ": .asciiz " + "\"" + s.value + "\"");
        writer.println(text);
        writer.println("la "+strLiteral+", " + curString);
        return strLiteral;
    }

    @Override
    public Register visitChrLiteral(ChrLiteral c) {
        Register chrLiteral = getRegister();
        writer.println("li "+chrLiteral+", '" + printChar(c.value)+"'");
        return chrLiteral;
    }

    @Override
    public Register visitVarExpr(VarExpr v) {
        Register variable = getRegister();
        writer.println("la "+variable+", "+v.vd.varLoc);
        writer.println("lw "+variable+", ("+variable+")");
        return variable;
    }

    @Override
    public Register visitFunCallExpr(FunCallExpr fc) {
        // TODO: complete undefined function
        // inbuilt print_s
        if (fc.name.equals("print_s")) {
            Register addressToPrint = fc.params.get(0).accept(this);
            writer.println("la $a0, (" + addressToPrint + ")");
            writer.println("li $v0, 4");
            writer.println(syscall);
            freeRegister(addressToPrint);
            writer.println("li $v0, 0");
            return Register.v0;
            }

            // inbuilt print_i
        if (fc.name.equals("print_i")) {
            Register toPrint = fc.params.get(0).accept(this);
            writer.println("addi $a0, " + toPrint + ", 0");
            writer.println("li $v0, 1");
            writer.println(syscall);
            freeRegister(toPrint);
            writer.println("li $v0, 0");
            return Register.v0;
        }

        // inbuilt print_c
        if (fc.name.equals("print_c")) {
            Register toPrint = fc.params.get(0).accept(this);
            writer.println("addi $a0, " + toPrint + ", 0");
            writer.println("li $v0, 11");
            writer.println(syscall);
            freeRegister(toPrint);
            writer.println("li $v0, 0");
            return Register.v0;
        }

        // inbuilt read_c
        if (fc.name.equals("read_c")){
            writer.println("li $v0, 12");
            writer.println(syscall);
            return Register.v0;
        }

        // inbuilt read_i
        if (fc.name.equals("read_i")){
            writer.println("li $v0, 5");
            writer.println(syscall);
            return Register.v0;
        }

        // inbuilt mcmalloc
        if (fc.name.equals("mcmalloc")) {
            Register amount = fc.params.get(0).accept(this);
            writer.println("addi $a0, " + amount + ", 0");
            writer.println("li $v0, 9");
            writer.println(syscall);
            freeRegister(amount);
            return Register.v0;
        }
        return null;
    }

    @Override
    public Register visitBinOp(BinOp bo) {
        Register lhsReg = bo.lhs.accept(this);
        Register rhsReg;
        Register result = getRegister();
        switch (bo.op){
            case ADD: rhsReg = bo.rhs.accept(this);
                      writer.println("add "+result+", "+lhsReg+", "+rhsReg);
                      freeRegister(rhsReg); break;

            case SUB: rhsReg = bo.rhs.accept(this);
                      writer.println("sub "+result+", "+lhsReg+", "+rhsReg);
                      freeRegister(rhsReg); break;

            case MUL: rhsReg = bo.rhs.accept(this);
                      writer.println("mul "+result+", "+lhsReg+", "+rhsReg);
                      freeRegister(rhsReg); break;

            case DIV: rhsReg = bo.rhs.accept(this);
                      writer.println("div "+lhsReg+", "+rhsReg);
                      writer.println("mflo "+result);
                      freeRegister(rhsReg); break;

            case MOD: rhsReg = bo.rhs.accept(this);
                      writer.println("div "+lhsReg+", "+rhsReg);
                      writer.println("mfhi "+result);
                      freeRegister(rhsReg); break;

            case GT:  rhsReg = bo.rhs.accept(this);
                      writer.println("bgt "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.print("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case LT:  rhsReg = bo.rhs.accept(this);
                      writer.println("blt "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.print("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case GE : rhsReg = bo.rhs.accept(this);
                      writer.println("bge "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.print("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case LE : rhsReg = bo.rhs.accept(this);
                      writer.println("ble "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.print("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case NE : rhsReg = bo.rhs.accept(this);
                      writer.println("bne "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.print("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case EQ : rhsReg = bo.rhs.accept(this);
                      writer.println("beq "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.print("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case OR : String trueStr = "true"+trueCount; trueCount++;
                      Register oneReg = getRegister();
                      writer.println("li "+oneReg+", 1");
                      writer.println("beq "+lhsReg+", "+oneReg+", "+trueStr);
                      rhsReg = bo.rhs.accept(this);
                      writer.println("beq "+rhsReg+", "+oneReg+", "+trueStr);
                      freeRegister(oneReg);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println(trueStr+": li "+result+", 1");
                      writer.print("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case AND: String falseStr = "false"+trueCount;
                      writer.println("beqz "+lhsReg+", "+falseStr);
                      rhsReg = bo.rhs.accept(this);
                      writer.println("beqz "+rhsReg+", "+falseStr);
                      writer.println("li "+result+", 1");
                      writer.println("j end"+endCount);
                      writer.println(falseStr+": li "+result+", 0"); trueCount++;
                      writer.print("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;
        }
        freeRegister(lhsReg);
        return result;
    }

    @Override
    public Register visitOp(Op op) { return null; }

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
        Register size = getRegister();
        if (so.type == BaseType.CHAR){
            writer.println("li "+size+", 1");
        } else {
            writer.println("li "+size+", 4");
        }
        return size;
    }

    @Override
    public Register visitTypecastExpr(TypecastExpr tc) { return null; }


    // STATEMENTS
    @Override
    public Register visitExprStmt(ExprStmt e) {
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
        Register truthValue = i.expr.accept(this);
        String elseStr = "else"+elseCount; elseCount++;
        String endStr = "end"+endCount; endCount++;
        writer.println("beqz "+ truthValue +", "+elseStr);
        i.stmt1.accept(this);
        writer.println("j "+endStr);
        writer.println(elseStr+":");
        if (i.stmt2 != null){
            i.stmt2.accept(this);
        }
        writer.print(endStr+": ");
        return null;
    }

    @Override
    public Register visitAssign(Assign a) {
        // TODO: to complete
        Register expr1Address = getRegister();
        if (a.expr1 instanceof VarExpr){
            writer.println("la "+expr1Address+", "+((VarExpr) a.expr1).vd.varLoc);
        }
        // TODO field,array,valueat expressions
        Register expr2 = a.expr2.accept(this);
        writer.println("sw "+expr2+", ("+expr1Address+")");
        freeRegister(expr1Address);
        if (expr2 != Register.v0)
            freeRegister(expr2);
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
        fpOffset = 0;
        for (VarDecl vd : b.params){
            vd.accept(this);
        }
        for (Stmt stmts : b.stmts){
            stmts.accept(this);
        }
        return null;
    }
}
