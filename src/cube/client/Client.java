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

import cell.core.net.Endpoint;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.client.file.FileProcessor;
import cube.client.file.FileUploader;
import cube.client.hub.HubController;
import cube.client.listener.ContactListener;
import cube.client.listener.FileUploadListener;
import cube.client.listener.MessageReceiveListener;
import cube.client.listener.MessageSendListener;
import cube.client.tool.MessageIterator;
import cube.client.tool.MessageReceiveEvent;
import cube.client.tool.MessageSendEvent;
import cube.client.tool.TokenTools;
import cube.client.util.*;
import cube.common.UniqueKey;
import cube.common.action.ClientAction;
import cube.common.action.ContactAction;
import cube.common.entity.*;
import cube.common.state.AuthStateCode;
import cube.common.state.FileStorageStateCode;
import cube.common.state.MessagingStateCode;
import cube.report.LogLine;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ???????????????????????????
 */
public final class Client {

    public final static String VERSION = "1.0.1";

    public final static String NAME = "Client";

    private ClientDescription description;

    private Long id;

    private long sessionId;

    private Connector connector;

    private Receiver receiver;

    private FileUploader uploader;

    private FileProcessor processor;

    private HubController hubController;

    private Timer timer;

    private boolean interrupted;

    private Contact pretender;

    protected ContactListener contactListener;

    protected ConcurrentMap<String, MessageReceiveEvent> messageReceiveEventMap;

    protected ConcurrentMap<String, MessageSendEvent> messageSendEventMap;

    protected File filePath;

    /**
     * ???????????????
     *
     * @param address ??????????????????
     */
    public Client(String address, String name, String password) {
        this(address, 6000, name, password);
    }

    /**
     * ???????????????
     *
     * @param address ??????????????????
     * @param port ??????????????????
     */
    public Client(String address, int port, String name, String password) {
        this.id = Utils.generateSerialNumber();

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes(Charset.forName("UTF-8")));
            byte[] hashMD5 = md5.digest();
            String passwordMD5 = FileUtils.bytesToHexString(hashMD5).toLowerCase();

            this.description = new ClientDescription(name, passwordMD5);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.messageReceiveEventMap = new ConcurrentHashMap<>();
        this.messageSendEventMap = new ConcurrentHashMap<>();

        this.filePath = new File("data/");
        if (!this.filePath.exists()) {
            this.filePath.mkdirs();
        }

        this.connector = new Connector(address, port);
        this.receiver = new Receiver(this);

        // ??????
        this.connector.setListener(this.receiver);

        try {
            this.connector.connect();
        } catch (Exception e) {
            Logger.w(this.getClass(), e.getMessage());
        }

        this.timer = new Timer();
        this.timer.schedule(new Daemon(), 5000, 10000);

        this.interrupted = false;
    }

    /**
     * ??????????????? ID ???
     *
     * @return ??????????????? ID ???
     */
    public Long getId() {
        return this.id;
    }

    /**
     * ???????????????????????????
     *
     * @return ???????????????????????????
     */
    public String getName() {
        return this.description.getName();
    }

    /**
     * ????????????????????? HASH ??????
     *
     * @return ????????????????????? HASH ??????
     */
    public String getPassword() {
        return this.description.getPassword();
    }

    /**
     * ??????????????????????????????
     *
     * @return ??????????????????????????????
     */
    public ClientDescription getDescription() {
        return this.description;
    }

    protected void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * ??????????????????
     */
    public synchronized void destroy() {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        if (null != this.receiver) {
            this.receiver.destroy();
            this.receiver = null;
        }

        if (null != this.connector) {
            this.connector.disconnect();
            this.connector.destroy();
            this.connector = null;
        }
    }

    /**
     * ??????????????????
     */
    public void interrupt() {
        synchronized (this) {
            if (this.interrupted) {
                return;
            }

            this.interrupted = true;
        }

        Logger.w(Client.class, "Connection has disconnected");
    }

    public void connected() {
        synchronized (this) {
            if (this.interrupted) {
                Logger.i(Client.class, "Connection has reconnected");
            }

            this.interrupted = false;
        }
    }

    /**
     * ???????????????????????????
     *
     * @return
     */
    public File getFilePath() {
        return this.filePath;
    }

    /**
     * ??????????????????
     *
     * @return ?????????????????? {@code true} ???
     */
    public boolean isReady() {
        return (null != this.connector && this.connector.isConnected());
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    public boolean waitReady() {
        Logger.d(this.getClass(), "Waiting client ready");

        boolean result = true;

        long time = System.currentTimeMillis();

        while (null != this.connector && !this.connector.isConnected()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (System.currentTimeMillis() - time > 10000) {
                result = false;
                break;
            }
        }

        if (null == this.connector) {
            Logger.d(this.getClass(), "Interrupted");
            return false;
        }

        Logger.d(this.getClass(), "Ready state : " + result);

        return result;
    }

    /**
     * ???????????????
     *
     * @param pretender
     */
    public void prepare(Contact pretender) {
        this.pretender = pretender;
        this.description.setPretender(pretender);
    }

    /**
     * ???????????????
     *
     * @param pretender
     * @param hubEnabled ???????????? HUB ?????????
     */
    public void prepare(Contact pretender, boolean hubEnabled) {
        this.prepare(pretender);

        if (hubEnabled) {
            // ?????? HUB ?????????
            if (null == this.hubController) {
                this.hubController = new HubController(this);
            }
            this.hubController.prepare(this.connector, this.receiver);
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    public Contact getPretender() {
        return this.pretender;
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    public List<LogLine> getServerLogs() {
        ActionDialect actionDialect = new ActionDialect(ClientAction.GetLog.name);
        actionDialect.addParam("limit", 100);

        ActionDialect response = this.connector.send(this.receiver.inject(), actionDialect);
        if (response.getParamAsInt("code") != AuthStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getServerLogs - State error : " + response.getParamAsInt("code"));
            return null;
        }

        JSONObject data = response.getParamAsJson("data");
        JSONArray list = data.getJSONArray("logs");

        ArrayList<LogLine> logLines = new ArrayList<>(list.length());
        for (int i = 0; i < list.length(); ++i) {
            logLines.add(new LogLine(list.getJSONObject(i)));
        }
        return logLines;
    }

    /**
     * ????????????????????????
     *
     * @return ????????????????????????
     */
    public FileUploader getFileUploader() {
        if (null == this.uploader) {
            this.uploader = new FileUploader(this.connector);
        }

        return this.uploader;
    }

    /**
     * ????????????????????????
     *
     * @return ????????????????????????
     */
    public FileProcessor getFileProcessor() {
        if (null == this.processor) {
            this.processor = new FileProcessor(this.filePath, this.connector, this.receiver);
        }

        if (null != this.pretender) {
            this.processor.setContactId(this.pretender.getId());
            this.processor.setDomainName(this.pretender.getDomain().getName());
        }

        return this.processor;
    }

    /**
     * ?????? Hub ????????????
     *
     * @return
     */
    public HubController getHubController() {
        if (null == this.hubController) {
            this.hubController = new HubController(this);
        }

        return this.hubController;
    }

    /**
     * ???????????????????????????<b>???????????????????????????????????????</b>
     *
     * @param listener ?????????????????????
     * @return ???????????????????????????
     */
    public boolean registerListener(ContactListener listener) {
        if (!this.connector.isConnected()) {
            return false;
        }

        this.contactListener = listener;

        ActionDialect actionDialect = new ActionDialect(ClientAction.AddEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SignIn.name);
        this.connector.send(actionDialect);

        actionDialect = new ActionDialect(ClientAction.AddEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SignOut.name);
        this.connector.send(actionDialect);

        actionDialect = new ActionDialect(ClientAction.AddEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.DeviceTimeout.name);
        this.connector.send(actionDialect);

        return true;
    }

    /**
     * ???????????????????????????<b>???????????????????????????????????????</b>
     *
     * @param listener ?????????????????????
     * @return ???????????????????????????
     */
    public boolean deregisterListener(ContactListener listener) {
        if (!this.connector.isConnected()) {
            return false;
        }

        if (this.contactListener != listener) {
            return false;
        }

        this.contactListener = null;

        ActionDialect actionDialect = new ActionDialect(ClientAction.RemoveEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SignIn.name);
        this.connector.send(actionDialect);

        actionDialect = new ActionDialect(ClientAction.RemoveEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SignOut.name);
        this.connector.send(actionDialect);

        actionDialect = new ActionDialect(ClientAction.RemoveEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.DeviceTimeout.name);
        this.connector.send(actionDialect);

        return true;
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param contact ??????????????????????????????
     * @param listener ??????????????????
     * @return ???????????????????????????
     */
    public boolean registerMessageReceiveListener(Contact contact, MessageReceiveListener listener) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageReceiveEvent event = this.messageReceiveEventMap.get(contact.getUniqueKey());
        if (null != event) {
            event.listener = listener;
            return true;
        }

        event = new MessageReceiveEvent(contact, listener);
        this.messageReceiveEventMap.put(contact.getUniqueKey(), event);

        ActionDialect actionDialect = new ActionDialect(ClientAction.AddEventListener.name);
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
     * ??????????????????????????????????????????????????????
     *
     * @param contact ??????????????????????????????
     * @return ???????????????????????????
     */
    public boolean deregisterMessageReceiveListener(Contact contact) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageReceiveEvent event = this.messageReceiveEventMap.remove(contact.getUniqueKey());
        if (null == event) {
            return false;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.RemoveEventListener.name);
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
     * ?????????????????????????????????????????????
     *
     * @param group ???????????????????????????
     * @param listener ??????????????????
     * @return ???????????????????????????
     */
    public boolean registerMessageReceiveListener(Group group, MessageReceiveListener listener) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageReceiveEvent event = this.messageReceiveEventMap.get(group.getUniqueKey());
        if (null != event) {
            event.listener = listener;
            return true;
        }

        event = new MessageReceiveEvent(group, listener);
        this.messageReceiveEventMap.put(group.getUniqueKey(), event);

        ActionDialect actionDialect = new ActionDialect(ClientAction.AddEventListener.name);
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
     * ?????????????????????????????????????????????
     *
     * @param group ???????????????????????????
     * @return ???????????????????????????
     */
    public boolean deregisterMessageReceiveListener(Group group) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageReceiveEvent event = this.messageReceiveEventMap.remove(group.getUniqueKey());
        if (null == event) {
            return false;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.RemoveEventListener.name);
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
     * ?????????????????????????????????????????????
     *
     * @param contact ??????????????????????????????
     * @param listener ??????????????????
     * @return ???????????????????????????
     */
    public boolean registerMessageSendListener(Contact contact, MessageSendListener listener) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageSendEvent event = this.messageSendEventMap.get(contact.getUniqueKey());
        if (null != event) {
            event.listener = listener;
            return true;
        }

        event = new MessageSendEvent(contact, listener);
        this.messageSendEventMap.put(contact.getUniqueKey(), event);

        ActionDialect actionDialect = new ActionDialect(ClientAction.AddEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SendMessage.name);

        JSONObject param = new JSONObject();
        param.put("domain", contact.getDomain().getName());
        param.put("contactId", contact.getId().longValue());
        actionDialect.addParam("param", param);

        this.connector.send(actionDialect);

        return true;
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param contact ??????????????????????????????
     * @return ???????????????????????????
     */
    public boolean deregisterMessageSendListener(Contact contact) {
        if (!this.connector.isConnected()) {
            return false;
        }

        MessageSendEvent event = this.messageSendEventMap.remove(contact.getUniqueKey());
        if (null == event) {
            return false;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.RemoveEventListener.name);
        actionDialect.addParam("id", this.id.longValue());
        actionDialect.addParam("event", Events.SendMessage.name);

        JSONObject param = new JSONObject();
        param.put("domain", contact.getDomain().getName());
        param.put("contactId", contact.getId().longValue());
        actionDialect.addParam("param", param);

        this.connector.send(actionDialect);

        return true;
    }

    /**
     * ????????????????????????
     *
     * @param domainName ????????????
     * @param appKey ??? App Key ???
     * @return ????????????????????????
     */
    public AuthDomain getDomain(String domainName, String appKey) {
        if (!this.connector.isConnected()) {
            return null;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.GetDomain.name);
        actionDialect.addParam("domain", domainName);
        actionDialect.addParam("appKey", appKey);

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        JSONObject domainJson = result.getParamAsJson("authDomain");
        if (null == domainJson) {
            return null;
        }

        return new AuthDomain(domainJson);
    }

    /**
     * ??????????????????
     *
     * @param domainName ??????????????????
     * @param appKey ?????? App Key ???
     * @param appId ?????? App ID ???
     * @param mainEndpoint ??????????????????????????????
     * @param httpEndpoint ?????? HTTP ????????????
     * @param httpsEndpoint ?????? HTTPS ????????????
     * @param ferry ???????????????????????????
     * @return ????????????????????????
     */
    public AuthDomain createDomainApp(String domainName, String appKey, String appId, Endpoint mainEndpoint,
                                Endpoint httpEndpoint, Endpoint httpsEndpoint, boolean ferry) {
        if (!this.connector.isConnected()) {
            return null;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.CreateDomainApp.name);
        actionDialect.addParam("domainName", domainName);
        actionDialect.addParam("appKey", appKey);
        actionDialect.addParam("appId", appId);
        actionDialect.addParam("mainEndpoint", mainEndpoint.toJSON());
        actionDialect.addParam("httpEndpoint", httpEndpoint.toJSON());
        actionDialect.addParam("httpsEndpoint", httpsEndpoint.toJSON());
        actionDialect.addParam("ferry", ferry);

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        JSONObject authDomainJSON = result.getParamAsJson("authDomain");
        AuthDomain authDomain = new AuthDomain(authDomainJSON);

        return authDomain;
    }

    /**
     * ???????????????????????????
     *
     * @param domainName ??????????????????
     * @param mainEndpoint ??????????????????????????????
     * @param httpEndpoint ?????? HTTP ????????????
     * @param httpsEndpoint ?????? HTTPS ????????????
     * @return ????????????????????????
     */
    public AuthDomain updateDomainInfo(String domainName, Endpoint mainEndpoint,
                                 Endpoint httpEndpoint, Endpoint httpsEndpoint) {
        if (!this.connector.isConnected()) {
            return null;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.UpdateDomain.name);
        actionDialect.addParam("domainName", domainName);
        actionDialect.addParam("mainEndpoint", mainEndpoint.toJSON());
        actionDialect.addParam("httpEndpoint", httpEndpoint.toJSON());
        actionDialect.addParam("httpsEndpoint", httpsEndpoint.toJSON());

        AuthDomain authDomain = null;

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);

        if (null != result && result.containsParam("authDomain")) {
            JSONObject authDomainJSON = result.getParamAsJson("authDomain");
            authDomain = new AuthDomain(authDomainJSON);
        }

        return authDomain;
    }

    /**
     * ?????????????????????
     *
     * @param domainName ??????????????????
     * @param appKey ?????? App Key ???
     * @param cid ??????????????? ID ???
     * @param duration ?????????????????????
     * @return ?????????????????????
     */
    public AuthToken applyToken(String domainName, String appKey, Long cid, long duration) {
        if (!this.connector.isConnected()) {
            return null;
        }

        Notifier notifier = this.receiver.inject();

        ActionDialect actionDialect = new ActionDialect(ClientAction.ApplyToken.name);
        actionDialect.addParam("domain", domainName);
        actionDialect.addParam("appKey", appKey);
        actionDialect.addParam("cid", cid.longValue());
        actionDialect.addParam("duration", duration);

        // ???????????????????????????
        ActionDialect result = this.connector.send(notifier, actionDialect);
        JSONObject tokenJson = result.getParamAsJson("token");
        AuthToken token = new AuthToken(tokenJson);

        return token;
    }

    /**
     * ?????????????????????????????????
     *
     * @param contactId
     * @return
     */
    public AuthToken getAuthToken(Long contactId) {
        return TokenTools.getAuthToken(this.connector, this.receiver, contactId);
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @return ???????????????????????????????????????????????????????????????
     */
    public List<Contact> getOnlineContacts() {
        if (!this.connector.isConnected()) {
            return new ArrayList<>();
        }

        Notifier notifier = this.receiver.inject();

        ActionDialect actionDialect = new ActionDialect(ClientAction.ListOnlineContacts.name);

        // ????????????????????????????????????
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
     * ??????????????????
     *
     * @param domain ????????????
     * @param id ??????????????? ID ???
     * @param name ????????????????????????
     * @param context ?????????????????????????????????????????? {@code null} ??????
     * @return ???????????????????????????????????????????????? {@code null} ???
     */
    public Contact createContact(String domain, Long id, String name, JSONObject context) {
        if (!this.connector.isConnected()) {
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(ClientAction.CreateContact.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("id", id.longValue());
        actionDialect.addParam("name", name);
        if (null != context) {
            actionDialect.addParam("context", context);
        }

        // ????????????????????????????????????
        ActionDialect result = this.connector.send(notifier, actionDialect);

        JSONObject data = result.getParamAsJson("contact");

        Contact contact = new Contact(data);

        return contact;
    }

    /**
     * ????????????????????????
     *
     * @param domain ????????????
     * @param contactId ??????????????? ID ???
     * @param newName ??????????????????????????????????????? {@code null} ????????????????????????
     * @param newContact ????????????????????????????????? {@code null} ?????????????????????????????????
     * @return ????????????????????????????????????????????? {@code null} ??????
     */
    public Contact updateContact(String domain, Long contactId, String newName, JSONObject newContact) {
        if (!this.connector.isConnected()) {
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(ClientAction.UpdateContact.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("id", contactId.longValue());

        if (null != newName) {
            actionDialect.addParam("name", newName);
        }

        if (null != newContact) {
            actionDialect.addParam("context", newContact);
        }

        // ????????????????????????????????????
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
     * ???????????? ID ???????????????
     *
     * @param domain ??????????????????
     * @param id ?????????????????? ID ???
     * @return ?????????????????????????????????
     */
    public Contact getContact(String domain, Long id) {
        if (!this.connector.isConnected()) {
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(ClientAction.GetContact.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("contactId", id.longValue());

        // ????????????????????????????????????
        ActionDialect result = this.connector.send(notifier, actionDialect);

        JSONObject data = result.getParamAsJson("contact");

        Contact contact = new Contact(data);

        return contact;
    }

    /**
     * ???????????? ID ????????????
     *
     * @param domain ??????????????????
     * @param id ??????????????? ID ???
     * @return ??????????????????????????????????????????????????????????????? {@code null} ??????
     */
    public Group getGroup(String domain, Long id) {
        if (!this.connector.isConnected()) {
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(ClientAction.GetGroup.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("groupId", id.longValue());

        // ????????????????????????????????????
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
     * ???????????????????????????????????????????????????
     *
     * @param contact
     * @param zoneName
     * @param participant
     * @return
     */
    public ContactZone addParticipantToZoneByForce(Contact contact, String zoneName, Contact participant) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.ModifyContactZone.name);
        actionDialect.addParam("domain", contact.getDomain().getName());
        actionDialect.addParam("contactId", contact.getId());
        actionDialect.addParam("zoneName", zoneName);
        actionDialect.addParam("action", ContactAction.AddParticipantToZone.name);
        ContactZoneParticipant zoneParticipant = new ContactZoneParticipant(participant.getId(),
                ContactZoneParticipantType.Contact, participant.getTimestamp(),
                contact.getId(), "Operated by administrator", ContactZoneParticipantState.Normal);
        actionDialect.addParam("participant", zoneParticipant.toJSON());

        Notifier notifier = new Notifier();
        this.receiver.inject(notifier);

        ActionDialect result = this.connector.send(notifier, actionDialect);
        if (result.containsParam("contactZone")) {
            return new ContactZone(result.getParamAsJson("contactZone"));
        }
        else {
            return null;
        }
    }

    public ContactZone removeParticipantFromZoneByForce(Contact contact, String zoneName, Contact participant) {
        // TODO
        return null;
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param contact
     * @param zoneName
     * @param participant
     * @param state
     * @return
     */
    public ContactZoneParticipant modifyParticipantByForce(Contact contact, String zoneName, Contact participant,
                                                           ContactZoneParticipantState state) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.ModifyContactZone.name);
        actionDialect.addParam("domain", contact.getDomain().getName());
        actionDialect.addParam("contactId", contact.getId());
        actionDialect.addParam("zoneName", zoneName);
        actionDialect.addParam("action", ContactAction.ModifyZoneParticipant.name);
        ContactZoneParticipant zoneParticipant = new ContactZoneParticipant(participant.getId(),
                ContactZoneParticipantType.Contact, participant.getTimestamp(),
                contact.getId(), "Operated by administrator", state);
        actionDialect.addParam("participant", zoneParticipant.toJSON());

        Notifier notifier = new Notifier();
        this.receiver.inject(notifier);

        ActionDialect result = this.connector.send(notifier, actionDialect);
        if (result.containsParam("participant")) {
            return new ContactZoneParticipant(result.getParamAsJson("participant"));
        }
        else {
            return null;
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param receiver
     * @param payload
     * @return
     */
    public boolean pushMessage(Contact receiver, JSONObject payload) {
        if (null == this.pretender) {
            Logger.e(this.getClass(), "#pushMessage - No pretender data");
            return false;
        }

        return this.pushMessageWithPretender(receiver, this.pretender, payload);
    }

    /**
     * ????????????????????????????????????
     *
     * @param receiver
     * @param payload
     * @param file
     * @return
     */
    public boolean pushMessage(Contact receiver, JSONObject payload, File file) {
        if (null == this.pretender) {
            Logger.e(this.getClass(), "#pushMessage - No pretender data");
            return false;
        }

        FileLabel fileLabel = null;
        if (null != file) {
            // ???????????????????????????
            fileLabel = this.putFileWithPretender(this.pretender, file);
            if (null == fileLabel) {
                Logger.d(Client.class, "#pushMessage - push file: \"" + file.getName() + "\" failed");
                return false;
            }
        }

        // ????????????
        long timestamp = System.currentTimeMillis();
        Device device = new Device("Client", "Cube Server Client " + VERSION);

        Message message = null;

        if (null != fileLabel) {
            FileAttachment fileAttachment = new FileAttachment(fileLabel);
            fileAttachment.setCompressed(true);

            message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                    this.pretender.getId(), receiver.getId(), 0L, 0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                    device.toCompactJSON(), payload, fileAttachment.toJSON());
        }
        else {
            message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                    this.pretender.getId(), receiver.getId(), 0L, 0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                    device.toCompactJSON(), payload, null);
        }

        // ????????????
        boolean result = this.pushMessage(message, this.pretender, device);
        return result;
    }

    /**
     * ?????????????????????????????????
     *
     * @param receiver ????????????????????????
     * @param pretender ???????????????????????????
     * @param payload ???????????????????????????
     * @return ???????????????????????????????????? {@code true} ???
     */
    public boolean pushMessageWithPretender(Contact receiver, Contact pretender, JSONObject payload) {
        return this.pushMessageWithPretender(receiver, pretender,
                new Device("Client", "Cube Server Client " + VERSION), payload);
    }

    /**
     * ?????????????????????????????????
     *
     * @param receiver ????????????????????????
     * @param pretender ???????????????????????????
     * @param device ??????????????????????????????
     * @param payload ???????????????????????????
     * @return ???????????????????????????????????? {@code true} ???
     */
    public boolean pushMessageWithPretender(Contact receiver, Contact pretender, Device device, JSONObject payload) {
        long timestamp = System.currentTimeMillis();
        // ????????????
        Message message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                pretender.getId(), receiver.getId(), 0L,
                0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                device.toCompactJSON(), payload, null);

        // ????????????
        return pushMessage(message, pretender, device);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param receiver ????????????????????????
     * @param pretender ???????????????????????????
     * @param file ???????????????
     * @return ???????????????????????????????????? {@code true} ???
     */
    public boolean pushFileMessageWithPretender(Contact receiver, Contact pretender, File file) {
        Logger.d(Client.class, "#pushFileMessageWithPretender - push file: \"" + file.getName() + "\" to \""
                + receiver.getId() + "\" from \"" + pretender.getId() + "\"");

        // ???????????????????????????
        FileLabel fileLabel = this.putFileWithPretender(pretender, file);
        if (null == fileLabel) {
            Logger.d(Client.class, "#pushFileMessageWithPretender - push file: \"" + file.getName() + "\" failed");
            return false;
        }

        // ????????????
        long timestamp = System.currentTimeMillis();
        Device device = new Device("Client", "Cube Server Client " + VERSION);

        JSONObject payload = new JSONObject();
        payload.put("type", "file");

        FileAttachment fileAttachment = new FileAttachment(fileLabel);

        Message message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                pretender.getId(), receiver.getId(), 0L, 0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                device.toCompactJSON(), payload, fileAttachment.toJSON());

        // ????????????
        boolean result = this.pushMessage(message, pretender, device);
        return result;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param receiver
     * @param pretender
     * @param file
     * @return
     */
    public boolean pushImageMessageWithPretender(Contact receiver, Contact pretender, File file) {
        Logger.d(Client.class, "#pushImageMessageWithPretender - push file: \"" + file.getName() + "\" to \""
                + receiver.getId() + "\" from \"" + pretender.getId() + "\"");

        // ???????????????????????????
        FileLabel fileLabel = this.putFileWithPretender(pretender, file);
        if (null == fileLabel) {
            Logger.d(Client.class, "#pushImageMessageWithPretender - push file: \"" + file.getName() + "\" failed");
            return false;
        }

        // ????????????
        long timestamp = System.currentTimeMillis();
        Device device = new Device("Client", "Cube Server Client " + VERSION);

        JSONObject payload = new JSONObject();
        payload.put("type", "image");

        FileAttachment fileAttachment = new FileAttachment(fileLabel);
        fileAttachment.setCompressed(true);

        Message message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                pretender.getId(), receiver.getId(), 0L, 0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                device.toCompactJSON(), payload, fileAttachment.toJSON());

        // ????????????
        boolean result = this.pushMessage(message, pretender, device);
        return result;
    }

    /**
     * ?????????????????????????????????
     *
     * @param pretender ???????????????????????????
     * @param file ???????????????
     * @return ???????????????????????????????????? {@code true} ???
     */
    private FileLabel putFileWithPretender(Contact pretender, File file) {
        MutableFileLabel mutableFileLabel = new MutableFileLabel();

        Promise.create(new PromiseHandler<FileUploader.UploadMeta>() {
            @Override
            public void emit(PromiseFuture<FileUploader.UploadMeta> promise) {
                // ??????????????????
                getFileUploader().upload(pretender.getId(), pretender.getDomain().getName(), file, new FileUploadListener() {
                    @Override
                    public void onUploading(FileUploader.UploadMeta meta, long processedSize) {
                        Logger.d(Client.class, "#putFileWithPretender - onUploading : " +
                                processedSize + "/" + meta.file.length());
                    }

                    @Override
                    public void onCompleted(FileUploader.UploadMeta meta) {
                        Logger.i(Client.class, "#putFileWithPretender - onCompleted : " + meta.fileCode);
                        promise.resolve(meta);
                    }

                    @Override
                    public void onFailed(FileUploader.UploadMeta meta, Throwable throwable) {
                        Logger.w(Client.class, "#putFileWithPretender - onFailed : " + file.getName(), throwable);
                        promise.reject(meta);
                    }
                });
            }
        }).then(new Future<FileUploader.UploadMeta>() {
            @Override
            public void come(FileUploader.UploadMeta meta) {
                // ????????????
                FileLabel fileLabel = putFileLabel(pretender, meta.fileCode, meta.file, meta.getMD5Code(), meta.getSHA1Code());
                if (null == fileLabel) {
                    // ??????????????????
                    synchronized (mutableFileLabel) {
                        mutableFileLabel.notify();
                    }
                    return;
                }

                // ?????????????????????????????????
                fileLabel = queryFileLabel(pretender.getDomain().getName(), fileLabel.getFileCode());
                if (null == fileLabel) {
                    synchronized (mutableFileLabel) {
                        mutableFileLabel.notify();
                    }
                    return;
                }

                // ??????
                mutableFileLabel.value = fileLabel;

                synchronized (mutableFileLabel) {
                    mutableFileLabel.notify();
                }
            }
        }).catchReject(new Future<FileUploader.UploadMeta>() {
            @Override
            public void come(FileUploader.UploadMeta meta) {
                synchronized (mutableFileLabel) {
                    mutableFileLabel.notify();
                }
            }
        }).launch();

        synchronized (mutableFileLabel) {
            try {
                mutableFileLabel.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return mutableFileLabel.value;
    }

    private boolean pushMessage(Message message, Contact pretender, Device device) {
        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(ClientAction.PushMessage.name);
        actionDialect.addParam("message", message.toJSON());
        actionDialect.addParam("pretender", pretender.toCompactJSON());
        actionDialect.addParam("device", device.toCompactJSON());

        // ????????????????????????????????????
        ActionDialect result = this.connector.send(notifier, actionDialect);
        if (!result.containsParam("result")) {
            // ????????????
            return false;
        }

        JSONObject pushResult = result.getParamAsJson("result");
        return pushResult.getInt("state") == MessagingStateCode.Ok.code;
    }

    /**
     * ???????????????
     *
     * @param domain
     * @param fileCode
     * @return
     */
    private FileLabel queryFileLabel(String domain, String fileCode) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.GetFile.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("fileCode", fileCode);

        // ????????????????????????????????????
        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.w(Client.class, "#queryFileLabel - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("fileLabel");
        return new FileLabel(data);
    }

    /**
     * ???????????????
     *
     * @param contact
     * @param fileCode
     * @param file
     * @param md5Code
     * @param sha1Code
     * @return
     */
    private FileLabel putFileLabel(Contact contact, String fileCode, File file, String md5Code, String sha1Code) {
        // ??????????????????
        FileType fileType = FileUtils.extractFileExtensionType(file.getName());

        FileLabel fileLabel = new FileLabel(contact.getDomain().getName(), fileCode,
                contact.getId(), file.getName(), file.length(), file.lastModified(), System.currentTimeMillis(), 0);
        fileLabel.setFileType(fileType);
        fileLabel.setMD5Code(md5Code);
        fileLabel.setSHA1Code(sha1Code);

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(ClientAction.PutFile.name);
        actionDialect.addParam("fileLabel", fileLabel.toJSON());

        // ????????????????????????????????????
        ActionDialect result = this.connector.send(notifier, actionDialect);

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.w(Client.class, "#putFileLabel - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("fileLabel");
        return new FileLabel(data);
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param beginning ???????????????????????????
     * @param contact ??????????????????????????????
     * @return ??????????????????????????????
     */
    public MessageIterator queryMessages(long beginning, Contact contact) {
        MessageIterator iterator = new MessageIterator(this.connector, this.receiver);
        iterator.prepare(beginning, contact);
        return iterator;
    }

    /**
     * ?????????????????????
     *
     * @param receiver ???????????????????????????
     * @param sender ???????????????????????????
     * @param messagesList ?????????????????????
     * @return ???????????????????????????????????????????????????????????????????????????????????????????????? {@code null} ??????
     */
    public List<Message> markRead(Contact receiver, Contact sender, List<Message> messagesList) {
        JSONArray idList = new JSONArray();
        for (Message message : messagesList) {
            if (message.getSource() > 0 || message.getState() != MessageState.Sent) {
                continue;
            }

            if (message.getTo().longValue() == receiver.getId().longValue()
                    && message.getFrom().longValue() == sender.getId().longValue()) {
                idList.put(message.getId().longValue());
            }
        }

        if (idList.length() == 0) {
            return null;
        }

        JSONObject data = new JSONObject();
        data.put("to", receiver.getId().longValue());
        data.put("from", sender.getId().longValue());
        data.put("list", idList);

        ActionDialect actionDialect = new ActionDialect(ClientAction.MarkReadMessages.name);
        actionDialect.addParam("domain", receiver.getDomain().getName());
        actionDialect.addParam("data", data);

        Notifier notifier = new Notifier();
        this.receiver.inject(notifier);
        ActionDialect response = this.connector.send(notifier, actionDialect);
        JSONObject result = response.getParamAsJson("result");
        if (result.has("messages")) {
            JSONArray messageArray = result.getJSONArray("messages");
            List<Message> list = new ArrayList<>();
            for (int i = 0; i < messageArray.length(); ++i) {
                Message message = new Message(messageArray.getJSONObject(i));
                list.add(message);
            }
            return list;
        }

        return null;
    }

    protected MessageReceiveListener getMessageReceiveListener(long contactId, String domain) {
        MessageReceiveEvent event = this.messageReceiveEventMap.get(UniqueKey.make(contactId, domain));
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    protected MessageReceiveListener getMessageReceiveListener(Contact contact) {
        MessageReceiveEvent event = this.messageReceiveEventMap.get(contact.getUniqueKey());
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    protected MessageReceiveListener getMessageReceiveListener(Group group) {
        MessageReceiveEvent event = this.messageReceiveEventMap.get(group.getUniqueKey());
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    protected MessageSendListener getMessageSendListener(long contactId, String domain) {
        MessageSendEvent event = this.messageSendEventMap.get(UniqueKey.make(contactId, domain));
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    protected MessageSendListener getMessageSendListener(Contact contact) {
        MessageSendEvent event = this.messageSendEventMap.get(contact.getUniqueKey());
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    /**
     * ???????????????
     */
    protected class Daemon extends TimerTask {

        public Daemon() {
        }

        @Override
        public void run() {

        }
    }
}
