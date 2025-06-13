
package it.mathsanalysis.load.metadata;

/**
 * Tipo di database supportato
 */
public enum DatabaseType {
    SQL("sql"),
    DOCUMENT("document"),
    KEY_VALUE("key-value"),
    GRAPH("graph");
    
    private final String type;
    
    DatabaseType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public boolean isRelational() {
        return this == SQL;
    }
    
    public boolean isDocument() {
        return this == DOCUMENT;
    }
}