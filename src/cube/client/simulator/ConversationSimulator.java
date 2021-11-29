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

package cube.client.simulator;

import cell.util.Utils;
import cell.util.log.LogLevel;
import cell.util.log.LogManager;
import cube.client.CubeClient;
import cube.client.listener.MessageReceiveListener;
import cube.client.listener.MessageSendListener;
import cube.client.tool.MessageIterator;
import cube.client.tool.MessageTools;
import cube.common.action.MessagingAction;
import cube.common.entity.Contact;
import cube.common.entity.Message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * 会话模拟器。
 */
public class ConversationSimulator implements MessageReceiveListener, MessageSendListener {

    private final static String HELP = "help";

    private final static String LIST = "list";

    private final static String SEND = "send";

    private final static String READ = "read";

    private String printPrefix = "#";

    private CubeClient client;

    private String exitCommand;

    private Scanner scanner;

    private Contact self;

    private Contact partner;

    private List<Message> messageList;

    public ConversationSimulator(CubeClient client) {
        this(client, null, null);
    }

    public ConversationSimulator(CubeClient client, Contact self, Contact partner) {
        LogManager.getInstance().setLevel(LogLevel.INFO);
        this.client = client;
        this.messageList = new ArrayList<>();
        this.bindConversation(self, partner);
    }

    public void bindConversation(Contact self, Contact partner) {
        this.self = self;
        this.partner = partner;

        this.printPrefix = "cube:" + self.getId().toString() + "$ ";
    }

    public void runInConsole(String exitCommand) {
        this.exitCommand = exitCommand;

        System.out.println("Run conversation simulator in console mode");
        System.out.println("----------------------------------------------");
        System.out.println("Self   : " + this.self.getId());
        System.out.println("Partner: " + this.partner.getId());
        System.out.println("----------------------------------------------");

        if (!this.client.isReady()) {
            System.out.println(this.printPrefix + "waiting for ready...");
        }
        while (!this.client.isReady()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.start();
        System.out.println(this.printPrefix + "simulator is ready.");

        String input = "";
        this.scanner = new Scanner(System.in);
        while (!input.equals(exitCommand)) {
            System.out.print(this.printPrefix);
            input = scanner.nextLine();
            // 执行命令
            exeCommand(input);
        }

        this.stop();
    }

    private void start() {
        (new Thread(() -> {
            listMessages();
        })).start();

        this.client.registerMessageReceiveListener(this.self, this);
        this.client.registerMessageSendListener(this.self, this);
    }

    private void stop() {
        this.client.deregisterMessageReceiveListener(this.self);
        this.client.deregisterMessageSendListener(this.self);
    }

    private void listMessages() {
        this.messageList.clear();

        long beginning = System.currentTimeMillis() - 30l * 24l * 60l * 60l * 1000l;
        MessageIterator iterator = this.client.queryMessages(beginning, this.self);
        while (iterator.hasNext()) {
            Message message = iterator.next();
            if (message.getSource().longValue() > 0) {
                continue;
            }

            if (message.getTo().longValue() == this.partner.getId().longValue() ||
                    message.getFrom().longValue() == this.partner.getId().longValue()) {
                this.messageList.add(message);
            }
        }
    }

    private void exeCommand(String input) {
        String[] data = input.split(" ");
        String command = data[0];

        if (LIST.equals(command)) {
            if (data.length > 1) {
                if (data[1].equalsIgnoreCase("-reset")) {
                    this.listMessages();
                }
            }

            System.out.println("List last messages, total is " + this.messageList.size());
            System.out.println("-------------------------------------------------------");
            for (Message message : this.messageList) {
                StringBuilder buf = new StringBuilder();
                printMessage(buf, message);
                System.out.println(buf.toString());
            }
            System.out.println("-------------------------------------------------------");
        }
        else if (SEND.equals(command)) {
            if (data.length <= 1) {
                System.out.println("Params error");
                return;
            }

            StringBuilder text = new StringBuilder();
            for (int i = 1; i < data.length; ++i) {
                text.append(data[i]);
            }

            // 发送消息
            this.client.pushMessageWithPretender(this.partner, this.self, MessageTools.buildHypertextMessagePayload(text.toString()));

            System.out.println("Send message: \"" + text.toString() + "\"");
        }
        else if (READ.equals(command)) {
            // 标记为已读
            List<Message> result = null;

            if (data.length <= 1) {
                result = this.client.markRead(this.self, this.partner, this.messageList);
            }
            else {
                // 标记指定 ID 的消息
                List<Message> list = new ArrayList<>();
                for (int i = 1; i < data.length; ++i) {
                    Message message = findMessage(Long.parseLong(data[i]));
                    if (null != message) {
                        list.add(message);
                    }
                }
                result = this.client.markRead(this.self, this.partner, list);
            }

            if (null != result) {
                System.out.println("Mark read messages: " + result.size());
                System.out.println("-------------------------------------------------------");
                for (Message message : result) {
                    StringBuilder buf = new StringBuilder();
                    printMessage(buf, message);
                    System.out.println(buf.toString());
                }
                System.out.println("-------------------------------------------------------");
            }
            else {
                System.out.println("Mark read error");
            }
        }
        else if (this.exitCommand.equals(command)) {
            System.out.println("Exit simulator");
        }
        else if (HELP.equals(command)) {
            StringBuilder buf = new StringBuilder();
            printUsage(buf);
            System.out.println(buf.toString());
        }
    }

    private Message findMessage(Long id) {
        for (Message message : this.messageList) {
            if (message.getId().longValue() == id.longValue()) {
                return message;
            }
        }
        return null;
    }

    @Override
    public void onReceived(Message message) {
        this.messageList.add(message);

        StringBuilder buf = new StringBuilder();
        buf.append("Received message: \n");
        printMessage(buf, message);
        System.out.println();
        System.out.println(buf.toString());
        System.out.println();
        System.out.print(this.printPrefix);
    }

    @Override
    public void onSent(Message message) {
        this.messageList.add(message);

        StringBuilder buf = new StringBuilder();
        buf.append("Send message: \n");
        printMessage(buf, message);
        System.out.println();
        System.out.println(buf.toString());
        System.out.println();
        System.out.print(this.printPrefix);
    }

    private void printMessage(StringBuilder buf, Message message) {
        buf.append(message.getId()).append(" - ");
        buf.append(message.getFrom()).append(" -> ").append(message.getTo()).append(" - ");
        buf.append(Utils.gsDateFormat.format(new Date(message.getRemoteTimestamp()))).append(" (");
        buf.append(message.getState().name()).append(") : ");
        if (null != message.getPayload()) {
            buf.append(message.getPayload().toString());
        }
        else {
            buf.append("none");
        }
    }

    private void printUsage(StringBuilder buf) {
        buf.append("Cube conversation simulator usage:\n");
        buf.append("\t").append(LIST).append(" [-reset]\t").append("List last 30 days messages.").append("\n");
        buf.append("\t").append(SEND).append(" <text>  \t").append("Send text as text message.").append("\n");
        buf.append("\t").append(READ).append(" [mid]   \t").append("Mark message read state.").append("\n");
        buf.append("\t").append(this.exitCommand).append("\t\t\t").append("Exit simulator.").append("\n");
    }
}
