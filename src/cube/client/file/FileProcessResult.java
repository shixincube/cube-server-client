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

import cube.common.action.FileProcessorAction;
import cube.common.entity.FileLabel;
import cube.common.entity.ProcessResultStream;
import cube.util.file.OCRFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件处理结果。
 */
public class FileProcessResult {

    public final String process;

    private OCRProcessResult ocrResult;

    private ProcessResultStream resultStream;

    public FileProcessResult(JSONObject json) {
        this.process = json.getString("process");

        if (FileProcessorAction.OCR.name.equals(this.process)) {
            this.ocrResult = new OCRProcessResult(json);
        }
        else if (FileProcessorAction.Snapshot.name.equals(this.process)) {

        }

        if (json.has("stream")) {
            this.resultStream = new ProcessResultStream(json.getJSONObject("stream"));
        }
    }

    public boolean hasResultStream() {
        return (null != this.resultStream);
    }

    public ProcessResultStream getResultStream() {
        return this.resultStream;
    }

    public OCRProcessResult getOCRResult() {
        return this.ocrResult;
    }

    /**
     *
     */
    public class OCRProcessResult {

        private List<String> resultText;

        private OCRFile ocr;

        private FileLabel imageFile;

        public OCRProcessResult(JSONObject json) {
            this.resultText = new ArrayList<>();

            JSONArray textArray = json.getJSONArray("text");
            for (int i = 0; i < textArray.length(); ++i) {
                this.resultText.add(textArray.getString(i));
            }

            this.ocr = new OCRFile(json.getJSONObject("ocr"));

            if (json.has("image")) {
                this.imageFile = new FileLabel(json.getJSONObject("image"));
            }
        }

        public List<String> getResultText() {
            return this.resultText;
        }

        public FileLabel getImageFile() {
            return this.imageFile;
        }
    }
}
