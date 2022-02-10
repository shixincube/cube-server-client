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

import cell.core.talk.PrimitiveOutputStream;
import cube.client.Connector;
import cube.client.listener.FileUploadListener;
import cube.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private Map<String, UploadMeta> uploadMetaMap;

    public FileUploader(Connector connector) {
        this.connector = connector;
        this.uploadMetaMap = new ConcurrentHashMap<>();
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
        String fileCode = FileUtils.makeFileCode(contactId, domain, file.getName());
        UploadMeta uploadMeta = new UploadMeta(contactId, domain, file, fileCode, listener);

        this.uploadMetaMap.put(fileCode, uploadMeta);

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

                    uploadMeta.md5.update(bytes, 0, length);
                    uploadMeta.sha1.update(bytes, 0, length);

                    totalSize += length;
                    uploadMeta.fireUploading(totalSize);
                }

                outputStream.flush();
            } catch (IOException e) {
                uploadMeta.fireFailed(e);
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
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            uploadMetaMap.remove(fileCode);
            uploadMeta.fireCompleted();
        })).start();
    }


    /**
     * 信息描述。
     */
    public class UploadMeta {

        public final Long contactId;

        public final String domain;

        public final File file;

        public final String fileCode;

        protected MessageDigest md5;

        private String md5Code;

        protected MessageDigest sha1;

        private String sha1Code;

        protected final FileUploadListener listener;

        UploadMeta(Long contactId, String domain, File file, String fileCode, FileUploadListener listener) {
            this.contactId = contactId;
            this.domain = domain;
            this.file = file;
            this.fileCode = fileCode;
            try {
                this.md5 = MessageDigest.getInstance("MD5");
                this.sha1 = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            this.listener = listener;
        }

        public String getMD5Code() {
            if (null == this.md5Code) {
                byte[] hashMD5 = this.md5.digest();
                this.md5Code = FileUtils.bytesToHexString(hashMD5);
            }
            return this.md5Code;
        }

        public String getSHA1Code() {
            if (null == this.sha1Code) {
                byte[] hashSHA1 = this.sha1.digest();
                this.sha1Code = FileUtils.bytesToHexString(hashSHA1);
            }
            return this.sha1Code;
        }

        protected void fireUploading(long processedSize) {
            this.listener.onUploading(this, processedSize);
        }

        protected void fireCompleted() {
            this.listener.onCompleted(this);
        }

        protected void fireFailed(Throwable throwable) {
            this.listener.onFailed(this, throwable);
        }
    }
}
