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

package cube.client.robot;

import cell.api.Speakable;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.client.Client;
import cube.client.Connector;
import cube.client.Receiver;
import cube.robot.RobotAction;
import cube.robot.RobotStateCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Robot 服务控制器。
 */
public class RobotController {

    public final static String NAME = "Robot";

    private Client client;

    private Connector connector;

    private Receiver receiver;

    private List<RobotReportListener> reportListeners;

    public RobotController(Client client) {
        this.client = client;
        this.reportListeners = new ArrayList<>();
    }

    public void prepare(Connector connector, Receiver receiver) {
        this.connector = connector;
        this.receiver = receiver;
    }

    public boolean register(RobotReportListener listener) {
        ActionDialect dialect = new ActionDialect(RobotAction.RegisterListener.name);
        dialect.addParam("name", "Report");

        ActionDialect response = this.connector.synSend(this.receiver.inject(), NAME, dialect);
        if (null == response) {
            return false;
        }

        int stateCode = response.getParamAsInt("code");
        if (stateCode != RobotStateCode.Ok.code) {
            Logger.w(this.getClass(), "#register - State error : " + stateCode);
            return false;
        }

        synchronized (this.reportListeners) {
            if (!this.reportListeners.contains(listener)) {
                this.reportListeners.add(listener);
            }
        }
        return true;
    }

    public boolean deregister(RobotReportListener listener) {
        ActionDialect dialect = new ActionDialect(RobotAction.DeregisterListener.name);
        dialect.addParam("name", "Report");

        ActionDialect response = this.connector.synSend(this.receiver.inject(), NAME, dialect);
        if (null == response) {
            return false;
        }

        int stateCode = response.getParamAsInt("code");
        if (stateCode != RobotStateCode.Ok.code) {
            Logger.w(this.getClass(), "#deregister - State error : " + stateCode);
            return false;
        }

        synchronized (this.reportListeners) {
            this.reportListeners.remove(listener);
        }
        return true;
    }

    public boolean processAction(ActionDialect actionDialect, Speakable speaker) {
        return false;
    }
}
