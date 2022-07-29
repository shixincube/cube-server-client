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
import cube.client.listener.WorkflowListener;
import cube.client.tool.TokenTools;
import cube.client.util.*;
import cube.common.action.ClientAction;
import cube.common.entity.FileLabel;
import cube.common.entity.FileResult;
import cube.common.entity.SharingTag;
import cube.common.entity.VisitTrace;
import cube.common.notice.GetSharingTag;
import cube.common.notice.ListSharingTags;
import cube.common.notice.ListSharingTraces;
import cube.common.notice.NoticeData;
import cube.common.state.FileProcessorStateCode;
import cube.common.state.FileStorageStateCode;
import cube.file.FileProcessResult;
import cube.file.OperationWorkflow;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件处理器。
 */
public class FileProcessor {

    private File filePath;

    private Connector connector;

    private Receiver receiver;

    private FileUploader uploader;

    private Long contactId;

    private String domainName;

    protected WorkflowListener workflowListener;

    public FileProcessor(File filePath, Connector connector, Receiver receiver) {
        this.filePath = filePath;
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

    public File getFilePath() {
        return this.filePath;
    }

    /**
     * 注册工作流监听器。
     *
     * @param workflowListener
     */
    public void register(WorkflowListener workflowListener) {
        this.workflowListener = workflowListener;
    }

    /**
     * 注销工作流监听器。
     */
    public void deregister() {
        this.workflowListener = null;
    }

    public WorkflowListener getWorkflowListener() {
        return this.workflowListener;
    }

    /**
     * 获取文件标签。
     *
     * @param fileCode
     * @return
     */
    public FileLabel getFileLabel(String fileCode) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.GetFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("fileCode", fileCode);

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.w(FileProcessor.class, "#getFileLabel - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("fileLabel");
        return new FileLabel(data);
    }

    /**
     * 获取指定文件的文件码。
     *
     * @param file
     * @return
     */
    public FileLabel getFileLabel(File file) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.FindFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("contactId", this.contactId.longValue());
        actionDialect.addParam("fileName", file.getName());
        actionDialect.addParam("fileSize", file.length());
        actionDialect.addParam("lastModified", file.lastModified());

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
            return new FileLabel(result.getParamAsJson("fileLabel"));
        }

        return null;
    }

    /**
     * 批量获取分享标签。
     *
     * @param contactId
     * @param domainName
     * @param beginIndex
     * @param endIndex
     * @param valid
     * @return
     */
    public List<SharingTag> listSharingTags(long contactId, String domainName,
                                            int beginIndex, int endIndex, boolean valid) {
        List<SharingTag> list = new ArrayList<>();

        final int step = 10;
        List<Integer> indexes = new ArrayList<>();
        int delta = endIndex - beginIndex;
        if (delta > 9) {
            int num = (int) Math.floor((float)(delta + 1) / (float)step);
            int mod = (delta + 1) % step;
            int index = beginIndex;
            for (int i = 0; i < num; ++i) {
                index += step - 1;
                indexes.add(index);
                index += 1;
            }

            if (mod != 0) {
                index += mod - 1;
                indexes.add(index);
            }
        }
        else {
            indexes.add(endIndex);
        }

        int begin = beginIndex;
        int end = 0;
        for (Integer index : indexes) {
            end = index;

            ActionDialect actionDialect = new ActionDialect(ClientAction.ListSharingTags.name);
            actionDialect.addParam(NoticeData.PARAMETER, new ListSharingTags(contactId, domainName,
                    begin, end, valid));

            ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
            if (null == result) {
                Logger.w(this.getClass(), "#listSharingTags - Network error");
                break;
            }

            if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
                JSONObject data = result.getParamAsJson("data");
                JSONArray array = data.getJSONArray("list");
                if (array.length() == 0) {
                    // 没有数据，结束数据获取
                    continue;
                }

                for (int i = 0; i < array.length(); ++i) {
                    SharingTag tag = new SharingTag(array.getJSONObject(i));
                    list.add(tag);
                }
            }

            // 更新索引
            begin = index + 1;
        }

        return list;
    }

    /**
     * 批量获取文件访问痕迹。
     *
     * @param contactId
     * @param domainName
     * @param sharingCode
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public List<VisitTrace> listSharingTrace(long contactId, String domainName, String sharingCode,
                                             int beginIndex, int endIndex) {
        List<VisitTrace> list = new ArrayList<>();

        ActionDialect actionDialect = new ActionDialect(ClientAction.ListSharingTraces.name);
        actionDialect.addParam(NoticeData.PARAMETER, new ListSharingTraces(contactId, domainName, sharingCode,
                beginIndex, endIndex));

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (null == result) {
            Logger.w(this.getClass(), "#listSharingTrace - Network error");
            return list;
        }

        if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
            JSONObject data = result.getParamAsJson("data");
            JSONArray array = data.getJSONArray("list");
            for (int i = 0; i < array.length(); ++i) {
                VisitTrace trace = new VisitTrace(array.getJSONObject(i));
                list.add(trace);
            }
        }

        return list;
    }

    /**
     * 获取分享标签。
     *
     * @param sharingCode
     * @return
     */
    public SharingTag getSharingTag(String sharingCode) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.GetSharingTag.name);
        actionDialect.addParam(NoticeData.PARAMETER, new GetSharingTag(sharingCode));

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (null == result) {
            Logger.w(this.getClass(), "#getSharingTag - Network error");
            return null;
        }

        if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
            JSONObject data = result.getParamAsJson("data");
            return new SharingTag(data);
        }
        else {
            return null;
        }
    }

    /**
     * 下载指定文件码的文件。
     *
     * @param fileCode
     * @return
     */
    public FileLabel downloadFile(String fileCode) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.GetFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("fileCode", fileCode);
        actionDialect.addParam("transmitting", true);

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.w(FileProcessor.class, "#downloadFile - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("fileLabel");
        FileLabel fileLabel = new FileLabel(data);

        this.receiver.setStreamListener(fileLabel.getFileName(), new StreamListener() {
            @Override
            public void onCompleted(String streamName, File streamFile) {
                synchronized (fileLabel) {
                    fileLabel.notify();
                }
            }
        });

        synchronized (fileLabel) {
            try {
                fileLabel.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.receiver.removeStreamListener(fileLabel.getFileName());

        // 设置本地文件实例
        fileLabel.setFile(new File(this.filePath, fileLabel.getFileName()));

        return fileLabel;
    }

    /**
     * 删除文件，该操作将从服务器上删除指定文件数据，不可以逆。
     *
     * @param fileLabel
     * @return
     */
    public FileLabel deleteFile(FileLabel fileLabel) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.DeleteFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("fileCode", fileLabel.getFileCode());

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
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
     * 为指定的文件准备 M3U8 流文件。
     *
     * @param fileCode
     * @param dispatcherHttpAddress
     * @return
     */
    public String prepareM3U8(String fileCode, String dispatcherHttpAddress) {
        FileLabel fileLabel = this.getFileLabel(fileCode);
        if (null == fileLabel) {
            Logger.w(FileProcessor.class, "#prepareM3U8 - No file label : " + fileCode);
            return null;
        }

        String sourceURL = this.getMediaSource(dispatcherHttpAddress, fileLabel.getFileCode());
        if (null == sourceURL) {
            Logger.w(FileProcessor.class, "#prepareM3U8 - Can NOT gets media source : " + fileLabel.getFileCode());
            return null;
        }

        // 获取令牌
        AuthToken authToken = TokenTools.getAuthToken(this.connector, this.receiver, this.contactId);

        StringBuilder buf = new StringBuilder(sourceURL);
        buf.append("?t=");
        buf.append(authToken.getCode());
        sourceURL = buf.toString();

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        boolean success = false;

        long time = System.currentTimeMillis();

        // 访问 M3U8 文件
        try {
            URL url = new URL(sourceURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setUseCaches(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            // 连接
            connection.connect();
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                success = true;

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    Logger.d(FileProcessor.class, "[M3U8] " + line);
                }
            }
            else {
                Logger.w(FileProcessor.class, "#prepareM3U8 - Request failed : " + code);
            }
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

            connection.disconnect();
        }

        Logger.d(FileProcessor.class, "#prepareM3U8 - Elapsed : " + (System.currentTimeMillis() - time));

        return success ? sourceURL : null;
    }

    /**
     * 对指定文件进行操作。
     *
     * @param fileProcessing
     * @param file
     * @return
     */
    public FileProcessResult call(FileProcessing fileProcessing, File file) {
        FileLabel fileLabel = this.checkWithUploadStrategy(file);
        if (null == fileLabel) {
            Logger.i(FileProcessor.class, "#call - Can NOT get file : " + file.getName());
            return null;
        }

        Notifier notifier = this.receiver.inject();

        ActionDialect actionDialect = new ActionDialect(ClientAction.ProcessFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("fileCode", fileLabel.getFileCode());
        actionDialect.addParam("process", fileProcessing.process);

        JSONObject parameter = fileProcessing.getParameter();
        if (null != parameter) {
            actionDialect.addParam("parameter", parameter);
        }

        ActionDialect result = this.connector.send(notifier, actionDialect);
        if (null == result) {
            Logger.w(this.getClass(), "#call timeout");
            return null;
        }

        int code = result.getParamAsInt("code");
        if (code != FileProcessorStateCode.Ok.code) {
            Logger.w(FileProcessor.class, "#call - " + fileProcessing.process + " - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("result");
        FileProcessResult processResult = new FileProcessResult(data);

        if (processResult.hasResult()) {
            waitingForStream(processResult);
        }

        return processResult;
    }

    private void waitingForStream(FileProcessResult fileProcessResult) {
        List<FileResult> resultList = fileProcessResult.getResultList();
        AtomicInteger count = new AtomicInteger(resultList.size());

        (new Thread() {
            @Override
            public void run() {
                for (FileResult result : resultList) {
                    // 添加监听器
                    receiver.setStreamListener(result.streamName, new StreamListener() {
                        @Override
                        public void onCompleted(String streamName, File streamFile) {
                            // 设置结果文件
                            fileProcessResult.addLocalFile(streamFile);

                            if (count.decrementAndGet() == 0) {
                                synchronized (resultList) {
                                    resultList.notify();
                                }
                            }
                        }
                    });
                }
            }
        }).start();

        synchronized (resultList) {
            try {
                resultList.wait(15 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (FileResult result : resultList) {
            // 移除监听器
            receiver.setStreamListener(result.streamName, null);
        }
    }

    /**
     *
     *
     * @param workflow
     * @param file
     * @return
     */
    public FileProcessResult call(OperationWorkflow workflow, File file) {
        FileLabel fileLabel = this.checkWithUploadStrategy(file);
        if (null == fileLabel) {
            Logger.i(FileProcessor.class, "#call - Can NOT get file : " + file.getName());
            return null;
        }

        workflow.setDomain(this.domainName);
        workflow.setSourceFileCode(fileLabel.getFileCode());

        ActionDialect actionDialect = new ActionDialect(ClientAction.SubmitWorkflow.name);
        actionDialect.addParam("workflow", workflow.toJSON());
        ActionDialect response = this.connector.send(this.receiver.inject(), actionDialect);

        if (response.getParamAsInt("code") != FileProcessorStateCode.Ok.code) {
            return null;
        }

        FileProcessResult result = new FileProcessResult(response.getParamAsJson("result"));
        return result;
    }

    /**
     * 检查文件，如果文件在服务器上不存在则上传。
     *
     * @param file
     * @return
     */
    public FileLabel checkWithUploadStrategy(File file) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.FindFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("contactId", this.contactId.longValue());
        actionDialect.addParam("fileName", file.getName());
        actionDialect.addParam("fileSize", file.length());
        actionDialect.addParam("lastModified", file.lastModified());

        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (null == result || !result.containsParam("code")) {
            Logger.e(this.getClass(), "#checkWithUploadStrategy - State error");
            return null;
        }

        MutableFileLabel mutableFileLabel = new MutableFileLabel();

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.i(FileProcessor.class, "#checkWithUploadStrategy - Not find file : " + file.getName());

            Promise.create(new PromiseHandler<FileUploader.UploadMeta>() {
                @Override
                public void emit(PromiseFuture<FileUploader.UploadMeta> promise) {
                    // 上传文件数据
                    uploader.upload(contactId, domainName, file, new FileUploadListener() {
                        @Override
                        public void onUploading(FileUploader.UploadMeta meta, long processedSize) {
                            Logger.d(FileProcessor.class, "#checkWithUploadStrategy - onUploading : " +
                                    FileUtils.scaleFileSize(processedSize) + "/" +
                                    FileUtils.scaleFileSize(meta.file.length()));
                        }

                        @Override
                        public void onCompleted(FileUploader.UploadMeta meta) {
                            Logger.i(FileProcessor.class, "#checkWithUploadStrategy - onCompleted : " + meta.fileCode);
                            promise.resolve(meta);
                        }

                        @Override
                        public void onFailed(FileUploader.UploadMeta meta, Throwable throwable) {
                            Logger.w(FileProcessor.class, "#checkWithUploadStrategy - onFailed : " + file.getName(), throwable);
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
                    mutableFileLabel.wait(10 * 60 * 1000);
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
                this.contactId, file.getName(), file.length(), file.lastModified(), System.currentTimeMillis(),
                0);
        fileLabel.setFileType(fileType);
        fileLabel.setMD5Code(md5Code);
        fileLabel.setSHA1Code(sha1Code);

        ActionDialect actionDialect = new ActionDialect(ClientAction.PutFile.name);
        actionDialect.addParam("fileLabel", fileLabel.toJSON());

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(this.receiver.inject(), actionDialect);
        if (null == result) {
            return null;
        }

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.w(FileProcessor.class, "#putFileLabel - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("fileLabel");
        return new FileLabel(data);
    }
}
