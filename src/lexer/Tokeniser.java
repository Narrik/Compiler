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
        System.out.println("Lexing error: unrecognised character (" + c + ") at " + line + ":" + col);
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


        // IDENTIFIERS TYPES AND KEYWORDS
        if (Character.isLetter(c) || c == '_') {
            StringBuilder sb = new StringBuilder(Character.toString(c));

            while (Character.isDigit(scanner.peek()) || Character.isLetter(scanner.peek()) || scanner.peek() == '_') {
                sb.append(scanner.next());
            }

            // TYPES
            if (sb.toString().equals("int"))
                return new Token(TokenClass.INT, line, column);

            if (sb.toString().equals("void"))
                return new Token(TokenClass.VOID, line, column);

            if (sb.toString().equals("char"))
                return new Token(TokenClass.CHAR, line, column);

            // KEYWORDS
            if (sb.toString().equals("if"))
                return new Token(TokenClass.IF, line, column);

            if (sb.toString().equals("else"))
                return new Token(TokenClass.ELSE, line, column);

            if (sb.toString().equals("while"))
                return new Token(TokenClass.WHILE, line, column);

            if (sb.toString().equals("return"))
                return new Token(TokenClass.RETURN, line, column);

            if (sb.toString().equals("struct"))
                return new Token(TokenClass.STRUCT, line, column);

            if (sb.toString().equals("sizeof"))
                return new Token(TokenClass.SIZEOF, line, column);

            return new Token(TokenClass.IDENTIFIER, sb.toString(), line, column);
        }

        // #INCLUDE
        if (c == '#') {
            StringBuilder sb = new StringBuilder(Character.toString(c));
            while (Character.isDigit(scanner.peek()) || Character.isLetter(scanner.peek()) || scanner.peek() == '_') {
                sb.append(scanner.next());
            }
            if (sb.toString().equals("#include"))
                return new Token(TokenClass.INCLUDE, line, column);
        }


        // COMMENTS AND DIV
        if (c == '/') {
            // single line comment
            if (scanner.peek() == '/') {
                // check for end of line
                while (scanner.peek() != '\n' && scanner.peek() != '\r') {
                    scanner.next();
                }
                return next();
            }

            // multiline comment
            if (scanner.peek() == '*') {
                // jump over the '/'
                scanner.next();
                // check for end of file before end of comment
                if (scanner.peek() == '\uFFFF') {
                    error(c, line, column);
                    return new Token(TokenClass.INVALID, line, column);
                }

                // jump over the '*'
                char c1 = scanner.next();
                while (c1 != '*' || scanner.peek() != '/'){
                    // check for end of file before end of comment
                    if (scanner.peek() == '\uFFFF') {
                        error(c, line, column);
                        return new Token(TokenClass.INVALID, line, column);
                    }
                    c1 = scanner.next();
                }
                scanner.next();
                return next();
            }
            // not a comment, therefore div operator
            return new Token(TokenClass.DIV, line, column);
        }

        // LITERALS
        // recognises STRING_LITERAL
        if (c == '"') {
            StringBuilder sb = new StringBuilder();
            while (scanner.peek() != '"') {
                // check for end of line or file
                if (scanner.peek() == '\n' || scanner.peek() == '\r' || scanner.peek() == '\uFFFF') {
                    error(c, line, column);
                    return new Token(TokenClass.INVALID, line, column);
                }
                char c1 = scanner.next();
                // check for escape characters
                if (c1 == '\\') {
                    switch (scanner.peek()) {
                        case 't':
                            sb.append(c1);
                            break;
                        case 'b':
                            sb.append(c1);
                            break;
                        case 'n':
                            sb.append(c1);
                            break;
                        case 'r':
                            sb.append(c1);
                            break;
                        case 'f':
                            sb.append(c1);
                            break;
                        case '\'':
                            sb.append(c1);
                            break;
                        case '"':
                            sb.append(c1);
                            sb.append('"');
                            scanner.next();
                            break;
                        case '\\':
                            sb.append(c1);
                            break;
                        case '0':
                            sb.append(c1);
                            break;
                        default:
                            error(c, line, column);
                            return new Token(TokenClass.INVALID, line, column);
                    }
                } else {
                    sb.append(c1);
                }
            }
            scanner.next();
            return new Token(TokenClass.STRING_LITERAL, sb.toString(), line, column);
        }

        // recognises INT_LITERAL
        if (Character.isDigit(c)) {
            StringBuilder sb = new StringBuilder(Character.toString(c));
            while (Character.isDigit(scanner.peek())) {
                sb.append(scanner.next());
            }
            return new Token(TokenClass.INT_LITERAL, sb.toString(), line, column);
        }

        // recognises CHAR_LITERAL
        if (c == '\'') {
            StringBuilder sb = new StringBuilder();
            // check for end of line or end of file or empty char
            if (scanner.peek() == '\n' || scanner.peek() == '\r' || scanner.peek() == '\uFFFF' || scanner.peek() == '\'') {
                error(c, line, column);
                return new Token(TokenClass.INVALID, line, column);
            }
            char c1 = scanner.next();
            sb.append(c1);
            if (c1 == '\\') {
                switch (scanner.peek()) {
                    case 't':
                        break;
                    case 'b':
                        break;
                    case 'n':
                        break;
                    case 'r':
                        break;
                    case 'f':
                        break;
                    case '\'':
                        break;
                    case '"':
                        break;
                    case '\\':
                        break;
                    case '0':
                        break;
                    default:
                        error(c, line, column);
                        return new Token(TokenClass.INVALID, line, column);
                }
                sb.append(scanner.next());
            }
            if (scanner.peek() == '\'') {
                scanner.next();
                return new Token(TokenClass.CHAR_LITERAL, sb.toString(), line, column);
            }
        }


        // DELIMITERS
        if (c == '{')
            return new Token(TokenClass.LBRA, line, column);

        if (c == '}')
            return new Token(TokenClass.RBRA, line, column);

        if (c == '(')
            return new Token(TokenClass.LPAR, line, column);

        if (c == ')')
            return new Token(TokenClass.RPAR, line, column);

        if (c == '[')
            return new Token(TokenClass.LSBR, line, column);

        if (c == ']')
            return new Token(TokenClass.RSBR, line, column);

        if (c == ';')
            return new Token(TokenClass.SC, line, column);

        if (c == ',')
            return new Token(TokenClass.COMMA, line, column);

        // LOGICAL OPERATORS
        // recognises the and operator
        if (c == '&') {
            if (scanner.peek() == '&') {
                scanner.next();
                return new Token(TokenClass.AND, line, column);
            }
        }

        // recognises the or operator
        if (c == '|') {
            if (scanner.peek() == '|') {
                scanner.next();
                return new Token(TokenClass.OR, line, column);
            }
        }


        // COMPARISONS
        // recognises the equals and assign sign
        if (c == '=') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.EQ, line, column);
            } else {
                return new Token(TokenClass.ASSIGN, line, column);
            }
        }

        // recognises the not equals sign
        if (c == '!') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.NE, line, column);
            }
        }

        // recognises the less than and less than equals sign
        if (c == '<') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.LE, line, column);
            } else {
                return new Token(TokenClass.LT, line, column);
            }
        }

        // recognises the greater than and greater than equals sign
        if (c == '>') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.GE, line, column);
            } else {
                return new Token(TokenClass.GT, line, column);
            }
        }


        // OPERATORS
        if (c == '+')
            return new Token(TokenClass.PLUS, line, column);

        if (c == '-')
            return new Token(TokenClass.MINUS, line, column);

        if (c == '*')
            return new Token(TokenClass.ASTERIX, line, column);

        // div operator in comments

        if (c == '%')
            return new Token(TokenClass.REM, line, column);


        // struct member access
        if (c == '.')
            return new Token(TokenClass.DOT, line, column);


        // if we reach this point, it means we did not recognise a valid token
        error(c, line, column);
        return new Token(TokenClass.INVALID, line, column);
    }

}