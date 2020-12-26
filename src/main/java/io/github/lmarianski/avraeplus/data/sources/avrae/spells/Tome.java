package io.github.lmarianski.avraeplus.data.sources.avrae.spells;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.lmarianski.avraeplus.Main;
import io.github.lmarianski.avraeplus.data.SpellSchool;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpell;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpellCollection;
import io.github.lmarianski.avraeplus.data.sources.avrae.AvraeClient;
import org.bson.*;
import org.bson.codecs.*;

import java.lang.reflect.Type;
import java.util.*;

public class Tome implements ISpellCollection {

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
    public AvraeSpell[] spells;

    @Override
    public String toString() {
        return name;
    }

    public static Tome fromJSON(String json) {
        try {
            return Main.gson.fromJson(json, Tome.class);
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ISpellCollection && ((ISpellCollection) o).getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getImage() {
        return image;
    }

    @Override
    public AvraeSpell[] getSpells() {
        return this.spells;
    }

    @Override
    public Map<String, List<String>> getSpellLists() {
        return null;
    }

    public static class TomeSubscriber {
        public String username;
        public String id;
    }

    public static class AvraeSpell implements ISpell {
        public String name;
        public int level;
        public SpellSchool school;
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

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public SpellSchool getSchool() {
            return school;
        }

        @Override
        public String[] getClasses() {
            return getClassString().split(", ");
        }

        @Override
        public String getClassString() {
            String classes = this.classes;
            if (classes.endsWith(",")) {
                classes = classes.substring(0, classes.length() - 1);
            }
            return classes.trim();
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public boolean isRitual() {
            return ritual;
        }

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
}
