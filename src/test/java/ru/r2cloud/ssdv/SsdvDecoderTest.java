package ru.r2cloud.ssdv;

import static org.junit.Assert.assertEquals;

import java.io.File;

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
				ImageIO.write(cur.getImage(), "jpg", new File("output-" + total + ".jpg"));
				total++;
				// FIXME
				break;
			}
		}
		assertEquals(2, total);
	}

	// test image from 1 ssdvpacket
	// test duplicate ssdvpacket
	// test corrupted packet in between os 2 valid
}
