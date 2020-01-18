package ru.r2cloud.ssdv;

import java.util.Arrays;

class McuDecoder {

	static final int PIXELS_PER_MCU = 16;
	private static final int DEFAULT_SAMPLING = 1;
	private static final int[] DEFAULT_Y_DU = new int[DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU];
	private static final int[] DEFAULT_CBCR_DU = new int[DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU];

	private final ColorComponent yComponent = new ColorComponent();
	private final ColorComponent cbComponent = new ColorComponent();
	private final ColorComponent crComponent = new ColorComponent();

	private int[] rgb;

	public void reset(SsdvPacket firstPacket) {
		switch (firstPacket.getSubsamplingMode()) {
		case 0:
			yComponent.reset(2, 2);
			break;
		case 1:
			yComponent.reset(1, 2);
			break;
		case 2:
			yComponent.reset(2, 1);
			break;
		case 3:
			yComponent.reset(1, 1);
			break;
		default:
			throw new IllegalArgumentException("unsupported subsampling mode: " + firstPacket.getSubsamplingMode());
		}
		// Cb and Cr always have 1x1 in ssdv
		cbComponent.reset(DEFAULT_SAMPLING, DEFAULT_SAMPLING);
		crComponent.reset(DEFAULT_SAMPLING, DEFAULT_SAMPLING);

		if (rgb == null || rgb.length != yComponent.getBuffer().length) {
			rgb = new int[yComponent.getBuffer().length];
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

		// here I assume cb and cr are always 1x1
		// no need to find minimum subsampling across all 3 components
		for (int row = 0; row < DataUnitDecoder.PIXELS_PER_DU; row++) {
			for (int col = 0; col < DataUnitDecoder.PIXELS_PER_DU; col++) {
				int cbCrSourceIndex = row * DataUnitDecoder.PIXELS_PER_DU + col;
				for (int subRow = 0; subRow < yComponent.getMaxRows(); subRow++) {
					for (int subCol = 0; subCol < yComponent.getMaxCols(); subCol++) {
						int ySourceIndex = row * DataUnitDecoder.PIXELS_PER_DU * yComponent.getMaxCols() * yComponent.getMaxRows() + subRow * DataUnitDecoder.PIXELS_PER_DU * yComponent.getMaxCols() + col * yComponent.getMaxCols() + subCol;
						rgb[ySourceIndex] = convertToRgb(yComponent.getBuffer()[ySourceIndex], cbComponent.getBuffer()[cbCrSourceIndex], crComponent.getBuffer()[cbCrSourceIndex]);
					}
				}
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
	// do not append if already appened to mcu
	// return false if current color component is not full
	private static void appendDu(int[] du, ColorComponent component) {
		for (int row = 0; row < DataUnitDecoder.PIXELS_PER_DU; row++) {
			for (int col = 0; col < DataUnitDecoder.PIXELS_PER_DU; col++) {
				int source = du[row * DataUnitDecoder.PIXELS_PER_DU + col];
				int blockFullRows = component.getCurrentRow() * DataUnitDecoder.PIXELS_PER_DU * component.getMaxCols() * DataUnitDecoder.PIXELS_PER_DU;
				int blockIndexRows = row * component.getMaxCols() * DataUnitDecoder.PIXELS_PER_DU;
				int colOffset = component.getCurrentCol() * DataUnitDecoder.PIXELS_PER_DU;
				component.getBuffer()[blockFullRows + blockIndexRows + colOffset + col] = source;
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

}
