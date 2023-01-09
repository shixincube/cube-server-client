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

import cell.core.net.Endpoint;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.client.Client;
import cube.common.entity.AuthDomain;

/**
 * 新建域。
 */
public class DomainTools {

    private String serverAddress;

    private String user;

    private String password;

    public DomainTools(String serverAddress) {
        this.serverAddress = serverAddress;
        this.user = "admin";
        this.password = "shixincube.com";
    }

    public void setUser(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public void newDomain(String domainName, String appKey, String appId, Endpoint mainEndpoint,
                        Endpoint httpEndpoint, Endpoint httpsEndpoint, boolean ferry) {
        Logger.i(this.getClass(), "#newDomain - create client: " + this.serverAddress);
        Client client = new Client(this.serverAddress, this.user, this.password);

        if (!client.waitReady()) {
            Logger.w(this.getClass(), "#newDomain - client is not ready: " + this.serverAddress);
            return;
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AuthDomain authDomain = client.createDomainApp(domainName, appKey, appId,
                mainEndpoint, httpEndpoint, httpsEndpoint, ferry);
        if (null != authDomain) {
            Logger.i(this.getClass(), "#newDomain - create auth domain success - " + authDomain.domainName);
        }
        else {
            Logger.w(this.getClass(), "#newDomain - create auth domain failed - " + domainName);
        }

        client.destroy();
        Logger.i(this.getClass(), "#newDomain - end: " + this.serverAddress);
    }

    public void updateDomain(String domainName, Endpoint mainEndpoint,
                             Endpoint httpEndpoint, Endpoint httpsEndpoint) {
        Logger.i(this.getClass(), "#updateDomain - create client: " + this.serverAddress);
        Client client = new Client(this.serverAddress, this.user, this.password);

        if (!client.waitReady()) {
            Logger.w(this.getClass(), "#updateDomain - client is not ready: " + this.serverAddress);
            return;
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AuthDomain authDomain = client.updateDomainInfo(domainName, mainEndpoint, httpEndpoint, httpsEndpoint);
        if (null != authDomain) {
            Logger.i(this.getClass(), "#updateDomain - update domain success - " + authDomain.domainName);
        }
        else {
            Logger.w(this.getClass(), "#updateDomain - update domain failed - " + domainName);
        }

        client.destroy();
        Logger.i(this.getClass(), "#updateDomain - end: " + this.serverAddress);
    }

    public static void main(String[] args) {
        String address = "127.0.0.1";
        DomainTools tool = new DomainTools(address);

        String host = "127.0.0.1";
        Endpoint mainEndpoint = new Endpoint(host, 7000);
        Endpoint httpEndpoint = new Endpoint(host, 7010);
        Endpoint httpsEndpoint = new Endpoint(host, 7017);

//        tool.newDomain("first-prototype-box", Utils.randomString(16),
//                Long.toString(System.currentTimeMillis()), mainEndpoint, httpEndpoint, httpsEndpoint, true);

//        tool.updateDomain("first-prototype-box", mainEndpoint, httpEndpoint, httpsEndpoint);
    }
}
