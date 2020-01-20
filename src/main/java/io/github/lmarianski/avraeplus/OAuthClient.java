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

            conn = (HttpURLConnection) new URL(API_ENDPOINT + "/oauth2/token").openConnection();
            Authenticator.setDefault(new PasswordAuthenticator(CLIENT_ID, CLIENT_SECRET));

            String formData = String.format("grant_type=%s&scope=%s",
                    "client_credentials", "identify");

            byte[] postData = formData.getBytes(StandardCharsets.UTF_8);
            int dataLength = postData.length;

            conn.setRequestMethod("POST");

            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer.toString(dataLength));
            conn.setRequestProperty("Charset", "utf-8");

            conn.setDoInput(true);
            conn.setDoOutput(true);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
            }

            lastToken = Token.fromJSON(IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8));
            lastTokenTime = System.currentTimeMillis();

            Authenticator.setDefault(null);
            return lastToken;
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    System.err.println(IOUtils.toString(conn.getErrorStream(), StandardCharsets.UTF_8));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
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

        private final String login;
        private final char[] password;

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