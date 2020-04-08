package io.github.lmarianski.avraeplus.avrae;

import io.github.lmarianski.avraeplus.OAuthClient;
import io.github.lmarianski.avraeplus.avrae.homebrew.spells.Tome;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class AvraeClient {

    public static final String API_ENDPOINT = "https://api.avrae.io";

    public static final Tome SRD = getSRD();

    public static Tome getSRD() {
        if (SRD != null)
            return SRD;
        return Objects.requireNonNull(getTome("srd"));
    }

    public static boolean isURL(String uri) {
        final URL url;
        try {
            url = new URL(uri);
        } catch (Exception e1) {
            return false;
        }
        return url.getProtocol().matches("http(s)");
    }

    public static Tome getTome(String id) {
        try {
            URL url = new URL(isURL(id) ? id : API_ENDPOINT+"/homebrew/spells/"+id);

            ProcessBuilder builder = new ProcessBuilder(
                    "curl", url.toString(),
                    "--header", "Authorization: "+OAuthClient.getToken().accessToken
            );

            Process p = builder.start();

            String response = IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);

            String jsonString = id.equalsIgnoreCase("srd") ? "{\"spells\":"+response+"}" : response;

            Tome t = Tome.fromJSON(jsonString);
            t.id = id;

            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}