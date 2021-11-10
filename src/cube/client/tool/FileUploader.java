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

package cube.client.tool;

import cell.core.talk.PrimitiveOutputStream;
import cube.client.Connector;
import cube.client.listener.FileUploadListener;
import cube.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件上传器。
 */
public class FileUploader {

    private Connector connector;

    private int bufferSize = 128 * 1024;

    /**
     * 流名称对应的上传器。
     */
    private Map<String, Uploader> uploaderMap;

    public FileUploader(Connector connector) {
        this.connector = connector;
        this.uploaderMap = new ConcurrentHashMap<>();
    }

    /**
     * 以指定联系人的身份上传文件。
     *
     * @param contactId
     * @param domain
     * @param file
     * @param listener
     */
    public void upload(Long contactId, String domain, File file, FileUploadListener listener) {
        Uploader uploader = new Uploader(contactId, domain, file, listener);
        String fileCode = FileUtils.makeFileCode(contactId, domain, file.getName());

        this.uploaderMap.put(fileCode, uploader);

        (new Thread(() -> {
            PrimitiveOutputStream outputStream = connector.sendStream(fileCode);
            FileInputStream fis = null;

            long totalSize = 0;

            try {
                fis = new FileInputStream(file);
                byte[] bytes = new byte[bufferSize];
                int length = 0;
                while ((length = fis.read(bytes)) > 0) {
                    outputStream.write(bytes, 0, length);

                    totalSize += length;
                    uploader.fireUploading(fileCode, totalSize);
                }

                outputStream.flush();
            } catch (IOException e) {
                uploader.fireFailed(fileCode, e);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }

                if (null != outputStream) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                    }
                }
            }

            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            uploader.fireCompleted(fileCode);
        })).start();
    }


    /**
     * 信息描述。
     */
    protected class Uploader {

        protected final Long contactId;

        protected final String domain;

        protected final File file;

        protected final FileUploadListener listener;

        Uploader(Long contactId, String domain, File file, FileUploadListener listener) {
            this.contactId = contactId;
            this.domain = domain;
            this.file = file;
            this.listener = listener;
        }

        protected void fireUploading(String fileCode, long processedSize) {
            this.listener.onUploading(fileCode, processedSize, this.contactId, this.domain, this.file);
        }

        protected void fireCompleted(String fileCode) {
            this.listener.onCompleted(fileCode, this.contactId, this.domain, this.file);
        }

        protected void fireFailed(String fileCode, Throwable throwable) {
            this.listener.onFailed(fileCode, this.contactId, this.domain, this.file, throwable);
        }
    }
}
