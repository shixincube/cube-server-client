/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cube.client.robot.RobotReportListener;
import cube.common.entity.Contact;
import cube.robot.Report;
import cube.robot.TaskNames;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestRobotController {

    public static void testRegisterListener(Client client) {
        System.out.println("*** START testRegisterListener ***");

        RobotReportListener listener = new RobotReportListener() {
            @Override
            public void onReport(Report report) {

            }
        };

        boolean result = client.getRobotController().register(listener);
        System.out.println("Register result : " + result);

        result = client.getRobotController().deregister(listener);
        System.out.println("Deregister result : " + result);

        System.out.println("*** END ***");
    }

    public static void testListener(Client client) {
        System.out.println("*** START testListener ***");

        AtomicBoolean received = new AtomicBoolean(false);

        RobotReportListener listener = new RobotReportListener() {
            @Override
            public void onReport(Report report) {
                received.set(true);

                System.out.println("Report:\n" + report.toJSON().toString(4));
            }
        };

        boolean result = client.getRobotController().register(listener);
        if (result) {
            int count = 0;
            while (!received.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ++count;
                if (count % 10 == 0) {
                    System.out.println("waiting report");
                }
                else if (count > 120) {
                    System.out.println("wait report timeout");
                    break;
                }
            }
        }
        else {
            System.out.println("register failed");
        }

        client.getRobotController().deregister(listener);

        System.out.println("*** END ***");
    }

    public static void testFulfill(Client client) {
        System.out.println("*** START testFulfill ***");

        boolean success = client.getRobotController().fulfill(TaskNames.ReportDouYinAccountData);
        System.out.println("Fulfill " + TaskNames.ReportDouYinAccountData + " - " + success);

        System.out.println("*** END ***");
    }

    public static void main(String[] args) {
        // 111.203.186.243
        Client client = new Client("127.0.0.1", "admin", "shixincube.com");

        if (!client.waitReady()) {
            client.destroy();
            return;
        }

        Contact contact = new Contact(10000, "shixincube.com");
        client.prepare(contact);

//        testRegisterListener(client);

//        testListener(client);

        testFulfill(client);

        client.destroy();
    }
}
