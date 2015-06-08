package com.linkedin.metrowka.logging;

import org.HdrHistogram.Histogram;

import com.linkedin.metrowka.InstrumentType;

public interface HistogramEventSerializer {

  String serialize(Histogram histogram, InstrumentType type, String name);

  HistogramEvent deserialize(String logEventString);

}
