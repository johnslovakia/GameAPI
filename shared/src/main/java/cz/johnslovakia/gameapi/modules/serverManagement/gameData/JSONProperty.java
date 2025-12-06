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
        switch (type.toLowerCase()) {
            case "string":
                return valueInterface.getStringValue(context);
            case "integer":
                return valueInterface.getIntegerValue(context);
            case "double":
                return valueInterface.getDoubleValue(context);
            case "boolean":
                return valueInterface.getBooleanValue(context);
            default:
                return null;
        }
    }

    public UpdatedValueInterface<T> getUpdatedValueInterface() {
        return valueInterface;
    }
}