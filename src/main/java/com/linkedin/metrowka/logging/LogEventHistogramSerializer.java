package com.linkedin.metrowka.logging;

import org.HdrHistogram.Base64CompressedHistogramSerializer;
import org.HdrHistogram.Histogram;

import com.linkedin.metrowka.InstrumentType;


public class LogEventHistogramSerializer implements HistogramEventSerializer {

  private static final char SEPARATOR_CH = ':';
  private static final String SEPARATOR_STR = ":";
  private final HistogramSerializer _serializer = new Base64CompressedHistogramSerializer();

  @Override
  public String serialize(Histogram histogram, InstrumentType type, String name) {
    return type + SEPARATOR_STR + name + SEPARATOR_STR + _serializer.serlialize(histogram);
  }

  @Override
  public HistogramEvent deserialize(String logEventString) {
    int firstIndex = logEventString.indexOf(SEPARATOR_CH, 0);
    if (firstIndex < 0) {
      throw new RuntimeException(
          "incorrect format of histogram log event, expected to find character '" + SEPARATOR_CH + "'");
    }
    int secondIndex = logEventString.indexOf(SEPARATOR_CH, firstIndex + 1);
    if (secondIndex < 0) {
      throw new RuntimeException(
          "incorrect format of histogram log event, expected to find character '" + SEPARATOR_CH + "' at least twice");
    }
    return new HistogramEvent(_serializer.deserialize(logEventString.substring(secondIndex + 1)),
        InstrumentType.valueOf(logEventString.substring(0, firstIndex)),
        logEventString.substring(firstIndex + 1, secondIndex));
  }

}
