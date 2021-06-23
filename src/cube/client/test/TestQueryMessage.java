/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
import cube.client.tool.MessageIterator;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Message;
import org.json.JSONObject;

/**
 * 测试查询消息。
 */
public class TestQueryMessage {


    public static void main(String[] args) {

        CubeClient client = new CubeClient("127.0.0.1");

        Helper.sleepInSeconds(3);

        System.out.println("[TestQueryMessage] Query Message");

        Contact contact = client.getContact("shixincube.com", 100100L);

        long beginning = System.currentTimeMillis() - (60L * 60L * 1000L);

        int count = 0;

        MessageIterator iterator = client.queryMessages(beginning, contact);
        while (iterator.hasNext()) {
            ++count;

            Message message = iterator.next();
        }

        System.out.println("[TestQueryMessage] Number: " + count);

        System.out.println("** END ***");
        client.destroy();
    }
}
