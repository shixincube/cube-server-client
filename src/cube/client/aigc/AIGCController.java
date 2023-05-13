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

package cube.client.aigc;

import cell.core.talk.dialect.ActionDialect;
import cube.client.Client;
import cube.client.Connector;
import cube.client.Receiver;
import cube.common.action.ClientAction;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AIGC 服务控制器。
 */
public class AIGCController {

    public final static String NAME = "AIGC";

    private Client client;

    private Connector connector;

    private Receiver receiver;

    public AIGCController(Client client) {
        this.client = client;
    }

    public void prepare(Connector connector, Receiver receiver) {
        this.connector = connector;
        this.receiver = receiver;
    }

    public ServiceInfo getServiceInfo() {
        ActionDialect request = new ActionDialect(ClientAction.AIGCGetServiceInfo.name);
        ActionDialect response = this.connector.send(this.receiver.inject(), request);
        if (null == response) {
            return null;
        }

        JSONObject data = response.getParamAsJson("data");
        ServiceInfo info = new ServiceInfo(data.getJSONArray("unitList"), data.getJSONArray("channelList"));
        return info;
    }


    public class ServiceInfo {

        public final List<AIGCUnit> unitList;

        public final List<AIGCChannel> channelList;

        protected ServiceInfo(JSONArray unitArray, JSONArray channelArray) {
            this.unitList = new ArrayList<>();
            this.channelList = new ArrayList<>();

            for (int i = 0; i < unitArray.length(); ++i) {
                this.unitList.add(new AIGCUnit(unitArray.getJSONObject(i)));
            }
            for (int i = 0; i < channelArray.length(); ++i) {
                this.channelList.add(new AIGCChannel(channelArray.getJSONObject(i)));
            }
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("#Units\n");
            for (AIGCUnit unit : this.unitList) {
                buf.append(unit.toJSON().toString(4));
                buf.append("\n");
            }
            buf.append("#Channel\n");
            for (AIGCChannel channel : this.channelList) {
                buf.append(channel.toInfo().toString(4));
                buf.append("\n");
            }
            return buf.toString();
        }
    }
}
