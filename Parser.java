import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author wangjs
 */
//This class is used to make the grammar checking, semantic analysis and code generation.
//The symbol tables will also be created in this class
public class Parser {
    //symbolTables[0] = classStaticVariableSymbolTable
    //symbolTables[1] = classFieldVariableSymbolTable
    //symbolTables[2] = functionVarSymbolTable
    //symbolTables[3] = functionArgumentSymbolTable
    //symbolTables[4] = otherClassFunctionsSymbolTable
    private Lexer lexer = null;
    private SymbolTable[] symbolTables = new SymbolTable[5];
    private String textContent = null;
    private JackClasses jackClasses = null;
    private String Type = "int|char|boolean|ID|void|String|Array";
    private String otherClassType = "";
    private File vmFile = null;
    private String className = "";
    private String expressionReturnType = "";
    private String functionName = "";
    private String subroutineKind = "";
    private String returnType = "";
    private String oldToken = "";
    private String lastFunctionName = "";
    private boolean arrayInitOrNot = false;
    private boolean constructorOrNot = false;
    private boolean methodOrNot = false;
    private int ifCounter = 0;
    private int whileCounter = 0;
    private int pushCounter = 0;
    private int eleNumberCounter = 0;
    private Map classFieldVar = new HashMap();
    private Map classArrayNumOfEle = new HashMap();
    private Map functionArrayNumOfEle = new HashMap();

    //Initialize the lexer and each symboltable
    public Parser(Lexer lexer) {
        this.lexer = lexer;
        lexer.initLocalFile();
        symbolTables[0] = new SymbolTable();
        symbolTables[1] = new SymbolTable();
        symbolTables[2] = new SymbolTable();
        symbolTables[3] = new SymbolTable();
        symbolTables[4] = new SymbolTable();
        textContent = this.lexer.getTextContent();
        jackClasses = new JackClasses();
        parserAnalysis();
    }
    //Create the VM file if it not exists currently
    private void vmFileCreate() {
        try {
            vmFile = new File(lexer.getFolderPath() + File.separator + className + ".vm");
            if (!vmFile.exists()) {
                vmFile.createNewFile();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //Insert the VM codes into the VM file
    private void vmCodeInput(String vmCodes) {
        try {
            File log = new File(lexer.getFolderPath() + File.separator + className + ".vm");
            FileWriter fileWriter = new FileWriter(log.getAbsoluteFile(), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(vmCodes);
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Output the error information and terminate the execution of the parser
    private void error(String errorInfor) {
        System.out.printf("%s\n", errorInfor);
        exit(0);
    }
    //Check if the class used in current Jack source codes exists in the local folder
    private boolean localFileCheck(String className) {
        try {
            className += ".jack";
            File files = new File(lexer.getFolderPath());
            File[] allFile = files.listFiles();
            for (File f : allFile) {
                if (f.isFile()) {
                    if (className.equals(f.getName())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    //Clear the values of the temporary variables in the symbol tables
    private void symbolTableVarClear() {
        for (SymbolTable symbolTable : symbolTables) {
            symbolTable.memorySegment = "";
            symbolTable.dataType = "";
            symbolTable.returnType = "";
            symbolTable.functionType = "";
            symbolTable.numOfArgs = 0;
            symbolTable.numOfVar = 0;
            symbolTable.offset = 0;
            symbolTable.initOrNot = false;
        }
    }
    //Exact tokens from the lexical analyser and call the corresponding proecessing methods
    private void parserAnalysis() {
        try {

            while (lexer.getReadIndex() < textContent.length() - 2) {
                //Identify the token to call corresponding methods
                Token newToken = lexer.GetNextToken();
                if (newToken.Token.equals("class")) {
                    classCheck();
                    vmFileCreate();
                    ClassesFunctionsReference();
                } else if (newToken.Token.equals("constructor")
                        || newToken.Token.equals("function")
                        || newToken.Token.equals("method")) {
                    if (!functionName.equals(lastFunctionName)) {
                        error("Error: in class: " + className + ", function \""
                                + functionName + "\" doesn't have return statement");
                    }
                    if (newToken.Token.equals("method")) {
                        methodOrNot = true;
                    } else if (newToken.Token.equals("constructor")) {
                        constructorOrNot = true;
                    }
                    subroutineKind = newToken.Token;
                    functionCheck();
                } else if (newToken.Token.equals("field")) {
                    classVarDeclarCheck();
                } else if (newToken.Token.equals("var")) {
                    functionVarDeclarCheck();
                } else if (newToken.Token.equals("let")) {
                    letStatementCheck();
                } else if (newToken.Token.equals("if")) {
                    ifStatementCheck();
                } else if (newToken.Token.equals("while")) {
                    whileStatementCheck();
                } else if (newToken.Token.equals("do")) {
                    doStatementCheck();
                } else if (newToken.Token.equals("return")) {
                    lastFunctionName = functionName;
                    returnStatementCheck();
                }
            }
            if (!functionName.equals(lastFunctionName)) {
                error("Error: function \"" + functionName
                        + "\" doesn't have return statement");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //Check if the format of the class is correct
    private void classCheck() {
        Token lastToken = null;
        if (!lexer.PeekNextToken().Type.toString().equals("ID")) {
            error("Error: in class: " + className + ", identifier is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            className = lexer.PeekNextToken().Token;
            Type += ("|" + className);
            otherClassType += className;
            lexer.GetNextToken();
        }
        //Check the grammar of the source codes between the braces
        if (!lexer.PeekNextToken().Token.equals("{")) {
            error("Error: in class: " + className + ", \"{\" is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            lexer.GetNextToken();
        }
        //Record the current index for recovering
        int oldIndex = lexer.getReadIndex();
        Lexer.newLineCheck = false;
        //Get the last token
        while (lexer.getReadIndex() < textContent.length() - 2) {
            if (!lexer.PeekNextToken().Token.equals("")) {
                lastToken = lexer.PeekNextToken();
            }
            lexer.GetNextToken();

        }
        //Recover the index to the original value
        lexer.setReadIndex(oldIndex);
        Lexer.newLineCheck = true;
        //Check if the last token is right brace
        if (!lastToken.Token.equals("}")) {
            error("Error: in class: " + className + ", \"}\" is expected, line: "
                    + lastToken.LineNumber);
        }

    }
    /*Declare a new lexer and exact each token from source codes to find all
    other classes used in current source codes. Then call the methods to search
    their variables and methods.*/
    private void ClassesFunctionsReference() {
        Lexer classCheckLexer = new Lexer(className + ".jack");
        classCheckLexer.initLocalFile();
        //Read all tokens from source codes to find all kinds of variables
        while (classCheckLexer.getReadIndex() < classCheckLexer.getTextContent().length() - 2) {
            if (classCheckLexer.PeekNextToken().Token.equals("var")
                    || classCheckLexer.PeekNextToken().Token.equals("field")
                    || classCheckLexer.PeekNextToken().Token.equals("static")) {

                classCheckLexer.GetNextToken();
                boolean jackLibrariesOrNot = false;
                for (String library : jackClasses.jackLibraries) {
                    if (classCheckLexer.PeekNextToken().Token.equals(library)) {
                        jackLibrariesOrNot = true;
                        break;
                    }
                }
                //If the data type is not pre-defined or Jack libraries
                if (!jackLibrariesOrNot) {
                    Type += ("|" + classCheckLexer.PeekNextToken().Token);
                    otherClassType += ("|" + classCheckLexer.PeekNextToken().Token);
                    if (!classCheckLexer.PeekNextToken().Token.equals(className)) {
                        classFunctionsCheck(classCheckLexer.PeekNextToken().Token);
                        classStaticDeclarCheck(classCheckLexer.PeekNextToken().Token);
                    }
                }
            }
            classCheckLexer.GetNextToken();
        }
        classFunctionsCheck(className);
        classStaticDeclarCheck(className);
    }
    //This method is used to search and record all subroutines in the current class
    private void classFunctionsCheck(String name) {
        Pattern pattern = Pattern.compile(Type);
        ArrayList<String> fieldVars = new ArrayList<String>();
        Token lastToken = null;
        String functionType = "";
        String dataType = "";
        //Declare a new lexer to get the tokens
        Lexer temLexer = new Lexer(name + ".jack");
        temLexer.initLocalFile();
        /*Construct a loop to supervise the value of index for terminating the loop
        when end of source codes have been reached.*/
        while (temLexer.getReadIndex() < temLexer.getTextContent().length() - 2) {
            //Store all fields belonging to the current class
            if (temLexer.PeekNextToken().Token.equals("field")) {
                temLexer.GetNextToken();

                if (!(pattern.matcher(temLexer.PeekNextToken().Token).matches()
                        || localFileCheck(temLexer.PeekNextToken().Token))) {
                    error("Error: in class: " + name + ", keyword is expected, line: "
                            + temLexer.GetNextToken().LineNumber);
                } else {
                    dataType = temLexer.PeekNextToken().Token;
                    temLexer.GetNextToken();
                }
                while (true) {

                    if (!temLexer.PeekNextToken().Type.toString().equals("ID")) {
                        error("Error: in class: " + name + ", identifier is expected, line: "
                                + temLexer.PeekNextToken().LineNumber);
                    } else {
                        fieldVars.add(dataType);
                        temLexer.GetNextToken();
                    }
                    if (!temLexer.PeekNextToken().Token.equals(",")) {
                        break;
                    } else {
                        temLexer.GetNextToken();
                    }
                }

                if (!temLexer.PeekNextToken().Token.equals(";")) {
                    error("Error: in class: " + name + ", \";\" is expected, line: "
                            + temLexer.PeekNextToken().LineNumber);
                }
            /*Store the information of all subroutines into the symbol table and
                check if the grammar of these subroutines are correct*/
            } else if (temLexer.PeekNextToken().Token.equals("function")
                    || temLexer.PeekNextToken().Token.equals("method")
                    || temLexer.PeekNextToken().Token.equals("constructor")) {
                functionType = temLexer.PeekNextToken().Token;
                temLexer.GetNextToken();
                int numOfVar = 0;
                String localFunctionName = "";
                String localReturnType = "";
                ArrayList<String> argumentsDataType = new ArrayList<String>();
                if (!pattern.matcher(temLexer.PeekNextToken().Token).matches()
                        && !pattern.matcher(temLexer.PeekNextToken().Type.toString()).matches()) {
                    error("Error: in class: " + name + ", keyword is expected, line: "
                            + temLexer.PeekNextToken().LineNumber);
                } else {
                    localReturnType = temLexer.PeekNextToken().Token;
                    temLexer.GetNextToken();
                }

                if (!temLexer.PeekNextToken().Type.toString().equals("ID")) {
                    error("Error: in class: " + name + ", identifier is expected, line: "
                            + temLexer.PeekNextToken().LineNumber);
                } else {
                    localFunctionName = temLexer.PeekNextToken().Token;
                    temLexer.GetNextToken();
                }
                //Check if the grammar in the argument list is correct
                if (!temLexer.PeekNextToken().Token.equals("(")) {
                    error("Error: in class: " + name + ", \"(\" is expected, line: "
                            + temLexer.GetNextToken().LineNumber);
                } else {
                    temLexer.GetNextToken();
                    while (!temLexer.PeekNextToken().Token.equals(")")) {

                        if (!pattern.matcher(temLexer.PeekNextToken().Token).matches()
                                && !pattern.matcher(temLexer.PeekNextToken().Type.toString()).matches()) {
                            error("Error: in class: " + name + ", keyword is expected, line: "
                                    + temLexer.PeekNextToken().LineNumber);
                        } else {
                            //Store the data type can be accepted for type conversion
                            if (temLexer.PeekNextToken().Token.equals("Array")) {
                                argumentsDataType.add("(int|Array|class|null|all)");
                            } else if (temLexer.PeekNextToken().Token.equals("String")) {
                                argumentsDataType.add("(String|null|all)");
                            } else if (temLexer.PeekNextToken().Token.equals("char")) {
                                argumentsDataType.add("(int|char|all)");
                            } else if (temLexer.PeekNextToken().Token.equals("boolean")) {
                                argumentsDataType.add("(int|boolean|all)");
                            } else if (temLexer.PeekNextToken().Token.equals("int")) {
                                argumentsDataType.add("(int|all)");
                            } else {
                                argumentsDataType.add("(" + temLexer.PeekNextToken().Token + "|null|Array)");
                            }

                            temLexer.GetNextToken();
                        }

                        if (!temLexer.PeekNextToken().Type.toString().equals("ID")) {
                            error("Error: in class: " + name + ", identifier is expected, line: "
                                    + temLexer.PeekNextToken().LineNumber);
                        } else {
                            temLexer.GetNextToken();
                        }

                        if (!temLexer.PeekNextToken().Token.equals(",")) {
                            break;
                        } else {
                            temLexer.GetNextToken();
                        }
                    }
                    if (!temLexer.PeekNextToken().Token.equals(")")) {
                        error("Error: in class: " + name + ", \")\" is expected, line: "
                                + temLexer.PeekNextToken().LineNumber);
                    } else {
                        temLexer.GetNextToken();
                    }
                }
                /*Check if the grammar of braces are correct and count the number
                of the local variables in the braces used in the code generation*/
                if (!temLexer.PeekNextToken().Token.equals("{")) {
                    error("Error: in class: " + name + ", \"{\" is expected, line: "
                            + temLexer.PeekNextToken().LineNumber);
                } else {

                    int counterLB = 0;
                    int counterRB = 0;
                    int oldIndex = 0;
                    int result = 0;
                    counterLB++;

                    temLexer.GetNextToken();
                    oldIndex = temLexer.getReadIndex();
                    temLexer.newLineCheck = false;
                    //Count the number of local variables
                    while (temLexer.getReadIndex() < temLexer.getTextContent().length() - 2) {
                        if (temLexer.PeekNextToken().Token.equals("var")) {
                            temLexer.GetNextToken();

                            if (!pattern.matcher(temLexer.PeekNextToken().Token).matches()) {
                                error("Error: in class: " + name + ", keyword is expected, line: "
                                        + temLexer.PeekNextToken().LineNumber);
                            } else {
                                temLexer.GetNextToken();
                            }
                            while (true) {
                                if (!temLexer.PeekNextToken().Type.toString().equals("ID")) {
                                    error("Error: in class: " + name + ", identifier is expected, line: "
                                            + temLexer.PeekNextToken().LineNumber);
                                } else {
                                    numOfVar++;
                                    temLexer.GetNextToken();
                                }
                                if (!temLexer.PeekNextToken().Token.equals(",")) {
                                    break;
                                } else {
                                    temLexer.GetNextToken();
                                }
                            }

                            if (!temLexer.PeekNextToken().Token.equals(";")) {
                                error("Error: in class: " + name + ", \";\" is expected, line: "
                                        + temLexer.PeekNextToken().LineNumber);
                            }
                        }

                        if (temLexer.PeekNextToken().Token.equals("{")) {
                            counterLB++;
                        } else if (temLexer.PeekNextToken().Token.equals("}")) {
                            counterRB++;
                        }
                        lastToken = temLexer.GetNextToken();

                        if (counterLB == counterRB) {
                            result = 1;
                            break;
                        }
                    }
                    if (result == 0) {
                        error("Error: in class: " + name + ", \"}\" is expected, line: "
                                + temLexer.PeekNextToken().LineNumber);
                    }

                    temLexer.setReadIndex(oldIndex);
                    temLexer.newLineCheck = true;
                }
                //Store the information of subroutines into the symbol table
                if (symbolTables[4].findFunctionSymbol(name + "." + localFunctionName)) {
                    error("Error: in class: " + name + ", function redeclaration, line:"
                            + temLexer.PeekNextToken().LineNumber);
                } else {
                    if (functionType.equals("function")) {
                        symbolTables[4].addFunctionSymbolTable(name + "."
                                + localFunctionName, Symbol.SymbolType.function,
                                argumentsDataType, localReturnType, numOfVar);
                    } else if (functionType.equals("method")) {
                        symbolTables[4].addFunctionSymbolTable(name + "."
                                + localFunctionName, Symbol.SymbolType.method,
                                argumentsDataType, localReturnType, numOfVar);
                    } else {
                        symbolTables[4].addFunctionSymbolTable(name + "."
                                + localFunctionName, Symbol.SymbolType.constructor,
                                argumentsDataType, localReturnType, numOfVar);
                    }
                }
            }
            temLexer.GetNextToken();
        }
        //Store the arraylist used to store the field variables into a dictionary
        classFieldVar.put(name, fieldVars);
    }
    //This method is used to check if the grammar of subroutine in current class is correct
    private void functionCheck() throws Exception {
        symbolTables[2].setCounter(0);
        symbolTables[3].setCounter(0);
        symbolTables[2].symbols.clear();
        symbolTables[3].symbols.clear();
        functionArrayNumOfEle.clear();
        ifCounter = 0;
        whileCounter = 0;
        //If current subroutine is a method, the first argument should be 'this'
        if (methodOrNot) {
            symbolTables[3].addIdentifierSymbolTable("this",
                    Symbol.SymbolType.argument, className, className, "this");
        }
        //Check the grammar of the definition of subroutine
        Pattern pattern = Pattern.compile(Type);
        if (!pattern.matcher(lexer.PeekNextToken().Token).matches()
                && !pattern.matcher(lexer.PeekNextToken().Type.toString()).matches()) {
            error("Error: in class: " + className + ", keyword is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {

            returnType = lexer.PeekNextToken().Token;
            lexer.GetNextToken();
        }

        if (!lexer.PeekNextToken().Type.toString().equals("ID")) {
            error("Error: in class: " + className + ", identifier is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            functionName = lexer.PeekNextToken().Token;
            lexer.GetNextToken();
        }
        //Check the grammar of the argument list
        if (!lexer.PeekNextToken().Token.equals("(")) {
            error("Error: in class: " + className + ", \"(\" is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            lexer.GetNextToken();
            while (!lexer.PeekNextToken().Token.equals(")")) {
                String assignArgumentType = "";
                String beAssignedArgumentType = "";
                if (!pattern.matcher(lexer.PeekNextToken().Token).matches()
                        && !pattern.matcher(lexer.PeekNextToken().Type.toString()).matches()) {
                    error("Error: in class: " + className + ", keyword is expected, line: "
                            + lexer.PeekNextToken().LineNumber);
                } else {
                    //Store all the data types can be accepted for type conversion
                    assignArgumentType = lexer.PeekNextToken().Token;
                    if (assignArgumentType.equals("Array")) {
                        beAssignedArgumentType = "(int|Array)";
                    } else if (assignArgumentType.equals("String")) {
                        beAssignedArgumentType = "(String|null)";
                    } else if (assignArgumentType.equals("char")) {
                        beAssignedArgumentType = "(int|char)";
                    } else if (assignArgumentType.equals("boolean")) {
                        beAssignedArgumentType = "(int|boolean)";
                    } else if (assignArgumentType.equals("int")) {
                        beAssignedArgumentType = assignArgumentType;
                    } else {
                        beAssignedArgumentType = "(" + assignArgumentType + "|null|Array)";
                    }
                    lexer.GetNextToken();
                }

                if (!lexer.PeekNextToken().Type.toString().equals("ID")) {
                    error("Error: in class: " + className + ", identifier is expected, line: "
                            + lexer.PeekNextToken().LineNumber);
                } else {
                    if (symbolTables[3].findIdentifierSymbol(lexer.PeekNextToken().Token)) {
                        error("Error: in class: " + className + ", argument redeclaration, line:"
                                + lexer.PeekNextToken().LineNumber);
                    } else {
                        symbolTables[3].addIdentifierSymbolTable(lexer.PeekNextToken().Token,
                                Symbol.SymbolType.argument, assignArgumentType, beAssignedArgumentType, "argument");
                    }
                    lexer.GetNextToken();
                }

                if (!lexer.PeekNextToken().Token.equals(",")) {
                    break;
                } else {
                    lexer.GetNextToken();
                }
            }
            if (!lexer.PeekNextToken().Token.equals(")")) {
                error("Error: in class: " + className + ", \")\" is expected, line: "
                        + lexer.PeekNextToken().LineNumber);
            } else {
                lexer.GetNextToken();
            }
        }
        oldToken = lexer.PeekNextToken().Token;
        symbolTables[4].findFunctionSymbol(className + "." + functionName);
        //Insert the VM codes of subroutine into the VM file
        vmCodeInput("function " + className + "." + functionName
                + " " + symbolTables[4].numOfVar + "\n");
        if (constructorOrNot) {
            ArrayList<String> fieldVarArray = (ArrayList<String>) classFieldVar.get(className);
            vmCodeInput("push constant " + fieldVarArray.size() + "\n");
            vmCodeInput("call Memory.alloc 1\n");
            vmCodeInput("pop pointer 0\n");
            constructorOrNot = false;
        } else if (methodOrNot) {
            vmCodeInput("push argument 0\n");
            vmCodeInput("pop pointer 0\n");
            methodOrNot = false;
        }
        //Reset the values of temporary variables in the symbol tables
        symbolTableVarClear();
    }
    //This method is used to find all static belonging to current classes
    private void classStaticDeclarCheck(String name) {
        try {
            Pattern pattern = Pattern.compile(Type);
            //Declare the new lexical analyser
            Lexer temLexer = new Lexer(name + ".jack");
            temLexer.initLocalFile();
            /*Construct a loop to supervise the value of index for terminating the loop
            when end of source codes have been reached.*/
            while (temLexer.getReadIndex() < temLexer.getTextContent().length() - 2) {
                if (temLexer.PeekNextToken().Token.equals("static")) {
                    temLexer.GetNextToken();
                    String assignArgumentType = "";
                    String beAssignedArgumentType = "";
                    if (!pattern.matcher(temLexer.PeekNextToken().Token).matches()
                            && !pattern.matcher(temLexer.PeekNextToken().Type.toString()).matches()) {
                        error("Error: in class: " + name + ", keyword is expected, line: " + temLexer.PeekNextToken().LineNumber);
                    } else {
                        //Store all data types can be accepted for type conversion
                        assignArgumentType = temLexer.PeekNextToken().Token;
                        if (assignArgumentType.equals("Array")) {
                            beAssignedArgumentType = "(int|Array|null|class|all)";
                        } else if (assignArgumentType.equals("String")) {
                            beAssignedArgumentType = "(String|null|all)";
                        } else if (assignArgumentType.equals("char")) {
                            beAssignedArgumentType = "(int|char|all)";
                        } else if (assignArgumentType.equals("boolean")) {
                            beAssignedArgumentType = "(int|boolean|all)";
                        } else if (assignArgumentType.equals("int")) {
                            beAssignedArgumentType = "(int|all)";
                        } else {
                            beAssignedArgumentType = "(" + assignArgumentType + "|null|Array)";
                        }
                        temLexer.GetNextToken();
                    }
                    //Store the information of static variables into the symbol table
                    while (true) {
                        if (!temLexer.PeekNextToken().Type.toString().equals("ID")) {
                            error("Error: in class: " + name + ", identifier is expected, line: " + temLexer.PeekNextToken().LineNumber);
                        } else {
                            if (symbolTables[0].findIdentifierSymbol(name + "." + temLexer.PeekNextToken().Token)
                                    && symbolTables[1].findIdentifierSymbol(temLexer.PeekNextToken().Token)) {
                                error("Error: in class: " + className + ", Variable redeclaration, line: " + temLexer.PeekNextToken().LineNumber);
                            } else {
                                symbolTables[0].addIdentifierSymbolTable(name + "." + temLexer.PeekNextToken().Token, Symbol.SymbolType.Static, assignArgumentType, beAssignedArgumentType, "static");
                                if (assignArgumentType.equals("Array")) {
                                    classArrayNumOfEle.put(temLexer.PeekNextToken().Token, null);
                                }
                            }

                            temLexer.GetNextToken();
                        }
                        if (!temLexer.PeekNextToken().Token.equals(",")) {
                            break;
                        } else {
                            temLexer.GetNextToken();
                        }
                    }

                    if (!temLexer.PeekNextToken().Token.equals(";")) {
                        error("Error: in class: " + name + ", \";\" is expected, line: " + temLexer.PeekNextToken().LineNumber);
                    }
                }
                temLexer.GetNextToken();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        //Clear the values of temporary variables
        symbolTableVarClear();
    }
    /*This method is used to check the grammar of the fields in current source
    codes and store the information of them into the corresponding symbol table
    */
    private void classVarDeclarCheck() {
        Pattern pattern = Pattern.compile(Type);
        String assignArgumentType = "";
        String beAssignedArgumentType = "";
        if (!pattern.matcher(lexer.PeekNextToken().Token).matches()
                && !pattern.matcher(lexer.PeekNextToken().Type.toString()).matches()) {
            error("Error: in class: " + className + ", keyword is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            //Store all data types can be accepted for type conversion
            assignArgumentType = lexer.PeekNextToken().Token;
            if (assignArgumentType.equals("Array")) {
                beAssignedArgumentType = "(int|Array|all)";
            } else if (assignArgumentType.equals("String")) {
                beAssignedArgumentType = "(String|null|all)";
            } else if (assignArgumentType.equals("char")) {
                beAssignedArgumentType = "(int|char|all)";
            } else if (assignArgumentType.equals("boolean")) {
                beAssignedArgumentType = "(int|boolean|all)";
            } else if (assignArgumentType.equals("int")) {
                beAssignedArgumentType = "(int|all)";
            } else {
                beAssignedArgumentType = "(" + assignArgumentType + "|null|Array)";
            }

            lexer.GetNextToken();
        }
        //Store the information of field variables into the symbol table
        while (true) {
            if (!lexer.PeekNextToken().Type.toString().equals("ID")) {
                error("Error: in class: " + className + ", identifier is expected, line: " + lexer.PeekNextToken().LineNumber);
            } else {

                if (symbolTables[1].findIdentifierSymbol(lexer.PeekNextToken().Token)
                        && symbolTables[0].findIdentifierSymbol(className + "." + lexer.PeekNextToken().Token)) {
                    error("Error: in class: " + className + ", variable redeclaration, line: " + lexer.PeekNextToken().LineNumber);
                } else {
                    symbolTables[1].addIdentifierSymbolTable(lexer.PeekNextToken().Token, Symbol.SymbolType.field, assignArgumentType, beAssignedArgumentType, "this");
                    if (assignArgumentType.equals("Array")) {
                        classArrayNumOfEle.put(lexer.PeekNextToken().Token, null);
                    }
                }

                lexer.GetNextToken();
            }
            if (!lexer.PeekNextToken().Token.equals(",")) {
                break;
            } else {
                lexer.GetNextToken();
            }
        }

        if (!lexer.PeekNextToken().Token.equals(";")) {
            error("Error: in class: " + className + ", \";\" is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            lexer.GetNextToken();
        }
        //Clear the values of temporary variables in the symbol table
        symbolTableVarClear();
    }
    /*This method is used to check the grammar of the local variables in the subroutine
      and insert the information of these variables into the symbol table
    */
    private void functionVarDeclarCheck() {
        Pattern pattern = Pattern.compile(Type);
        String assignArgumentType = "";
        String beAssignedArgumentType = "";
        if (!pattern.matcher(lexer.PeekNextToken().Token).matches()) {
            error("Error: in class: " + className + ", keyword is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            //Store all data types can be accepted for type conversion
            assignArgumentType = lexer.PeekNextToken().Token;
            if (assignArgumentType.equals("Array")) {
                beAssignedArgumentType = "(int|Array|null|all)";
            } else if (assignArgumentType.equals("String")) {
                beAssignedArgumentType = "(String|null|all)";
            } else if (assignArgumentType.equals("char")) {
                beAssignedArgumentType = "(int|char|all)";
            } else if (assignArgumentType.equals("boolean")) {
                beAssignedArgumentType = "(int|boolean|all)";
            } else if (assignArgumentType.equals("int")) {
                beAssignedArgumentType = "(int|all)";
            } else {
                beAssignedArgumentType = "(" + assignArgumentType + "|null|Array)";
            }

            lexer.GetNextToken();
        }
        //Store the information of local variables into the symbol table
        while (true) {
            if (!lexer.PeekNextToken().Type.toString().equals("ID")) {
                error("Error: in class: " + className + ", identifier is expected, line: " + lexer.PeekNextToken().LineNumber);
            } else {
                if (symbolTables[3].findIdentifierSymbol(lexer.PeekNextToken().Token)
                        || symbolTables[2].findIdentifierSymbol(lexer.PeekNextToken().Token)) {
                    error("Error: in class: " + className + ", variable redeclaration, line: " + lexer.PeekNextToken().LineNumber);
                } else {
                    symbolTables[2].addIdentifierSymbolTable(lexer.PeekNextToken().Token, Symbol.SymbolType.var, assignArgumentType, beAssignedArgumentType, "local");
                    if (assignArgumentType.equals("Array")) {
                        functionArrayNumOfEle.put(lexer.PeekNextToken().Token, null);
                    }
                }

                lexer.GetNextToken();
            }

            if (!lexer.PeekNextToken().Token.equals(",")) {
                break;
            } else {
                lexer.GetNextToken();
            }
        }
        if (!lexer.PeekNextToken().Token.equals(";")) {
            error("Error: in class: " + className + ", \";\" is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            oldToken = lexer.PeekNextToken().Token;
            lexer.GetNextToken();
        }
        //Clear the values of the temporary variables in the symbol table
        symbolTableVarClear();
    }

    /*This method is used to check the grammar of the let statement, store
      variable has been initialized and make the data type checking for the
      two sides around the equal sign.
    */
    private void letStatementCheck() throws Exception {
        String lastToken = "";
        String varName = "";
        String memorySegment = "";
        String dataType = "";
        int offset = 0;
        boolean arrayOrNot = false;
        Pattern pattern = Pattern.compile(otherClassType);
        if (!lexer.PeekNextToken().Type.toString().equals("ID")) {
            error("Error: in class: " + className
                    + ", identifier is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            //Check if the variable is defined before using
            if (!(symbolTables[2].findIdentifierSymbol(lexer.PeekNextToken().Token)
                    || symbolTables[3].findIdentifierSymbol(lexer.PeekNextToken().Token)
                    || symbolTables[1].findIdentifierSymbol(lexer.PeekNextToken().Token)
                    || symbolTables[0].findIdentifierSymbol(className + "." + lexer.PeekNextToken().Token))) {
                error("Error: in class: " + className + ", variable \""
                        + lexer.PeekNextToken().Token + "\" is not declared before, line: "
                        + lexer.PeekNextToken().LineNumber);
            }

            for (SymbolTable symbolTable : symbolTables) {
                if (!symbolTable.dataType.equals("")
                        && !symbolTable.memorySegment.equals("")) {
                    varName = symbolTable.varName;
                    offset = symbolTable.offset;
                    dataType = symbolTable.dataType;
                    memorySegment = symbolTable.memorySegment;
                    break;
                }
                symbolTable.memorySegment = "";
                symbolTable.dataType = "";
                symbolTable.offset = 0;
            }

            lastToken = lexer.PeekNextToken().Token;
            lexer.GetNextToken();

        }
        //Check if the variable on the left-hand side is an array
        if (lexer.PeekNextToken().Token.equals("[")) {
            lexer.GetNextToken();
            expression();
            vmCodeInput("push " + memorySegment + " " + offset + "\n");
            if (!(expressionReturnType.equals("int")
                    || expressionReturnType.equals("all"))) {
                error("Error: in class: " + className
                        + ", \"int value\" is expected, line: "
                        + lexer.PeekNextToken().LineNumber);
            }
            if (!lexer.PeekNextToken().Token.equals("]")) {
                error("Error: in class: " + className
                        + ", \"]\" is expected, line: "
                        + lexer.PeekNextToken().LineNumber);
            } else {
                if (!dataType.equals("Array")) {
                    error("Error: in class: " + className
                            + ", variable \"" + lastToken + "\" is not an array variable, line: "
                            + lexer.PeekNextToken().LineNumber);
                }
                vmCodeInput("add\n");
                arrayOrNot = true;
                lexer.GetNextToken();
            }
        }
        if (!lexer.PeekNextToken().Token.equals("=")) {
            error("Error: in class: " + className
                    + ", \"=\" is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            lexer.GetNextToken();

            String arrayVarName = lexer.PeekNextToken().Token;
            expression();
            //Check if it is the initialization for array and record the number of elements defined,
            if (dataType.equals("Array") && arrayInitOrNot) {
                if (functionArrayNumOfEle.containsKey(varName)) {
                    functionArrayNumOfEle.replace(varName, eleNumberCounter);
                } else {
                    classArrayNumOfEle.replace(varName, eleNumberCounter);
                }
                arrayInitOrNot = false;
            }
            //Insert the VM codes if the expression on the right-hand side is an array
            if (arrayOrNot) {
                vmCodeInput("pop temp 0\n");
                vmCodeInput("pop pointer 1\n");
                vmCodeInput("push temp 0\n");
                vmCodeInput("pop that 0\n");

            } else {
                //Check if the data type of two sides match with each other
                vmCodeInput("pop " + memorySegment + " " + offset + "\n");
                if (Pattern.compile(symbolTables[2].
                        getIdentifierBeAssignedDataType(lastToken)).
                        matcher(expressionReturnType).matches()
                        || Pattern.compile(symbolTables[3].
                                getIdentifierBeAssignedDataType(lastToken)).
                                matcher(expressionReturnType).matches()
                        || Pattern.compile(symbolTables[1].
                                getIdentifierBeAssignedDataType(lastToken)).
                                matcher(expressionReturnType).matches()
                        || Pattern.compile(symbolTables[0].
                                getIdentifierBeAssignedDataType(className + "." + lastToken)).
                                matcher(expressionReturnType).matches()) {
                    if (((pattern.matcher(symbolTables[2].
                            getIdentifierAssignDataType(lastToken)).matches()
                            || pattern.matcher(symbolTables[3].
                                    getIdentifierAssignDataType(lastToken)).matches()
                            || pattern.matcher(symbolTables[1].
                                    getIdentifierAssignDataType(lastToken)).matches()
                            || pattern.matcher(symbolTables[0].
                                    getIdentifierAssignDataType(className + "." + lastToken)).matches())
                            && expressionReturnType.equals("Array"))) {
                        if (!(functionArrayNumOfEle.containsKey(arrayVarName)
                                && (((ArrayList<String>) classFieldVar.get(dataType)).size()
                                == (int) functionArrayNumOfEle.get(arrayVarName)))) {
                            if (!(classArrayNumOfEle.containsKey(arrayVarName)
                                    && (((ArrayList<String>) classFieldVar.get(dataType)).size()
                                    == (int) classArrayNumOfEle.get(arrayVarName)))) {
                                error("Error: in class: " + className
                                        + ", the numer of fields not matches with the number of elements in the array, line: "
                                        + lexer.PeekNextToken().Token);
                            }
                        }
                    }
                } else {
                    error("Error: in class: " + className + ", data type of return acvalue is wrong, line "
                            + lexer.PeekNextToken().LineNumber);
                }

            }
            if (!lexer.PeekNextToken().Token.equals(";")) {
                error("Error: in class: " + className + ", \";\" is expected, line: "
                        + lexer.PeekNextToken().LineNumber);
            } else {
                //Mark if the variable on the left-hand side is initialized
                if (memorySegment.equals("local")) {
                    symbolTables[2].setInitOrNot(lastToken);
                } else if (memorySegment.equals("argument")) {
                    symbolTables[3].setInitOrNot(lastToken);
                } else if (memorySegment.equals("this")) {
                    symbolTables[1].setInitOrNot(lastToken);
                } else {
                    symbolTables[0].setInitOrNot(className + "." + lastToken);
                }
                oldToken = lexer.PeekNextToken().Token;
                lexer.GetNextToken();
            }
        }
        symbolTableVarClear();
    }
    //This method is used to check the grammar of the if statement and insert VM codes for if statement
    private void ifStatementCheck() throws Exception {
        boolean ifReturnOrNot = false;
        boolean elseReturnOrNot = false;
        if (!lexer.PeekNextToken().Token.equals("(")) {
            error("Error: in class: " + className + ", \"(\" is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            lexer.GetNextToken();
        }
        expression();

        if (!lexer.PeekNextToken().Token.equals(")")) {
            error("Error: in class: " + className + ", \")\" is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            lexer.GetNextToken();
        }
        int currentIfCounter = ifCounter;
        vmCodeInput("if-goto IF_TRUE" + currentIfCounter + "\n");
        vmCodeInput("goto IF_FALSE" + currentIfCounter + "\n");

        if (!lexer.PeekNextToken().Token.equals("{")) {
            error("Error: in class: " + className + ", \"{\" is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            vmCodeInput("label IF_TRUE" + currentIfCounter + "\n");
            lexer.GetNextToken();
            ifCounter++;
        }
        //Check the grammar of the source codes in the braces behind the if keyword
        Token newToken = lexer.PeekNextToken();
        try {
            while (lexer.getReadIndex() < textContent.length() - 2) {

                if (newToken.Token.equals("let")) {
                    lexer.GetNextToken();
                    letStatementCheck();
                } else if (newToken.Token.equals("if")) {
                    lexer.GetNextToken();
                    ifStatementCheck();
                } else if (newToken.Token.equals("while")) {
                    lexer.GetNextToken();
                    whileStatementCheck();
                } else if (newToken.Token.equals("do")) {
                    lexer.GetNextToken();
                    doStatementCheck();
                } else if (newToken.Token.equals("return")) {
                    ifReturnOrNot = true;
                    lexer.GetNextToken();
                    returnStatementCheck();
                }

                newToken = lexer.PeekNextToken();

                if (newToken.Token.equals("}")) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!lexer.PeekNextToken().Token.equals("}")) {
            error("Error: in class: " + className + ", \"}\" is expected, line: "
                    + lexer.PeekNextToken().LineNumber);
        } else {
            oldToken = lexer.PeekNextToken().Token;
            lexer.GetNextToken();
        }
        //Check the grammar of the source codes in the braces behind the else keyword
        if (lexer.PeekNextToken().Token.equals("else")) {
            vmCodeInput("goto IF_END" + currentIfCounter + "\n");
            vmCodeInput("label IF_FALSE" + currentIfCounter + "\n");
            lexer.GetNextToken();
            if (!lexer.PeekNextToken().Token.equals("{")) {
                error("Error: in class: " + className + ", \"{\" is expected, line: "
                        + lexer.PeekNextToken().LineNumber);
            } else {
                lexer.GetNextToken();
            }
            newToken = lexer.PeekNextToken();
            try {
                while (lexer.getReadIndex() < textContent.length() - 2) {

                    if (newToken.Token.equals("let")) {
                        lexer.GetNextToken();
                        letStatementCheck();
                    } else if (newToken.Token.equals("if")) {
                        lexer.GetNextToken();
                        ifStatementCheck();

                    } else if (newToken.Token.equals("while")) {
                        lexer.GetNextToken();
                        whileStatementCheck();
                    } else if (newToken.Token.equals("do")) {
                        lexer.GetNextToken();
                        doStatementCheck();
                    } else if (newToken.Token.equals("return")) {
                        elseReturnOrNot = true;
                        lastFunctionName = functionName;
                        lexer.GetNextToken();
                        returnStatementCheck();
                    }

                    newToken = lexer.PeekNextToken();

                    if (newToken.Token.equals("}")) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!lexer.PeekNextToken().Token.equals("}")) {
                error("Error: in class: " + className + ", \"}\" is expected, line: "
                        + lexer.PeekNextToken().LineNumber);
            } else {
                oldToken = lexer.PeekNextToken().Token;
                lexer.GetNextToken();
            }
            vmCodeInput("label IF_END" + currentIfCounter + "\n");
        } else {
            vmCodeInput("label IF_FALSE" + currentIfCounter + "\n");
        }

        if (ifReturnOrNot && elseReturnOrNot) {
            lastFunctionName = functionName;
        }
    }
    //This methd is used to check the grammar of the while loop and insert corresponding VM codes
    private void whileStatementCheck() throws Exception {//need to process the }
        int currentIfCounter = whileCounter;
        vmCodeInput("label WHILE_EXP" + currentIfCounter + "\n");
        if (!lexer.PeekNextToken().Token.equals("(")) {
            error("Error: in class: " + className
                    + ", \"(\" is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            lexer.GetNextToken();
        }
        expression();

        if (!lexer.PeekNextToken().Token.equals(")")) {
            error("Error: in class: " + className
                    + ", \")\" is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            lexer.GetNextToken();
        }
        vmCodeInput("not\n");
        vmCodeInput("if-goto WHILE_END" + currentIfCounter + "\n");
        //Check the grammar of the source codes in the braces behind the while keyword
        if (!lexer.PeekNextToken().Token.equals("{")) {
            error("Error: in class: " + className
                    + ", \"{\" is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            whileCounter++;
            lexer.GetNextToken();
        }
        Token newToken = lexer.PeekNextToken();
        try {
            while (lexer.getReadIndex() < textContent.length() - 2) {

                if (newToken.Token.equals("}")) {
                    break;
                }
                if (newToken.Token.equals("var")) {
                    lexer.GetNextToken();
                    functionVarDeclarCheck();
                } else if (newToken.Token.equals("let")) {
                    lexer.GetNextToken();
                    letStatementCheck();
                } else if (newToken.Token.equals("if")) {
                    lexer.GetNextToken();
                    ifStatementCheck();
                } else if (newToken.Token.equals("while")) {
                    lexer.GetNextToken();
                    whileStatementCheck();
                } else if (newToken.Token.equals("do")) {
                    lexer.GetNextToken();
                    doStatementCheck();
                } else if (newToken.Token.equals("return")) {
                    lexer.GetNextToken();
                    returnStatementCheck();
                }
                newToken = lexer.PeekNextToken();
            }
        } catch (Exception e) {

        }
        if (!lexer.PeekNextToken().Token.equals("}")) {
            error("Error: in class: " + className
                    + ", \"}\" is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            oldToken = lexer.PeekNextToken().Token;
            lexer.GetNextToken();
        }
        vmCodeInput("goto WHILE_EXP" + currentIfCounter + "\n");
        vmCodeInput("label WHILE_END" + currentIfCounter + "\n");
    }
    //This method is used to check the grammar of the do statement
    private void doStatementCheck() {
        try {
            subroutineCallCheck();
            if (!lexer.PeekNextToken().Token.equals(";")) {
                error("Error: in class: " + className
                        + ", \";\" is expected, line: " + lexer.PeekNextToken().LineNumber);
            } else {
                oldToken = lexer.PeekNextToken().Token;
                lexer.GetNextToken();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //This method is used to check the grammar of the subroutine call
    private void subroutineCallCheck() throws Exception {
        Token lastToken = null;
        String subroutineName = "";

        if (!lexer.PeekNextToken().Type.toString().equals("ID")) {
            error("Error: in class: " + className
                    + ", identifier is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            lastToken = lexer.PeekNextToken();
            subroutineName = lastToken.Token;
            lexer.GetNextToken();
        }

        //Used to check the type like identifier.identifier
        if (lexer.PeekNextToken().Token.equals(".")) {
            String arguments = "";
            String methodCallLexeme = "";
            boolean methodCallOrNot = false;
            lexer.GetNextToken();
            subroutineName += "." + lexer.PeekNextToken().Token;
            //Use the if statements to exclude each possibility to find the grammar mistakes
            if (lexer.PeekNextToken().Type.toString().equals("ID")) {
                if (!jackClasses.functionsArguments.containsKey(subroutineName)) {
                    if (!(symbolTables[4].findFunctionSymbol(subroutineName)
                            && (symbolTables[4].functionType.equals("function")
                            || symbolTables[4].functionType.equals("constructor")))) {
                        String objectFunction = subroutineName.split("\\.")[1];
                        if (symbolTables[2].findIdentifierSymbol(lastToken.Token)
                                && (symbolTables[4].findFunctionSymbol(symbolTables[2].dataType + "." + objectFunction)
                                || jackClasses.methodMatch(symbolTables[2].dataType + "." + objectFunction))) {
                            methodCallOrNot = true;
                            methodCallLexeme = symbolTables[2].dataType + "." + objectFunction;

                            vmCodeInput("push " + symbolTables[2].memorySegment + " " + symbolTables[2].offset + "\n");
                            pushCounter++;
                            if (!(symbolTables[4].functionType.equals("method")
                                    || jackClasses.methodMatch(symbolTables[2].dataType + "." + objectFunction))) {
                                error("Error: in class: " + className + ", function calling is not a method, line: " + lastToken.LineNumber);
                            }
                        } else if (symbolTables[3].findIdentifierSymbol(lastToken.Token)
                                && (symbolTables[4].findFunctionSymbol(symbolTables[3].dataType + "." + objectFunction)
                                || jackClasses.methodMatch(symbolTables[3].dataType + "." + objectFunction))) {
                            methodCallOrNot = true;
                            methodCallLexeme = symbolTables[3].dataType + "." + objectFunction;
                            vmCodeInput("push " + symbolTables[3].memorySegment + " " + symbolTables[3].offset + "\n");
                            pushCounter++;
                            if (!(symbolTables[4].functionType.equals("method")
                                    || jackClasses.methodMatch(symbolTables[3].dataType + "." + objectFunction))) {
                                error("Error: in class: " + className + ", function calling is not a method, line: " + lastToken.LineNumber);
                            }
                        } else if (symbolTables[1].findIdentifierSymbol(lastToken.Token)
                                && (symbolTables[4].findFunctionSymbol(symbolTables[1].dataType + "." + objectFunction)
                                || jackClasses.methodMatch(symbolTables[1].dataType + "." + objectFunction))) {
                            methodCallOrNot = true;
                            methodCallLexeme = symbolTables[1].dataType + "." + objectFunction;
                            vmCodeInput("push " + symbolTables[1].memorySegment + " " + symbolTables[1].offset + "\n");
                            pushCounter++;
                            if (!(symbolTables[4].functionType.equals("method")
                                    || jackClasses.methodMatch(symbolTables[1].dataType + "." + objectFunction))) {
                                error("Error: in class: " + className + ", function calling is not a method, line: " + lastToken.LineNumber);
                            }
                        } else {
                            error("Error: in class: " + className + ", function called \""
                                    + subroutineName + "\" is not defined, line: " + lastToken.LineNumber);
                        }
                        //Release the array after the dispose method has been called
                        if (methodCallLexeme.equals("Array.dispose")) {
                            for (int tra = 3; tra >= 0; tra--) {
                                if (symbolTables[tra].findIdentifierSymbol(lastToken.Token)) {
                                    symbolTables[tra].setInitToNot(lastToken.Token);
                                    break;
                                }
                            }
                        }

                    }
                }
                //Check the grammar of argument list
                lexer.GetNextToken();
                if (lexer.PeekNextToken().Token.equals("(")) {
                    if (methodCallOrNot) {
                        pushCounter = 1;
                    } else {
                        pushCounter = 0;
                    }
                    lexer.GetNextToken();
                    while (!lexer.PeekNextToken().Token.equals(")")) {
                        int currentPushCounter = pushCounter;
                        expressionReturnType = "";
                        expression();
                        pushCounter = currentPushCounter + 1;
                        if (Pattern.compile(otherClassType).matcher(expressionReturnType).matches()) {
                            arguments += "class ";
                        } else {
                            arguments += expressionReturnType + " ";
                        }
                        if (!lexer.PeekNextToken().Token.equals(",")) {
                            break;
                        } else {
                            lexer.GetNextToken();
                        }
                    }
                    if (!lexer.PeekNextToken().Token.equals(")")) {
                        error("Error: in class: " + className + ", \")\" is expected, line: "
                                + lexer.PeekNextToken().LineNumber);
                    } else {
                        lexer.GetNextToken();
                    }
                }
                if (!(Pattern.compile(symbolTables[4].
                        getArgument(subroutineName)).
                        matcher(arguments).matches()
                        || Pattern.compile(symbolTables[4].
                                getArgument(methodCallLexeme)).
                                matcher(arguments).matches())
                        && !((jackClasses.functionsArguments.containsKey(subroutineName)
                        && Pattern.compile(((String) jackClasses.
                                functionsArguments.get(subroutineName))).
                                matcher(arguments).matches())
                        || (jackClasses.functionsArguments.containsKey(methodCallLexeme)
                        && Pattern.compile(((String) jackClasses.
                                functionsArguments.get(methodCallLexeme))).
                                matcher(arguments).matches()))) {
                    error("Error: in class: " + className + ", function \""
                            + subroutineName + "\" got wrong arguments, line:" + lastToken.LineNumber);
                }
                //Match the subroutine name with the different symbol tables to insert corresponding VM codes
                if (jackClasses.returnTypes.containsKey(subroutineName)) {
                    expressionReturnType = (String) jackClasses.returnTypes.get(subroutineName);
                    vmCodeInput("call " + subroutineName + " " + pushCounter + "\n");
                    pushCounter = 0;
                } else if (jackClasses.returnTypes.containsKey(methodCallLexeme)) {
                    expressionReturnType = (String) jackClasses.returnTypes.get(methodCallLexeme);
                    vmCodeInput("call " + methodCallLexeme + " " + pushCounter + "\n");
                    pushCounter = 0;
                } else if (!methodCallOrNot && symbolTables[4].findFunctionSymbol(subroutineName)) {
                    expressionReturnType = symbolTables[4].returnType;
                    vmCodeInput("call " + subroutineName + " " + pushCounter + "\n");
                    pushCounter = 0;
                } else if (symbolTables[4].findFunctionSymbol(methodCallLexeme)) {
                    expressionReturnType = symbolTables[4].returnType;
                    vmCodeInput("call " + methodCallLexeme + " " + pushCounter + "\n");
                    pushCounter = 0;
                }
                vmCodeInput("pop temp 0\n");
            } else {
                error("Error: in class: " + className + ", identifier is expected, line: " + lexer.PeekNextToken().LineNumber);
            }
        //Check the grammar of the subroutine call like 'identifier()'
        } else if (lexer.PeekNextToken().Token.equals("(")) {
            String arguments = "";
            if (!symbolTables[4].findFunctionSymbol(className + "." + subroutineName)) {
                error("Error: in class: " + className + ", function: \""
                        + lexer.PeekNextToken().Token + "\" is not defined, line:" + lexer.PeekNextToken().LineNumber);
            } else {

                if (symbolTables[4].functionType.equals("method")) {
                    if (symbolTables[4].findFunctionSymbol(className + "." + functionName)
                            && (symbolTables[4].functionType.equals("method") || symbolTables[4].functionType.equals("constructor"))) {
                        vmCodeInput("push pointer 0\n");
                        pushCounter++;
                    } else {
                        error("Error: in class: " + className + ", function calling for method happens in a non-method, line: "
                                + lexer.PeekNextToken().LineNumber);
                    }
                } else if (symbolTables[4].functionType.equals("function")) {
                    error("Error: in class: " + className + ", function called as a method in a function, line: "
                            + lexer.PeekNextToken().LineNumber);
                }
            }
            //Check the grammar of the argument list
            lexer.GetNextToken();
            while (!lexer.PeekNextToken().Token.equals(")")) {
                String currentArgumentType = lexer.PeekNextToken().Type.toString();

                expressionReturnType = "";

                expression();
                pushCounter++;
                if (!expressionReturnType.equals("")) {
                    if (expressionReturnType.equals("Array")) {
                        arguments += "int ";
                    } else {
                        arguments += expressionReturnType + " ";
                    }
                } else {
                    arguments += currentArgumentType + " ";
                }

                if (!lexer.PeekNextToken().Token.equals(",")) {
                    break;
                } else {
                    lexer.GetNextToken();
                }
            }
            if (!lexer.PeekNextToken().Token.equals(")")) {
                error("Error: in class: " + className +
                        ", \")\" is expected, line: " + lexer.PeekNextToken().LineNumber);
            } else {
                lexer.GetNextToken();
            }
            //Check if the arguments in the subroutine call matches with the definition of the subroutine
            if (!Pattern.compile(symbolTables[4].getArgument(className + "." + subroutineName)).matcher(arguments).matches()) {
                error("Error: in class: " + className +
                        ", function \"" + subroutineName + "\" got wrong arguments, line:" + lastToken.LineNumber);
            }
            expressionReturnType = symbolTables[4].getFunctionReturnType(subroutineName);

            vmCodeInput("call " + className + "." + subroutineName + " " + pushCounter + "\n");
            vmCodeInput("pop temp 0\n");
            pushCounter = 0;
        }
        symbolTableVarClear();
    }

    /*This method is used to check the grammar of the return statement
      and insert the VM codes for the return statement
    */
    private void returnStatementCheck() throws Exception {
        String valueReturned = "";
        if (!lexer.PeekNextToken().Token.equals(";")) {
            valueReturned = lexer.PeekNextToken().Token;
            expression();
        } else {
            expressionReturnType = "void";
            vmCodeInput("push constant 0\n");
        }
        //Check if the current subroutine is a constructor
        if (symbolTables[4].findFunctionSymbol(className + "." + functionName)
                && symbolTables[4].functionType.equals("constructor")
                && !valueReturned.equals("this")) {
            error("Error: in class: " + className +
                    ", the \"this\" is not referenced in a constructor, line: " +
                    lexer.PeekNextToken().LineNumber);
        }
        //Check if there is a subroutine returning this as the return value
        if (valueReturned.equals("this")
                && symbolTables[4].findFunctionSymbol(className + "." + functionName)
                && symbolTables[4].functionType.equals("function")) {
            error("Error: in class: " + className +
                    ", the \"this\" cannot be referenced in a function, line: " +
                    lexer.PeekNextToken().LineNumber);
        }

        //Check if the data type of the return value matches with the return of the subroutine in the definition of subroutine
        if (!expressionReturnType.equals(returnType)) {
            error("Error: data type of return value not matches with the definition of function, line: " + lexer.PeekNextToken().LineNumber);
        }

        if (!lexer.PeekNextToken().Token.equals(";")) {
            error("Error: in class: " + className + ", \";\" is expected, line: " + lexer.PeekNextToken().LineNumber);
        } else {
            lexer.GetNextToken();
        }
        if (oldToken.equals(";") || oldToken.equals("}") || oldToken.equals("{")) {
            if (!lexer.PeekNextToken().Token.equals("}")) {
                error("Error: in class: " + className + ", unreachable codes, line: " + lexer.GetNextToken().LineNumber);
            }
        }
        vmCodeInput("return\n");
    }

    //Divide expression into the relational expression
    private void expression() throws Exception {
        relationalExpression();
        String currentReturnType = expressionReturnType;
        while (lexer.PeekNextToken().Token.equals("&")
                || lexer.PeekNextToken().Token.equals("|")) {
            String relationSymbol = lexer.PeekNextToken().Token;

            lexer.GetNextToken();
            relationalExpression();
            //Check if the data types of two sides match with each other
            if (!((currentReturnType.equals("boolean") && expressionReturnType.equals("boolean"))
                    || (currentReturnType.equals("int") && expressionReturnType.equals("int")))) {
                error("Error: in class: " + className +
                        ", two ends of relational expression are not all int, line: " +
                        lexer.PeekNextToken().LineNumber);
            }
            if (relationSymbol.equals("&")) {
                vmCodeInput("and\n");
            } else {
                vmCodeInput("or\n");
            }
            expressionReturnType = "boolean";
        }

    }
    //Divide relational expression into the arithmetic expression
    private void relationalExpression() throws Exception {
        ArithmeticExpression();
        String currentReturnType = "";
        if (jackClasses.dataTypeTransfer.containsKey(expressionReturnType)) {
            currentReturnType = (String) jackClasses.dataTypeTransfer.get(expressionReturnType);
        } else {
            currentReturnType = (expressionReturnType + "|null|Array");
        }

        while (lexer.PeekNextToken().Token.equals("=")
                || lexer.PeekNextToken().Token.equals("<")
                || lexer.PeekNextToken().Token.equals(">")) {
            String relationSymbol = lexer.PeekNextToken().Token;
            lexer.GetNextToken();
            ArithmeticExpression();
            //Check if the data types of two sides match with each other
            if (relationSymbol.equals("=")) {
                if (!Pattern.compile(currentReturnType).matcher(expressionReturnType).matches()) {
                    error("Error: in class: " + className +
                            ", two ends of relational expression not have same type, line: " +
                            lexer.PeekNextToken().LineNumber);
                }
                vmCodeInput("eq\n");
            } else if (relationSymbol.equals("<")) {
                if (!Pattern.compile(currentReturnType).matcher(expressionReturnType).matches()) {
                    error("Error: in class: " + className +
                            ", two ends of relational expression are not all int, line: " +
                            lexer.PeekNextToken().LineNumber);
                }
                vmCodeInput("lt\n");
            } else {
                if (!Pattern.compile(currentReturnType).matcher(expressionReturnType).matches()) {
                    error("Error: in class: " + className +
                            ", two ends of relational expression are not all int, line: " +
                            lexer.PeekNextToken().LineNumber);
                }
                vmCodeInput("gt\n");
            }
            expressionReturnType = "boolean";
        }

    }
    //Divide arithmetic expression into the term
    private void ArithmeticExpression() throws Exception {
        term();

        if (lexer.PeekNextToken().Token.equals("+")
                || lexer.PeekNextToken().Token.equals("-")) {

            if (!(expressionReturnType.equals("int")
                    || expressionReturnType.equals("all"))) {
                error("Error: in class: " + className +
                        ", \"int\" type is expected, line " +
                        lexer.PeekNextToken().LineNumber);
            }
        }

        while (lexer.PeekNextToken().Token.equals("+")
                || lexer.PeekNextToken().Token.equals("-")) {
            String arithmeticSymbol = "";
            if (lexer.PeekNextToken().Token.equals("+")) {
                arithmeticSymbol = "add";
            } else {
                arithmeticSymbol = "sub";
            }
            lexer.GetNextToken();
            term();
            //Check if the data types of two sides match with each other
            if (!(expressionReturnType.equals("int")
                    || expressionReturnType.equals("all"))) {
                error("Error: in class: " + className +
                        ", \"int\" type is expected, line " +
                        lexer.PeekNextToken().LineNumber);
            }
            expressionReturnType = "int";
            vmCodeInput(arithmeticSymbol + "\n");
        }
    }
    //Divide term into the factor
    private void term() throws Exception {
        factor();
        if (lexer.PeekNextToken().Token.equals("*")
                || lexer.PeekNextToken().Token.equals("/")) {
            if (!(expressionReturnType.equals("int")
                    || expressionReturnType.equals("all"))) {
                error("Error: in class: " + className +
                        ", \"int\" type is expected, line " +
                        lexer.PeekNextToken().LineNumber);
            }
        }

        while (lexer.PeekNextToken().Token.equals("*")
                || lexer.PeekNextToken().Token.equals("/")) {

            String arithmeticSymbol = lexer.PeekNextToken().Token;

            lexer.GetNextToken();
            factor();
            //Check if the data types of two sides match with each other
            if (!(expressionReturnType.equals("int")
                    || expressionReturnType.equals("all"))) {
                error("Error: in class: " + className +
                        ", \"int\" type is expected, line " +
                        lexer.PeekNextToken().LineNumber);
            }

            if (arithmeticSymbol.equals("*")) {
                vmCodeInput("call Math.multiply 2\n");
            } else {
                vmCodeInput("call Math.divide 2\n");
            }
            expressionReturnType = "int";
        }
    }
    //Divide factor expression into the operand
    private void factor() throws Exception {
        String negOrNot = "";
        if (lexer.PeekNextToken().Token.equals("-")
                || lexer.PeekNextToken().Token.equals("~")) {
            negOrNot = lexer.PeekNextToken().Token;
            lexer.GetNextToken();
        }
        operand();
        //Check if the data types of two sides match with each other
        if (negOrNot.equals("-")) {
            if (!expressionReturnType.equals("int")) {
                error("Error: in class: " + className +
                        ", \"int\" type is expected, line " +
                        lexer.PeekNextToken().LineNumber);
            }
            vmCodeInput("neg\n");
        } else if (negOrNot.equals("~")) {
            if (!expressionReturnType.equals("boolean")) {
                error("Error: in class: " + className +
                        ", \"boolean\" type is expected, line " +
                        lexer.PeekNextToken().LineNumber);
            }
            vmCodeInput("not\n");
        }
    }
    //This method is used to process all kinds of operands
    private void operand() throws Exception {
        //Identify the type of the operand and insert the VM code into the VM file
        //If the operand is an integer
        if (lexer.PeekNextToken().Type.toString().equals("Constant")) {
            eleNumberCounter = Integer.parseInt(lexer.PeekNextToken().Token);
            expressionReturnType = "int";
            vmCodeInput("push constant " + lexer.PeekNextToken().Token + "\n");
            lexer.GetNextToken();
        //If the first of the operand is an identifier
        } else if (lexer.PeekNextToken().Type.toString().equals("ID")) {
            Token lastToken = lexer.PeekNextToken();
            String lastLexeme = lastToken.Token;
            lexer.GetNextToken();
            //If this operand has the format like 'identifier[[]|()]'
            if (!lexer.PeekNextToken().Token.equals(".")) {
                //Check if the operand is a variable by traversing all four symbol tables
                if (symbolTables[2].findIdentifierSymbol(lastToken.Token)
                        || symbolTables[3].findIdentifierSymbol(lastToken.Token)
                        || symbolTables[1].findIdentifierSymbol(lastToken.Token)
                        || symbolTables[0].findIdentifierSymbol(className + "." + lastToken.Token)) {
                    String memorySegment = "";
                    int offset = 0;
                    //Check if this variable is an array
                    if (lexer.PeekNextToken().Token.equals("[")) {
                        boolean variableOrNot = false;
                        for (int index = 3; index >= 0; index--) {
                            if (symbolTables[index].dataType.equals("Array")) {
                                memorySegment = symbolTables[index].memorySegment;
                                offset = symbolTables[index].offset;
                                variableOrNot = true;
                                break;
                            }
                            symbolTables[index].memorySegment = "";
                            symbolTables[index].dataType = "";
                            symbolTables[index].offset = 0;
                        }
                        if (!variableOrNot) {
                            error("Error: in class: " + className +
                                    ", variable \"" + lastLexeme + "\" not exists, line:" + lastToken.LineNumber);
                        }
                        lexer.GetNextToken();
                        expression();
                        vmCodeInput("push " + memorySegment + " " + offset + "\n");
                        if (!(expressionReturnType.equals("int")
                                || expressionReturnType.equals("all"))) {
                            error("Error: in class: " + className +
                                    ", \"int value\" is expected, line: " + lexer.PeekNextToken().LineNumber);
                        }
                        if (!lexer.PeekNextToken().Token.equals("]")) {
                            error("Error: in class: " + className +
                                    ", \"]\" is expected, line: " + lexer.PeekNextToken().LineNumber);
                        } else {
                            lexer.GetNextToken();

                            expressionReturnType = "all";
                        }

                        vmCodeInput("add\n");
                        vmCodeInput("pop pointer 1\n");
                        vmCodeInput("push that 0\n");
                    //Check if this operand is a variable
                    } else {
                        if (symbolTables[2].findIdentifierSymbol(lastLexeme)) {
                            if (!symbolTables[2].initOrNot) {
                                error("Error: in class: " + className +
                                        ", variable \"" + lastLexeme + "\" is not initialized, line: " + lastToken.LineNumber);
                            }
                            expressionReturnType = symbolTables[2].getIdentifierAssignDataType(lastLexeme);
                            vmCodeInput("push " + symbolTables[2].memorySegment + " " + symbolTables[2].offset + "\n");
                        } else if (symbolTables[3].findIdentifierSymbol(lastLexeme)) {
                            expressionReturnType = symbolTables[3].getIdentifierAssignDataType(lastLexeme);
                            vmCodeInput("push " + symbolTables[3].memorySegment + " " + symbolTables[3].offset + "\n");
                        } else if (symbolTables[1].findIdentifierSymbol(lastLexeme)) {
                            if (!symbolTables[1].initOrNot) {
                                error("Error: in class: " + className +
                                        ", variable \"" + lastLexeme + "\" is not initialized, line: " + lastToken.LineNumber);
                            }
                            expressionReturnType = symbolTables[1].getIdentifierAssignDataType(lastLexeme);
                            vmCodeInput("push " + symbolTables[1].memorySegment + " " + symbolTables[1].offset + "\n");
                        } else if (symbolTables[0].findIdentifierSymbol(className + "." + lastLexeme)) {
                            if (!symbolTables[0].initOrNot) {
                                error("Error: in class: " + className +
                                        ", variable \"" + lastLexeme + "\" is not initialized, line: " + lastToken.LineNumber);
                            }
                            expressionReturnType = symbolTables[0].getIdentifierAssignDataType(className + "." + lastLexeme);
                            vmCodeInput("push " + symbolTables[0].memorySegment + " " + symbolTables[0].offset + "\n");
                        }
                    }
                //Check if the operand is a subroutine
                } else if (symbolTables[4].findFunctionSymbol(className + "." + lastToken.Token)) {
                    //Check the grammar of this subroutine call
                    if (symbolTables[4].functionType.equals("method")) {
                        if (symbolTables[4].findFunctionSymbol(className + "." + functionName)
                                && (symbolTables[4].functionType.equals("method") || symbolTables[4].functionType.equals("constructor"))) {
                            vmCodeInput("push pointer 0\n");
                            pushCounter++;
                        } else {
                            error("Error: in class: " + className +
                                    ", function calling for method happens in a non-method, line: " + lexer.PeekNextToken().LineNumber);
                        }
                    } else if (symbolTables[4].functionType.equals("function")) {
                        error("Error: in class: " + className +
                                ", function called as a method in a function, line: " + lexer.PeekNextToken().LineNumber);
                    }
                    if (lexer.PeekNextToken().Token.equals("(")) {
                        String arguments = "";
                        lexer.GetNextToken();

                        while (!lexer.PeekNextToken().Token.equals(")")) {
                            String currentArgumentType = lexer.PeekNextToken().Type.toString();
                            expressionReturnType = "";
                            expression();
                            if (!expressionReturnType.equals("")) {
                                arguments += expressionReturnType + " ";
                            } else {
                                arguments += currentArgumentType + " ";
                            }

                            if (!lexer.PeekNextToken().Token.equals(",")) {
                                break;
                            } else {
                                lexer.GetNextToken();
                            }
                        }
                        if (!lexer.PeekNextToken().Token.equals(")")) {
                            error("Error: in class: " + className +
                                    ", \")\" is expected, line: " + lexer.PeekNextToken().LineNumber);
                        } else {
                            lexer.GetNextToken();
                        }

                        if (!Pattern.compile(symbolTables[4].getArgument(className + "." + lastToken.Token)).matcher(arguments).matches()) {
                            error("Error: in class: " + className +
                                    ", arguments' type not match, line: " + lexer.PeekNextToken().LineNumber);
                        }

                        expressionReturnType = symbolTables[4].getFunctionReturnType(className + "." + lastLexeme);
                        vmCodeInput("call " + className +
                                "." + lastLexeme + " " + symbolTables[4].numOfArgs + "\n");
                    }
                } else {
                    error("Error: in class: " + className +
                            ", variable or function: \"" + lastToken.Token + "\" is not defined, line: " + lastToken.LineNumber);
                }
            //Check if the operand has the format like 'identifier.identifier'
            } else {
                lexer.GetNextToken();
                if (!lexer.PeekNextToken().Type.toString().equals("ID")) {
                    error("Error: in class: " + className +
                            ", identifier is expected, line: " + lexer.PeekNextToken().LineNumber);
                } else {
                    lastLexeme += ("." + lexer.PeekNextToken().Token);
                    boolean staticVarOrNot = false;
                    boolean methodCallOrNot = false;
                    String methodCallLexeme = "";
                    String memorySegment = "";
                    int offset = 0;
                    //Check if the subroutine belongs to the Jack libraries
                    if (jackClasses.functionsArguments.containsKey(lastLexeme)) {
                        if (lastLexeme.equals("Array.new")) {
                            arrayInitOrNot = true;
                        }
                        lexer.GetNextToken();
                    //Check if the subroutine belongs to the subroutines defined before
                    } else if ((symbolTables[4].findFunctionSymbol(lastLexeme)
                            && (symbolTables[4].functionType.equals("function")
                            || symbolTables[4].functionType.equals("constructor")))
                            || symbolTables[0].findIdentifierSymbol(lastLexeme)) {
                        if (symbolTables[4].findFunctionSymbol(lastLexeme)) {
                        } else {
                            staticVarOrNot = true;
                            if (lexer.PeekNextToken().Token.equals("[")) {
                                boolean variableOrNot = false;
                                if (symbolTables[0].findIdentifierSymbol(lastLexeme)
                                        && symbolTables[0].dataType.equals("Array")) {
                                    variableOrNot = true;

                                }
                                if (!variableOrNot) {
                                    error("Error: in class: " + className + ", variable \"" + lastLexeme + "\" not exists, line: " + lastToken.LineNumber);
                                }
                                lexer.GetNextToken();
                                expression();

                                vmCodeInput("push " + memorySegment + " " + offset + "\n");
                                if (!(expressionReturnType.equals("int")
                                        || expressionReturnType.equals("all"))) {
                                    error("Error: in class: " + className +
                                            ", \"int value\" is expected, line: " + lexer.PeekNextToken().LineNumber);
                                }
                                if (!lexer.PeekNextToken().Token.equals("]")) {
                                    error("Error: in class: " + className +
                                            ", \"]\" is expected, line: " + lexer.PeekNextToken().LineNumber);
                                } else {
                                    lexer.GetNextToken();
                                    expressionReturnType = "all";
                                }

                                vmCodeInput("add\n");
                                vmCodeInput("pop pointer 1\n");
                                vmCodeInput("push that 0\n");
                            }
                        }
                        lexer.GetNextToken();
                    } else {
                        //Check if the subroutine call belongs to the calls for methods
                        methodCallOrNot = true;
                        String objectFunction = lastLexeme.split("\\.")[1];
                        if (symbolTables[2].findIdentifierSymbol(lastToken.Token)
                                && (symbolTables[4].findFunctionSymbol(symbolTables[2].dataType + "." + objectFunction)
                                || jackClasses.methodMatch(symbolTables[2].dataType + "." + objectFunction))) {
                            methodCallLexeme = symbolTables[2].dataType + "." + objectFunction;
                            if (!(symbolTables[4].functionType.equals("method")
                                    || jackClasses.methodMatch(symbolTables[2].dataType + "." + objectFunction))) {
                                error("Error: in class: " + className +
                                        ", function calling is not a method, line: " + lastToken.LineNumber);
                            } else {
                                vmCodeInput("push " + symbolTables[2].memorySegment + " " + symbolTables[2].offset + "\n");
                                pushCounter++;
                            }
                        } else if (symbolTables[3].findIdentifierSymbol(lastToken.Token)
                                && (symbolTables[4].findFunctionSymbol(symbolTables[3].dataType + "." + objectFunction)
                                || jackClasses.methodMatch(symbolTables[3].dataType + "." + objectFunction))) {
                            methodCallLexeme = symbolTables[3].dataType + "." + objectFunction;
                            if (!(symbolTables[4].functionType.equals("method")
                                    || jackClasses.methodMatch(symbolTables[3].dataType + "." + objectFunction))) {
                                error("Error: in class: " + className +
                                        ", function calling is not a method, line: " + lastToken.LineNumber);
                            } else {
                                vmCodeInput("push " + symbolTables[3].memorySegment + " " + symbolTables[3].offset + "\n");
                                pushCounter++;
                            }
                        } else if (symbolTables[1].findIdentifierSymbol(lastToken.Token)
                                && (symbolTables[4].findFunctionSymbol(symbolTables[1].dataType + "." + objectFunction)
                                || jackClasses.methodMatch(symbolTables[1].dataType + "." + objectFunction))) {
                            methodCallLexeme = symbolTables[1].dataType + "." + objectFunction;
                            if (!(symbolTables[4].functionType.equals("method")
                                    || jackClasses.methodMatch(symbolTables[1].dataType + "." + objectFunction))) {
                                error("Error: in class: " + className +
                                        ", function calling is not a method, line: " + lastToken.LineNumber);
                            } else {
                                vmCodeInput("push " + symbolTables[1].memorySegment + " " + symbolTables[1].offset + "\n");
                                pushCounter++;
                            }
                        } else {
                            error("Error: in class: " + className + ", function calling is not defined, line: " + lastToken.LineNumber);
                        }
                        lexer.GetNextToken();
                    }
                    //Check the grammar of the argument list
                    if (!staticVarOrNot && lexer.PeekNextToken().Token.equals("(")) {
                        String arguments = "";
                        if (methodCallOrNot) {
                            pushCounter = 1;
                        } else {
                            pushCounter = 0;
                        }
                        lexer.GetNextToken();

                        while (!lexer.PeekNextToken().Token.equals(")")) {
                            String currentArgumentType = lexer.PeekNextToken().Type.toString();
                            expressionReturnType = "";
                            int currentPushCounter = pushCounter;
                            expression();
                            pushCounter = currentPushCounter + 1;

                            if (!expressionReturnType.equals("")) {
                                arguments += expressionReturnType + " ";
                            } else {
                                arguments += currentArgumentType + " ";
                            }

                            if (!lexer.PeekNextToken().Token.equals(",")) {
                                break;
                            } else {
                                lexer.GetNextToken();
                            }
                        }

                        if (!lexer.PeekNextToken().Token.equals(")")) {
                            error("Error: in class: " + className + ", \")\" is expected, line: " + lexer.PeekNextToken().LineNumber);
                        } else {
                            lexer.GetNextToken();
                        }
                        //Check if the arguments in the subroutine call matches with the definitions of the subroutines
                        if (!(Pattern.compile(symbolTables[4].getArgument(lastLexeme)).matcher(arguments).matches()
                                || Pattern.compile(symbolTables[4].getArgument(methodCallLexeme)).matcher(arguments).matches())
                                && !((jackClasses.functionsArguments.get(lastLexeme) != null
                                && Pattern.compile(((String) jackClasses.functionsArguments.get(lastLexeme))).matcher(arguments).matches())
                                || (jackClasses.functionsArguments.get(methodCallLexeme) != null
                                && Pattern.compile(((String) jackClasses.functionsArguments.get(methodCallLexeme))).matcher(arguments).matches()))) {
                            error("Error: in class: " + className +
                                    ", function \"" + lexer.PeekNextToken().Token + "\" got wrong arguments, line:" + lexer.PeekNextToken().LineNumber);
                        }

                        if (jackClasses.returnTypes.containsKey(lastLexeme)) {
                            expressionReturnType = (String) jackClasses.returnTypes.get(lastLexeme);
                            vmCodeInput("call " + lastLexeme + " " + pushCounter + "\n");
                            pushCounter = 0;
                        } else if (!methodCallOrNot) {
                            expressionReturnType = symbolTables[4].getFunctionReturnType(lastLexeme);
                            vmCodeInput("call " + lastLexeme + " " + pushCounter + "\n");
                            pushCounter = 0;
                        } else {
                            expressionReturnType = symbolTables[4].getFunctionReturnType(methodCallLexeme);
                            if (expressionReturnType.equals("")) {
                                expressionReturnType = (String) jackClasses.returnTypes.get(methodCallLexeme);
                            }
                            vmCodeInput("call " + methodCallLexeme + " " + pushCounter + "\n");
                            pushCounter = 0;
                        }
                    }
                }
            }
        } else if (lexer.PeekNextToken().Token.equals("(")) {
            lexer.GetNextToken();
            expression();
            if (!lexer.PeekNextToken().Token.equals(")")) {
                error("Error: in class: " + className + ", \")\" is expected, line: " + lexer.PeekNextToken().LineNumber);
            } else {
                lexer.GetNextToken();
            }
        //Check if the operand is a string and insert VM codes for string
        } else if (lexer.PeekNextToken().Type.toString().equals("String")) {
            expressionReturnType = "String";
            String currentString = lexer.PeekNextToken().Token;
            vmCodeInput("push constant " + (currentString.length() - 2) + "\n");
            vmCodeInput("call String.new 1\n");
            for (int index = 1; index < currentString.length() - 1; index++) {
                vmCodeInput("push constant " + (int) currentString.charAt(index) + "\n");
                vmCodeInput("call String.appendChar 2\n");
            }
            lexer.GetNextToken();
        //Check if the operand is a boolean variable and insert VM codes for boolean
        } else if (lexer.PeekNextToken().Token.equals("true")) {
            expressionReturnType = "boolean";
            lexer.GetNextToken();
            vmCodeInput("push constant 0\n");
            vmCodeInput("not\n");
        //Check if the operand is a boolean variable and insert VM codes for boolean
        } else if (lexer.PeekNextToken().Token.equals("false")) {
            expressionReturnType = "boolean";
            lexer.GetNextToken();
            vmCodeInput("push constant 0\n");
        //Check if the operand is null and insert VM codes for null
        } else if (lexer.PeekNextToken().Token.equals("null")) {
            expressionReturnType = "null";
            lexer.GetNextToken();
            vmCodeInput("push constant 0\n");
        //Check if the operand is this and insert VM codes for this
        } else if (lexer.PeekNextToken().Token.equals("this")) {
            expressionReturnType = className;
            lexer.GetNextToken();
            vmCodeInput("push pointer 0\n");
        }
        symbolTableVarClear();
    }

}
