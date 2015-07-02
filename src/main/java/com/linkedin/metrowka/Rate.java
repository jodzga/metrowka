package com.linkedin.metrowka;

import java.util.concurrent.atomic.AtomicLong;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

public class Rate extends Harvestable {

  private final Recorder _recorder;
  private final AtomicLong _lastNano = new AtomicLong(0);
  private Histogram _recycle;

   // Specifies smallest registered rate, equivalent to 1/2^48ns that is 1/3.4day.
  public static final long MAX_INTERVAL_BETWEEN_EVENTS_IN_NS = 1L << 48;

  public Rate(final String name, final long lowestDiscernibleValue, final long highestTrackableValue, final int numberOfSignificantValueDigits) {
    super(InstrumentType.rate, name);
    _recorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
  }

  public void record(final long count, final long currentNano) {
    if (count > 0) {
      long last = 0;
      do {
        last = _lastNano.get();
      } while ((last == 0 || (currentNano - last) > 0) && (!_lastNano.compareAndSet(last, currentNano)));
      if (last != 0) {
        if ((currentNano - last) > 0) {
          long delta = currentNano - last;
          _recorder.recordValueWithCount(scale(count, delta), delta);
        } else {
          //if there is a time measurement jitter e.g. last >= current
          //we attribute event to one ns time period
          _recorder.recordValueWithCount(count * MAX_INTERVAL_BETWEEN_EVENTS_IN_NS, 1);
        }
      }
    }
  }

  private long scale(long count, long delta) {
    return count * (MAX_INTERVAL_BETWEEN_EVENTS_IN_NS / delta);
  }

  @Override
  public synchronized void harvest(Harvester harvester) {
    _recycle = _recorder.getIntervalHistogram(_recycle);
    harvester.harvest(_recycle, getType(), getName());
  }

}
