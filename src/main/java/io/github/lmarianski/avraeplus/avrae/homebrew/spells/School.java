package io.github.lmarianski.avraeplus.avrae.homebrew.spells;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Locale;

@JsonAdapter(School.Serializer.class)
public enum School {

    ABJURATION("A"),
    CONJURATION("C"),
    DIVINATION("D"),
    ENCHANTMENT("E"),
    EVOCATION("V"),
    ILLUSION("I"),
    NECROMANCY("N"),
    TRANSMUTATION("T");

    public String id;

    School(String id) {
        this.id = id;
    }

    public static School get(String string) {
        try {
            return valueOf(string.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    public static School getByLetter(String letter) {
        for (School s : values())
            if (letter.equalsIgnoreCase(s.id)) return s;
        return null;
    }


    public static class Serializer implements JsonSerializer<School>, JsonDeserializer<School> {
        @Override
        public School deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return School.getByLetter(json.getAsString());
        }

        @Override
        public JsonElement serialize(School src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.name().substring(0, 1));
        }
    }
}
