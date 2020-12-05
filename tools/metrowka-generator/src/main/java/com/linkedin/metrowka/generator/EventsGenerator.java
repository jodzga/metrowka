package com.linkedin.metrowka.generator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.metrowka.Metrowka;

public class EventsGenerator {

  private static final Logger _logger = LoggerFactory.getLogger(Metrowka.class);

  private final EventsArrival _distribution;
  private final BlockingQueue<Long> _events;
  private volatile boolean _stopped = false;
  private final CountDownLatch _latch = new CountDownLatch(1);
  private long _initialDelay = 0;
  private final Executor _executor;
  private final Consumer<Event> _consumer;

  public EventsGenerator(EventsArrival distribution, BlockingQueue<Long> queue, Executor executor, Consumer<Event> consumer) {
    _distribution = distribution;
    _events = queue;
    _executor = executor;
    _consumer = consumer;
  }

  public BlockingQueue<Long> getEventsQueue() {
    return _events;
  }

  public void start(long initialDelay, TimeUnit tu) {

    _logger.info("starting event generator with initial delay of " + initialDelay + " " + tu.toString());

    _executor.execute(() -> {
      try {
        _latch.await();
        _logger.info("starting queue feeder");
        long nextNano = System.nanoTime() + _initialDelay + _distribution.nanosToNextEvent();
        while (!_stopped) {
          if (_events.offer(nextNano, 1, TimeUnit.SECONDS)) {
            nextNano = nextNano + _distribution.nanosToNextEvent();
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      _logger.info("stopped queue feeder");
    });

    for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
      _executor.execute(() -> {
        try {
          _logger.info("starting queue worker");
          while (!_stopped) {
            Long nextNano = _events.poll(1, TimeUnit.SECONDS);
            if (nextNano != null) {
              long actualNano = waitUntil(nextNano);
              _consumer.accept(new Event(nextNano, actualNano));
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        _logger.info("stopped queue worker");
      });
    }

    _initialDelay = tu.toNanos(initialDelay);
    _latch.countDown();
  }

  private static long waitUntil(long nextNano) throws InterruptedException {
    long current = System.nanoTime();
    if ((nextNano - current) > 0) {
      return waitNano(nextNano, current);
    } else {
      return current;
    }
  }

  private static long waitNano(long nextNano, long current) throws InterruptedException {
    long waitTime = nextNano - current;
    long millis = (waitTime >> 20) - 1;  //2^20ns = 1048576ns ~ 1ms
    if (millis < 0) {
      millis = 0;
    }
    if (millis > 0) {
      Thread.sleep(millis);
      return waitUntil(nextNano);
    } else {
      return busyWaitUntil(nextNano);
    }
  }

  private static long busyWaitUntil(long nextNano) {
    long counter = 0L;
    while (true) {
      counter += 1;
      if (counter % 1000 == 0) {
        long current = System.nanoTime();
        if (current - nextNano >= 0) {
          return current;
        }
      }
    }
  }

  public void stop() {
    _logger.info("stopping event generator");
    _stopped = true;
    _latch.countDown();
  }

  public static class Event {
    private final long _expectedNanoTimestamp;
    private final long _actualNanoTimestamp;

    public Event(long expectedNanoTimestamp, long actualNanoTimestamp) {
      _expectedNanoTimestamp = expectedNanoTimestamp;
      _actualNanoTimestamp = actualNanoTimestamp;
    }

    public long getExpectedNanoTimestamp() {
      return _expectedNanoTimestamp;
    }

    public long getActualNanoTimestamp() {
      return _actualNanoTimestamp;
    }

    @Override
    public String toString() {
      return "Event [expectedNanoTimestamp=" + _expectedNanoTimestamp + ", actualNanoTimestamp="
          + _actualNanoTimestamp + ", diff=" + (_actualNanoTimestamp - _expectedNanoTimestamp) + "]";
    }

  }

}
