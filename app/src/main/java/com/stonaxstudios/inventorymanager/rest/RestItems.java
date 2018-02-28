package com.stonaxstudios.inventorymanager.rest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Matthew on 6/16/2016.
 */
public class RestItems {

    String urlStr = "";
    String json = "";
    JSONObject jsonObject = null;


    public RestItems(String url)
    {
        this.urlStr = url;

    }

    public JSONObject getItems()
    {
        try{
            URL url = new URL(urlStr);

            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            conn.setConnectTimeout(2000);

            InputStream in = new BufferedInputStream(conn.getInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            StringBuilder sb = new StringBuilder();
            String line;
            while((line = reader.readLine())!=null)
            {
                sb.append(line + "\n");
            }

            in.close();

            json = sb.toString();



        }catch(MalformedURLException e)
        {
            return null;
        }catch(IOException i){
            return null;
        }

        try{
            jsonObject = new JSONObject(json);
        }catch(JSONException j)
        {
            return null;
        }

        return jsonObject;

    }
}
