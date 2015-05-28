package com.linkedin.metrowka;

import org.HdrHistogram.Base64CompressedHistogramSerializer;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingReaper implements Harvester {

  private static final Base64CompressedHistogramSerializer _serializer =
      new  Base64CompressedHistogramSerializer();

  private final Logger _logger;

  private LoggingReaper(Logger logger) {
    _logger = logger;
  }

  public static LoggingReaper forLogger(String loggerName) {
    return new LoggingReaper(LoggerFactory.getLogger(loggerName));
  }

  @Override
  public void harvest(Histogram histogram, InstrumentType type, String name) {
    _logger.info(type + ":" + name + ":" + _serializer.serlialize(histogram));
  }

}
