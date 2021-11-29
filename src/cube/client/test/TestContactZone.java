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
import cube.common.entity.Contact;
import cube.common.entity.ContactZone;
import cube.common.entity.ContactZoneParticipant;

import java.util.List;

/**
 * 测试分区。
 */
public class TestContactZone {

    public static void testAddParticipant(CubeClient client) {
        System.out.println("[TestContactZone] testAddParticipant");

        Contact contact = new Contact(11444455L, "shixincube.com", "Cube");
        Contact participant = new Contact(50001004L, "shixincube.com", "Participant");

        ContactZone contactZone = client.addParticipantToZoneByForce(contact, "contacts", participant);
        if (null != contactZone) {
            print(contactZone);
        }
        else {
            System.out.println("[TestContactZone] testAddParticipant ERROR");
        }
    }


    public static void main(String[] args) {

        CubeClient client = new CubeClient("127.0.0.1");

        Helper.sleepInSeconds(3);

        testAddParticipant(client);

        Helper.sleepInSeconds(1);

        System.out.println("*** END ***");
        client.destroy();
    }

    private static void print(ContactZone contactZone) {
        StringBuilder buf = new StringBuilder("--------------------------------\n");
        buf.append("Contact      : ").append(contactZone.owner).append("\n");
        buf.append("Name         : ").append(contactZone.name).append("\n");
        buf.append("Display name : ").append(contactZone.displayName).append("\n");
        buf.append("State        : ").append(contactZone.state.name()).append("\n");
        buf.append("Peer mode    : ").append(contactZone.peerMode).append("\n");

        for (ContactZoneParticipant participant : contactZone.getParticipants()) {
            buf.append("Participant [").append(participant.id);
            buf.append("] - ").append(participant.type.name());
            buf.append(" - ").append(participant.state.name());
            buf.append(" (").append(participant.inviterId).append(")");
            buf.append("\n");
        }

        System.out.println(buf.toString());
    }
}
