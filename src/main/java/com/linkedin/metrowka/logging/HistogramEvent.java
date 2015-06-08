package com.linkedin.metrowka.logging;

import org.HdrHistogram.Histogram;

import com.linkedin.metrowka.InstrumentType;

public class HistogramEvent {

  private final Histogram _histogram;
  private final InstrumentType _type;
  private final String _name;

  public HistogramEvent(Histogram histogram, InstrumentType type, String name) {
    _histogram = histogram;
    _type = type;
    _name = name;
  }

  public Histogram getHistogram() {
    return _histogram;
  }

  public InstrumentType getType() {
    return _type;
  }

  public String getName() {
    return _name;
  }

}
