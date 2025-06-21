package it.mathsanalysis.load.resilience.event.structure;

/**
 * Interface per event listeners del DataLoader
 */
public interface DataLoaderEventListener {

    /**
     * Handle dell'evento
     */
    <T> void handleEvent(DataLoaderEvent<T> event);

    /**
     * Controlla se questo listener supporta il tipo di evento
     */
    default <T> boolean supports(DataLoaderEvent<T> event) {
        return true;
    }

    /**
     * Se questo listener dovrebbe essere eseguito async
     */
    default boolean isAsync() {
        return false;
    }
}
