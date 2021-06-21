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

package cube.client.listener;

import cube.client.CubeClient;
import cube.common.entity.Contact;
import cube.common.entity.Device;

/**
 * 联系人模块监听器。
 */
public interface ContactListener {

    /**
     * 当有联系人签入系统时回调该方法。
     *
     * @param client 客户端实例。
     * @param contact 签入的联系人。
     * @param device 签入的设备。
     */
    void onSignIn(CubeClient client, Contact contact, Device device);

    /**
     * 当有联系人签出系统时回调该方法。
     *
     * @param client 客户端实例。
     * @param contact 签出的联系人。
     * @param device 签出的设备。
     */
    void onSignOut(CubeClient client, Contact contact, Device device);

    /**
     * 当有联系人的设备超时时回调该方法。
     *
     * @param client 客户端实例。
     * @param contact 联系人。
     * @param device 超时的设备。
     */
    void onDeviceTimeout(CubeClient client, Contact contact, Device device);

}
