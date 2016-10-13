import java.io.PrintStream;
import java.util.Optional;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import tokenizer.Token;
import tokenizer.Token.Type;
import tokenizer.Tokenizer;

public class Basic {
	private static void prova(String s) throws Exception {
		Tokenizer x = new Tokenizer(s);
		Token token;
		while ((token = x.nextToken()).getType() != Token.Type.EOF) {
			System.out.println("Type: " + token.getType().toString() + " Value: " + token.getValue().toString());

			System.out.println(token.getValue().toString().toUpperCase().replace("OPTIONAL[", "").replace("]", ""));
		}
	}

	private static void exp(String s) {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");
		try {
			System.out.println(engine.eval(s));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Interpreter i = new Interpreter();
		i.welcome();
		i.shell();
	}
}
