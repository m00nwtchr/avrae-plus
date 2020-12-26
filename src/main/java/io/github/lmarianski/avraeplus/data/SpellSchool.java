package io.github.lmarianski.avraeplus.data;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Locale;

@JsonAdapter(SpellSchool.Serializer.class)
public enum SpellSchool {

    ABJURATION("A"),
    CONJURATION("C"),
    DIVINATION("D"),
    ENCHANTMENT("E"),
    EVOCATION("V"),
    ILLUSION("I"),
    NECROMANCY("N"),
    TRANSMUTATION("T");

    public String id;

    SpellSchool(String id) {
        this.id = id;
    }

    public static SpellSchool get(String string) {
        try {
            return valueOf(string.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    public static SpellSchool getByLetter(String letter) {
        for (SpellSchool s : values())
            if (letter.equalsIgnoreCase(s.id)) return s;
        return null;
    }


    public static class Serializer implements JsonSerializer<SpellSchool>, JsonDeserializer<SpellSchool> {
        @Override
        public SpellSchool deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return SpellSchool.getByLetter(json.getAsString());
        }

        @Override
        public JsonElement serialize(SpellSchool src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.name().substring(0, 1));
        }
    }
}
