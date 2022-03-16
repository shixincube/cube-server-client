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

import cube.client.Client;
import cube.client.hub.HubController;
import cube.common.entity.Contact;
import cube.hub.MetaMessage;
import cube.hub.event.NewMessageEvent;
import org.json.JSONObject;

public class TestHubController {

    public static void testNewMessageEvent(Client client) {
        System.out.println("*** START testNewMessageEvent ***");

        long id = 98765;
        String sender = "来自火星";
        String ground = "阳关灿烂的日子";
        long timestamp = System.currentTimeMillis();
        JSONObject meta = new JSONObject();

        MetaMessage metaMessage = new MetaMessage(id, sender, ground, timestamp, meta);
        NewMessageEvent event = new NewMessageEvent(metaMessage);
        boolean result = HubController.getInstance().sendEvent(event);
        System.out.println("Result : " + result);

        System.out.println("*** END ***");
    }

    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", "admin", "shixincube.com");

        if (!client.waitReady()) {
            client.destroy();
            return;
        }

        Contact contact = new Contact(10000, "shixincube.com");
        client.prepare(contact);

        testNewMessageEvent(client);

        client.destroy();
    }
}
