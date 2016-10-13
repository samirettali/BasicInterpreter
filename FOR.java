public class FOR {
	private final float variableStart;
	private final float variableLimit;
	private final float step;
	private final int beginningLine;
	private final int endingLine;
	private float currentValue;

	public FOR(float variableStart, float variableLimit, float step, int beginningLine, int endingLine) {
		this.variableStart = variableStart;
		this.variableLimit = variableLimit;
		this.step = step;
		this.beginningLine = beginningLine;
		this.endingLine = endingLine;
		this.currentValue = variableStart;
	}

	public void increase() {
		this.currentValue += this.step;
	}

	public boolean isEnded() {
		if (this.step > 0.0F) {
			return this.currentValue <= this.variableLimit;
		}
		return this.currentValue >= this.variableLimit;
	}

	public int getForLine() {
		return this.beginningLine;
	}

	public float getVariableValue() {
		return this.currentValue;
	}
}
