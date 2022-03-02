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
import cube.client.file.FileProcessor;
import cube.client.file.OCRProcessing;
import cube.client.file.VideoProcessing;
import cube.client.listener.WorkflowListener;
import cube.common.entity.Contact;
import cube.common.entity.FileLabel;
import cube.file.*;
import cube.vision.Color;
import cube.vision.Size;

import java.io.File;
import java.util.List;

/**
 * 测试文件处理器。
 */
public class TestFileProcessor {

    public static void testFileLabel(FileProcessor fileProcessor) {
        System.out.println("*** START ***");

        File file = new File("data/video.mp4");
        FileLabel fileLabel = fileProcessor.getFileLabel(file);
        if (null != fileLabel) {
            System.out.println("Find file: " + file.getName() + " - " + fileLabel.getFileCode());

            String url = fileProcessor.prepareM3U8(fileLabel.getFileCode(), "http://127.0.0.1:7010");
            if (null != url) {
                System.out.println("URL : " + url);
            }
            else {
                System.out.println("Can NOT create M3U8 : " + file.getName());
            }
        }
        else {
            System.out.println("Not find file: " + file.getName());
        }

        System.out.println("*** END ***");
    }

    public static void testORC(FileProcessor fileProcessor) {
        System.out.println("*** START OCR ***");

        FileProcessResult result = fileProcessor.call(new OCRProcessing(), new File("data/screenshot_shixincube.jpg"));
        FileProcessResult.OCRProcessResult ocrResult = result.getOCRResult();

        for (String text : ocrResult.getResultText()) {
            System.out.println("LINE: " + text);
        }

        System.out.println("*** END ***");
    }

    public static void testSnapshot(FileProcessor fileProcessor) {
        System.out.println("*** START Snapshot ***");

        SnapshotOperation operation = new SnapshotOperation();
        FileProcessResult result = fileProcessor.call(new VideoProcessing(operation), new File("data/video.mp4"));
        System.out.println("Result : " + result.getVideoResult().getOperation().getOperation());

        if (result.getVideoResult().successful) {
            List<String> logs = result.getLogs();
            for (String line : logs) {
                System.out.println("[L] " + line);
            }
        }

        System.out.println("*** END ***");
    }

    public static void testWorkFlow(FileProcessor fileProcessor) {
        System.out.println("*** START testWorkFlow ***");

        Object mutex = new Object();

        fileProcessor.register(new WorkflowListener() {
            @Override
            public void onWorkflowStarted(OperationWorkflow workflow) {
                System.out.println("#onWorkflowStarted");
            }

            @Override
            public void onWorkflowStopped(OperationWorkflow workflow) {
                System.out.println("#onWorkflowStopped");

                synchronized (mutex) {
                    mutex.notify();
                }
            }

            @Override
            public void onWorkStarted(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkStarted");
            }

            @Override
            public void onWorkStopped(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkStopped");
            }
        });

        OperationWorkflow workflow = new OperationWorkflow();

        SnapshotOperation ssOperation = new SnapshotOperation();
        workflow.append(new OperationWork(ssOperation));

//        EliminateColorOperation ecOperation = new EliminateColorOperation(new Color("#FFFFFF"), new Color("#000000"));
//        workflow.append(new OperationWork(ecOperation));

        ReverseColorOperation rcOperation = new ReverseColorOperation();
        workflow.append(new OperationWork(rcOperation));

        // 发起
//        FileProcessResult result = fileProcessor.call(workflow, new File("data/sc.jpg"));
        FileProcessResult result = fileProcessor.call(workflow, new File("data/video.mp4"));
        System.out.println("#testWorkFlow - result : " + result.getSubmitWorkflowResult().successful);

        synchronized (mutex) {
            try {
                mutex.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("*** END ***");
    }

    public static void testSteganographyWithWorkFlow(FileProcessor fileProcessor) {
        System.out.println("*** START testSteganographyWithWorkFlow ***");

        Object mutex = new Object();

        fileProcessor.register(new WorkflowListener() {
            @Override
            public void onWorkflowStarted(OperationWorkflow workflow) {
                System.out.println("#onWorkflowStarted");
            }

            @Override
            public void onWorkflowStopped(OperationWorkflow workflow) {
                System.out.println("#onWorkflowStopped : " + workflow.getResultFilename());

                synchronized (mutex) {
                    mutex.notify();
                }
            }

            @Override
            public void onWorkStarted(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkStarted");
            }

            @Override
            public void onWorkStopped(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkStopped");
            }
        });

        OperationWorkflow workflow = new OperationWorkflow();

        SteganographyOperation operation = new SteganographyOperation("来自魔方\n隐写数据",
                new Size(100, 100));

        workflow.append(new OperationWork(operation));

        // 发起
        FileProcessResult result = fileProcessor.call(workflow, new File("data/zhong.png"));
        System.out.println("#testSteganographyWithWorkFlow - result : " + result.getSubmitWorkflowResult().successful);

        synchronized (mutex) {
            try {
                mutex.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("*** END ***");
    }

    public static void main(String[] args) {

        CubeClient client = new CubeClient("127.0.0.1", "admin", "shixincube.com");

        if (!client.waitReady()) {
            client.destroy();
            return;
        }

        Contact contact = new Contact(10000, "shixincube.com");
        client.pretend(contact);

        FileProcessor fileProcessor = client.getFileProcessor();

        testSteganographyWithWorkFlow(fileProcessor);

        client.destroy();
    }
}
