package ru.r2cloud.ssdv;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
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
				assertSsdvImage("output-" + cur.getImageId(), cur);
			}
		}
		assertEquals(2, total);
	}

	private static void assertSsdvImage(String expectedName, SsdvImage cur) throws IOException {
		try (InputStream is1 = SsdvDecoderTest.class.getClassLoader().getResourceAsStream(expectedName + ".png")) {
			BufferedImage expected = ImageIO.read(is1);
			for (int i = 0; i < expected.getWidth(); i++) {
				for (int j = 0; j < expected.getHeight(); j++) {
					assertEquals("failure in image: " + expectedName, expected.getRGB(i, j), cur.getImage().getRGB(i, j));
				}
			}
		}
		AssertJson.assertObjectsEqual(expectedName + ".json", cur);
	}

	// test image from 1 ssdvpacket
	// test duplicate ssdvpacket
	// test corrupted packet in between os 2 valid
}
