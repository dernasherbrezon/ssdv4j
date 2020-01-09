package ru.r2cloud.ssdv;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class SsdvDecoder implements Iterator<SsdvImage> {

	private static final int DEFAULT_SAMPLING = 1;
	private final Iterator<SsdvPacket> packets;
	private boolean hasNext = false;

	private DataUnitDecoder dataUnitDecoder;
	private int currentMcu = 0;
	private int maxYCols;
	private int maxYRows;

	// Cb and Cr always have 1x1 in ssdv
	private int maxCbCols = DEFAULT_SAMPLING;
	private int maxCbRows = DEFAULT_SAMPLING;
	private int maxCrCols = DEFAULT_SAMPLING;
	private int maxCrRows = DEFAULT_SAMPLING;

	private SsdvPacket previousPacket = null;
	private SsdvImage currentImage = null;
	private SsdvImage completedImage = null;

	public SsdvDecoder(List<SsdvPacket> packets) {
		Collections.sort(packets, SsdvPacketComparator.INSTANCE);
		this.packets = packets.iterator();
		this.dataUnitDecoder = new DataUnitDecoder();
	}

	public SsdvDecoder(Iterator<SsdvPacket> packets) {
		this.packets = packets;
	}

	@Override
	public boolean hasNext() {
		hasNext = hasNextInternal();
		return hasNext;
	}

	private boolean hasNextInternal() {
		while (packets.hasNext()) {
			SsdvPacket currentPacket = packets.next();

			// handle image start/finish
			if (currentImage == null) {
				currentImage = create(currentPacket);
			} else {
				if (currentImage.getImageId() != currentPacket.getImageId()) {
					completeImage();
					currentImage = create(currentPacket);
				}
			}

			// setup data for data unit decoder
			if (previousPacket == null) {
				if (currentPacket.getPacketId() > 0) {
					fillTheGap(currentPacket.getMcuIndex());
				}
				dataUnitDecoder.reset(skipTheOffset(currentPacket));
			} else {
				if (currentPacket.getPacketId() - 1 > previousPacket.getPacketId()) {
					fillTheGap(currentPacket.getMcuIndex());
					dataUnitDecoder.reset(skipTheOffset(currentPacket));
				} else {
					dataUnitDecoder.append(currentPacket.getPayload());
				}
			}

			// FIXME process remaining du
			// FIXME start next MCU

			previousPacket = currentPacket;

			if (completedImage != null) {
				return true;
			}
		}
		if (currentImage != null) {
			// return remaining
			completeImage();
			currentImage = null;
			return true;
		} else {
			return false;
		}
	}

	private static byte[] skipTheOffset(SsdvPacket currentPacket) {
		byte[] data = new byte[currentPacket.getPayload().length - currentPacket.getMcuOffset()];
		System.arraycopy(currentPacket.getPayload(), currentPacket.getMcuOffset(), data, 0, data.length);
		return data;
	}

	private void completeImage() {
		fillTheGap(currentImage.getTotalMcu());
		reset();
		completedImage = currentImage;
	}

	private void reset() {
		previousPacket = null;
		currentMcu = 0;
	}

	private void fillTheGap(int nextMcu) {
		if (currentMcu == nextMcu) {
			return;
		}
		//FIXME finish DU in current mCU
		// generate empty MCU until expected
	}

	private SsdvImage create(SsdvPacket firstPacket) {
		SsdvImage result = new SsdvImage();
		result.setImageId(firstPacket.getImageId());
		result.setTotalMcu(firstPacket.getHeightMcu() * firstPacket.getWidthMcu());
		result.setImage(new BufferedImage(firstPacket.getWidthMcu() * 16, firstPacket.getHeightMcu() * 16, BufferedImage.TYPE_INT_RGB));
		switch (firstPacket.getSubsamplingMode()) {
		case 0:
			maxYCols = 2;
			maxYRows = 2;
			break;
		case 1:
			maxYCols = 1;
			maxYRows = 2;
			break;
		case 2:
			maxYCols = 2;
			maxYRows = 1;
			break;
		case 3:
			maxYCols = 1;
			maxYRows = 1;
			break;
		default:
			throw new IllegalArgumentException("unsupported subsampling mode: " + firstPacket.getSubsamplingMode());
		}
		return result;
	}

	@Override
	public SsdvImage next() {
		if (!hasNext) {
			throw new NoSuchElementException();
		}
		SsdvImage result = completedImage;
		completedImage = null;
		return result;
	}

}
