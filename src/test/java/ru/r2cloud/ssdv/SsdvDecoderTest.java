package ru.r2cloud.ssdv;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;

import org.junit.Test;

public class SsdvDecoderTest {

	@Test
	public void testSuccess() throws Exception {
		assertData("jy1sat", 2);
	}

	@Test
	public void test1Packet() throws Exception {
		assertData("1packet", 1);
	}

	@Test
	public void testDuplicatePacket() throws Exception {
		assertData("duplicate", 1);
	}

	@Test
	public void testOutOfOrderPackets() throws Exception {
		String prefixSource = "3packets";
		String prefixExpected = "outoforder";
		List<SsdvPacket> packets = new ArrayList<>();
		try (SsdvInputStream is = new SsdvInputStream(SsdvInputStreamTest.class.getClassLoader().getResourceAsStream(prefixSource + ".bin"), 189)) {
			while (is.hasNext()) {
				packets.add(is.next());
			}
		}
		Collections.shuffle(packets);
		SsdvDecoder decoder = new SsdvDecoder(packets);
		int actual = 0;
		while (decoder.hasNext()) {
			SsdvImage cur = decoder.next();
			actual++;
			assertSsdvImage(prefixExpected + "-" + cur.getImageId(), cur);
		}
		assertEquals(1, actual);
	}

	@Test
	public void testCorrupted() throws Exception {
		// specially handcrafted corrupted packets
		// first packet is ok, second has payload 0xFF, third is ok
		assertData("corrupted", 1);
	}

	@Test(expected = NoSuchElementException.class)
	public void testIllegalToCallNextFirst() throws Exception {
		new SsdvDecoder(new ArrayList<>()).next();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnsupportedSubSamplingMode() throws Exception {
		SsdvPacket packet = new SsdvPacket();
		packet.setSubsamplingMode(22);
		packet.setHeightMcu(2);
		packet.setWidthMcu(2);
		SsdvDecoder decoder = new SsdvDecoder(Collections.singletonList(packet));
		decoder.hasNext();
	}

	private static void assertData(String prefix, int expected) throws IOException {
		int actual = 0;
		try (SsdvInputStream is = new SsdvInputStream(SsdvInputStreamTest.class.getClassLoader().getResourceAsStream(prefix + ".bin"), 189)) {
			SsdvDecoder decoder = new SsdvDecoder(is);
			while (decoder.hasNext()) {
				SsdvImage cur = decoder.next();
				actual++;
				assertSsdvImage(prefix + "-" + cur.getImageId(), cur);
			}
		}
		assertEquals(expected, actual);
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

}
