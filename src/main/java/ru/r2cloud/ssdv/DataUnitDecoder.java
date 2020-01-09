package ru.r2cloud.ssdv;

import java.util.Iterator;
import java.util.NoSuchElementException;

class DataUnitDecoder implements Iterator<int[]> {

	private byte[] data;

	private boolean hasNext = false;
	private int[] currentPixels = new int[64];

	@Override
	public boolean hasNext() {
		hasNext = hasNextInternal();
		return hasNext;
	}

	private boolean hasNextInternal() {
		return false;
	}

	@Override
	public int[] next() {
		if (!hasNext) {
			throw new NoSuchElementException();
		}
		return currentPixels;
	}

	public void reset(byte[] data) {
		hasNext = false;
		this.data = data;
	}

	public void append(byte[] payload) {
		byte[] newData = new byte[data.length + payload.length];
		System.arraycopy(data, 0, newData, 0, data.length);
		System.arraycopy(payload, 0, newData, data.length, payload.length);
		this.data = newData;
	}

}
