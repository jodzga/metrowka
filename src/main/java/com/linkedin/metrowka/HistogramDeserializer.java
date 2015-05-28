package com.linkedin.metrowka;

import org.HdrHistogram.Histogram;

public interface HistogramDeserializer {

  Histogram deserialize(String serialized);

}
