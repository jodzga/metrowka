package com.linkedin.metrowka;

import org.HdrHistogram.Histogram;

public interface HistogramSerializer {

  String serlialize(Histogram histogram);

}
