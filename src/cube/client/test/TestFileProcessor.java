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
import cube.client.file.FileProcessResult;
import cube.client.file.FileProcessor;
import cube.client.file.ImageProcess;

import java.io.File;

/**
 * 测试文件处理器。
 */
public class TestFileProcessor {

    public static void main(String[] args) {

        CubeClient client = new CubeClient("127.0.0.1");

        Helper.sleepInSeconds(3);

        FileProcessor fileProcessor = client.getFileProcessor();

        fileProcessor.setContactId(10000L);
        fileProcessor.setDomainName("shixincube.com");

        System.out.println("*** START OCR ***");

        FileProcessResult result = fileProcessor.call(ImageProcess.OCR, new File("data/screenshot_shixincube.jpg"));
        FileProcessResult.OCRProcessResult ocrResult = result.getOCRResult();

        for (String text : ocrResult.getResultText()) {
            System.out.println("LINE: " + text);
        }

        System.out.println("*** END ***");
        client.destroy();
    }
}
