package io.github.lmarianski.avraeplus.avrae.homebrew.spells;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.lmarianski.avraeplus.Main;
import io.github.lmarianski.avraeplus.avrae.AvraeClient;
import org.bson.*;
import org.bson.codecs.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Tome {

    @SerializedName("_id")
    public transient String id;
    public String name;
//    @SerializedName("public")
//    public boolean isPublic;
//    public String desc;
    public String image;
//    public String owner;
    //public String[] editors;
//    public TomeSubscriber[] subscribers;
//    public String[] active;
//    @SerializedName("server_active")
//    public String[] serverActive;
    public Spell[] spells;
    @SerializedName("spell_lists")
    @JsonAdapter(SpellListMapSerializer.class)
    public HashMap<String, ArrayList<String>> spellLists;

    @Override
    public String toString() {
        return name;
    }

    public static Tome fromJSON(String json) {
        return Main.gson.fromJson(json, Tome.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tome tome = (Tome) o;
        return id.equals(tome.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class TomeSubscriber {
        public String username;
        public String id;
    }

    public static class Spell {
        public String name;
        public int level;
        public School school;
        public String classes;
        public String subclasses;
        public String casttime;
        public String range;
        public SpellComponents components;
        public String duration;
        public boolean ritual;
        public String description;

        @SerializedName("higherlevels")
        public String higherLevels;
        public boolean concentration;

        public static class SpellComponents {
            public boolean verbal;
            public boolean somatic;
            public String material;
        }
    }

    public static class TomeCodec implements CollectibleCodec<Tome> {
        @Override
        public Tome generateIdIfAbsentFromDocument(Tome document) {
            return document;
        }

        @Override
        public boolean documentHasId(Tome document) {
            return document.id != null;
        }

        @Override
        public BsonValue getDocumentId(Tome document) {
            return new BsonString(document.id);
        }

        @Override
        public Tome decode(BsonReader reader, DecoderContext decoderContext) {
            return AvraeClient.getTome(reader.readString());
        }

        @Override
        public void encode(BsonWriter writer, Tome value, EncoderContext encoderContext) {
            writer.writeString(value.id);
        }

        @Override
        public Class<Tome> getEncoderClass() {
            return Tome.class;
        }
    }

    public static class SpellListMapSerializer implements JsonSerializer<HashMap<String, ArrayList<String>>>, JsonDeserializer<HashMap<String, ArrayList<String>>> {
        @Override
        public HashMap<String, ArrayList<String>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Main.gson.fromJson(json, new TypeToken<HashMap<String, ArrayList<String>>>(){}.getType());
        }

        @Override
        public JsonElement serialize(HashMap<String, ArrayList<String>> src, Type typeOfSrc, JsonSerializationContext context) {
            return Main.gson.toJsonTree(src);
        }
    }

}
