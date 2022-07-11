import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author wangjs
 */
//This class is used to store the information of Jack libraries
public class JackClasses {

    final public String[] jackLibraries = {"int",
        "boolean", "char", "Math", "Array", "Memory", "Screen", "Keyboard", "Output", "String", "Sys"};
    final public Map functionsArguments = new HashMap();
    final public Map returnTypes = new HashMap();
    final public Map dataTypeTransfer = new HashMap();
    public String[] methods = {"Array.dispose", "String.dispose", "String.length", "String.charAt", 
        "String.setCharAt", "String.appendChar", "String.eraseLastChar", "String.intValue", "String.setInt"};

    public JackClasses() {
        initArgumentDictionary();
        initReturnTypeDictionary();
        dataTypeTransferDictionary();
    }
    //Insert the arguments of Jack libraries into the dictionary
    private void initArgumentDictionary() {
        functionsArguments.put("Math.abs", "(int|all) ");
        functionsArguments.put("Math.multiply", "(int|all) (int|all) ");
        functionsArguments.put("Math.divide", "(int|all) (int|all) ");
        functionsArguments.put("Math.min", "(int|all) (int|all) ");
        functionsArguments.put("Math.max", "(int|all) (int|all) ");
        functionsArguments.put("Math.sqrt", "(int|all) ");

        functionsArguments.put("Array.new", "(int|all) ");
        functionsArguments.put("Array.dispose", "");

        functionsArguments.put("Memory.peek", "(int|all) ");
        functionsArguments.put("Memory.poke", "(int|all) (int|all) ");
        functionsArguments.put("Memory.alloc", "(int|all) ");
        functionsArguments.put("Memory.deAlloc", "(Array|class|all) ");

        functionsArguments.put("Screen.clearScreen", "");
        functionsArguments.put("Screen.setColor", "(int|boolean|all) ");
        functionsArguments.put("Screen.drawPixel", "(int|all) (int|all) (int|all) (int|all) ");
        functionsArguments.put("Screen.drawRectangle", "(int|all) (int|all) (int|all) (int|all) ");
        functionsArguments.put("Screen.drawLine", "(int|all) (int|all) (int|all) (int|all) ");
        functionsArguments.put("Screen.drawCircle", "(int|all) (int|all) (int|all) ");
        functionsArguments.put("Screen.clearCircle", "(int|all) (int|all) (int|all) ");
        

        functionsArguments.put("Keyboard.keyPressed", "");
        functionsArguments.put("Keyboard.readChar", "");
        functionsArguments.put("Keyboard.readLine", "(String|null|all) ");
        functionsArguments.put("Keyboard.readInt", "(String|null|all) ");

        functionsArguments.put("Output.init", "");
        functionsArguments.put("Output.moveCursor", "(int|all) (int|all) ");
        functionsArguments.put("Output.printChar", "(int|char|all) ");
        functionsArguments.put("Output.printString", "(String|null|all) ");
        functionsArguments.put("Output.printInt", "(int|Array|char|all) ");
        functionsArguments.put("Output.println", "");
        functionsArguments.put("Output.backSpace", "");

        functionsArguments.put("String.new", "(int|all) ");
        functionsArguments.put("String.dispose", "");
        functionsArguments.put("String.length", "");
        functionsArguments.put("String.charAt", "(int|all) ");
        functionsArguments.put("String.setCharAt", "(int|all) (int|char|all) ");
        functionsArguments.put("String.appendChar", "(int|char|all) ");
        functionsArguments.put("String.eraseLastChar", "");
        functionsArguments.put("String.intValue", "");
        functionsArguments.put("String.setInt", "(int|all) ");
        functionsArguments.put("String.newLine", "");
        functionsArguments.put("String.backSpace", "");
        functionsArguments.put("String.doubleQuote", "");

        functionsArguments.put("Sys.halt", "");
        functionsArguments.put("Sys.error", "(int|all) ");
        functionsArguments.put("Sys.wait", "(int|all) ");
    }
    //Insert the return type of Jack libraries into the dictionary
    private void initReturnTypeDictionary() {
        returnTypes.put("Math.abs", "int");
        returnTypes.put("Math.multiply", "int");
        returnTypes.put("Math.divide", "int");
        returnTypes.put("Math.min", "int");
        returnTypes.put("Math.max", "int");
        returnTypes.put("Math.sqrt", "int");

        returnTypes.put("Array.new", "Array");
        returnTypes.put("Array.dispose", "void");

        returnTypes.put("Memory.peek", "int");
        returnTypes.put("Memory.poke", "void");
        returnTypes.put("Memory.alloc", "Array");
        returnTypes.put("Memory.deAlloc", "void");

        returnTypes.put("Screen.clearScreen", "void");
        returnTypes.put("Screen.setColor", "void");
        returnTypes.put("Screen.drawPixel", "void");
        returnTypes.put("Screen.drawRectangle", "void");
        returnTypes.put("Screen.drawLine", "void");
        returnTypes.put("Screen.drawCircle", "void");
        returnTypes.put("Screen.clearCircle", "void");

        returnTypes.put("Keyboard.keyPressed", "char");
        returnTypes.put("Keyboard.readChar", "char");
        returnTypes.put("Keyboard.readLine", "String");
        returnTypes.put("Keyboard.readInt", "int");

        returnTypes.put("Output.init", "void");
        returnTypes.put("Output.moveCursor", "void");
        returnTypes.put("Output.printChar", "void");
        returnTypes.put("Output.printString", "void");
        returnTypes.put("Output.printInt", "void");
        returnTypes.put("Output.println", "void");
        returnTypes.put("Output.backSpace", "void");

        returnTypes.put("String.new", "String");
        returnTypes.put("String.dispose", "void");
        returnTypes.put("String.length", "int");
        returnTypes.put("String.charAt", "char");
        returnTypes.put("String.setCharAt", "void");
        returnTypes.put("String.appendChar", "String");
        returnTypes.put("String.eraseLastChar", "void");
        returnTypes.put("String.intValue", "int");
        returnTypes.put("String.setInt", "void");
        returnTypes.put("String.newLine", "char");
        returnTypes.put("String.backSpace", "char");
        returnTypes.put("String.doubleQuote", "char");

        returnTypes.put("Sys.halt", "void");
        returnTypes.put("Sys.error", "void");
        returnTypes.put("Sys.wait", "void");
    }
    //Store the information for type conversion into the dictionary
    private void dataTypeTransferDictionary() {
        dataTypeTransfer.put("char", "(char|int|null)");
        dataTypeTransfer.put("String", "(String|null)");
        dataTypeTransfer.put("int", "int|Array");
        dataTypeTransfer.put("boolean", "(boolean|int)");
        dataTypeTransfer.put("Array", "(Array|int|class|null)");
    }
    //Check if the subroutine called is a method
    public boolean methodMatch(String methodName) {
        for (String method : methods) {
            if (method.equals(methodName)) {
                return true;
            }
        }
        return false;
    }
}
