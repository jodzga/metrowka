package com.linkedin.metrowka.metrics;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.metrowka.Interval;


public class Hiccup extends Interval {

  private static final Logger _logger = LoggerFactory.getLogger(Hiccup.class);

  private static final long _lowestTrackableValue = 1000L * 20L; // default to ~20usec best-case resolution
  private static final long _highestTrackableValue = 3600 * 1000L * 1000L * 1000L;
  private static final int _numberOfSignificantValueDigits = 2;

  private volatile boolean _stopped = false;

  public Hiccup() {
    super("hiccup", _lowestTrackableValue, _highestTrackableValue, _numberOfSignificantValueDigits);
  }

  public void start() {
    new Thread(() -> {
      final long resolutionNsec = 1000L * 1000L; //1ms
      try {
        long shortestObservedDeltaTimeNsec = Long.MAX_VALUE;
        while (!_stopped) {
          final long timeBeforeMeasurement = System.nanoTime();
          TimeUnit.NANOSECONDS.sleep(resolutionNsec);
          final long timeAfterMeasurement = System.nanoTime();
          final long deltaTimeNsec = timeAfterMeasurement - timeBeforeMeasurement;

          if (deltaTimeNsec < shortestObservedDeltaTimeNsec) {
            shortestObservedDeltaTimeNsec = deltaTimeNsec;
          }

          long hiccupTimeNsec = deltaTimeNsec - shortestObservedDeltaTimeNsec;

          recordWithExpectedInterval(hiccupTimeNsec, resolutionNsec);
        }
      } catch (InterruptedException e) {
        _logger.info("hiccup interrupted, terminating...");
      }
    } , "hiccup").start();
  }

  public void stop() {
    _stopped = true;
  }

}
