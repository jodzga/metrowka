package com.linkedin.metrowka;

import java.util.concurrent.TimeUnit;

public class UniformEventsArrival implements EventsArrival {
  private final double _nanosToNextEvent;

  public UniformEventsArrival(double events, TimeUnit perUnit) {
    _nanosToNextEvent = perUnit.toNanos(1) / events;
  }

  @Override
  public long nanosToNextEvent() {
    return (long)_nanosToNextEvent;
  }
}
