package ast;

public interface ASTVisitor<T> {

    // Implement Type
    public T visitBaseType(BaseType bt);
    public T visitPointerType(PointerType pt);
    public T visitStructType(StructType st);
    public T visitArrayType(ArrayType at);

    // Extends Expr (except Op)
    public T visitIntLiteral(IntLiteral i);
    public T visitStrLiteral(StrLiteral s);
    public T visitChrLiteral(ChrLiteral c);
    public T visitVarExpr(VarExpr v);
    public T visitFunCallExpr(FunCallExpr f);
    public T visitBinOp(BinOp bo);
    public T visitOp(Op op);
    public T visitArrayAccessExpr(ArrayAccessExpr aa);
    public T visitFieldAccessExpr(FieldAccessExpr fa);
    public T visitValueAtExpr(ValueAtExpr va);
    public T visitSizeOfExpr(SizeOfExpr so);
    public T visitTypecastExpr(TypecastExpr t);

    // Extends Stmt
    public T visitExprStmt(ExprStmt es);
    public T visitWhile(While w);
    public T visitIf(If i);
    public T visitAssign(Assign a);
    public T visitReturn(Return r);
    public T visitBlock(Block b);

    public T visitProgram(Program p);
    public T visitStructTypeDecl(StructTypeDecl st);
    public T visitVarDecl(VarDecl vd);
    public T visitFunDecl(FunDecl p);

    // to complete ... (should have one visit method for each concrete AST node class)
}
