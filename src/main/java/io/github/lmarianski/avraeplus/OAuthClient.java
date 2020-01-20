package io.github.lmarianski.avraeplus;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class OAuthClient {

    public static final String API_ENDPOINT  = "https://discordapp.com/api";
    public static String CLIENT_ID;
    public static String CLIENT_SECRET;

    public static Token lastToken;
    public static long lastTokenTime;

    public static Token getToken() {
        HttpURLConnection conn = null;
        try {
            if (lastToken != null && System.currentTimeMillis() <= lastToken.expiresIn + lastTokenTime) {
                return lastToken;
            }

            ProcessBuilder builder = new ProcessBuilder(
                    "curl", "https://discordapp.com/api/oauth2/token",
                    "-d", "grant_type=client_credentials&scope=identify",
                    "--header", "Authorization: Basic "+Base64.getEncoder().encodeToString((CLIENT_ID+":"+CLIENT_SECRET).getBytes(StandardCharsets.UTF_8))
            );

            Process p = builder.start();



//            conn = (HttpURLConnection) new URL(API_ENDPOINT + "/oauth2/token").openConnection();
//            //Authenticator.setDefault(new PasswordAuthenticator(CLIENT_ID, CLIENT_SECRET));
//
//            String formData = String.format("grant_type=%s&scope=%s",
//                    "client_credentials", "identify");
//
//            byte[] postData = formData.getBytes(StandardCharsets.UTF_8);
//            int dataLength = postData.length;
//
//            conn.setRequestMethod("POST");
//
//            //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            //conn.setRequestProperty("Content-Length", Integer.toString(dataLength));
//            conn.setRequestProperty("Authorization", "Basic "+Base64.getEncoder().encodeToString((CLIENT_ID+":"+CLIENT_SECRET).getBytes(StandardCharsets.UTF_8)));
//            conn.setRequestProperty("charset", "utf-8");
//
//            conn.setDoInput(true);
//            conn.setDoOutput(true);
//
//            IOUtils.write(postData, conn.getOutputStream());


            lastToken = Token.fromJSON(IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8));
            //lastToken = Token.fromJSON(IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8));
            lastTokenTime = System.currentTimeMillis();

            //Authenticator.setDefault(null);
            return lastToken;
        } catch (Exception e) {
            e.printStackTrace();
//            if (conn != null) {
//                try {
//                    System.err.println(IOUtils.toString(conn.getErrorStream(), StandardCharsets.UTF_8));
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
        }
        return null;
    }

    public static class Token {

        @SerializedName("access_token")
        public String accessToken;
        public String scope;
        @SerializedName("token_type")
        public String tokenType;
        public int expiresIn;

        public static Token fromJSON(String json) {
            return Main.gson.fromJson(json, Token.class);
        }
    }

    public static class PasswordAuthenticator extends Authenticator {

        private String login;
        private char[] password;

        public PasswordAuthenticator(String login, String password) {
            this.login = login;
            this.password = password.toCharArray();
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(login, password);
        }
    }
}