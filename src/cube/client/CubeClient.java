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

import cell.core.net.Endpoint;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.client.listener.ContactListener;
import cube.client.listener.MessageReceiveListener;
import cube.client.tool.MessageIterator;
import cube.client.tool.MessageReceiver;
import cube.common.entity.*;
import cube.common.state.MessagingStateCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 服务器客户端程序。
 */
public final class CubeClient {

    public final static String VERSION = "1.0.0";

    public final static String NAME = "Client";

    private Long id;

    private Connector connector;

    private Receiver receiver;

    private Timer timer;

    protected ContactListener contactListener;

    protected ConcurrentMap<String, MessageReceiver> messageReceiverMap;

    /**
     * 构造函数。
     *
     * @param address 服务器地址。
     */
    public CubeClient(String address) {
        this(address, 6000);
    }

    /**
     * 构造函数。
     *
     * @param address 服务器地址。
     * @param port 服务器端口。
     */
    public CubeClient(String address, int port) {
        this.id = Utils.generateSerialNumber();

        this.messageReceiverMap = new ConcurrentHashMap<>();

        this.connector = new Connector(address, port);
        this.receiver = new Receiver(this);
        this.connector.setListener(this.receiver);

        try {
            this.connector.connect();
        } catch (Exception e) {
            Logger.w(this.getClass(), e.getMessage());
        }

        this.timer = new Timer();
        this.timer.schedule(new Daemon(), 5000, 10000);
    }

    /**
     * 获取客户端 ID 。
     *
     * @return 返回客户端 ID 。
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 销毁客户端。
     */
    public void destroy() {
        this.timer.cancel();
        this.timer = null;

        this.connector.disconnect();
        this.connector.destroy();
    }

    /**
     * 是否已就绪。
     *
     * @return 如果就绪返回 {@code true} 。
     */
    public boolean isReady() {
        return this.connector.isConnected();
    }

    /**
     * 注册联系人监听器。<b>仅对当前连接的服务器有效。</b>
     *
     * @param listener 联系人监听器。
     * @return 返回操作是否有效。
     */
    public boolean registerListener(ContactListener listener) {
        if (!this.connector.isConnected()) {
            return false;
        }

        this.contactListener = listener;

        ActionDialect actionDialect = new ActionDialect(Actions.AddEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SignIn.name);
        this.connector.send(actionDialect);

        actionDialect = new ActionDialect(Actions.AddEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SignOut.name);
        this.connector.send(actionDialect);

        actionDialect = new ActionDialect(Actions.AddEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.DeviceTimeout.name);
        this.connector.send(actionDialect);

        return true;
    }

    /**
     * 注销联系人监听器。<b>仅对当前连接的服务器有效。</b>
     *
     * @param listener 联系人监听器。
     * @return 返回操作是否有效。
     */
    public boolean deregisterListener(ContactListener listener) {
        if (!this.connector.isConnected()) {
            return false;
        }

        if (this.contactListener != listener) {
            return false;
        }

        this.contactListener = null;

        ActionDialect actionDialect = new ActionDialect(Actions.RemoveEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SignIn.name);
        this.connector.send(actionDialect);

        actionDialect = new ActionDialect(Actions.RemoveEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SignOut.name);
        this.connector.send(actionDialect);

        actionDialect = new ActionDialect(Actions.RemoveEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.DeviceTimeout.name);
        this.connector.send(actionDialect);

        return true;
    }

    /**
     * 注册监听指定联系人接收到的消息。
     *
     * @param contact 指定被监听的联系人。
     * @param listener 指定监听器。
     * @return 返回操作是否有效。
     */
    public boolean registerMessageReceiveListener(Contact contact, MessageReceiveListener listener) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageReceiver receiver = this.messageReceiverMap.get(contact.getUniqueKey());
        if (null != receiver) {
            receiver.listener = listener;
            return true;
        }

        receiver = new MessageReceiver(contact, listener);
        this.messageReceiverMap.put(contact.getUniqueKey(), receiver);

        ActionDialect actionDialect = new ActionDialect(Actions.AddEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.ReceiveMessage.name);

        JSONObject param = new JSONObject();
        param.put("domain", contact.getDomain().getName());
        param.put("contactId", contact.getId().longValue());
        actionDialect.addParam("param", param);

        this.connector.send(actionDialect);

        return true;
    }

    /**
     * 注销监听指定联系人接收消息的监听器。
     *
     * @param contact 指定被监听的联系人。
     * @return 返回操作是否有效。
     */
    public boolean deregisterMessageReceiveListener(Contact contact) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageReceiver receiver = this.messageReceiverMap.remove(contact.getUniqueKey());
        if (null == receiver) {
            return false;
        }

        ActionDialect actionDialect = new ActionDialect(Actions.RemoveEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.ReceiveMessage.name);

        JSONObject param = new JSONObject();
        param.put("domain", contact.getDomain().getName());
        param.put("contactId", contact.getId().longValue());
        actionDialect.addParam("param", param);

        this.connector.send(actionDialect);

        return true;
    }

    /**
     * 注册监听指定群组消息的监听器。
     *
     * @param group 指定被监听的群组。
     * @param listener 指定监听器。
     * @return 返回操作是否有效。
     */
    public boolean registerMessageReceiveListener(Group group, MessageReceiveListener listener) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageReceiver receiver = this.messageReceiverMap.get(group.getUniqueKey());
        if (null != receiver) {
            receiver.listener = listener;
            return true;
        }

        receiver = new MessageReceiver(group, listener);
        this.messageReceiverMap.put(group.getUniqueKey(), receiver);

        ActionDialect actionDialect = new ActionDialect(Actions.AddEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.ReceiveMessage.name);

        JSONObject param = new JSONObject();
        param.put("domain", group.getDomain().getName());
        param.put("groupId", group.getId().longValue());
        actionDialect.addParam("param", param);

        this.connector.send(actionDialect);

        return true;
    }

    /**
     * 注销监听指定群组消息的监听器。
     *
     * @param group 指定被监听的群组。
     * @return 返回操作是否有效。
     */
    public boolean deregisterMessageReceiveListener(Group group) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageReceiver receiver = this.messageReceiverMap.remove(group.getUniqueKey());
        if (null == receiver) {
            return false;
        }

        ActionDialect actionDialect = new ActionDialect(Actions.RemoveEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.ReceiveMessage.name);

        JSONObject param = new JSONObject();
        param.put("domain", group.getDomain().getName());
        param.put("groupId", group.getId().longValue());
        actionDialect.addParam("param", param);

        this.connector.send(actionDialect);

        return true;
    }

    /**
     * 创建域应用。
     *
     * @param domainName 指定域名称。
     * @param appKey 指定 App Key 。
     * @param appId 指定 App ID 。
     * @param mainEndpoint 指定主服务器连接点。
     * @param httpEndpoint 指定 HTTP 连接点。
     * @param httpsEndpoint 指定 HTTPS 连接点。
     * @return 返回是否执行了该操作。
     */
    public boolean createDomainApp(String domainName, String appKey, String appId, Endpoint mainEndpoint,
                                Endpoint httpEndpoint, Endpoint httpsEndpoint) {
        if (!this.connector.isConnected()) {
            return false;
        }

        ActionDialect actionDialect = new ActionDialect(Actions.CreateDomainApp.name);
        actionDialect.addParam("domainName", domainName);
        actionDialect.addParam("appKey", appKey);
        actionDialect.addParam("appId", appId);
        actionDialect.addParam("mainEndpoint", mainEndpoint.toJSON());
        actionDialect.addParam("httpEndpoint", httpEndpoint.toJSON());
        actionDialect.addParam("httpsEndpoint", httpsEndpoint.toJSON());

        this.connector.send(actionDialect);

        return true;
    }

    /**
     * 获取当前连接服务器上所有在线的联系人。
     *
     * @return 返回当前连接服务器上所有在线的联系人列表。
     */
    public List<Contact> getOnlineContacts() {
        if (!this.connector.isConnected()) {
            return new ArrayList<>();
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(Actions.ListOnlineContacts.name);

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(notifier, actionDialect);

        JSONObject data = result.getParamAsJson("data");
        JSONArray list = data.getJSONArray("contacts");

        ArrayList<Contact> resultList = new ArrayList<>(list.length());

        for (int i = 0; i < list.length(); ++i) {
            JSONObject json = list.getJSONObject(i);
            Contact contact = new Contact(json);
            resultList.add(contact);
        }

        return resultList;
    }

    /**
     * 创建联系人。
     *
     * @param domain 指定域。
     * @param id 指定联系人 ID 。
     * @param name 指定联系人名称。
     * @param context 指定联系人上下文数据。可以为 {@code null} 值。
     * @return 返回创建的联系人。操作失败时返回 {@code null} 。
     */
    public Contact createContact(String domain, Long id, String name, JSONObject context) {
        if (!this.connector.isConnected()) {
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(Actions.CreateContact.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("id", id.longValue());
        actionDialect.addParam("name", name);
        if (null != context) {
            actionDialect.addParam("context", context);
        }

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(notifier, actionDialect);

        JSONObject data = result.getParamAsJson("contact");

        Contact contact = new Contact(data);

        return contact;
    }

    /**
     * 更新联系人信息。
     *
     * @param domain 指定域。
     * @param contactId 指定联系人 ID 。
     * @param newName 指定联系人的新名称，设置为 {@code null} 值时不更新名称。
     * @param newContact 指定上下文数据，设置为 {@code null} 值时不更新上下文数据。
     * @return 返回联系人实例。操作失败时返回 {@code null} 值。
     */
    public Contact updateContact(String domain, Long contactId, String newName, JSONObject newContact) {
        if (!this.connector.isConnected()) {
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(Actions.UpdateContact.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("id", id.longValue());

        if (null != newName) {
            actionDialect.addParam("name", newName);
        }

        if (null != newContact) {
            actionDialect.addParam("context", newContact);
        }

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(notifier, actionDialect);

        JSONObject data = result.containsParam("contact") ? result.getParamAsJson("contact") : null;
        if (null != data) {
            Contact contact = new Contact(data);
            return contact;
        }
        else {
            return null;
        }
    }

    /**
     * 获取指定 ID 的联系人。
     *
     * @param domain 指定域名称。
     * @param id 指定联系人的 ID 。
     * @return 返回指定的联系人实例。
     */
    public Contact getContact(String domain, Long id) {
        if (!this.connector.isConnected()) {
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(Actions.GetContact.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("contactId", id.longValue());

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(notifier, actionDialect);

        JSONObject data = result.getParamAsJson("contact");

        Contact contact = new Contact(data);

        return contact;
    }

    /**
     * 获取指定 ID 的群组。
     *
     * @param domain 指定域名称。
     * @param id 指定群组的 ID 。
     * @return 返回指定的群组实例，如果没有找到该群组返回 {@code null} 值。
     */
    public Group getGroup(String domain, Long id) {
        if (!this.connector.isConnected()) {
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(Actions.GetGroup.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("groupId", id.longValue());

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(notifier, actionDialect);

        if (result.containsParam("group")) {
            JSONObject data = result.getParamAsJson("group");
            return new Group(data);
        }
        else {
            return null;
        }
    }

    /**
     * 使用伪装身份推送消息。
     *
     * @param receiver 指定消息接收者。
     * @param pretender 指定伪装的联系人。
     * @param payload 指定消息数据负载。
     * @return 如果消息被服务器处理返回 {@code true} 。
     */
    public boolean pushMessageWithPretender(Contact receiver, Contact pretender, JSONObject payload) {
        return this.pushMessageWithPretender(receiver, pretender,
                new Device("Client", "Cube Server Client " + VERSION), payload);
    }

    /**
     * 使用伪装身份推送消息。
     *
     * @param receiver 指定消息接收者。
     * @param pretender 指定伪装的联系人。
     * @param device 指定发送消息的设备。
     * @param payload 指定消息数据负载。
     * @return 如果消息被服务器处理返回 {@code true} 。
     */
    public boolean pushMessageWithPretender(Contact receiver, Contact pretender, Device device, JSONObject payload) {
        long timestamp = System.currentTimeMillis();
        // 创建消息
        Message message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                pretender.getId(), receiver.getId(), 0L,
                0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                device.toCompactJSON(), payload, null);

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(Actions.PushMessage.name);
        actionDialect.addParam("message", message.toJSON());
        actionDialect.addParam("pretender", pretender.toCompactJSON());
        actionDialect.addParam("device", device.toCompactJSON());

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(notifier, actionDialect);
        if (!result.containsParam("result")) {
            // 推送失败
            return false;
        }

        JSONObject pushResult = result.getParamAsJson("result");
        return pushResult.getInt("state") == MessagingStateCode.Ok.code;
    }

    /**
     * 查询与指定联系人相关的消息。
     *
     * @param beginning
     * @param contact
     * @return
     */
    public MessageIterator queryMessages(long beginning, Contact contact) {
        MessageIterator iterator = new MessageIterator(this.connector, this.receiver);
        iterator.prepare(beginning, contact);
        return iterator;
    }

    protected MessageReceiveListener getMessageReceiveListener(Contact contact) {
        MessageReceiver receiver = this.messageReceiverMap.get(contact.getUniqueKey());
        if (null == receiver) {
            return null;
        }

        return receiver.listener;
    }

    protected MessageReceiveListener getMessageReceiveListener(Group group) {
        MessageReceiver receiver = this.messageReceiverMap.get(group.getUniqueKey());
        if (null == receiver) {
            return null;
        }

        return receiver.listener;
    }

    /**
     * 守护任务。
     */
    protected class Daemon extends TimerTask {

        public Daemon() {
        }

        @Override
        public void run() {

        }
    }
}
