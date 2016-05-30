package com.example.lxy.testlearn.lanscanner;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by lxy on 16-5-24.
 */
public class DeviceInfoResolver {

    private static final String TAG = "DeviceInfoResolver";
    private static final boolean DEBUG = true;

    private static final String REQUEST_URL = "http://www.macvendorlookup.com/api/v2/";

    public static void test() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String info = requestDeviceInfoByMac("ac:fd:ce:20:e9:6e");
                Log.d(TAG, "DeviceInfoResolver TEST = " + info);
            }
        }).start();

    }


    public static String requestDeviceInfoByMac(String mac) {
        try {
            String url = REQUEST_URL + convertMacForRequest(mac);
            String rawResponse = commonGet(url);
            if (DEBUG) {
                 Log.d(TAG, "rawResponse = " + rawResponse);
            }
            if (rawResponse != null) {
                return getDeviceInfoFromResponse(rawResponse);
            }
        } catch (IOException e) {
            if (DEBUG) {
                Log.d(TAG, " requestDeviceInfoByMac error! " + e);
            }
        }
        return null;
    }

    public static String getDeviceInfoFromResponse(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            String company = jsonObject.getString("company");
            if (TextUtils.isEmpty(company)) {
                return null;
            } else {
                return company;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String convertMacForRequest(String rawMac) {
        return rawMac.toUpperCase().replaceAll(":","-");
    }

    public static String commonGet(String url)
            throws IOException {
        if (DEBUG) {
            Log.d(TAG, "commonGet() " + url);
        }
        HttpURLConnection httpConn = getHttpConnection(url, false);
        try {
            httpConn.connect();
            int statusCode = httpConn.getResponseCode();
            if (statusCode != 200) {
                return null;
            }
            return getResponse(httpConn, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            httpConn.disconnect();
        }
    }

    private static String getResponse(HttpURLConnection httpConn, boolean tryErrorStream) throws IOException {
        String contentEncoding = httpConn.getContentEncoding();
        if (DEBUG) {
            Log.d(TAG, "response code: " + httpConn.getResponseCode()
                    + ", encoding: " + contentEncoding
                    + ", method: " + httpConn.getRequestMethod());
        }

        InputStream httpInputStream = null;
        try {
            if (tryErrorStream) {
                try {
                    httpInputStream = httpConn.getInputStream();
                } catch (IOException e) {
                    if (DEBUG) {
                        Log.w(TAG, "failed to get input stream, to try error stream", e);
                    }
                    httpInputStream = httpConn.getErrorStream();
                }
            } else {
                httpInputStream = httpConn.getInputStream();
            }
        } catch (IllegalStateException e) {
            // ignore
        }
        if (httpInputStream == null) {
            // null will be returned on some phones
            throw new IOException("HttpURLConnection.getInputStream() returned null");
        }

        InputStream is;
        if (contentEncoding != null && contentEncoding.contains("gzip")) {
            is = new GZIPInputStream(httpInputStream);
        } else if (contentEncoding != null && contentEncoding.contains("deflate")) {
            is = new InflaterInputStream(httpInputStream);
        } else {
            is = httpInputStream;
        }

        // Read the response content
        try {
            byte[] responseContent = readAllBytes(is);
            return new String(responseContent);  // TODO default encoding
        } finally {
            // Must be called before calling HttpURLConnection.disconnect()
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                if (DEBUG) Log.w(TAG, "Failed to close the is", e);
            }
        }
    }

    public static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bytesBuf = new ByteArrayOutputStream(1024);
        int bytesRead = 0;
        byte[] readBuf = new byte[1024];
        while ((bytesRead = is.read(readBuf, 0, readBuf.length)) != -1) {
            bytesBuf.write(readBuf, 0, bytesRead);
        }
        return bytesBuf.toByteArray();
    }

    private static HttpURLConnection getHttpConnection(String url, boolean post) throws IOException {
        URL linkUrl = new URL(url);
        HttpURLConnection httpConn = (HttpURLConnection) linkUrl.openConnection();
        httpConn.setConnectTimeout(20 * 1000);
        httpConn.setReadTimeout(2000);
        httpConn.setDoInput(true);
        httpConn.setUseCaches(false);
        httpConn.setRequestProperty("Accept-Encoding", "gzip,deflate");
        httpConn.setRequestProperty("Charset", "UTF-8");
        httpConn.setRequestProperty("Content-Type", "application/json");
        if (post) {
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
        } else {
            httpConn.setRequestMethod("GET");  // by default
        }

        return httpConn;
    }



}
