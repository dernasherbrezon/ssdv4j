package ru.r2cloud.ssdv;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Test;

public class SsdvDecoderTest {

	@Test
	public void testSuccess() throws Exception {
		int total = 0;
		try (SsdvInputStream is = new SsdvInputStream(SsdvInputStreamTest.class.getClassLoader().getResourceAsStream("jy1sat.bin"), 189)) {
			SsdvDecoder decoder = new SsdvDecoder(is);
			while (decoder.hasNext()) {
				SsdvImage cur = decoder.next();
				total++;
				try (InputStream is1 = SsdvDecoderTest.class.getClassLoader().getResourceAsStream("output-" + cur.getImageId() + ".png")) {
					BufferedImage expected = ImageIO.read(is1);
					for (int i = 0; i < expected.getWidth(); i++) {
						for (int j = 0; j < expected.getHeight(); j++) {
							assertEquals("failure in image: " + cur.getImageId(), expected.getRGB(i, j), cur.getImage().getRGB(i, j));
						}
					}
				}				
			}
		}
		assertEquals(2, total);
	}

	// test image from 1 ssdvpacket
	// test duplicate ssdvpacket
	// test corrupted packet in between os 2 valid
}
