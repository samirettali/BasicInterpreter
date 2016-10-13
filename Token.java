package tokenizer;

import java.util.Objects;
import java.util.Optional;

public class Token {
	private final Type type;
	private final Optional<String> value;

	public static enum Type {
		EOF, LF, VAR, KEYWORD, NUMBER, STRING, PLUS, MINUS, MULT, DIV, MOD, POINT, LPAREN, RPAREN, EQ, NE, GT, GTE, LT, LTE, COMMA, SEMICOMMA;

		private Type() {
		}
	}

	public Token(Type type) {
		this.type = type;
		this.value = Optional.empty();
	}

	public Token(Type type, String value) {
		this.type = type;
		this.value = Optional.of(value);
	}

	public Type getType() {
		return this.type;
	}

	public Optional<String> getValue() {
		return this.value;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		Token token = (Token) o;
		if (this.type != token.type) {
			return false;
		}
		if (!this.value.equals(token.value)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		return Objects.hash(new Object[] { this.type, this.value });
	}

	public String toString() {
		return Token.class.getSimpleName() + " [type=" + this.type + ", value=" + this.value + "]";
	}
}
