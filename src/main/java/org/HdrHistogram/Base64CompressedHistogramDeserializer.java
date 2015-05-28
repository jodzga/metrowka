package org.HdrHistogram;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

import javax.xml.bind.DatatypeConverter;

import org.HdrHistogram.EncodableHistogram;
import org.HdrHistogram.Histogram;

import com.linkedin.metrowka.HistogramDeserializer;


public class Base64CompressedHistogramDeserializer implements HistogramDeserializer {

  @Override
  public Histogram deserialize(String serialized) {
    try {
      byte[] rawBytes = DatatypeConverter.parseBase64Binary(serialized);
      final ByteBuffer buffer = ByteBuffer.wrap(rawBytes, 0, rawBytes.length - 16);
      Histogram histogram = (Histogram) EncodableHistogram.decodeFromCompressedByteBuffer(buffer, 0);
      final ByteBuffer timestamps = ByteBuffer.wrap(rawBytes, rawBytes.length - 16, 16);
      histogram.setStartTimeStamp(timestamps.getLong(0));
      histogram.setEndTimeStamp(timestamps.getLong(8));
      return histogram;
    } catch (DataFormatException e) {
      throw new RuntimeException(e);
    }
  }

}
