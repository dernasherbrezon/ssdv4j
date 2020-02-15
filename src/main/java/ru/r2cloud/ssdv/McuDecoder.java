package ru.r2cloud.ssdv;

import java.util.Arrays;

class McuDecoder {

	private static final int MAX_SAMPLING = 2;
	static final int PIXELS_PER_MCU = DataUnitDecoder.PIXELS_PER_DU * MAX_SAMPLING;
	private static final int DEFAULT_SAMPLING = 1;
	private static final int[] DEFAULT_Y_DU = new int[DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU];
	private static final int[] DEFAULT_CBCR_DU = new int[DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU];
	// map 8x8 -> 16x16
	private static final int[] SRC_1X1;
	// map 8x8 -> 8x16
	private static final int[] SRC_2X1;
	// map 8x8 -> 16x8
	private static final int[] SRC_1X2;
	// map 8x8 -> 8x8
	private static final int[] SRC_2X2;

	// map 8x8 to 1 of 4 quadrants within 16x16
	private static final int[] DST_INDEX_MAPPING;

	private final ColorComponent yComponent = new ColorComponent();
	private final ColorComponent cbComponent = new ColorComponent();
	private final ColorComponent crComponent = new ColorComponent();

	private int[] rgb;
	private int mcuHeight;
	private int mcuWidth;

	static {
		// missing MCU = black RGB
		Arrays.fill(DEFAULT_CBCR_DU, 128);
		SRC_1X1 = setup1x1Table();
		SRC_1X2 = setup1x2Table();
		SRC_2X1 = setup2x1Table();
		SRC_2X2 = setup2x2Table();
		DST_INDEX_MAPPING = setupDstMappingTable();
	}

	public void reset(SsdvPacket firstPacket) {
		switch (firstPacket.getSubsamplingMode()) {
		case 0:
			yComponent.reset(0, MAX_SAMPLING, MAX_SAMPLING);
			break;
		case 1:
			yComponent.reset(1, 1, MAX_SAMPLING);
			break;
		case 2:
			yComponent.reset(2, MAX_SAMPLING, 1);
			break;
		case 3:
			yComponent.reset(3, 1, 1);
			break;
		default:
			throw new IllegalArgumentException("unsupported subsampling mode: " + firstPacket.getSubsamplingMode());
		}
		// Cb and Cr always have 1x1 in ssdv
		cbComponent.reset(3, DEFAULT_SAMPLING, DEFAULT_SAMPLING);
		crComponent.reset(3, DEFAULT_SAMPLING, DEFAULT_SAMPLING);

		if (rgb == null || rgb.length != yComponent.getBuffer().length) {
			rgb = new int[yComponent.getBuffer().length];
		}

		mcuHeight = yComponent.getMaxRows() * DataUnitDecoder.PIXELS_PER_DU;
		mcuWidth = yComponent.getMaxCols() * DataUnitDecoder.PIXELS_PER_DU;
	}

	// append until mcu full
	// return decoded rgb
	public int[] append(int[] du) {
		if (!yComponent.isFull()) {
			appendDu(du, yComponent);
			return null;
		}
		if (!cbComponent.isFull()) {
			appendDu(du, cbComponent);
			return null;
		}
		if (!crComponent.isFull()) {
			appendDu(du, crComponent);
			// last component needs one more du (unlikely)
			if (!crComponent.isFull()) {
				return null;
			}
		}

		int[] cbCrIndexMapping = getSrcIndexMapping(yComponent.getSubsamplingMode());
		for (int row = 0; row < yComponent.getMaxRows() * DataUnitDecoder.PIXELS_PER_DU; row++) {
			for (int col = 0; col < yComponent.getMaxCols() * DataUnitDecoder.PIXELS_PER_DU; col++) {
				int sourceIndex = row * yComponent.getMaxCols() * DataUnitDecoder.PIXELS_PER_DU + col;
				rgb[sourceIndex] = convertToRgb(yComponent.getBuffer()[sourceIndex], cbComponent.getBuffer()[cbCrIndexMapping[sourceIndex]], crComponent.getBuffer()[cbCrIndexMapping[sourceIndex]]);
			}
		}

		yComponent.reset();
		cbComponent.reset();
		crComponent.reset();

		return rgb;
	}

	private static int convertToRgb(int y, int cb, int cr) {
		double r = 1.402 * (cr - 128) + y;
		double g = -0.34414 * (cb - 128) - 0.71414 * (cr - 128) + y;
		double b = 1.772 * (cb - 128) + y;
		return (clip(r) << 16) | (clip(g) << 8) | clip(b);
	}

	private static int clip(double color) {
		if (color > 255) {
			return 255;
		} else if (color < 0) {
			return 0;
		} else {
			return (int) color;
		}
	}

	// can be called multiple times
	private static void appendDu(int[] du, ColorComponent component) {
		// shortcut for cbcr and 1x1 mapping
		if (du.length == component.getBuffer().length) {
			System.arraycopy(du, 0, component.getBuffer(), 0, du.length);
		} else {
			int offset = (component.getCurrentRow() * component.getMaxCols() + component.getCurrentCol()) * DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU;
			for (int i = 0; i < du.length; i++) {
				component.getBuffer()[DST_INDEX_MAPPING[offset + i]] = du[i];
			}
		}

		component.incrementCol();
		if (component.getCurrentCol() >= component.getMaxCols()) {
			component.incrementRow();
			if (component.getCurrentRow() < component.getMaxRows()) {
				component.setCurrentCol(0);
			}
		}
	}

	public int[] finishCurrentMcu() {
		int[] result = null;
		while (result == null && !Thread.currentThread().isInterrupted()) {
			if (isYComponent()) {
				result = append(DEFAULT_Y_DU);
			} else {
				result = append(DEFAULT_CBCR_DU);
			}
		}
		return result;
	}

	public int[] createEmptyMcu() {
		Arrays.fill(rgb, 0);
		return rgb;
	}

	public boolean isYComponent() {
		return !yComponent.isFull();
	}

	public boolean isCbComponent() {
		return yComponent.isFull() && !cbComponent.isFull();
	}

	public boolean isCrComponent() {
		return yComponent.isFull() && cbComponent.isFull() && !crComponent.isFull();
	}

	@Override
	public String toString() {
		return "McuDecoder [y=" + yComponent + ", cb=" + cbComponent + ", cr=" + crComponent + "]";
	}

	private static int[] setup1x1Table() {
		int[] table = new int[McuDecoder.PIXELS_PER_MCU * McuDecoder.PIXELS_PER_MCU];
		for (int i = 0, dsti = 0; i < DataUnitDecoder.PIXELS_PER_DU; i++) {
			for (int j = 0, dstj = 0; j < DataUnitDecoder.PIXELS_PER_DU; j++) {
				table[dsti * McuDecoder.PIXELS_PER_MCU + dstj] = i * DataUnitDecoder.PIXELS_PER_DU + j;
				table[dsti * McuDecoder.PIXELS_PER_MCU + dstj + 1] = i * DataUnitDecoder.PIXELS_PER_DU + j;
				table[(dsti + 1) * McuDecoder.PIXELS_PER_MCU + dstj] = i * DataUnitDecoder.PIXELS_PER_DU + j;
				table[(dsti + 1) * McuDecoder.PIXELS_PER_MCU + dstj + 1] = i * DataUnitDecoder.PIXELS_PER_DU + j;
				dstj += MAX_SAMPLING;
			}
			dsti += MAX_SAMPLING;
		}
		return table;
	}

	private static int[] setup2x1Table() {
		int[] table = new int[MAX_SAMPLING * DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU];
		for (int row = 0, dstRow = 0; row < DataUnitDecoder.PIXELS_PER_DU; row++) {
			for (int col = 0, dstCol = 0; col < DataUnitDecoder.PIXELS_PER_DU; col++) {
				table[dstRow * McuDecoder.PIXELS_PER_MCU + dstCol] = row * DataUnitDecoder.PIXELS_PER_DU + col;
				table[dstRow * McuDecoder.PIXELS_PER_MCU + dstCol + 1] = row * DataUnitDecoder.PIXELS_PER_DU + col;
				dstCol += MAX_SAMPLING;
			}
			dstRow += 1;
		}
		return table;
	}

	private static int[] setup1x2Table() {
		int[] table = new int[MAX_SAMPLING * DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU];
		for (int row = 0, dstRow = 0; row < DataUnitDecoder.PIXELS_PER_DU; row++) {
			for (int col = 0, dstCol = 0; col < DataUnitDecoder.PIXELS_PER_DU; col++) {
				table[dstRow * DataUnitDecoder.PIXELS_PER_DU + dstCol] = row * DataUnitDecoder.PIXELS_PER_DU + col;
				table[(dstRow + 1) * DataUnitDecoder.PIXELS_PER_DU + dstCol] = row * DataUnitDecoder.PIXELS_PER_DU + col;
				dstCol += 1;
			}
			dstRow += MAX_SAMPLING;
		}
		return table;
	}

	// not an actual mapping,
	// but useful for getSrcIndexMapping()
	private static int[] setup2x2Table() {
		int[] result = new int[DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU];
		for (int i = 0; i < result.length; i++) {
			result[i] = i;
		}
		return result;
	}

	private static int[] getSrcIndexMapping(int subsamplingMode) {
		switch (subsamplingMode) {
		case 0:
			return SRC_1X1;
		case 1:
			return SRC_1X2;
		case 2:
			return SRC_2X1;
		case 3:
			return SRC_2X2;
		default:
			throw new IllegalArgumentException("unsupported subsampling mode: " + subsamplingMode);
		}
	}

	private static int[] setupDstMappingTable() {
		int[] table = new int[McuDecoder.PIXELS_PER_MCU * McuDecoder.PIXELS_PER_MCU];
		int cur = 0;
		for (int yRow = 0; yRow < MAX_SAMPLING; yRow++) {
			for (int yCol = 0; yCol < MAX_SAMPLING; yCol++) {
				for (int duRow = 0; duRow < DataUnitDecoder.PIXELS_PER_DU; duRow++) {
					for (int duCol = 0; duCol < DataUnitDecoder.PIXELS_PER_DU; duCol++) {
						int colIndex = yCol * DataUnitDecoder.PIXELS_PER_DU + duCol;
						int rowIndex = yRow * MAX_SAMPLING * DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU + duRow * MAX_SAMPLING * DataUnitDecoder.PIXELS_PER_DU;
						table[cur] = rowIndex + colIndex;
						cur++;
					}
				}
			}
		}
		return table;
	}

	public int getMcuHeight() {
		return mcuHeight;
	}

	public int getMcuWidth() {
		return mcuWidth;
	}
}
