package ru.r2cloud.ssdv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class Base40Test {

	@Test
	public void test() {
		assertEquals("SORA", Base40.decode(0x000e7240));
	}

	@Test
	public void testInvalid() {
		assertNull(Base40.decode(0L));
	}
	
	@Test
	public void testInvalid2() {
		assertNull(Base40.decode(0xF4240000L));
	}
	
}
