import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import static java.lang.System.exit;
import java.util.regex.Pattern;
/**
 *
 * @author wangjs
 */
//This class is used to read each char and get the tokens
public class Lexer {
    //Encoding format
    private String encoding;
    //File path
    private String path;
    //Directory path
    private static String folderPath;
    //File object
    private File file;
    //Variable used to store the line number
    private int lineNumber;
    //String variable used to store the text content stored in the source code file
    private String textContent;
    //Variable used to store the index used to record which characters have been accessed
    private int index;
    //Variable used to control whether the line number is needed to be increased
    public static boolean newLineCheck = true;
    //Constructor for initializing the lexer when parser needs to traverse the tokens from specific file
    public Lexer(String fileName) {
        encoding = "GBK";
        path = folderPath + File.separator + fileName;
        file = new File(path);
        textContent = "";
        lineNumber = 1;
        index = 0;
    }
    //Constructor for initializing the lexer when file is read from the specific directory
    public Lexer(String folderPath, String fileName) {
        encoding = "GBK";
        this.folderPath = folderPath; 
        path = folderPath + file.separator + fileName;
        file = new File(path);
        textContent = "";
        lineNumber = 1;
        index = 0;
    }

    //Read the source code from the file into the program
    public void initLocalFile() {
        try {
            //String variable used to store the source codes
            textContent = "";
            //Check whether the file read is available
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineText = null;
                while ((lineText = bufferedReader.readLine()) != null) {
                    textContent += (lineText + "\\n");
                }
                read.close();
            } else {
                System.out.println("Cannot find the file");
            }
        } catch (Exception e) {
            System.out.println("Error: class not exists.");
            e.printStackTrace();
        }
    }
    //Method used to get the folder path
    public String getFolderPath() {
        return folderPath;
    }
    //Change the index used to record the number of characters which have been read already
    public void setReadIndex(int value) {
        index = value;
    }
    //Method used to return the current index for recording the number of characters which have been read already
    public int getReadIndex() {
        return index;
    }
    //Method used to return the source codes
    public String getTextContent() {
        return textContent;
    }
    //Method used to create the error information when the format of comments are not suitable
    private void error(String errorInfor) {
        System.out.printf("%s", errorInfor);
        exit(0);
    }

    //Method used to operate the token exactly
    private Token TokenOperation(String operationType) {
        Token token = new Token();
        String TokenName = "";
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]+$");
        while (index < textContent.length() - 2) {
            if (textContent.charAt(index) == ' ') {
                index++;
                continue;
            } else if (Character.isLowerCase(textContent.charAt(index))
                    || Character.isUpperCase(textContent.charAt(index))
                    || Character.isDigit(textContent.charAt(index))) {
                TokenName += textContent.charAt(index);
                index++;

                if (index < textContent.length()
                        && !Character.isLowerCase(textContent.charAt(index))
                        && !Character.isUpperCase(textContent.charAt(index))
                        && !Character.isDigit(textContent.charAt(index))) {
                    break;
                }
            } else {
                TokenName += textContent.charAt(index);
                TokenName = TokenName.trim();
                index++;
                if (token.Symbols.containsKey(TokenName)) {
                    if (TokenName.startsWith("/")) {
                        if (TokenName.equals("/")) {
                            if (Character.isLowerCase(textContent.charAt(index))
                                    || Character.isUpperCase(textContent.charAt(index))
                                    || Character.isDigit(textContent.charAt(index))
                                    || textContent.charAt(index) == ' ') {
                                break;
                            } else {
                                continue;
                            }
                        } else if (TokenName.equals("//") || TokenName.equals("/*")) {
                            if (TokenName.equals("/*")) {
                                if (textContent.indexOf("*/", index) == -1) {
                                    error("Error: comment doesn't have the end symbol, line: " + lineNumber);
                                }
                                String comment = textContent.substring(index,
                                        textContent.indexOf("*/", index));
                                String[] comment_s = comment.split("\\\\n");
                                lineNumber += comment_s.length - 1;

                                TokenName += comment + "*/";
                                index = textContent.indexOf("*/", index) + 2;

                                TokenName = "";
                                continue;
                            } else {
                                Token com_token = new Token();
                                String comment = textContent.substring(index,
                                        textContent.indexOf("\\n", index));
                                TokenName += comment;
                                index = textContent.indexOf("\\n", index);
                                TokenName = "";
                                continue;
                            }
                        }
                    } else if (TokenName.startsWith("\"")) {
                        Token string_token = new Token();
                        int i = textContent.indexOf("\"", index);
                        if (i > 0) {
                            TokenName += textContent.substring(index, i + 1);
                            string_token.setToken(TokenName,
                                    string_token.Type.String, lineNumber);
                            index = i + 1;
                            TokenName = "";
                            return string_token;
                        }
                        break;
                    } else {
                        break;
                    }
                } else {
                    if (TokenName.startsWith("\\")) {
                        if (textContent.charAt(index) == 'n') {
                            if (newLineCheck && operationType.equals("get")) {
                                lineNumber++;
                            }
                            TokenName = "";
                            index++;
                            continue;
                        }
                    }
                }
            }
        }
        //Identify the type of the token and return the token
        if (!TokenName.equals("")) {
            TokenName = TokenName.trim();

            while (true) {
                if (token.Symbols.containsKey(TokenName)) {

                    token.setToken(TokenName, token.Type.Symbol, lineNumber);
                    break;
                } else {
                    for (String keyword : token.Keywords) {
                        if (keyword.equals(TokenName)) {
                            token.setToken(TokenName, token.Type.Keyword, lineNumber);
                        }
                    }
                    if (token.Token != "") {
                        break;
                    }
                }
                if (pattern.matcher(TokenName).matches()) {
                    token.setToken(TokenName, token.Type.Constant, lineNumber);
                    break;
                } else {
                    token.setToken(TokenName, token.Type.ID, lineNumber);
                    break;
                }
            }
        }
        return token;
    }

    //Get the next token from the source code and move the index
    public Token GetNextToken() {
        return TokenOperation("get");
    }

    //Peek the next token but not move the index
    public Token PeekNextToken() {
        int OldIndex = index;
        Token token = TokenOperation("peek");
        index = OldIndex;
        return token;
    }
}
