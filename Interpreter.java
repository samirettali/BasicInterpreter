import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import tokenizer.Token;
import tokenizer.Token.Type;
import tokenizer.Tokenizer;

public class Interpreter {
	Tokenizer tokenizer;
	Token token;
	HashMap<Integer, String> instructions;
	HashMap<String, Token> variables;
	HashMap<Integer, Integer> gosub;
	HashMap<String, FOR> forh;
	int instructionPointer;
	int freeMemory = 65536;
	boolean execute;

	Interpreter() {
		reset();
	}

	private void reset() {
		this.instructions = new HashMap<Integer, String>();
		this.variables = new HashMap<String, Token>();
		this.gosub = new HashMap<Integer, Integer>();
		this.forh = new HashMap<String, FOR>();
		this.freeMemory = 65536;
		this.instructionPointer = 1;
	}

	public void consume() {
		try {
			this.token = this.tokenizer.nextToken();
		} catch (IOException e) {
			err("NO TOKENS AVAILABLE ON LINE " + this.instructionPointer);
		}
	}

	public void run() {
		System.out.println();

		this.instructionPointer = 0;
		if (this.instructions.size() > 0) {
			inc();
			if (checkEnd()) {
				this.execute = true;
			}
			while (this.execute) {
				String currentLine = (String) this.instructions.get(Integer.valueOf(this.instructionPointer));
				this.tokenizer = new Tokenizer(currentLine);

				consume();
				if (this.token.getType() == Token.Type.KEYWORD) {
					String keyword = this.token.getValue().toString().toUpperCase().replace("OPTIONAL[", "")
							.replace("]", "").trim();
					switch (keyword) {
					case "PRINT":
						PRINT();
						break;
					case "LET":
						LET();
						break;
					case "GOTO":
						GOTO();
						break;
					case "IF":
						IF();
						break;
					case "FOR":
						FOR();
						break;
					case "NEXT":
						NEXT();
						break;
					case "INPUT":
						INPUT();
						break;
					case "GOSUB":
						GOSUB();
						break;
					case "RETURN":
						RETURN();
						break;
					case "END":
						stop();
						break;
					case "REM":
						inc();
						break;
					default:
						err("SYNTAX ERROR ON LINE " + this.instructionPointer);
						stop();
					}
				} else {
					exp(Token.Type.KEYWORD);
				}
			}
			ok();
		} else {
			err("NO INSTRUCTIONS");
		}
	}

	private String expression() {
		String e = "(";
		int lpar = 1;
		do {
			consume();
			if (this.token.getType() == Token.Type.LPAREN) {
				lpar++;
				e = e + "(";
			} else if ((lpar > 0) && (this.token.getType() == Token.Type.RPAREN)) {
				lpar--;
				e = e + ")";
			} else if (this.token.getType() == Token.Type.VAR) {
				String variableName = (String) this.token.getValue().get();
				if (this.variables.containsKey(variableName)) {
					Token variable = (Token) this.variables.get(variableName);
					if (variable.getType() == Token.Type.NUMBER) {
						e = e + (String) variable.getValue().get();
					} else {
						exp(Token.Type.NUMBER);
					}
				} else {
					undef();
				}
			} else if (this.token.getType() == Token.Type.PLUS) {
				e = e + "+";
			} else if (this.token.getType() == Token.Type.MINUS) {
				e = e + "-";
			} else if (this.token.getType() == Token.Type.MULT) {
				e = e + "*";
			} else if (this.token.getType() == Token.Type.DIV) {
				e = e + "/";
			} else if (this.token.getType() == Token.Type.MOD) {
				e = e + "%";
			} else if (this.token.getType() == Token.Type.KEYWORD) {
				e = e + (String) this.token.getValue().get();
				expect(Token.Type.LPAREN);
				if (this.execute) {
					e = e + "(";
					do {
						if (this.token.getType().equals(Token.Type.VAR)) {
							String variableName = (String) this.token.getValue().get();
							if (this.variables.containsKey(variableName)) {
								Token variable = (Token) this.variables.get(variableName);
								if (variable.getType() == Token.Type.NUMBER) {
									e = e + (String) variable.getValue().get();
								} else {
									System.out.println("Unexpected " + variable.getType() + " on line "
											+ this.instructionPointer + ", expecting " + Token.Type.NUMBER.toString());
								}
							} else {
								undef();
							}
						} else if (this.token.getType() == Token.Type.NUMBER) {
							e = e + (String) this.token.getValue().get();
						} else if (this.token.getType() == Token.Type.COMMA) {
							e = e + ",";
						} else if (this.token.getType() == Token.Type.PLUS) {
							e = e + "+";
						} else if (this.token.getType() == Token.Type.MINUS) {
							e = e + "-";
						} else if (this.token.getType() == Token.Type.MULT) {
							e = e + "*";
						} else if (this.token.getType() == Token.Type.DIV) {
							e = e + "/";
						}
						consume();
					} while (this.token.getType() != Token.Type.RPAREN);
					e = e + ")";
				}
			} else if (this.token.getType() == Token.Type.NUMBER) {
				e = e + (String) this.token.getValue().get();
			}
		} while ((lpar > 0) || (this.token.getType() != Token.Type.RPAREN));
		e = e.replaceAll("POW", "Math.pow");
		e = e.replaceAll("SQRT", "Math.sqrt");
		e = e.replaceAll("ABS", "Math.abs");
		e = e.replaceAll("COS", "Math.cos");
		e = e.replaceAll("SIN", "Math.sin");
		e = e.replaceAll("LOG", "Math.log");
		e = e.replaceAll("MIN", "Math.min");
		e = e.replaceAll("MAX", "Math.mac");
		e = e.replaceAll("ACOS", "Math.acos");
		e = e.replaceAll("ASIN", "Math.asin");
		e = e.replaceAll("ATAN", "Math.atan");
		e = e.replaceAll("CEIL", "Math.ceil");
		e = e.replaceAll("ROUND", "Math.round");
		e = e.replaceAll("TAN", "Math.tan");
		e = e.replaceAll("FLOOR", "Math.floor");

		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");
		try {
			return engine.eval(e).toString();
		} catch (Exception ex) {
			stop();

			stop();
		}
		return "EXPRESSION ERROR";
	}

	private void PRINT() {
		if (!this.execute) {
			return;
		}
		String finalString = "";

		String currentLine = (String) this.instructions.get(Integer.valueOf(this.instructionPointer));

		String expression = "";

		float total = 0.0F;

		consume();
		while (this.token.getType() != Token.Type.EOF) {
			while (this.token.getType() == Token.Type.SEMICOMMA) {
				consume();
			}
			if (null != this.token.getType()) {
				switch (this.token.getType()) {
				case VAR:
					if (this.variables.containsKey(this.token.getValue().get())) {
						finalString = finalString
								+ (String) ((Token) this.variables.get(this.token.getValue().get())).getValue().get();
					} else {
						undef();
					}
					break;
				case STRING:
					finalString = finalString + (String) this.token.getValue().get();
					break;
				case LPAREN:
					String expressionResult = expression();
					if (!expressionResult.equals("EXPRESSION ERROR")) {
						finalString = finalString + expressionResult;
					} else {
						System.out.println("EXPRESSION ERROR");
					}
					break;
				case NUMBER:
					finalString = finalString + (String) this.token.getValue().get();
					break;
				default:
					exp(Token.Type.VAR.toString() + " or " + Token.Type.STRING.toString() + " or "
							+ Token.Type.LPAREN.toString() + " to start a mathematical expression");
				}
			}
			consume();
		}
		if (this.execute) {
			System.out.println(finalString);
		}
		inc();
	}

	private void LET() {
		consume();

		String variableName = ((String) this.token.getValue().get()).replaceAll("$", "");
		String variableValue = null;
		Token finalVariable = null;

		expect(Token.Type.EQ);
		if (this.execute) {
			consume();
			switch (this.token.getType().toString()) {
			case "VAR":
				if (this.variables.containsKey(this.token.getValue().get())) {
					Token variableToGet = (Token) this.variables.get(this.token.getValue().get());
					variableValue = (String) this.token.getValue().get();
					finalVariable = new Token(variableToGet.getType(), (String) variableToGet.getValue().get());
					if (this.variables.containsKey(variableName)) {
						this.variables.put(variableName, finalVariable);
					} else if (occupyMemory(finalVariable)) {
						this.variables.put(variableName, finalVariable);
					}
				} else {
					undef();
				}
				break;
			case "NUMBER":
				variableValue = (String) this.token.getValue().get();
				finalVariable = new Token(Token.Type.NUMBER, variableValue);
				if (this.variables.containsKey(variableName)) {
					this.variables.put(variableName, finalVariable);
				} else if (occupyMemory(finalVariable)) {
					this.variables.put(variableName, finalVariable);
				}
				break;
			case "STRING":
				variableValue = (String) this.token.getValue().get();
				finalVariable = new Token(Token.Type.STRING, variableValue);
				if (this.variables.containsKey(variableName)) {
					this.variables.put(variableName, finalVariable);
				} else if (occupyMemory(finalVariable)) {
					this.variables.put(variableName, finalVariable);
				}
				break;
			case "LPAREN":
				String result = expression();
				finalVariable = new Token(Token.Type.NUMBER, result);
				if (this.variables.containsKey(variableName)) {
					this.variables.put(variableName, finalVariable);
				} else if (occupyMemory(finalVariable)) {
					this.variables.put(variableName, finalVariable);
				}
				break;
			case "KEYWORD":
				if (this.token.getValue().toString().toUpperCase().replace("OPTIONAL[", "").replace("]", "").trim()
						.equals("CONCAT")) {
					String finalString = "";
					expect(Token.Type.LPAREN);
					consume();
					do {
						if (this.token.getType() == Token.Type.VAR) {
							String a = (String) this.token.getValue().get();
							if (this.variables.containsKey(a)) {
								finalString = finalString + (String) ((Token) this.variables.get(a)).getValue().get();
							} else {
								undef();
							}
							consume();
						} else if ((this.token.getType() == Token.Type.NUMBER)
								|| (this.token.getType() == Token.Type.STRING)) {
							finalString = finalString + (String) this.token.getValue().get();
							consume();
						} else if (this.token.getType() == Token.Type.COMMA) {
							consume();
						} else {
							exp("VAR, NUMBER, STRING or COMMA to concatenate");
						}
					} while ((this.execute) && (this.token.getType() != Token.Type.RPAREN)
							&& (this.token.getType() != Token.Type.EOF));
					if ((this.execute) && (this.token.getType() == Token.Type.RPAREN)) {
						finalVariable = new Token(Token.Type.STRING, finalString);
						this.variables.put(variableName, finalVariable);
						if (this.variables.containsKey(variableName)) {
							this.variables.put(variableName, finalVariable);
						} else if (occupyMemory(finalVariable)) {
							this.variables.put(variableName, finalVariable);
						}
					} else {
						exp(Token.Type.RPAREN);
					}
				} else {
					erl("THE ONLY ALLOWED KEYWORD IS CONCAT()");
				}
				break;
			default:
				exp(Token.Type.STRING.toString() + ", " + Token.Type.NUMBER.toString() + ", "
						+ Token.Type.VAR.toString() + " or " + Token.Type.LPAREN.toString()
						+ " to start a mathematical expression");
				stop();
			}
		}
		if (this.execute) {
			expect(Token.Type.EOF);
		}
		inc();
	}

	private void GOTO() {
		consume();
		try {
			int line = Integer.valueOf((String) this.token.getValue().get()).intValue();
			if (this.instructions.containsKey(Integer.valueOf(line))) {
				this.instructionPointer = Integer.valueOf((String) this.token.getValue().get()).intValue();
			} else {
				err("GOTO ERROR ON LINE " + this.instructionPointer + ": LINE " + line + " DOESN'T EXIST");
			}
		} catch (NumberFormatException e) {
			err("GOTO ERROR ON LINE " + this.instructionPointer + ": THE NUMBER FORMAT IS WRONG");
		}
	}

	private void LIST() {
		System.out.println();
		if (this.instructions.size() > 0) {
			Set<Integer> set = this.instructions.keySet();

			int[] keys = new int[this.instructions.size()];

			int index = 0;
			for (Iterator localIterator = set.iterator(); localIterator.hasNext();) {
				int key = ((Integer) localIterator.next()).intValue();
				keys[(index++)] = key;
			}
			Arrays.sort(keys);
			for (int i = 0; i < keys.length; i++) {
				System.out.println(keys[i] + " " + (String) this.instructions.get(Integer.valueOf(keys[i])));
			}
		} else {
			err("NO INSTRUCTIONS");
		}
		ok();
	}

	private void FOR() {
		String value = "";
		float limit = 0.0F;
		float step = 1.0F;

		expect(Token.Type.VAR);
		String variableName = (String) this.token.getValue().get();

		expect(Token.Type.EQ);

		consume();
		if (this.token.getType() == Token.Type.NUMBER) {
			value = (String) this.token.getValue().get();
		} else if (this.token.getType() == Token.Type.VAR) {
			String varName = (String) this.token.getValue().get();
			if (this.variables.containsKey(varName)) {
				Token var = (Token) this.variables.get(varName);
				value = (String) var.getValue().get();
			} else {
				undef();
			}
		} else if (this.token.getType() == Token.Type.LPAREN) {
			value = expression();
		} else {
			exp("NUMBER, VAR or LPAREN to start a mathematical expression");
		}
		Token var = new Token(Token.Type.NUMBER, value);
		this.variables.put(variableName, var);

		expect(Token.Type.KEYWORD);
		consume();
		if (this.token.getType() == Token.Type.NUMBER) {
			limit = Float.parseFloat((String) this.token.getValue().get());
		} else if (this.token.getType() == Token.Type.VAR) {
			String varName = (String) this.token.getValue().get();
			if (this.variables.containsKey(varName)) {
				Token to = (Token) this.variables.get(varName);
				limit = Float.parseFloat((String) to.getValue().get());
			} else {
				undef();
			}
		} else if (this.token.getType() == Token.Type.LPAREN) {
			limit = Float.parseFloat(expression());
		} else {
			exp(Token.Type.NUMBER.toString() + " or " + Token.Type.VAR.toString());
		}
		if (this.execute) {
			int counter = this.instructionPointer;
			int forLine = this.instructionPointer;

			int fors = 0;
			do {
				do {
					counter++;
				} while ((!this.instructions.containsKey(Integer.valueOf(counter))) && (counter <= 1000));
				if ((counter <= 1000) && (((String) this.instructions.get(Integer.valueOf(counter))).contains("FOR"))) {
					fors++;
				} else if ((counter <= 1000) && (fors > 0)
						&& (((String) this.instructions.get(Integer.valueOf(counter))).contains("NEXT"))) {
					fors--;
				}
			} while ((counter <= 1000) && ((fors > 0)
					|| (!((String) this.instructions.get(Integer.valueOf(counter))).trim().contains("NEXT"))));
			if (counter > 1000) {
				err("LINE " + this.instructionPointer + ": FOR WITHOUT CORRESPONDING NEXT");
			}
			if (((String) this.instructions.get(Integer.valueOf(this.instructionPointer))).contains("STEP")) {
				expect(Token.Type.KEYWORD);
				if (((String) this.token.getValue().get()).contains("STEP")) {
					expect(Token.Type.NUMBER);
					step = Float.parseFloat((String) this.token.getValue().get());
				}
			}
			inc();
			if (step == 0.0F) {
				err("LINE " + this.instructionPointer + ": STEP VALUE CAN NOT BE 0");
				stop();
			} else {
				FOR f = new FOR(Float.parseFloat(value), limit, step, this.instructionPointer, counter);
				this.forh.put(variableName, f);
			}
		}
	}

	private void NEXT() {
		expect(Token.Type.VAR);
		if (this.execute) {
			String variableName = (String) this.token.getValue().get();
			if (this.forh.containsKey(variableName)) {
				FOR f = (FOR) this.forh.get(variableName);
				f.increase();
				if (f.isEnded()) {
					this.forh.put(variableName, f);
					Token updatedVariable = new Token(Token.Type.NUMBER, Float.toString(f.getVariableValue()));
					this.variables.put(variableName, updatedVariable);
					this.instructionPointer = f.getForLine();
				} else {
					inc();
				}
			} else {
				err("NEXT WITHOUT CORRESPONDING FOR ON LINE " + this.instructionPointer);
			}
		}
	}

	private void OLDIF() {
		String s1 = "";
		String s2 = "";

		consume();
		Token leftExp = this.token;
		Token.Type leftType = leftExp.getType();
		if (leftType == Token.Type.LPAREN) {
			s1 = expression();
		} else if ((leftType == Token.Type.VAR) && (this.variables.containsKey(this.token.getValue().get()))) {
			s1 = (String) ((Token) this.variables.get(this.token.getValue().get())).getValue().get();
		} else if ((leftType == Token.Type.STRING) || (leftType == Token.Type.NUMBER)) {
			s1 = (String) this.token.getValue().get();
		} else {
			exp("STRING, NUMBER OR VAR");
			stop();
		}
		consume();
		Token condition = this.token;

		consume();
		Token rightExp = this.token;
		Token.Type rightType = rightExp.getType();
		if (rightType == Token.Type.LPAREN) {
			s2 = expression();
		} else if ((rightType == Token.Type.VAR) && (this.variables.containsKey(this.token.getValue().get()))) {
			s2 = (String) ((Token) this.variables.get(this.token.getValue().get())).getValue().get();
		} else if ((rightType == Token.Type.STRING) || (rightType == Token.Type.NUMBER)) {
			s2 = (String) this.token.getValue().get();
		} else {
			exp("STRING, NUMBER OR VAR");
			stop();
		}
		float n1 = 0.0F;
		float n2 = 0.0F;

		boolean isN1 = false;
		boolean isN2 = false;

		boolean success = false;
		if (((leftType == Token.Type.NUMBER) || (leftType == Token.Type.LPAREN))
				&& ((rightType == Token.Type.NUMBER) || (rightType == Token.Type.LPAREN))) {
			try {
				n1 = Float.valueOf(s1).floatValue();
				isN1 = true;
			} catch (NumberFormatException e) {
				erl("FIRST OPERAND NUMBER FORMAT ERROR");
			}
			try {
				n2 = Float.valueOf(s2).floatValue();
				isN2 = true;
			} catch (NumberFormatException e) {
				erl("SECOND OPERAND NUMBER FORMAT ERROR");
			}
			switch (condition.getType().toString()) {
			case "EQ":
				if (n1 == n2) {
					success = true;
				}
				break;
			case "NE":
				if (n1 != n2) {
					success = true;
				}
				break;
			case "GT":
				if (n1 > n2) {
					success = true;
				}
				break;
			case "GTE":
				if (n1 >= n2) {
					success = true;
				}
				break;
			case "LT":
				if (n1 < n2) {
					success = true;
				}
				break;
			case "LTE":
				if (n1 <= n2) {
					success = true;
				}
				break;
			}
		} else {
			switch (condition.getType().toString()) {
			case "EQ":
				if (leftExp.equals(rightExp)) {
					success = true;
				}
				break;
			case "NE":
				if (!leftExp.equals(rightExp)) {
					success = true;
				}
				break;
			}
		}
		boolean gt = false;

		consume();
		if ((success) && (this.token.getType() == Token.Type.KEYWORD)
				&& (((String) this.token.getValue().get()).equals("THEN"))) {
			consume();
			if (this.token.getType() == Token.Type.NUMBER) {
				this.instructionPointer = Integer.valueOf((String) this.token.getValue().get()).intValue();
				gt = true;
			}
			if (this.token.getType() == Token.Type.KEYWORD) {
				String keyword = this.token.getValue().toString().toUpperCase().replace("OPTIONAL[", "")
						.replace("]", "").trim();
				if (keyword.equals("PRINT")) {
					PRINT();
					gt = true;
				}
				if (keyword.equals("GOSUB")) {
					GOSUB();
					gt = true;
				}
				if (keyword.equals("LET")) {
					LET();
					gt = true;
				}
				if (keyword.equals("IF")) {
					IF();
					gt = true;
				}
			}
		} else if ((success) && ((this.token.getType() != Token.Type.KEYWORD)
				|| (((String) this.token.getValue().get()).equals("THEN")))) {
			exp("THEN");
		}
		if (!gt) {
			inc();
		}
	}

	private void IF()
  {
    String s1 = "";
    String s2 = "";
    float n1 = 0.0F;
    float n2 = 0.0F;
    boolean isN1 = false;
    boolean isN2 = false;
    
    boolean success = false;
    
    consume();
    Token leftExp = this.token;
    Token.Type leftType = leftExp.getType();
    if ((leftType == Token.Type.STRING) || (leftType == Token.Type.NUMBER))
    {
      s1 = (String)this.token.getValue().get();
    }
    else if (leftType == Token.Type.LPAREN)
    {
      s1 = expression();
      leftType = Token.Type.NUMBER;
    }
    else if ((leftType == Token.Type.VAR) && (this.variables.containsKey(this.token.getValue().get())))
    {
      String variableName = (String)this.token.getValue().get();
      Token variable = (Token)this.variables.get(variableName);
      s1 = (String)variable.getValue().get();
      leftType = variable.getType();
    }
    else
    {
      exp("STRING, NUMBER, VAR or LPAREN to start a mathematical expression");
    }
    consume();
    Token condition = this.token;
    
    consume();
    Token rightExp = this.token;
    Token.Type rightType = rightExp.getType();
    if ((rightType == Token.Type.STRING) || (rightType == Token.Type.NUMBER))
    {
      s2 = (String)this.token.getValue().get();
    }
    else if (rightType == Token.Type.LPAREN)
    {
      s2 = expression();
      rightType = Token.Type.NUMBER;
    }
    else if ((rightType == Token.Type.VAR) && (this.variables.containsKey(this.token.getValue().get())))
    {
      String variableName = (String)this.token.getValue().get();
      Token finalVariable = (Token)this.variables.get(variableName);
      s2 = (String)finalVariable.getValue().get();
      rightType = finalVariable.getType();
    }
    else
    {
      exp("STRING, NUMBER, VAR or LPAREN to start a mathematical expression");
    }
    if ((leftType == Token.Type.NUMBER) && (rightType == Token.Type.NUMBER))
    {
      try
      {
        n1 = Float.valueOf(s1).floatValue();
        isN1 = true;
      }
      catch (NumberFormatException e)
      {
        erl("FIRST OPERAND NUMBER FORMAT ERROR");
      }
      try
      {
        n2 = Float.valueOf(s2).floatValue();
        isN2 = true;
      }
      catch (NumberFormatException e)
      {
        erl("SECOND OPERAND NUMBER FORMAT ERROR");
      }
      switch (condition.getType().toString())
      {
      case "EQ": 
        if (n1 == n2) {
          success = true;
        }
        break;
      case "NE": 
        if (n1 != n2) {
          success = true;
        }
        break;
      case "GT": 
        if (n1 > n2) {
          success = true;
        }
        break;
      case "GTE": 
        if (n1 >= n2) {
          success = true;
        }
        break;
      case "LT": 
        if (n1 < n2) {
          success = true;
        }
        break;
      case "LTE": 
        if (n1 <= n2) {
          success = true;
        }
        break;
      default: 
        exp("CONDITION");
      }
    }
    else if ((leftType == Token.Type.STRING) && (rightType == Token.Type.STRING))
    {
      switch (condition.getType().toString())
      {
      case "EQ": 
        if (leftExp.equals(rightExp)) {
          success = true;
        }
        break;
      case "NE": 
        if (!leftExp.equals(rightExp)) {
          success = true;
        }
        break;
      default: 
        exp("= or <>");
      }
    }
    else
    {
      erl("UNCOMPARABLE TYPES");
    }
    boolean gt = false;
    
    expect(Token.Type.KEYWORD);
    if ((this.execute) && (success))
    {
      String keyword = this.token.getValue().toString().toUpperCase().replace("OPTIONAL[", "").replace("]", "").trim();
      if (keyword.equals("THEN"))
      {
        consume();
        keyword = this.token.getValue().toString().toUpperCase().replace("OPTIONAL[", "").replace("]", "").trim();
        if (keyword.equals("PRINT")) {
          PRINT();
        }
        if (keyword.equals("GOSUB")) {
          GOSUB();
        }
        if (keyword.equals("LET")) {
          LET();
        }
        if (keyword.equals("IF")) {
          IF();
        }
        if (keyword.equals("INPUT")) {
          INPUT();
        }
      }
      else if (keyword.equals("GOTO"))
      {
        consume();
        if (this.token.getType() == Token.Type.NUMBER) {
          this.instructionPointer = Integer.valueOf((String)this.token.getValue().get()).intValue();
        }
      }
      else
      {
        exp("THEN or GOTO");
      }
    }
    if (!success) {
      inc();
    }
  }

	private void INPUT() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			consume();
			String variableName = ((String) this.token.getValue().get()).replace("$", "");

			System.out.print(": ");
			String value = br.readLine();
			Token variable;
			try {
				float floatValue = Float.parseFloat(value);
				variable = new Token(Token.Type.NUMBER, value);
			} catch (NumberFormatException e) {
				variable = new Token(Token.Type.STRING, value);
			}
			this.variables.put(variableName, variable);

			inc();
		} catch (IOException ex) {
			err("INPUT ERROR");
		}
	}

	private void GOSUB() {
		consume();

		int target = Integer.parseInt((String) this.token.getValue().get());
		if (this.instructions.containsKey(Integer.valueOf(target))) {
			int counter = target;
			do {
				counter++;
			} while (((!this.instructions.containsKey(Integer.valueOf(counter))) && (counter < 1000))
					|| ((counter < 1000)
							&& (!((String) this.instructions.get(Integer.valueOf(counter))).trim().equals("RETURN"))));
			if (counter == 1000) {
				erl("GOSUB WITHOUT CORRESPONDING NEXT");
			} else if ((this.instructions.containsKey(Integer.valueOf(counter)))
					&& (((String) this.instructions.get(Integer.valueOf(counter))).trim().equals("RETURN"))) {
				this.gosub.put(Integer.valueOf(counter), Integer.valueOf(this.instructionPointer));
				this.instructionPointer = target;
			} else {
				erl("GOSUB ERROR");
			}
		} else {
			err("GOTO ERROR ON LINE " + this.instructionPointer + ": LINE " + target + " DOESN'T EXIST");
		}
	}

	private void RETURN() {
		this.instructionPointer = ((Integer) this.gosub.remove(Integer.valueOf(this.instructionPointer))).intValue();
		inc();
	}

	private void inc() {
		do {
			this.instructionPointer += 1;
		} while ((this.instructionPointer <= 1000)
				&& (!this.instructions.containsKey(Integer.valueOf(this.instructionPointer))));
		if (this.instructionPointer == 1001) {
			err("WARNING: AN END LINE IS PROBABLY MISSING");
			stop();
		}
	}

	public void welcome() {
		System.out.println(
				"Benvenuto nell'interprete Basic.\nPotrai iniziare a scrivere i comandi seguendo questa sintassi:\n[NUMERO LINEA] ISTRUZIONE\nPer avviare l'esecuzione del programma scritto, scrivere RUN e premere invio.\nPer una guida alle istruzioni che si possono usare, scrivere GUIDE e premere invio.\nPer uscire dal programma scrivere Q e premere invio.");
	}

	private void guide() {
		System.out.println(
				"Un parametro pu�� essere una stringa, un numero, una variabile o un espressione. \n\nL'inizio di un espressione �� definito da una parentesi tonda e si possono fare somme, sottrazioni, moltiplicazioni e divisioni. Le altre operazioni sono indicate alla fine del documento.\n\nIl nome di una variabile pu�� essere composto da una sola lettera.\n\nNel codice sorgente deve essere presente una linea di fine esecuzione indicata dall'istruzione END. L'interprete indica il fatto che manchi una linea ma non corregge gli errori logici che l'utente pu�� commettere quindi bisogna far attenzione a tutti i percorsi logici che il programma che si sta scrivendo pu�� intraprendere.\n\nNell'interprete �� stato implementato un semplice meccanismo che simula una memoria RAM di 64kb, molto simile ai sistemi originari su cui girava. Ogni variabile numerica occupa 32 bit, ogni stringa occupa 8 bit per carattere e ogni istruzione occupa 64 bit, quindi attenzione a non riempire la memoria!\n\nLe istruzioni del BASIC sono le seguenti:\n\nPRINT parametro\nL'istruzione PRINT permette di stampare a video ci�� che segue il comando, si possono concatenare parametri con un punto e virgola.\n\nGOTO numero\nL'istruzione GOTO, seguita da un numero, permette di spostare l'esecuzione del programma alla linea indicata dal numero.\n\nGOSUB numero\nL'istruzione GOSUB funziona in modo simile al comando GOTO. Essa permette di spostare l'esecuzione del programma alla linea indicata dal numero, e alla fine dell'esecuzione, ovvero non appena l'interprete arriverr�� alla linea RETURN, il programma continuer�� da dopo l'istruzione  GOSUB. Si pu�� definire come una sorta di funzione.\n\nRETURN\nL'istruzione RETURN indica la fine di una subroutine.\n\nINPUT variabile\nL'istruzione INPUT permette di assegnare un valore ad una variabile.\n\nLET variabile = parametro\nIl comando LET permette di creare una variabile e assegnarle un valore. Si possono concatenare le stringhe con l'istruzione CONCAT(parametro, parametro, ...).\n\nIF parametro confronto parametro [THEN o GOTO] [ISTRUZIONE o LINEA]\nL'istruzione IF permette di eseguire un comando se il confronto �� vero. I confronti possibili sono >, <, =, >=, <= o <> (diverso).\n\nFOR variabile = numero TO numero [OPZIONALE: STEP incremento]\nL'istruzione FOR permette di eseguire un blocco di codice finch�� la variabile indicata, con il suo valore di partenza non raggiunge un valore. La fine del blocco di codice �� indicata con l'istruzione NEXT seguita dalla variabile del FOR corrispondente. Ad ogni NEXT la variabile viene incrementata del valore indicato dallo STEP, se non lo si indica viene incrementata di 1.\n\nREM testo\nL'istruzione REM non affligge in alcun modo l'esecuzione del codice, viene usata per inserire dei commenti nel sorgente.");

		System.out.println(
				"Le operazioni matematiche sono:\n\n\nPOW(A,B): AB \nSQRT(A): calcola la radice quadrata di A\nABS(A): calcola il valore assoluto di A\nSIN(A): calcola il seno di A\nCOS(A): calcola il coseno di A\nTAN(A): calcola la tangente di A\nASIN(A): calcola l'arcoseno di A\nACOS(A): calcola l'arcocoseno di A\nATAN(A): calcola l'arcotangente di A\nLOG(A): calcola il logaritmo naturale di A\nMIN(A,B): trova il valore minimo tra A e B o pi�� parametri\nMAX(A,B): trova il valore massimo tra A e B o pi�� parametri\nRANDOM(): restituisce un valore casuale\nROUND(A): arrotonda A\nCEIL(A): arrotonda A per eccesso\nFLOOR(A): arrotonda A per difetto");

		ok();
	}

	private void ok() {
		System.out.println("\nFree memory: " + this.freeMemory + " bits\nOk.\n");
	}

	private void err(String s) {
		System.out.println("??" + s);
		stop();
	}

	private void erl(String s) {
		System.out.println("??ERROR ON LINE " + this.instructionPointer + ": " + s);
		stop();
	}

	private void undef() {
		err("UNDEFINED VARIABLE ON LINE " + this.instructionPointer);

		stop();
	}

	private void stop() {
		this.execute = false;
	}

	private void file() {
		System.out.println();
		try {
			File folder = new File(new File(".").getCanonicalPath());
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if ((listOfFiles[i].isFile()) && (listOfFiles[i].getName().contains(".bsc"))) {
					System.out.println(listOfFiles[i].getName().replaceAll(".bsc", ""));
				}
			}
		} catch (IOException ex) {
			err("DIRECTORY ERROR");
		}
		ok();
	}

	public void shell() {
		String input = " ";

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		ok();
		while (!input.toUpperCase().equals("Q")) {
			try {
				input = br.readLine();
				if (!input.equals("")) {
					switch (input.toUpperCase()) {
					case "LIST":
						LIST();
						break;
					case "RUN":
						run();
						break;
					case "GUIDE":
						guide();
						break;
					case "RESET":
						reset();
						break;
					case "FILE":
						file();
						break;
					case "Q":
						break;
					default:
						try {
							if (input.substring(0, 4).toUpperCase().equals("SAVE")) {
								save(input.toUpperCase().replaceAll("SAVE ", ""));
							} else if (input.substring(0, 4).toUpperCase().equals("LOAD")) {
								load(input.toUpperCase().replaceAll("LOAD ", ""));
							} else if (input.substring(0, 3).toUpperCase().equals("DEL")) {
								try {
									if (this.instructions.containsKey(
											Integer.valueOf(Integer.parseInt(input.replaceAll("DEL", "").trim())))) {
										this.instructions.remove(
												Integer.valueOf(Integer.parseInt(input.replaceAll("DEL", "").trim())));
									} else {
										err("LINE " + Integer.parseInt(input.replaceAll("DEL", "").trim())
												+ " DOESN'T EXIST");
									}
								} catch (Exception e) {
									err("NUMBER FORMAT ERROR");
								}
							} else {
								insertInstruction(input);
							}
						} catch (Exception e) {
							err("SYNTAX ERROR");
						}
					}
				}
			} catch (IOException e) {
				err("IOEXCEPTION");
			} catch (NumberFormatException e) {
				err("MISSING LINE NUMBER");
			}
		}
	}

	public void insertInstruction(String line) {
		try {
			int lineNumber = Integer.valueOf(line.substring(0, line.indexOf(" "))).intValue();
			if ((lineNumber >= 1) && (lineNumber <= 1000)) {
				String command = line.substring(line.indexOf(" ") + 1);
				if (this.instructions.containsKey(Integer.valueOf(lineNumber))) {
					this.instructions.put(Integer.valueOf(lineNumber), command);
				} else if (this.freeMemory > 64) {
					this.instructions.put(Integer.valueOf(lineNumber), command);
					this.freeMemory -= 64;
				} else {
					err("NOT ENOUGH MEMORY AVAILABLE, 64 BITS ARE NEEDED FOR AN INSTRUCTION, BUT " + this.freeMemory
							+ " BITS ARE AVAILABLE");
				}
			} else {
				err("LINE NUMBER MUST BE BETWEEN 1 AND 1000");
			}
		} catch (Exception e) {
			err("SYNTAX ERROR ON LAST LINE");
		}
	}

	private boolean accept(Token.Type type) {
		consume();
		return this.token.getType() == type;
	}

	private boolean expect(Token.Type type) {
		if (!accept(type)) {
			System.out.println("Unexpected " + this.token.getType() + " on line " + this.instructionPointer
					+ ", expecting " + type.toString());
			stop();
			return false;
		}
		return true;
	}

	private void exp(String s) {
		System.out.println(
				"Unexpected " + this.token.getType() + " on line " + this.instructionPointer + ", expecting " + s);

		stop();
	}

	private void exp(Token.Type t) {
		System.out.println("Unexpected " + this.token.getType() + " on line " + this.instructionPointer + ", expecting "
				+ t.toString());

		stop();
	}

	public void save(String fileName) {
		try {
			PrintWriter wr = new PrintWriter(fileName.toLowerCase() + ".bsc");
			int counter = 0;
			int lines = 0;
			while (lines < this.instructions.size()) {
				do {
					counter++;
				} while (!this.instructions.containsKey(Integer.valueOf(counter)));
				wr.println(counter + " " + (String) this.instructions.get(Integer.valueOf(counter)));
				lines++;
			}
			wr.close();
		} catch (IOException e) {
			err("INVALID FILENAME");
		}
	}

	public void load(String fileName) {
		reset();
		try {
			String line = null;
			FileReader fr = new FileReader(fileName.toLowerCase() + ".bsc");
			BufferedReader br = new BufferedReader(fr);
			while ((line = br.readLine()) != null) {
				insertInstruction(line);
			}
			br.close();
		} catch (FileNotFoundException e) {
			err("FILE NOT FOUND");
		} catch (IOException e) {
			err("INVALID FILENAME");
		}
	}

	private boolean occupyMemory(Token t) {
		if (t.getType() == Token.Type.NUMBER) {
			if (this.freeMemory >= 32) {
				this.freeMemory -= 32;
				return true;
			}
			err("NOT ENOUGH MEMORY AVAILABLE, 32 BITS ARE NEEDED AND " + this.freeMemory + " ARE AVAILABLE");
			stop();
		}
		if (t.getType() == Token.Type.STRING) {
			String s = (String) t.getValue().get();
			int size = s.length() * 8;
			if (this.freeMemory >= size) {
				this.freeMemory -= size;
				return true;
			}
			err("NOT ENOUGH MEMORY AVAILABLE, TO INITIALIZE LINE " + this.instructionPointer + ", " + size
					+ " ARE BITS NEEDED AND " + this.freeMemory + " ARE AVAILABLE");

			stop();
		}
		return false;
	}

	private boolean checkEnd() {
		for (Iterator localIterator = this.instructions.keySet().iterator(); localIterator.hasNext();) {
			int key = ((Integer) localIterator.next()).intValue();
			if (((String) this.instructions.get(Integer.valueOf(key))).trim().equals("END")) {
				return true;
			}
		}
		err("END LINE IS MISSING");
		return false;
	}
}
