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

package cube.client;

import cell.api.*;
import cell.core.Version;
import cell.core.talk.PrimitiveOutputStream;
import cell.core.talk.dialect.ActionDialect;

/**
 * 与服务进行网络连接的连接器。
 */
public class Connector {

    private String address;

    private int port;

    private Nucleus nucleus;

    private Speakable speakable;

    /**
     * 构造函数。
     *
     * @param address 连接地址。
     * @param port 连接端口。
     */
    public Connector(String address, int port) {
        this.address = address;
        this.port = port;

        NucleusConfig config = new NucleusConfig();
        config.nucleusDevice = NucleusDevice.DESKTOP;

        this.nucleus = new Nucleus(config);
        System.out.println("Nucleus version " + Version.getNumbers());
    }

    /**
     * 连接服务器。
     */
    public void connect() {
        if (null != this.speakable) {
            return;
        }

        this.speakable = this.nucleus.getTalkService().call(this.address, this.port);
    }

    /**
     * 断开连接。
     */
    public void disconnect() {
        if (null != this.speakable) {
            this.nucleus.getTalkService().hangup(this.address, this.port, false);
            this.speakable = null;
        }
    }

    /**
     * 是否已经连接服务器。
     *
     * @return 如果已经连接服务器返回 {@code true} 。
     */
    public boolean isConnected() {
        return this.nucleus.getTalkService().isCalled(this.address, this.port);
    }

    public void setListener(TalkListener listener) {
        this.nucleus.getTalkService().setListener(Client.NAME, listener);
    }

    public void send(ActionDialect actionDialect) {
        this.nucleus.getTalkService().speak(Client.NAME, actionDialect);
    }

    public ActionDialect send(Notifier notifier, ActionDialect actionDialect) {
        // 增加数据字段
        actionDialect.addParam(Notifier.ParamName, notifier.toJSON());

        this.nucleus.getTalkService().speak(Client.NAME, actionDialect);

        // 阻塞等待结果
        return notifier.waiting();
    }

    public PrimitiveOutputStream sendStream(String streamName) {
        return this.nucleus.getTalkService().speakStream(Client.NAME, streamName);
    }

    public void destroy() {
        this.nucleus.destroy();
    }
}
