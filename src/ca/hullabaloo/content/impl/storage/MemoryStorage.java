package ca.hullabaloo.content.impl.storage;

import ca.hullabaloo.content.api.StorageSpi;
import com.google.common.base.Preconditions;
import com.google.common.collect.Interner;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryStorage extends BaseStorage {
  private volatile int maxReads = Integer.MAX_VALUE;
  private final AtomicInteger reads = new AtomicInteger(0);

  private final Queue<byte[]> data = new ConcurrentLinkedQueue<byte[]>();
  private final StorageTypes types = new StorageTypes();
  private final StorageSpi spi = new StorageSpi() {
    @Override
    public Interner<String> properties(Class<?> type) {
      return types.properties(type);
    }

    @Override
    public Iterator<byte[]> data() {
      Preconditions.checkState(reads.getAndIncrement() < maxReads, "only allowed to read %s times", maxReads);
      return data.iterator();
    }

    @Override
    public int[] ids(Class<?> type) {
      return types.ids(type);
    }
  };
  private final EventBus events;

  public MemoryStorage() {
    this.events = new EventBus();
    this.events.register(this);
  }

  @Subscribe
  public void updates(UpdateBatch updates) {
    this.data.add(updates.bytes());
  }

  public MemoryStorage maxReads(int max) {
    this.maxReads = max;
    return this;
  }

  @Override
  protected EventBus eventBus() {
    return this.events;
  }

  @Override
  protected StorageSpi spi() {
    return this.spi;
  }

  @Override
  protected StorageTypes storageTypes() {
    return this.types;
  }
}
