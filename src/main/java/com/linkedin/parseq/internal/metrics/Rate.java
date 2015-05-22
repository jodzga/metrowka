package com.linkedin.parseq.internal.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

public class Rate {

  private final Recorder _recorder;
  private final AtomicLong _lastNano = new AtomicLong(0);
  private Histogram _recycle;

   // Specifies smallest registered rate, equivalent to 1/2^48ns that is 1/3.4day.
  public static final long MAX_INTERVAL_BETWEEN_EVENTS_IN_NS = 1L << 48;

  public Rate(final long lowestDiscernibleValue, final long highestTrackableValue, final int numberOfSignificantValueDigits) {
    _recorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
  }

  public void record(final long count, final long currentNano) {
    long last;
    do {
      last = _lastNano.get();
    } while ((last == 0 || last < currentNano) && (!_lastNano.compareAndSet(last, currentNano)));
    //TODO handle case when timestamp has not changed
    if (last != 0 && last < currentNano) {
      long delta = currentNano - last;
      _recorder.recordValueWithCount(scale(count, delta), delta);
    }
  }

  private long scale(long count, long delta) {
    return count * (MAX_INTERVAL_BETWEEN_EVENTS_IN_NS / delta);
  }

  public synchronized void harvest(Consumer<Histogram> consumer) {
    _recycle = _recorder.getIntervalHistogram(_recycle);
    consumer.accept(_recycle);
  }

}
