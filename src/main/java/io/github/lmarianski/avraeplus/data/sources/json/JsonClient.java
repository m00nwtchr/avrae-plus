package io.github.lmarianski.avraeplus.data.sources.json;

import io.github.lmarianski.avraeplus.Main;
import io.github.lmarianski.avraeplus.Util;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpellCollection;

import java.io.IOException;
import java.util.Optional;

public class JsonClient {

    public static Optional<ISpellCollection> getTome(String source) {
        try {
            JsonTome tome =  Main.gson.fromJson(Util.GET(source), JsonTome.class);
            tome.url = source;
            return Optional.of(tome);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

}
