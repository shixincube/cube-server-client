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
import cube.client.aigc.AIGCController;
import cube.common.entity.Contact;

import java.util.List;

/**
 * 测试 AIGC 。
 */
public class TestAIGC {


    public static void testServiceInfo(Client client) {
        System.out.println("*** testServiceInfo ***");

        AIGCController.ServiceInfo info = client.getAIGCController().getServiceInfo();
        if (null == info) {
            System.out.println("[Error] return null");
            return;
        }

        System.out.println(info.toString());
    }


    public static void main(String[] args) {
        System.out.println("*** START ***");
        Client client = new Client("127.0.0.1", "baize", "shixincube.com");

        if (!client.waitReady()) {
            System.out.println("[Error] client error");
            return;
        }

        testServiceInfo(client);

        client.destroy();
        System.out.println("*** END ***");
    }
}
