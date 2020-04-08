package UI;

public class TUIprogressBar {

	private String barString;
	
	private char barChar;
	
	private int percentageComplete;

	/**
	 * @param barString
	 * @param barChar
	 * @param percentageComplete
	 */
	public TUIprogressBar(String barString, char barChar, int percentageComplete) {
		super();
		this.barString = barString;
		this.barChar = barChar;
		this.percentageComplete = percentageComplete;
	}

	public String update() {
		
		// do update
		
		return this.barString;
	}
	
	public String getBarString() {
		return barString;
	}

	public void setBarString(String barString) {
		this.barString = barString;
	}

	public char getBarChar() {
		return barChar;
	}

	public void setBarChar(char barChar) {
		this.barChar = barChar;
	}

	public int getPercentageComplete() {
		return percentageComplete;
	}

	public void setPercentageComplete(int percentageComplete) {
		this.percentageComplete = percentageComplete;
	}
	
}
