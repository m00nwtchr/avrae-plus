package io.github.lmarianski.avraeplus.data.sources.json;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.lmarianski.avraeplus.Main;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpell;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpellCollection;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JsonTome implements ISpellCollection {

    public String url;

//    public ISpell[] spells;
    @SerializedName("spell_lists")
    @JsonAdapter(SpellListMapSerializer.class)
    public Map<String, List<String>> spellLists;

    @Override
    public String getId() {
        return url;
    }

    @Override
    public String getName() {
        String[] strs = getId().split("/");
        return strs[strs.length-1];
    }

    @Override
    public String getImage() {
        return null;
    }

    @Override
    public ISpell[] getSpells() {
        return new ISpell[0];
    }

    @Override
    public Map<String, List<String>> getSpellLists() {
        return spellLists;
    }

    public static class SpellListMapSerializer implements JsonSerializer<Map<String, List<String>>>, JsonDeserializer<Map<String, List<String>>> {
        @Override
        public Map<String, List<String>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Main.gson.fromJson(json, new TypeToken<Map<String, List<String>>>(){}.getType());
        }

        @Override
        public JsonElement serialize(Map<String, List<String>> src, Type typeOfSrc, JsonSerializationContext context) {
            return Main.gson.toJsonTree(src);
        }
    }

}
