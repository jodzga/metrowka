package com.linkedin.parseq.internal.metrics;

public class LongMeasurement {

  public LongMeasurement(long value, long timestamp) {
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
