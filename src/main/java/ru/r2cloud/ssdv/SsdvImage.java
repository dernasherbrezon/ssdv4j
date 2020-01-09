package ru.r2cloud.ssdv;

import java.awt.image.BufferedImage;

public class SsdvImage {

	private int imageId;
	private int successfulMcu;
	private int totalMcu;
	private BufferedImage image;

	public int getImageId() {
		return imageId;
	}

	public void setImageId(int imageId) {
		this.imageId = imageId;
	}

	public int getSuccessfulMcu() {
		return successfulMcu;
	}

	public void setSuccessfulMcu(int successfulMcu) {
		this.successfulMcu = successfulMcu;
	}

	public int getTotalMcu() {
		return totalMcu;
	}

	public void setTotalMcu(int totalMcu) {
		this.totalMcu = totalMcu;
	}

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

}
