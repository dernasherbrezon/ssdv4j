package ru.r2cloud.ssdv;

class ColorComponent {

	private int maxCols;
	private int maxRows;

	private int currentCol = 0;
	private int currentRow = 0;

	private int[] buffer;

	public void reset(int maxCols, int maxRows) {
		this.maxCols = maxCols;
		this.maxRows = maxRows;
		currentCol = 0;
		currentRow = 0;
		int newLength = maxCols * DataUnitDecoder.PIXELS_PER_DU * maxRows * DataUnitDecoder.PIXELS_PER_DU;
		if (buffer == null || buffer.length != newLength) {
			buffer = new int[newLength];
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

	@Override
	public String toString() {
		return "[maxCols=" + maxCols + ", maxRows=" + maxRows + ", currentCol=" + currentCol + ", currentRow=" + currentRow + "]";
	}

}
