package io.github.lmarianski.avraeplus;

import java.io.*;
import java.net.*;
import java.nio.Buffer;

import org.json.JSONObject;

public class AvraeClient {

    public static final String API_ENDPOINT = "https://api.avrae.io";

    public static JSONObject getTome(String id) {
        try {
            String discordToken = OAuthClient.getToken().getString("access_token");

            HttpURLConnection conn = (HttpURLConnection)new URL(API_ENDPOINT+"/homebrew/spells/"+id).openConnection();
            
            //byte[] postData = formData.getBytes("UTF8");
            //int dataLength = postData.length;

            conn.setRequestMethod("GET");

            //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //conn.setRequestProperty("Content-Length", Integer.toString(dataLength));
            //conn.setRequestProperty("Charset", "utf-8");
            conn.setRequestProperty("Authorization", discordToken);

            //conn.setDoInput(true);
            conn.setDoOutput(true);

            // try(DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            //     wr.write(postData);
            // }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line = br.readLine();

            System.out.println(line);

            return new JSONObject(line);
        } catch (Exception e) {e.printStackTrace();}
        return null;
    }

}