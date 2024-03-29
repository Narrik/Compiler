package gen;

import ast.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
        if (reg != Register.v0) {
            freeRegs.push(reg);
        }
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

    public List<StructTypeDecl> structTypeDeclList = new ArrayList<>();
    private final String data = ".data";
    private final String text = ".text";
    private final String syscall = "syscall";
    private int strCount = 0;
    private int funCount = 0;
    private int fpOffset = 0;
    private int trueCount = 0;
    private int startCount = 0;
    private int endCount = 0;
    private int elseCount = 0;
    private int eofCount = 0;


    @Override
    public Register visitProgram(Program p) {
        writer.println(data);
        for (StructTypeDecl std : p.structTypeDecls) {
            std.accept(this);
        }
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
            // if variable is a struct type
            } else if (vd.type instanceof StructType){
                for (StructTypeDecl s : structTypeDeclList) {
                    if (s.structType.structName.equals(((StructType) vd.type).structName)) {
                        for (VarDecl svd : s.params) {
                            writer.println(vd.varName+"_"+svd.varName + ": .space 4");
                            svd.varLoc = vd.varName+"_"+svd.varName;
                        }
                    }
                }
            }
        }
        writer.println(text);
        for (FunDecl fd : p.funDecls) {
            fd.accept(this);
        }
        return null;
    }

    @Override
    public Register visitStructTypeDecl(StructTypeDecl st) {
        structTypeDeclList.add(st);
        return null;
    }

    @Override
    public Register visitVarDecl(VarDecl vd) {
        // handled by VarDeclarator
        return null;
    }

    @Override
    public Register visitFunDecl(FunDecl p) {
        // exclude built in functions
        if (!(p.name.equals("print_s") || p.name.equals("print_i") || p.name.equals("print_c") ||
              p.name.equals("read_c") || p.name.equals("read_i") || p.name.equals("mcmalloc"))){
            // reset fpOffset
            fpOffset = 0;
            // main function
            if (p.name.equals("main")){
                p.funLoc = "main";
                writer.println(".globl main");
                writer.print(p.funLoc+": ");
                writer.println("move $fp, $sp");
                fpOffset = 0;

                // declare all local variables
                VarDeclarator vd = new VarDeclarator();
                fpOffset = vd.addVarDecls(p, fpOffset, writer, structTypeDeclList);

                //continue as usual
                p.block.accept(this);

                //mark end of function for early return statements
                writer.println("eof"+eofCount+":"); eofCount++;
                if (p.type == BaseType.INT) {
                    writer.println("move $a0, $v0");
                } else {
                    writer.println("li $a0, 0");
                }
                writer.println("li $v0, 17");
                writer.println(syscall);
            // not main function
            } else {
                p.funLoc = p.name + funCount; funCount++;
                writer.print(p.funLoc + ": ");
                writer.println("move $fp, $sp");
                fpOffset = 0;

                // store temporary registers
                for (Register r : Register.tmpRegs){
                writer.println("addi $sp, $sp, -4");
                writer.println("sw "+r+", -"+fpOffset+"($fp)"); fpOffset += 4;
                }

                // load arguments from stack
                int fpStart = 0;
                int fpEnd = 0;
                for (VarDecl vd : p.params){
                    if (vd.type == BaseType.INT || vd.type == BaseType.CHAR || vd.type instanceof PointerType ){
                        fpEnd += 4;
                        p.block.params.remove(0);
                    }
                }
                for (VarDecl vd : p.params){
                    if (vd.type == BaseType.INT || vd.type == BaseType.CHAR || vd.type instanceof PointerType ){
                        vd.varLoc = "+"+((fpEnd-fpStart)+8)+"($fp)";
                        fpStart += 4;
                    }
                }

                // declare all local variables
                VarDeclarator vd = new VarDeclarator();
                fpOffset = vd.addVarDecls(p, fpOffset, writer, structTypeDeclList);

                // continue as usual
                p.block.accept(this);

                //mark end of function for early return statements
                writer.println("eof"+eofCount+":"); eofCount++;
                // restore temporary registers
                int restore = 0;
                for (Register r : Register.tmpRegs){
                    writer.println("lw "+r+", -"+restore+"($fp)"); restore += 4;

                }
                writer.println("move $sp, $fp");
                writer.println("jr $ra");
                writer.println();
            }
        }
        return null;
    }



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
        if (!(v.vd.type instanceof ArrayType))
        writer.println("lw "+variable+", ("+variable+")");
        return variable;
    }

    @Override
    public Register visitFunCallExpr(FunCallExpr fc) {
        // inbuilt print_s
        if (fc.name.equals("print_s")) {
            Register addressToPrint = fc.params.get(0).accept(this);
            writer.println("la $a0, (" + addressToPrint + ")");
            writer.println("li $v0, 4");
            writer.println(syscall);
            freeRegister(addressToPrint);
            return null;
            }

            // inbuilt print_i
        if (fc.name.equals("print_i")) {
            Register toPrint = fc.params.get(0).accept(this);
            writer.println("addi $a0, " + toPrint + ", 0");
            writer.println("li $v0, 1");
            writer.println(syscall);
            freeRegister(toPrint);
            return null;
        }

        // inbuilt print_c
        if (fc.name.equals("print_c")) {
            Register toPrint = fc.params.get(0).accept(this);
            writer.println("addi $a0, " + toPrint + ", 0");
            writer.println("li $v0, 11");
            writer.println(syscall);
            freeRegister(toPrint);
            return null;
        }

        // inbuilt read_c
        if (fc.name.equals("read_c")){
            writer.println("li $v0, 12");
            writer.println(syscall);
            Register ret = getRegister();
            writer.println("move "+ret+", $v0");
            return ret;
        }

        // inbuilt read_i
        if (fc.name.equals("read_i")){
            writer.println("li $v0, 5");
            writer.println(syscall);
            Register ret = getRegister();
            writer.println("move "+ret+", $v0");
            return ret;
        }

        // inbuilt mcmalloc
        if (fc.name.equals("mcmalloc")) {
            Register amount = fc.params.get(0).accept(this);
            writer.println("addi $a0, " + amount + ", 0");
            writer.println("li $v0, 9");
            writer.println(syscall);
            freeRegister(amount);
            Register ret = getRegister();
            writer.println("move "+ret+", $v0");
            return ret;
        }
        int paramCount = 0;
        int fpTemporary = 0;

        // initialize arguments
        for (VarDecl vd : fc.fd.params) {
            if (vd.type == BaseType.INT || vd.type == BaseType.CHAR || vd.type instanceof PointerType ){
                Register arg = fc.params.get(paramCount).accept(this);
                writer.println("addi $sp, $sp, -4");
                writer.println("sw "+arg+", -"+(fpOffset+fpTemporary)+"($fp)"); fpTemporary += 4;
                freeRegister(arg);
                paramCount++;
            }
        }

        // store $ra and $fp before function call
        writer.println("addi $sp, $sp, -4");
        writer.println("sw $ra, -"+(fpOffset+fpTemporary)+"($fp)");
        writer.println("addi $sp, $sp, -4");
        writer.println("sw $fp, -"+(fpOffset+fpTemporary+4)+"($fp)");

        // function call
        writer.println("jal "+fc.fd.funLoc);

        // restore $ra and $fp after function call and free stack
        writer.println("lw $ra, 8($fp)");
        writer.println("addi $sp, $sp, +4");
        writer.println("lw $fp, 4($fp)");
        writer.println("addi $sp, $sp, +4");

        // free stack from function arguments
        for (VarDecl vd : fc.fd.params) {
            if (vd.type == BaseType.INT || vd.type == BaseType.CHAR || vd.type instanceof PointerType ){
                writer.println("addi $sp, $sp, +4");
            }
        }

        // return result
        if (fc.fd.type == BaseType.VOID) {
            return null;
        }
        Register ret = getRegister();
        writer.println("move "+ret+", $v0");
        return ret;
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
                      writer.println("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case LT:  rhsReg = bo.rhs.accept(this);
                      writer.println("blt "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.println("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case GE : rhsReg = bo.rhs.accept(this);
                      writer.println("bge "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.println("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case LE : rhsReg = bo.rhs.accept(this);
                      writer.println("ble "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.println("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case NE : rhsReg = bo.rhs.accept(this);
                      writer.println("bne "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.println("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case EQ : rhsReg = bo.rhs.accept(this);
                      writer.println("beq "+lhsReg+", "+rhsReg+", true"+trueCount);
                      writer.println("li "+result+", 0");
                      writer.println("j end"+endCount);
                      writer.println("true"+trueCount+": li "+result+", 1"); trueCount++;
                      writer.println("end"+endCount+": "); endCount++;
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
                      writer.println("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;

            case AND: String falseStr = "false"+trueCount;
                      writer.println("beqz "+lhsReg+", "+falseStr);
                      rhsReg = bo.rhs.accept(this);
                      writer.println("beqz "+rhsReg+", "+falseStr);
                      writer.println("li "+result+", 1");
                      writer.println("j end"+endCount);
                      writer.println(falseStr+": li "+result+", 0"); trueCount++;
                      writer.println("end"+endCount+": "); endCount++;
                      freeRegister(rhsReg); break;
        }
        freeRegister(lhsReg);
        return result;
    }

    @Override
    public Register visitOp(Op op) { return null; }

    @Override
    public Register visitArrayAccessExpr(ArrayAccessExpr aa) {
        Register arrayAddress = getRegister();
        Register arrayIndex = aa.index.accept(this);
        Register value = getRegister();
        // if our array isn't of type char, we need to multiply the index by 4 and use sw instead of sb
        VarDecl vd =((VarExpr) aa.array).vd;
        if (((ArrayType) vd.type).type != BaseType.CHAR){
            Register num = getRegister();
            writer.println("li "+num+", 4");
            writer.println("mul "+arrayIndex+", "+arrayIndex+", "+num);
            freeRegister(num);
            writer.println("la "+arrayAddress+", "+((VarExpr) aa.array).vd.varLoc);
            writer.println("add "+arrayAddress+", "+arrayAddress+", "+arrayIndex);
            writer.println("lw "+value+", ("+arrayAddress+")");
        } else {
            writer.println("la "+arrayAddress+", "+((VarExpr) aa.array).vd.varLoc);
            writer.println("add "+arrayAddress+", "+arrayAddress+", "+arrayIndex);
            writer.println("lb "+value+", ("+arrayAddress+")");
        }

        freeRegister(arrayAddress);
        freeRegister(arrayIndex);
        return value;
    }

    @Override
    public Register visitFieldAccessExpr(FieldAccessExpr fa) {
        Register value = getRegister();
        for (StructTypeDecl s : structTypeDeclList) {
            if (s.structType.structName.equals(((StructType) fa.structure.type).structName)) {
                for (VarDecl vd : s.params) {
                    if (fa.field.equals(vd.varName)) {
                        writer.println("la "+value+", "+vd.varLoc);
                        writer.println("lw "+value+", ("+value+")");
                    }
                }
            }
        }
        return value;
    }

    @Override
    public Register visitValueAtExpr(ValueAtExpr va) {
        Register value = getRegister();
        String valueAddress = ((VarExpr) va.expr).vd.varLoc;
        writer.println("la "+value+", "+valueAddress);
        writer.println("lw "+value+", ("+value+")");
        writer.println("lw "+value+", ("+value+")");
        return value;
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
    public Register visitTypecastExpr(TypecastExpr tc) {
        return tc.expr.accept(this);
    }


    // STATEMENTS
    @Override
    public Register visitBlock(Block b) {
        // variables declared by VarDeclarator
        for (Stmt stmts : b.stmts){
            stmts.accept(this);
        }
        return null;
    }

    @Override
    public Register visitExprStmt(ExprStmt e) {
        e.expr.accept(this);
        return null;
    }

    @Override
    public Register visitWhile(While w) {
        String endStr = "end"+endCount; endCount++;
        String whileStr = "while"+startCount; startCount++;
        writer.println(whileStr+": ");
        Register truthValue = w.expr.accept(this);
        writer.println("beqz "+ truthValue +", "+endStr);
        freeRegister(truthValue);
        w.stmt.accept(this);
        writer.println("j "+whileStr);
        writer.println(endStr+": ");
        return null;
    }

    @Override
    public Register visitIf(If i) {
        String elseStr = "else"+elseCount; elseCount++;
        String endStr = "end"+endCount; endCount++;
        Register truthValue = i.expr.accept(this);
        writer.println("beqz "+ truthValue +", "+elseStr);
        freeRegister(truthValue);
        i.stmt1.accept(this);
        writer.println("j "+endStr);
        writer.println(elseStr+":");
        if (i.stmt2 != null){
            i.stmt2.accept(this);
        }
        writer.println(endStr+": ");
        return null;
    }

    @Override
    public Register visitAssign(Assign a) {
        int isChar = 0;
        Register expr1Address = getRegister();
        if (a.expr1 instanceof VarExpr){
            writer.println("la "+expr1Address+", "+((VarExpr) a.expr1).vd.varLoc);
        }

        if(a.expr1 instanceof ArrayAccessExpr){
            String arrayAddress = ((VarExpr)((ArrayAccessExpr) a.expr1).array).vd.varLoc;
            Register arrayIndex = ((ArrayAccessExpr) a.expr1).index.accept(this);
            // if our array isn't of type char, we need to multiply the index by 4
            VarDecl vd =((VarExpr)((ArrayAccessExpr) a.expr1).array).vd;
            if (((ArrayType) vd.type).type != BaseType.CHAR){
                Register num = getRegister();
                writer.println("li "+num+", 4");
                writer.println("mul "+arrayIndex+", "+arrayIndex+", "+num);
                freeRegister(num);
                writer.println("la "+expr1Address+", "+arrayAddress);
                writer.println("add "+expr1Address+", "+expr1Address+", "+arrayIndex);
                freeRegister(arrayIndex);
            } else {
                isChar = 1;
                writer.println("la "+expr1Address+", "+arrayAddress);
                writer.println("add "+expr1Address+", "+expr1Address+", "+arrayIndex);
                freeRegister(arrayIndex);
            }
        }

        if (a.expr1 instanceof ValueAtExpr){
            String valueAddress = ((VarExpr) ((ValueAtExpr) a.expr1).expr).vd.varLoc;
            writer.println("la "+expr1Address+", "+valueAddress);
            writer.println("lw "+expr1Address+", ("+expr1Address+")");
        }

        if (a.expr1 instanceof FieldAccessExpr) {
            for (StructTypeDecl s : structTypeDeclList) {
                if (s.structType.structName.equals(((StructType) ((FieldAccessExpr) a.expr1).structure.type).structName)) {
                    for (VarDecl vd : s.params) {
                        if (((FieldAccessExpr)a.expr1).field.equals(vd.varName)) {
                            writer.println("la " + expr1Address + ", " + vd.varLoc);
                        }
                    }
                }
            }
        }

        Register expr2 = a.expr2.accept(this);
        if (isChar == 1){
            writer.println("sb "+expr2+", ("+expr1Address+")");
        } else {
            writer.println("sw " + expr2 + ", (" + expr1Address + ")");
        }
        freeRegister(expr1Address);
        freeRegister(expr2);
        return null;
    }

    @Override
    public Register visitReturn(Return r) {
        if (r.expr == null){
            writer.println("li $v0, 0");
            writer.println("j eof"+eofCount);
        } else {
            Register ret = r.expr.accept(this);
            writer.println("move $v0, "+ret);
            writer.println("j eof"+eofCount);
            freeRegister(ret);
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

}