import java.io.File;

/**
 *
 * @author wangjs
 */
//This is main class used to call other parts of the compiler
public class Compiler {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //Read the files stored in the directory provided
        File files = new File(args[0]);
        File[] allFile = files.listFiles();
        /*Traverse the files in the directory. 
        Use lexer to produce the tokens and use parser to check the programming language grammar.
        Then check whether there are lots of sementic mistakes and produce the vm codes*/
        for (File f : allFile) {
            if (f.isFile() && f.getName().contains(".jack")) {
                Lexer lexer = new Lexer(args[0], f.getName());
                Parser parser = new Parser(lexer);
            }
        }

    }

}
