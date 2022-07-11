import java.util.ArrayList;

/**
 *
 * @author wangjs
 */
//This class is used to store the symbols and provide several interfaces for other classes
public class SymbolTable {
    public ArrayList<Symbol> symbols;
    public String varName;
    public String memorySegment;
    public String dataType;
    public String functionType;
    public String returnType;
    public int offset;
    public int counter;
    public int numOfVar;
    public int numOfArgs;
    public boolean initOrNot;
    //Initialize each variable
    public SymbolTable() {
        symbols = new ArrayList<Symbol>();
        counter = 0;
        varName = "";
        memorySegment = "";
        dataType = "";
        functionType = "";
        returnType = "";
        offset = 0;
        numOfVar = 0;
        numOfArgs = 0;
    }
    //Add the symbol of identifier into the symbol table
    public void addIdentifierSymbolTable(String lexeme, Symbol.SymbolType kind, String assignDataType, String beAssignDataType, String memorySegment) {
        Symbol symbol = new Symbol();
        symbol.lexeme = lexeme;
        symbol.kind = kind;
        symbol.offset = counter++;
        symbol.assignDataType = assignDataType;
        symbol.beAssignedDataType = beAssignDataType;
        symbol.memorySegment = memorySegment;
        symbols.add(symbol);
    }
    //Add the symbol of subroutine into the symbol table
    public void addFunctionSymbolTable(String lexeme, Symbol.SymbolType kind, ArrayList<String> argumentsDataType, String returnType, int numOfVar) {
        Symbol symbol = new Symbol();
        symbol.lexeme = lexeme;
        symbol.kind = kind;
        symbol.argumentsDataType = argumentsDataType;
        symbol.returnType = returnType;
        symbol.numOfVar = numOfVar;
        symbols.add(symbol);
    }
    //Store the names of subroutines in the current class
    public void addLocalFunctionName(String lexeme) {
        Symbol symbol = new Symbol();
        symbol.lexeme = lexeme;
    }
    //Find if the lexeme required exists in the current symbol table for variable
    public boolean findIdentifierSymbol(String lexeme) {
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                varName = symbol.lexeme;
                memorySegment = symbol.memorySegment;
                offset = symbol.offset;
                dataType = symbol.assignDataType;
                initOrNot = symbol.initOrNot;
                return true;
            }
        }
        return false;
    }
    //Find if the lexeme required exists in the current symbol table for subroutine
    public boolean findFunctionSymbol(String lexeme) {
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                functionType = symbol.kind.toString();
                returnType = symbol.returnType;
                numOfVar = symbol.numOfVar;
                return true;
            }
        }
        return false;
    }
    //Return the value of offset
    public int getOffset(String lexeme) {
        int offset = 0;
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                offset = symbol.offset;
                break;
            }
        }
        return offset;
    }
    /*Check if the data type of current lexeme matches
    with the type transmitted into the method as the argument
    */
    public boolean checkSymbolType(String lexeme, String type) {
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                if (symbol.kind.toString().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }
    //Return the arguments of the subroutine
    public String getArgument(String lexeme) {
        String arguments = "";
        numOfArgs = 0;
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                for (String argument : symbol.argumentsDataType) {
                    numOfArgs++;
                    arguments += (argument + " ");
                }
            }
        }
        return arguments;
    }
    //Return the data type of return value of subroutine
    public String getFunctionReturnType(String lexeme) {
        String returnType = "";
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                returnType = symbol.returnType;
                break;
            }
        }
        return returnType;
    }

    public String getIdentifierAssignDataType(String lexeme) {
        String dataType = "";
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                dataType = symbol.assignDataType;
                break;
            }
        }
        return dataType;
    }

    public String getIdentifierBeAssignedDataType(String lexeme) {
        String dataType = "";
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                dataType = symbol.beAssignedDataType;
                break;
            }
        }
        return dataType;
    }


    //Set the counter variable
    public void setCounter(int counter) {
        this.counter = counter;
    }
    //Set the state of initialization to true
    public void setInitOrNot(String lexeme) {
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                symbol.initOrNot = true;
                break;
            }
        }
    }
    //Set the state of initialization to false
    public void setInitToNot(String lexeme) {
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                symbol.initOrNot = false;
                break;
            }
        }
    }

    //Return the state of initialization
    public boolean getInitOrNot(String lexeme) {
        boolean initOrNot = false;
        for (Symbol symbol : symbols) {
            if (symbol.lexeme.equals(lexeme)) {
                initOrNot = symbol.initOrNot;
                break;
            }
        }
        return initOrNot;
    }
    //Print the current symbol table
    public void print() {
        for (Symbol symbol : symbols) {
            System.out.println(symbol.lexeme + "," + symbol.initOrNot);
        }
    }
}
