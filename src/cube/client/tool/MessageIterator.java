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

package cube.client.tool;

import cell.core.talk.dialect.ActionDialect;
import cube.client.Connector;
import cube.client.Notifier;
import cube.client.Receiver;
import cube.common.action.ClientAction;
import cube.common.entity.Contact;
import cube.common.entity.Message;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 消息迭代器。
 */
public class MessageIterator implements Iterator<Message> {

    private Connector connector;

    private Receiver receiver;

    /**
     * 每次请求服务器的步长。
     */
    private long step = 12 * 60L * 60L * 1000L;

    private Contact contact;

    /**
     * 迭代器的起始时间戳。
     */
    private long beginning;

    /**
     * 当前迭代的起始时间戳。
     */
    private long currentBeginning;

    /**
     * 当期迭代的结束时间戳。
     */
    private long currentEnding;

    /**
     * 迭代器可查询的截止时间。
     */
    private long terminal;

    /**
     * 游标。
     */
//    private Message cursor;

    /**
     * 游标数组。
     */
    private List<Message> cursorArray;

    /**
     * 游标索引。
     */
    private int cursorIndex;

    public MessageIterator(Connector connector, Receiver receiver) {
        this.connector = connector;
        this.receiver = receiver;
        this.terminal = System.currentTimeMillis();
        this.cursorArray = new ArrayList<>();
    }

    public void prepare(long beginning, Contact contact) {
        this.contact = contact;
        this.beginning = beginning;
        this.currentBeginning = beginning;
        this.currentEnding = Math.min(beginning + this.step, this.terminal);

        while (this.iterate()) {
            if (!this.cursorArray.isEmpty()) {
                break;
            }

            if (this.currentEnding == this.terminal) {
                // 当前结束位置已经到末尾
                break;
            }

            // 进行步进
            this.currentBeginning = this.currentEnding;
            this.currentEnding = Math.min(this.currentEnding + this.step, this.terminal);
        }

        this.cursorIndex = 0;
    }

    @Override
    public boolean hasNext() {
        // 先判断分段遍历是否结束
        if (this.currentEnding == this.terminal) {
            // 已经遍历到最后一段
            int size = this.cursorArray.size();
            if (size > 0 && this.cursorIndex < size) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            int size = this.cursorArray.size();
            if (size > 0 && this.cursorIndex < size) {
                return true;
            }

            // 清空
            this.cursorArray.clear();

            // 进行步进
            this.currentBeginning = this.currentEnding;
            this.currentEnding = Math.min(this.currentEnding + this.step, this.terminal);

            while (this.iterate()) {
                if (!this.cursorArray.isEmpty()) {
                    break;
                }

                if (this.currentEnding == this.terminal) {
                    // 当前结束位置已经到末尾
                    break;
                }

                // 进行步进
                this.currentBeginning = this.currentEnding;
                this.currentEnding = Math.min(this.currentEnding + this.step, this.terminal);
            }

            this.cursorIndex = 0;

            return !this.cursorArray.isEmpty();
        }
    }

    @Override
    public Message next() {
        if (this.cursorIndex >= this.cursorArray.size()) {
            return null;
        }

        Message message = this.cursorArray.get(this.cursorIndex);
        ++this.cursorIndex;
        return message;
    }

    private boolean iterate() {
        Notifier notifier = new Notifier();

        this.receiver.inject(notifier);

        ActionDialect actionDialect = new ActionDialect(ClientAction.QueryMessages.name);
        actionDialect.addParam("beginning", this.currentBeginning);
        actionDialect.addParam("ending", this.currentEnding);
        actionDialect.addParam("domain", this.contact.getDomain().getName());
        actionDialect.addParam("contactId", this.contact.getId().longValue());

        // 阻塞线程，并等待返回结果
        ActionDialect result = this.connector.send(notifier, actionDialect);
        if (!result.containsParam("result")) {
            // 查询失败
            return false;
        }

        JSONObject data = result.getParamAsJson("result");
        JSONArray list = data.getJSONArray("list");
        if (list.length() == 0) {
            return true;
        }

        this.cursorArray.clear();

        for (int i = 0; i < list.length(); ++i) {
            JSONObject json = list.getJSONObject(i);
            this.cursorArray.add(new Message(json));
        }

        return true;
    }
}
