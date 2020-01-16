package ru.r2cloud.ssdv;

import java.util.Arrays;

class McuDecoder {

	static final int PIXELS_PER_MCU = 16;
	private static final int DEFAULT_SAMPLING = 1;
	private static final int[] DEFAULT_DU = new int[DataUnitDecoder.PIXELS_PER_DU * DataUnitDecoder.PIXELS_PER_DU];

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
		if (!appendDu(du, yComponent)) {
			return null;
		}
		if (!appendDu(du, cbComponent)) {
			return null;
		}
		if (!appendDu(du, crComponent)) {
			return null;
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
		return rgb;
	}

	private static int convertToRgb(int y, int cb, int cr) {
		cb = cb - 128;
		cr = cr - 128;
		double r = y + 1.402 * cr;
		double g = y - 0.34414 * cb - 0.71414 * cr;
		double b = y + 1.772 * cb;
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
	private static boolean appendDu(int[] du, ColorComponent component) {
		if (component.isFull()) {
			return true;
		}

		System.arraycopy(du, 0, component.getBuffer(), component.getCurrentRow() * component.getMaxCols() * DataUnitDecoder.PIXELS_PER_DU + component.getCurrentCol() * DataUnitDecoder.PIXELS_PER_DU, du.length);

		component.incrementCol();
		if (component.getCurrentCol() >= component.getMaxCols()) {
			component.incrementRow();
			if (component.getCurrentRow() < component.getMaxRows()) {
				component.setCurrentCol(0);
			} else {
				// this was the last du in current color component
				return true;
			}
		}
		return false;
	}

	public int[] finishCurrentMcu() {
		int[] result = null;
		while (result == null || !Thread.currentThread().isInterrupted()) {
			result = append(DEFAULT_DU);
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

}
