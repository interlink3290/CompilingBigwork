package miniplc0java.instruction;

import miniplc0java.tokenizer.TokenType;

import java.util.List;
import java.util.Objects;

public class Instruction {
    private Operation opt;
    long x;
    int bytes; // 字节数

    public Instruction(Operation opt) {
        // 普通指令
        this.opt = opt;
        this.x = 0;
        this.bytes = 0;
    }

    public Instruction(Operation opt, long x, int bytes) {
        // 涉及变量的指令
        this.opt = opt;
        this.x = x;
        this.bytes = bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction that = (Instruction) o;
        return opt == that.opt && Objects.equals(x, that.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, x);
    }

    public Operation getOpt() {
        return opt;
    }

    public void setOpt(Operation opt) {
        this.opt = opt;
    }

    public long getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public int getBytes() {
        return bytes;
    }

    public void setBytes(int bytes) {
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return ""+opt +'(' + x +')'+ '\n';
    }

    // 将运算符转为对应的运算操作，并加入instructions中
    public static void addInstruction(TokenType tokenType,List<Instruction> instructions){
        // + - * / == != < > <= >=
        switch (tokenType) {
            case PLUS:
                instructions.add(new Instruction(Operation.add_i));
                break;
            case MINUS:
                instructions.add(new Instruction(Operation.sub_i));
                break;
            case MUL:
                instructions.add(new Instruction(Operation.mul_i));
                break;
            case DIV:
                instructions.add(new Instruction(Operation.div_i));
                break;
            case EQ:
                instructions.add(new Instruction(Operation.cmp_i));
                instructions.add(new Instruction(Operation.nop));
                break;
            case NEQ:
                instructions.add(new Instruction(Operation.cmp_i));
                break;
            case LT:
                instructions.add(new Instruction(Operation.cmp_i));
                instructions.add(new Instruction(Operation.set_lt));
                break;
            case LE:
                instructions.add(new Instruction(Operation.cmp_i));
                instructions.add(new Instruction(Operation.set_gt));
                instructions.add(new Instruction(Operation.not));
                break;
            case GT:
                instructions.add(new Instruction(Operation.cmp_i));
                instructions.add(new Instruction(Operation.set_gt));
                break;
            case GE:
                instructions.add(new Instruction(Operation.cmp_i));
                instructions.add(new Instruction(Operation.set_lt));
                instructions.add(new Instruction(Operation.not));
                break;
        }
    }
}
