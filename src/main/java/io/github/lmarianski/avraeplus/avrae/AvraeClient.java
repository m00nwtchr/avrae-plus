package io.github.lmarianski.avraeplus.avrae;

import io.github.lmarianski.avraeplus.OAuthClient;
import io.github.lmarianski.avraeplus.avrae.homebrew.spells.Tome;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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

    public static Tome getTome(String id) {
        try {

            ProcessBuilder builder = new ProcessBuilder(
                    "curl", API_ENDPOINT+"/homebrew/spells/"+id,
                    "--header", "Authorization: "+OAuthClient.getToken().accessToken
            );

            Process p = builder.start();

//            HttpURLConnection conn = (HttpURLConnection)new URL(API_ENDPOINT+"/homebrew/spells/"+id).openConnection();
//
//            //byte[] postData = formData.getBytes("UTF8");
//            //int dataLength = postData.length;
//
//            conn.setRequestMethod("GET");
//
//            //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            //conn.setRequestProperty("Content-Length", Integer.toString(dataLength));
//            //conn.setRequestProperty("Charset", "utf-8");
//            conn.setRequestProperty("Authorization", OAuthClient.getToken().accessToken);
//
//            //conn.setDoInput(true);
//            conn.setDoOutput(true);
//
//            // try(DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
//            //     wr.write(postData);
//            // }
//
//            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String response = IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);

            String jsonString = id.equalsIgnoreCase("srd") ? "{\"spells\":"+response+"}" : response;

            Tome t = Tome.fromJSON(jsonString);
            t.id = id;
            return t;
        } catch (Exception e) {e.printStackTrace();}
        return null;
    }

}