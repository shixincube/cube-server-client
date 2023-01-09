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

package cube.client;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import org.json.JSONObject;

/**
 * 同步通知器。
 */
public class Notifier {

    public final static String ParamName = "_notifier";

    public final static String AsyncParamName = "_async_notifier";

    public final long sn;

    private ActionDialect response;

    public Notifier() {
        this.sn = Utils.generateSerialNumber();
    }

    /**
     * 阻塞当前线程等待通知。
     *
     * @return 返回返回的原语。
     */
    public ActionDialect waiting() {
        synchronized (this) {
            try {
                this.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return this.response;
    }

    /**
     * 结束当前通知的阻塞。
     *
     * @param data 由服务器带回的数据。
     */
    public void over(ActionDialect data) {
        this.response = data;

        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
     * 判断 JSON 数据是否与该通知器一致。
     *
     * @param json
     * @return
     */
    public boolean equals(JSONObject json) {
        if (json.has("sn")) {
            return this.sn == json.getLong("sn");
        }

        return false;
    }

    /**
     * 转为 JSON 结构。
     *
     * @return
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        return json;
    }

    public static boolean equals(Notifier notifier, JSONObject json) {
        return notifier.equals(json);
    }
}
