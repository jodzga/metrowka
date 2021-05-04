package com.linkedin.metrowka.generator;

import static org.asynchttpclient.Dsl.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.metrowka.Harvester;
import com.linkedin.metrowka.Interval;
import com.linkedin.metrowka.Metrowka;
import com.linkedin.metrowka.logging.LogEventHistogramSerializer;
import com.linkedin.metrowka.logging.LoggingReaper;
import com.linkedin.metrowka.metrics.vm.Hiccup;

import static org.asynchttpclient.Dsl.*;

public class SimpleLoadGenerator {

  private static final Logger _logger = LoggerFactory.getLogger(SimpleLoadGenerator.class);
  private static final long INITIAL_DELAY_SECONDS = 5;

  public static void main(String[] args) throws Exception {

    final double rps = Double.parseDouble(args[0]);
    final long durationSeconds = Long.parseLong(args[1]);
    final String url = args[2];
        //"http://localhost:7479/restli-perf-pegasus-server/asyncLongKey/0?delay=0&fixture=small";

    _logger.info("RPS: " + rps + ", duration: " + durationSeconds + "s (after " + INITIAL_DELAY_SECONDS + "s of initial delay), url: " + url);

    final String arrivalType = args[3];

    final AtomicLong successes = new AtomicLong(0);
    final AtomicLong failures = new AtomicLong(0);

    final AsyncHttpClient client = asyncHttpClient(config());

    final Metrowka metrowka = new Metrowka(60*1000);
    final Harvester loggingReaper = new LoggingReaper(LoggerFactory.getLogger("metrowka"), new LogEventHistogramSerializer());

    final Hiccup hiccups = new Hiccup();
    hiccups.start();

    final Interval eventsGeneratorJitter = new Interval("eventsGeneratorJitter", 1, TimeUnit.MINUTES.toNanos(1), 3);
    final EventsArrival arrivalDistribution = EventsArrival.fromName(arrivalType, rps, TimeUnit.SECONDS);

    final Interval latency = new Interval("latency", 1, TimeUnit.MINUTES.toNanos(1), 3);

    final BlockingQueue<Long> queue = new LinkedBlockingDeque<Long>(100000);
    final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

    EventsGenerator generator = new EventsGenerator(arrivalDistribution, queue, executor,
        event -> {
          eventsGeneratorJitter.record(Math.abs(event.getActualNanoTimestamp() - event.getExpectedNanoTimestamp()));

          final AsyncCompletionHandler<Response> responseHandler = new AsyncCompletionHandler<Response>() {

            @Override
            public Response onCompleted(Response response) throws Exception {
              if (response.getStatusCode() == 200) {
                successes.incrementAndGet();
                latency.record(System.nanoTime() - event.getActualNanoTimestamp());
              } else {
                failures.incrementAndGet();
              }
              return response;
            }

            @Override
            public void onThrowable(Throwable t) {
              failures.incrementAndGet();
              super.onThrowable(t);
            }
          };

          client.prepareGet(url).execute(responseHandler);
        });

    generator.start(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS);

    metrowka.register(hiccups, loggingReaper);
    metrowka.register(eventsGeneratorJitter, loggingReaper);
    metrowka.register(latency, loggingReaper);
    metrowka.start();

    TimeUnit.SECONDS.sleep(durationSeconds + INITIAL_DELAY_SECONDS);

    _logger.info("Finished, successful: " + successes.get() + ", failed: " + failures.get());

    generator.stop();
    metrowka.stop();
    hiccups.stop();
    executor.shutdown();
    client.close();

  }

}
