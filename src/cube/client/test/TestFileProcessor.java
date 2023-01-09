/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

import cube.client.Client;
import cube.client.file.FileProcessor;
import cube.client.file.OCRProcessing;
import cube.client.file.OfficeConvertToProcessing;
import cube.client.file.VideoProcessing;
import cube.client.listener.WorkflowListener;
import cube.common.entity.Contact;
import cube.common.entity.FileLabel;
import cube.file.FileProcessResult;
import cube.file.OCRFile;
import cube.file.OperationWork;
import cube.file.OperationWorkflow;
import cube.file.operation.*;
import cube.util.FileType;
import cube.vision.Size;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

/**
 * 测试文件处理器。
 */
public class TestFileProcessor {

    public static void testVideoFile(FileProcessor fileProcessor) {
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

    public static void testOCR(FileProcessor fileProcessor) {
        System.out.println("*** START OCR ***");

        FileProcessResult result = fileProcessor.call(new OCRProcessing(), new File("data/screenshot_20220307.png"));
        FileProcessResult.OCRProcessResult ocrResult = result.getOCRResult();

        for (String text : ocrResult.getResultText()) {
            System.out.println("LINE: " + text);
        }

        System.out.println("*** END ***");
    }

    public static void testSnapshotVideo(FileProcessor fileProcessor) {
        System.out.println("*** START Snapshot ***");

        SnapshotOperation operation = new SnapshotOperation();

        FileProcessResult result = fileProcessor.call(new VideoProcessing(operation), new File("data/video.mp4"));
        System.out.println("Result : " + result.getVideoResult().getOperation().getOperation());
        System.out.println("Result file : " + result.getLocalFileList().get(0).getName());

        if (result.getVideoResult().successful) {
            List<String> logs = result.getLogs();
            for (String line : logs) {
                System.out.println("[L] " + line);
            }
        }

        System.out.println("*** END ***");
    }

    public static void testExtractAudioVideo(FileProcessor fileProcessor) {
        System.out.println("*** START Extract Audio ***");

        ExtractAudioOperation operation = new ExtractAudioOperation();
        operation.outputType = FileType.WAV;

        FileProcessResult result = fileProcessor.call(new VideoProcessing(operation),
                new File("data/video_douyin_yuebing.mp4"));
        if (null == result) {
            System.out.println("*** ERROR ***");
            return;
        }

        System.out.println("Result file : " + result.getResultList().get(0).fullPath);
        System.out.println("Media channels : " + result.getResultList().get(0).mediaAttribute.getStreamAttributes()[0].channels);
        System.out.println("Media sample rate : " + result.getResultList().get(0).mediaAttribute.getStreamAttributes()[0].sampleRate);
        System.out.println("Media byte width : " + result.getResultList().get(0).mediaAttribute.getStreamAttributes()[0].bitsPerSample);

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
            public void onWorkBegun(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkBegun");
            }

            @Override
            public void onWorkEnded(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkEnded");
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

    public static void testSteganography(FileProcessor fileProcessor) {
        System.out.println("*** START testSteganography ***");

        Object mutex = new Object();

        StringBuilder resultFilename = new StringBuilder();

        fileProcessor.register(new WorkflowListener() {
            @Override
            public void onWorkflowStarted(OperationWorkflow workflow) {
                if (((SteganographyOperation) workflow.getFirstWork().getFileOperation()).isRecover()) {
                    System.out.println("#onWorkflowStarted - Recover");
                }
                else {
                    System.out.println("#onWorkflowStarted");
                }
            }

            @Override
            public void onWorkflowStopped(OperationWorkflow workflow) {
                resultFilename.delete(0, resultFilename.length());
                System.out.println("#onWorkflowStopped : " + workflow.getResultFilename());
                resultFilename.append(workflow.getResultFilename());

                synchronized (mutex) {
                    mutex.notify();
                }
            }

            @Override
            public void onWorkBegun(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkBegun");
            }

            @Override
            public void onWorkEnded(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkEnded");
            }
        });

        OperationWorkflow workflow = new OperationWorkflow();
        SteganographyOperation operation = new SteganographyOperation("来自魔方\n隐写数据");
        workflow.append(new OperationWork(operation));

        Size markSize = operation.getWatermarkSize();

        System.out.println("Watermark size: " + markSize.toString());

        // 执行工作流
        FileProcessResult result = fileProcessor.call(workflow, new File("data/zhong.png"));
        System.out.println("#testSteganography - result : " + result.getSubmitWorkflowResult().successful);

        synchronized (mutex) {
            try {
                mutex.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Recover steganography watermark");

        workflow = new OperationWorkflow();
        operation = new SteganographyOperation(markSize);
        workflow.append(new OperationWork(operation));

        // 执行工作流
        result = fileProcessor.call(workflow, new File(fileProcessor.getFilePath(), resultFilename.toString()));
        System.out.println("#testSteganography (recover) - result : " + result.getSubmitWorkflowResult().successful);

        synchronized (mutex) {
            try {
                mutex.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        File resultFile = new File(fileProcessor.getFilePath(), resultFilename.toString());
        if (!resultFile.exists()) {
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("*** END ***");
    }

    public static void testReadSteganographyWatermark(FileProcessor fileProcessor) {
        System.out.println("*** START testReadSteganographyWatermark ***");

        File sourceFile = new File("data/zhong_stegano.png");

        Object mutex = new Object();

        StringBuilder resultFilename = new StringBuilder();

        fileProcessor.register(new WorkflowListener() {
            @Override
            public void onWorkflowStarted(OperationWorkflow workflow) {
                System.out.println("#onWorkflowStarted");
            }

            @Override
            public void onWorkflowStopped(OperationWorkflow workflow) {
                System.out.println("#onWorkflowStopped : " + workflow.getResultFilename());
                resultFilename.append(workflow.getResultFilename());

                synchronized (mutex) {
                    mutex.notify();
                }
            }

            @Override
            public void onWorkBegun(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkBegun");
            }

            @Override
            public void onWorkEnded(OperationWorkflow workflow, OperationWork work) {
                System.out.println("#onWorkEnded");
            }
        });

        OperationWorkflow workflow = new OperationWorkflow();

        SteganographyOperation operation = new SteganographyOperation(new Size(150, 106));
        workflow.append(new OperationWork(operation));

        OCROperation ocrOperation = new OCROperation();
        workflow.append(new OperationWork(ocrOperation));

        // 执行工作流
        FileProcessResult result = fileProcessor.call(workflow, sourceFile);
        System.out.println("#testReadSteganographyWatermark - result : " + result.getSubmitWorkflowResult().successful);

        synchronized (mutex) {
            try {
                mutex.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        File resultFile = new File(fileProcessor.getFilePath(), resultFilename.toString());
        if (!resultFile.exists()) {
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("--------------------------------------------------------------------------------");
        OCRFile ocrFile = new OCRFile(resultFile);
        for (String text : ocrFile.toText()) {
            System.out.println(text);
        }
        System.out.println("--------------------------------------------------------------------------------");

        System.out.println("*** END ***");
    }

    public static void testDownloadFile(FileProcessor fileProcessor) {
        System.out.println("*** START testDownloadFile ***");

        String fileCode = "xwwwwrLABQBGvNuxuvHFrFBGBuMQMrxwwwwHYBmNkRPMiKOxaPZJJZPaxOKiMPRk";
        FileLabel fileLabel = fileProcessor.downloadFile(fileCode);
        if (null != fileLabel) {
            System.out.println("Download file finished: " + fileLabel.getFile().getAbsolutePath());
        }
        else {
            System.out.println("Download file error");
        }
    }

    public static void testOCRWorkflow(FileProcessor fileProcessor) {
        System.out.println("*** START testOCRWorkflow ***");

        OperationWorkflow workflow = new OperationWorkflow();

        GrayscaleOperation grayscaleOperation = new GrayscaleOperation();
        workflow.append(new OperationWork(grayscaleOperation));

        /*BrightnessOperation brightnessOperation = new BrightnessOperation(3, 10);
        workflow.append(new OperationWork(brightnessOperation));

        SharpeningOperation sharpening = new SharpeningOperation(2.0);
        workflow.append(new OperationWork(sharpening));

        List<ReplaceColorOperation> rcoList = new ArrayList<>();
        ReplaceColorOperation replace1 = new ReplaceColorOperation(new Color(193, 193, 193),
                new Color(134, 134, 134));
        replace1.setFuzzFactor(3);
        rcoList.add(replace1);
        workflow.append(new OperationWork(new ReplaceColorOperation(rcoList)));*/

        OCROperation ocrOperation = new OCROperation(OCROperation.LANG_CHINESE, true);
        workflow.append(new OperationWork(ocrOperation));

        File file = new File("data/date_monday.png");
        FileProcessResult result = fileProcessor.call(workflow, file);

        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /*File resultFile = result.getResultFile();
        OCRFile ocrFile = new OCRFile(resultFile);
        for (String line : ocrFile.toText()) {
            System.out.println("[L]: " + line);
        }*/
    }

    public static void testSteganoHandler(FileProcessor fileProcessor) {
        System.out.println("*** START testSteganoHandler ***");

        String url = "http://127.0.0.1:7010/file/steganographic/";

        // 进行编码
        JSONObject requestData = new JSONObject();
        requestData.put("content", "今天是周日");

        JSONObject responseData = Helper.httpPost(url, requestData);
        if (null == responseData) {
            System.out.println("POST Error");
            return;
        }

        JSONObject data = responseData.getJSONObject("data");
        String mask = data.getString("maskCode");
        String fileCode = data.getJSONObject("fileLabel").getString("fileCode");
        System.out.println("FileCode: " + fileCode);
        System.out.println("Mask: " + mask);

        // 下载文件
        FileLabel fileLabel = fileProcessor.downloadFile(fileCode);
        System.out.println("Download file: " + fileLabel.getFileName());

        // 解码还原
        url += "?fc=" + fileCode + "&mask=" + mask;
        responseData = Helper.httpGet(url);
        if (null == responseData) {
            System.out.println("GET Error");
            return;
        }

        data = responseData.getJSONObject("data");
        String contentFileCode = data.getJSONObject("fileLabel").getString("fileCode");
        System.out.println("Content file code: " + contentFileCode);
        // 下载文件
        fileLabel = fileProcessor.downloadFile(contentFileCode);
        System.out.println("Download file: " + fileLabel.getFileName());
    }

    public static void testOfficeConvertTo(FileProcessor fileProcessor) {
        System.out.println("*** START OfficeConvertTo ***");

        FileProcessResult result = fileProcessor.call(new OfficeConvertToProcessing(OfficeConvertToOperation.OUTPUT_FORMAT_PNG),
                new File("data/简历模板.docx"));

        if (result.hasResult()) {
            for (File file : result.getLocalFileList()) {
                System.out.println("File: " + file.getName());
            }
        }
        else {
            System.out.println("No result");
        }

        System.out.println("*** END ***");
    }

    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", "admin", "shixincube.com");

        if (!client.waitReady()) {
            client.destroy();
            return;
        }

        Contact contact = new Contact(10000, "shixincube.com");
        client.prepare(contact);

        FileProcessor fileProcessor = client.getFileProcessor();

//        testOCRWorkflow(fileProcessor);

//        testDownloadFile(fileProcessor);

//        testSteganography(fileProcessor);
//        testReadSteganographyWatermark(fileProcessor);

//        testSteganoHandler(fileProcessor);

//        testOfficeConvertTo(fileProcessor);

//        testSnapshotVideo(fileProcessor);
        testExtractAudioVideo(fileProcessor);

        client.destroy();
    }
}
