package com.linkedin.metrowka;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

public class Latency extends Harvestable {

  private final Recorder _recorder;
  private Histogram _recycle;

  public Latency(final String name, final long lowestDiscernibleValue, final long highestTrackableValue, final int numberOfSignificantValueDigits) {
    super(InstrumentType.latency, name);
    _recorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
  }

  public void record(final long latency) {
    _recorder.recordValue(latency);
  }

  @Override
  public synchronized void harvest(Harvester harvester) {
    _recycle = _recorder.getIntervalHistogram(_recycle);
    harvester.harvest(_recycle, getType(), getName());
  }
}
