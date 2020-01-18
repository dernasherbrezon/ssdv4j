package ru.r2cloud.ssdv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.imageio.plugins.jpeg.JPEGHuffmanTable;

import org.jtransforms.dct.DoubleDCT_2D;

class DataUnitDecoder {

	private static final int TWO_BYTES_POSSIBLE_VALUES = (int) (Math.pow(2, 8) * Math.pow(2, 8));
	private static final int BIT_IN_TWO_BYTES = 16;

	private static final int[] Y_DQT_DEFAULT = new int[] { 0x10, 0x0C, 0x0C, 0x0E, 0x0C, 0x0A, 0x10, 0x0E, 0x0E, 0x0E, 0x12, 0x12, 0x10, 0x14, 0x18, 0x28, 0x1A, 0x18, 0x16, 0x16, 0x18, 0x32, 0x24, 0x26, 0x1E, 0x28, 0x3A, 0x34, 0x3E, 0x3C, 0x3A, 0x34, 0x38, 0x38, 0x40, 0x48, 0x5C, 0x4E, 0x40, 0x44, 0x58, 0x46, 0x38, 0x38, 0x50, 0x6E, 0x52, 0x58, 0x60, 0x62, 0x68, 0x68, 0x68, 0x3E, 0x4E, 0x72, 0x7A, 0x70, 0x64, 0x78, 0x5C, 0x66, 0x68, 0x64 };
	private static final int[] CB_CR_DQT_DEFAULT = new int[] { 0x12, 0x12, 0x12, 0x16, 0x16, 0x16, 0x30, 0x1A, 0x1A, 0x30, 0x64, 0x42, 0x38, 0x42, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64, 0x64 };
	private static final int[] DQT_SCALES = new int[] { 5000, 357, 172, 116, 100, 58, 28, 0 };
	private static final DoubleDCT_2D DCTTransform = new DoubleDCT_2D(8, 8);
	private static final byte[] ZIGZAG_INDEXES = new byte[] { 0, 1, 5, 6, 14, 15, 27, 28, 2, 4, 7, 13, 16, 26, 29, 42, 3, 8, 12, 17, 25, 30, 41, 43, 9, 11, 18, 24, 31, 40, 44, 53, 10, 19, 23, 32, 39, 45, 52, 54, 20, 22, 33, 38, 46, 51, 55, 60, 21, 34, 37, 47, 50, 56, 59, 61, 35, 36, 48, 49, 57, 58, 62, 63 };

	// mapping between codeword and AcCode
	// spatial table, stores less AcCode objects than 65536
	private static AcCode[] acYCodes;
	private static DcCode[] dcYCodes;
	private static AcCode[] acCbcrCodes;
	private static DcCode[] dcCbcrCodes;
	// mapping between all possible 2bytes and the codeword. Saves 12 bitwise shift and compare operations
	private static AcCode[] acYLookup;
	private static AcCode[] acCbcrLookup;
	// lookup table for max 2 bytes. category lookup table has max 9 bits (i.e. 8 bit + 1 bit)
	// it saves us 12 bitwise shift and compare operations (worst case scenario)
	private static int[] dcYLookup;
	private static int[] dcCbcrLookup;

	static {
		dcYCodes = createTable(JPEGHuffmanTable.StdDCLuminance);
		dcYLookup = setupDcLookup(dcYCodes);
		acYCodes = createAcTable(JPEGHuffmanTable.StdACLuminance);
		acYLookup = setupAcLookup(acYCodes);

		dcCbcrCodes = createTable(JPEGHuffmanTable.StdDCChrominance);
		dcCbcrLookup = setupDcLookup(dcCbcrCodes);
		acCbcrCodes = createAcTable(JPEGHuffmanTable.StdACChrominance);
		acCbcrLookup = setupAcLookup(acCbcrCodes);
	}

	static final int PIXELS_PER_DU = 8;
	private byte[] data;
	private int currentBitIndex = 0;
	private boolean hasNext = false;
	private final int[] currentPixels = new int[PIXELS_PER_DU * PIXELS_PER_DU];
	private final double[] dct = new double[currentPixels.length];

	private Integer qualityLevel;
	private int[] yDqt;
	private int[] cbcrDqt;
	private double[] previousDc = new double[3];

	public boolean hasNext(boolean isYComponent, boolean isCbComponent, boolean isCrComponent) throws IOException {
		int previousBitIndex = currentBitIndex;
		hasNext = hasNextInternal(isYComponent, isCbComponent, isCrComponent);
		// reset bit index to the beginning of the data unit
		if (!hasNext) {
			currentBitIndex = previousBitIndex;
		}
		return hasNext;
	}

	private boolean hasNextInternal(boolean isYComponent, boolean isCbComponent, boolean isCrComponent) throws IOException {
		int index = peekNext(BIT_IN_TWO_BYTES);
		if (index < 0) {
			return false;
		}
		int dcCategory = lookupDc(isYComponent, index);
		if (dcCategory == -1) {
			throw new IOException("invalid dc category");
		}
		int codeWordLength = getDcCodeWordLength(isYComponent, dcCategory);
		currentBitIndex += codeWordLength;
		int dcBitmask = peekNext(dcCategory);
		if (dcBitmask < 0) {
			return false;
		}
		// dc category also defines number of bits in the diff value
		currentBitIndex += dcCategory;

		double[] zigzagDct = new double[dct.length];
		zigzagDct[0] = mapBitmaskToValue(dcCategory, dcBitmask);
		// for the first MCU previous dc should be 0
		int previousDcIndex;
		if (isYComponent) {
			previousDcIndex = 0;
		} else if (isCbComponent) {
			previousDcIndex = 1;
		} else if (isCrComponent) {
			previousDcIndex = 2;
		} else {
			throw new IllegalArgumentException("invalid component");
		}
		zigzagDct[0] += previousDc[previousDcIndex];

		// decode AC
		for (int i = 1; i < 64; i++) {
			index = peekNext(BIT_IN_TWO_BYTES);
			if (index < 0) {
				return false;
			}
			AcCode code = lookupAc(isYComponent, index);
			if (code == null) {
				throw new IOException("invalid ac code");
			}
			currentBitIndex += code.getCodeLength();
			// EOB
			if (code.getRun() == 0 && code.getCategory() == 0) {
				break;
			}
			i += code.getRun();
			// not ZRL
			if (code.getCategory() != 0) {
				int acBitmask = peekNext(code.getCategory());
				if (acBitmask < 0) {
					return false;
				}
				currentBitIndex += code.getCategory();
				zigzagDct[i] = mapBitmaskToValue(code.getCategory(), acBitmask);
			}
		}

		for (int i = 0; i < zigzagDct.length; i++) {
			dct[i] = zigzagDct[ZIGZAG_INDEXES[i]] * getDqt(isYComponent)[i];
		}
		DCTTransform.inverse(dct, true);
		for (int i = 0; i < dct.length; i++) {
			currentPixels[i] = (int) (Math.round(dct[i] + 128));
			if (currentPixels[i] < 0) {
				currentPixels[i] = 0;
			} else if (currentPixels[i] > 255) {
				currentPixels[i] = 255;
			}
		}

		// set previous only when DU fully read
		previousDc[previousDcIndex] = zigzagDct[0];
		return true;
	}

	private int[] getDqt(boolean isYComponent) {
		if (isYComponent) {
			return yDqt;
		} else {
			return cbcrDqt;
		}
	}

	private static int getDcCodeWordLength(boolean isYComponent, int dcCategory) {
		DcCode[] lookupTable;
		if (isYComponent) {
			lookupTable = dcYCodes;
		} else {
			lookupTable = dcCbcrCodes;
		}
		return lookupTable[dcCategory].getCodeLength();
	}

	private static int lookupDc(boolean isYComponent, int index) {
		int[] lookupTable;
		if (isYComponent) {
			lookupTable = dcYLookup;
		} else {
			lookupTable = dcCbcrLookup;
		}
		return lookupTable[index];
	}

	private static AcCode lookupAc(boolean isYComponent, int index) {
		AcCode[] lookupTable;
		if (isYComponent) {
			lookupTable = acYLookup;
		} else {
			lookupTable = acCbcrLookup;
		}
		return lookupTable[index];
	}

	private int peekNext(int numberOfBits) {
		if ((currentBitIndex >> 3) >= data.length) {
			// numberOfBits is always less than 31
			return -1;
		}
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
		for (int i = 0; i < previousDc.length; i++) {
			previousDc[i] = 0.0;
		}
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

	// TODO always append. Maybe shrink?
	public void append(byte[] payload) {
		byte[] newData = new byte[data.length + payload.length];
		System.arraycopy(data, 0, newData, 0, data.length);
		System.arraycopy(payload, 0, newData, data.length, payload.length);
		this.data = newData;
	}

	public void resetToTheNextByte() {
		int overflow = currentBitIndex % 8;
		if (overflow > 0) {
			currentBitIndex += (8 - currentBitIndex % 8);
		}
		// next packet starts with DC = 0
		for (int i = 0; i < previousDc.length; i++) {
			previousDc[i] = 0.0;
		}
	}

	private static int[] setupDcLookup(DcCode[] dcCodes) {
		Map<Integer, DcCode> dcYIndex = indexByCode(dcCodes);
		int[] result = new int[TWO_BYTES_POSSIBLE_VALUES];
		for (int i = 0; i < TWO_BYTES_POSSIBLE_VALUES; i++) {
			DcCode code = findCode(i, dcYIndex);
			// shouldn't happen. defensive check
			if (code != null) {
				result[i] = code.getCategory();
			} else {
				result[i] = -1;
			}
		}
		return result;
	}

	private static AcCode[] setupAcLookup(AcCode[] acCodes) {
		Map<Integer, AcCode> acYIndex = indexByCode(acCodes);
		AcCode[] result = new AcCode[TWO_BYTES_POSSIBLE_VALUES];
		for (int i = 0; i < TWO_BYTES_POSSIBLE_VALUES; i++) {
			result[i] = findCode(i, acYIndex);
		}
		return result;
	}

	private static <T extends DcCode> T findCode(int twoByteIndex, Map<Integer, T> index) {
		// iterate from shorter codewords to longer
		for (int j = BIT_IN_TWO_BYTES; j >= 0; j--) {
			int codeword = twoByteIndex >> j;
			T code = index.get(codeword);
			if (code == null) {
				continue;
			}
			// make sure we index codeword of the right length
			if (code.getCodeLength() != BIT_IN_TWO_BYTES - j) {
				continue;
			}
			return code;
		}
		return null;
	}

	private static <T extends DcCode> Map<Integer, T> indexByCode(T[] list) {
		Map<Integer, T> result = new HashMap<>();
		for (T cur : list) {
			result.put(cur.getCodeword(), cur);
		}
		return result;
	}

	private static DcCode[] createTable(JPEGHuffmanTable table) {
		List<DcCode> result = new ArrayList<>();
		int code = 0;
		int lengthIndex = 0;
		for (int i = 0; i < table.getLengths().length; i++) {
			for (int j = 0; j < table.getLengths()[i]; j++) {
				DcCode cur = new DcCode();
				cur.setCodeLength(i + 1);
				cur.setCodeword(code);
				cur.setCategory(table.getValues()[lengthIndex]);
				result.add(cur);
				code++;
				lengthIndex++;
			}
			code <<= 1;
		}
		return result.toArray(new DcCode[0]);
	}

	private static AcCode[] createAcTable(JPEGHuffmanTable table) {
		List<AcCode> result = new ArrayList<>();
		int code = 0;
		int lengthIndex = 0;
		for (int i = 0; i < table.getLengths().length; i++) {
			for (int j = 0; j < table.getLengths()[i]; j++) {
				int runLengthAndCategory = table.getValues()[lengthIndex];
				AcCode cur = new AcCode();
				cur.setCodeLength(i + 1);
				cur.setCodeword(code);
				cur.setRun(runLengthAndCategory >> 4);
				cur.setCategory(runLengthAndCategory & 0xF);
				result.add(cur);
				code++;
				lengthIndex++;
			}
			code <<= 1;
		}
		return result.toArray(new AcCode[0]);
	}

	private static int mapBitmaskToValue(int dcCategory, int bitmask) {
		boolean startWith1 = (bitmask >> (dcCategory - 1)) != 0;
		int maxval = (1 << dcCategory) - 1;
		if (startWith1) {
			return bitmask;
		} else {
			return bitmask - maxval;
		}
	}

}
