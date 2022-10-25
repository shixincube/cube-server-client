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
import cube.client.file.FileStorage;
import cube.common.entity.Contact;
import cube.common.entity.FileStoragePerformance;
import cube.common.entity.VisitTrace;
import cube.report.LogLine;

import java.util.List;

public class TestFileStorage {

    public static void testPerformance(FileStorage storage) {
        System.out.println("*** START testPerformance ***");

        FileStoragePerformance performance = storage.getStoragePerformance(50001001, "shixincube.com");
        if (null != performance) {
            System.out.println(performance.toJSON().toString(4));
        }
        else {
            System.err.println("No performance");
        }

        System.out.println("*** END testPerformance ***");
    }

    public static void testSearchVisitTraces(FileStorage storage) {
        System.out.println("*** START testSearchVisitTraces ***");

        long now = System.currentTimeMillis();
        long beginTime = now - 30L * 24 * 60 * 60 * 1000;
        long endTime = now;

        List<VisitTrace> visitTraceList = storage.searchVisitTraces(50001001, "shixincube.com", beginTime, endTime);

        System.out.println("expended time: " + Math.round(System.currentTimeMillis() - now) / 1000.0f + "s");
        System.out.println("size: " + visitTraceList.size());

        System.out.println("*** END testSearchVisitTraces ***");
    }

    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", "admin", "shixincube.com");

        if (!client.waitReady()) {
            client.destroy();
            return;
        }

        Contact contact = new Contact(10000, "shixincube.com");
        client.prepare(contact);

//        testPerformance(client.getFileStorage());

        testSearchVisitTraces(client.getFileStorage());

        client.destroy();
    }
}
