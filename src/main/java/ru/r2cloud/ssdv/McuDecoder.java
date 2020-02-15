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
	// map 8x8 to 1 of 4 quadrants within 16x16
	private static final int[] DST_2X2;
	private static final int[] DST_1X2;
	private static final int[] DST_2X1;

	private final ColorComponent yComponent = new ColorComponent();
	private final ColorComponent cbComponent = new ColorComponent();
	private final ColorComponent crComponent = new ColorComponent();

	private int[] rgb;

	static {
		// missing MCU = black RGB
		Arrays.fill(DEFAULT_CBCR_DU, 128);
		SRC_1X1 = setup1x1Table();
		DST_2X2 = setup2x2Table(2, 2);

		DST_1X2 = setup2x2Table(2, 1);
		SRC_1X2 = setupSrc1x2Table();

		DST_2X1 = setup2x2Table(1, 2);
		SRC_2X1 = setupSrc2x1Table();
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

		if (rgb == null) {
			rgb = new int[PIXELS_PER_MCU * PIXELS_PER_MCU];
		}
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

		for (int row = 0; row < McuDecoder.PIXELS_PER_MCU; row++) {
			for (int col = 0; col < McuDecoder.PIXELS_PER_MCU; col++) {
				int sourceIndex = row * McuDecoder.PIXELS_PER_MCU + col;
				rgb[sourceIndex] = convertToRgb(yComponent.getBuffer()[sourceIndex], cbComponent.getBuffer()[sourceIndex], crComponent.getBuffer()[sourceIndex]);
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
		if (component.getSubsamplingMode() == 3) {
			for (int i = 0; i < SRC_1X1.length; i++) {
				component.getBuffer()[i] = du[SRC_1X1[i]];
			}
		} else if (component.getSubsamplingMode() == 0) {
			int offset = (component.getCurrentRow() * component.getMaxCols() + component.getCurrentCol()) * DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU;
			for (int i = 0; i < du.length; i++) {
				component.getBuffer()[DST_2X2[offset + i]] = du[i];
			}
		} else if (component.getSubsamplingMode() == 2) {
			int offset = component.getCurrentCol() * McuDecoder.PIXELS_PER_MCU * DataUnitDecoder.PIXELS_PER_DU;
			for (int i = 0; i < SRC_2X1.length; i++) {
				component.getBuffer()[DST_2X1[offset + i]] = du[SRC_2X1[i]];
			}
		} else if (component.getSubsamplingMode() == 1) {
			int offset = component.getCurrentRow() * McuDecoder.PIXELS_PER_MCU * DataUnitDecoder.PIXELS_PER_DU;
			for (int i = 0; i < SRC_1X2.length; i++) {
				component.getBuffer()[DST_1X2[offset + i]] = du[SRC_1X2[i]];
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

	private static int[] setup2x2Table(int maxRows, int maxCols) {
		int[] table = new int[McuDecoder.PIXELS_PER_MCU * McuDecoder.PIXELS_PER_MCU];
		int cur = 0;
		for (int k = 0; k < maxRows; k++) {
			for (int l = 0; l < maxCols; l++) {
				for (int i = 0; i < McuDecoder.PIXELS_PER_MCU / maxRows; i++) {
					for (int j = 0; j < McuDecoder.PIXELS_PER_MCU / maxCols; j++) {
						int col = l * DataUnitDecoder.PIXELS_PER_DU + j;
						int row = k * maxCols * DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU + i * maxCols * DataUnitDecoder.PIXELS_PER_DU;
						table[cur] = row + col;
						cur++;
					}
				}
			}
		}
		return table;
	}

	private static int[] setupSrc2x1Table() {
		int[] table = new int[McuDecoder.PIXELS_PER_MCU * DataUnitDecoder.PIXELS_PER_DU];
		for (int i = 0, dstI = 0; i < McuDecoder.PIXELS_PER_MCU; i += 2, dstI++) {
			for (int j = 0; j < DataUnitDecoder.PIXELS_PER_DU; j++) {
				table[i * DataUnitDecoder.PIXELS_PER_DU + j] = dstI * DataUnitDecoder.PIXELS_PER_DU + j;
				table[(i + 1) * DataUnitDecoder.PIXELS_PER_DU + j] = dstI * DataUnitDecoder.PIXELS_PER_DU + j;
			}
		}
		return table;
	}

	private static int[] setupSrc1x2Table() {
		int[] table = new int[McuDecoder.PIXELS_PER_MCU * DataUnitDecoder.PIXELS_PER_DU];
		for (int i = 0, dstI = 0; i < DataUnitDecoder.PIXELS_PER_DU; i += 1, dstI++) {
			for (int j = 0, dstJ = 0; j < DataUnitDecoder.PIXELS_PER_DU; j += 1, dstJ++) {
				table[i * McuDecoder.PIXELS_PER_MCU + 2 * j] = dstI * DataUnitDecoder.PIXELS_PER_DU + dstJ;
				table[i * McuDecoder.PIXELS_PER_MCU + 2 * j + 1] = dstI * DataUnitDecoder.PIXELS_PER_DU + dstJ;
			}
		}
		return table;
	}

}
