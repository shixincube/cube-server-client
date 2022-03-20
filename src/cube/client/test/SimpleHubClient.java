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
import cube.client.hub.HubSignalListener;
import cube.common.entity.Contact;
import cube.hub.signal.Signal;

import java.util.ArrayList;
import java.util.List;

public class SimpleHubClient implements HubSignalListener {

    private Object mutex;

    protected SimpleHubClient(Object mutex) {
        this.mutex = mutex;
    }

    @Override
    public void onSignal(Signal signal) {
        System.out.println("SimpleHubClient #onSignal - " + signal.getName());

        synchronized (this.mutex) {
            this.mutex.notify();
        }
    }

    public static void main(String[] args) {
        long[] idList = new long[] {
                11000
        };
        List<Client> clientList = new ArrayList<>();

        System.out.println("SimpleHubClient - START " + idList.length);

        Object mutex = new Object();

        for (int i = 0; i < idList.length; ++i) {
            final long id = idList[i];
            (new Thread() {
                @Override
                public void run() {
                    Client client = new Client("127.0.0.1", "admin", "shixincube.com");
                    if (!client.waitReady()) {
                        client.destroy();
                        return;
                    }

                    Contact contact = new Contact(id, "shixincube.com");
                    client.prepare(contact, true);

                    HubController.getInstance().addListener(new SimpleHubClient(mutex));

                    System.out.println("SimpleHubClient - client ready : " + contact.getId());

                    clientList.add(client);
                }
            }).start();
        }

        synchronized (mutex) {
            try {
                mutex.wait(3 * 6 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("SimpleHubClient - END");

        for (Client client : clientList) {
            client.destroy();
        }
    }
}
