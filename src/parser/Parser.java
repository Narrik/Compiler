package parser;

import ast.*;

import lexer.Token;
import lexer.Tokeniser;
import lexer.Token.TokenClass;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * @author cdubach
 */
public class Parser {

    private Token token;

    // use for backtracking (useful for distinguishing decls from procs when parsing a program for instance)
    private Queue<Token> buffer = new LinkedList<>();

    private final Tokeniser tokeniser;


    public Parser(Tokeniser tokeniser) {
        this.tokeniser = tokeniser;
    }

    public Program parse() {
        // get the first token
        nextToken();

        return parseProgram();
    }

    public int getErrorCount() {
        return error;
    }

    private int error = 0;
    private Token lastErrorToken;

    private void error(TokenClass... expected) {

        if (lastErrorToken == token) {
            // skip this error, same token causing trouble
            return;
        }

        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (TokenClass e : expected) {
            sb.append(sep);
            sb.append(e);
            sep = "|";
        }
        System.out.println("Parsing error: expected (" + sb + ") found (" + token + ") at " + token.position);

        error++;
        lastErrorToken = token;
    }

    /*
     * Look ahead the i^th element from the stream of token.
     * i should be >= 1
     */
    private Token lookAhead(int i) {
        // ensures the buffer has the element we want to look ahead
        while (buffer.size() < i)
            buffer.add(tokeniser.nextToken());
        assert buffer.size() >= i;

        int cnt = 1;
        for (Token t : buffer) {
            if (cnt == i)
                return t;
            cnt++;
        }

        assert false; // should never reach this
        return null;
    }


    /*
     * Consumes the next token from the tokeniser or the buffer if not empty.
     */
    private void nextToken() {
        if (!buffer.isEmpty())
            token = buffer.remove();
        else
            token = tokeniser.nextToken();
    }

    /*
     * If the current token is equals to the expected one, then skip it, otherwise report an error.
     * Returns the expected token or null if an error occurred.
     */
    private Token expect(TokenClass... expected) {
        for (TokenClass e : expected) {
            if (e == token.tokenClass) {
                Token cur = token;
                nextToken();
                return cur;
            }
        }

        error(expected);
        return null;
    }

    /*
     * Returns true if the current token is equals to any of the expected ones.
     */
    private boolean accept(TokenClass... expected) {
        boolean result = false;
        for (TokenClass e : expected)
            result |= (e == token.tokenClass);
        return result;
    }


    private Program parseProgram() {
        try {
            parseIncludes();
            List<StructTypeDecl> stds = parseStructDecls();
            List<VarDecl> vds = parseVarDecls();
            List<FunDecl> fds = parseFunDecls();
            expect(TokenClass.EOF);
            return new Program(stds, vds, fds);
        } catch (NullPointerException e){
            System.out.println("Null pointer exception");
            e.printStackTrace();
        }
        return new Program(new ArrayList<>(),new ArrayList<>(),new ArrayList<>());
    }

    // includes are ignored, so does not need to return an AST node
    private void parseIncludes() {
        if (accept(TokenClass.INCLUDE)) {
            nextToken();
            expect(TokenClass.STRING_LITERAL);
            parseIncludes();
        }
    }

    private List<StructTypeDecl> parseStructDecls() {
        List<StructTypeDecl> structTypeDecls = new ArrayList<>();
        if (accept(TokenClass.STRUCT)) {
            if (lookAhead(2).tokenClass == TokenClass.LBRA) {
                structTypeDecls.add(parseStructDecl());
                structTypeDecls.addAll(parseStructDecls());
            }
        }
        return structTypeDecls;
    }

    private StructTypeDecl parseStructDecl(){
        StructType structType = parseStruct();
        expect(TokenClass.LBRA);
        List<VarDecl> varDecls = new ArrayList<>();
        if (classAfterTypeIdent() == TokenClass.SC) {
            varDecls.add(parseVarDeclNormal());
        } else {
            varDecls.add(parseVarDeclArray());
        }
        varDecls.addAll(parseVarDecls());
        expect(TokenClass.RBRA);
        expect(TokenClass.SC);
        return new StructTypeDecl(structType,varDecls);
    }

    private List<VarDecl> parseVarDecls() {
        List<VarDecl> varDecls = new ArrayList<>();
        if (classAfterTypeIdent() == TokenClass.SC) {
            varDecls.add(parseVarDeclNormal());
            varDecls.addAll(parseVarDecls());
        }
        else if (classAfterTypeIdent() == TokenClass.LSBR) {
            varDecls.add(parseVarDeclArray());
            varDecls.addAll(parseVarDecls());
        }
        return varDecls;
    }

    private VarDecl parseVarDeclNormal(){
        Type t = parseType();
        Token varName = expect(TokenClass.IDENTIFIER);
        expect(TokenClass.SC);
        return new VarDecl(t,varName.data);
    }

    private VarDecl parseVarDeclArray(){
        Type t = parseType();
        Token varName = expect(TokenClass.IDENTIFIER);
        expect(TokenClass.LSBR);
        Token n  = expect(TokenClass.INT_LITERAL);
        int elements = Integer.parseInt(n.data);
        expect(TokenClass.RSBR);
        expect(TokenClass.SC);
        return new VarDecl(new ArrayType(t,elements),varName.data);
    }

    private List<FunDecl> parseFunDecls() {
        List<FunDecl> funDecls = new ArrayList<>();
        if(classAfterTypeIdent() == TokenClass.LPAR) {
            funDecls.add(parseFunDecl());
            funDecls.addAll(parseFunDecls());
        }
        return funDecls;
    }

    private FunDecl parseFunDecl(){
        Type t = parseType();
        Token funName = expect(TokenClass.IDENTIFIER);
        expect(TokenClass.LPAR);
        List<VarDecl> params = new ArrayList<>();
        if (!accept(TokenClass.RPAR)) {
            params = parseParams();
        }
        expect(TokenClass.RPAR);
        Block block = parseBlock();
        return new FunDecl(t,funName.data, params, block);
    }



    private Type parseType() {
        Type t;
        if (accept(TokenClass.STRUCT)) {
            t = parseStruct();
        } else if(accept(TokenClass.INT)) {
            nextToken();
            t = BaseType.INT;
        } else if(accept(TokenClass.CHAR)){
            nextToken();
            t = BaseType.CHAR;
        } else {
            expect(TokenClass.VOID) ;
            t = BaseType.VOID;
        }
        if (accept(TokenClass.ASTERIX)) {
            nextToken();
            return new PointerType(t);
        }
        return t;
    }

    private StructType parseStruct() {
        nextToken();
        Token n = expect(TokenClass.IDENTIFIER);
        return new StructType(n.data);
    }

    private List<VarDecl> parseParams(){
        List<VarDecl> params = new ArrayList<>();
        Type t = parseType();
        Token paramName = expect(TokenClass.IDENTIFIER);
        params.add(new VarDecl(t,paramName.data));
        if (accept(TokenClass.COMMA)){
            nextToken();
            params.addAll(parseParams());
        }
        return params;
    }


    private Block parseBlock(){
        expect(TokenClass.LBRA);
        List<VarDecl> params = parseVarDecls();
        List<Stmt> stmts = parseStmtList();
        expect(TokenClass.RBRA);
        return new Block(params,stmts);
    }

    private List<Stmt> parseStmtList(){
        List<Stmt> stmts = new ArrayList<>();
        if(!accept(TokenClass.RBRA)){
            stmts.add(parseStmt());
            stmts.addAll(parseStmtList());
        }
        return stmts;
    }

    private Stmt parseStmt(){
        // Block
        if (accept(TokenClass.LBRA)) {
            return parseBlock();
        }
        // While
        else if (accept(TokenClass.WHILE)){
            nextToken();
            expect(TokenClass.LPAR);
            Expr e = parseExp();
            expect(TokenClass.RPAR);
            Stmt stmt = parseStmt();
            return new While(e,stmt);
        }
        // If
        else if (accept(TokenClass.IF)){
            nextToken();
            expect(TokenClass.LPAR);
            Expr e = parseExp();
            expect(TokenClass.RPAR);
            Stmt stmt1 = parseStmt();
            if (accept(TokenClass.ELSE)){
                nextToken();
                Stmt stmt2 = parseStmt();
                return new If(e,stmt1,stmt2);
            }
            return new If(e,stmt1,null);
        }
        // Return
        else if (accept(TokenClass.RETURN)){
            nextToken();
            Expr e = null;
            if (!accept(TokenClass.SC)) {
                e = parseExp();
            }
            expect(TokenClass.SC);
            return new Return(e);
        }
        // ExprStmt or Assign
        else {
            Expr e = parseExp();
            if (!accept(TokenClass.SC)) {
                expect(TokenClass.ASSIGN);
                Expr e2 = parseExp();
                expect(TokenClass.SC);
                return new Assign(e,e2);
            }
            expect(TokenClass.SC);
            return new ExprStmt(e);
        }
    }

    private Expr parseExp(){
        Expr lhs = parseAnd();
        if (accept(TokenClass.OR)) {
            nextToken();
            Expr rhs = parseExp();
            return new BinOp(lhs,Op.OR,rhs);
        }
        return lhs;
    }

    private Expr parseAnd(){
        Expr lhs = parseEqNeq();
        if (accept(TokenClass.AND)) {
            nextToken();
            Expr rhs = parseAnd();
            return new BinOp(lhs,Op.AND,rhs);
        }
        return lhs;
    }

    private Expr parseEqNeq(){
        Expr lhs = parseLtGtLeGe();
        if (accept(TokenClass.EQ, TokenClass.NE)) {
            Op op;
            if (accept(TokenClass.EQ))
                op = Op.EQ;
            else
                op = Op.NE;
            nextToken();
            Expr rhs = parseEqNeq();
            return new BinOp(lhs,op,rhs);
        }
        return lhs;
    }

    private Expr parseLtGtLeGe(){
        Expr lhs = parseAddSub();
        if (accept(TokenClass.LT, TokenClass.GT, TokenClass.LE, TokenClass.GE)) {
            Op op;
            if (accept(TokenClass.LT))
                op = Op.LT;
            else if (accept(TokenClass.GT))
                op = Op.GT;
            else if (accept(TokenClass.LE))
                op = Op.LE;
            else
                op = Op.GE;
            nextToken();
            Expr rhs = parseLtGtLeGe();
            return new BinOp(lhs,op,rhs);
        }
        return lhs;
    }

    private Expr parseAddSub(){
        Expr lhs = parseMulDivRem();
        if (accept(TokenClass.PLUS, TokenClass.MINUS)) {
            Op op;
            if (accept(TokenClass.PLUS))
                op = Op.ADD;
            else
                op = Op.SUB;
            nextToken();
            Expr rhs = parseAddSub();
            return new BinOp(lhs,op,rhs);
        }
        return lhs;
    }

    private Expr parseMulDivRem(){
        Expr lhs = parsePUTS();
        if (accept(TokenClass.ASTERIX, TokenClass.DIV, TokenClass.REM)) {
            Op op;
            if (accept(TokenClass.ASTERIX))
                op = Op.MUL;
            else if (accept(TokenClass.DIV))
                op = Op.DIV;
            else
                op = Op.MOD;
            nextToken();
            Expr rhs = parseMulDivRem();
            return new BinOp(lhs,op,rhs);
        }
        return lhs;
    }

    private Expr parsePUTS(){
        // SizeOfExpr
        if (accept(TokenClass.SIZEOF)) {
            nextToken();
            expect(TokenClass.LPAR);
            Type t = parseType();
            expect(TokenClass.RPAR);
            return new SizeOfExpr(t);
        }
        // ValueAtExpr
        else if (accept(TokenClass.ASTERIX)){
            nextToken();
            Expr e = parsePUTS();
            return new ValueAtExpr(e);
        }
        // TypecastExpr
        else if (accept(TokenClass.LPAR) && (lookAhead(1).tokenClass == TokenClass.INT || lookAhead(1).tokenClass == TokenClass.CHAR || lookAhead(1).tokenClass == TokenClass.VOID || lookAhead(1).tokenClass == TokenClass.STRUCT)){
            nextToken();
            Type t = parseType();
            expect(TokenClass.RPAR);
            Expr e = parsePUTS();
            return new TypecastExpr(t,e);
        }
        // Unary Minus == BinOp(0,SUB,exp)
        else if (accept(TokenClass.MINUS)){
            nextToken();
            Expr e = parsePUTS();
            return new BinOp(new IntLiteral(0),Op.SUB,e);
        }
        else {
            return parseAS();
        }
    }

    private Expr parseAS(){
        Expr e = parseRest();
        // ArrayAccessExpr
        if (accept(TokenClass.LSBR)){
            nextToken();
            Expr index = parseExp();
            expect(TokenClass.RSBR);
            return new ArrayAccessExpr(e,index);
        }
        // FieldAccessExpr
        else if (accept(TokenClass.DOT)){
            nextToken();
            Token n = expect(TokenClass.IDENTIFIER);
            return new FieldAccessExpr(e,n.data);
        }
        return e;
    }

    private Expr parseRest(){
        // FunCallExpr
        if (accept(TokenClass.IDENTIFIER) && lookAhead(1).tokenClass == TokenClass.LPAR){
            Token n = expect(TokenClass.IDENTIFIER);
            String name = n.data;
            expect(TokenClass.LPAR);
            List<Expr> params = new ArrayList<>();
            if (!accept(TokenClass.RPAR)){
                params.add(parseExp());
                while (accept(TokenClass.COMMA)){
                    nextToken();
                    params.add(parseExp());
                }
            }
            expect(TokenClass.RPAR);
            return new FunCallExpr(name,params);
        // (Expr)
        } else if (accept(TokenClass.LPAR)) {
            nextToken();
            Expr e = parseExp();
            expect(TokenClass.RPAR);
            return e;
        }
        // VarExpr
        else if(accept(TokenClass.IDENTIFIER)) {
            Token n = expect(TokenClass.IDENTIFIER);
            return new VarExpr(n.data);
        }
        // IntLiteral
        else if(accept(TokenClass.INT_LITERAL)){
            Token n = expect(TokenClass.INT_LITERAL);
            int i = Integer.parseInt(n.data);
            return new IntLiteral(i);
        }
        // StrLiteral
        else if(accept(TokenClass.STRING_LITERAL)){
            Token n = expect(TokenClass.STRING_LITERAL);
            return new StrLiteral(n.data);
        }
        // ChrLiteral
        else {
            Token n = expect(TokenClass.CHAR_LITERAL);
            char c = n.data.charAt(0);
            if (n.data.charAt(0) == '\\') {
                switch (n.data.charAt(1)) {
                    case 't': c = '\t'; break;
                    case 'b': c = '\b'; break;
                    case 'n': c = '\n'; break;
                    case 'r': c = '\r'; break;
                    case 'f': c = '\f'; break;
                    case '\'': c = '\''; break;
                    case '"': c = '\"'; break;
                    case '\\': c = '\\'; break;
                    case '0': c = '\0'; break;
                    default: break;
                }
            }
            return new ChrLiteral(c);
        }
    }





    private TokenClass classAfterTypeIdent() {
        if (accept(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID)) {
            if (lookAhead(1).tokenClass == TokenClass.ASTERIX) {
                return (lookAhead(3).tokenClass);
            } else return (lookAhead(2).tokenClass);
        }
        if (accept(TokenClass.STRUCT)){
            if (lookAhead(2).tokenClass == TokenClass.ASTERIX) {
                return (lookAhead(4).tokenClass);
            } else return (lookAhead(3).tokenClass);
        }
        return token.tokenClass;

    }

}

