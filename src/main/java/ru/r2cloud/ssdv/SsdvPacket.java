package ru.r2cloud.ssdv;

public class SsdvPacket {

	private int packetType;
	private int imageId;
	private int packetId;
	private int widthMcu;
	private int heightMcu;

	private int jpegQualityLevel;
	private boolean lastPacket;
	private int subsamplingMode;

	private int mcuOffset;
	private int mcuIndex;

	private byte[] payload;

	public int getPacketType() {
		return packetType;
	}

	public void setPacketType(int packetType) {
		this.packetType = packetType;
	}

	public int getImageId() {
		return imageId;
	}

	public void setImageId(int imageId) {
		this.imageId = imageId;
	}

	public int getPacketId() {
		return packetId;
	}

	public void setPacketId(int packetId) {
		this.packetId = packetId;
	}

	public int getWidthMcu() {
		return widthMcu;
	}

	public void setWidthMcu(int widthMcu) {
		this.widthMcu = widthMcu;
	}

	public int getHeightMcu() {
		return heightMcu;
	}

	public void setHeightMcu(int heightMcu) {
		this.heightMcu = heightMcu;
	}

	public int getJpegQualityLevel() {
		return jpegQualityLevel;
	}

	public void setJpegQualityLevel(int jpegQualityLevel) {
		this.jpegQualityLevel = jpegQualityLevel;
	}

	public boolean isLastPacket() {
		return lastPacket;
	}

	public void setLastPacket(boolean lastPacket) {
		this.lastPacket = lastPacket;
	}

	public int getSubsamplingMode() {
		return subsamplingMode;
	}

	public void setSubsamplingMode(int subsamplingMode) {
		this.subsamplingMode = subsamplingMode;
	}

	public int getMcuOffset() {
		return mcuOffset;
	}

	public void setMcuOffset(int mcuOffset) {
		this.mcuOffset = mcuOffset;
	}

	public int getMcuIndex() {
		return mcuIndex;
	}

	public void setMcuIndex(int mcuIndex) {
		this.mcuIndex = mcuIndex;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

}
