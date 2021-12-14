package io.github.lmarianski.avraeplus.data.sources.avrae;

import io.github.lmarianski.avraeplus.Main;
import io.github.lmarianski.avraeplus.Util;
import io.github.lmarianski.avraeplus.data.sources.avrae.spells.Tome;
import io.github.lmarianski.avraeplus.data.sources.avrae.spells.Tome.AvraeSpell;

import java.net.URL;
import java.util.Objects;

import com.google.gson.reflect.TypeToken;

public class AvraeClient {

    public static final String API_ENDPOINT = "https://api.avrae.io";

    private static Tome SRD;

    static {
        try {
            SRD = getSRD();
        } catch (Exception e) {
        }
    }

    public static Tome getSRD() {
        if (SRD != null)
            return SRD;
        SRD = Objects.requireNonNull(getTome("srd"));
        return SRD;
    }

    public static Tome getTome(String id) {
        try {
            URL url = new URL(API_ENDPOINT+"/homebrew/spells/"+id);

            String response = Util.GET(url);

            AvraeApiResponse<Tome> apiResponse;
            if (id.equalsIgnoreCase("srd")) {
                apiResponse = new AvraeApiResponse<Tome>();

                AvraeApiResponse<AvraeSpell[]> srdResponse = AvraeApiResponse.fromJSON(response, AvraeSpell[].class);

                apiResponse.success = srdResponse.success;
                apiResponse.error = srdResponse.error;

                apiResponse.data = new Tome();
                apiResponse.data.spells = srdResponse.data;
            }
            apiResponse = AvraeApiResponse.fromJSON(response, Tome.class);

            Tome t = apiResponse.data;

            if (!apiResponse.success) {
                throw new Exception(apiResponse.error);
            }

            t.id = id;

            if (t.name == null) {
                t.name = id;
            }

            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    static class AvraeApiResponse<T> {
        public boolean success;
        public String error;
        public T data;

        public static <T> AvraeApiResponse<T> fromJSON(String jsonText, Class<T> clazz) {
            try {
                return Main.gson.fromJson(jsonText, (new TypeToken<AvraeApiResponse<T>>() {}).getType());
            } catch (Exception e) {
                System.err.println(e);
                return null;
            }
        }
    }
}