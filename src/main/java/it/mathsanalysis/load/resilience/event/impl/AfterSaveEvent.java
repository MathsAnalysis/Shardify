package it.mathsanalysis.load.resilience.event.impl;

import it.mathsanalysis.load.resilience.event.structure.DataLoaderEvent;
import lombok.Getter;

import java.util.Map;

@Getter
public class AfterSaveEvent<T> extends DataLoaderEvent<T> {

    private final T originalItem;
    private final boolean isNewItem;

    public AfterSaveEvent(T originalItem, T savedItem, boolean isNewItem, Map<String, Object> context) {
        super(savedItem, "AFTER_SAVE", context);
        this.originalItem = originalItem;
        this.isNewItem = isNewItem;
    }

    public T getSavedItem() {
        return getItem();
    }

    public static <T> AfterSaveEvent<T> of(T originalItem, T savedItem, boolean isNewItem) {
        return new AfterSaveEvent<>(originalItem, savedItem, isNewItem, Map.of());
    }
}
