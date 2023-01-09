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

package cube.client.util;

import cube.vision.Size;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 屏幕捕获。
 */
public class ScreenCaptor {

    public ScreenCaptor() {
    }

    public Size getScreenSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return new Size(screenSize.width, screenSize.height);
    }

    public List<Size> enumScreenSize() {
        List<Size> result = new ArrayList<>();

        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[]  graphicsDevices = graphicsEnvironment.getScreenDevices();
        for (GraphicsDevice device : graphicsDevices) {
            DisplayMode[] displayModes = device.getDisplayModes();
            DisplayMode mode = displayModes[0];
            result.add(new Size(mode.getWidth(), mode.getHeight()));
        }

        return result;
    }

    public void capture(File outputFile) {
        try {
            Robot robot = new Robot();
            Size size = this.getScreenSize();
            BufferedImage image = robot.createScreenCapture(new Rectangle(0, 0, size.width, size.height));
            ImageIO.write(image, "png", outputFile);
            image = null;
        } catch (AWTException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
