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
import cube.client.StreamListener;
import cube.robot.Report;
import cube.robot.RobotAction;
import cube.robot.RobotStateCode;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Robot 服务的控制器。
 */
public class RobotController {

    public final static String NAME = "Robot";

    private final static String EVENT_REPORT = "Report";

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

    /**
     * 注册报告监听器。
     *
     * @param listener 指定监听器。
     * @return 如果注册成功返回 <code>true</code> ，否则返回 <code>false</code> 。
     */
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

    /**
     * 注销报告监听器。
     *
     * @param listener 指定监听器。
     * @return 如果注销成功返回 <code>true</code> ，否则返回 <code>false</code> 。
     */
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

    /**
     * 向机器人下发执行任务请求。机器人实时根据任务配置执行任务。
     *
     * @param taskName 指定任务名称。
     * @return 机器人接受到执行指令返回 <code>true</code> 。
     */
    public boolean fulfill(String taskName) {
        return this.fulfill(taskName, null);
    }

    /**
     * 向机器人下发执行任务请求。机器人实时根据任务配置执行任务。
     *
     * @param taskName 指定任务名称。
     * @param parameter 指定任务参数。
     * @return 机器人接受到执行指令返回 <code>true</code> 。
     */
    public boolean fulfill(String taskName, JSONObject parameter) {
        ActionDialect dialect = new ActionDialect(RobotAction.Fulfill.name);
        dialect.addParam("name", taskName);
        if (null != parameter) {
            dialect.addParam("parameter", parameter);
        }

        ActionDialect response = this.connector.synSend(this.receiver.inject(), NAME, dialect);
        if (null == response) {
            return false;
        }

        int stateCode = response.getParamAsInt("code");
        if (stateCode != RobotStateCode.Ok.code) {
            Logger.w(this.getClass(), "#fulfill - State error : " + stateCode);
            return false;
        }

        return true;
    }

    /**
     * 下载报告文件。
     * 该函数以阻塞方式执行，直到文件下载完成或者超时才返回。
     *
     * @param filename 指定任务报告名。
     * @return 返回下载成功保存在本地的文件。
     */
    public File downloadReportFile(String filename) {
        StringBuilder filePath = new StringBuilder();

        StreamListener streamListener = new StreamListener() {
            @Override
            public void onStarted(String streamName) {
            }

            @Override
            public void onCompleted(String streamName, File streamFile) {
                synchronized (filePath) {
                    filePath.append(streamFile.getAbsolutePath());

                    filePath.notify();
                }
            }
        };

        this.receiver.setStreamListener(filename, streamListener);

        ActionDialect actionDialect = new ActionDialect(RobotAction.GetReportFile.name);
        actionDialect.addParam("filename", filename);
        this.connector.send(NAME, actionDialect);

        synchronized (filePath) {
            try {
                filePath.wait(2 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (filePath.length() > 0) {
            return new File(filePath.toString());
        }
        else {
            return null;
        }
    }

    public boolean processAction(ActionDialect actionDialect, Speakable speaker) {
        String action = actionDialect.getName();

        if (RobotAction.Event.name.equals(action)) {
            // 事件名
            String name = actionDialect.getParamAsString("name");
            if (EVENT_REPORT.equals(name)) {
                JSONObject data = actionDialect.getParamAsJson("data");
                Report report = new Report(data);
                synchronized (this.reportListeners) {
                    for (RobotReportListener listener : this.reportListeners) {
                        listener.onReport(report);
                    }
                }
            }

            return true;
        }

        return false;
    }
}
