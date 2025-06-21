package it.mathsanalysis.load.resilience.event.impl;

import it.mathsanalysis.load.resilience.event.structure.DataLoaderEvent;

import java.util.Map;

public class BeforeSaveEvent<T> extends DataLoaderEvent<T> {

    public BeforeSaveEvent(T item, Map<String, Object> context) {
        super(item, "BEFORE_SAVE", context);
    }

    public static <T> BeforeSaveEvent<T> of(T item) {
        return new BeforeSaveEvent<>(item, Map.of());
    }

    public static <T> BeforeSaveEvent<T> of(T item, Map<String, Object> context) {
        return new BeforeSaveEvent<>(item, context);
    }
}
