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

import cell.api.Speakable;
import cell.api.TalkListener;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.client.listener.MessageReceiveListener;
import cube.client.listener.MessageSendListener;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Group;
import cube.common.entity.Message;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据接收器。
 */
public class Receiver implements TalkListener {

    private final CubeClient client;

    private ConcurrentMap<Long, Notifier> notifiers;

    private Map<String, AtomicLong> receivingStreamMap;

    private ExecutorService executor;

    private Map<String, StreamListener> streamListenerMap;

    public Receiver(CubeClient client) {
        this.client = client;
        this.notifiers = new ConcurrentHashMap<>();
        this.receivingStreamMap = new ConcurrentHashMap<>();
        this.executor = Executors.newCachedThreadPool();
        this.streamListenerMap = new ConcurrentHashMap<>();
    }

    /**
     * 销毁接收器。
     */
    public void destroy() {
        long start = System.currentTimeMillis();
        while (!this.receivingStreamMap.isEmpty()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (System.currentTimeMillis() - start > 30000) {
                break;
            }
        }

        this.executor.shutdown();
    }

    /**
     * 指定流名称的文件是否存在。
     *
     * @param streamName
     * @return
     */
    public boolean existsStreamFile(String streamName, long size) {
        File file = new File(this.client.getFilePath(), streamName);
        return (file.exists() && file.length() == size);
    }

    /**
     * 是否正在接收指定的流。
     *
     * @param streamName
     * @return
     */
    public boolean isReceiving(String streamName) {
        return this.receivingStreamMap.containsKey(streamName);
    }

    /**
     * 是否正在接收数据。
     *
     * @return
     */
    public boolean isReceiving() {
        return !this.receivingStreamMap.isEmpty();
    }

    public void setStreamListener(String streamName, StreamListener listener) {
        if (null == listener) {
            this.streamListenerMap.remove(streamName);
            return;
        }

        this.streamListenerMap.put(streamName, listener);
    }

    /**
     * 注入新的通知器。
     *
     * @param notifier
     */
    public void inject(Notifier notifier) {
        this.notifiers.put(notifier.sn, notifier);
    }

    @Override
    public void onListened(Speakable speakable, String cellet, Primitive primitive) {
        if (CubeClient.NAME.equals(cellet)) {
            ActionDialect actionDialect = DialectFactory.getInstance().createActionDialect(primitive);

            if (actionDialect.containsParam(Notifier.ParamName)) {
                this.processNotifier(actionDialect);
            }
            else {
                String action = actionDialect.getName();

                if (Actions.NotifyEvent.name.equals(action)) {
                    this.processNotifyEvent(actionDialect);
                }
                else {
                    Logger.w(this.getClass(), "Unknown action: " + action);
                }
            }
        }
    }

    /**
     * 处理通知事件。
     *
     * @param actionDialect
     */
    private void processNotifyEvent(ActionDialect actionDialect) {
        String event = actionDialect.getParamAsString("event");
        if (Events.SignIn.name.equals(event)) {
            JSONObject data = actionDialect.getParamAsJson("data");
            this.client.contactListener.onSignIn(this.client, new Contact(data.getJSONObject("contact")),
                    new Device(data.getJSONObject("device")));
        }
        else if (Events.DeviceTimeout.name.equals(event)) {
            JSONObject data = actionDialect.getParamAsJson("data");
            this.client.contactListener.onDeviceTimeout(this.client, new Contact(data.getJSONObject("contact")),
                    new Device(data.getJSONObject("device")));
        }
        else if (Events.SignOut.name.equals(event)) {
            JSONObject data = actionDialect.getParamAsJson("data");
            this.client.contactListener.onSignOut(this.client, new Contact(data.getJSONObject("contact")),
                    new Device(data.getJSONObject("device")));
        }
        else if (Events.ReceiveMessage.name.equals(event)) {
            JSONObject data = actionDialect.getParamAsJson("data");
            if (data.has("contact")) {
                JSONObject contact = data.getJSONObject("contact");
                MessageReceiveListener listener = this.client.getMessageReceiveListener(
                        contact.getLong("id"), contact.getString("domain"));
                if (null != listener) {
                    listener.onReceived(new Message(data.getJSONObject("message")));
                }
            }
            else if (data.has("group")) {
                MessageReceiveListener listener = this.client.getMessageReceiveListener(new Group(data.getJSONObject("group")));
                if (null != listener) {
                    listener.onReceived(new Message(data.getJSONObject("message")));
                }
            }
        }
        else if (Events.SendMessage.name.equals(event)) {
            JSONObject data = actionDialect.getParamAsJson("data");
            if (data.has("contact")) {
                JSONObject contact = data.getJSONObject("contact");
                MessageSendListener listener = this.client.getMessageSendListener(
                        contact.getLong("id"), contact.getString("domain"));
                if (null != listener) {
                    listener.onSent(new Message(data.getJSONObject("message")));
                }
            }
        }
    }

    /**
     * 处理同步通知器。
     *
     * @param actionDialect
     */
    private void processNotifier(ActionDialect actionDialect) {
        JSONObject notifierJSON = actionDialect.getParamAsJson(Notifier.ParamName);
        Long sn = notifierJSON.getLong("sn");

        Notifier notifier = this.notifiers.remove(sn);
        if (null != notifier) {
            notifier.over(actionDialect);
        }
    }

    @Override
    public void onListened(Speakable speakable, String cellet, PrimitiveInputStream primitiveInputStream) {
        // 接收到文件流
        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#onListened - Input Stream : " + cellet + " - " + primitiveInputStream.getName());
        }

        this.receivingStreamMap.put(primitiveInputStream.getName(), new AtomicLong(System.currentTimeMillis()));

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                File targetFile = new File(client.getFilePath(), primitiveInputStream.getName());
                if (targetFile.exists()) {
                    targetFile.delete();
                }

                FileOutputStream fos = null;
                byte[] bytes = new byte[4096];
                int length = 0;
                try {
                    fos = new FileOutputStream(targetFile);

                    while (((length = primitiveInputStream.read(bytes)) > 0)) {
                        fos.write(bytes, 0, length);
                    }

                    primitiveInputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (null != fos) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                        }
                    }
                }

                receivingStreamMap.remove(primitiveInputStream.getName());

                StreamListener listener = streamListenerMap.get(primitiveInputStream.getName());
                if (null != listener) {
                    listener.onCompleted(primitiveInputStream.getName(), targetFile);
                }
            }
        });
    }

    @Override
    public void onSpoke(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onAck(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onContacted(Speakable speakable) {
        ActionDialect actionDialect = new ActionDialect(Actions.LOGIN.name);
        actionDialect.addParam("id", this.client.getId().longValue());
        actionDialect.addParam("name", this.client.getName());
        actionDialect.addParam("password", this.client.getPassword());
        speakable.speak(CubeClient.NAME, actionDialect);
    }

    @Override
    public void onQuitted(Speakable speakable) {
        this.client.interrupt();
    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {
        // Nothing
    }
}
