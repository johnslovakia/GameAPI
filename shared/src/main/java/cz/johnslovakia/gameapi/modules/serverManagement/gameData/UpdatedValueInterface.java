package cz.johnslovakia.gameapi.modules.serverManagement.gameData;

public interface UpdatedValueInterface<T> {
    String getWhat(); // "String", "Integer", "Double", "Boolean"
    
    String getStringValue(T context);
    Integer getIntegerValue(T context);
    Double getDoubleValue(T context);
    Boolean getBooleanValue(T context);
}