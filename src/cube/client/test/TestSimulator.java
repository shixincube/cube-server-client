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

import cube.client.CubeClient;
import cube.client.simulator.ConversationSimulator;
import cube.common.entity.Contact;

/**
 * 测试模拟器。
 */
public class TestSimulator {

    public static void main(String[] args) {

        CubeClient client = new CubeClient("127.0.0.1", "admin", "shixincube.com");

        Contact self = new Contact(50001001L, "shixincube.com", "Self");
        Contact partner = new Contact(11444455L, "shixincube.com", "Partner");

        // 创建模拟器
        ConversationSimulator simulator = new ConversationSimulator(client, self, partner);

        // 在控制台中运行模拟器，可以输入交互命令
        simulator.runInConsole("exit");

        Helper.sleepInSeconds(1);
        System.out.println("*** END ***");
        client.destroy();
    }
}
