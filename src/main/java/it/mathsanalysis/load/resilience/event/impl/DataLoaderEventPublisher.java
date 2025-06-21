package it.mathsanalysis.load.resilience.event.impl;

import it.mathsanalysis.load.resilience.event.structure.DataLoaderEventListener;
import it.mathsanalysis.load.resilience.event.structure.DataLoaderEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Event publisher for data loader events.
 * Support use synchronous or asynchronous event handling
 */
public class DataLoaderEventPublisher {

    private final List<DataLoaderEventListener> listeners = new CopyOnWriteArrayList<>();
    private final Executor asyncExecutor;
    private final boolean asyncByDefault;

    public DataLoaderEventPublisher() {
        this(ForkJoinPool.commonPool(), false);
    }

    public DataLoaderEventPublisher(Executor asyncExecutor, boolean asyncByDefault) {
        this.asyncExecutor = asyncExecutor;
        this.asyncByDefault = asyncByDefault;
    }

    public void addListener(DataLoaderEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DataLoaderEventListener listener) {
        listeners.remove(listener);
    }

    public <T> void publishBeforeSave(BeforeSaveEvent<T> event) {
        publishEvent(event);
    }

    public <T> void publishAfterSave(AfterSaveEvent<T> event) {
        publishEvent(event);
    }

    public <T> void publishBeforeDelete(BeforeDeleteEvent<T> event) {
        publishEvent(event);
    }

    public <T> void publishAfterDelete(AfterDeleteEvent<T> event) {
        publishEvent(event);
    }

    public <T> void publishError(ErrorEvent<T> event) {
        publishEvent(event);
    }

    @SuppressWarnings("unchecked")
    private <T> void publishEvent(DataLoaderEvent<T> event) {
        for (DataLoaderEventListener listener : listeners) {
            if (listener.supports(event)) {
                if (asyncByDefault || listener.isAsync()) {
                    asyncExecutor.execute(() -> safeHandleEvent(listener, event));
                } else {
                    safeHandleEvent(listener, event);
                }
            }
        }
    }

    private <T> void safeHandleEvent(DataLoaderEventListener listener, DataLoaderEvent<T> event) {
        try {
            listener.handleEvent(event);
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Error in event listener: " + e.getMessage());
        }
    }
}
