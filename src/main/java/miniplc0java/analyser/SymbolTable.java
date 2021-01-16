package miniplc0java.analyser;

public class SymbolTable {
    String name;
    String type;
    int level;
    long offset;
    int isConstant;    //常量
    int isInitialized; //初始化
    String value; // 存字符串值
    int size;

    public SymbolTable(int isConstant, int isInitialized, String name, int level, String type, long offset) {
        this.name = name;
        this.type = type;
        this.level = level;
        this.offset = offset;
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
    }


    public SymbolTable(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public SymbolTable(String name, int isConstant, String value) {
        this.name = name;
        this.isConstant = isConstant;
        this.value = value;
        this.size = this.value.length();
    }

    public SymbolTable(String name, int isConstant) {
        this.name = name;
        this.isConstant = isConstant;
        this.size = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public int isConstant() {
        return isConstant;
    }

    public void setConstant(int constant) {
        isConstant = constant;
    }

    public int isInitialized() {
        return isInitialized;
    }

    public void setInitialized(int initialized) {
        isInitialized = initialized;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
