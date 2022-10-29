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
import cube.client.file.FileStorage;
import cube.client.file.FileUploader;
import cube.client.hub.HubController;
import cube.client.listener.ContactListener;
import cube.client.message.MessageService;
import cube.client.tool.TokenTools;
import cube.common.action.ClientAction;
import cube.common.action.ContactAction;
import cube.common.entity.*;
import cube.common.notice.ListContactBehaviors;
import cube.common.notice.NoticeData;
import cube.common.state.AuthStateCode;
import cube.common.state.RiskMgmtStateCode;
import cube.report.LogLine;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 服务器客户端程序。
 */
public class Client {

    public final static String VERSION = "1.0.1";

    public final static String NAME = "Client";

    private ClientDescription description;

    private Long id;

    private long sessionId;

    private Connector connector;

    private Receiver receiver;

    private FileUploader uploader;

    private FileProcessor processor;

    private FileStorage storage;

    private HubController hubController;

    private Timer timer;

    private boolean interrupted;

    private Contact pretender;

    protected ContactListener contactListener;

    protected MessageService messageService;

    protected File filePath;

    /**
     * 构造函数。
     *
     * @param address 服务器地址。
     */
    public Client(String address, String name, String password) {
        this(address, 6000, name, password);
    }

    /**
     * 构造函数。
     *
     * @param address 服务器地址。
     * @param port 服务器端口。
     */
    public Client(String address, int port, String name, String password) {
        this.id = Utils.generateSerialNumber();

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes(StandardCharsets.UTF_8));
            byte[] hashMD5 = md5.digest();
            String passwordMD5 = FileUtils.bytesToHexString(hashMD5).toLowerCase();

            this.description = new ClientDescription(name, passwordMD5);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.filePath = new File("data/");
        if (!this.filePath.exists()) {
            this.filePath.mkdirs();
        }

        this.connector = new Connector(address, port);
        this.receiver = new Receiver(this);

        // 关联
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
     * 获取客户端 ID 。
     *
     * @return 返回客户端 ID 。
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 获取客户端登录名。
     *
     * @return 返回客户端登录名。
     */
    public String getName() {
        return this.description.getName();
    }

    /**
     * 获取登录密码的 HASH 码。
     *
     * @return 返回登录密码的 HASH 码。
     */
    public String getPassword() {
        return this.description.getPassword();
    }

    /**
     * 获取客户端描述信息。
     *
     * @return 返回客户端描述信息。
     */
    public ClientDescription getDescription() {
        return this.description;
    }

    /**
     * 同步传输。
     *
     * @param actionDialect
     * @return
     */
    public ActionDialect syncTransmit(ActionDialect actionDialect) {
        return this.connector.send(this.receiver.inject(), actionDialect);
    }

    /**
     * @private
     * @return
     */
    public Connector getConnector() {
        return this.connector;
    }

    /**
     * @private
     * @return
     */
    public Receiver getReceiver() {
        return this.receiver;
    }

    protected void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 销毁客户端。
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
     * 中断客户端。
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

    /**
     * 恢复客户端。
     */
    public void connected() {
        synchronized (this) {
            if (this.interrupted) {
                Logger.i(Client.class, "Connection has reconnected");
            }

            this.interrupted = false;
        }
    }

    /**
     * 返回文件存储路径。
     *
     * @return 返回文件存储路径。
     */
    public File getFilePath() {
        return this.filePath;
    }

    /**
     * 是否已就绪。
     *
     * @return 如果就绪返回 {@code true} 。
     */
    public boolean isReady() {
        return (null != this.connector && this.connector.isConnected());
    }

    /**
     * 等待客户端就绪。
     *
     * @return 返回 {@code false} 表示无法与服务器建立连接。
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
     * 准备数据。
     *
     * @param pretender 指定伪装者。
     */
    public void prepare(Contact pretender) {
        this.pretender = pretender;
        this.description.setPretender(pretender);
    }

    /**
     * 准备数据。
     *
     * @param pretender 指定伪装者。
     * @param hubEnabled 是否启用 HUB 功能。
     */
    public void prepare(Contact pretender, boolean hubEnabled) {
        this.prepare(pretender);

        if (hubEnabled) {
            // 准备 HUB 控制器
            if (null == this.hubController) {
                this.hubController = new HubController(this);
            }
            this.hubController.prepare(this.connector, this.receiver);
        }
    }

    /**
     * 获取当前伪装的联系人。
     *
     * @return 返回当前伪装的联系人。
     */
    public Contact getPretender() {
        return this.pretender;
    }

    /**
     * 获取服务器日志。
     *
     * @return 返回最近产生的服务器日志。
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
     * 获取消息服务接口。
     *
     * @return 返回消息服务接口。
     */
    public MessageService getMessageService() {
        if (null == this.messageService) {
            // 创建消息代理
            this.messageService = new MessageService(this, this.connector, this.receiver);
        }
        return this.messageService;
    }

    /**
     * 获取文件上传器。
     *
     * @return 返回文件上传器。
     */
    public FileUploader getFileUploader() {
        if (null == this.uploader) {
            this.uploader = new FileUploader(this.connector);
        }

        return this.uploader;
    }

    /**
     * 获取文件处理器服务接口。
     *
     * @return 返回文件处理器服务接口。
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
     * 获取文件存储服务接口。
     *
     * @return 返回文件存储服务接口。
     */
    public FileStorage getFileStorage() {
        if (null == this.storage) {
            this.storage = new FileStorage(this);
        }
        return this.storage;
    }

    /**
     * 获取 Hub 控制器。
     *
     * @return 返回 Hub 控制器。
     */
    public HubController getHubController() {
        if (null == this.hubController) {
            this.hubController = new HubController(this);
        }

        return this.hubController;
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
     * 获取指定域数据。
     *
     * @param domainName 指定服务域名称。
     * @return 返回授权域数据实例。
     */
    public AuthDomain getDomain(String domainName) {
        if (!this.connector.isConnected()) {
            return null;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.GetDomain.name);
        actionDialect.addParam("domain", domainName);

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        JSONObject domainJson = result.getParamAsJson("authDomain");
        if (null == domainJson) {
            return null;
        }

        return new AuthDomain(domainJson);
    }

    /**
     * 获取指定域数据。
     *
     * @param domainName 域名称。
     * @param appKey 域 App Key 。
     * @return 返回授权域实例。
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
     * 创建域应用。
     *
     * @param domainName 指定域名称。
     * @param appKey 指定 App Key 。
     * @param appId 指定 App ID 。
     * @param mainEndpoint 指定主服务器连接点。
     * @param httpEndpoint 指定 HTTP 连接点。
     * @param httpsEndpoint 指定 HTTPS 连接点。
     * @param ferry 指定是否是摆渡域。
     * @return 返回授权域数据。
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
     * 更新域接入点信息。
     *
     * @param domainName 指定域名称。
     * @param mainEndpoint 指定主服务器连接点。
     * @param httpEndpoint 指定 HTTP 连接点。
     * @param httpsEndpoint 指定 HTTPS 连接点。
     * @return 返回授权域数据。
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
     * 申请访问令牌。
     *
     * @param domainName 指定域名称。
     * @param appKey 指定 App Key 。
     * @param cid 指定联系人 ID 。
     * @param duration 指定有效时长。
     * @return 返回访问令牌。
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

        // 发送请求并等待结果
        ActionDialect result = this.connector.send(notifier, actionDialect);
        JSONObject tokenJson = result.getParamAsJson("token");
        AuthToken token = new AuthToken(tokenJson);

        return token;
    }

    /**
     * 获取指定联系人的令牌。
     *
     * @param contactId 指定联系人 ID 。
     * @return 返回联系人当前使用的访问令牌。
     */
    public AuthToken getAuthToken(long contactId) {
        return TokenTools.getAuthToken(this.connector, this.receiver, contactId);
    }

    /**
     * 注入访问令牌。
     *
     * @param authToken 指定访问令牌。
     * @return 返回被注入的令牌实例，如果失败返回 {@code null} 值。
     */
    public AuthToken injectAuthToken(AuthToken authToken) {
        if (authToken.getContactId() == 0) {
            return null;
        }

        return TokenTools.injectAuthToken(this.connector, this.receiver, authToken);
    }

    /**
     * 注入联系人。
     *
     * @param contact 指定联系人。
     * @return 返回被注入的联系人实例，如果失败返回 {@code null} 值。
     */
    public Contact injectContact(Contact contact) {
        if (!this.connector.isConnected()) {
            return null;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.NewContact.name);
        actionDialect.addParam("contact", contact.toJSON());

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (null == result) {
            return null;
        }

        JSONObject jsonContact = result.getParamAsJson("contact");
        return new Contact(jsonContact);
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

        ActionDialect actionDialect = new ActionDialect(ClientAction.ListOnlineContacts.name);

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);

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
     * 批量获取联系人行为。
     *
     * @param contactId 指定联系人 ID 。
     * @param domain 指定访问域。
     * @param beginTime 指定查询起始时间戳。
     * @param endTime 指定查询截止时间戳。
     * @return 返回联系人行为列表。
     */
    public List<ContactBehavior> listContactBehaviors(long contactId, String domain, long beginTime, long endTime) {
        return this.listContactBehaviors(contactId, domain, beginTime, endTime, null);
    }

    /**
     * 批量获取联系人行为。
     *
     * @param contactId 指定联系人 ID 。
     * @param domain 指定访问域。
     * @param beginTime 指定查询起始时间戳。
     * @param endTime 指定查询截止时间戳。
     * @param behavior 指定行为。
     * @return 返回联系人行为列表。
     */
    public List<ContactBehavior> listContactBehaviors(long contactId, String domain, long beginTime, long endTime,
                                             String behavior) {
        if (!this.connector.isConnected()) {
            return null;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.ListContactBehaviors.name);
        ListContactBehaviors parameter = new ListContactBehaviors(contactId, domain, beginTime, endTime, behavior);
        actionDialect.addParam(NoticeData.PARAMETER, parameter);

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (null == result) {
            return null;
        }

        if (RiskMgmtStateCode.Ok.code != result.getParamAsInt(NoticeData.CODE)) {
            return null;
        }

        List<ContactBehavior> list = new ArrayList<>();
        JSONObject data = result.getParamAsJson(NoticeData.DATA);
        JSONArray array = data.getJSONArray("list");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject json = array.getJSONObject(i);
            ContactBehavior contactBehavior = new ContactBehavior(json);
            list.add(contactBehavior);
        }
        return list;
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

        ActionDialect actionDialect = new ActionDialect(ClientAction.CreateContact.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("id", id.longValue());
        actionDialect.addParam("name", name);
        if (null != context) {
            actionDialect.addParam("context", context);
        }

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (null == result) {
            return null;
        }

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

        ActionDialect actionDialect = new ActionDialect(ClientAction.UpdateContact.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("id", contactId.longValue());

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
     * 使用令牌码查找指定的联系人。
     *
     * @param tokenCode 指定令牌码。
     * @return 返回联系人实例。如果查询失败返回 {@code null} 值。
     */
    public Contact getContactByToken(String tokenCode) {
        if (!this.connector.isConnected()) {
            return null;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.GetContact.name);
        actionDialect.addParam("token", tokenCode);

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (!result.containsParam("contact")) {
            return null;
        }

        JSONObject data = result.getParamAsJson("contact");
        Contact contact = new Contact(data);
        return contact;
    }

    /**
     * 获取指定 ID 的联系人。
     *
     * @param domain 指定域名称。
     * @param id 指定联系人的 ID 。
     * @return 返回指定的联系人实例。
     */
    public Contact getContact(String domain, long id) {
        if (!this.connector.isConnected()) {
            return null;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.GetContact.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("contactId", id);

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (!result.containsParam("contact")) {
            return null;
        }

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

        ActionDialect actionDialect = new ActionDialect(ClientAction.GetGroup.name);
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
     * 强制向指定联系人的分区添加参与人。
     *
     * @param contact 指定联系人。
     * @param zoneName 指定分区名。
     * @param participant 指定参与人。
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

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (result.containsParam("contactZone")) {
            return new ContactZone(result.getParamAsJson("contactZone"));
        }
        else {
            return null;
        }
    }

    /**
     * 强制从分区移除指定参与人。
     *
     * @param contact 指定联系人。
     * @param zoneName 指定分区名。
     * @param participant 指定参与人。
     * @return
     */
    public ContactZone removeParticipantFromZoneByForce(Contact contact, String zoneName, Contact participant) {
        // TODO
        return null;
    }

    /**
     * 强制修改指定分区里参与人的状态。
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
