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

package cube.client.tool;

import cell.core.talk.dialect.ActionDialect;
import cube.auth.AuthToken;
import cube.client.Connector;
import cube.client.Notifier;
import cube.client.Receiver;
import cube.common.action.ClientAction;
import cube.common.state.AuthStateCode;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌工具。
 */
public final class TokenTools {

    private final static ConcurrentHashMap<Long, AuthToken> sContactTokenMap = new ConcurrentHashMap<>();

    private TokenTools() {
    }

    /**
     * 将令牌数据写入文件。
     *
     * @param token
     * @param outputFile
     * @return
     */
    public final static boolean saveAuthToken(AuthToken token, String outputFile) {
        Path file = Paths.get(outputFile);

        try {
            Files.write(file, token.toJSON().toString(4).getBytes(Charset.forName("UTF-8")),
                    StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Files.exists(file);
    }

    /**
     * 从令牌数据文件里读取令牌。
     *
     * @param file
     * @return
     */
    public final static AuthToken loadAuthToken(String file) {
        AuthToken authToken = null;

        try {
            byte[] data = Files.readAllBytes(Paths.get(file));
            String jsonString = new String(data, Charset.forName("UTF-8"));
            JSONObject json = new JSONObject(jsonString);
            authToken = new AuthToken(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return authToken;
    }

    /**
     * 获取指定联系人的访问令牌。
     *
     * @param connector
     * @param receiver
     * @param contactId
     * @return
     */
    public static AuthToken getAuthToken(Connector connector, Receiver receiver, Long contactId) {
        AuthToken token = sContactTokenMap.get(contactId);
        if (null != token) {
            return token;
        }

        if (!connector.isConnected()) {
            return null;
        }

        Notifier notifier = receiver.inject();
        ActionDialect actionDialect = new ActionDialect(ClientAction.GetAuthToken.name);
        actionDialect.addParam("contactId", contactId);

        // 发送请求并等待结果
        ActionDialect result = connector.send(notifier, actionDialect);
        if (result.getParamAsInt("code") != AuthStateCode.Ok.code) {
            return null;
        }

        token = new AuthToken(result.getParamAsJson("token"));
        return token;
    }
}
