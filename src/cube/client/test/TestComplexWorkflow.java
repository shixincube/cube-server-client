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
import cube.client.listener.WorkflowListener;
import cube.common.entity.Contact;
import cube.file.OCRFile;
import cube.file.OperationWork;
import cube.file.OperationWorkflow;
import cube.file.operation.*;
import cube.util.FileUtils;
import cube.vision.BoundingBox;
import cube.vision.Color;

import java.io.File;

public class TestComplexWorkflow {

    public static void test(FileProcessor fileProcessor) {
        System.out.println("*** START test ***");

        Object mutex = new Object();

        fileProcessor.register(new WorkflowListener() {
            @Override
            public void onWorkflowStarted(OperationWorkflow workflow) {
                System.out.println("#onWorkflowStarted");
            }

            @Override
            public void onWorkflowStopped(OperationWorkflow workflow) {
                System.out.println("#onWorkflowStopped");

                System.out.println("Result file: " + workflow.getResultFilename());

                File file = new File("data", workflow.getResultFilename());
                if (FileUtils.extractFileExtension(file.getName()).equalsIgnoreCase("ocr")) {
                    OCRFile ocrFile = new OCRFile(file);

//                    parseOCRData(ocrFile);

                    System.out.println("--------------------------------");
                    for (String line : ocrFile.toText()) {
                        System.out.println(line);
                    }
                    System.out.println("--------------------------------");
                }

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

//        CropOperation cropOperation = new CropOperation(224, 126, 414, 1496);
//        workflow.append(new OperationWork(cropOperation));

//        ReplaceColorOperation replace1 = new ReplaceColorOperation(new Color(180,180,180),
//                new Color(233,233,233), 10);
//        workflow.append(new OperationWork(replace1));

//        ReplaceColorOperation replace2 = new ReplaceColorOperation(new Color(154,154,154),
//                new Color(233,233,233), 10);
//        workflow.append(new OperationWork(replace2));

//        BrightnessOperation brightnessOperation = new BrightnessOperation(5, 20);
//        workflow.append(new OperationWork(brightnessOperation));

        SharpeningOperation sharpeningOperation = new SharpeningOperation(3.0);
        workflow.append(new OperationWork(sharpeningOperation));

        GrayscaleOperation grayscaleOperation = new GrayscaleOperation();
        workflow.append(new OperationWork(grayscaleOperation));

        OCROperation ocrOperation = new OCROperation();
        workflow.append(new OperationWork(ocrOperation));

        fileProcessor.call(workflow, new File("data/wechat.png"));

        synchronized (mutex) {
            try {
                mutex.wait(2 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("*** END test ***");
    }

    public static void parseOCRData(OCRFile file) {
        int prevY = 0;
        for (OCRFile.Page page : file.getPages()) {
            for (OCRFile.Area area : page.getAreas()) {
                for (OCRFile.Part part : area.getParts()) {
                    for (OCRFile.Line line : part.getLines()) {
                        System.out.println("line: " + line.toText());
                        BoundingBox bbox = line.getBoundingBox();
                        if (prevY > 0) {
                            System.out.println("dY : " + (bbox.y - prevY));
                        }

                        prevY = bbox.y;
                        System.out.println();
                    }
                }
            }
        }
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

        test(fileProcessor);

        client.destroy();
    }
}
