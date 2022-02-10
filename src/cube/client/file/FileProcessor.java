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
import cube.client.*;
import cube.client.listener.FileUploadListener;
import cube.common.entity.FileLabel;
import cube.common.state.FileStorageStateCode;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.io.File;

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

    public FileLabel getFileLabel(String fileCode) {
        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(Actions.GetFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("fileCode", fileCode);

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(notifier, actionDialect);

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.w(CubeClient.class, "#getFileLabel - error : " + code);
            return null;
        }

        JSONObject data = result.getParamAsJson("fileLabel");
        return new FileLabel(data);
    }

    /**
     * 对指定文件进行操作。
     *
     * @param process
     * @param file
     * @return
     */
    public FileProcessResult call(ImageProcess process, File file) {
        FileType fileType = FileUtils.extractFileExtensionType(file.getName());
        if (!FileUtils.isImageType(fileType)) {
            return null;
        }

        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(Actions.FindFile.name);
        actionDialect.addParam("domain", this.domainName);
        actionDialect.addParam("contactId", this.contactId.longValue());
        actionDialect.addParam("fileName", file.getName());
        actionDialect.addParam("fileSize", file.length());
        actionDialect.addParam("lastModified", file.lastModified());

        ActionDialect result = this.connector.send(notifier, actionDialect);

        FileLabel fileLabel = null;
        StringBuilder fileCode = new StringBuilder();

        int code = result.getParamAsInt("code");
        if (code != FileStorageStateCode.Ok.code) {
            Logger.i(FileProcessor.class, "#call - Not find file : " + file.getName());

            Object mutex = new Object();

            // 上传文件
            this.uploader.upload(this.contactId, this.domainName, file, new FileUploadListener() {
                @Override
                public void onUploading(FileUploader.UploadMeta meta, long processedSize) {
                    Logger.d(FileProcessor.class, "Uploading file");
                }

                @Override
                public void onCompleted(FileUploader.UploadMeta meta) {
                    Logger.i(FileProcessor.class, "#call - Upload file : " + file.getName());

                    (new Thread() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            fileCode.append(meta.fileCode);

                            synchronized (mutex) {
                                mutex.notify();
                            }
                        }
                    }).start();
                }

                @Override
                public void onFailed(FileUploader.UploadMeta meta, Throwable throwable) {
                    synchronized (mutex) {
                        mutex.notify();
                    }
                }
            });

            synchronized (mutex) {
                try {
                    mutex.wait(5 * 60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            fileLabel = new FileLabel(result.getParamAsJson("fileLabel"));
        }

        if (null == fileLabel && fileCode.length() > 0) {
            fileLabel = this.getFileLabel(fileCode.toString());
        }

        if (null == fileLabel) {
            Logger.i(FileProcessor.class, "#call - Can NOT get file : " + file.getName());
            return null;
        }

        System.out.println(fileLabel.getFileURL());

        return null;
    }
}
