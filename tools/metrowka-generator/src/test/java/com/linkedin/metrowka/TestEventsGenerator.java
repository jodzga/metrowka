package com.linkedin.metrowka;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.linkedin.metrowka.logging.LogEventHistogramSerializer;
import com.linkedin.metrowka.logging.LoggingReaper;
import com.linkedin.metrowka.metrics.Hiccup;

public class TestEventsGenerator {

  public static void main(String[] args) throws InterruptedException {
    final Metrowka metrowka = new Metrowka(60*1000);
    final Harvester loggingReaper = new LoggingReaper(LoggerFactory.getLogger("metrowka"), new LogEventHistogramSerializer());

    final Hiccup hiccups = new Hiccup();
    hiccups.start();

    final Interval eventsGeneratorJitter = new Interval("eventsGeneratorJitter", 1, TimeUnit.MINUTES.toNanos(1), 3);
    EventsArrival arrivalDistribution = new PoissonEventsArrival(1000, TimeUnit.SECONDS);

    BlockingQueue<Long> queue = new LinkedBlockingDeque<Long>(1000000);
    Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    EventsGenerator generator = new EventsGenerator(arrivalDistribution, queue, executor,
        event -> {
          eventsGeneratorJitter.record(Math.abs(event.getActualNanoTimestamp() - event.getExpectedNanoTimestamp()));
        });

    generator.start(0, TimeUnit.SECONDS);

    metrowka.register(hiccups, loggingReaper);
    metrowka.register(eventsGeneratorJitter, loggingReaper);
    metrowka.start();

    TimeUnit.SECONDS.sleep(Long.MAX_VALUE);
  }

}
