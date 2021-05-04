package com.linkedin.metrowka;

import org.HdrHistogram.Base64CompressedHistogramSerializer;
import org.HdrHistogram.Histogram;

public class TestSerialization {

	public static void main(String[] args) {
		Histogram h = new Histogram(3);
		Base64CompressedHistogramSerializer ser = new Base64CompressedHistogramSerializer();
		System.out.println(ser.serlialize(h));
	}

}
