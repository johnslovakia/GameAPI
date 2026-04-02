package cz.johnslovakia.gameapi.modules.serverManagement.gameData;

public interface UpdatedValueInterface<T> {
    String getWhat(); // "String", "Integer", "Double", "Boolean"

    default String getStringValue(T context) { return null; }
    default Integer getIntegerValue(T context) { return null; }
    default Double getDoubleValue(T context) { return null; }
    default Boolean getBooleanValue(T context) { return null; }
}