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
import cube.client.listener.MessageReceiveListener;
import cube.client.listener.MessageSendListener;
import cube.common.entity.Contact;
import cube.common.entity.Message;

/**
 * 测试消息监听器监听器。
 */
public class TestMessageListener {


    public static void main(String[] args) {
        Object mutex = new Object();

        Client client = new Client("127.0.0.1", "admin", "shixincube.com");

        Helper.sleepInSeconds(3);

        Contact contactA = client.getContact("shixincube.com", 50001001L);
        Contact contactB = client.getContact("shixincube.com", 63045555L);

        client.getMessageService().registerMessageReceiveListener(contactA, new MessageReceiveListener() {
            @Override
            public void onReceived(Message message) {
                System.out.println("[TestMessageListener] onReceived : " + contactA.getId() + " - " + message.getPayload().toString());
            }
        });

        client.getMessageService().registerMessageSendListener(contactB, new MessageSendListener() {
            @Override
            public void onSent(Message message) {
                System.out.println("[TestMessageListener] onSent : " + contactB.getId() + " - " + message.getPayload().toString());
            }
        });

        System.out.println("[TestMessageListener] Waiting");
        synchronized (mutex) {
            try {
                mutex.wait(10 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("*** END ***");
        client.destroy();
    }
}
