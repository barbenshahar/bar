import java.io.File;
import java.util.ArrayList;

public class VMTranslator {
    public static ArrayList<File> getVMFiles(File dir) {
        File[] files = dir.listFiles();
        ArrayList<File> allVMFiles = new ArrayList<File>();
        for (File f:files) {
            if (f.getName().endsWith(".vm")) {
                allVMFiles.add(f);
            }
        }
        return allVMFiles;
    }
    public static void main(String[] args) {
        if (args.length != 1){
            System.out.println("not the right length");
        } 
        else {
            File inFile = new File(args[0]);
            File outFile;
            String pathOfFile = "";
            ArrayList<File> VMFiles = new ArrayList<File>();
            CodeWriter cWriter;
            if (inFile.isFile()) {
                String path = inFile.getAbsolutePath();
                if (!Parser.getExt(path).equals(".vm")) {
                    throw new IllegalArgumentException("not a vm file");
                }
                VMFiles.add(inFile);
                pathOfFile = inFile.getAbsolutePath().substring(0, inFile.getAbsolutePath().lastIndexOf(".")) + ".asm";
            } 

            else if (inFile.isDirectory()) {
                VMFiles = getVMFiles(inFile);
                if (VMFiles.size() == 0) {
                    throw new IllegalArgumentException("this path doesn't contain VM file");
                }
                pathOfFile = inFile.getAbsolutePath() + "/" +  inFile.getName() + ".asm";
            }
            outFile = new File(pathOfFile);
            cWriter = new CodeWriter(outFile);
            cWriter.writeInit();
            for (File f : VMFiles) {
                int type = -1;
                Parser p = new Parser(f);
                cWriter.setFileName(f);
                while (p.hasMoreLines()) {
                    p.advance();
                    type = p.commandType();
                    if (Parser.ARITHMETIC == type) {
                        cWriter.writeArithmetic(p.arg1());
                    } else if (Parser.PUSH == type || Parser.POP == type) {
                        cWriter.writePushPop(type, p.arg1(), p.arg2());
                    }
                    else if (Parser.LABEL == type) {
                        cWriter.writeLabel(p.arg1());
                    } 
                    else if (Parser.GOTO == type) {
                        cWriter.writeGoto(p.arg1());
                    } 
                    else if (Parser.IF == type) {
                        cWriter.writeIf(p.arg1());
                    } 
                    else if (Parser.FUNCTION == type) {
                        cWriter.writeFunction(p.arg1(),p.arg2());
                    } 
                    else if (Parser.CALL == type) {
                        cWriter.writeCall(p.arg1(),p.arg2());
                    }
                    else if (Parser.RETURN == type) {
                        cWriter.writeReturn();
                    } 
                }
            }
            cWriter.close();
            System.out.println(pathOfFile + " is the file");
        }
    }
}

import java.io.File;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Parser {
    private String currCommand;
    private Scanner commands;
    private String argA;
    private int argB;
    private int typeOfArg;
    public static final int ARITHMETIC = 0;
    public static final int PUSH = 1;
    public static final int POP = 2;
    public static final int LABEL = 3;
    public static final int GOTO = 4;
    public static final int IF = 5;
    public static final int FUNCTION = 6;
    public static final int RETURN = 7;
    public static final int CALL = 8;
    public static final ArrayList<String> arthCommands = new ArrayList<String>();
    static {
        arthCommands.add("add");
        arthCommands.add("sub");
        arthCommands.add("or");
        arthCommands.add("and");
        arthCommands.add("eq");
        arthCommands.add("lt");
        arthCommands.add("gt");
        arthCommands.add("neg");
        arthCommands.add("not");
    }

    public Parser(File fileIn) {
        argA = "";
        argB = -1;
        typeOfArg = -1;
        try {
            commands = new Scanner(fileIn);
            String l = "";
            String buildStr = "";
            while(commands.hasNext()){
                l = commentsHandle(commands.nextLine()).trim();
                if (l.length() > 0) {
                    buildStr += l + "\n";
                }
            }
            commands = new Scanner(buildStr.trim());
        } 
        catch (FileNotFoundException e) {
            System.out.println("no file");
        }
    }

    public boolean hasMoreLines() {
       return commands.hasNextLine();
    }

    public void advance(){
        currCommand = commands.nextLine();
        argA = "";
        argB = -1;
        String[] s = currCommand.split(" ");
        if (s.length >= 4) {
            throw new IllegalArgumentException("too many args");
        }
        if (arthCommands.contains(s[0])) {
            argA = s[0];
            typeOfArg = ARITHMETIC; 
        } 
        else if (s[0].equals("return")) {
            argA = s[0];
            typeOfArg = RETURN;
        } 
        else {
            argA = s[1];
            if (s[0].equals("push")) {
                typeOfArg = PUSH;
            } 
            else if (s[0].equals("pop")) {
                typeOfArg = POP;
            } 
            else if (s[0].equals("label")) {
                typeOfArg = LABEL;
            } 
            else if (s[0].equals("goto")) {
                typeOfArg = GOTO;
            } 
            else if (s[0].equals("if-goto")) { ///////
                typeOfArg = IF;
            } 
            else if (s[0].equals("function")) {
                typeOfArg = FUNCTION;
            } 
            else if (s[0].equals("call")) {
                typeOfArg = CALL;
            } 
            else {
                throw new IllegalArgumentException("not one of the require type of command");
            }
            if (typeOfArg == CALL || typeOfArg == FUNCTION || typeOfArg == POP || typeOfArg == PUSH) {
                try {
                    argB = Integer.parseInt(s[2]);
                } 
                catch (Exception e) {
                    throw new IllegalArgumentException("integer require");
                }
            }
        }
    }

    public int commandType() {
        if (typeOfArg != -1) {
            return typeOfArg;
        } 
        else {
            throw new IllegalStateException("no command");
        }
    }

    public static String spacesHandle(String strIn) {
        String str = "";
        if (strIn.length() != 0){
            String[] seg = strIn.split(" ");
            for (String s: seg){
                str += s;
            }
        }
        return str;
    }

    public static String commentsHandle(String strIn) {
        if (strIn.indexOf("//") != -1) {
            strIn = strIn.substring(0, strIn.indexOf("//"));
        }
        return strIn;
    }

    public static String getExt(String fileName) {
        if (fileName.lastIndexOf('.') != -1){
            return fileName.substring(fileName.lastIndexOf('.'));
        } else {
            return "";
        }
    }

    public String arg1() {
        if (commandType() != RETURN){
            return argA;
        } 
        else {
            throw new IllegalStateException("can't get this arg");
        }
    }

    public int arg2() {
        if (commandType() == CALL || commandType() == FUNCTION || commandType() == POP || commandType() == PUSH) {
            return argB;
        } 
        else {
            throw new IllegalStateException("can't get this arg");
        }
    }

}

import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class CodeWriter {
    private PrintWriter printerOut;
    private int jumpArth;
    private static String fName = "";
    private static int lCount = 0;
    private static final Pattern lREG = Pattern.compile("^[^0-9][0-9A-Za-z\\_\\:\\.\\$]+"); 
     
    public CodeWriter(File fileOut) {
        try {
            fName = fileOut.getName(); 
            printerOut = new PrintWriter(fileOut);
            jumpArth = 0;
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void writeArithmetic(String command) {
        if (command.equals("add")) {
            printerOut.print(buildArithmeticA() + "M=M+D\n");
        } 
        else if (command.equals("sub")) {
            printerOut.print(buildArithmeticA() + "M=M-D\n");
        } 
        else if (command.equals("or")) {
            printerOut.print(buildArithmeticA() + "M=M|D\n");
        } 
        else if (command.equals("and")) {
            printerOut.print(buildArithmeticA() + "M=M&D\n");
        } 
        else if (command.equals("eq")) {
            jumpArth += 1;
            printerOut.print(buildArithmeticB("JNE"));
        }
        else if (command.equals("lt")) {
            jumpArth += 1;
            printerOut.print(buildArithmeticB("JGE"));
        } 
        else if (command.equals("gt")) {
            jumpArth += 1;
            printerOut.print(buildArithmeticB("JLE"));
        } 
        else if (command.equals("neg")) {
            printerOut.print("D=0\n@SP\nA=M-1\nM=D-M\n");
        } 
        else if (command.equals("not")) {
            printerOut.print("@SP\nA=M-1\nM=!M\n");
        } 
        else {
            throw new IllegalArgumentException("The func should get an arithmetic command");
        }
    }

    public void writePushPop(int command, String segment, int index){
        if (Parser.PUSH == command) {
            if (segment.equals("constant")) {
                printerOut.print("@" + index + "\n" + "D=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } 
            else if (segment.equals("local")) {
                printerOut.print(buildPushA("LCL",index,false));
            } 
            else if (segment.equals("argument")) {
                printerOut.print(buildPushA("ARG",index,false));
            } 
            else if (segment.equals("this")) {
                printerOut.print(buildPushA("THIS",index,false));
            } 
            else if (segment.equals("that")) {
                printerOut.print(buildPushA("THAT",index,false));
            } 
            else if (segment.equals("temp")) {
                printerOut.print(buildPushA("R5", index + 5,false));
            } 
            else if (segment.equals("pointer") && index == 0) {
                printerOut.print(buildPushA("THIS",index,true));
            } 
            else if (segment.equals("pointer") && index == 1) {
                printerOut.print(buildPushA("THAT",index,true));
            } 
            else if (segment.equals("static")) {
                printerOut.print("@" + fName + index + "\n" + "D=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n"); //
            }
        } 
        else if(Parser.POP == command){
            if (segment.equals("local")) {
                printerOut.print(buildPopA("LCL",index,false));
            } 
            else if (segment.equals("argument")) {
                printerOut.print(buildPopA("ARG",index,false));
            } 
            else if (segment.equals("this")) {
                printerOut.print(buildPopA("THIS",index,false));
            } 
            else if (segment.equals("that")) {
                printerOut.print(buildPopA("THAT",index,false));
            } 
            else if (segment.equals("temp")) {
                printerOut.print(buildPopA("R5", index + 5,false));
            } 
            else if (segment.equals("pointer") && index == 0) {
                printerOut.print(buildPopA("THIS",index,true));
            } 
            else if (segment.equals("pointer") && index == 1) {
                printerOut.print(buildPopA("THAT",index,true));
            } 
            else if (segment.equals("static")) {
                printerOut.print("@" + fName + index + "\nD=A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n"); //
            }
        } else {
            throw new IllegalArgumentException("The func should get a pushpop command");
        }
    }

    public void setFileName(File fileOut) {
        fName = fileOut.getName(); 
    }

    private String buildArithmeticA() {
        return "@SP\n" + "AM=M-1\n" + "D=M\n" + "A=A-1\n";
    }

    private String buildArithmeticB(String type) {
        return "@SP\n" + "AM=M-1\n" + "D=M\n" + "A=A-1\n" + "D=M-D\n" + "@FALSE" + jumpArth + "\n" + "D;" + type + "\n" +
                "@SP\n" + "A=M-1\n" + "M=-1\n" + "@CONTINUE" + jumpArth + "\n" + "0;JMP\n" + "(FALSE" + jumpArth + ")\n" +
                "@SP\n" + "A=M-1\n" + "M=0\n" + "(CONTINUE" + jumpArth + ")\n";
    }

    private String buildPushA(String segment, int index, boolean direct) {
        String str = (direct)? "" : "@" + index + "\n" + "A=D+A\nD=M\n";
        return "@" + segment + "\n" + "D=M\n"+ str + "@SP\n" + "A=M\n" + "M=D\n" + "@SP\n" + "M=M+1\n";
    }

    private String buildPopA(String segment, int index, boolean direct) {
        String str = (direct)? "D=A\n" : "D=M\n@" + index + "\nD=D+A\n";
        return "@" + segment + "\n" + str + "@R13\n" + "M=D\n" + "@SP\n" + "AM=M-1\n" + "D=M\n" + "@R13\n" + "A=M\n" + "M=D\n";
    }

    public void close() {
        printerOut.close();
    }

    public void writeLabel(String l) {
        Matcher mat = lREG.matcher(l);
        if (mat.find()) {
            printerOut.print("(" + l +")\n");
        }
        else {
            throw new IllegalArgumentException("not a right form");
        }
    }

    public void writeGoto(String l) {
        Matcher mat = lREG.matcher(l);
        if (mat.find()) {
            printerOut.print("@" + l +"\n0;JMP\n");
        } 
        else {
            throw new IllegalArgumentException("not a right form");
        }
    }

    public void writeIf(String l) {
        Matcher mat = lREG.matcher(l);
        if (mat.find()) {
            printerOut.print(buildArithmeticA() + "@" + l +"\nD;JNE\n");
        } 
        else {
            throw new IllegalArgumentException("not a right form");
        }
    }

    public void writeFunction(String func, int localsAmount) {
        printerOut.print("(" + func +")\n");
        for (int i = 1; i <= localsAmount; i++) {
            writePushPop(Parser.PUSH,"constant",0);
        }
    }

    public void writeCall(String func, int argsAmount) {
        String lNew = "RETURN_LABEL" + (lCount++);
        printerOut.print("@" + lNew + "\n" + "D=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
        printerOut.print(buildPushA("LCL",0,true));
        printerOut.print(buildPushA("ARG",0,true));
        printerOut.print(buildPushA("THIS",0,true));
        printerOut.print(buildPushA("THAT",0,true));
        printerOut.print("@SP\n" + "D=M\n" + "@5\n" + "D=D-A\n" + "@" + argsAmount + "\n" + "D=D-A\n" + "@ARG\n" + "M=D\n" + "@SP\n" + "D=M\n" +
                        "@LCL\n" + "M=D\n" + "@" + func + "\n" + "0;JMP\n" + "(" + lNew + ")\n");
    }

    public void writeReturn() {
        printerOut.print(structBuilder2());
    }

    public void writeInit() {
        printerOut.print("@256\n" + "D=A\n" + "@SP\n" + "M=D\n");
        writeCall("Sys.init",0);
    }

    public String structBuilder1(String pos) {
        return "@R11\n" + "D=M-1\n" + "AM=D\n" + "D=M\n" + "@" + pos + "\n" + "M=D\n";
    }

    public String structBuilder2() {
        return "@LCL\n" + "D=M\n" + "@R11\n" + "M=D\n" + "@5\n" + "A=D-A\n" + "D=M\n" + "@R12\n" + "M=D\n" +
                buildPopA("ARG",0,false) + "@ARG\n" + "D=M\n" + "@SP\n" + "M=D+1\n" +
                structBuilder1("THAT") + structBuilder1("THIS") + structBuilder1("ARG") + structBuilder1("LCL") +
                "@R12\n" + "A=M\n" + "0;JMP\n";
    }
}