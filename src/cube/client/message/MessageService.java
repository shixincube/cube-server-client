package cube.client.message;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.client.*;
import cube.client.file.FileUploader;
import cube.client.listener.FileUploadListener;
import cube.client.listener.MessageReceiveListener;
import cube.client.listener.MessageSendListener;
import cube.client.tool.MessageIterator;
import cube.client.tool.MessageReceiveEvent;
import cube.client.tool.MessageSendEvent;
import cube.client.util.*;
import cube.common.UniqueKey;
import cube.common.action.ClientAction;
import cube.common.entity.*;
import cube.common.state.FileStorageStateCode;
import cube.common.state.MessagingStateCode;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MessageService {

    private Client client;

    private Connector connector;

    private Receiver receiver;

    private ConcurrentMap<String, MessageReceiveEvent> messageReceiveEventMap;

    private ConcurrentMap<String, MessageSendEvent> messageSendEventMap;

    public MessageService(Client client, Connector connector, Receiver receiver) {
        this.client = client;
        this.connector = connector;
        this.receiver = receiver;
        this.messageReceiveEventMap = new ConcurrentHashMap<>();
        this.messageSendEventMap = new ConcurrentHashMap<>();
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

        MessageReceiveEvent event = this.messageReceiveEventMap.get(contact.getUniqueKey());
        if (null != event) {
            event.listener = listener;
            return true;
        }

        event = new MessageReceiveEvent(contact, listener);
        this.messageReceiveEventMap.put(contact.getUniqueKey(), event);

        ActionDialect actionDialect = new ActionDialect(ClientAction.AddEventListener.name);
        actionDialect.addParam("id", this.client.getId().longValue());
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

        MessageReceiveEvent event = this.messageReceiveEventMap.remove(contact.getUniqueKey());
        if (null == event) {
            return false;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.RemoveEventListener.name);
        actionDialect.addParam("id", this.client.getId().longValue());
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

        MessageReceiveEvent event = this.messageReceiveEventMap.get(group.getUniqueKey());
        if (null != event) {
            event.listener = listener;
            return true;
        }

        event = new MessageReceiveEvent(group, listener);
        this.messageReceiveEventMap.put(group.getUniqueKey(), event);

        ActionDialect actionDialect = new ActionDialect(ClientAction.AddEventListener.name);
        actionDialect.addParam("id", this.client.getId().longValue());
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

        MessageReceiveEvent event = this.messageReceiveEventMap.remove(group.getUniqueKey());
        if (null == event) {
            return false;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.RemoveEventListener.name);
        actionDialect.addParam("id", this.client.getId().longValue());
        actionDialect.addParam("event", Events.ReceiveMessage.name);

        JSONObject param = new JSONObject();
        param.put("domain", group.getDomain().getName());
        param.put("groupId", group.getId().longValue());
        actionDialect.addParam("param", param);

        this.connector.send(actionDialect);

        return true;
    }

    /**
     * 注册监听指定联系人发送的消息。
     *
     * @param contact 指定被监听的联系人。
     * @param listener 指定监听器。
     * @return 返回操作是否有效。
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
        actionDialect.addParam("id", this.client.getId().longValue());
        actionDialect.addParam("event", Events.SendMessage.name);

        JSONObject param = new JSONObject();
        param.put("domain", contact.getDomain().getName());
        param.put("contactId", contact.getId().longValue());
        actionDialect.addParam("param", param);

        this.connector.send(actionDialect);

        return true;
    }

    /**
     * 注销监听指定联系人发送消息的监听器。
     *
     * @param contact 指定被监听的联系人。
     * @return 返回操作是否有效。
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
        actionDialect.addParam("id", this.client.getId().longValue());
        actionDialect.addParam("event", Events.SendMessage.name);

        JSONObject param = new JSONObject();
        param.put("domain", contact.getDomain().getName());
        param.put("contactId", contact.getId().longValue());
        actionDialect.addParam("param", param);

        this.connector.send(actionDialect);

        return true;
    }

    public MessageReceiveListener getMessageReceiveListener(long contactId, String domain) {
        MessageReceiveEvent event = this.messageReceiveEventMap.get(UniqueKey.make(contactId, domain));
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    public MessageReceiveListener getMessageReceiveListener(Contact contact) {
        MessageReceiveEvent event = this.messageReceiveEventMap.get(contact.getUniqueKey());
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    public MessageReceiveListener getMessageReceiveListener(Group group) {
        MessageReceiveEvent event = this.messageReceiveEventMap.get(group.getUniqueKey());
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    public MessageSendListener getMessageSendListener(long contactId, String domain) {
        MessageSendEvent event = this.messageSendEventMap.get(UniqueKey.make(contactId, domain));
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    public MessageSendListener getMessageSendListener(Contact contact) {
        MessageSendEvent event = this.messageSendEventMap.get(contact.getUniqueKey());
        if (null == event) {
            return null;
        }

        return event.listener;
    }

    /**
     * 推送消息给指定的联系人。
     *
     * @param receiver
     * @param payload
     * @return
     */
    public boolean pushMessage(Contact receiver, JSONObject payload) {
        if (null == this.client.getPretender()) {
            Logger.e(this.getClass(), "#pushMessage - No pretender data");
            return false;
        }

        return this.pushMessageWithPretender(receiver, this.client.getPretender(), payload);
    }

    /**
     * 推送消息给指定的联系人。
     *
     * @param receiver
     * @param payload
     * @param file
     * @return
     */
    public boolean pushMessage(Contact receiver, JSONObject payload, File file) {
        if (null == this.client.getPretender()) {
            Logger.e(this.getClass(), "#pushMessage - No pretender data");
            return false;
        }

        FileLabel fileLabel = null;
        if (null != file) {
            // 将文件发送给服务器
            fileLabel = this.putFileWithPretender(this.client.getPretender(), file);
            if (null == fileLabel) {
                Logger.d(Client.class, "#pushMessage - push file: \"" + file.getName() + "\" failed");
                return false;
            }
        }

        // 创建消息
        long timestamp = System.currentTimeMillis();
        Device device = new Device("Client", "Cube Server Client " + Client.VERSION);

        Message message = null;

        if (null != fileLabel) {
            FileAttachment fileAttachment = new FileAttachment(fileLabel);
            fileAttachment.setCompressed(true);

            message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                    this.client.getPretender().getId(), receiver.getId(), 0L, 0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                    device.toCompactJSON(), payload, fileAttachment.toJSON());
        }
        else {
            message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                    this.client.getPretender().getId(), receiver.getId(), 0L, 0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                    device.toCompactJSON(), payload, null);
        }

        // 推送消息
        boolean result = this.pushMessage(message, this.client.getPretender(), device);
        return result;
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
                new Device("Client", "Cube Server Client " + Client.VERSION), payload);
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

        // 推送消息
        return pushMessage(message, pretender, device);
    }

    /**
     * 使用伪装身份推送文件消息。
     *
     * @param receiver 指定消息收件人。
     * @param pretender 指定伪装的联系人。
     * @param file 指定文件。
     * @return 如果消息被服务器处理返回 {@code true} 。
     */
    public boolean pushFileMessageWithPretender(Contact receiver, Contact pretender, File file) {
        Logger.d(Client.class, "#pushFileMessageWithPretender - push file: \"" + file.getName() + "\" to \""
                + receiver.getId() + "\" from \"" + pretender.getId() + "\"");

        // 将文件发送给服务器
        FileLabel fileLabel = this.putFileWithPretender(pretender, file);
        if (null == fileLabel) {
            Logger.d(Client.class, "#pushFileMessageWithPretender - push file: \"" + file.getName() + "\" failed");
            return false;
        }

        // 创建消息
        long timestamp = System.currentTimeMillis();
        Device device = new Device("Client", "Cube Server Client " + Client.VERSION);

        JSONObject payload = new JSONObject();
        payload.put("type", "file");

        FileAttachment fileAttachment = new FileAttachment(fileLabel);

        Message message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                pretender.getId(), receiver.getId(), 0L, 0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                device.toCompactJSON(), payload, fileAttachment.toJSON());

        // 推送消息
        boolean result = this.pushMessage(message, pretender, device);
        return result;
    }

    /**
     * 使用伪装身份推送图片消息。
     *
     * @param receiver
     * @param pretender
     * @param file
     * @return
     */
    public boolean pushImageMessageWithPretender(Contact receiver, Contact pretender, File file) {
        Logger.d(Client.class, "#pushImageMessageWithPretender - push file: \"" + file.getName() + "\" to \""
                + receiver.getId() + "\" from \"" + pretender.getId() + "\"");

        // 将文件发送给服务器
        FileLabel fileLabel = this.putFileWithPretender(pretender, file);
        if (null == fileLabel) {
            Logger.d(Client.class, "#pushImageMessageWithPretender - push file: \"" + file.getName() + "\" failed");
            return false;
        }

        // 创建消息
        long timestamp = System.currentTimeMillis();
        Device device = new Device("Client", "Cube Server Client " + Client.VERSION);

        JSONObject payload = new JSONObject();
        payload.put("type", "image");

        FileAttachment fileAttachment = new FileAttachment(fileLabel);
        fileAttachment.setCompressed(true);

        Message message = new Message(receiver.getDomain().getName(), Utils.generateSerialNumber(),
                pretender.getId(), receiver.getId(), 0L, 0L, timestamp, 0L, MessageState.Sending.getCode(), 0,
                device.toCompactJSON(), payload, fileAttachment.toJSON());

        // 推送消息
        boolean result = this.pushMessage(message, pretender, device);
        return result;
    }

    /**
     * 使用伪装身份发送文件。
     *
     * @param pretender 指定伪装的联系人。
     * @param file 指定文件。
     * @return 如果消息被服务器处理返回 {@code true} 。
     */
    private FileLabel putFileWithPretender(Contact pretender, File file) {
        MutableFileLabel mutableFileLabel = new MutableFileLabel();

        Promise.create(new PromiseHandler<FileUploader.UploadMeta>() {
            @Override
            public void emit(PromiseFuture<FileUploader.UploadMeta> promise) {
                // 上传文件数据
                client.getFileUploader().upload(pretender.getId(), pretender.getDomain().getName(), file, new FileUploadListener() {
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
                // 放置文件
                FileLabel fileLabel = putFileLabel(pretender, meta.fileCode, meta.file, meta.getMD5Code(), meta.getSHA1Code());
                if (null == fileLabel) {
                    // 放置标签失败
                    synchronized (mutableFileLabel) {
                        mutableFileLabel.notify();
                    }
                    return;
                }

                // 上传完成，查询文件标签
                fileLabel = queryFileLabel(pretender.getDomain().getName(), fileLabel.getFileCode());
                if (null == fileLabel) {
                    synchronized (mutableFileLabel) {
                        mutableFileLabel.notify();
                    }
                    return;
                }

                // 赋值
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
     * 查询标签。
     *
     * @param domain
     * @param fileCode
     * @return
     */
    private FileLabel queryFileLabel(String domain, String fileCode) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.GetFile.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("fileCode", fileCode);

        // 阻塞线程，并等待返回结果
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
     * 放置标签。
     *
     * @param contact
     * @param fileCode
     * @param file
     * @param md5Code
     * @param sha1Code
     * @return
     */
    private FileLabel putFileLabel(Contact contact, String fileCode, File file, String md5Code, String sha1Code) {
        // 判断文件类型
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

        // 阻塞线程，并等待返回结果
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
     * 查询与指定联系人相关的消息。
     *
     * @param beginning 指定查询开始时间。
     * @param contact 指定待查询的联系人。
     * @return 返回消息数据迭代器。
     */
    public MessageIterator queryMessages(long beginning, Contact contact) {
        MessageIterator iterator = new MessageIterator(this.connector, this.receiver);
        iterator.prepare(beginning, contact);
        return iterator;
    }

    /**
     * 标记消息已读。
     *
     * @param receiver 指定消息的收件人。
     * @param sender 指定消息的发件人。
     * @param messagesList 指定消息列表。
     * @return 返回被修改状态的消息列表，该列表里的消息为紧凑格式。发送错误返回 {@code null} 值。
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
}
