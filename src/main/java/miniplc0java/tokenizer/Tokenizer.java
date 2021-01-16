package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexDigit();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return lexIdentOrKeyword();
        } else if (peek == '\"') {
            return lexString();
        } else {
            Token token = lexOperatorOrUnknown();
            if (token == null) return nextToken();
            return token;
        }
    }

    private Token lexDigit() throws TokenizeError {
        Pos start = it.currentPos();

        String tempToken = "";
        tempToken += it.nextChar();
        while(Character.isDigit(it.peekChar())) {
            tempToken += it.nextChar();
        }

        /*
        if(it.peekChar() == '.'){
            // 小数情况

        }
        */
        Pos end = it.currentPos();
        return new Token(TokenType.UINT_LITERAL, Long.parseLong(tempToken), start, end);
    }


    private Token lexIdentOrKeyword() throws TokenizeError {
        Pos start = it.currentPos();
        String tempToken = "";
        tempToken += it.nextChar();
        while(Character.isAlphabetic(it.peekChar()) || Character.isDigit(it.peekChar()) || it.peekChar() == '_') {
            tempToken += it.nextChar();
        }
        Pos end = it.currentPos();

        if(tempToken.equals("fn"))
            return new Token(TokenType.FN_KW, tempToken, start, end);
        else if(tempToken.equals("let"))
            return new Token(TokenType.LET_KW, tempToken, start, end);
        else if(tempToken.equals("const"))
            return new Token(TokenType.CONST_KW, tempToken, start, end);
        else if(tempToken.equals("as"))
            return new Token(TokenType.AS_KW, tempToken, start, end);
        else if(tempToken.equals("while"))
            return new Token(TokenType.WHILE_KW, tempToken, start, end);
        else if(tempToken.equals("if"))
            return new Token(TokenType.IF_KW, tempToken, start, end);
        else if(tempToken.equals("else"))
            return new Token(TokenType.ELSE_KW, tempToken, start, end);
        else if(tempToken.equals("return"))
            return new Token(TokenType.RETURN_KW, tempToken, start, end);
        else if(tempToken.equals("break"))
            return new Token(TokenType.BREAK_KW, tempToken, start, end);
        else if(tempToken.equals("continue"))
            return new Token(TokenType.CONTINUE_KW, tempToken, start, end);
        else if(tempToken.equals("int"))
            return new Token(TokenType.IDENT, tempToken, start, end);
        else if(tempToken.equals("void"))
            return new Token(TokenType.IDENT, tempToken, start, end);
        else if(tempToken.equals("double"))
            return new Token(TokenType.IDENT, tempToken, start, end);
        else
            return new Token(TokenType.IDENT, tempToken, start, end);
    }

    // 字符串
    private Token lexString() throws TokenizeError {
        Pos start = it.currentPos();

        it.nextChar();

        char nextch;
        String tmpstr = new String();

        while ((nextch = it.peekChar()) != '"') {
            if (it.isEOF()) {
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
            if (nextch == '\\') {
                it.nextChar();
                if ((nextch = it.peekChar()) == '\\') {
                    tmpstr += '\\';
                }
                else if (nextch == '\'') tmpstr += '\'';
                else if (nextch == '\"') tmpstr += '\"';
                else if (nextch == 'n') tmpstr += '\n';
                else if (nextch == 't') tmpstr += '\t';
                else if (nextch == 'r') tmpstr += '\r';
                else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            } else {
                tmpstr += nextch;
            }
            it.nextChar();
        }
        it.nextChar();

        Pos end = it.currentPos();
        return new Token(TokenType.STRING_LITERAL, tmpstr, start, end);
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());

            case '-':
                if (it.peekChar() != '>') {
                    return new Token(TokenType.MINUS, "-", it.previousPos(), it.currentPos());
                } else {
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }

            case '*':
                return new Token(TokenType.MUL, "*", it.previousPos(), it.currentPos());

            case '/':
                if (it.peekChar() == '/') {
                    skipComment();
                    return null;
                } else {
                    return new Token(TokenType.DIV, "/", it.previousPos(), it.currentPos());
                }

            case '=':
                if (it.peekChar() != '=') {
                    return new Token(TokenType.ASSIGN, "=", it.previousPos(), it.currentPos());
                } else {
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());
                }

            case '!':
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                } else {
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }

            case '<':
                if (it.peekChar() != '=') {
                    return new Token(TokenType.LT, "<", it.previousPos(), it.currentPos());
                } else {
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                }

            case '>':
                if (it.peekChar() != '=') {
                    return new Token(TokenType.GT, ">", it.previousPos(), it.currentPos());
                } else {
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());
                }

            case '(':
                return new Token(TokenType.L_PAREN, "(", it.previousPos(), it.currentPos());

            case ')':
                return new Token(TokenType.R_PAREN, ")", it.previousPos(), it.currentPos());

            case '{':
                return new Token(TokenType.L_BRACE, "{", it.previousPos(), it.currentPos());

            case '}':
                return new Token(TokenType.R_BRACE, "}", it.previousPos(), it.currentPos());

            case ',':
                return new Token(TokenType.COMMA, ",", it.previousPos(), it.currentPos());

            case ':':
                return new Token(TokenType.COLON, ":", it.previousPos(), it.currentPos());

            case ';':
                return new Token(TokenType.SEMICOLON, ";", it.previousPos(), it.currentPos());

            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }

    private void skipComment() {
        it.nextChar();
        while (it.peekChar() != '\n') it.nextChar();
    }
}
