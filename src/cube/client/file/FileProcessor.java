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

package cube.client.file;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.client.Connector;
import cube.client.Notifier;
import cube.client.Receiver;
import cube.client.StreamListener;
import cube.client.listener.FileUploadListener;
import cube.client.tool.TokenTools;
import cube.client.util.*;
import cube.common.action.ClientAction;
import cube.common.entity.FileLabel;
import cube.common.entity.ProcessResultStream;
import cube.common.state.FileProcessorStateCode;
import cube.common.state.FileStorageStateCode;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 文件处理器。
 */
public class FileProcessor {

    private Connector connector;

    private Receiver receiver;

    private FileUploader uploader;

    private Long contactId;

    private String domainName;

    public FileProcessor(Connector connector, Receiver receiver) {
        this.connector = connector;
        this.receiver = receiver;
        this.uploader = new FileUploader(connector);
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * 获取文件标签。
     *
     * @param fileCode
     * @return
     */
    public FileLabel getFileLabel(String fileCode) {
        Notifier notifier = this.receiver.inject();

        ActionDialect actionDialect = new ActionDialect(ClientAction.GetFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("fileCode", fileCode);

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(notifier, actionDialect);

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.w(FileProcessor.class, "#getFileLabel - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("fileLabel");
        return new FileLabel(data);
    }

    public FileLabel getFileLabel(File file) {
        Notifier notifier = this.receiver.inject();

        ActionDialect actionDialect = new ActionDialect(ClientAction.FindFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("contactId", this.contactId.longValue());
        actionDialect.addParam("fileName", file.getName());
        actionDialect.addParam("fileSize", file.length());
        actionDialect.addParam("lastModified", file.lastModified());

        ActionDialect result = this.connector.send(notifier, actionDialect);
        if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
            return new FileLabel(result.getParamAsJson("fileLabel"));
        }

        return null;
    }

    /**
     *
     * @param dispatcherHttpAddress
     * @param fileCode
     * @return
     */
    public String getMediaSource(String dispatcherHttpAddress, String fileCode) {
        AuthToken authToken = TokenTools.getAuthToken(this.connector, this.receiver, this.contactId);
        if (null == authToken) {
            return null;
        }

        StringBuilder buf = new StringBuilder(dispatcherHttpAddress);
        buf.append("/file/source/");
        buf.append("?t=");
        buf.append(authToken.getCode());
        buf.append("&fc=");
        buf.append(fileCode);

        BufferedReader reader = null;

        try {
            URL url = new URL(buf.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setUseCaches(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            // 连接
            connection.connect();
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                buf.delete(0, buf.length());

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                }
            }

            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        if (buf.length() > 3) {
            JSONObject result = new JSONObject(buf.toString());
            return result.getString("url");
        }

        return null;
    }

    /**
     * 对指定文件进行操作。
     *
     * @param process
     * @param file
     * @return
     */
    public FileProcessResult call(FileOperation process, File file) {
        FileLabel fileLabel = this.checkAndGet(file);
        if (null == fileLabel) {
            Logger.i(FileProcessor.class, "#call - Can NOT get file : " + file.getName());
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(ClientAction.ProcessFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("fileCode", fileLabel.getFileCode());
        actionDialect.addParam("process", process.process);

        ActionDialect result = this.connector.send(notifier, actionDialect);
        if (null == result) {
            Logger.w(this.getClass(), "#call timeout");
            return null;
        }

        int code = result.getParamAsInt("code");
        if (code != FileProcessorStateCode.Ok.code) {
            Logger.w(FileProcessor.class, "#call - " + process.process + " - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("result");
        FileProcessResult processResult = new FileProcessResult(data);

        if (processResult.hasResultStream()) {
            final ProcessResultStream resultStream = processResult.getResultStream();
            // 添加监听器
            this.receiver.setStreamListener(resultStream.streamName, new StreamListener() {
                @Override
                public void onCompleted(String streamName, File streamFile) {
                    synchronized (resultStream) {
                        resultStream.notify();
                    }
                }
            });

            synchronized (resultStream) {
                try {
                    resultStream.wait(10 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 移除监听器
            this.receiver.setStreamListener(resultStream.streamName, null);
        }

        return processResult;
    }

    /**
     * 检查并获取文件。
     *
     * @param file
     * @return
     */
    private FileLabel checkAndGet(File file) {
        Notifier notifier = this.receiver.inject();

        ActionDialect actionDialect = new ActionDialect(ClientAction.FindFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("contactId", this.contactId.longValue());
        actionDialect.addParam("fileName", file.getName());
        actionDialect.addParam("fileSize", file.length());
        actionDialect.addParam("lastModified", file.lastModified());

        ActionDialect result = this.connector.send(notifier, actionDialect);

        MutableFileLabel mutableFileLabel = new MutableFileLabel();

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.i(FileProcessor.class, "#checkAndGet - Not find file : " + file.getName());

            Promise.create(new PromiseHandler<FileUploader.UploadMeta>() {
                @Override
                public void emit(PromiseFuture<FileUploader.UploadMeta> promise) {
                    // 上传文件数据
                    uploader.upload(contactId, domainName, file, new FileUploadListener() {
                        @Override
                        public void onUploading(FileUploader.UploadMeta meta, long processedSize) {
                            Logger.d(FileProcessor.class, "#checkAndGet - onUploading : " +
                                    processedSize + "/" + meta.file.length());
                        }

                        @Override
                        public void onCompleted(FileUploader.UploadMeta meta) {
                            Logger.i(FileProcessor.class, "#checkAndGet - onCompleted : " + meta.fileCode);
                            promise.resolve(meta);
                        }

                        @Override
                        public void onFailed(FileUploader.UploadMeta meta, Throwable throwable) {
                            Logger.w(FileProcessor.class, "#checkAndGet - onFailed : " + file.getName(), throwable);
                            promise.reject(meta);
                        }
                    });
                }
            }).then(new Future<FileUploader.UploadMeta>() {
                @Override
                public void come(FileUploader.UploadMeta meta) {
                    // 放置文件
                    FileLabel fileLabel = putFileLabel(meta.fileCode, meta.file, meta.getMD5Code(), meta.getSHA1Code());
                    if (null == fileLabel) {
                        // 放置标签失败
                        synchronized (mutableFileLabel) {
                            mutableFileLabel.notify();
                        }
                        return;
                    }

                    // 上传完成，查询文件标签
                    fileLabel = getFileLabel(fileLabel.getFileCode());
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
        }
        else {
            mutableFileLabel.value = new FileLabel(result.getParamAsJson("fileLabel"));
        }

        return mutableFileLabel.value;
    }

    private FileLabel putFileLabel(String fileCode, File file, String md5Code, String sha1Code) {
        // 判断文件类型
        FileType fileType = FileUtils.extractFileExtensionType(file.getName());

        FileLabel fileLabel = new FileLabel(this.domainName, fileCode,
                this.contactId, file.getName(), file.length(), file.lastModified(), System.currentTimeMillis(), 0);
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
            Logger.w(FileProcessor.class, "#putFileLabel - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("fileLabel");
        return new FileLabel(data);
    }
}
