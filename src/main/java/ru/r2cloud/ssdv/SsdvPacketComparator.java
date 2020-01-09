package ru.r2cloud.ssdv;

import java.util.Comparator;

public class SsdvPacketComparator implements Comparator<SsdvPacket> {

	public static final SsdvPacketComparator INSTANCE = new SsdvPacketComparator();

	@Override
	public int compare(SsdvPacket o1, SsdvPacket o2) {
		int result = Integer.compare(o1.getImageId(), o2.getImageId());
		if (result != 0) {
			return result;
		}
		return Integer.compare(o1.getPacketId(), o2.getPacketId());
	}

}
