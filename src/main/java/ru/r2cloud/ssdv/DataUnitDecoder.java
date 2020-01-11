package ru.r2cloud.ssdv;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.imageio.plugins.jpeg.JPEGQTable;

class DataUnitDecoder implements Iterator<int[]> {

	private static final int[] Y_DQT = new int[] {0x10,0x0C,0x0C,0x0E,0x0C,0x0A,0x10,0x0E,0x0E,0x0E,0x12,0x12,0x10,0x14,0x18,
			0x28,0x1A,0x18,0x16,0x16,0x18,0x32,0x24,0x26,0x1E,0x28,0x3A,0x34,0x3E,0x3C,0x3A,
			0x34,0x38,0x38,0x40,0x48,0x5C,0x4E,0x40,0x44,0x58,0x46,0x38,0x38,0x50,0x6E,0x52,
			0x58,0x60,0x62,0x68,0x68,0x68,0x3E,0x4E,0x72,0x7A,0x70,0x64,0x78,0x5C,0x66,0x68,
			0x64};
	private static final int[] CB_CR_DQT = new int[] {0x12,0x12,0x12,0x16,0x16,0x16,0x30,0x1A,0x1A,0x30,0x64,0x42,0x38,0x42,0x64,
			0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,
			0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,
			0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,0x64,
			0x64};
	static final int PIXELS_PER_DU = 8;
	private byte[] data;
	private int currentBitIndex = 0;
	private boolean hasNext = false;
	private int[] currentPixels = new int[PIXELS_PER_DU * PIXELS_PER_DU];

	@Override
	public boolean hasNext() {
		hasNext = hasNextInternal();
		return hasNext;
	}

	private boolean hasNextInternal() {
		// FIXME implement
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
	
	@Override
	public int[] next() {
		if (!hasNext) {
			throw new NoSuchElementException();
		}
		return currentPixels;
	}

	public void reset(byte[] data) {
		hasNext = false;
		currentBitIndex = 0;
		this.data = data;
	}

	public void append(byte[] payload) {
		byte[] newData = new byte[data.length + payload.length];
		System.arraycopy(data, 0, newData, 0, data.length);
		System.arraycopy(payload, 0, newData, data.length, payload.length);
		this.data = newData;
	}

	public static void main(String[] args) throws Exception {
		
		for( int i =0;i<JPEGQTable.K2Chrominance.getTable().length;i++ ) {
			System.out.println(Integer.toString(JPEGQTable.K2Chrominance.getTable()[i], 16) + " ");
		}
		
	}
}
