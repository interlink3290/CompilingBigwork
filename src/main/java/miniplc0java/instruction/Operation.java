package miniplc0java.instruction;

public enum Operation {
    nop(0x00),
    push(0x01),
    pop(0x02),
    popn(0x03),
    loca(0x0a),
    arga(0x0b),
    globa(0x0c),
    load64(0x13),
    store64(0x17),
    add_i(0x20),
    sub_i(0x21),
    mul_i(0x22),
    div_i(0x23),
    not(0x2e),
    cmp_i(0x30),
    neg_i(0x34),
    set_lt(0x39),
    set_gt(0x3a),
    br(0x41),
    br_false(0x42),
    br_true(0x43),
    call(0x48),
    ret(0x49),
    callname(0x4a),
    stackalloc(0x1a);

    private int value;

    Operation(int i) {
        this.value = i;
    }

    public int getValue(){
        return value;
    }
}
