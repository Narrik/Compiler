package parser;

import ast.*;

import lexer.Token;
import lexer.Tokeniser;
import lexer.Token.TokenClass;

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
        parseIncludes();
        List<StructTypeDecl> stds = parseStructDecls();
        List<VarDecl> vds = parseVarDecls();
        List<FunDecl> fds = parseFunDecls();
        expect(TokenClass.EOF);
        return new Program(stds, vds, fds);
    }

    // includes are ignored, so does not need to return an AST node
    private void parseIncludes() {
        if (accept(TokenClass.INCLUDE)) {
            nextToken();
            expect(TokenClass.STRING_LITERAL);
            parseIncludes();
        }
    }
    
    private void parseStructDecls() {
        if (accept(TokenClass.STRUCT)) {
            if (lookAhead(2).tokenClass == TokenClass.LBRA) {
                parseStruct();
                expect(TokenClass.LBRA);
                parseVarDeclsMust();
                expect(TokenClass.RBRA);
                expect(TokenClass.SC);
                parseStructDecls();
            }
        }
    }

    private void parseVarDecls() {
        if (classAfterTypeIdent() == TokenClass.SC) {
            parseType();
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.SC);
            parseVarDecls();
        }
        else if (classAfterTypeIdent() == TokenClass.LSBR) {
            parseType();
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LSBR);
            expect(TokenClass.INT_LITERAL);
            expect(TokenClass.RSBR);
            expect(TokenClass.SC);
            parseVarDecls();
        }
    }

    private void parseFunDecls() {
        if(classAfterTypeIdent()== TokenClass.LPAR) {
            parseType();
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LPAR);
            if (!accept(TokenClass.RPAR))
                parseParams();
            expect(TokenClass.RPAR);
            parseBlock();
            parseFunDecls();
        }

    }

    private void parseVarDeclsMust() {
        parseType();
        expect(TokenClass.IDENTIFIER);
        if (accept(TokenClass.LSBR)){
            nextToken();
            expect(TokenClass.INT_LITERAL);
            expect(TokenClass.RSBR);
        }
        expect(TokenClass.SC);
        parseVarDecls();
    }


    private void parseType() {
        if (accept(TokenClass.STRUCT)) {
            parseStruct();
        } else {
            expect(TokenClass.INT, TokenClass.CHAR, TokenClass.VOID);
        }
        if (accept(TokenClass.ASTERIX)) {
            nextToken();
        }
    }

    private void parseStruct() {
        nextToken();
        expect(TokenClass.IDENTIFIER);
    }

    private void parseParams(){
        parseType();
        expect(TokenClass.IDENTIFIER);
        if (accept(TokenClass.COMMA)){
            nextToken();
            parseParams();
        }
    }

    private void parseBlock(){
        expect(TokenClass.LBRA);
        parseVarDecls();
        parseStmt();
        expect(TokenClass.RBRA);
    }

    private void parseStmt(){
        if (accept(TokenClass.LBRA)) {
            parseBlock();
            parseStmt();
        }
        else if (accept(TokenClass.WHILE)){
            nextToken();
            expect(TokenClass.LPAR);
            parseExp();
            expect(TokenClass.RPAR);
            parseStmtMust();
        }
        else if (accept(TokenClass.IF)){
            nextToken();
            expect(TokenClass.LPAR);
            parseExp();
            expect(TokenClass.RPAR);
            parseStmtMust();
            if (accept(TokenClass.ELSE)){
                nextToken();
                parseStmtMust();
            }
        }
        else if (accept(TokenClass.RETURN)){
            nextToken();
            if (!accept(TokenClass.SC))
                parseExp();
            expect(TokenClass.SC);
            parseStmt();
        }
        else if (!accept(TokenClass.RBRA, TokenClass.ELSE)){
            parseExp();
            if (!accept(TokenClass.SC)) {
                expect(TokenClass.ASSIGN);
                parseExp();
            }
            // if parsed successfully, repeat
            if (accept(TokenClass.SC)){
                nextToken();
                parseStmt();
            }
        }
    }

    private void parseStmtMust(){
        if (accept(TokenClass.LBRA)) {
            parseBlock();
            parseStmt();
        }
        else if (accept(TokenClass.WHILE)){
            nextToken();
            expect(TokenClass.LPAR);
            parseExp();
            expect(TokenClass.RPAR);
            parseStmtMust();
        }
        else if (accept(TokenClass.IF)){
            nextToken();
            expect(TokenClass.LPAR);
            parseExp();
            expect(TokenClass.RPAR);
            parseStmtMust();
            if (accept(TokenClass.ELSE)){
                nextToken();
                parseStmtMust();
            }
        }
        else if (accept(TokenClass.RETURN)){
            nextToken();
            if (!accept(TokenClass.SC))
                parseExp();
            expect(TokenClass.SC);
            parseStmt();
        }
        else {
            parseExp();
            if (!accept(TokenClass.SC)) {
                expect(TokenClass.ASSIGN);
                parseExp();
            }
            // if parsed successfully, repeat
            if (accept(TokenClass.SC)){
                nextToken();
                parseStmt();
            }
        }

    }

    private void parseExp(){
        parseAnd();
        if (accept(TokenClass.OR)) {
            nextToken();
            parseExp();
        }
    }

    private void parseAnd(){
        parseEqNeq();
        if (accept(TokenClass.AND)) {
            nextToken();
            parseAnd();
        }
    }

    private void parseEqNeq(){
        parseLtGtLeGe();
        if (accept(TokenClass.EQ, TokenClass.NE)) {
            nextToken();
            parseEqNeq();
        }
    }

    private void parseLtGtLeGe(){
        parseAddSub();
        if (accept(TokenClass.LT, TokenClass.GT, TokenClass.LE, TokenClass.GE)) {
            nextToken();
            parseLtGtLeGe();
        }
    }

    private void parseAddSub(){
        parseMulDivRem();
        if (accept(TokenClass.PLUS, TokenClass.MINUS)) {
            nextToken();
            parseAddSub();
        }
    }

    private void parseMulDivRem(){
        parsePUTS();
        if (accept(TokenClass.ASTERIX, TokenClass.DIV, TokenClass.REM)) {
            nextToken();
            parseMulDivRem();
        }
    }

    private void parsePUTS(){
        // sizeof
        if (accept(TokenClass.SIZEOF)) {
            nextToken();
            expect(TokenClass.LPAR);
            parseType();
            expect(TokenClass.RPAR);
        }
        // pointer indirection
        else if (accept(TokenClass.ASTERIX)){
            nextToken();
            parseExp();
        }
        // type casting
        else if (accept(TokenClass.LPAR) && (lookAhead(1).tokenClass == TokenClass.INT || lookAhead(1).tokenClass == TokenClass.CHAR || lookAhead(1).tokenClass == TokenClass.VOID || lookAhead(1).tokenClass == TokenClass.STRUCT)){
            nextToken();
            parseType();
            expect(TokenClass.RPAR);
            parseExp();
        }
        // unary minus
        else if (accept(TokenClass.MINUS)){
            nextToken();
            parseExp();
        }
        else {
            parseFAS();
        }
    }

    private void parseFAS(){
        // function call
        if (accept(TokenClass.IDENTIFIER) && lookAhead(1).tokenClass == TokenClass.LPAR){
            nextToken();
            expect(TokenClass.LPAR);
            if (!accept(TokenClass.RPAR)){
                parseExp();
                while (accept(TokenClass.COMMA)){
                    nextToken();
                    parseExp();
                }
            }
            expect(TokenClass.RPAR);
        }
        else {
            parseRest();
            if (accept(TokenClass.LSBR)){
                nextToken();
                parseExp();
                expect(TokenClass.RSBR);
            }
            else if (accept(TokenClass.DOT)){
                nextToken();
                expect(TokenClass.IDENTIFIER);
            }
        }
    }

    private void parseRest(){
        if (accept(TokenClass.LPAR)) {
            nextToken();
            parseExp();
            expect(TokenClass.RPAR);
        }
        else
            expect(TokenClass.IDENTIFIER, TokenClass.INT_LITERAL, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL);
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

