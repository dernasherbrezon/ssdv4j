package ru.r2cloud.ssdv;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SsdvDecoder implements Iterator<SsdvImage> {

	private static final Logger LOG = LoggerFactory.getLogger(SsdvDecoder.class);

	private final Iterator<SsdvPacket> packets;
	private boolean hasNext = false;

	private DataUnitDecoder dataUnitDecoder;
	private McuDecoder mcuDecoder;
	private int currentMcu = 0;
	private int currentX = 0;
	private int currentY = 0;

	private SsdvPacket previousPacket = null;
	private SsdvImage currentImage = null;
	private SsdvImage completedImage = null;

	public SsdvDecoder(List<SsdvPacket> packets) {
		Collections.sort(packets, SsdvPacketComparator.INSTANCE);
		this.packets = packets.iterator();
		this.dataUnitDecoder = new DataUnitDecoder();
		this.mcuDecoder = new McuDecoder();
	}

	public SsdvDecoder(Iterator<SsdvPacket> packets) {
		this.packets = packets;
		this.dataUnitDecoder = new DataUnitDecoder();
		this.mcuDecoder = new McuDecoder();
	}

	@Override
	public boolean hasNext() {
		hasNext = hasNextInternal();
		return hasNext;
	}

	private boolean hasNextInternal() {
		while (packets.hasNext()) {
			SsdvPacket currentPacket = packets.next();

			if (!validate(currentPacket)) {
				continue;
			}

			// handle image start/finish
			if (currentImage == null) {
				currentImage = create(currentPacket);
				mcuDecoder.reset(currentPacket);
			} else {
				if (currentImage.getImageId() != currentPacket.getImageId()) {
					if (currentImage.getSuccessfulMcu() > 0) {
						completeImage();
					} else {
						reset();
					}
					currentImage = create(currentPacket);
					mcuDecoder.reset(currentPacket);
				}
			}

			// setup data for data unit decoder
			if (previousPacket == null) {
				if (currentPacket.getPacketId() > 0) {
					fillTheGap(currentPacket.getMcuIndex());
				}
				dataUnitDecoder.reset(skipTheOffset(currentPacket), currentPacket.getJpegQualityLevel());
			} else {
				if (currentPacket.getPacketId() - 1 > previousPacket.getPacketId()) {
					fillTheGap(currentPacket.getMcuIndex());
					dataUnitDecoder.reset(skipTheOffset(currentPacket), currentPacket.getJpegQualityLevel());
				} else if (currentPacket.getPacketId() == previousPacket.getPacketId()) {
					// duplicate packet
					continue;
				} else {
					dataUnitDecoder.append(currentPacket.getPayload());
				}
			}

			// append to mcuDecoder until next MCU is available
			// MCU might be partially filled in the mcuDecoder
			// read the next packet with the remaining DU to finish MCU
			try {
				while (dataUnitDecoder.hasNext(mcuDecoder.isYComponent(), mcuDecoder.isCbComponent(), mcuDecoder.isCrComponent())) {
					int[] rgb = mcuDecoder.append(dataUnitDecoder.next());
					// not enough data for mcu to complete
					if (rgb == null) {
						continue;
					}
					drawMcu(rgb);
					currentImage.incrementSuccessfulMcu();
					// discard the rest of packet
					if (currentMcu >= currentImage.getTotalMcu()) {
						break;
					}
					// align beginning of the first mcu in the packet to byte
					if (currentMcu > 0 && currentMcu == currentPacket.getMcuIndex()) {
						dataUnitDecoder.resetToTheNextByte();
					}
				}
				previousPacket = currentPacket;
			} catch (IOException e) {
				// this will trigger reset and filling gap
				previousPacket = null;
			}

			if (completedImage != null) {
				return true;
			}
		}
		if (currentImage != null && currentImage.getSuccessfulMcu() > 0) {
			// return remaining
			completeImage();
			currentImage = null;
			return true;
		} else {
			return false;
		}
	}

	private static boolean validate(SsdvPacket packet) {
		if (packet.getWidthMcu() == 0 || packet.getHeightMcu() == 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("invalid width or height: {}", packet);
			}
			return false;
		}
		if (packet.getHeightMcu() * packet.getWidthMcu() > 65536) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("invalid width or height: {}", packet);
			}
			return false;
		}
		if (packet.getPayload() == null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("payload is missing");
			}
			return false;
		}
		if (packet.getMcuOffset() > packet.getPayload().length) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("invalid mcu offset: {}", packet);
			}
			return false;
		}
		if (packet.getPacketId() > 65536) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("invalid packet id: {}", packet);
			}
			return false;
		}
		return true;
	}

	private static byte[] skipTheOffset(SsdvPacket packet) {
		if (packet.getMcuOffset() == 0) {
			return packet.getPayload();
		}
		byte[] data = new byte[packet.getPayload().length - packet.getMcuOffset()];
		System.arraycopy(packet.getPayload(), packet.getMcuOffset(), data, 0, data.length);
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
		currentX = 0;
		currentY = 0;
	}

	private void fillTheGap(int nextMcu) {
		if (currentMcu == nextMcu) {
			return;
		}
		drawMcu(mcuDecoder.finishCurrentMcu());
		for (int i = currentMcu; i < nextMcu; i++) {
			drawMcu(mcuDecoder.createEmptyMcu());
		}
	}

	private void drawMcu(int[] rgb) {
		// defensive
		if (currentY >= currentImage.getImage().getHeight()) {
			return;
		}

		for (int j = 0; j < McuDecoder.PIXELS_PER_MCU; j++) {
			for (int i = 0; i < McuDecoder.PIXELS_PER_MCU; i++) {
				currentImage.getImage().setRGB(currentX + i, currentY + j, rgb[j * McuDecoder.PIXELS_PER_MCU + i]);
			}
		}

		if (currentX + McuDecoder.PIXELS_PER_MCU >= currentImage.getImage().getWidth()) {
			currentX = 0;
			currentY += McuDecoder.PIXELS_PER_MCU;
		} else {
			currentX += McuDecoder.PIXELS_PER_MCU;
		}
		currentMcu++;
	}

	private static SsdvImage create(SsdvPacket firstPacket) {
		SsdvImage result = new SsdvImage();
		result.setImageId(firstPacket.getImageId());
		result.setTotalMcu(firstPacket.getHeightMcu() * firstPacket.getWidthMcu());
		result.setImage(new BufferedImage(firstPacket.getWidthMcu() * McuDecoder.PIXELS_PER_MCU, firstPacket.getHeightMcu() * McuDecoder.PIXELS_PER_MCU, BufferedImage.TYPE_INT_RGB));
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
