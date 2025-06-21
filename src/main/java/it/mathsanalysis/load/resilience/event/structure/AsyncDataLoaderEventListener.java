package it.mathsanalysis.load.resilience.event.structure;

/**
 * Async listener base
 */
public abstract class AsyncDataLoaderEventListener implements DataLoaderEventListener {

    @Override
    public final boolean isAsync() {
        return true;
    }
}
