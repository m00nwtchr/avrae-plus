package io.github.lmarianski.avraeplus.avrae.homebrew.spells;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.github.lmarianski.avraeplus.Main;

import java.io.IOException;

public class Tome {

    @SerializedName("_id")
    public transient String id;
    public String name;
    @SerializedName("public")
    public boolean isPublic;
    public String desc;
    public String image;
    public String owner;
    //public String[] editors;
    public TomeSubscriber[] subscribers;
    public String[] active;
    @SerializedName("server_active")
    public String[] serverActive;
    public Spell[] spells;

    @Override
    public String toString() {
        return name;
    }

    public static Tome fromJSON(String json) {
        return Main.gson.fromJson(json, Tome.class);
    }

    public static class TomeSubscriber {
        public String username;
        public String id;
    }

    public static class Spell {
        public String name;
        public int level;
        //@JsonAdapter(SchoolJsonAdapter.class)
        public String school;
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

    public static class SchoolJsonAdapter extends TypeAdapter<String> {

        @Override
        public void write(JsonWriter out, String value) throws IOException {

        }

        @Override
        public String read(JsonReader in) throws IOException {
            return null;
        }
    }
}