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
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Message;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 数据接收器。
 */
public class Receiver implements TalkListener {

    private final CubeClient client;

    private ConcurrentMap<Long, Notifier> notifiers;

    public Receiver(CubeClient client) {
        this.client = client;
        this.notifiers = new ConcurrentHashMap<>();
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
                MessageReceiveListener listener = this.client.getMessageReceiveListener(new Contact(data.getJSONObject("contact")));
                if (null != listener) {
                    listener.onReceived(new Message(data.getJSONObject("message")));
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
        // Nothing
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
        speakable.speak(CubeClient.NAME, actionDialect);
    }

    @Override
    public void onQuitted(Speakable speakable) {
        // Nothing
    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {
        // Nothing
    }
}
