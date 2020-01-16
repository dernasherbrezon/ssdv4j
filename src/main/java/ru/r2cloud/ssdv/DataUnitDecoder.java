package ru.r2cloud.ssdv;

import java.util.NoSuchElementException;

import org.jtransforms.dct.DoubleDCT_2D;

class DataUnitDecoder {

	private static final int[] Y_DQT_DEFAULT = new int[] { 0x10, 0x0C, 0x0C, 0x0E, 0x0C, 0x0A, 0x10, 0x0E, 0x0E, 0x0E, 0x12, 0x12, 0x10, 0x14, 0x18, 0x28, 0x1A, 0x18, 0x16, 0x16, 0x18, 0x32, 0x24, 0x26, 0x1E, 0x28, 0x3A, 0x34, 0x3E, 0x3C, 0x3A, 0x34, 0x38, 0x38, 0x40, 0x48, 0x5C, 0x4E, 0x40, 0x44, 0x58, 0x46, 0x38, 0x38, 0x50, 0x6E, 0x52, 0x58, 0x60, 0x62, 0x68, 0x68, 0x68, 0x3E, 0x4E, 0x72, 0x7A, 0x70, 0x64, 0x78, 0x5C, 0x66, 0x68, 0x64 };
	private static final int[] CB_CR_DQT_DEFAULT = new int[] { 0x12, 0x12, 0x12, 0x16, 0x16, 0x16, 0x30, 0x1A, 0x1A, 0x30, 0x64, 0x42, 0x38, 0x42, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64 };
	private static final int[] DQT_SCALES = new int[] { 5000, 357, 172, 116, 100, 58, 28, 0 };
	private static final DoubleDCT_2D DCTTransform = new DoubleDCT_2D(8, 8);
	private static final byte[] ZIGZAG_INDEXES = new byte[] { 0, 1, 5, 6, 14, 15, 27, 28, 2, 4, 7, 13, 16, 26, 29, 42, 3, 8, 12, 17, 25, 30, 41, 43, 9, 11, 18, 24, 31, 40, 44, 53, 10, 19, 23, 32, 39, 45, 52, 54, 20, 22, 33, 38, 46, 51, 55, 60, 21, 34, 37, 47, 50, 56, 59, 61, 35, 36, 48, 49, 57, 58, 62, 63 };

	static final int PIXELS_PER_DU = 8;
	private byte[] data;
	private int currentBitIndex = 0;
	private boolean hasNext = false;
	private final int[] currentPixels = new int[PIXELS_PER_DU * PIXELS_PER_DU];
	private final double[] dct = new double[currentPixels.length];

	private Integer qualityLevel;
	private int[] yDqt;
	private int[] cbcrDqt;

	public boolean hasNext(boolean isYComponent) {
		hasNext = hasNextInternal(isYComponent);
		return hasNext;
	}

	private boolean hasNextInternal(boolean isYComponent) {
		double[] zigzagDct = new double[dct.length];
		return false;
	}

	// FIXME should return EOFException or flag if no data
	private int getNext(int numberOfBits) {
		int result = 0;
		for (int i = 0; i < numberOfBits; i++) {
			int bitIndex = currentBitIndex + i;
			int currentByteIndex = bitIndex >> 3;
			// peeking into next 16 bits might overflow byte array
			// this is fine, since not all bits will be used for matching codeword
			// stuff the overflow with zeroes
			if (currentByteIndex >= data.length) {
				result = result << 1;
				continue;
			}
			int bit;
			if (((data[currentByteIndex] & 0xFF) & (1 << (7 - (bitIndex & 7)))) != 0) {
				bit = 1;
			} else {
				bit = 0;
			}
			result = (result << 1) | bit;
		}
		return result;
	}

	public int[] next() {
		if (!hasNext) {
			throw new NoSuchElementException();
		}
		return currentPixels;
	}

	public void reset(byte[] data, int qualityLevel) {
		if (this.qualityLevel == null || this.qualityLevel != qualityLevel) {
			yDqt = adjustDqt(Y_DQT_DEFAULT, qualityLevel);
			cbcrDqt = adjustDqt(CB_CR_DQT_DEFAULT, qualityLevel);
			this.qualityLevel = qualityLevel;
		}
		hasNext = false;
		currentBitIndex = 0;
		this.data = data;
	}

	private static int[] adjustDqt(int[] defaultTable, int qualityLevel) {
		int[] result = new int[defaultTable.length];
		if (qualityLevel > 7) {
			qualityLevel = 7;
		}
		int scaleFactor = DQT_SCALES[qualityLevel];
		for (int i = 0; i < defaultTable.length; i++) {
			int tmp = (defaultTable[i] * scaleFactor + 50) / 100;
			if (tmp == 0) {
				tmp = 1;
			}
			if (tmp > 255) {
				tmp = 255;
			}
			result[i] = tmp;
		}
		return result;
	}

	public void append(byte[] payload) {
		byte[] newData = new byte[data.length + payload.length];
		System.arraycopy(data, 0, newData, 0, data.length);
		System.arraycopy(payload, 0, newData, data.length, payload.length);
		this.data = newData;
	}

}
