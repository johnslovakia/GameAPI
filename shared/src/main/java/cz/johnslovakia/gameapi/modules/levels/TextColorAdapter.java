package cz.johnslovakia.gameapi.modules.levels;

import com.google.gson.*;
import net.kyori.adventure.text.format.TextColor;

import java.lang.reflect.Type;

public class TextColorAdapter implements JsonSerializer<TextColor>, JsonDeserializer<TextColor> {

    @Override
    public JsonElement serialize(TextColor src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(String.format("#%06X", src.value() & 0xFFFFFF));
    }

    @Override
    public TextColor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String hex = json.getAsString();
        return TextColor.fromHexString(hex);
    }
}