package miniplc0java.generator;

import miniplc0java.instruction.Instruction;
import miniplc0java.analyser.FunctionTable;
import miniplc0java.analyser.SymbolTable;

import java.util.*;

public class Generator {
    List<SymbolTable> globalTable;
    List<Map.Entry<String, FunctionTable>> functionTable;
    List<Byte> result;

    public Generator (List<SymbolTable> globalTable, List<Map.Entry<String, FunctionTable>> functionTable){
        this.globalTable = globalTable;
        this.functionTable = functionTable;
        this.result = new ArrayList<>();
        result.addAll(numToByte(0x72303b3e,4));
        result.addAll(numToByte(0x00000001,4));
        generateGlabal();
        generateFunction();
    }

    public byte[] getResult(){
        byte[] res = new byte[result.size()];
        for (int i = 0; i < result.size(); ++i) {
            res[i] = result.get(i);
        }
        return res;
    }

    private void generateGlabal() {
        result.addAll(numToByte(globalTable.size(),4));
        for(SymbolTable global : globalTable){
            result.addAll(numToByte(global.isConstant(), 1) );
            if(global.getSize() != 0){
                result.addAll(numToByte(global.getSize(), 4));
                result.addAll(stringToBytes(global.getValue()));
            } else {
                result.addAll(numToByte(8, 4));
                result.addAll(longToByte(0));
            }
        }
    }

    private void generateFunction() {
        result.addAll(numToByte(functionTable.size(),4));
        Collections.sort(functionTable, new Comparator<Map.Entry<String, FunctionTable>>() {
            public int compare(Map.Entry<String, FunctionTable> f1, Map.Entry<String, FunctionTable> f2) {
                return (f1.getValue().getFunId() - f2.getValue().getFunId());
            }
        });

        for(Map.Entry<String, FunctionTable> function : functionTable){
            FunctionTable func = function.getValue();
            result.addAll(numToByte(func.getFunId(), 4));
            result.addAll(numToByte(func.getRet_slots(), 4));
            result.addAll(numToByte(func.getParam_slots(), 4));
            result.addAll(numToByte(func.getLoc_slots(), 4));

            List<Instruction> instructions = func.getInstructions();
            result.addAll(numToByte(instructions.size(), 4));
            for(Instruction instruction : instructions){
                result.addAll(numToByte(instruction.getOpt().getValue(), 1));
                if(instruction.getBytes() == 4){
                    result.addAll(numToByte((int)instruction.getX(), 4));
                }
                else if(instruction.getBytes() == 8){
                    result.addAll(longToByte((long)instruction.getX()));
                }
            }
        }

    }

    private List<Byte> stringToBytes(String name) {
        ArrayList<Byte> bytes = new ArrayList<>();
        for( int i=0; i<name.length(); i++){
            char ch = name.charAt(i);
            bytes.add((byte) (ch & 0xff));
        }
        return bytes;
    }

    private List<Byte> numToByte(int x, int count) {
        ArrayList<Byte> bytes = new ArrayList<>();
        if(count == 1){
            bytes.add((byte) (x & 0xFF));
        }
        else if(count == 4){
            for(int i=0; i<4; i++){
                int tmp = x >> (24 - i*8);
                bytes.add((byte) (tmp & 0xFF));
            }
        }

        return bytes;
    }

    private List<Byte> longToByte(long x) {
        ArrayList<Byte> bytes = new ArrayList<>();
        for(int i=0; i<4; i++) {
            long tmp = x >> (56 - i * 8);
            bytes.add((byte) (tmp & 0xFF));
        }
        return bytes;
    }



}
