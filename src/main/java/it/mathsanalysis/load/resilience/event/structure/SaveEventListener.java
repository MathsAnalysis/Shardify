package it.mathsanalysis.load.resilience.event.structure;

import it.mathsanalysis.load.resilience.event.impl.AfterSaveEvent;
import it.mathsanalysis.load.resilience.event.impl.BeforeSaveEvent;

/**
 * Base listener per operazioni di save
 */
public abstract class SaveEventListener implements DataLoaderEventListener {

    @Override
    public final <T> void handleEvent(DataLoaderEvent<T> event) {
        switch (event.getOperationType()) {
            case "BEFORE_SAVE" -> handleBeforeSave((BeforeSaveEvent<T>) event);
            case "AFTER_SAVE" -> handleAfterSave((AfterSaveEvent<T>) event);
        }
    }

    @Override
    public <T> boolean supports(DataLoaderEvent<T> event) {
        return event instanceof BeforeSaveEvent || event instanceof AfterSaveEvent;
    }

    protected abstract <T> void handleBeforeSave(BeforeSaveEvent<T> event);

    protected abstract <T> void handleAfterSave(AfterSaveEvent<T> event);
}
