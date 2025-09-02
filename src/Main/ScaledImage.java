/*
 *
 * MIT License
 *
 * Copyright (c) 2020 Free Geek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
// This class is based on TwemojiImage.java from QA Helper (Copyright Free Geek - MIT License): https://github.com/freegeek-pdx/Java-QA-Helper/blob/b511d0259a657d1eebd4224a0950094f03d48156/src/Utilities/TwemojiImage.java
package Main;

import com.formdev.flatlaf.util.UIScale;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * @author Pico Mitchell (of Free Geek)
 */
public class ScaledImage {

    String name;
    double userScaleFactor;
    double systemScaleFactor;

    boolean isMacOS;
    boolean isWindows;

    public ScaledImage(String imageName, Window window) {
        String osName = System.getProperty("os.name");
        isMacOS = osName.startsWith("Mac OS X") || osName.startsWith("macOS");
        isWindows = osName.startsWith("Windows");

        name = imageName.replace(" ", "");
        if (name.equals("AppIcon")) {
            name = "KeyboardTest";
        }

        // Linux Mint uses userScaleFactor for HiDPI while macOS and Windows use systemScaleFactor for HiDPI.
        // This code only handles whole and half number scaling factors because thosea are the only PNG sizes I've included. (125% scaling will use 150% sizes and 175% scaling will use 200% sizes).
        userScaleFactor = (Math.round(UIScale.getUserScaleFactor() * 2.0) / 2.0);
        systemScaleFactor = (Math.round(UIScale.getSystemScaleFactor((window != null) ? window.getGraphicsConfiguration() : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()) * 2.0) / 2.0);
    }

    public String toImgTag(String positionInText) {
        int size = 16;

        int userScaledSize = (int) Math.round(size * userScaleFactor);
        int userAndSystemScaledSize = (int) Math.round(userScaledSize * systemScaleFactor);

        if (userAndSystemScaledSize < 24) {
            userAndSystemScaledSize = 16;
        } else if (userAndSystemScaledSize < 32) {
            userAndSystemScaledSize = 24;
        } else if (userAndSystemScaledSize < 48) {
            userAndSystemScaledSize = 32;
        } else if (userAndSystemScaledSize < 64) {
            userAndSystemScaledSize = 48;
        } else if (userAndSystemScaledSize < 96) {
            userAndSystemScaledSize = 64;
        } else if (userAndSystemScaledSize < 128) {
            userAndSystemScaledSize = 96;
        } else {
            userAndSystemScaledSize = 128;
        }

        // For Linux, the userScaleFactor needs to be applied to the actual size of the img tag or the image are displayed small, on other OSes the userScaleFactor will be 1 so it won't hurt.
        // On Windows, for some reason with 150% scaling, the images get slightly clipped on the right and bottom instead of being properly scaled.
        //  After much trial and error, I found that subtracting 1px from the height makes the scaling work properly to not clip the image.
        //  Although, this does cause a little distortion from slightly funky scaling, it looks better than lots of images being clipped.
        URL thisImageURL = this.getClass().getResource("/Resources/Images/" + name + (name.equals("Blank") ? "" : userAndSystemScaledSize) + ".png");

        return ((thisImageURL == null) ? "<b>[MISSING IMAGE: " + name + "]</b>"
                : ((positionInText.equals("right") || positionInText.equals("inline")) ? "&nbsp;" : "")
                + "<img width='" + userScaledSize + "' height='" + (userScaledSize - ((isWindows && String.valueOf(systemScaleFactor).endsWith(".5")) ? 1 : 0)) + "' src='" + thisImageURL.toString() + "' />"
                + ((positionInText.equals("left") || positionInText.equals("inline")) ? "&nbsp;" : ""));
    }

    public ImageIcon toImageIcon() {
        return toImageIcon(64, true);
    }

    public ImageIcon toImageIcon(int size) {
        return toImageIcon(size, true);
    }

    public ImageIcon toImageIcon(boolean shouldTrimTopTransparentPixels) {
        return toImageIcon(64, shouldTrimTopTransparentPixels);
    }

    public ImageIcon toImageIcon(int size, boolean shouldTrimTopTransparentPixels) {
        int userScaledSize = (int) Math.round(size * userScaleFactor);
        int userAndSystemScaledSize = (int) Math.round(userScaledSize * systemScaleFactor);

        // For Linux, the userScaleFactor needs to be applied to base image size or it will be too small in the window, on other OSes the userScaleFactor will be 1 so it won't hurt.
        URL thisImageURL = this.getClass().getResource("/Resources/Images/" + name + userScaledSize + ".png");

        if (thisImageURL == null) {
            userAndSystemScaledSize = (int) Math.round(size * systemScaleFactor);
            thisImageURL = this.getClass().getResource("/Resources/Images/" + name + size + ".png");
        }

        if (thisImageURL == null) {
            return null;
        }

        try {
            BufferedImage thisImage = ImageIO.read(thisImageURL);

            if (shouldTrimTopTransparentPixels) {
                thisImage = trimTopTransparentPixels(thisImage);
            }

            if (userAndSystemScaledSize > userScaledSize) {
                List<Image> multiResolutionImages = new ArrayList<>();

                multiResolutionImages.add(thisImage);

                URL thisImageURLscaled = this.getClass().getResource("/Resources/Images/" + name + userAndSystemScaledSize + ".png");

                if (thisImageURLscaled != null) {
                    BufferedImage scaledImage = ImageIO.read(thisImageURLscaled);

                    if (shouldTrimTopTransparentPixels) {
                        scaledImage = trimTopTransparentPixels(scaledImage);
                    }

                    multiResolutionImages.add(scaledImage);
                }

                return new ImageIcon(new BaseMultiResolutionImage(multiResolutionImages.toArray(Image[]::new)));
            } else {
                return new ImageIcon(thisImage);
            }
        } catch (IOException e) {
            return null;
        }
    }

    public List<Image> toImageIconsForFrame() {
        return (isMacOS ? null : toImageList()); // Do not use any ImageIcons for Frames on macOS because they are only used a minized window image, which we don't want.
    }

    public List<Image> toImageList() {
        List<Image> imageList = new ArrayList<>();
        String[] everyImageSize = new String[]{"16", "24", "32", "48", "64", "96", "128"};

        for (String thisImageSize : everyImageSize) {
            URL thisImageURL = this.getClass().getResource("/Resources/Images/" + name + thisImageSize + ".png");

            if (thisImageURL != null) {
                try {
                    imageList.add(ImageIO.read(thisImageURL));
                } catch (IOException ex) {

                }
            }
        }

        return imageList;
    }

    private BufferedImage trimTopTransparentPixels(BufferedImage image) {
        // Based on: https://stackoverflow.com/questions/3224561/crop-image-to-smallest-size-by-removing-transparent-pixels-in-java
        //  and: https://stackoverflow.com/questions/47164777/crop-transparent-edges-of-an-image

        int height = image.getHeight();

        if (height <= 1) {
            return image;
        }

        int width = image.getWidth();

        int top = 0;

        topLoop:
        for (; top < (height - 1); top++) {
            for (int x = 0; x < width; x++) {
                if (new Color(image.getRGB(x, top), true).getAlpha() != 0) {
                    break topLoop;
                }
            }
        }

        if (top == 0) {
            return image;
        }

        // Instead of actually trimming the image, redraw it with it's the same size but the actual image contents at the top of the frame (with no transparent pixels on the top).
        // This way seems to avoid to artifacts that occur when using getSubimage.
        BufferedImage repositionedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        repositionedImage.getGraphics().drawImage(image, 0, (top * -1), null);

        return repositionedImage;
    }
}
