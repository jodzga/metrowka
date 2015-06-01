package com.linkedin.metrowka;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MonitoredBlockingQueue<T> implements BlockingQueue<T> {

  private final Gauge _queueSize;
  private final HarvestableQueueSize _harvestableQueueSize;
  private final Throughput _enqueueRate;
  private final Throughput _dequeueRate;
  private final BlockingQueue<T> _delegate;

  public MonitoredBlockingQueue(final String name, final BlockingQueue<T> delegate) {
    _delegate = delegate;
    _queueSize = new Gauge(name, 1, Integer.MAX_VALUE, 3);
    _harvestableQueueSize = new HarvestableQueueSize(name);
    _enqueueRate = new Throughput(name, 1, Long.MAX_VALUE, 3);
    _dequeueRate = new Throughput(name, 1, Long.MAX_VALUE, 3);
  }

  public void forEach(Consumer<? super T> action) {
    _delegate.forEach(action);
  }

  public boolean add(T e) {
    boolean r = _delegate.add(e);
    long currentNano = System.nanoTime();
    _queueSize.update(_delegate.size(), currentNano);
    _enqueueRate.record(1, currentNano);
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
    long currentNano = System.nanoTime();
    _queueSize.update(_delegate.size(), currentNano);
    _dequeueRate.record(1, currentNano);
    return r;
  }

  public boolean offer(T e) {
    boolean r = _delegate.offer(e);
    if (r) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
      _enqueueRate.record(1, currentNano);
    }
    return r;
  }

  public T poll() {
    T r = _delegate.poll();
    if (r != null) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
      _dequeueRate.record(1, currentNano);
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
    long currentNano = System.nanoTime();
    _queueSize.update(_delegate.size(), currentNano);
    _enqueueRate.record(1, currentNano);
  }

  public Object[] toArray() {
    return _delegate.toArray();
  }

  public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {
    boolean r = _delegate.offer(e, timeout, unit);
    if (r) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
      _enqueueRate.record(1, currentNano);
    }
    return r;
  }

  @SuppressWarnings("hiding")
  public <T> T[] toArray(T[] a) {
    return _delegate.toArray(a);
  }

  public T take() throws InterruptedException {
    T r = _delegate.take();
    long currentNano = System.nanoTime();
    _queueSize.update(_delegate.size(), currentNano);
    _dequeueRate.record(1, currentNano);
    return r;
  }

  public T poll(long timeout, TimeUnit unit) throws InterruptedException {
    T r = _delegate.poll(timeout, unit);
    if (r != null) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
      _dequeueRate.record(1, currentNano);
    }
    return r;
  }

  public int remainingCapacity() {
    return _delegate.remainingCapacity();
  }

  public boolean remove(Object o) {
    boolean r = _delegate.remove(o);
    if (r) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
      _dequeueRate.record(1, currentNano);
    }
    return r;
  }

  public boolean contains(Object o) {
    return _delegate.contains(o);
  }

  public int drainTo(Collection<? super T> c) {
    int r = _delegate.drainTo(c);
    if (r > 0) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
      _dequeueRate.record(r, currentNano);
    }
    return r;
  }

  public int drainTo(Collection<? super T> c, int maxElements) {
    int r = _delegate.drainTo(c, maxElements);
    if (r > 0) {
      long currentNano = System.nanoTime();
      _queueSize.update(_delegate.size(), currentNano);
      _dequeueRate.record(r, currentNano);
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
    if (r) {
      long currentNano = System.nanoTime();
      int size = _delegate.size();
      int delta = beforeSize - size;
      if (delta > 0) {
        _queueSize.update(size, currentNano);
        _dequeueRate.record(delta, currentNano);
      }
    }
    return r;
  }

  public boolean removeIf(Predicate<? super T> filter) {
    int beforeSize = _delegate.size();
    boolean r = _delegate.removeIf(filter);
    if (r) {
      long currentNano = System.nanoTime();
      int size = _delegate.size();
      int delta = beforeSize - size;
      if (delta > 0) {
        _queueSize.update(size, currentNano);
        _dequeueRate.record(delta, currentNano);
      }
    }
    return r;
  }

  public boolean retainAll(Collection<?> c) {
    int beforeSize = _delegate.size();
    boolean r = _delegate.retainAll(c);
    if (r) {
      long currentNano = System.nanoTime();
      int size = _delegate.size();
      int delta = beforeSize - size;
      if (delta > 0) {
        _queueSize.update(size, currentNano);
        _dequeueRate.record(delta, currentNano);
      }
    }
    return r;
  }

  public void clear() {
    int beforeSize = _delegate.size();
    _delegate.clear();
    long currentNano = System.nanoTime();
    _queueSize.update(0, currentNano);
    _dequeueRate.record(beforeSize, currentNano);
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
      super(InstrumentType.gauge, name);
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

  public Harvestable dequeueRate() {
    return _dequeueRate;
  }

}
