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

package cube.client.hub;

import cell.api.Speakable;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.client.Client;
import cube.client.Connector;
import cube.client.Receiver;
import cube.common.entity.FileLabel;
import cube.hub.HubAction;
import cube.hub.HubStateCode;
import cube.hub.SignalBuilder;
import cube.hub.event.Event;
import cube.hub.signal.PassBySignal;
import cube.hub.signal.ReadySignal;
import cube.hub.signal.Signal;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 控制器。
 */
public class HubController {

    private final static HubController instance = new HubController();

    public final static String NAME = "Hub";

    private Client client;

    private Connector connector;

    private Receiver receiver;

    private List<HubSignalListener> signalListenerList;

    private HubController() {
        this.signalListenerList = new ArrayList<>();
    }

    public static HubController getInstance() {
        return HubController.instance;
    }

    public void prepare(Client client, Connector connector, Receiver receiver) {
        this.client = client;
        this.connector = connector;
        this.receiver = receiver;

        // 向服务器报告就绪
        this.sendSignal(new ReadySignal(client.getDescription()));
    }

    public void addListener(HubSignalListener listener) {
        if (this.signalListenerList.contains(listener)) {
            return;
        }

        this.signalListenerList.add(listener);
    }

    public void removeListener(HubSignalListener listener) {
        this.signalListenerList.remove(listener);
    }

    public boolean sendEvent(Event event) {
        // 设置客户端描述
        event.setDescription(this.client.getDescription());

        File file = event.getFile();
        if (null != file) {
            // 上传文件
            FileLabel fileLabel = this.client.getFileProcessor().checkWithUploadStrategy(file);
            if (null == fileLabel) {
                Logger.w(this.getClass(), "#sendEvent - Upload file failed : " + file.getName());
                return false;
            }

            // 设置文件标签
            event.setFileLabel(fileLabel);
        }

        ActionDialect actionDialect = new ActionDialect(HubAction.TriggerEvent.name);
        actionDialect.addParam("event", event.toJSON());

        ActionDialect response = this.connector.synSend(this.receiver.inject(), NAME, actionDialect);
        if (null == response) {
            Logger.e(this.getClass(), "#sendEvent - timeout: " + actionDialect.getName());
            return false;
        }
        int stateCode = response.getParamAsInt("code");
        if (HubStateCode.Ok.code != stateCode) {
            Logger.e(this.getClass(), "#sendEvent - state code: " + stateCode);
            return false;
        }

        return true;
    }

    public boolean sendSignal(Signal signal) {
        if (PassBySignal.NAME.equals(signal.getName())) {
            signal.setDescription(this.client.getDescription());
        }

        ActionDialect actionDialect = new ActionDialect(HubAction.TransmitSignal.name);
        actionDialect.addParam("signal", signal.toJSON());

        ActionDialect response = this.connector.synSend(this.receiver.inject(), NAME, actionDialect);
        int stateCode = response.getParamAsInt("code");
        if (HubStateCode.Ok.code != stateCode) {
            Logger.e(this.getClass(), "#sendSignal - state code: " + stateCode);
            return false;
        }

        return true;
    }

    public boolean processAction(ActionDialect actionDialect, Speakable speakable) {
        String action = actionDialect.getName();
        if (HubAction.TransmitSignal.name.equals(action)) {
            JSONObject data = actionDialect.getParamAsJson("signal");

            Signal signal = SignalBuilder.build(data);
            for (HubSignalListener listener : this.signalListenerList) {
                listener.onSignal(signal);
            }

            return true;
        }

        return false;
    }
}
