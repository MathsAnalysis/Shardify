package it.mathsanalysis.load.resilience.event.impl;

import it.mathsanalysis.load.resilience.event.structure.DataLoaderEvent;

import java.util.Map;

public class BeforeDeleteEvent<T> extends DataLoaderEvent<T> {
    public BeforeDeleteEvent(T item, Map<String, Object> context) {
        super(item, "BEFORE_DELETE", context);
    }
}
