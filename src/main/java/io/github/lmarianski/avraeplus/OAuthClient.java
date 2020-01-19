package io.github.lmarianski.avraeplus;

import java.io.*;
import java.net.*;
import java.nio.Buffer;

import org.json.JSONObject;

public class OAuthClient {

    public static final String API_ENDPOINT  = "https://discordapp.com/api";
    public static final String CLIENT_ID     = "@discordClientId@";
    public static final String CLIENT_SECRET = "@discordClientSecret@";

    public static JSONObject lastToken;
    public static long lastTokenTime;

    public static JSONObject getToken() throws Exception {

        if (lastToken != null && System.currentTimeMillis() <= lastToken.getInt("expires_in")+lastTokenTime) {
            return lastToken;
        }

        HttpURLConnection conn = (HttpURLConnection)new URL(API_ENDPOINT+"/oauth2/token").openConnection();

        conn.setAuthenticator(new DiscordAuthenticator());

        String formData = String.format("grant_type=%s&scope=%s",
            "client_credentials", "identify");

        System.out.println(formData);

        byte[] postData = formData.getBytes("UTF8");
        int dataLength = postData.length;

        conn.setRequestMethod("POST");

        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", Integer.toString(dataLength));
        conn.setRequestProperty("Charset", "utf-8");

        conn.setAuthenticator(new DiscordAuthenticator());

        conn.setDoInput(true);
        conn.setDoOutput(true);

        try(DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        lastToken = new JSONObject(br.readLine());
        lastTokenTime = System.currentTimeMillis();

        return lastToken;
    }

    // public static String exchangeCode(String code) throws Exception {
    //     HttpURLConnection conn = (HttpURLConnection)new URL(API_ENDPOINT+"/oauth2/token").openConnection();

    //     String formData = String.format("client_id=%s&client_secret=%s&code=%s&grant_type=%s&scope=%s&redirect_uri=%s",
    //         CLIENT_ID, CLIENT_SECRET, code, "authorization_code", "identify", "http%3A%2F%2Flocalhost");

    //     System.out.println(formData);

    //     byte[] postData = formData.getBytes("UTF8");
    //     int dataLength = postData.length;

    //     conn.setRequestMethod("POST");

    //     conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    //     conn.setRequestProperty("Content-Length", Integer.toString(dataLength));
    //     conn.setRequestProperty("charset", "utf-8");

    //     conn.setDoInput(true);
    //     conn.setDoOutput(true);

    //     try(DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
    //         wr.write(postData);
    //     }

    //     BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    //     return br.readLine();
    // }

    public static class DiscordAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(CLIENT_ID, CLIENT_SECRET.toCharArray());
        }
    }
}