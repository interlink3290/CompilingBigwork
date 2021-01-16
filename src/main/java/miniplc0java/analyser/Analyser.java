package miniplc0java.analyser;

import miniplc0java.error.*;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;

    /** 指令集,普通和全局 */
    List<Instruction> instructions;
    List<Instruction> globalInstructions = new ArrayList<>() ;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    int globalOffset = 0;
    List<SymbolTable> globalTable = new ArrayList<>(); // 全局
    HashMap<String, FunctionTable> functionTable = new HashMap<>(); // 函数
    List<SymbolTable> symbolTable = new ArrayList<>(); // 函数的局部符号表

    /** 函数参数 */
    Boolean isVoid =false;      // 是否为空
    Boolean isReturn = false; // 有无返回


    int functionId = 1;         // 函数id
    int paramOffset = 0;        // 参数偏移
    String returnType = "";     // 返回类型
    List<SymbolTable> params= new ArrayList<>(); // 参数列表
    int loc_slots = 0; //局部变量大小
    int return_slots = 0; // 返回值参数

    /** 算符优先分析矩阵 */
    // + - * / ( ) < > <= >= == !=
    int[][] priority = {
            {1,1,-1,-1,-1,1,1,1,1,1,1,1},
            {1,1,-1,-1,-1,1,1,1,1,1,1,1},
            {1,1,1,1,-1,1,1,1,1,1,1,1},
            {1,1,1,1,-1,1,1,1,1,1,1,1},
            {-1,-1,-1,-1,-1,2,-1,-1,-1,-1,-1,-1},
            {-1,-1,-1,-1,0,0,-1,-1,-1,-1,-1,-1},
            {-1,-1,-1,-1,-1,1,1,1,1,1,1,1},
            {-1,-1,-1,-1,-1,1,1,1,1,1,1,1},
            {-1,-1,-1,-1,-1,1,1,1,1,1,1,1},
            {-1,-1,-1,-1,-1,1,1,1,1,1,1,1},
            {-1,-1,-1,-1,-1,1,1,1,1,1,1,1},
            {-1,-1,-1,-1,-1,1,1,1,1,1,1,1},
    };
    Stack<TokenType> opaStack = new Stack<>();

    /** 层次 */
    int level = 0;


    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
    }

    /**
     * 查看下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            TokenType xx = token.getTokenType();
            throw new ExpectedTokenError(tt, token);
        }
    }

    // 分析主程序
    private void analyseProgram() throws CompileError {
        while(check(TokenType.FN_KW)||check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            Token var = peek();
            if(check(TokenType.FN_KW)){
                analyseFunction();
            }else if(check(TokenType.LET_KW) || check(TokenType.CONST_KW)){
                analyseDeclStmt();
            }else{
                throw new AnalyzeError(ErrorCode.InvalidInput, var.getStartPos());
            }
        }

        FunctionTable main = functionTable.get("main");
        if(main == null){
            throw new AnalyzeError(ErrorCode.InvalidInput);
        }

        globalTable.add(new SymbolTable("_start", 1, "_start"));
        Instruction tmp = new Instruction(Operation.stackalloc, 0,4);
        globalInstructions.add(tmp);
        if(main.getFunType().equals("int") || main.getFunType().equals("double")){
            tmp.setX(1);
            globalInstructions.add(new Instruction(Operation.call, functionId - 1,4));
            globalInstructions.add(new Instruction(Operation.popn, 1,4));
        }else{
            globalInstructions.add(new Instruction(Operation.call, functionId - 1,4));
        }
        functionTable.put("_start", new FunctionTable("_start","void",0, null,globalOffset,0, 0, 0, 0, globalInstructions));
        globalOffset++;
    }

    private void analyseFunction() throws CompileError {
        initializeFuncVar();
        expect(TokenType.FN_KW);
        Token tmp = expect(TokenType.IDENT);
        expect(TokenType.L_PAREN);

        if(check(TokenType.CONST_KW) || check(TokenType.IDENT)){
            params = analyseFunctionParamList();
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);

        String type = analyseTy();
        if(type.equals("void")) {
            isVoid = true;
        }
        if(!isVoid){
            return_slots = 1;
            paramOffset = 1;
        } else {
            return_slots = 0;
        }

        if (repeatFunction(globalTable, tmp.getValueString())){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, tmp.getStartPos());
        }
        FunctionTable function = new FunctionTable(tmp.getValueString(), type, params.size(), params, 0, functionId, return_slots, params.size(), loc_slots, instructions);
        functionTable.put(function.getFunName(), function);

        analyseBlockStmt(false, 0 ,type);

        if((!returnType.equals(type)) || isVoid&&isReturn || ((!isVoid)&&(!isReturn)) ){
            throw new AnalyzeError(ErrorCode.InvalidReturn, tmp.getEndPos());
        }
        if (type.equals("void")) {
            instructions.add(new Instruction(Operation.ret));
        }
        function.setOffset(globalOffset);
        function.setLoc_slots(loc_slots);
        function.setInstructions(instructions);

        addToGlobalTable(globalTable, 1, tmp.getValueString());

        globalOffset++;
        functionId++;
        initializeFuncVar();
    }

    private List<SymbolTable> analyseFunctionParamList() throws CompileError {
        List<SymbolTable> paramList = new ArrayList<>();
        paramList.add(analyseFunctionParam());
        while(check(TokenType.COMMA)){
            Token var = expect(TokenType.COMMA);
            SymbolTable param = analyseFunctionParam();
            //判断变量是否重复
            if(repeatParam(paramList, param)){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, var.getEndPos());
            }else {
                paramList.add(param);
            }
        }
        return paramList;

    }

    private SymbolTable analyseFunctionParam() throws CompileError {
        if(check(TokenType.CONST_KW)){
            expect(TokenType.CONST_KW);
        }
        Token ident = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        String ty = analyseTy();
        return new SymbolTable(ident.getValueString(), ty);
    }


    /**
     * expr ->
     *       operator_expr
     *     | negate_expr
     *     | assign_expr
     *     | as_expr
     *     | call_expr
     *     | literal_expr
     *     | ident_expr
     *     | group_expr
     * @return
     */

    private String analyseExpression() throws CompileError {
        String type = "";
        if(check(TokenType.MINUS)){
            type = analyseNegateExpr();
        } else if (check(TokenType.IDENT)){
            type = analyseIdent();
        } else if (check(TokenType.L_PAREN)){
            type = analyseGroupExpr();
        } else if (check(TokenType.UINT_LITERAL) || check(TokenType.STRING_LITERAL) ){
            type = analyseLiteralExpr();
        }
        // 执行算符优先分析
        analysePriority(type);
        return type;
    }

    /**
     * operator_expr -> expr binary_operator expr
     * @throws TokenizeError
     */
    private void analysePriority(String type) throws CompileError {
        // 是运算符，就继续分析
        while(check(TokenType.PLUS) || check(TokenType.MINUS)
                || check(TokenType.MUL) || check(TokenType.DIV)
                || check(TokenType.ASSIGN) || check(TokenType.EQ)
                || check(TokenType.NEQ)
                || check(TokenType.LT) || check(TokenType.GT)
                ||check(TokenType.LE) || check(TokenType.GE)
                || check(TokenType.AS_KW)){

            if(check(TokenType.PLUS) || check(TokenType.MINUS)
                    || check(TokenType.MUL) || check(TokenType.DIV)
                    || check(TokenType.ASSIGN) || check(TokenType.EQ)
                    || check(TokenType.NEQ)
                    || check(TokenType.LT) || check(TokenType.GT)
                    ||check(TokenType.LE) || check(TokenType.GE) ){
                Token tmp = next();

                if (!opaStack.empty()) {
                    int front = getIndexByType(opaStack.peek());
                    int next = getIndexByType(tmp.getTokenType());
                    if (priority[front][next] > 0) {
                            Instruction.addInstruction(opaStack.pop(), instructions);
                    }
                }
                opaStack.push(tmp.getTokenType());
                String type2 = analyseExpression();
                if (!type.equals(type2)){
                    throw new AnalyzeError(ErrorCode.InvalidExpression, tmp.getStartPos());
                }
            }
            else {
                Token tmp=peek();

                if(!type.equals("int")&&!type.equals("double")){
                    throw new AnalyzeError(ErrorCode.InvalidExpression, tmp.getStartPos());
                }
                expect(TokenType.AS_KW);
                String Type2 = analyseTy();

                if(!type.equals(Type2)){

                    if(type.equals("int")){
                        if(level == 0){
                           globalInstructions.add(new Instruction(Operation.itof));
                        }else{
                            instructions.add(new Instruction(Operation.itof));
                        }
                        type = "double";
                    }

                    else{
                        if(level == 0){
                            globalInstructions.add(new Instruction(Operation.ftoi));
                        }else{
                            instructions.add(new Instruction(Operation.ftoi));
                        }
                        type = "int";
                    }
                }
            }
        }
    }

    /**
     * negate_expr -> '-' expr
     *
     * @return
     */
    private String analyseNegateExpr() throws CompileError {
        expect(TokenType.MINUS);
        String type = "";
        Token tmp = peek();
        type = analyseExpression();
        if(type.equalsIgnoreCase("int")) {
            if(level==0){
                globalInstructions.add(new Instruction(Operation.neg_i));
            } else {
                instructions.add(new Instruction(Operation.neg_i));
            }
        } else {
            throw new AnalyzeError(ErrorCode.IncompleteExpression, peek().getStartPos());
        }
        return type;
    }

    /**
     * assign_expr -> IDENT '=' expr
     *
     * call_param_list -> expr (',' expr)*
     * call_expr -> IDENT '(' call_param_list? ')'
     *
     * ident_expr -> IDENT
     *
     * @return
     */
    private String analyseIdent() throws CompileError {
        String type = "";
        Token tmp = expect(TokenType.IDENT);
        if(check(TokenType.ASSIGN)) {
            // assign_expr
            type = analyseAssignExpr(tmp);
        }
        else if (check(TokenType.L_PAREN) ){
            // call_expr
            type = analyseCallExpr(tmp);
        }
        else {
            // ident_expr
            SymbolTable symbol = canUse(symbolTable, level, tmp.getValueString());
            SymbolTable param = isParameter(params, tmp.getValueString());
            if (symbol==null && param==null)
                throw new AnalyzeError(ErrorCode.NotDeclared);
            Instruction instruction;

            long id;
            if (param!=null){
                id = getParamOffset(param.getName(), params);
                instruction = new Instruction(Operation.arga, paramOffset + id,4);
                instructions.add(instruction);
                type = param.getType();
            }
            else {
                if(symbol.getLevel()>0){
                    id = symbol.getOffset();
                    instruction = new Instruction(Operation.loca, id,4);
                    instructions.add(instruction);
                }
                /* 局部 */
                else {
                    id = symbol.getOffset();
                    instruction = new Instruction(Operation.globa, id,4);
                    instructions.add(instruction);
                }
                type = symbol.getType();
            }
            instructions.add(new Instruction(Operation.load64));
        }
        return type;
    }



    private String analyseAssignExpr(Token tmp) throws CompileError {
        expect(TokenType.ASSIGN);
        String type = "";
        // 符号定义过 或者 符号是函数参数
        SymbolTable symbol = canUse(symbolTable,level,tmp.getValueString());
        // 函数列表里找
        SymbolTable param = isParameter(params, tmp.getValueString());
        if(symbol == null && param == null){
            throw new AnalyzeError(ErrorCode.NotDeclared, tmp.getStartPos());
        }
        else {
            if(symbol!=null&&symbol.getLevel()>0){
                type = symbol.getType();
                if(type.equals("void")) {
                    throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
                }
                else if(symbol.isConstant() == 1) {
                    throw new AnalyzeError(ErrorCode.AssignToConstant, tmp.getStartPos());
                }
                else {
                    instructions.add(new Instruction(Operation.loca, symbol.getOffset(), 4));
                }
            }
            else if(param!=null){
                type = param.getType();
                if (type.equals("void")) {
                    throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
                }
                int offset = getParamOffset(param.getName(), params);
                instructions.add(new Instruction(Operation.arga,paramOffset+offset, 4));
            }
            else if(symbol.getLevel() == 0){
                type = symbol.getType();
                if (type.equals("void")) {
                    throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
                }
                else if (symbol.isConstant() == 1) {
                    throw new AnalyzeError(ErrorCode.AssignToConstant, tmp.getStartPos());
                }
                else {
                    instructions.add(new Instruction(Operation.globa, symbol.getOffset(), 4));
                }
            }
        }
        String type2 = "";
        if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_PAREN)||check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)) {
            type2 = analyseExpression();
        }
        if(!type.equals(type2)){
            throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
        }
        while(!opaStack.empty()) {
            Instruction.addInstruction(opaStack.pop(), instructions);
        }
        instructions.add(new Instruction(Operation.store64));
        type = "void";
        return type;
    }

    private String analyseCallExpr(Token tmp) throws CompileError {
        expect(TokenType.L_PAREN);
        String type = "";
        opaStack.push(TokenType.L_PAREN);

        FunctionTable function = functionTable.get(tmp.getValueString());
        Instruction instruction;

        if(function!=null ||  isLibraryFunction(tmp.getValueString())){
            int offset;

            if(isLibraryFunction(tmp.getValueString())){
                offset = globalOffset;
                globalTable.add(new SymbolTable(tmp.getValueString(),1, tmp.getValueString()));
                globalOffset++;
                instruction = new Instruction(Operation.callname, offset, 4);

                type = typeReturnOfLibrary(tmp.getValueString());
            }
            else{
                offset = function.getFunId();
                instruction = new Instruction(Operation.call,offset, 4);

                type = function.getFunType();

            }
        } else {
            throw new AnalyzeError(ErrorCode.NotDeclared, tmp.getStartPos());
        }

        if(hasReturn(tmp.getValueString(), functionTable)){
            instructions.add(new Instruction(Operation.stackalloc,1, 4));
        }else{
            instructions.add(new Instruction(Operation.stackalloc,0, 4));
        }

        if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_PAREN)||check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)){
            analyseCallParamList(tmp.getValueString());
        }
        expect(TokenType.R_PAREN);

        while (opaStack.peek() != TokenType.L_PAREN) {
            TokenType tokenType = opaStack.pop();
            Instruction.addInstruction(tokenType, instructions);
        }
        opaStack.pop();

        instructions.add(instruction);

        return type;
    }

    private int analyseCallParamList(String name) throws CompileError{
        List<String> TypeList = new ArrayList<>();
        int count = 0;
        String type = analyseExpression();
        TypeList.add(type);
        while (!opaStack.empty() && opaStack.peek() != TokenType.L_PAREN) {
            Instruction.addInstruction(opaStack.pop(), instructions);
        }
        count++;

        while(nextIf(TokenType.COMMA)!=null){
            type = analyseExpression();
            TypeList.add(type);
            while (!opaStack.empty() && opaStack.peek() != TokenType.L_PAREN) {
                Instruction.addInstruction(opaStack.pop(), instructions);
            }
            count++;
        }

        List<String> ParamTypeList = typeReturn(name, functionTable);
        if(ParamTypeList.size()==TypeList.size()){
            for (int i=0 ;i<TypeList.size();i++){
                if(!TypeList.get(i).equals(ParamTypeList.get(i))){
                    throw new AnalyzeError(ErrorCode.WrongParam);
                }
            }
        }else{
            throw new AnalyzeError(ErrorCode.WrongParam);
        }

        return count;
    }

    /**
     * group_expr -> '(' expr ')'
     * @return
     * @throws CompileError
     */
    private String analyseGroupExpr() throws CompileError {
        String type = "";
        expect(TokenType.L_PAREN);
        opaStack.push(TokenType.L_PAREN); // 左括号 入栈
        type = analyseExpression();
        expect(TokenType.R_PAREN);
        // 右括号，出栈直到遇到左括号
        while(opaStack.peek()!=TokenType.L_PAREN){
            Instruction.addInstruction(opaStack.pop(),instructions);
        }
        opaStack.pop();
        return type;
    }

    /**
     *
     * @return
     */
    private String analyseLiteralExpr() throws TokenizeError {
        Token tmp = next();
        String type = "";
        if(tmp.getTokenType()==TokenType.UINT_LITERAL||tmp.getTokenType()==TokenType.CHAR_LITERAL){
            if(level==0){
                globalInstructions.add(new Instruction(Operation.push , (Long) tmp.getValue(), 8));
            }else {
                instructions.add(new Instruction(Operation.push, (Long) tmp.getValue(), 8));
            }
            type = "int";
        } else if (tmp.getTokenType() == TokenType.STRING_LITERAL){
            globalTable.add(new SymbolTable(tmp.getValueString(), 1, tmp.getValueString()));
            if(level==0) {
                globalInstructions.add(new Instruction(Operation.push, globalOffset, 8));
            }else{
                instructions.add(new Instruction(Operation.push, globalOffset, 8));
            }

            globalOffset++;
            type = "int";
        }
        return type;
    }

    private void analyseStmt(Boolean isWhile, int startOfWhile ,String type) throws CompileError {
        Token tmp = peek();
        if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_PAREN)||check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)){
            analyseExpression();
            while(!opaStack.empty()){
                Instruction.addInstruction(opaStack.pop(), instructions);
            }
            expect(TokenType.SEMICOLON);
        } else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            analyseDeclStmt();
        } else if(check(TokenType.IF_KW)){
            analyseIfStmt(isWhile,startOfWhile,type);
        } else if(check(TokenType.WHILE_KW)){
            analyseWhileStmt(type);
        } else if(check(TokenType.BREAK_KW)){
            analyseBreakStmt(isWhile,startOfWhile,type);
        } else if(check(TokenType.CONTINUE_KW)){
            analyseContinueStmt(isWhile,startOfWhile,type);
        } else if(check(TokenType.RETURN_KW)){
            analyseReturnStmt(type);
        } else if(check(TokenType.L_BRACE)){
            analyseBlockStmt(isWhile,startOfWhile,type);
        } else if(check(TokenType.SEMICOLON)){
            expect(TokenType.SEMICOLON);
        } else{
            throw new AnalyzeError(ErrorCode.InvalidInput, tmp.getStartPos());
        }
    }

    private void analyseDeclStmt() throws CompileError {
        if(check(TokenType.LET_KW)){
            expect(TokenType.LET_KW);
            Token tmp = expect(TokenType.IDENT);

            if(symbolDefined(functionTable, symbolTable, level, tmp.getValueString())
                    || isParameter(params, tmp.getValueString()) != null){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, tmp.getStartPos());
            }

            expect(TokenType.COLON);
            String ty = analyseTy();
            if(!ty.equals("int")&&!ty.equals("double")){
                throw new AnalyzeError(ErrorCode.InvalidReturn, tmp.getStartPos());
            }
            if(level==0){
                symbolTable.add(new SymbolTable(0, 0, tmp.getValueString(), level, ty, globalOffset));
                globalTable.add(new SymbolTable(tmp.getValueString(), 0));
            } else {
                symbolTable.add(new SymbolTable(0, 0, tmp.getValueString(), level, ty, loc_slots));
            }

            if(check(TokenType.ASSIGN)){
                expect(TokenType.ASSIGN);
                initializeSymbol(symbolTable, tmp.getValueString(), level, tmp.getStartPos());
                if(level == 0){
                    globalInstructions.add(new Instruction(Operation.globa,globalOffset,4));
                }else{
                    instructions.add(new Instruction(Operation.loca,loc_slots,4));
                }

                String type = analyseExpression();
                if(!type.equals(ty)){
                    throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
                }
                while(!opaStack.empty()){
                    Instruction.addInstruction(opaStack.pop(), instructions);
                }
                if(level==0) {
                    globalInstructions.add(new Instruction(Operation.store64));
                }else{
                    instructions.add(new Instruction(Operation.store64));
                }
            }

            expect(TokenType.SEMICOLON);
        }
        else if(check(TokenType.CONST_KW)){
            expect(TokenType.CONST_KW);
            Token tmp = expect(TokenType.IDENT);

            if(symbolDefined(functionTable, symbolTable, level, tmp.getValueString())){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, tmp.getStartPos());
            }

            expect(TokenType.COLON);
            String ty = analyseTy();

            expect(TokenType.ASSIGN);

            if(level==0){
                symbolTable.add(new SymbolTable(1, 1, tmp.getValueString(), level, ty, globalOffset));
                globalTable.add(new SymbolTable(tmp.getValueString(),1));
                globalInstructions.add(new Instruction(Operation.globa,globalOffset,4));
            } else {
                symbolTable.add(new SymbolTable(1, 1, tmp.getValueString(), level, ty, loc_slots));
                instructions.add(new Instruction(Operation.loca,loc_slots,4));
            }

            String type = analyseExpression();

            if(!type.equals(ty)){
                throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
            }
            while(!opaStack.empty()){
                Instruction.addInstruction(opaStack.pop(), instructions);
            }

            if(level==0) {
                globalInstructions.add(new Instruction(Operation.store64));
            }else{
                instructions.add(new Instruction(Operation.store64));
            }
            expect(TokenType.SEMICOLON);
        }
        if(level == 0){
            globalOffset++;
        } else {
            loc_slots++;
        }
    }

    private void analyseIfStmt(Boolean isWhile, int startOfWhile ,String type) throws CompileError {
        expect(TokenType.IF_KW);
        analyseExpression();
        while(!opaStack.empty()){
            Instruction.addInstruction(opaStack.pop(), instructions);
        }

        instructions.add(new Instruction(Operation.br_true,1,4));

        Instruction jump_ifInstruction = new Instruction(Operation.br, 0,4);
        instructions.add(jump_ifInstruction);
        int if_begin = instructions.size();

        analyseBlockStmt(isWhile, startOfWhile, type);

        Instruction jump_elseInstruction = new Instruction(Operation.br, 0,4);
        instructions.add(jump_elseInstruction);
        int else_start = instructions.size();

        int jump = instructions.size() - if_begin;
        jump_ifInstruction.setX(jump);

        if (check( TokenType.ELSE_KW)) {
            expect(TokenType.ELSE_KW);
            if (check(TokenType.IF_KW)){
                analyseIfStmt(isWhile, startOfWhile, type);
                instructions.add(new Instruction(Operation.br,0,4));
            }
            else {
                analyseBlockStmt(isWhile, startOfWhile, type);
                instructions.add(new Instruction(Operation.br,0,4));
            }
        }

        jump = instructions.size() - else_start;
        jump_elseInstruction.setX(jump);

    }

    private void analyseWhileStmt(String type) throws CompileError {
        expect(TokenType.WHILE_KW);

        int size_begin = instructions.size();

        analyseExpression();
        while(!opaStack.empty()){
            Instruction.addInstruction(opaStack.pop(), instructions);
        }

        instructions.add(new Instruction(Operation.br_true,1,4));
        Instruction br = new Instruction(Operation.br,0,4);
        instructions.add(br);

        int size_running = instructions.size();
        boolean iswhile = true;

        analyseBlockStmt(iswhile, size_begin, type);

        Instruction goBack = new Instruction(Operation.br, 0,4);
        instructions.add(goBack);

        int size_after = instructions.size();

        goBack.setX(size_begin - size_after);
        br.setX(size_after - size_running);
    }


    private void analyseBreakStmt(Boolean isWhile, int startOfWhile ,String type) throws CompileError {
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
        if(isWhile) {
            int nowOffset = instructions.size();
            instructions.add(new Instruction(Operation.br, -(nowOffset - startOfWhile), 4));
        }else{
            throw new AnalyzeError(ErrorCode.InvalidExpression);
        }
    }


    private void analyseContinueStmt(Boolean isWhile, int startOfWhile ,String type) throws CompileError {
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
        if(isWhile){
            int nowOffset = instructions.size();
            instructions.add(new Instruction(Operation.br,-(nowOffset-startOfWhile-1),4));
        }else{
            throw new AnalyzeError(ErrorCode.InvalidExpression);
        }
    }

    private void analyseReturnStmt(String type) throws CompileError {
        expect(TokenType.RETURN_KW);
        Token tmp = peek();

        if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_PAREN)
                ||check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)){
            if((type.equals("int")||type.equals("double"))){
                instructions.add(new Instruction(Operation.arga,0,4));
                returnType = analyseExpression();
                if(!type.equals(returnType)){
                    throw new AnalyzeError(ErrorCode.InvalidReturn, tmp.getStartPos());
                }
                while (!opaStack.empty()) {
                    Instruction.addInstruction(opaStack.pop(), instructions);
                }
                instructions.add(new Instruction(Operation.store64));
                isReturn = true ;
            } else {
                throw new AnalyzeError(ErrorCode.InvalidReturn, tmp.getStartPos());
            }
        }
        expect(TokenType.SEMICOLON);
        while(!opaStack.empty()){
            Instruction.addInstruction(opaStack.pop(), instructions);
        }
        instructions.add(new Instruction(Operation.ret));
    }

    private void  analyseBlockStmt(Boolean isWhile, int startOfWhile ,String type) throws CompileError {
        expect(TokenType.L_BRACE);
        level++;
        while(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_PAREN)
                ||check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)
                ||check(TokenType.LET_KW)||check(TokenType.CONST_KW)
                ||check(TokenType.IF_KW)||check(TokenType.WHILE_KW)
                ||check(TokenType.BREAK_KW)||check(TokenType.CONTINUE_KW)
                ||check(TokenType.RETURN_KW)||check(TokenType.L_BRACE)||
                check(TokenType.SEMICOLON)){
            analyseStmt(isWhile,startOfWhile,type);
        }
        expect(TokenType.R_BRACE);
        // 清除该层的变量
        symbolTable.removeIf(tmp -> tmp.getLevel() == level);

        level--;
    }

    private String analyseTy() throws CompileError{
        Token tmp = expect(TokenType.IDENT);
        if (tmp.getValue().equals("int")||tmp.getValue().equals("void")||tmp.getValue().equals("double")){
            return tmp.getValue().toString();
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput, tmp.getStartPos());
        }
    }


    public static int getIndexByType(TokenType tokenType){
        if(tokenType== TokenType.PLUS){
            return 0;
        } else if (tokenType== TokenType.MINUS){
            return 1;
        } else if (tokenType== TokenType.MUL){
            return 2;
        } else if (tokenType== TokenType.DIV){
            return 3;
        } else if (tokenType== TokenType.L_PAREN){
            return 4;
        } else if (tokenType== TokenType.R_PAREN){
            return 5;
        } else if  (tokenType== TokenType.LT){
            return 6;
        } else if (tokenType== TokenType.GT){
            return 7;
        } else if (tokenType== TokenType.LE){
            return 8;
        } else if (tokenType== TokenType.GE){
            return 9;
        } else if (tokenType== TokenType.EQ){
            return 10;
        } else if (tokenType== TokenType.NEQ){
            return 11;
        }
        return -1;
    }

    public static SymbolTable canUse (List<SymbolTable>symbolTable, int level, String name) {
        for(int i=level;i>=0;i--){
            for (SymbolTable symbol : symbolTable) {
                if (symbol.getLevel()==i && symbol.getName().equals(name)) {
                    return symbol;
                }
            }
        }
        return null;
    }

    public static SymbolTable isParameter(List<SymbolTable> parameters , String name){
        for(SymbolTable parameter:parameters){
            if(parameter.getName().equals(name)){
                return parameter;
            }
        }
        return null;
    }

    public static int getParamOffset(String name, List<SymbolTable> params) {
        for (int i = 0; i < params.size(); ++i) {
            if (params.get(i).getName().equals(name))
                return i;
        }
        return -1;
    }

    public static boolean hasReturn(String name, HashMap<String,FunctionTable>FunctionTable ){
        FunctionTable functionDef = FunctionTable.get(name);
        if (name.equals("getint") || name.equals("getdouble") || name.equals("getchar")) {
            return true;
        }else if(functionDef!=null){
            return functionDef.getFunType().equals("int") || functionDef.getFunType().equals("double");
        }

        return false;
    }

    // 判断函数是不是库函数
    public static boolean isLibraryFunction(String name) {
        if (name.equals("getint") || name.equals("getdouble") || name.equals("getchar") ||
                name.equals("putint") || name.equals("putdouble") || name.equals("putchar") ||
                name.equals("putstr") || name.equals("putln"))
            return true;
        return false;
    }

    public static String typeReturnOfLibrary(String name){
        if (name.equals("getint") || name.equals("getchar")) {
            return "int";
        }else if(name.equals("getdouble")){
            return "double";
        }else if(name.equals("putint")||name.equals("putdouble")||
                name.equals("putchar")||name.equals("putstr")||
                name.equals("putln")){
            return "void";
        }
        return null;
    }

    public static List<String> typeReturn(String name, HashMap<String,FunctionTable>FunctionTable ){
        List<String> TypeList = new ArrayList<>();
        FunctionTable function = FunctionTable.get(name);
        if (name.equals("putint") || name.equals("putchar") || name.equals("putstr")) {
            TypeList.add("int");
            return TypeList;
        }else if(name.equals("putdouble")){
            TypeList.add("double");
            return TypeList;
        }

        if(function!=null){
            List<SymbolTable> parameters = function.getParameters();
            for (SymbolTable parameter : parameters) {
                TypeList.add(parameter.getType());
            }
            return TypeList;
        }
        return TypeList;
    }

    public static boolean symbolDefined (HashMap<String, FunctionTable> FunctionTable, List<SymbolTable> symbolTable, int level, String name){
        if(level == 0 && FunctionTable.get(name)!=null){
            return true;
        }
        for (SymbolTable symbol : symbolTable) {
            if (symbol.getLevel() == level && symbol.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static void initializeSymbol(List<SymbolTable> symbolTable , String name, int level, Pos curPos) throws AnalyzeError {
        SymbolTable s = canUse(symbolTable, level, name);
        if (s == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            s.setInitialized(1);
        }
    }

    private void initializeFuncVar() {
        isVoid = false;
        isReturn = false;
        returnType = "void";
        instructions = new ArrayList<>();
        paramOffset = 0;
        loc_slots = 0;
        return_slots = 0;
        params = new ArrayList<>();
    }

    public static boolean repeatParam(List<SymbolTable> parameters, SymbolTable param){
        for (SymbolTable parameter : parameters) {
            if (parameter.getName().equals(param.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean repeatFunction(List<SymbolTable> GlobalTable, String name){
        for(SymbolTable global : GlobalTable){
            if(global.getName().equals(name)){
                return true;
            }
        }
        return isLibraryFunction(name);
    }

    public static void addToGlobalTable(List<SymbolTable> GlobalTable
            , int isConst ,String name) throws AnalyzeError {
        for(SymbolTable global:GlobalTable){
            if(global.getName().equals(name)){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration);
            }
        }
        GlobalTable.add(new SymbolTable(name, isConst, name));
    }

    public List<SymbolTable> getGlobalTable() {
        return globalTable;
    }

    public HashMap<String, FunctionTable> getFunctionTable() {
        return functionTable;
    }
}
