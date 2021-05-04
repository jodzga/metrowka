package org.HdrHistogram;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

import javax.xml.bind.DatatypeConverter;

import com.linkedin.metrowka.logging.HistogramSerializer;


public class Base64CompressedHistogramSerializer implements HistogramSerializer {

  private ByteBuffer targetBuffer;

  @Override
  public synchronized String serlialize(Histogram histogram) {
    int requiredBytes = histogram.getNeededByteBufferCapacity();
    if ((targetBuffer == null) || targetBuffer.capacity() < requiredBytes) {
      targetBuffer = ByteBuffer.allocate(requiredBytes);
    }
    targetBuffer.clear();

    int compressedLength = histogram.encodeIntoCompressedByteBuffer(targetBuffer, Deflater.DEFAULT_COMPRESSION);
    byte[] compressedArray = Arrays.copyOf(targetBuffer.array(), compressedLength);
    return DatatypeConverter.printBase64Binary(compressedArray);
  }

  @Override
  public Histogram deserialize(String serialized) {
    try {
      byte[] rawBytes = DatatypeConverter.parseBase64Binary(serialized);
      final ByteBuffer buffer = ByteBuffer.wrap(rawBytes);
      Histogram histogram = (Histogram) EncodableHistogram.decodeFromCompressedByteBuffer(buffer, 0);
      return histogram;
    } catch (DataFormatException e) {
      throw new RuntimeException(e);
    }
  }

}
