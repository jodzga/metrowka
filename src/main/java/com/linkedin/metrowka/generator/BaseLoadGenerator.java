package com.linkedin.metrowka.generator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.metrowka.Harvester;
import com.linkedin.metrowka.Interval;
import com.linkedin.metrowka.Metrowka;
import com.linkedin.metrowka.logging.LogEventHistogramSerializer;
import com.linkedin.metrowka.logging.LoggingReaper;
import com.linkedin.metrowka.metrics.vm.Hiccup;


public abstract class BaseLoadGenerator {

  private static final Logger _logger = LoggerFactory.getLogger(BaseLoadGenerator.class);

  final double _rate;
  final TimeUnit _rateUnit;
  final long _duration;
  final TimeUnit _durationUnit;

  final AtomicLong _successes = new AtomicLong(0);
  final AtomicLong _failures = new AtomicLong(0);

  final AtomicLong _started = new AtomicLong(0);
  final AtomicLong _completed = new AtomicLong(0);

  abstract void initialize();

  abstract void cleanUp();

  abstract void action(Consumer<Boolean> callback);

  public BaseLoadGenerator(double rate, TimeUnit rateUnit, long duration, TimeUnit durationUnit) {
    _rate = rate;
    _rateUnit = rateUnit;
    _duration = duration;
    _durationUnit = durationUnit;
  }

  public void run() {

    _logger.info("Initializing load generator");

    initialize();

    _logger.info("Starting load generation with rate: " + _rate + " per " + _rateUnit.name() + ", duration: "
        + _duration + " " + _durationUnit.name());

    final Metrowka metrowka = new Metrowka(60 * 1000);
    final Harvester loggingReaper =
        new LoggingReaper(LoggerFactory.getLogger("metrowka"), new LogEventHistogramSerializer());

    final Hiccup hiccups = new Hiccup();
    hiccups.start();

    final Interval eventsGeneratorJitter = new Interval("eventsGeneratorJitter", 1, TimeUnit.MINUTES.toNanos(1), 3);
    final EventsArrival arrivalDistribution = new PoissonEventsArrival(_rate, _rateUnit);

    final Interval latency = new Interval("latency", 1, TimeUnit.MINUTES.toNanos(1), 3);

    final BlockingQueue<Long> queue = new LinkedBlockingDeque<Long>(100000);
    final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

    final EventsGenerator generator = new EventsGenerator(arrivalDistribution, queue, executor, event -> {
      eventsGeneratorJitter.record(Math.abs(event.getActualNanoTimestamp() - event.getExpectedNanoTimestamp()));

      final Consumer<Boolean> completionHandler = isSuccess -> {
        if (isSuccess) {
          _successes.incrementAndGet();
          latency.record(System.nanoTime() - event.getActualNanoTimestamp());
        } else {
          _failures.incrementAndGet();
        }
        _completed.incrementAndGet();
      };

      action(completionHandler);
      _started.incrementAndGet();

    });

    generator.start(5, TimeUnit.SECONDS);

    metrowka.register(hiccups, loggingReaper);
    metrowka.register(eventsGeneratorJitter, loggingReaper);
    metrowka.register(latency, loggingReaper);
    metrowka.start();

    try {
      _durationUnit.sleep(_duration);
    } catch (InterruptedException e) {
      _logger.error("Interrupted while waiting for load generator to complete");
      Thread.currentThread().interrupt();
    }

    generator.stop();
    metrowka.stop();
    hiccups.stop();
    executor.shutdown();

    _logger.info("Finished load generator, successful: " + _successes.get() + ", failed: " + _failures.get()
        + ", started: " + _started.get() + ", completed: " + _completed.get());

    cleanUp();
  }

}
