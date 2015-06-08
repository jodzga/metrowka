package com.linkedin.metrowka;

import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Metrowka {

  private static final Logger _logger = LoggerFactory.getLogger(Metrowka.class);

  private static final ThreadFactory _threadFactory = new ThreadFactory() {

    private final AtomicInteger _poolNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "reaper-" + _poolNumber.getAndIncrement());
    }
  };

  private final AtomicBoolean _started = new AtomicBoolean(false);
  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private final long _harvestPeriodMs;
  private final ScheduledExecutorService _scheduler =
      Executors.newSingleThreadScheduledExecutor(_threadFactory);
  private final ConcurrentHashMap<Harvestable, Harvester> _harvesters = new ConcurrentHashMap<>();

  public Metrowka(long harvestPeriodMs) {
    _harvestPeriodMs = harvestPeriodMs;
  }

  public Harvester register(Harvestable harvestable, Harvester harvester) {
    return _harvesters.put(harvestable, harvester);
  }

  public void start() {
    if (_started.compareAndSet(false, true)) {
      long currentMs = System.currentTimeMillis();
      long msUntilNextPeriod = _harvestPeriodMs - currentMs % _harvestPeriodMs;
      _logger.info("scheduled reaper to start at " + new Date(currentMs + msUntilNextPeriod));
      _scheduler.scheduleAtFixedRate(this::harvest, msUntilNextPeriod, _harvestPeriodMs, TimeUnit.MILLISECONDS);
    } else {
      _logger.warn("reaper already started");
    }
  }

  public void stop() {
    if (!_started.get()) {
      _logger.warn("asked to stop reaper that has not been started");
    } else {
      if (stopped.compareAndSet(false, true)) {
        _scheduler.shutdownNow();
      } else {
        _logger.warn("reaper already stopped");
      }
    }
  }

  private void harvest() {
    for (Entry<Harvestable, Harvester> entry: _harvesters.entrySet()) {
      try {
        entry.getKey().harvest(entry.getValue());
      } catch (Exception e) {
        _logger.error("failed to harvest histogram", e);
      }
    }
  }

}
