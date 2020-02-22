package ru.r2cloud.ssdv;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

class Base40 {

	static String decode(long code) {
		if (code > 0xF423FFFFL || code == 0) {
			return null;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		long s;
		while (code > 0) {
			s = code % 40;
			if (s == 0) {
				baos.write((byte) '-');
			} else if (s < 11) {
				baos.write((byte) '0' + (byte) (s - 1));
			} else if (s < 14) {
				baos.write((byte) '-');
			} else {
				baos.write((byte) 'A' + (byte) (s - 14));
			}
			code /= 40;
		}
		return new String(baos.toByteArray(), StandardCharsets.ISO_8859_1);
	}

	private Base40() {
		// do nothing
	}
}
