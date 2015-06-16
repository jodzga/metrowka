package com.linkedin.metrowka;

import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.linkedin.metrowka.logging.LogEventHistogramSerializer;
import com.linkedin.metrowka.logging.LoggingReaper;
import com.linkedin.metrowka.metrics.Hiccup;

public class HiccupMeter {

  public static void main(String[] args) throws InterruptedException {
    final Metrowka metrowka = new Metrowka(60*1000);
    final Harvester loggingReaper = new LoggingReaper(LoggerFactory.getLogger("metrowka"), new LogEventHistogramSerializer());

    final Hiccup jitter = new Hiccup();
    jitter.start();

    metrowka.register(jitter, loggingReaper);
    metrowka.start();

    TimeUnit.SECONDS.sleep(Long.MAX_VALUE);
  }

}
