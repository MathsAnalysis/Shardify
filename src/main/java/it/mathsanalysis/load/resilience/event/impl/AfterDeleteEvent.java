package it.mathsanalysis.load.resilience.event.impl;

import it.mathsanalysis.load.resilience.event.structure.DataLoaderEvent;

import java.util.Map;

public final class AfterDeleteEvent<T> extends DataLoaderEvent<T> {
    public AfterDeleteEvent(T item, Map<String, Object> context) {
        super(item, "AFTER_DELETE", context);
    }
}
