package tokenizer;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;

public final class Tokenizer implements Closeable {
	private final Reader reader;

	private static boolean isWhitespace(int ch) {
		return Character.isWhitespace(ch);
	}

	private static boolean isDigit(int ch) {
		return (ch >= 48) && (ch <= 57);
	}

	private static boolean isAlpha(int ch) {
		return (ch >= 65) && (ch <= 90);
	}

	public Tokenizer(String str) {
		this.reader = new StringReader(str);
	}

	public Tokenizer(Reader reader) {
		this.reader = reader;
	}

	private int peek() throws IOException {
		this.reader.mark(1);
		try {
			return this.reader.read();
		} finally {
			this.reader.reset();
		}
	}

	public Token nextToken() throws IOException {
		for (;;) {
			int ch = this.reader.read();
			if (ch == -1) {
				return new Token(Token.Type.EOF);
			}
			if (ch == 10) {
				return new Token(Token.Type.LF);
			}
			if (ch == 43) {
				return new Token(Token.Type.PLUS);
			}
			if (ch == 45) {
				return new Token(Token.Type.MINUS);
			}
			if (ch == 42) {
				return new Token(Token.Type.MULT);
			}
			if (ch == 47) {
				return new Token(Token.Type.DIV);
			}
			if (ch == 37) {
				return new Token(Token.Type.MOD);
			}
			if (ch == 46) {
				return new Token(Token.Type.POINT);
			}
			if (ch == 40) {
				return new Token(Token.Type.LPAREN);
			}
			if (ch == 41) {
				return new Token(Token.Type.RPAREN);
			}
			if (ch == 44) {
				return new Token(Token.Type.COMMA);
			}
			if (ch == 59) {
				return new Token(Token.Type.SEMICOMMA);
			}
			if (ch == 34) {
				return nextStringToken();
			}
			if (ch == 61) {
				return new Token(Token.Type.EQ);
			}
			if ((ch == 62) || (ch == 60)) {
				return nextRelationalOperatorToken(ch);
			}
			if ((isAlpha(ch)) && (!isAlpha(peek()))) {
				return new Token(Token.Type.VAR, new String(new char[] { (char) ch }));
			}
			if (isAlpha(ch)) {
				return nextKeywordToken(ch);
			}
			if (isDigit(ch)) {
				return nextNumberToken(ch);
			}
			if (!isWhitespace(ch)) {
				throw new IOException("Unexpected character: " + ch);
			}
		}
	}

	private Token nextRelationalOperatorToken(int first) throws IOException {
		int second = peek();
		if (first == 62) {
			if (second == 60) {
				this.reader.skip(1L);
				return new Token(Token.Type.NE);
			}
			if (second == 61) {
				this.reader.skip(1L);
				return new Token(Token.Type.GTE);
			}
			return new Token(Token.Type.GT);
		}
		assert (first == 60);
		if (second == 62) {
			this.reader.skip(1L);
			return new Token(Token.Type.NE);
		}
		if (second == 61) {
			this.reader.skip(1L);
			return new Token(Token.Type.LTE);
		}
		return new Token(Token.Type.LT);
	}

	private Token nextStringToken() throws IOException {
		StringBuilder buf = new StringBuilder();
		for (;;) {
			int ch = this.reader.read();
			if (ch == -1) {
				throw new IOException("Unexpected EOF within string");
			}
			if (ch == 34) {
				break;
			}
			buf.append((char) ch);
		}
		return new Token(Token.Type.STRING, buf.toString());
	}

	private Token nextKeywordToken(int first) throws IOException {
		StringBuilder buf = new StringBuilder();
		buf.append((char) first);
		for (;;) {
			int ch = peek();
			if (!isAlpha(ch)) {
				break;
			}
			this.reader.skip(1L);
			buf.append((char) ch);
		}
		return new Token(Token.Type.KEYWORD, buf.toString());
	}

	private Token nextNumberToken(int first) throws IOException {
		StringBuilder buf = new StringBuilder();
		boolean point = false;
		buf.append((char) first);
		for (;;) {
			int ch = peek();
			if (ch == 46) {
				if (!point) {
					point = true;
				} else {
					System.out.println("UNEXPECTED POINT");
					break;
				}
			}
			if ((ch != 46) && (!isDigit(ch))) {
				break;
			}
			this.reader.skip(1L);
			buf.append((char) ch);
		}
		return new Token(Token.Type.NUMBER, buf.toString());
	}

	public void close() throws IOException {
		this.reader.close();
	}
}
