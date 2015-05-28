package com.linkedin.metrowka;

import org.HdrHistogram.Histogram;

@FunctionalInterface
public interface Harvester {
  void harvest(Histogram histogram, InstrumentType type, String name);
}
