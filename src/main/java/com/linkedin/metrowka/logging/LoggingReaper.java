package com.linkedin.metrowka.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.HdrHistogram.Histogram;
import org.slf4j.Logger;

import com.linkedin.metrowka.Harvester;
import com.linkedin.metrowka.InstrumentType;

public class LoggingReaper implements Harvester {

  private final HistogramEventSerializer _serializer;
  private final Map<String, Histogram> _histograms = new HashMap<>();
  private final Map<String, Histogram> _histogramsWarmedUp = new HashMap<>();
  private final Map<String, AtomicLong> _skipCounters = new HashMap<>();

  private final Logger _logger;

  public LoggingReaper(Logger logger, HistogramEventSerializer serializer) {
    _logger = logger;
    _serializer = serializer;
  }

  @Override
  public void harvest(Histogram histogram, InstrumentType type, String name) {
	synchronized(_histograms) {
		if (_histograms.containsKey(name)) {
			_histograms.get(name).add(histogram);
		} else {
			_histograms.put(name, histogram.copy());
		}
	}
	synchronized(_histogramsWarmedUp) {
		AtomicLong counter = _skipCounters.computeIfAbsent(name, k -> new AtomicLong());
		long cnt = counter.incrementAndGet();
		if (cnt > 5) {
			if (_histogramsWarmedUp.containsKey(name)) {
				_histogramsWarmedUp.get(name).add(histogram);
			} else {
				_histogramsWarmedUp.put(name, histogram.copy());
			}
		}
	}
	
    _logger.info(_serializer.serialize(histogram, type, name));
  }
  
  public void consumeTotals(BiConsumer<String, Histogram> consumer) {
	  synchronized(_histograms) {
		  _histograms.forEach(consumer);
	  }
  }

  public void consumeWarmedUpTotals(BiConsumer<String, Histogram> consumer) {
	  synchronized(_histogramsWarmedUp) {
		  _histogramsWarmedUp.forEach(consumer);
	  }
  }
  
}
