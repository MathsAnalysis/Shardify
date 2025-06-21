package it.mathsanalysis.load.resilience.event.impl;

import it.mathsanalysis.load.resilience.event.structure.DataLoaderEvent;
import lombok.Getter;

import java.util.Map;

@Getter
public class ErrorEvent<T> extends DataLoaderEvent<T> {

    private final Exception error;

    public ErrorEvent(T item, Exception error, Map<String, Object> context) {
        super(item, "ERROR", context);
        this.error = error;
    }

}
