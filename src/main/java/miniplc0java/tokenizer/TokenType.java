package miniplc0java.tokenizer;

public enum TokenType {
    /** 空 */
    None,
    /** 关键字 */
    FN_KW,
    LET_KW,
    CONST_KW,
    AS_KW,
    WHILE_KW,
    IF_KW,
    ELSE_KW,
    RETURN_KW,
    // 扩展
    BREAK_KW,
    CONTINUE_KW,

    /** 字面量 */
    UINT_LITERAL,
    STRING_LITERAL,
    // 扩展
    DOUBLE_LITERAL,
    CHAR_LITERAL,

    /** 标识符 */
    IDENT,

    /** 运算符 */
    PLUS,
    MINUS,
    MUL,
    DIV,
    ASSIGN,
    EQ,
    NEQ,
    LT,
    GT ,
    LE,
    GE,
    L_PAREN,
    R_PAREN ,
    L_BRACE,
    R_BRACE,
    ARROW ,
    COMMA  ,
    COLON ,
    SEMICOLON ,

    COMMENT,

    /** 类型 */
    INT,
    VOID,
    DOUBLE,
    BOOL,

    /** 文件尾 */
    EOF;

    @Override
    public String toString() {
        switch (this) {
            case None:
                return "NullToken";

            case FN_KW:
                return "FN";
            case LET_KW:
                return "LET";
            case CONST_KW:
                return "CONST";
            case AS_KW:
                return "AS";
            case WHILE_KW:
                return "WHILE";
            case IF_KW:
                return "IF";
            case ELSE_KW:
                return "ELSE";
            case RETURN_KW:
                return "RETURN";
            case BREAK_KW:
                return "BREAK";
            case CONTINUE_KW:
                return "CONTINUE";
            case UINT_LITERAL:
                return "UINT";
            case STRING_LITERAL:
                return "STRING";
            case DOUBLE_LITERAL:
                return "DOUBLE";
            case CHAR_LITERAL:
                return "CHAR";

            case IDENT:
                return "IDENT";

            case PLUS:
                return "PLUS";
            case MINUS:
                return "MINUS";
            case MUL:
                return "MUL";
            case DIV:
                return "DIV";
            case ASSIGN:
                return "ASSIGN";
            case EQ:
                return "EQ";
            case NEQ:
                return "NEQ";
            case LT:
                return "LT";
            case GT:
                return "GT";
            case LE:
                return "LE";
            case GE:
                return "GE";
            case L_PAREN:
                return "LPAREN";
            case R_PAREN:
                return "RPAREN";
            case L_BRACE:
                return "LBRACE";
            case R_BRACE:
                return "RBRACE";
            case ARROW:
                return "ARROW";
            case COMMA:
                return "COMMA";
            case COLON:
                return "COLON";
            case SEMICOLON:
                return "SEMICOLON";
            case COMMENT:
                return "COMMENT";
            case INT:
                return "INT";
            case VOID:
                return "VOID";
            case DOUBLE:
                return "DOUBLE";
            case BOOL:
                return "BOOL";
            case EOF:
                return "EOF";
            default:
                return "InvalidToken";
        }
    }
}
