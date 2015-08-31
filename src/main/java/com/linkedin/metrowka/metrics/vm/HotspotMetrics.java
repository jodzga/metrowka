package com.linkedin.metrowka.metrics.vm;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.linkedin.metrowka.Harvester;
import com.linkedin.metrowka.MeasureUnit;
import com.linkedin.metrowka.Metrowka;
import com.linkedin.metrowka.Rate;
import com.sun.management.OperatingSystemMXBean;
import com.sun.management.ThreadMXBean;

public class HotspotMetrics {

  private final Metrowka _metrowka;
  private final Harvester _harvester;

  private final ThreadMXBean _threadsMBean = ((ThreadMXBean)ManagementFactory.getThreadMXBean());
  private final OperatingSystemMXBean _osMBean = ((OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean());
  private final int _availableProcessors = _osMBean.getAvailableProcessors();

  private final Map<String, Rate> _rates = new HashMap<>();
  private long _lastTimestamp = 0;

  private final List<String> _threadGroupNames;

  private Map<Long, PerfStat> _threadPerfStats = new HashMap<>();
  private long _lastProcessCpu = 0;
  private final ScheduledExecutorService _scheduler = Executors.newSingleThreadScheduledExecutor();

  public HotspotMetrics(List<String> threadGroupNames, Metrowka metrowka, Harvester harvester) {
    _threadGroupNames = Collections.unmodifiableList(threadGroupNames);
    _metrowka = metrowka;
    _harvester = harvester;
  }

  public void start() {
    _scheduler.scheduleWithFixedDelay(this::update, 100, 100, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    _scheduler.shutdownNow();
  }

  private void update() {

    final long[] ids = _threadsMBean.getAllThreadIds();
    final ThreadInfo[] tis = _threadsMBean.getThreadInfo(ids);
    final long[] cpu = _threadsMBean.getThreadCpuTime(ids);
    final long[] usr = _threadsMBean.getThreadUserTime(ids);
    final long[] alloc = _threadsMBean.getThreadAllocatedBytes(ids);
    final long processCpu = _osMBean.getProcessCpuTime();
    final long timestamp = System.nanoTime();

    final Map<Long, PerfStat> currentPerfStats = new HashMap<>();
    final Map<String, PerfStatBuilder> threadGroupBuilders = new HashMap<String, PerfStatBuilder>();
    final PerfStatBuilder appStats = new PerfStatBuilder();

    for (int i= 0; i < ids.length; i++) {
      final long id = ids[i];
      final PerfStat lastPerfStats = _threadPerfStats.getOrDefault(id, new PerfStat(0, 0, 0, 0));
      final PerfStat perfStats = new PerfStat(cpu[i], usr[i], cpu[i] - usr[i], alloc[i]);

      appStats.add(perfStats, lastPerfStats);

      final String threadGroup = threadGroup(tis[i].getThreadName());
      if (threadGroup != null) {
        PerfStatBuilder builder = threadGroupBuilders.get(threadGroup);
        if (builder == null) {
          builder = new PerfStatBuilder();
          threadGroupBuilders.put(threadGroup, builder);
        }
        builder.add(perfStats, lastPerfStats);
      }

      currentPerfStats.put(id, perfStats);
    }
    _threadPerfStats = currentPerfStats;

    updateRate("ProcessCPU", processCpu - _lastProcessCpu, timestamp, MeasureUnit.time);
    _lastProcessCpu = processCpu;

    updateNamedRate("Application", appStats.build(), timestamp);
    threadGroupBuilders.forEach((name, builder) -> updateNamedRate(name, builder.build(), timestamp));
  }

  private void updateNamedRate(String name, PerfStat stat, long timestamp) {
    updateRate(name + "CPU", stat.getCpu(), timestamp, MeasureUnit.time);
    updateRate(name + "User", stat.getUser(), timestamp, MeasureUnit.time);
    updateRate(name + "Sys", stat.getSys(), timestamp, MeasureUnit.time);
    updateRate(name + "Alloc", stat.getAlloc(), timestamp, MeasureUnit.memory);
  }

  private void updateRate(String name, long count, long timestamp, MeasureUnit unit) {
    Rate rate = _rates.get(name);
    if (rate == null) {
      rate = new Rate(name, 1, Long.MAX_VALUE, 3, unit);
      _metrowka.register(rate, _harvester);
      _rates.put(name, rate);
    }
    rate.record(count, timestamp);
  }

  private String threadGroup(String threadName) {
    for (String tg : _threadGroupNames) {
      if (threadName.startsWith(tg)) {
        return tg;
      }
    }
    return null;
  }

  private static class PerfStatBuilder {
    private long cpu = 0;
    private long user = 0;
    private long alloc = 0;

    public void add(PerfStat current, PerfStat last) {
      cpu += (current.getCpu() - last.getCpu());
      user += (current.getUser() - last.getUser());
      alloc += (current.getAlloc() - last.getAlloc());
    }

    public PerfStat build() {
      return new PerfStat(cpu, user, cpu - user, alloc);
    }
  }

  private static class PerfStat {
    private final long _cpu;
    private final long _user;
    private final long _sys;
    private final long _alloc;

    public PerfStat(long cpu, long user, long sys, long alloc) {
      _cpu = cpu;
      _user = user;
      _sys = sys;
      _alloc = alloc;
    }

    public long getCpu() {
      return _cpu;
    }

    public long getUser() {
      return _user;
    }

    public long getSys() {
      return _sys;
    }

    public long getAlloc() {
      return _alloc;
    }
  }

}
