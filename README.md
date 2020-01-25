## About [![Build Status](https://travis-ci.org/dernasherbrezon/ssdv4j.svg?branch=master)](https://travis-ci.org/dernasherbrezon/ssdv4j) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ru.r2cloud%3Assdv4j&metric=alert_status)](https://sonarcloud.io/dashboard?id=ru.r2cloud%3Assdv4j)

SSDV decoder, written in java.

## Usage

Configure maven:

```xml
<dependency>
	<groupId>ru.r2cloud</groupId>
	<artifactId>ssdv4j</artifactId>
	<version>1.1</version>
</dependency>
```

Configure source. It can be ```java.util.List<SsdvPacket>``` or ```ru.r2cloud.ssdv.SsdvInputStream```. For example:

```java
try (SsdvInputStream is = new SsdvInputStream(SsdvInputStreamTest.class.getClassLoader().getResourceAsStream("file.bin"), 189)) {
	SsdvDecoder decoder = new SsdvDecoder(is);
	while (decoder.hasNext()) {
		SsdvImage cur = decoder.next();
	}
}
```

Class ```ru.r2cloud.ssdv.SsdvImage``` contains ```java.awt.image.BufferedImage``` and some ssdv statistics.
