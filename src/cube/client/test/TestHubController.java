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
import cube.client.hub.HubController;
import cube.common.entity.Contact;
import cube.common.entity.ConversationType;
import cube.common.entity.Group;
import cube.common.entity.Message;
import cube.hub.event.SubmitMessagesEvent;
import cube.hub.signal.PassBySignal;
import cube.hub.signal.SendMessageSignal;
import cube.hub.signal.Signal;
import cube.hub.signal.SilenceSignal;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TestHubController {

    public static void testSubmitMessagesEvent(Client client) {
        System.out.println("*** START testNewMessageEvent ***");

        long id = 98765;

        JSONObject payload = new JSONObject();
        payload.put("content", Utils.randomString(32));

        Contact account = new Contact();
        account.setName("来自火星");

        Contact sender = new Contact();
        sender.setName("少年");

        Group group = new Group();
        group.setName("阳关灿烂的日子");

        List<Message> messageList = new ArrayList<>();
        Message message = new Message(id, sender, group, System.currentTimeMillis(), payload);
        messageList.add(message);

        SubmitMessagesEvent event = new SubmitMessagesEvent(account, group, messageList);

        boolean result = client.getHubController().sendEvent(event);
        System.out.println("Result : " + result);

        System.out.println("*** END ***");
    }

    public static void testPassBySignal(Client client) {
        System.out.println("*** START testPassBySignal ***");

        PassBySignal passBySignal = new PassBySignal(true);

        // 静默 3 分钟
        passBySignal.addSignal(new SilenceSignal(10 * 60 * 1000L));
        passBySignal.addDestination(10009L);

        Signal result = client.getHubController().sendSignal(passBySignal);
        System.out.println("Result : " + (null != result));

        System.out.println("*** END ***");
    }

    public static void testSendMessageSignal(Client client) {
        System.out.println("*** START testPassBySignal ***");

        PassBySignal passBySignal = new PassBySignal(true);

        // 发送消息
        //Contact account = DataHelper.makeContact("歌的旋律释怀心的情绪", "leif668800");
        //Contact partner = DataHelper.makeContact("立福", "chu190_6");

        SendMessageSignal signal = new SendMessageSignal("bGhjaFWGKsStbiDKmjhOWIOXZjQFOcmh",
                ConversationType.Contact,
                "chu190_6");
        signal.setText("来自透传测试 - " + Utils.randomString(4));

        passBySignal.addSignal(signal);
        passBySignal.addDestination(10009L);

        Signal result = client.getHubController().sendSignal(passBySignal);
        System.out.println("Result : " + (null != result));

        System.out.println("*** END ***");
    }

    public static void main(String[] args) {
        Client client = new Client("", "admin", "shixincube.com");

        if (!client.waitReady()) {
            client.destroy();
            return;
        }

        Contact contact = new Contact(10000, "shixincube.com");
        client.prepare(contact, true);

//        testSubmitMessagesEvent(client);
        testPassBySignal(client);
//        testSendMessageSignal(client);

        client.destroy();
    }
}
