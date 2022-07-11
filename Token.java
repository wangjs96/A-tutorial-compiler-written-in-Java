import java.util.HashMap;
import java.util.Map;
/**
 *
 * @author wangjs
 */
public class Token {
    //Enum used to identify the type of tokens
    public enum TokenType{ID,Symbol,Keyword,EOF,Constant,Comment,String,Error,Null};
    //Enum used to identify the specific operators
    public enum SymbolType{assignment,addition,multiplication,subtraction,division,
        equal,parentheses,braces,brackets,and,or,not,lower,larger,one_line_com,
        multi_line_com,API_com,com_e,semicolon,dot,quotation,dquotation,comma,line_break};
    //Array used to store the keywords
    final public String[] Keywords = {"class","constructor","method","function","int",
        "boolean","char","void","var","static","field","let","do","if","else",
        "while","return","true","false","null","this"};
    //Hashmap used to construct the map between the symbols and their usages
    final public Map Symbols = new HashMap();

    public String Token = "";
    public TokenType Type = null;
    public int LineNumber = 0;
    public SymbolType SymbolType = null;
    //Initialize the hashmap
    public Token(){
        initSymbols();
    }
    private void initSymbols(){
        Symbols.put("(", SymbolType.parentheses);
        Symbols.put(")", SymbolType.parentheses);
        Symbols.put("{", SymbolType.braces);
        Symbols.put("}", SymbolType.braces);
        Symbols.put("[", SymbolType.brackets);
        Symbols.put("]", SymbolType.brackets);
        Symbols.put("=", SymbolType.assignment);
        Symbols.put("+", SymbolType.addition);
        Symbols.put("-", SymbolType.subtraction);
        Symbols.put("*", SymbolType.multiplication);
        Symbols.put("/", SymbolType.division);
        Symbols.put("&", SymbolType.and);
        Symbols.put("|", SymbolType.or);
        Symbols.put("~", SymbolType.not);
        Symbols.put(">", SymbolType.larger);
        Symbols.put("<", SymbolType.lower);
        Symbols.put("//", SymbolType.one_line_com);
        Symbols.put("/*", SymbolType.multi_line_com);
        Symbols.put("/**", SymbolType.API_com);
        Symbols.put("*/",SymbolType.com_e);
        Symbols.put("==", SymbolType.equal);
        Symbols.put(";", SymbolType.semicolon);
        Symbols.put(".",SymbolType.dot);
        Symbols.put("\"",SymbolType.dquotation);
        Symbols.put("'",SymbolType.quotation);
        Symbols.put(",",SymbolType.comma);
        Symbols.put("\\n",SymbolType.line_break);
    }
    //Set the values of the parameters of token object
    public void setToken(String Token,TokenType Type,int LineNumber){
        this.Token = Token;
        this.Type = Type;
        this.LineNumber = LineNumber;
    }
    public void changeTokenContent(){
        TokenType Type = null;
        this.Token = "";
        this.Type = Type.Null;
    }
    //Print out the contents stored in the each token object
    public void print(){
        if(!Token.equals("")&&Type != null)
            System.out.println("<"+Token+","+Type.toString()+","+LineNumber+">");
    }
}
