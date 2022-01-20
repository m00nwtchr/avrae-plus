package io.github.lmarianski.avraeplus.data.sources.avrae;

import io.github.lmarianski.avraeplus.Main;
import io.github.lmarianski.avraeplus.Util;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpellCollection;
import io.github.lmarianski.avraeplus.data.sources.avrae.spells.Tome;
import io.github.lmarianski.avraeplus.data.sources.avrae.spells.Tome.AvraeSpell;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.reflect.TypeToken;

public class AvraeClient {

    public static final String API_ENDPOINT = "https://api.avrae.io";

    private static Tome SRD;

    private static final TypeToken<AvraeApiResponse<AvraeSpell[]>> AVRAE_SRD_RESPONSE_TOKEN = new TypeToken<AvraeApiResponse<AvraeSpell[]>>() {};
    private static final TypeToken<AvraeApiResponse<Tome>> AVRAE_TOME_RESPONSE_TOKEN = new TypeToken<AvraeApiResponse<Tome>>() {};

    static {
        try {
            SRD = getSRD();
        } catch (Exception e) {
        }
    }

    public static Tome getSRD() {
        if (SRD != null)
            return SRD;
        SRD = (Tome) getTome("srd").get();
        return SRD;
    }

    public static Optional<ISpellCollection> getTome(String id) {
        try {
            URL url = new URL(API_ENDPOINT+"/homebrew/spells/"+id);

            String response = Util.GET(url);

            AvraeApiResponse<Tome> apiResponse;
            if (id.equalsIgnoreCase("srd")) {
                apiResponse = new AvraeApiResponse<Tome>();

                AvraeApiResponse<AvraeSpell[]> srdResponse = AvraeApiResponse.fromJSON(response, AVRAE_SRD_RESPONSE_TOKEN.getType());

                apiResponse.success = srdResponse.success;
                apiResponse.error = srdResponse.error;

                apiResponse.data = new Tome();
                apiResponse.data.spells = srdResponse.data;
            } else {
                apiResponse = AvraeApiResponse.fromJSON(response, AVRAE_TOME_RESPONSE_TOKEN.getType());
            }

            Tome t = apiResponse.data;

            if (!apiResponse.success) {
                throw new Exception(apiResponse.error);
            }

            t.id = id;

            if (t.name == null) {
                t.name = id;
            }

            return Optional.of(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }


    static class AvraeApiResponse<T> {
        public boolean success;
        public String error;
        public T data;

        public static <T> AvraeApiResponse<T> fromJSON(String jsonText, Type type) {
            try {
                return Main.gson.fromJson(jsonText, type);
            } catch (Exception e) {
                System.err.println(e);
                return null;
            }
        }
    }
}