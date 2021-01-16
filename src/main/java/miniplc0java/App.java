package miniplc0java;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import miniplc0java.analyser.Analyser;
import miniplc0java.error.CompileError;
import miniplc0java.generator.Generator;
import miniplc0java.analyser.FunctionTable;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Tokenizer;


public class App {
    public static void main(String[] args) throws CompileError {
        System.out.println(args[0]);
        try{
            InputStream input = new FileInputStream(args[0]);
            Scanner sc = new Scanner(input);
            var iter = new StringIter(sc);
            var tokenizer = new Tokenizer(iter);
            /*var tokens = new ArrayList<Token>();
            try {
                while (true) {
                    var token = tokenizer.nextToken();
                    if (token.getTokenType().equals(TokenType.EOF)) {
                        break;
                    }
                    tokens.add(token);
                }
            } catch (Exception e) {
                // 遇到错误不输出，直接退出
                System.err.println(e);
                System.exit(0);
                return;
            }
            for (Token token : tokens) {
                System.out.println(token.toString());
            }*/

            Analyser analyser = new Analyser(tokenizer);
            analyser.analyse();

            FileOutputStream output = new FileOutputStream(new File(args[1]));
            DataOutputStream out = new DataOutputStream(output);

            Generator generator = new Generator(analyser.getGlobalTable(), new ArrayList<Map.Entry<String, FunctionTable>>(analyser.getFunctionTable().entrySet()));
            byte[] result = generator.getResult();
            output.write(result);

        } catch (Exception e) {
            System.exit(-1);
        }


    }

}
