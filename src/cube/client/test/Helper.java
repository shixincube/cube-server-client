/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.client.test;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 辅助函数。
 */
public final class Helper {

    private Helper() {
    }

    public static JSONObject httpGet(String urlString) {
        return Helper.httpGet(urlString, null);
    }

    public static JSONObject httpGet(String urlString, Map<String, String> params) {
        String validUrl = new String(urlString);
        if (null != params) {
            try {
                validUrl += "?";
                for (String key : params.keySet()) {
                    validUrl += key + "=" + URLEncoder.encode(params.get(key), "UTF-8");
                    validUrl += "&";
                }
                validUrl = validUrl.substring(0, validUrl.length() - 1);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        System.out.println("HTTP [GET] : " + validUrl);

        JSONObject json = null;

        BufferedReader reader = null;
        try {
            URL url = new URL(validUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setUseCaches(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            // 连接
            connection.connect();
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                System.out.println("Response OK");

                StringBuilder buf = new StringBuilder();

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                }

                json = new JSONObject(buf.toString());
            }
            else {
                System.out.println("Response : " + code);
            }

            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        return json;
    }

    public static JSONObject httpPost(String urlString, JSONObject body) {
        System.out.println("HTTP [POST] : " + urlString);

        JSONObject responseJSON = null;

        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            // 连接
            connection.connect();

            OutputStream os = connection.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                System.out.println("Response OK");

                StringBuilder buf = new StringBuilder();

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                }

                responseJSON = new JSONObject(buf.toString());
            }
            else {
                System.out.println("Response : " + code);
            }

            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        return responseJSON;
    }

    public static void sleepInSeconds(int seconds) {
        int count = seconds;

        while (count > 0) {
            --count;
            System.out.println("Sleep count down: " + count);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
