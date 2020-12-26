package io.github.lmarianski.avraeplus.data.sources.avrae;

import io.github.lmarianski.avraeplus.Util;
import io.github.lmarianski.avraeplus.data.sources.avrae.spells.Tome;

import java.net.URL;
import java.util.Objects;

public class AvraeClient {

    public static final String API_ENDPOINT = "https://api.avrae.io";

    public static final Tome SRD = getSRD();

    public static Tome getSRD() {
        if (SRD != null)
            return SRD;
        return Objects.requireNonNull(getTome("srd"));
    }

    public static Tome getTome(String id) {
        try {
//            boolean flag = isURL(id);
            URL url = new URL(API_ENDPOINT+"/homebrew/spells/"+id);

            String response = Util.GET(url);

            String jsonString = id.equalsIgnoreCase("srd") ? "{\"spells\":"+response+"}" : response;

            if (jsonString.contains("\"error\":")) {
                throw new Exception(jsonString);
            }

            Tome t = Tome.fromJSON(jsonString);
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

}