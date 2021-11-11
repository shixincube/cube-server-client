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

package cube.client.test;


import cube.client.CubeClient;
import cube.client.listener.FileUploadListener;
import cube.client.tool.FileUploader;

import java.io.File;

/**
 * 测试推送消息。
 */
public class TestFileUpload {


    public static void main(String[] args) {

        CubeClient client = new CubeClient("127.0.0.1");

        Helper.sleepInSeconds(3);

        Long contactId = 50001001L;

        File targetFile = new File("data/cube-framework.png");

        System.out.println("[TestFileUpload] Upload file : " + targetFile.getName());

        client.getFileUploader().upload(contactId, "shixincube.com", targetFile, new FileUploadListener() {
            @Override
            public void onUploading(FileUploader.UploadMeta meta, long processedSize) {
                System.out.println("[TestFileUpload] Uploading : " + processedSize + "/" + meta.file.length());
            }

            @Override
            public void onCompleted(FileUploader.UploadMeta meta) {
                System.out.println("[TestFileUpload] Completed : " + meta.fileCode);

                synchronized (targetFile) {
                    targetFile.notify();
                }
            }

            @Override
            public void onFailed(FileUploader.UploadMeta meta, Throwable throwable) {
                throwable.printStackTrace();

                synchronized (targetFile) {
                    targetFile.notify();
                }
            }
        });

        System.out.println("[TestFileUpload] Start upload: " + contactId);

        synchronized (targetFile) {
            try {
                targetFile.wait(5L * 60L * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("** END ***");
        client.destroy();
    }
}
