package com.linkedin.metrowka;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.metrowka.logging.LogEventHistogramSerializer;
import com.linkedin.metrowka.logging.LoggingReaper;
import com.linkedin.metrowka.metrics.Hiccup;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

public class SimpleLoadGenerator {

  private static final Logger _logger = LoggerFactory.getLogger(SimpleLoadGenerator.class);

  public static void main(String[] args) throws Exception {

    final double rps = Double.parseDouble(args[0]);
    final long durationSeconds = Long.parseLong(args[1]);
    final String url = args[2];
        //"http://localhost:7479/restli-perf-pegasus-server/asyncLongKey/0?delay=0&fixture=small";

    _logger.info("RPS: " + rps + ", duration: " + durationSeconds + "s, url: " + url);

    final AtomicLong successes = new AtomicLong(0);
    final AtomicLong failures = new AtomicLong(0);

    final AsyncHttpClientConfig.Builder cfgBuilder = new AsyncHttpClientConfig.Builder();
    final AsyncHttpClient client = new AsyncHttpClient(cfgBuilder.build());

    final Metrowka metrowka = new Metrowka(60*1000);
    final Harvester loggingReaper = new LoggingReaper(LoggerFactory.getLogger("metrowka"), new LogEventHistogramSerializer());

    final Hiccup hiccups = new Hiccup();
    hiccups.start();

    final Interval eventsGeneratorJitter = new Interval("eventsGeneratorJitter", 1, TimeUnit.MINUTES.toNanos(1), 3);
    final EventsArrival arrivalDistribution = new PoissonEventsArrival(rps, TimeUnit.SECONDS);

    final Interval latency = new Interval("latency", 1, TimeUnit.MINUTES.toNanos(1), 3);

    final BlockingQueue<Long> queue = new LinkedBlockingDeque<Long>(100000);
    final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

    generator.start(5, TimeUnit.SECONDS);

    metrowka.register(hiccups, loggingReaper);
    metrowka.register(eventsGeneratorJitter, loggingReaper);
    metrowka.register(latency, loggingReaper);
    metrowka.start();

    TimeUnit.SECONDS.sleep(durationSeconds);

    _logger.info("Finished, successful: " + successes.get() + ", failed: " + failures.get());

    generator.stop();
    metrowka.stop();
    hiccups.stop();
    executor.shutdown();
    client.close();

  }

}
