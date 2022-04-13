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

import cell.util.Base64;
import cell.util.Utils;
import cube.common.entity.ContactZone;
import cube.common.entity.Conversation;
import cube.common.entity.FileLabel;
import cube.hub.EventBuilder;
import cube.hub.event.*;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestHub {

    private String address = "127.0.0.1";
//    private String address = "";

    private String code = "bGhjaFWGKsStbiDKmjhOWIOXZjQFOcmh";

    public TestHub() {
    }

    private String getAddressString() {
        return "http://" + this.address + ":7010/";
    }

    private JSONObject httpGet(String urlString) {
        return this.httpGet(urlString, null);
    }

    private JSONObject httpGet(String urlString, Map<String, String> params) {
        String validUrl = urlString + "/" + this.code;
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

    private JSONObject httpPost(String urlString, JSONObject body) {
        String validUrl = urlString + "/" + this.code;

        System.out.println("HTTP [POST] : " + validUrl);

        JSONObject responseJSON = null;

        BufferedReader reader = null;
        try {
            URL url = new URL(validUrl);
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

    private JSONObject httpPostForm(String urlString, File file) {
        String validUrl = urlString + "/" + this.code;

        System.out.println("HTTP [POST] : " + validUrl);

        JSONObject responseJSON = null;

        BufferedReader reader = null;
        try {
            URL url = new URL(validUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "multipart/form-data");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            // 连接
            connection.connect();

            OutputStream os = connection.getOutputStream();

            String boundary = "-------------" + Utils.randomString(32);
            os.write(boundary.getBytes(StandardCharsets.UTF_8));
            os.write('\n');

            String filenameLine = "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"";
            os.write(filenameLine.getBytes(StandardCharsets.UTF_8));
            os.write('\n');

            os.write("Content-Type: application/octet-stream".getBytes(StandardCharsets.UTF_8));
            os.write('\n');

            os.write('\n');
            byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
            os.write(bytes);
            os.write('\n');
            os.write((boundary + "--").getBytes(StandardCharsets.UTF_8));

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

    private File download(String urlString, String fileCode) {
        String filename = urlString.substring(urlString.lastIndexOf("/") + 1);
        return this.download(urlString, fileCode, filename);
    }

    private File download(String urlString, String fileCode, String downloadFilename) {
        System.out.println("HTTP (download) : " + urlString);

        String filename = downloadFilename;
        File file = null;
        FileOutputStream fos = null;

        try {
            URL url = new URL(urlString + "/" + this.code + "/" + fileCode);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            // 连接
            connection.connect();
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                System.out.println("Response OK");

                Map<String, List<String>> headers = connection.getHeaderFields();
                for (String key : headers.keySet()) {
                    if (null != key && key.contains("Content-Disposition")) {
                        for (String content : headers.get(key)) {
                            if (content.contains("filename")) {
                                String[] tmp = content.split("=");
                                filename = URLEncoder.encode(tmp[1], "UTF-8");
                                break;
                            }
                        }
                    }
                }

                file = new File("data/" + filename);
                if (file.exists()) {
                    file.delete();
                }

                fos = new FileOutputStream(file);

                byte[] buf = new byte[2048];
                int length = 0;
                InputStream is = connection.getInputStream();
                while ((length = is.read(buf)) > 0) {
                    fos.write(buf, 0, length);
                }

                System.out.println("Write to file : " + file.getName());
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
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

        return file;
    }

    public void testOpen() {
        System.out.println("#testOpen");

        String urlString = getAddressString() + "hub/open";
        JSONObject json = httpGet(urlString);
        if (null == json) {
            System.out.println("Error");
            return;
        }

        Event event = EventBuilder.build(json);
        System.out.println(event.getName());
        System.out.println(event.getCode());
        System.out.println(event.getFileLabel().getFileCode());

        // 下载
        urlString = getAddressString() + "hub/file";
        File file = download(urlString, event.getFileLabel().getFileCode(),"wechat_qr_" + event.getCode() + ".png");
        if (null != file) {
            System.out.println("QR file: " + file + " - " + file.exists());
        }
        else {
            System.out.println("Can NOT download file: " + event.getFileLabel().getFileName());
        }
    }

    public void testClose() {
        System.out.println("#testClose");

        String urlString = getAddressString() + "hub/close";
        JSONObject json = httpGet(urlString);
        if (null == json) {
            System.out.println("Error");
            return;
        }

        Event event = EventBuilder.build(json);
        if (event instanceof LogoutEvent) {
            System.out.println("Close channel: " + event.getCode());
        }
        else {
            System.out.println("Close failed");
        }
    }

    public void testGetAccount() {
        System.out.println("#testGetAccount");

        String urlString = getAddressString() + "hub/account";
        JSONObject json = httpGet(urlString);
        if (null == json) {
            System.out.println("Error");
            return;
        }

        Event event = EventBuilder.build(json);
        if (event instanceof AccountEvent) {
            System.out.println(((AccountEvent) event).getAccount().toJSON());
        }
        else {
            System.out.println("Event error");
        }
    }

    public void testGetConversations() {
        System.out.println("#testGetConversations");

        String urlString = getAddressString() + "hub/conversations";
        JSONObject json = httpGet(urlString);
        if (null == json) {
            System.out.println("Error");
            return;
        }

        Event event = EventBuilder.build(json);
        ConversationsEvent conversationsEvent = (ConversationsEvent) event;
        for (Conversation conversation : conversationsEvent.getConversations()) {
            System.out.println("Conversation: " + conversation.getPivotalEntity().getName() +
                    " - " + conversation.getRecentMessages().size());
        }
    }

    public void testGetMessages() {
        System.out.println("#testGetMessages");

        Map<String, String> params = new HashMap<>();
        params.put("begin", "0");
        params.put("end", "9");
        params.put("cid", "chu190_6");

        String urlString = getAddressString() + "hub/messages";
        JSONObject json = httpGet(urlString, params);
        if (null == json) {
            System.out.println("Error");
            return;
        }

        Event event = EventBuilder.build(json);
        MessagesEvent messagesEvent = (MessagesEvent) event;
        System.out.println("Size: " + messagesEvent.getMessages().size());
    }

    public void testGetContactBook() {
        System.out.println("#testGetContactBook");

        Map<String, String> params = new HashMap<>();
        params.put("begin", "0");
        params.put("end", "9");

        String urlString = getAddressString() + "hub/book";
        JSONObject json = httpGet(urlString, params);
        if (null == json) {
            System.out.println("Error");
            return;
        }

        Event event = EventBuilder.build(json);
        if (null != event) {
            ContactZoneEvent contactZoneEvent = (ContactZoneEvent) event;
            ContactZone zone = contactZoneEvent.getContactZone();
            System.out.println("Begin: " + contactZoneEvent.getBeginIndex());
            System.out.println("End  : " + contactZoneEvent.getEndIndex());
            System.out.println("Total: " + contactZoneEvent.getTotalSize());
            System.out.println("Zone size: " + zone.getParticipants().size());

            System.out.println(zone.getParticipants().get(0).getLinkedContact().getContext().toString());
        }
        else {
            System.out.println("Data error");
        }
    }

    public void testSendText() {
        System.out.println("#testSendText");

        String content = "貌似今天会一直下雨";

        String urlString = getAddressString() + "hub/message";

        JSONObject payload = new JSONObject();
        payload.put("text", Base64.encodeBytes(content.getBytes(StandardCharsets.UTF_8)));
        payload.put("partnerId", "wxid_hquknrtbiiod22");

        JSONObject result = httpPost(urlString, payload);
        if (null == result) {
            System.out.println("Failed");
        }
        else {
            System.out.println("Result : " + result.toString(2));
        }
    }

    public void testPostFileData() {
        System.out.println("#testPostFileData");

        String urlString = getAddressString() + "hub/file";

        JSONObject result = httpPostForm(urlString, new File("data/mini.txt"));
        if (null == result) {
            System.out.println("Failed");
        }
        else {
            FileLabel fileLabel = new FileLabel(result);
            System.out.println("File: " + fileLabel.getFileName());
            System.out.println("File Code: " + fileLabel.getFileCode());
        }
    }

    public static void main(String[] args) {
        TestHub test = new TestHub();
//        test.testOpen();
//        test.testClose();
//        test.testGetAccount();

//        test.testGetConversations();
//        test.testGetContactBook();
//        test.testGetMessages();

//        test.testSendText();
        test.testPostFileData();
    }
}
