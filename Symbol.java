import java.util.ArrayList;

/**
 *
 * @author wangjs
 */
//This class is used to define the symbol
public class Symbol {

    public enum SymbolType {
        var, function, method, constructor, field, Static, argument
    };
    public SymbolType kind;
    public boolean initOrNot = false;
    public String lexeme;
    public String returnType;
    public String assignDataType;
    public String beAssignedDataType;
    public String memorySegment;
    public int numOfVar;
    public int offset;
    public ArrayList<String> argumentsDataType = new ArrayList<String>();
}
