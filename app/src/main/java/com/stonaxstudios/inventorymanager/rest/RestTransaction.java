package com.stonaxstudios.inventorymanager.rest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by Matthew on 7/4/2016.
 */
public class RestTransaction {

    String url;

    public RestTransaction(String url)
    {
        this.url = url;
    }


    public int postTransaction(String transDate,int itemNo,int quantity)
    {
        String output = "";
        int success = 0;
        try
        {
            HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setRequestMethod("POST");

            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty( "charset", "utf-8");
            //conn.setRequestProperty("Content-Type", "application/json");
            //conn.setRequestProperty("Accept", "application/json");

            String requsetBody = buildQuery(transDate,itemNo,quantity);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8")); //creates a new output stream to write data to server
            writer.write(requsetBody); //sends the request body as json
            writer.flush(); //flushes the output stream
            writer.close();  //close the output stream
            os.close();


            InputStream in = new BufferedInputStream(conn.getInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            StringBuilder sb = new StringBuilder();
            String line;
            while((line = reader.readLine())!=null)
            {
                sb.append(line + "\n");
            }

            in.close();
            output = sb.toString();

        }catch(MalformedURLException mal)
        {
            success = 0;
        }catch(IOException i){
            success = 0;
        }

        try{
            JSONObject jsonObject = new JSONObject(output);
            success = jsonObject.getInt("success");
        }catch(JSONException e)
        {
            success = 0;
        }





        return success;
    }

    private String buildQuery(String transDate,int itemNo,int quantity) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();

        result.append(URLEncoder.encode("TransDate","UTF-8"));
        result.append("=");
        result.append(URLEncoder.encode(transDate,"UTF-8"));

        result.append("&");

        result.append(URLEncoder.encode("ItemNo","UTF-8"));
        result.append("=");
        result.append(URLEncoder.encode(itemNo+"","UTF-8"));

        result.append("&");

        result.append(URLEncoder.encode("Quantity","UTF-8"));
        result.append("=");
        result.append(URLEncoder.encode(quantity+"","UTF-8"));

        return result.toString();

    }
}
