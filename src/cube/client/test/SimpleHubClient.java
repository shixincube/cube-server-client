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
import cube.client.hub.HubSignalListener;
import cube.common.entity.Contact;
import cube.hub.event.LoginQRCodeEvent;
import cube.hub.event.ReportEvent;
import cube.hub.signal.LoginQRCodeSignal;
import cube.hub.signal.ReportSignal;
import cube.hub.signal.Signal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SimpleHubClient implements HubSignalListener {

    private static boolean sSpinning = true;

    private Contact contact;

    protected SimpleHubClient(Contact contact) {
        this.contact = contact;
    }

    @Override
    public void onSignal(Signal signal) {
        String signalName = signal.getName();

        System.out.println("SimpleHubClient #onSignal - " + signalName);

        if (ReportSignal.NAME.equals(signalName)) {
            ReportEvent reportEvent = new ReportEvent(8, Utils.randomInt(2, 8), new ArrayList<>());
            HubController.getInstance().sendEvent(reportEvent);
        }
        else if (LoginQRCodeSignal.NAME.equals(signalName)) {
            try {
                Thread.sleep(Utils.randomInt(1000, 3000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            LoginQRCodeEvent event = new LoginQRCodeEvent(signal.getSerialNumber(),
                    new File("data/qr.jpg"));
            HubController.getInstance().sendEvent(event);
        }
    }

    public static void main(String[] args) {
        long[] idList = new long[] {
                11000
        };
        List<Client> clientList = new ArrayList<>();

        System.out.println("SimpleHubClient - START " + idList.length);

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

                    HubController.getInstance().addListener(new SimpleHubClient(contact));

                    client.prepare(contact, true);

                    System.out.println("SimpleHubClient - client ready : " + contact.getId());

                    clientList.add(client);
                }
            }).start();
        }

        while (sSpinning) {
            try {
                Thread.sleep(1000);
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
