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

import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.client.Client;
import cube.common.entity.Contact;

/**
 * 账号工具。
 */
public final class ContactTools {

    private String serverAddress;

    private String user;

    private String password;

    public ContactTools(String serverAddress) {
        this.serverAddress = serverAddress;
        this.user = "admin";
        this.password = "shixincube.com";
    }

    /**
     * 添加联系人。
     *
     * @param contact
     * @return
     */
    public Contact newContact(Contact contact) {
        Logger.i(this.getClass(), "#newContact - create client: " + this.serverAddress);
        Client client = new Client(this.serverAddress, this.user, this.password);

        if (!client.waitReady()) {
            Logger.w(this.getClass(), "#newContact - client is not ready: " + this.serverAddress);
            return null;
        }

        Contact newContact = client.injectContact(contact);

        client.destroy();

        return newContact;
    }


    /**
     * 注入访问令牌
     *
     * @param authToken
     * @return
     */
    public AuthToken injectAuthToken(AuthToken authToken) {
        Logger.i(this.getClass(), "#injectAuthToken - create client: " + this.serverAddress);
        Client client = new Client(this.serverAddress, this.user, this.password);

        if (!client.waitReady()) {
            Logger.w(this.getClass(), "#newContact - client is not ready: " + this.serverAddress);
            return null;
        }

        AuthToken result = client.injectAuthToken(authToken);

        client.destroy();

        return result;
    }

    /**
     * 激活新账号。
     *
     * @param contactId
     * @param contactName
     * @return
     */
    public boolean activateNewContact(long contactId, String contactName) {
        Client client = new Client(this.serverAddress, this.user, this.password);
        if (!client.waitReady()) {
            Logger.w(this.getClass(), "#activateNewContact - client is not ready: " + this.serverAddress);
            return false;
        }

        // 新建联系人
        Contact contact = new Contact(contactId, "shixincube.com", contactName);
        Contact newContact = client.injectContact(contact);
        if (null == newContact) {
            Logger.w(this.getClass(), "#activateNewContact - create contact failed: " + contactId);
            return false;
        }

        Logger.i(this.getClass(), "New contact:\n" + newContact.toJSON().toString(4));

        // 注入访问令牌
        AuthToken authToken = new AuthToken(Utils.randomString(32),
                "shixincube.com", "shixin-cubeteam-opensource-appkey", contactId,
                System.currentTimeMillis(), System.currentTimeMillis() + 10 * 365 * 24 * 60 * 60 * 1000L, false);
        AuthToken newAuthToken = client.injectAuthToken(authToken);
        if (null == newAuthToken) {
            Logger.w(this.getClass(), "#activateNewContact - inject auth token failed: " + contactId);
            return false;
        }

        Logger.i(this.getClass(), "Auth token:\n" + newAuthToken.toJSON().toString(4));

        client.destroy();

        return true;
    }


    public static void main(String[] args) {
        String address = "111.203.186.243";
//        String address = "127.0.0.1";
        ContactTools tools = new ContactTools(address);

        for (int i = 0; i < 10; ++i) {
            long contactId = 67890006 + i;
            String contactName = "用户-" + (6 + i);
            if (!tools.activateNewContact(contactId, contactName)) {
                Logger.e("main", "Activate contact error: " + contactId + " - " + contactName);
            }
        }

//        long contactId = 67890005;
//
//        // 新建联系人
//        Contact contact = new Contact(contactId, "shixincube.com", "未命名用户0005");
//        Contact newContact = tools.newContact(contact);
//        if (null != newContact) {
//            System.out.println(newContact.toJSON().toString(4));
//        }
//        else {
//            System.out.println("Error");
//        }
//
//        // 注入访问令牌
//        AuthToken authToken = new AuthToken(Utils.randomString(32),
//                "shixincube.com", "shixin-cubeteam-opensource-appkey", contactId,
//                System.currentTimeMillis(), System.currentTimeMillis() + 10 * 365 * 24 * 60 * 60 * 1000L, false);
//        AuthToken newAuthToken = tools.injectAuthToken(authToken);
//        if (null != newAuthToken) {
//            System.out.println(newAuthToken.toJSON().toString(4));
//        }
//        else {
//            System.out.println("Error");
//        }
    }
}
