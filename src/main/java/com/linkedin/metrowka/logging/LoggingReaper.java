package com.linkedin.metrowka.logging;

import org.HdrHistogram.Histogram;
import org.slf4j.Logger;

import com.linkedin.metrowka.Harvester;
import com.linkedin.metrowka.InstrumentType;

public class LoggingReaper implements Harvester {

  private final HistogramEventSerializer _serializer;

  private final Logger _logger;

  public LoggingReaper(Logger logger, HistogramEventSerializer serializer) {
    _logger = logger;
    _serializer = serializer;
  }

  @Override
  public void harvest(Histogram histogram, InstrumentType type, String name) {
    _logger.info(_serializer.serialize(histogram, type, name));
  }

}
