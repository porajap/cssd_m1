package com.poseintelligence.cssdm1.core.connect;

import android.util.Log;

import com.poseintelligence.cssdm1.CssdProject;
import com.poseintelligence.cssdm1.model.Parameter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class HTTPConnect  {

    public String sendPostRequest(String requestURL, HashMap<String, String> postDataParams) {
        int responseCode = 0;
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            Parameter pm = CssdProject.getPm();

            if(pm!=null){
                int B_ID = CssdProject.getPm().getBdCode();
                postDataParams.put("B_ID", B_ID+"");
            }

            postDataParams.put("p_DB", CssdProject.D_DATABASE);

            //System.out.println("URL = " + requestURL + "?" + getPostDataString(postDataParams));

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Connection","close");

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            System.out.println("URL = " + requestURL + "?" + getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();

            try {
                responseCode =
                        conn.getResponseCode();
            }catch (Exception e){

            }
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                response = br.readLine();
            } else {
                response = "Error Registering";
            }

            conn.disconnect();


        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d("tog_","Data = "+postDataParams);
        Log.d("tog_","result = "+response);
        return response;
    }

    public String sendPostRequest(String requestURL, HashMap<String, String> postDataParams, int time) {
        int responseCode = 0;
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(time);
            conn.setConnectTimeout(time);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Connection","close");

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            System.out.println("URL = " + requestURL + "?" + getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();

            try {
                responseCode =
                        conn.getResponseCode();
            }catch (Exception e){

            }
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                response = br.readLine();
            } else {
                response = "Error Registering";
            }

            conn.disconnect();


        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }
}