package ru.r2cloud.ssdv;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SsdvInputStream implements Iterator<SsdvPacket>, Closeable {

	private final Integer payloadLength;
	private final DataInputStream dis;

	private SsdvPacket next = null;

	public SsdvInputStream(InputStream is) {
		this(is, null);
	}

	public SsdvInputStream(InputStream is, Integer payloadLength) {
		this.payloadLength = payloadLength;
		this.dis = new DataInputStream(is);
	}

	@Override
	public void close() throws IOException {
		dis.close();
	}

	@Override
	public boolean hasNext() {
		try {
			next = readPacket();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private SsdvPacket readPacket() throws IOException {
		SsdvPacket result = new SsdvPacket();
		while (!Thread.currentThread().isInterrupted()) {
			int currentByte = dis.readByte();
			if (currentByte != 0x55) {
				result.setPacketType(currentByte);
				break;
			}
		}
		// ignore callsign for now
		dis.skipBytes(4);
		result.setImageId(dis.readUnsignedByte());
		result.setPacketId(dis.readUnsignedShort());
		result.setWidthMcu(dis.readUnsignedByte());
		result.setHeightMcu(dis.readUnsignedByte());
		if (result.getWidthMcu() == 0 || result.getHeightMcu() == 0) {
			throw new IOException("invalid width or height");
		}

		int flags = dis.readUnsignedByte();
		result.setJpegQualityLevel(((flags >> 3) & 0b111) ^ 4);
		result.setLastPacket(((flags >> 2) & 0b1) > 0);
		result.setSubsamplingMode(flags & 0b11);

		result.setMcuOffset(dis.readUnsignedByte());
		result.setMcuIndex(dis.readUnsignedShort());
		int actualPayloadLength;
		// override packet type
		if (this.payloadLength != null) {
			actualPayloadLength = this.payloadLength;
		} else {
			if (result.getPacketType() == 0x66) {
				actualPayloadLength = 205;
			} else if (result.getPacketType() == 0x67) {
				actualPayloadLength = 237;
			} else {
				throw new IOException("unknown packet type: " + result.getPacketType() + ". override payload length in constructor");
			}
		}
		byte[] payload = new byte[actualPayloadLength];
		dis.readFully(payload);
		result.setPayload(payload);
		result.setChecksum(dis.readInt());
		if (result.getPacketType() == 0x66) {
			byte[] fec = new byte[32];
			dis.readFully(fec);
			result.setFec(fec);
		}
		return result;
	}

	@Override
	public SsdvPacket next() {
		if (next == null) {
			throw new NoSuchElementException();
		}
		return next;
	}

}
