package lexer;

import lexer.Token.TokenClass;

import java.io.EOFException;
import java.io.IOException;

/**
 * @author cdubach
 */
public class Tokeniser {

    private Scanner scanner;

    private int error = 0;
    public int getErrorCount() {
	return this.error;
    }

    public Tokeniser(Scanner scanner) {
        this.scanner = scanner;
    }

    private void error(char c, int line, int col) {
        System.out.println("Lexing error: unrecognised character ("+c+") at "+line+":"+col);
	error++;
    }


    public Token nextToken() {
        Token result;
        try {
             result = next();
        } catch (EOFException eof) {
            // end of file, nothing to worry about, just return EOF token
            return new Token(TokenClass.EOF, scanner.getLine(), scanner.getColumn());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            // something went horribly wrong, abort
            System.exit(-1);
            return null;
        }
        return result;
    }

    /*
     * To be completed
     */
    private Token next() throws IOException {

        int line = scanner.getLine();
        int column = scanner.getColumn();

        // get the next character
        char c = scanner.next();

        // skip white spaces
        if (Character.isWhitespace(c))
            return next();


        // recognises the assign operator
        if (c == '='){
            char c1 = scanner.peek();
            if (c1 != '=') {
                return new Token(TokenClass.ASSIGN, line, column);
            }
        }

        // recognises tokens starting with a letter
        if (Character.isLetter(c) || c == '_')
            return string_builder(c);


        // DELIMITERS
        // recognises left brace
        if (c == '{')
            return new Token(TokenClass.LBRA, line, column);

        // recognises right brace
        if (c == '}')
            return new Token(TokenClass.RBRA, line, column);

        // recognises left parenthesis
        if (c == '(')
            return new Token(TokenClass.LPAR, line, column);

        // recognises right parenthesis
        if (c == ')')
            return new Token(TokenClass.RPAR, line, column);

        // recognises left square brace
        if (c == '[')
            return new Token(TokenClass.LSBR, line, column);

        // recognises right square brace
        if (c == ']')
            return new Token(TokenClass.RSBR, line, column);

        // recognises semicolon
        if (c == ';')
            return new Token(TokenClass.SC, line, column);

        // recognises comma
        if (c == ',')
            return new Token(TokenClass.COMMA, line, column);

        // LITERALS
        // recognises int literal
        if (Character.isDigit(c))
            return int_builder();


        // LOGICAL OPERATORS
        // recognises the and operator
        if (c == '&'){
            char c1 = scanner.peek();
            if (c1 == '&') {
                scanner.next();
                return new Token(TokenClass.AND, line, column);
            }
        }

        // recognises the or operator
        if (c == '|'){
            char c1 = scanner.peek();
            if (c1 == '|') {
                scanner.next();
                return new Token(TokenClass.OR, line, column);
            }
        }


        // COMPARISIONS
        // recognises the equals sign
        if (c == '='){
            char c1 = scanner.peek();
            if (c1 == '=') {
                scanner.next();
                return new Token(TokenClass.EQ, line, column);
            }
        }

        // recognises the not equals sign
        if (c == '!'){
            char c1 = scanner.peek();
            if (c1 == '=') {
                scanner.next();
                return new Token(TokenClass.NE, line, column);
            }
        }

        // recognises the less than sign
        if (c == '<'){
            char c1 = scanner.peek();
            if (c1 != '=') {
                return new Token(TokenClass.LT, line, column);
            }
        }

        // recognises the greater than sign
        if (c == '>'){
            char c1 = scanner.peek();
            if (c1 != '=') {
                return new Token(TokenClass.GT, line, column);
            }
        }

        // recognises the less than or equals sign
        if (c == '<'){
            char c1 = scanner.peek();
            if (c1 == '=') {
                scanner.next();
                return new Token(TokenClass.LE, line, column);
            }
        }

        // recognises the greater than or equals sign
        if (c == '>'){
            char c1 = scanner.peek();
            if (c1 == '=') {
                scanner.next();
                return new Token(TokenClass.GE, line, column);
            }
        }


        // OPERATORS
        // recognises the plus operator
        if (c == '+')
            return new Token(TokenClass.PLUS, line, column);

        // recognises the minus operator
        if (c == '-')
            return new Token(TokenClass.MINUS, line, column);

        // recognises the asterix operator
        if (c == '*')
            return new Token(TokenClass.ASTERIX, line, column);

        // recognises the div operator
        if (c == '/')
            return new Token(TokenClass.DIV, line, column);

        // recognises the rem operator
        if (c == '%')
            return new Token(TokenClass.REM, line, column);



        // struct member access
        if (c == '.')
            return new Token(TokenClass.DOT, line, column);

        // ... to be completed


        // if we reach this point, it means we did not recognise a valid token
        error(c, line, column);
        return new Token(TokenClass.INVALID, line, column);
    }

    private Token int_builder() throws IOException {
        int line = scanner.getLine();
        int column = scanner.getColumn();
        char c1 = scanner.peek();

        while (Character.isDigit(c1)){
            scanner.next();
        }

        return new Token(TokenClass.INT_LITERAL, line, column);
    }

    private Token string_builder(char c) throws IOException {
        int line = scanner.getLine();
        int column = scanner.getColumn();
        char c1 = scanner.peek();
        StringBuilder sb = new StringBuilder(Character.toString(c));

        while (Character.isDigit(c1) || Character.isLetter(c1) || c1 =='_'){
            sb.append(c1);
            scanner.next();

            // TYPES
            // recognises the int type
            if (sb.toString().equals("int"))
                return new Token(TokenClass.INT, line, column);

            // recognises the void type
            if (sb.toString().equals("void"))
                return new Token(TokenClass.VOID, line, column);

            // recognises the char type
            if (sb.toString().equals("char"))
                return new Token(TokenClass.CHAR, line, column);

            // KEYWORDS
            // recognises the if keyword
            if (sb.toString().equals("if"))
                return new Token(TokenClass.IF, line, column);

            // recognises the else keyword
            if (sb.toString().equals("else"))
                return new Token(TokenClass.ELSE, line, column);

            // recognises the while keyword
            if (sb.toString().equals("while"))
                return new Token(TokenClass.WHILE, line, column);

            // recognises the return keyword
            if (sb.toString().equals("return"))
                return new Token(TokenClass.RETURN, line, column);

            // recognises the struct keyword
            if (sb.toString().equals("struct"))
                return new Token(TokenClass.STRUCT, line, column);

            // recognises the sizeof keyword
            if (sb.toString().equals("sizeof"))
                return new Token(TokenClass.SIZEOF, line, column);


        }

        return new Token(TokenClass.IDENTIFIER, line, column);
    }

}
