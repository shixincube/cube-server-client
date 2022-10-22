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


import cell.util.Utils;
import cube.client.Client;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;

/**
 * 测试推送消息。
 */
public class TestPush {

    public static void testPushMessage(Client client) {
        System.out.println("[TestPush] Push message");

        Contact receiver = new Contact(66611177L, "demo-ferryhouse-cube", "Cube");

        Contact pretender = new Contact(88811144L, "demo-ferryhouse-cube", "Pretender");

        Device device = new Device("Server", "Server Client");

        JSONObject payload = new JSONObject();
        payload.put("type", "hypertext");
        payload.put("content", "周日 10:34 " + Utils.gsDateFormat.format(new Date()));

        boolean result = client.getMessageService().pushMessageWithPretender(receiver, pretender, device, payload);
        System.out.println("[TestPush] Push message result: " + result);
    }

    public static void testPushFileMessage(Client client) {
        System.out.println("[TestPush] Push file message");

        File targetFile = new File("data/cube-framework.png");

        Contact receiver = new Contact(11444455L, "shixincube.com", "Cube");

        Contact pretender = new Contact(50001001L, "shixincube.com", "Pretender");

        boolean result = client.getMessageService().pushFileMessageWithPretender(receiver, pretender, targetFile);
        System.out.println("[TestPush] Push file message result: " + result);
    }

    public static void testPushImageMessage(Client client) {
        System.out.println("[TestPush] Push image message");

        File targetFile = new File("data/cube-framework.png");

        Contact receiver = new Contact(11444455L, "shixincube.com", "Cube");

        Contact pretender = new Contact(50001001L, "shixincube.com", "Pretender");

        boolean result = client.getMessageService().pushImageMessageWithPretender(receiver, pretender, targetFile);
        System.out.println("[TestPush] Push image message result: " + result);
    }


    public static void main(String[] args) {

        Client client = new Client("192.168.0.108", "admin", "shixincube.com");

        if (!client.waitReady()) {
            System.out.println("Client error");
            return;
        }

        Helper.sleepInSeconds(1);

        testPushMessage(client);

        System.out.println("*** END ***");
        client.destroy();
    }
}
