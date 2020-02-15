package ru.r2cloud.ssdv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
	public void testDslwpSuccess() throws Exception {
		assertData("dslwp", 1, null);
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

	@Test
	public void testAllPacketsCorrupted() throws Exception {
		SsdvPacket packet = createValidPacket();
		packet.setPayload(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
		SsdvPacket packet2 = createValidPacket();
		packet2.setPayload(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
		packet2.setImageId(10);
		List<SsdvPacket> source = new ArrayList<>();
		source.add(packet);
		source.add(packet2);
		SsdvDecoder decoder = new SsdvDecoder(source);
		assertFalse(decoder.hasNext());
	}

	@Test(expected = NoSuchElementException.class)
	public void testIllegalToCallNextFirst() throws Exception {
		new SsdvDecoder(new ArrayList<>()).next();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnsupportedSubSamplingMode() throws Exception {
		SsdvPacket packet = createValidPacket();
		packet.setSubsamplingMode(22);
		SsdvDecoder decoder = new SsdvDecoder(Collections.singletonList(packet));
		decoder.hasNext();
	}

	@Test
	public void testInvalidHeightWidth() throws Exception {
		SsdvPacket packet = createValidPacket();
		packet.setHeightMcu(0);
		assertPacketInvalid(packet);
	}

	@Test
	public void testInvalidHeightWidth2() throws Exception {
		SsdvPacket packet = createValidPacket();
		packet.setHeightMcu(65535);
		assertPacketInvalid(packet);
	}

	@Test
	public void testInvalidPayload() throws Exception {
		SsdvPacket packet = createValidPacket();
		packet.setPayload(null);
		assertPacketInvalid(packet);
	}

	@Test
	public void testInvalidMcuOffset() throws Exception {
		SsdvPacket packet = createValidPacket();
		packet.setPayload(new byte[2]);
		packet.setMcuOffset(3);
		assertPacketInvalid(packet);
	}

	@Test
	public void testInvalidPacketId() throws Exception {
		SsdvPacket packet = createValidPacket();
		packet.setPacketId(65537);
		assertPacketInvalid(packet);
	}

	private static void assertPacketInvalid(SsdvPacket packet) {
		SsdvDecoder decoder = new SsdvDecoder(Collections.singletonList(packet));
		assertFalse(decoder.hasNext());
	}

	private static void assertData(String prefix, int expected, Integer payloadLength) throws IOException {
		int actual = 0;
		try (SsdvInputStream is = new SsdvInputStream(SsdvInputStreamTest.class.getClassLoader().getResourceAsStream(prefix + ".bin"), payloadLength)) {
			SsdvDecoder decoder = new SsdvDecoder(is);
			while (decoder.hasNext()) {
				SsdvImage cur = decoder.next();
				actual++;
				assertSsdvImage(prefix + "-" + cur.getImageId(), cur);
			}
		}
		assertEquals(expected, actual);
	}

	private static void assertData(String prefix, int expected) throws IOException {
		assertData(prefix, expected, 189);
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

	private static SsdvPacket createValidPacket() {
		SsdvPacket result = new SsdvPacket();
		result.setPacketType(104);
		result.setImageId(9);
		result.setPacketId(1);
		result.setWidthMcu(23);
		result.setHeightMcu(41);
		result.setJpegQualityLevel(2);
		result.setLastPacket(false);
		result.setSubsamplingMode(0);
		result.setMcuOffset(2);
		result.setMcuIndex(42);
		result.setPayload(new byte[] { 20, 1, -65, 69, 45, 37, 49, 9, 69, 45, 37, 2, 18, -118, 90, 41, -128, -108, -108, -76, 80, 3, -88, -94, -106, -112, -60, -91, -94, -118, 0, 40, -91, -94, -128, 10, 41, 104, -96, 4, -91, -94, -118, 64, 20, 82, -47, 64, 9, 75, 69, 20, 12, 40, -94, -106, -128, 18, -118, 90, 40, 1, 40, -91, -94, -128, 18, -118, 90, 40, 1, 40, -91, -94, -128, 18, -118, 90, 40, 1, 40, -91, -94, -128, 18, -118, 90, 40, 1, 40, -91, -92, -96, 4, -94, -106, -118, 0, 74, 74, 90, 40,
				16, -108, 82, -46, 80, 2, 81, 75, 69, 48, 18, -110, -106, -118, 0, 74, 74, 90, 40, 1, 40, -91, -92, -96, 7, 81, 75, 69, 0, 20, 81, 75, 72, 2, -118, 41, 104, 24, -108, -76, 81, 64, 5, 20, -76, 80, 2, 127, -102, 90, 40, -96, 2, -118, 40, -92, 1, 69, 20, -76, 0, -108, 82, -47, 64, -60, -91, -94, -118, 0, 40, -94, -118, 0, 40, -94, -118, 0, 40, -94, -118, 0, 40, -94, -118, 0, 74 });
		result.setChecksum(0);
		result.setFec(null);
		return result;
	}

}
