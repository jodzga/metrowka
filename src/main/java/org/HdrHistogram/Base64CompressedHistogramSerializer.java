package org.HdrHistogram;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;

import javax.xml.bind.DatatypeConverter;

import org.HdrHistogram.Histogram;

import com.linkedin.metrowka.HistogramSerializer;


public class Base64CompressedHistogramSerializer implements HistogramSerializer {

  private ByteBuffer targetBuffer;

  @Override
  public synchronized String serlialize(Histogram histogram) {
    int requiredBytes = histogram.getNeededByteBufferCapacity() + 16;
    if ((targetBuffer == null) || targetBuffer.capacity() < requiredBytes) {
      targetBuffer = ByteBuffer.allocate(requiredBytes);
    }
    targetBuffer.clear();

    int compressedLength = histogram.encodeIntoCompressedByteBuffer(targetBuffer, Deflater.BEST_COMPRESSION);
    targetBuffer.putLong(compressedLength, histogram.getStartTimeStamp());
    targetBuffer.putLong(compressedLength + 8, histogram.getEndTimeStamp());
    byte[] compressedArray = Arrays.copyOf(targetBuffer.array(), compressedLength + 16);
    return DatatypeConverter.printBase64Binary(compressedArray);
  }

}
