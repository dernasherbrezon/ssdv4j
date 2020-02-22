package ru.r2cloud.ssdv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.util.NoSuchElementException;

import org.junit.Test;

public class SsdvInputStreamTest {

	@Test
	public void testReadFromStream() throws Exception {
		int total = 0;
		try (SsdvInputStream is = new SsdvInputStream(SsdvInputStreamTest.class.getClassLoader().getResourceAsStream("jy1sat.bin"), 189)) {
			while (is.hasNext()) {
				total++;
			}
		}
		assertEquals(99, total);
	}

	@Test(expected = NoSuchElementException.class)
	public void testIllegalToCallNextFirst() throws Exception {
		try (SsdvInputStream is = new SsdvInputStream(SsdvInputStreamTest.class.getClassLoader().getResourceAsStream("jy1sat.bin"), 189)) {
			is.next();
		}
	}

	@Test
	public void testEof() throws Exception {
		try (SsdvInputStream is = new SsdvInputStream(new ByteArrayInputStream(new byte[] { 0x55, 0x66, 0x0, 0x0 }))) {
			assertFalse(is.hasNext());
		}
	}

}
