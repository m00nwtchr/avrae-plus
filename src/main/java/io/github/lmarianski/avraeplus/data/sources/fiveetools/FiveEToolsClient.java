package io.github.lmarianski.avraeplus.data.sources.fiveetools;

import io.github.lmarianski.avraeplus.Main;
import io.github.lmarianski.avraeplus.Util;
import io.github.lmarianski.avraeplus.data.sources.fiveetools.spells.FiveEBook;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static io.github.lmarianski.avraeplus.Main.MAP_STRING_STRING_TYPE;

public class FiveEToolsClient {

    public static final String API_ENDPOINT = "https://5e.tools/data";

    private static final Map<String, String> SOURCES = getIndex();

    public static Map<String, String> getIndex() {
        if (SOURCES != null) {
            return SOURCES;
        }

        String response = null;
        try {
            response = Util.GET(API_ENDPOINT + "/spells/index.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Main.gson.fromJson(response, MAP_STRING_STRING_TYPE);
    }

    public static FiveEBook[] getSources(String... ids) {
        return Arrays.stream(ids).map(FiveEToolsClient::getSource).toArray(FiveEBook[]::new);
    }

    public static FiveEBook getSource(String id) {
        String file = getIndex().get(id);


        String response = null;
        try {
            response = Util.GET(API_ENDPOINT + "/spells/" + file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FiveEBook t = Main.gson.fromJson(response, FiveEBook.class);
        t.id = id;
//        t.name = id;

        return t;
    }

}
