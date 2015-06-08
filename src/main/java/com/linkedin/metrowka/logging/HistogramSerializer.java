package com.linkedin.metrowka.logging;

import org.HdrHistogram.Histogram;

public interface HistogramSerializer {

  String serlialize(Histogram histogram);

  Histogram deserialize(String serialized);

}
