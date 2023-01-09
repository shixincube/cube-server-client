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

package cube.client.test;

import cell.util.Utils;

import java.util.Date;

public class ReadTimestamp {

    public static void main(String[] args) {
        long timestamp = System.currentTimeMillis();
        Date date = new Date(timestamp);
        System.out.println(Utils.convertDateToSimpleString(date));


        String path = "/path/thisisafile.type";
        String name = null;
        String type = null;
        int typeSplit = path.lastIndexOf(".");
        int nameSplit = path.lastIndexOf("/");
        if (typeSplit != -1 && nameSplit != -1) {
            name = path.substring(nameSplit + 1, typeSplit);
        }
        if (typeSplit != -1) {
            type = path.substring(typeSplit, path.length());
        }
        System.out.println("Path : " + path);
        System.out.println("Name : " + name);
        System.out.println("Type : " + type);
    }
}
