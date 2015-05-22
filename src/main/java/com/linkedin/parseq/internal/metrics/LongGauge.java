package com.linkedin.parseq.internal.metrics;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

public class LongGauge {

  private final Recorder _recorder;
  private final AtomicReference<LongMeasurement> _lastMeasurment = new AtomicReference<LongMeasurement>();
  private Histogram _recycle;

  public LongGauge(final long lowestDiscernibleValue, final long highestTrackableValue, final int numberOfSignificantValueDigits) {
    _recorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
  }

  public void update(final long value, final long currentNano) {
    LongMeasurement last;
    final LongMeasurement current = new LongMeasurement(value, currentNano);
    do {
      last = _lastMeasurment.get();
    } while ((last == null || last.getTimestamp() < currentNano) && (!_lastMeasurment.compareAndSet(last, current)));
    if (last != null && last.getTimestamp() < currentNano) {
      _recorder.recordValueWithCount(last.getValue(), currentNano - last.getTimestamp());
    }
  }

  public synchronized void harvest(Consumer<Histogram> consumer) {
    _recycle = _recorder.getIntervalHistogram(_recycle);
    consumer.accept(_recycle);
  }

}
