package io.github.lmarianski.avraeplus.data.sources.json;

import io.github.lmarianski.avraeplus.Main;
import io.github.lmarianski.avraeplus.Util;

import java.io.IOException;

public class JsonClient {

    public static JsonTome getTome(String source) {
        try {
            JsonTome tome =  Main.gson.fromJson(Util.GET(source), JsonTome.class);
            tome.url = source;
            return tome;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
