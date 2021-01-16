package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;

import java.util.List;

// 函数符号表
public class FunctionTable {
    // 函数自己的值
    String funName;
    String funType;
    int paramCount;
    List<SymbolTable> parameters;

    // 机器码需要的
    int offset;
    int funId;
    int ret_slots;
    int param_slots;
    int loc_slots;
    List<Instruction> instructions;


    public FunctionTable(String funName,
            String funType,
            int paramCount,
            List<SymbolTable> parameters,
            int offset,
            int funId,
            int ret_slots,
            int param_slots,
            int loc_slots,
            List<Instruction> instructions) {
        this.funName = funName;
        this.funType = funType;
        this.paramCount = paramCount;
        this.parameters = parameters;

        // 机器码需要的
        this.offset = offset;
        this.funId = funId;
        this.ret_slots = ret_slots;
        this.param_slots = param_slots;
        this.loc_slots = loc_slots;
        this.instructions = instructions;
    }

    public String getFunName() {
        return funName;
    }

    public void setFunName(String funName) {
        this.funName = funName;
    }

    public String getFunType() {
        return funType;
    }

    public void setFunType(String funType) {
        this.funType = funType;
    }

    public int getParamCount() {
        return paramCount;
    }

    public void setParamCount(int paramCount) {
        this.paramCount = paramCount;
    }

    public List<SymbolTable> getParameters() {
        return parameters;
    }

    public void setParameters(List<SymbolTable> parameters) {
        this.parameters = parameters;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getFunId() {
        return funId;
    }

    public void setFunId(int funId) {
        this.funId = funId;
    }

    public int getRet_slots() {
        return ret_slots;
    }

    public void setRet_slots(int ret_slots) {
        this.ret_slots = ret_slots;
    }

    public int getParam_slots() {
        return param_slots;
    }

    public void setParam_slots(int param_slots) {
        this.param_slots = param_slots;
    }

    public int getLoc_slots() {
        return loc_slots;
    }

    public void setLoc_slots(int loc_slots) {
        this.loc_slots = loc_slots;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }
}
