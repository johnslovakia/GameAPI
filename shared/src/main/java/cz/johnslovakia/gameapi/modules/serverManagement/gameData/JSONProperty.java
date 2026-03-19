package cz.johnslovakia.gameapi.modules.serverManagement.gameData;

import lombok.Getter;

public class JSONProperty<T> {

    @Getter
    private final String property;
    private final UpdatedValueInterface<T> valueInterface;

    public JSONProperty(String property, UpdatedValueInterface<T> valueInterface) {
        this.property = property;
        this.valueInterface = valueInterface;
    }

    public Object getValue(T context) {
        String type = valueInterface.getWhat();
        return switch (type.toLowerCase()) {
            case "string" -> valueInterface.getStringValue(context);
            case "integer" -> valueInterface.getIntegerValue(context);
            case "double" -> valueInterface.getDoubleValue(context);
            case "boolean" -> valueInterface.getBooleanValue(context);
            default -> null;
        };
    }

    public UpdatedValueInterface<T> getUpdatedValueInterface() {
        return valueInterface;
    }
}