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

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.client.Client;
import cube.client.Connector;
import cube.client.Receiver;
import cube.hub.Event;
import cube.hub.HubAction;
import cube.hub.HubStateCode;
import org.json.JSONObject;

import java.io.File;

/**
 * 控制器。
 */
public class HubController {

    private final static HubController instance = new HubController();

    public final static String NAME = "Hub";

    private Client client;

    private Connector connector;

    private Receiver receiver;

    private HubController() {
    }

    public final static HubController getInstance() {
        return HubController.instance;
    }

    public void prepare(Client client, Connector connector, Receiver receiver) {
        this.client = client;
        this.connector = connector;
        this.receiver = receiver;
    }

    public boolean sendEvent(Event event) {
        JSONObject payload = new JSONObject();
        payload.put("client", this.client.getDescription().toCompactJSON());
        payload.put("event", event.toJSON());

        File file = event.getFile();
        if (null != file) {
            // 上传文件

        }

        ActionDialect actionDialect = new ActionDialect(HubAction.TriggerEvent.name);
        actionDialect.addParam("data", payload);

        ActionDialect response = this.connector.synSend(this.receiver.inject(), NAME, actionDialect);
        int stateCode = response.getParamAsInt("code");
        if (HubStateCode.Ok.code != stateCode) {
            Logger.e(this.getClass(), "#sendEvent - state code: " + stateCode);
            return false;
        }


        return true;
    }

}
