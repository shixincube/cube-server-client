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


import cube.client.CubeClient;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * 测试推送消息。
 */
public class TestPush {


    public static void main(String[] args) {

        CubeClient client = new CubeClient("127.0.0.1");

        Helper.sleepInSeconds(3);

        System.out.println("[TestPush] Push Message");

        Contact receiver = new Contact(11444455L, "shixincube.com", "Cube");

        Contact pretender = new Contact(50001001L, "shixincube.com", "Pretender");

        Device device = new Device("Server", "Server Client");

        JSONObject payload = new JSONObject();
        payload.put("type", "hypertext");
        payload.put("content", "今天周四 11月4日 " + (new Date()).toString());

        boolean result = client.pushMessageWithPretender(receiver, pretender, device, payload);
        System.out.println("[TestPush] Push Result: " + result);

        System.out.println("** END ***");
        client.destroy();
    }
}
