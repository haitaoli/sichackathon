package com.sensoria.webapi;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WebServiceBase {
    final public String baseUrl = "http://sensoriafitnesswebapi.cloudapp.net/api/1.0/";
    private byte[] buffer = new byte[4096];

    public void uploadFile(String url, InputStream content) {
        try {
            URL fileUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)fileUrl.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            int len = content.read(buffer);
            while (len > 0) {
                connection.getOutputStream().write(buffer, 0, len);
                len = content.read(buffer);
            }

            connection.getOutputStream().close();
            int response = connection.getResponseCode();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String sendGetRequest(String url) {
        StringBuilder builder = new StringBuilder();
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-type:", "application/json");
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
        }
        catch (ClientProtocolException e) {
            return null;
        }
        catch (IOException e) {
            return null;
        }

        return builder.toString();
    }

    public String sendPostRequest(String url, JSONObject json) {
        StringBuilder builder = new StringBuilder();
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-type", "application/json");

            StringEntity se = new StringEntity(json.toString());
            String j = json.toString();
            httpPost.setEntity(se);

            HttpResponse response = client.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode()/100 == 2) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
        }
        catch (ClientProtocolException e) {
            return null;
        }
        catch (IOException e) {
            return null;
        }

        return builder.toString();
    }

}
