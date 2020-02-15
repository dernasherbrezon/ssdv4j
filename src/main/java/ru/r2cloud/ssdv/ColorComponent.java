package ru.r2cloud.ssdv;

class ColorComponent {

	private int maxCols;
	private int maxRows;

	private int currentCol = 0;
	private int currentRow = 0;
	private int subsamplingMode;

	private int[] buffer;

	public void reset(int subsamplingMode, int maxCols, int maxRows) {
		this.maxCols = maxCols;
		this.maxRows = maxRows;
		this.subsamplingMode = subsamplingMode;
		currentCol = 0;
		currentRow = 0;
		if (buffer == null) {
			buffer = new int[McuDecoder.PIXELS_PER_MCU * McuDecoder.PIXELS_PER_MCU];
		}
	}

	public void reset() {
		this.currentCol = 0;
		this.currentRow = 0;
	}

	public boolean isFull() {
		return currentCol >= maxCols && currentRow >= maxRows;
	}

	public void incrementCol() {
		currentCol++;
	}

	public void incrementRow() {
		currentRow++;
	}

	public int getMaxCols() {
		return maxCols;
	}

	public void setMaxCols(int maxCols) {
		this.maxCols = maxCols;
	}

	public int getMaxRows() {
		return maxRows;
	}

	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	public int getCurrentCol() {
		return currentCol;
	}

	public void setCurrentCol(int currentCol) {
		this.currentCol = currentCol;
	}

	public int getCurrentRow() {
		return currentRow;
	}

	public void setCurrentRow(int currentRow) {
		this.currentRow = currentRow;
	}

	public int[] getBuffer() {
		return buffer;
	}

	public void setBuffer(int[] buffer) {
		this.buffer = buffer;
	}

	public int getSubsamplingMode() {
		return subsamplingMode;
	}
	
	public void setSubsamplingMode(int subsamplingMode) {
		this.subsamplingMode = subsamplingMode;
	}
	
	@Override
	public String toString() {
		return "[maxCols=" + maxCols + ", maxRows=" + maxRows + ", currentCol=" + currentCol + ", currentRow=" + currentRow + "]";
	}

}
