package com.linkedin.metrowka.ds;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.linkedin.metrowka.Gauge;
import com.linkedin.metrowka.Harvestable;
import com.linkedin.metrowka.Harvester;
import com.linkedin.metrowka.InstrumentType;
import com.linkedin.metrowka.MeasureUnit;
import com.linkedin.metrowka.Rate;

public class MonitoredBlockingQueue<T> implements BlockingQueue<T> {

  private final Gauge _queueSize;
  private final HarvestableQueueSize _harvestableQueueSize;
  private final Rate _enqueueRate;
  private final BlockingQueue<T> _delegate;
  private final boolean _monitorEnabled;
  private final boolean _monitorSize;
  private final boolean _monitorEnqueueRate;

  public MonitoredBlockingQueue(final String name, final BlockingQueue<T> delegate,
      boolean monitorSize, boolean monitorEnqueueRate) {
    _delegate = delegate;
    _monitorSize = monitorSize;
    _monitorEnqueueRate = monitorEnqueueRate;
    _monitorEnabled = _monitorSize || _monitorEnqueueRate;
    if (_monitorSize) {
      _queueSize = new Gauge(name + "-queueSize", 1, Integer.MAX_VALUE, 3);
      _harvestableQueueSize = new HarvestableQueueSize(name + "-queueSize");
    } else {
      _queueSize = null;
      _harvestableQueueSize = null;
    }
    if (_monitorEnqueueRate) {
      _enqueueRate = new Rate(name + "-enqueueRate", 1, Long.MAX_VALUE, 3, MeasureUnit.other);
    } else {
      _enqueueRate = null;
    }
  }

  public void forEach(Consumer<? super T> action) {
    _delegate.forEach(action);
  }

  public boolean add(T e) {
    boolean r = _delegate.add(e);
    if (_monitorEnabled) {
      long currentNano = System.nanoTime();
      if (_monitorSize && r) {
        _queueSize.update(_delegate.size(), currentNano);
      }
      if (_monitorEnqueueRate) {
        _enqueueRate.record(1, currentNano);
      }
    }
    return r;
  }

  public int size() {
    return _delegate.size();
  }

  public boolean isEmpty() {
    return _delegate.isEmpty();
  }

  public T remove() {
    T r = _delegate.remove();
    if (_monitorSize) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
    }
    return r;
  }

  public boolean offer(T e) {
    boolean r = _delegate.offer(e);
    if (_monitorEnabled && r) {
      long currentNano = System.nanoTime();
      if (_monitorSize) {
        _queueSize.update(_delegate.size(), currentNano);
      }
      if (_monitorEnqueueRate) {
        _enqueueRate.record(1, currentNano);
      }
    }
    return r;
  }

  public T poll() {
    T r = _delegate.poll();
    if (_monitorSize && r != null) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
    }
    return r;
  }

  public T element() {
    return _delegate.element();
  }

  public Iterator<T> iterator() {
    return _delegate.iterator();
  }

  public T peek() {
    return _delegate.peek();
  }

  public void put(T e) throws InterruptedException {
    _delegate.put(e);
    if (_monitorEnabled) {
      long currentNano = System.nanoTime();
      if (_monitorSize) {
        _queueSize.update(_delegate.size(), currentNano);
      }
      if (_monitorEnqueueRate) {
        _enqueueRate.record(1, currentNano);
      }
    }
  }

  public Object[] toArray() {
    return _delegate.toArray();
  }

  public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {
    boolean r = _delegate.offer(e, timeout, unit);
    if (_monitorEnabled && r) {
      long currentNano = System.nanoTime();
      if (_monitorSize) {
        _queueSize.update(_delegate.size(), currentNano);
      }
      if (_monitorEnqueueRate) {
        _enqueueRate.record(1, currentNano);
      }
    }
    return r;
  }

  @SuppressWarnings("hiding")
  public <T> T[] toArray(T[] a) {
    return _delegate.toArray(a);
  }

  public T take() throws InterruptedException {
    T r = _delegate.take();
    if (_monitorSize) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
    }
    return r;
  }

  public T poll(long timeout, TimeUnit unit) throws InterruptedException {
    T r = _delegate.poll(timeout, unit);
    if (_monitorSize && r != null) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
    }
    return r;
  }

  public int remainingCapacity() {
    return _delegate.remainingCapacity();
  }

  public boolean remove(Object o) {
    boolean r = _delegate.remove(o);
    if (_monitorSize && r) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
    }
    return r;
  }

  public boolean contains(Object o) {
    return _delegate.contains(o);
  }

  public int drainTo(Collection<? super T> c) {
    int r = _delegate.drainTo(c);
    if (_monitorSize && r > 0) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
    }
    return r;
  }

  public int drainTo(Collection<? super T> c, int maxElements) {
    int r = _delegate.drainTo(c, maxElements);
    if (_monitorSize && r > 0) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
    }
    return r;
  }

  public boolean containsAll(Collection<?> c) {
    return _delegate.containsAll(c);
  }

  public boolean addAll(Collection<? extends T> c) {
    int beforeSize = _delegate.size();
    boolean r = _delegate.addAll(c);
    if (r) {
      long currentNano = System.nanoTime();
      int size = _delegate.size();
      int delta = size - beforeSize;
      if (delta > 0) {
        _queueSize.update(size, currentNano);
        _enqueueRate.record(delta, currentNano);
      }
    }
    return r;
  }

  public boolean removeAll(Collection<?> c) {
    int beforeSize = _delegate.size();
    boolean r = _delegate.removeAll(c);
    if (_monitorSize && r) {
      int size = _delegate.size();
      int delta = beforeSize - size;
      if (delta > 0) {
        long currentNano = System.nanoTime();
        _queueSize.update(size, currentNano);
      }
    }
    return r;
  }

  public boolean removeIf(Predicate<? super T> filter) {
    int beforeSize = _delegate.size();
    boolean r = _delegate.removeIf(filter);
    if (_monitorSize && r) {
      int size = _delegate.size();
      int delta = beforeSize - size;
      if (delta > 0) {
        long currentNano = System.nanoTime();
        _queueSize.update(size, currentNano);
      }
    }
    return r;
  }

  public boolean retainAll(Collection<?> c) {
    int beforeSize = _delegate.size();
    boolean r = _delegate.retainAll(c);
    if (_monitorSize && r) {
      int size = _delegate.size();
      int delta = beforeSize - size;
      if (delta > 0) {
        long currentNano = System.nanoTime();
        _queueSize.update(size, currentNano);
      }
    }
    return r;
  }

  public void clear() {
    _delegate.clear();
    if (_monitorSize) {
      long currentNano = System.nanoTime();
      _queueSize.update(0, currentNano);
    }
  }

  public boolean equals(Object o) {
    return _delegate.equals(o);
  }

  public int hashCode() {
    return _delegate.hashCode();
  }

  public Spliterator<T> spliterator() {
    return _delegate.spliterator();
  }

  public Stream<T> stream() {
    return _delegate.stream();
  }

  public Stream<T> parallelStream() {
    return _delegate.parallelStream();
  }

  private class HarvestableQueueSize extends Harvestable {

    HarvestableQueueSize(final String name) {
      super(InstrumentType.gauge, MeasureUnit.other, name);
    }

    @Override
    public void harvest(Harvester harvester) {
      _queueSize.update(_delegate.size(), System.nanoTime());
      _queueSize.harvest(harvester);
    }
  }

  public Harvestable queueSize() {
    return _harvestableQueueSize;
  }

  public Harvestable enqueueRate() {
    return _enqueueRate;
  }

}
