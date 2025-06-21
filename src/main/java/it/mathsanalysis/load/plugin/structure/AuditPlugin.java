package it.mathsanalysis.load.plugin.structure;

import java.util.Map;
import java.util.Set;


public abstract class AuditPlugin implements DataLoaderPlugin {

    @Override
    public Set<String> getSupportedDatabases() {
        return Set.of("*");
    }

    @Override
    public <T> void afterSave(T item, T result, Map<String, Object> context) {
        auditSave(item, result, context);
    }

    @Override
    public <T> void afterDelete(T item, Map<String, Object> context) {
        auditDelete(item, context);
    }

    protected abstract <T> void auditSave(T item, T result, Map<String, Object> context);

    protected abstract <T> void auditDelete(T item, Map<String, Object> context);
}
