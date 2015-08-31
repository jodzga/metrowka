package com.linkedin.metrowka;

import java.util.concurrent.atomic.AtomicReference;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

public class Gauge extends Harvestable {

  private final Recorder _recorder;
  private final AtomicReference<Measurement> _lastMeasurment = new AtomicReference<Measurement>();
  private Histogram _recycle;

  public Gauge(final String name, final long lowestDiscernibleValue, final long highestTrackableValue, final int numberOfSignificantValueDigits) {
    super(InstrumentType.gauge, MeasureUnit.other, name);
    _recorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
  }

  public void update(final long value, final long currentNano) {
    Measurement last;
    final Measurement current = new Measurement(value, currentNano);
    do {
      last = _lastMeasurment.get();
    } while ((last == null || (currentNano - last.getTimestamp()) > 0 ) && (!_lastMeasurment.compareAndSet(last, current)));
    if (last != null && (currentNano - last.getTimestamp()) > 0) {
      _recorder.recordValueWithCount(last.getValue(), currentNano - last.getTimestamp());
    }
  }

  @Override
  public synchronized void harvest(Harvester harvester) {
    _recycle = _recorder.getIntervalHistogram(_recycle);
    harvester.harvest(_recycle, getType(), getName());
  }

  private static class Measurement {

    public Measurement(long value, long timestamp) {
      _value = value;
      _timestamp = timestamp;
    }

    private final long _value;
    private final long _timestamp;

    public long getValue() {
      return _value;
    }

    public long getTimestamp() {
      return _timestamp;
    }
  }
}
