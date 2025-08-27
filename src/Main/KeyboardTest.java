/*
 *
 * MIT License
 *
 * Copyright (c) 2020 Rajnish Mishra: https://github.com/darajnish/keyboardtester
 * Copyright (c) 2024-2025 Free Geek: https://github.com/freegeek-pdx/Keyboard-Test
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

 /*
 * App Icon is “Keyboard” from Twemoji (https://github.com/twitter/twemoji) by Twitter (https://twitter.com)
 * Licensed under CC-BY 4.0 (https://creativecommons.org/licenses/by/4.0/)
 */
package Main;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.desktop.AboutEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.text.StyleContext;

/**
 *
 * @author 2020: Rajnish Mishra, 2024: Pico Mitchell (of Free Geek).
 */
public class KeyboardTest extends javax.swing.JFrame {

    private final boolean debugLogging = false; // NOTE: Enabling "debugLogging" can cause freezing in WinRE (and maybe also Linux) when typing very fast.

    private LinkedHashMap<String, JLabel> keyLabels = new LinkedHashMap<>();
    private boolean isMacOS = false;
    private boolean isLinux = false;
    private boolean isWindows = false;
    private boolean isMacLaptop = false;
    private boolean didPressAllMacLaptopKeyboardKeys = false;
    private boolean isTogglingFullKeyboard = false;
    private boolean fullKeyboardHasBeenShown = false;

    private String launchPath = "";
    private String javaPath = "";

    private Border lastKeyPressedLabelBorder;
    private Border keyLabelBorder;
    private Border keyLabelOrangeHighlightBorder;
    private Border keyLabelGreenHighlightBorder;
    private Color keyLabelGreenHighlightBackgroundColor;

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        String osName = System.getProperty("os.name");
        final boolean isMacOS = (osName.startsWith("Mac OS X") || osName.startsWith("macOS"));
        final boolean isLinux = osName.startsWith("Linux");
        final boolean isWindows = osName.startsWith("Windows");

        if (isMacOS) {
            System.setProperty("apple.awt.application.name", "Keyboard Test"); // To not show "KeyboardTest" class name in App menu.
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            // NOTE: Using "com.apple.mrj.application.apple.menu.about.name" property or "-Xdock:name" java launch argument can no longer set JAR app names in the Dock and the "java" binary name is always used: https://bugs.openjdk.org/browse/JDK-8173753
        }

        try {
            if (!new File(KeyboardTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath().endsWith(".app/Contents/app/Keyboard_Test.jar")) {
                Taskbar.getTaskbar().setIconImage(ImageIO.read(KeyboardTest.class.getResource("/Resources/Twemoji/Keyboard1024.png"))); // For macOS Dock when run as JAR (not as compiled app bundle since it would override the ICNS file): https://stackoverflow.com/a/56924202
            }
        } catch (URISyntaxException | IllegalArgumentException | IOException | UnsupportedOperationException | SecurityException setTaskbarImageIconException) {
            // Ignore setTaskbarImageIconException
        }

        // Set FlatLaf settings based on what's used in QA Helper (Copyright Free Geek - MIT License): https://github.com/freegeek-pdx/Java-QA-Helper/blob/b511d0259a657d1eebd4224a0950094f03d48156/src/GUI/QAHelper.java#L203-L317
        boolean shouldSetSystemLookAndFeel = false; // This is here just for easy debugging.

        if (!shouldSetSystemLookAndFeel) {
            try {
                System.setProperty("flatlaf.uiScale.allowScaleDown", "true"); // https://www.formdev.com/flatlaf/system-properties/

                int uiScalePercentage = 100;
                for (String thisArg : args) {
                    if (thisArg.matches("^[0-9]+%$")) {
                        try {
                            uiScalePercentage = Integer.parseInt(thisArg.substring(0, thisArg.length() - 1));
                            if (uiScalePercentage < 50) {
                                uiScalePercentage = 50;
                            } else if (uiScalePercentage > 200) {
                                uiScalePercentage = 200;
                            }

                            if (!isLinux && (uiScalePercentage != 100)) {
                                // DO NOT set the "flatlaf.uiScale" on Linux because we first need to know the initial "userScaleFactor" based on the system scaling.
                                // Windows and macOS do not use the "userScaleFactor" for system scaling, but Linux does and the initial value can only be know after setting the FlatLaf look and feel.
                                // This setting is *supposed* to be set *before* setting the look and feel, but in testing it worked fine to set it after and then re-set the FlatLaf look and feel for our custom value to properly take effect.

                                System.setProperty("flatlaf.uiScale", (uiScalePercentage + "%")); // https://www.formdev.com/flatlaf/system-properties/
                            }
                        } catch (NumberFormatException parseUIScaleArgumentException) {
                            System.out.println("parseUIScaleArgumentException: " + parseUIScaleArgumentException);
                        }
                    }
                }

                System.setProperty("KeyboardTest.uiScalePercentage", Integer.toString(uiScalePercentage));
                // Must set a system property to be able to know the custom scale percentage outside of this main method since cannot set a global variable here.
                // It is important to save this value because on Linux it won't be the same as "flatlaf.uiScale" which will instead be the "uiScalePercentage" multiplied by the initial system "userScaleFactor".

                FlatLaf defaultLaf = new FlatLightLaf();
                FlatLaf platformFlatLaf = (isMacOS ? new com.formdev.flatlaf.themes.FlatMacLightLaf() : defaultLaf);
                UIManager.setLookAndFeel(platformFlatLaf);

                if (isLinux && (uiScalePercentage != 100)) {
                    // As noted above, Linux uses the "userScaleFactor" for the system scaling, and we can only know that initial value after setting the FlatLaf look and feel.
                    // So, now that we know the initial "UserScaleFactor", we can multiply it by our desired "uiScalePercentage" to set the proper custom scaling with the initial system scaling treated as the starting point of 100%.

                    System.setProperty("flatlaf.uiScale", (Math.round(UIScale.getUserScaleFactor() * uiScalePercentage) + "%")); // https://www.formdev.com/flatlaf/system-properties/
                    UIManager.setLookAndFeel(platformFlatLaf); // Must RE-SET the FlatLaf look and feel after setting "flatlaf.uiScale" for the custom scaling to properly be applied to the titlebar and any dialogs.
                }

                // All FlatLaf options:
                //  https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-theme-editor/src/main/resources/com/formdev/flatlaf/themeeditor/FlatLafUIKeys.txt
                //  https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-core/src/main/resources/com/formdev/flatlaf
                UIManager.put("ScrollPane.smoothScrolling", true);
                UIManager.put("Button.minimumWidth", 0); // Allow buttons to get smaller than the default minimum of 72px wide when font size is reduced. (https://github.com/JFormDesigner/FlatLaf/blob/fb44c8fbe4ea607a644670946e56897323e2eac1/flatlaf-core/src/main/resources/com/formdev/flatlaf/FlatLaf.properties#L159)
                UIManager.put("Button.default.boldText", false); // To not bold button text on focus with FlatDarkLaf theme (or FlatMac themes).
                UIManager.put("Button.defaultButtonFollowsFocus", true); // So that Enter key works properly to select focused button across all platforms (even macOS where Enter/Return never worked for focused button).
                UIManager.put("OptionPane.maxCharactersPerLine", Integer.MAX_VALUE); // Don't want FlatLaf to wrap our lines since line breaks were all set intentionally before using FlatLaf.
                UIManager.put("OptionPane.isYesLast", !isWindows); // FlatLaf properly sets isYesLast as true for macOS and false for Windows, but also sets false for Linux when it should actually be true for Linux Mint.
                UIManager.put("OptionPane.sameSizeButtons", false);

                Font defaultFont = (Font) UIManager.get("defaultFont");
                if (isMacOS) {
                    // When using the FlatMac themes along with "Button.defaultButtonFollowsFocus" being set to "true" (which offers great functionality),
                    // the bright blue highlighted default button moving around the window looks bad to me, but the rest of the "FlatMac" theme changes are good.
                    // So, set the following theme values to the default values from the non-Mac themes to continue using the default FlatLaf button style with the FlatMac themes.
                    // These values were determined by seeing what was changed by comparing these dumps: https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-testing/dumps/uidefaults
                    String[] resetUIDefaults = new String[]{"Component.focusWidth", "Component.innerFocusWidth", "Component.innerOutlineWidth",
                        "Button.default.borderWidth", "Button.default.foreground", "Button.default.background", "Button.default.focusedBackground"};
                    for (String thisUIDefault : resetUIDefaults) {
                        UIManager.put(thisUIDefault, defaultLaf.getDefaults().get(thisUIDefault));
                    }

                    // Copying font setting technique from https://github.com/JFormDesigner/FlatLaf/blob/3a784375d087c0e19903c77eb2936400cf38712e/flatlaf-core/src/main/java/com/formdev/flatlaf/FlatLaf.java#L495
                    // Set "Helvetica Neue" as font on all macOS versions for consistency (FlatLaf already uses it for Catalina). NOTE: This is still required even with new FlatMac theme.
                    // Would rather use system font of ".AppleSystemUIFont" or ".SF NS Text" but both have issues with Kerning and Tracking (letter spacing).
                    // Basically the San Francisco system fonts just look too wide. 
                    // https://github.com/JFormDesigner/FlatLaf/blob/3a784375d087c0e19903c77eb2936400cf38712e/flatlaf-core/src/main/java/com/formdev/flatlaf/FlatLaf.java#L442
                    // Related info about this issue: https://github.com/weisJ/darklaf/issues/128
                    if ((defaultFont == null) || !defaultFont.getFamily().equals("Helvetica Neue")) {
                        defaultFont = StyleContext.getDefaultStyleContext().getFont("Helvetica Neue", ((defaultFont == null) ? Font.PLAIN : defaultFont.getStyle()), ((defaultFont == null) ? 13 : defaultFont.getSize()));
                    }
                } else if (isWindows) {
                    UIManager.put("TitlePane.unifiedBackground", false); // I think the window looks better with a different color title bar on Windows (especially when menus are included in the titlebar).

                    if (osName.startsWith("Windows 11")) {
                        // Make the buttons slightly more rounded on Windows 11 to match the new button style.
                        UIManager.put("Button.arc", 8);
                        UIManager.put("Component.arc", 8);
                        UIManager.put("CheckBox.arc", 8);
                    } else {
                        // Square buttons on Windows 10 (but not Windows 11) to look a little more native. (https://www.formdev.com/flatlaf/customizing/#corners)
                        UIManager.put("Button.arc", 0);
                        UIManager.put("Component.arc", 0);
                        UIManager.put("CheckBox.arc", 0);
                    }

                    // No longer need any Decorations or Font changes for WinPE, since they are now built in to FlatLaf: https://github.com/JFormDesigner/FlatLaf/issues/279
                } else if (isLinux) {
                    // Unified title and toolbar can now be enabled on Linux as of FlatLaf 2.1: https://github.com/JFormDesigner/FlatLaf/releases/tag/2.1
                    JFrame.setDefaultLookAndFeelDecorated(true);
                    // DO NOT use window decorations for JDialog since decorated windows don't get shadows and the dialogs on top of the main window with no shadow looks weird.
                    // TODO: Re-asses that decision if decorated windows get proper shadows in the future.

                    UIManager.put("TitlePane.unifiedBackground", false); // I think the window looks better with a different color title bar on Linux (especially when menus are included in the titlebar).
                }

                if (defaultFont != null) {
                    UIManager.put("defaultFont", defaultFont);
                }
            } catch (UnsupportedLookAndFeelException setFlatLafException) {
                shouldSetSystemLookAndFeel = true;
            }
        }

        if (shouldSetSystemLookAndFeel) {
            if (isLinux) {
                System.setProperty("jdk.gtk.version", "2.2"); // Java 11 and newer defaults to GTK 3 which looks bad for some reason (still looks bad with Java 15). Setting to GTK 2.2 must be done before getSystemLookAndFeelClassName()
            }

            try {
                String lookAndFeel = UIManager.getSystemLookAndFeelClassName();

                if (!lookAndFeel.contains(".apple") && !lookAndFeel.contains(".gtk") && !lookAndFeel.contains(".windows")) {
                    String backupLookAndFeel = lookAndFeel;

                    for (UIManager.LookAndFeelInfo thisLookAndFeelInfo : UIManager.getInstalledLookAndFeels()) {
                        String thisLookAndFeelClassName = thisLookAndFeelInfo.getClassName();

                        if (thisLookAndFeelClassName.contains(".gtk")) {
                            lookAndFeel = thisLookAndFeelClassName;
                        } else if (thisLookAndFeelClassName.contains(".metal")) {
                            backupLookAndFeel = thisLookAndFeelClassName;
                        }
                    }

                    if (!lookAndFeel.contains(".gtk")) {
                        lookAndFeel = backupLookAndFeel;
                    }
                }

                UIManager.setLookAndFeel(lookAndFeel);
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException setLookAndFeelException) {
                System.out.println("setLookAndFeelException: " + setLookAndFeelException);
            }
        }

        java.awt.EventQueue.invokeLater(() -> {
            new KeyboardTest().setVisible(true);
        });
    }

    /**
     * Creates new form KeyboardTest
     */
    public KeyboardTest() {
        List<Image> imageList = new ArrayList<>();
        String[] everyImageSize = new String[]{"16", "24", "32", "48", "64", "96", "128"};

        for (String thisImageSize : everyImageSize) {
            URL thisEmojiURL = this.getClass().getResource("/Resources/Twemoji/Keyboard" + thisImageSize + ".png");

            if (thisEmojiURL != null) {
                try {
                    imageList.add(ImageIO.read(thisEmojiURL));
                } catch (IOException loadIconImagesException) {
                    if (debugLogging) {
                        System.out.println("loadIconImagesException: " + loadIconImagesException);
                    }
                }
            }
        }

        setIconImages(imageList);

        initComponents();

        uiScaleMenu.setEnabled(false); // "uiScaleMenu" will be set enabled and visible after loading the
        uiScaleMenu.setVisible(false); // "launchPath" and "javaPath" is completed in the background below.

        int uiScalePercentage = 100;
        if (System.getProperty("KeyboardTest.uiScalePercentage") != null) {
            uiScalePercentage = Integer.parseInt(System.getProperty("KeyboardTest.uiScalePercentage"));
            // The ACTUAL custom scaling is only known in this system property that is set in "main()"
            // because on Linux the "userScaleFactor" could either be the initial system scaling,
            // or could be set to custom the "flatlaf.uiScale" which on Linux would be
            // the intial system scaling multiplied by our custom scaling.
        }

        resetUIScaleMenuItem.setText("Reset UI Scale (Currently " + uiScalePercentage + "%)");

        lastKeyPressedLabelBorder = lastKeyPressedLabel.getBorder();
        keyLabelBorder = keyLabelEscape.getBorder();
        keyLabelOrangeHighlightBorder = BorderFactory.createLineBorder(new Color(255, 165, 0), UIScale.scale(2));
        keyLabelGreenHighlightBorder = BorderFactory.createLineBorder(new Color(0, 128, 0), UIScale.scale(2));
        keyLabelGreenHighlightBackgroundColor = new Color(44, 179, 44);

        keyLabels.put("keyLabel" + KeyEvent.VK_ESCAPE, keyLabelEscape);
        keyLabels.put("keyLabel" + KeyEvent.VK_F1, keyLabelF1);
        keyLabels.put("keyLabel" + KeyEvent.VK_F2, keyLabelF2);
        keyLabels.put("keyLabel" + KeyEvent.VK_F3, keyLabelF3);
        keyLabels.put("keyLabel" + KeyEvent.VK_F4, keyLabelF4);
        keyLabels.put("keyLabel" + KeyEvent.VK_F5, keyLabelF5);
        keyLabels.put("keyLabel" + KeyEvent.VK_F6, keyLabelF6);
        keyLabels.put("keyLabel" + KeyEvent.VK_F7, keyLabelF7);
        keyLabels.put("keyLabel" + KeyEvent.VK_F8, keyLabelF8);
        keyLabels.put("keyLabel" + KeyEvent.VK_F9, keyLabelF9);
        keyLabels.put("keyLabel" + KeyEvent.VK_F10, keyLabelF10);
        keyLabels.put("keyLabel" + KeyEvent.VK_F11, keyLabelF11);
        keyLabels.put("keyLabel" + KeyEvent.VK_F12, keyLabelF12);

        keyLabels.put("keyLabel" + KeyEvent.VK_BACK_QUOTE, keyLabelBackQuote);
        keyLabels.put("keyLabel" + KeyEvent.VK_1, keyLabel1);
        keyLabels.put("keyLabel" + KeyEvent.VK_2, keyLabel2);
        keyLabels.put("keyLabel" + KeyEvent.VK_3, keyLabel3);
        keyLabels.put("keyLabel" + KeyEvent.VK_4, keyLabel4);
        keyLabels.put("keyLabel" + KeyEvent.VK_5, keyLabel5);
        keyLabels.put("keyLabel" + KeyEvent.VK_6, keyLabel6);
        keyLabels.put("keyLabel" + KeyEvent.VK_7, keyLabel7);
        keyLabels.put("keyLabel" + KeyEvent.VK_8, keyLabel8);
        keyLabels.put("keyLabel" + KeyEvent.VK_9, keyLabel9);
        keyLabels.put("keyLabel" + KeyEvent.VK_0, keyLabel0);
        keyLabels.put("keyLabel" + KeyEvent.VK_MINUS, keyLabelMinus);
        keyLabels.put("keyLabel" + KeyEvent.VK_EQUALS, keyLabelEquals);
        keyLabels.put("keyLabel" + KeyEvent.VK_BACK_SPACE, keyLabelBackspace);

        keyLabels.put("keyLabel" + KeyEvent.VK_TAB, keyLabelTab);
        keyLabels.put("keyLabel" + KeyEvent.VK_Q, keyLabelQ);
        keyLabels.put("keyLabel" + KeyEvent.VK_W, keyLabelW);
        keyLabels.put("keyLabel" + KeyEvent.VK_E, keyLabelE);
        keyLabels.put("keyLabel" + KeyEvent.VK_R, keyLabelR);
        keyLabels.put("keyLabel" + KeyEvent.VK_T, keyLabelT);
        keyLabels.put("keyLabel" + KeyEvent.VK_Y, keyLabelY);
        keyLabels.put("keyLabel" + KeyEvent.VK_U, keyLabelU);
        keyLabels.put("keyLabel" + KeyEvent.VK_I, keyLabelI);
        keyLabels.put("keyLabel" + KeyEvent.VK_O, keyLabelO);
        keyLabels.put("keyLabel" + KeyEvent.VK_P, keyLabelP);
        keyLabels.put("keyLabel" + KeyEvent.VK_OPEN_BRACKET, keyLabelOpenBracket);
        keyLabels.put("keyLabel" + KeyEvent.VK_CLOSE_BRACKET, keyLabelCloseBracket);
        keyLabels.put("keyLabel" + KeyEvent.VK_BACK_SLASH, keyLabelBackSlash);

        keyLabels.put("keyLabel" + KeyEvent.VK_CAPS_LOCK, keyLabelCapsLock);
        keyLabels.put("keyLabel" + KeyEvent.VK_A, keyLabelA);
        keyLabels.put("keyLabel" + KeyEvent.VK_S, keyLabelS);
        keyLabels.put("keyLabel" + KeyEvent.VK_D, keyLabelD);
        keyLabels.put("keyLabel" + KeyEvent.VK_F, keyLabelF);
        keyLabels.put("keyLabel" + KeyEvent.VK_G, keyLabelG);
        keyLabels.put("keyLabel" + KeyEvent.VK_H, keyLabelH);
        keyLabels.put("keyLabel" + KeyEvent.VK_J, keyLabelJ);
        keyLabels.put("keyLabel" + KeyEvent.VK_K, keyLabelK);
        keyLabels.put("keyLabel" + KeyEvent.VK_L, keyLabelL);
        keyLabels.put("keyLabel" + KeyEvent.VK_SEMICOLON, keyLabelSemicolon);
        keyLabels.put("keyLabel" + KeyEvent.VK_QUOTE, keyLabelQuote);
        keyLabels.put("keyLabel" + KeyEvent.VK_ENTER, keyLabelEnter);

        keyLabels.put("keyLabelLeft" + KeyEvent.VK_SHIFT, keyLabelLeftShift);
        keyLabels.put("keyLabel" + KeyEvent.VK_Z, keyLabelZ);
        keyLabels.put("keyLabel" + KeyEvent.VK_X, keyLabelX);
        keyLabels.put("keyLabel" + KeyEvent.VK_C, keyLabelC);
        keyLabels.put("keyLabel" + KeyEvent.VK_V, keyLabelV);
        keyLabels.put("keyLabel" + KeyEvent.VK_B, keyLabelB);
        keyLabels.put("keyLabel" + KeyEvent.VK_N, keyLabelN);
        keyLabels.put("keyLabel" + KeyEvent.VK_M, keyLabelM);
        keyLabels.put("keyLabel" + KeyEvent.VK_COMMA, keyLabelComma);
        keyLabels.put("keyLabel" + KeyEvent.VK_PERIOD, keyLabelPeriod);
        keyLabels.put("keyLabel" + KeyEvent.VK_SLASH, keyLabelSlash);
        keyLabels.put("keyLabelRight" + KeyEvent.VK_SHIFT, keyLabelRightShift);

        keyLabels.put("keyLabelLeft" + KeyEvent.VK_CONTROL, keyLabelLeftControl);
        keyLabels.put("keyLabelLeft" + KeyEvent.VK_WINDOWS, keyLabelLeftStart);
        keyLabels.put("keyLabel" + KeyEvent.VK_WINDOWS, keyLabelLeftStart); // On Linux, both the Left and Right Start/Windows keys may not have a "keyLocation", so can't tell them apart and just consider either one the Left Start/Windows key.
        keyLabels.put("keyLabelLeft" + KeyEvent.VK_ALT, keyLabelLeftAlt);
        keyLabels.put("keyLabelLeft" + KeyEvent.VK_META, keyLabelLeftCommand);
        keyLabels.put("keyLabel" + KeyEvent.VK_SPACE, keyLabelSpace);
        keyLabels.put("keyLabelRight" + KeyEvent.VK_META, keyLabelRightCommand);
        keyLabels.put("keyLabelRight" + KeyEvent.VK_ALT, keyLabelRightAlt);
        keyLabels.put("keyLabel" + KeyEvent.VK_ALT, keyLabelRightAlt); // On Mac keyboards, the right Option key registers as "KEY_LOCATION_STANDARD" instead of "KEY_LOCATION_RIGHT", so add a "keyLabels" entry which connects that key code without a location to "keyLabelRightAlt".
        keyLabels.put("keyLabelRight" + KeyEvent.VK_WINDOWS, keyLabelRightStart);
        keyLabels.put("keyLabel" + KeyEvent.VK_CONTEXT_MENU, keyLabelMenu);
        keyLabels.put("keyLabelRight" + KeyEvent.VK_CONTROL, keyLabelRightControl);

        keyLabels.put("keyLabel" + KeyEvent.VK_PRINTSCREEN, keyLabelPrintScreen);
        keyLabels.put("keyLabel" + KeyEvent.VK_SCROLL_LOCK, keyLabelScrollLock);
        keyLabels.put("keyLabel" + KeyEvent.VK_PAUSE, keyLabelPause);

        keyLabels.put("keyLabel" + KeyEvent.VK_INSERT, keyLabelInsert);
        keyLabels.put("keyLabel" + KeyEvent.VK_HELP, keyLabelInsert); // On Mac keyboards, Insert (155) is Help (156) instead, so add a "keyLabels" entry which connects that key code to "keyLabelInsert".
        keyLabels.put("keyLabel" + KeyEvent.VK_HOME, keyLabelHome);
        keyLabels.put("keyLabel" + KeyEvent.VK_PAGE_UP, keyLabelPageUp);

        keyLabels.put("keyLabel" + KeyEvent.VK_DELETE, keyLabelDelete);
        keyLabels.put("keyLabel" + KeyEvent.VK_END, keyLabelEnd);
        keyLabels.put("keyLabel" + KeyEvent.VK_PAGE_DOWN, keyLabelPageDown);

        keyLabels.put("keyLabel" + KeyEvent.VK_UP, keyLabelArrowUp);
        keyLabels.put("keyLabel" + KeyEvent.VK_LEFT, keyLabelArrowLeft);
        keyLabels.put("keyLabel" + KeyEvent.VK_DOWN, keyLabelArrowDown);
        keyLabels.put("keyLabel" + KeyEvent.VK_RIGHT, keyLabelArrowRight);

        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUM_LOCK, keyLabelNumPadNumLock);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_CLEAR, keyLabelNumPadNumLock); // On Mac keyboards, Num Lock (144) is Clear (12) instead, so add a "keyLabels" entry which connects that key code to "keyLabelNumLock".
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_EQUALS, keyLabelNumPadEquals);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_DIVIDE, keyLabelNumPadDivide);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_MULTIPLY, keyLabelNumPadMultiply);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_SUBTRACT, keyLabelNumPadSubtract);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_ADD, keyLabelNumPadAdd);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_ENTER, keyLabelNumPadEnter);

        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD7, keyLabelNumPad7);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD8, keyLabelNumPad8);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD9, keyLabelNumPad9);

        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD4, keyLabelNumPad4);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD5, keyLabelNumPad5);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD6, keyLabelNumPad6);

        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD1, keyLabelNumPad1);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD2, keyLabelNumPad2);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD3, keyLabelNumPad3);

        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_NUMPAD0, keyLabelNumPad0);
        keyLabels.put("keyLabelNumPad" + KeyEvent.VK_DECIMAL, keyLabelNumPadDecimal);

        String osName = System.getProperty("os.name");
        if (osName.startsWith("Mac OS X") || osName.startsWith("macOS")) {
            isMacOS = true;

            try (BufferedReader commandReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(new String[]{"/usr/sbin/system_profiler", "SPHardwareDataType"}).getInputStream()))) {
                String commandOutput = commandReader.lines().collect(Collectors.joining("\n"));

                if (debugLogging) {
                    System.out.println("commandOutput:\n" + commandOutput);
                }

                isMacLaptop = commandOutput.contains("Book");
                if (debugLogging) {
                    System.out.println("isMacLaptop: " + isMacLaptop);
                }
            } catch (Exception getIsMacLaptopException) {
                if (debugLogging) {
                    System.out.println("getIsMacLaptopException: " + getIsMacLaptopException);
                }
            }

            Desktop.getDesktop().setAboutHandler((AboutEvent aboutEvent) -> {
                String appVersion = "UNKNOWN VERSION";
                try (BufferedReader appVersionReader = new BufferedReader(new InputStreamReader(this.getClass().getResource("/Resources/keyboard-test-version.txt").openStream()))) {
                    appVersion = appVersionReader.readLine();
                    if (appVersion == null) {
                        appVersion = "VERSION ERROR";
                    }

                    if (debugLogging) {
                        System.out.println("appVersion: " + appVersion);
                    }
                } catch (Exception getAppVersionException) {
                    if (debugLogging) {
                        System.out.println("getAppVersionException: " + getAppVersionException);
                    }
                }

                JOptionPane.showMessageDialog(this, "<html>"
                        + "<b>Keyboard Test</b> <i>(MIT License)</i><br/>"
                        + "Copyright &copy; 2020 Rajnish Mishra<br/>"
                        + "Copyright &copy; 2024-" + Year.now().toString() + " Free Geek"
                        + "<br/><br/>"
                        + "<b>Version:</b> " + appVersion + " (<b>Java:</b> " + System.getProperty("java.version") + ")"
                        + "<br/><br/>"
                        + "<b>App Icon:</b> <i>Keyboard</i> from <i>Twemoji</i> licensed under <i>CC-BY 4.0</i>&nbsp;<br/>"
                        + "Copyright &copy; 2021 Twitter, Inc and other contributors"
                        + "<br/><br/>"
                        + "<b>UI Theme:</b> <i>FlatLaf</i> licensed under the <i>Apache 2.0 License</i>&nbsp;<br/>"
                        + "Copyright &copy; 2025 FormDev Software GmbH. All rights reserved."
                        + "</html>", "About Keyboard Test", JOptionPane.INFORMATION_MESSAGE);
            });

            keyLabelBackspace.setText("delete");
            keyLabelEnter.setText("return");

            keyLabelLeftControl.setText("control");
            keyLabelLeftControl.setPreferredSize(new Dimension(UIScale.scale(55), UIScale.scale(40)));
            keyLabelLeftControl.setMaximumSize(keyLabelLeftControl.getPreferredSize());
            keyLabelLeftControl.setMinimumSize(keyLabelLeftControl.getPreferredSize());
            keyLabelRightControl.setText(keyLabelLeftControl.getText());

            keyLabelLeftAlt.setText("option");
            keyLabelLeftAlt.setPreferredSize(new Dimension(UIScale.scale(50), UIScale.scale(40)));
            keyLabelLeftAlt.setMaximumSize(keyLabelLeftAlt.getPreferredSize());
            keyLabelLeftAlt.setMinimumSize(keyLabelLeftAlt.getPreferredSize());
            keyLabelRightAlt.setText(keyLabelLeftAlt.getText());

            keyLabelInsert.setText("help"); // On Mac keyboards, Insert (155) is Help (156) instead, so change the label text.
            keyLabelNumPadNumLock.setText("clear"); // On Mac keyboards, Num Lock (144) is Clear (12) instead, so change the label text.

            JLabel[] keyLabelsToLowercase = new JLabel[]{
                keyLabelEscape, keyLabelTab, keyLabelCapsLock, keyLabelLeftShift, keyLabelRightShift, keyLabelSpace,
                keyLabelHome, keyLabelPageUp, keyLabelDelete, keyLabelEnd, keyLabelPageDown, keyLabelNumPadEnter
            };
            for (JLabel thisKeyLabelToLowercase : keyLabelsToLowercase) {
                thisKeyLabelToLowercase.setText(thisKeyLabelToLowercase.getText().toLowerCase());
            }

            keyLabelArrowLeft.setText("◀");
            keyLabelArrowLeft.setFont(new Font("Helvetica", 0, UIScale.scale(12)));
            keyLabelArrowUp.setText("▲");
            keyLabelArrowUp.setFont(keyLabelArrowLeft.getFont());
            keyLabelArrowRight.setText("▶");
            keyLabelArrowRight.setFont(keyLabelArrowLeft.getFont());
            keyLabelArrowDown.setText("▼");
            keyLabelArrowDown.setFont(keyLabelArrowLeft.getFont());

            topOtherKeysPanel.setVisible(false); // Hide Print Screen, Scroll Lock, and Pause Keys
            keyLabelLeftStart.setVisible(false);
            keyLabelRightStart.setVisible(false);
            keyLabelMenu.setVisible(false);

            // On Mac keyboards, a "-" key on the 2nd row of the NumPad is used rather than the "regular" top row key to make space for the "=" key on top row of Mac keyboard Num Pads.
            keyLabelNumPadSubtract.setVisible(false); // So, hide the "-" key on the top row of the NumPad since "keyLabelNumPadMacSubtract" on the 2nd row will be used instead,
            keyLabels.put("keyLabelNumPad" + KeyEvent.VK_SUBTRACT, keyLabelNumPadMacSubtract); // and replace the "keyLabelNumPadSubtract" entry with "keyLabelNumPadMacSubtract" in "keyLabels".
        } else {
            keyLabelLeftCommand.setVisible(false);
            keyLabelRightCommand.setVisible(false);
            keyLabelNumPadEquals.setVisible(false); // Only Mac keyboards have an "=" key in the NumPad.
            keyLabelNumPadMacSubtract.setVisible(false); // Hide the "-" key on the 2nd row of the NumPad since it only exists on Mac keyboards and the top row "keyLabelNumPadSubtract" will be used instead.

            if (osName.startsWith("Windows")) {
                isWindows = true;

                try {
                    File windowsKeyboardTestRelauncherFile = new File(System.getProperty("java.io.tmpdir"), "Keyboard_Test-Relauncher.cmd");

                    if (windowsKeyboardTestRelauncherFile.exists()) {
                        windowsKeyboardTestRelauncherFile.delete();
                    }
                } catch (Exception deleteTempRelauncherCmdException) {
                    if (debugLogging) {
                        System.out.println("deleteTempRelauncherCmdException: " + deleteTempRelauncherCmdException);
                    }
                }
            } else if (osName.startsWith("Linux")) {
                isLinux = true;

                keyLabelRightStart.setVisible(false); // Hide Right Start Key because Linux never differentiates between Left and Right Start Key, so will only highlight Left Start Key either way.
            }
        }

        resetPressedKeysMenuItemActionPerformed(null);
        toggleFullKeyboardMenuItemActionPerformed(null); // Always start with a non-Full Keyboard layout since generally testing laptops. The Full Keyboard layout will display automatically if any of those hidden keys are pressed.

        (new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (;;) {
                    updateLockKeysState();
                    TimeUnit.SECONDS.sleep(1);
                }
            }
        }).execute();

        (new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception { // Load the "javaPath" in the background because loading PowerShell for the Windows "javaPath" can add a noticable delay to the window opening after launch.
                // The following code to get "javaPath" is based on code from QA Helper (Copyright Free Geek - MIT License): https://github.com/freegeek-pdx/Java-QA-Helper/blob/b511d0259a657d1eebd4224a0950094f03d48156/src/GUI/QAHelper.java#L1413-L1447

                try {
                    URI launchURI = KeyboardTest.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                    String launchURIString = launchURI.toString();

                    if (isWindows && launchURIString.startsWith("file://")) {
                        launchPath = new File(launchURIString.replace("file://", "//").replace("%20", " ")).getPath(); // To fix server (or Parallels shared folder) paths on Windows.
                    } else {
                        launchPath = new File(launchURI).getPath();
                    }

                    if (launchPath.endsWith(".jar")) {
                        if (isMacOS && launchPath.endsWith(".app/Contents/app/Keyboard_Test.jar")) {
                            launchPath = launchPath.substring(0, launchPath.lastIndexOf("/Contents/app/Keyboard_Test.jar"));
                        } else if (isWindows) {
                            try (BufferedReader commandReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(new String[]{"\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "(Get-CimInstance Win32_Process -Filter \\\"Name LIKE 'java%.exe' AND CommandLine LIKE '%Keyboard_Test%.jar%'\\\" | Select-Object -First 1).Path"}).getInputStream()))) {
                                String firstLine;
                                if ((firstLine = commandReader.readLine()) != null) {
                                    javaPath = firstLine;
                                }
                            } catch (Exception getWindowsJavaPathException) {
                                if (debugLogging) {
                                    System.out.println("getWindowsJavaPathException: " + getWindowsJavaPathException);
                                }
                            }

                            if (javaPath.endsWith("java.exe") && new File(javaPath.replace("java.exe", "javaw.exe")).exists()) {
                                javaPath = javaPath.replace("java.exe", "javaw.exe");
                            }
                        } else {
                            try (BufferedReader commandReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(new String[]{"/usr/bin/pgrep", ("-fa" + (isMacOS ? "l" : "")), "Keyboard_Test.*\\.jar"}).getInputStream()))) {
                                String thisLine;
                                while ((thisLine = commandReader.readLine()) != null) {
                                    if (!thisLine.contains("sudo ")) {
                                        String runningJarInfoFirstPart = thisLine.split(" -jar ")[0];
                                        javaPath = runningJarInfoFirstPart.substring(runningJarInfoFirstPart.indexOf(" ") + 1);
                                        break;
                                    }
                                }
                            } catch (Exception getJavaPathException) {
                                if (debugLogging) {
                                    System.out.println("getJavaPathException: " + getJavaPathException);
                                }
                            }

                            if (javaPath.isEmpty() || !new File(javaPath).exists() || !new File(javaPath).canExecute()) {
                                javaPath = "/usr/bin/java";
                            }
                        }
                    }

                    if (debugLogging) {
                        System.out.println("launchPath: " + launchPath);
                        System.out.println("javaPath: " + javaPath);
                    }
                } catch (URISyntaxException getLaunchPathException) {
                    if (debugLogging) {
                        System.out.println("getLaunchPathException: " + getLaunchPathException);
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                if (!launchPath.isEmpty() && new File(launchPath).exists() && ((isMacOS && launchPath.endsWith(".app")) || (!javaPath.isEmpty() && new File(javaPath).exists() && new File(javaPath).canExecute()))) {
                    uiScaleMenu.setEnabled(true);
                    uiScaleMenu.setVisible(true);
                }
            }
        }).execute();
    }

    private void updateLockKeysState() {
        try {
            keyLabelCapsLock.setFont(keyLabelCapsLock.getFont().deriveFont((Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) ? (Font.BOLD | Font.ITALIC) : Font.PLAIN)));

            if (!isMacOS) {
                keyLabelNumPadNumLock.setFont(keyLabelNumPadNumLock.getFont().deriveFont((Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK) ? (Font.BOLD | Font.ITALIC) : Font.PLAIN)));
                keyLabelScrollLock.setFont(keyLabelScrollLock.getFont().deriveFont((Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_SCROLL_LOCK) ? (Font.BOLD | Font.ITALIC) : Font.PLAIN)));
            }
        } catch (UnsupportedOperationException updateLockKeysStateException) {
            if (debugLogging) {
                System.out.println("updateLockKeysStateException: " + updateLockKeysStateException);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contentScrollPane = new javax.swing.JScrollPane();
        contentPane = new javax.swing.JPanel();
        textAreaScrollPane = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();
        mainKeysPanel = new javax.swing.JPanel();
        keyLabelEscape = new javax.swing.JLabel();
        keyLabelF1 = new javax.swing.JLabel();
        keyLabelF2 = new javax.swing.JLabel();
        keyLabelF3 = new javax.swing.JLabel();
        keyLabelF4 = new javax.swing.JLabel();
        keyLabelF5 = new javax.swing.JLabel();
        keyLabelF6 = new javax.swing.JLabel();
        keyLabelF7 = new javax.swing.JLabel();
        keyLabelF8 = new javax.swing.JLabel();
        keyLabelF9 = new javax.swing.JLabel();
        keyLabelF10 = new javax.swing.JLabel();
        keyLabelF11 = new javax.swing.JLabel();
        keyLabelF12 = new javax.swing.JLabel();
        keyLabelBackQuote = new javax.swing.JLabel();
        keyLabel1 = new javax.swing.JLabel();
        keyLabel2 = new javax.swing.JLabel();
        keyLabel3 = new javax.swing.JLabel();
        keyLabel4 = new javax.swing.JLabel();
        keyLabel5 = new javax.swing.JLabel();
        keyLabel6 = new javax.swing.JLabel();
        keyLabel7 = new javax.swing.JLabel();
        keyLabel8 = new javax.swing.JLabel();
        keyLabel9 = new javax.swing.JLabel();
        keyLabel0 = new javax.swing.JLabel();
        keyLabelMinus = new javax.swing.JLabel();
        keyLabelEquals = new javax.swing.JLabel();
        keyLabelBackspace = new javax.swing.JLabel();
        keyLabelTab = new javax.swing.JLabel();
        keyLabelQ = new javax.swing.JLabel();
        keyLabelW = new javax.swing.JLabel();
        keyLabelE = new javax.swing.JLabel();
        keyLabelR = new javax.swing.JLabel();
        keyLabelT = new javax.swing.JLabel();
        keyLabelY = new javax.swing.JLabel();
        keyLabelU = new javax.swing.JLabel();
        keyLabelI = new javax.swing.JLabel();
        keyLabelO = new javax.swing.JLabel();
        keyLabelP = new javax.swing.JLabel();
        keyLabelOpenBracket = new javax.swing.JLabel();
        keyLabelCloseBracket = new javax.swing.JLabel();
        keyLabelBackSlash = new javax.swing.JLabel();
        keyLabelCapsLock = new javax.swing.JLabel();
        keyLabelA = new javax.swing.JLabel();
        keyLabelS = new javax.swing.JLabel();
        keyLabelD = new javax.swing.JLabel();
        keyLabelF = new javax.swing.JLabel();
        keyLabelG = new javax.swing.JLabel();
        keyLabelH = new javax.swing.JLabel();
        keyLabelJ = new javax.swing.JLabel();
        keyLabelK = new javax.swing.JLabel();
        keyLabelL = new javax.swing.JLabel();
        keyLabelSemicolon = new javax.swing.JLabel();
        keyLabelQuote = new javax.swing.JLabel();
        keyLabelEnter = new javax.swing.JLabel();
        keyLabelLeftShift = new javax.swing.JLabel();
        keyLabelZ = new javax.swing.JLabel();
        keyLabelX = new javax.swing.JLabel();
        keyLabelC = new javax.swing.JLabel();
        keyLabelV = new javax.swing.JLabel();
        keyLabelB = new javax.swing.JLabel();
        keyLabelN = new javax.swing.JLabel();
        keyLabelM = new javax.swing.JLabel();
        keyLabelComma = new javax.swing.JLabel();
        keyLabelPeriod = new javax.swing.JLabel();
        keyLabelSlash = new javax.swing.JLabel();
        keyLabelRightShift = new javax.swing.JLabel();
        keyLabelLeftControl = new javax.swing.JLabel();
        keyLabelLeftStart = new javax.swing.JLabel();
        keyLabelLeftAlt = new javax.swing.JLabel();
        keyLabelLeftCommand = new javax.swing.JLabel();
        keyLabelSpace = new javax.swing.JLabel();
        keyLabelRightCommand = new javax.swing.JLabel();
        keyLabelRightAlt = new javax.swing.JLabel();
        keyLabelRightStart = new javax.swing.JLabel();
        keyLabelMenu = new javax.swing.JLabel();
        keyLabelRightControl = new javax.swing.JLabel();
        topOtherKeysAndLastKeyPressedPanel = new javax.swing.JPanel();
        topOtherKeysPanel = new javax.swing.JPanel();
        keyLabelPrintScreen = new javax.swing.JLabel();
        keyLabelScrollLock = new javax.swing.JLabel();
        keyLabelPause = new javax.swing.JLabel();
        lastKeyPressedLabel = new javax.swing.JLabel();
        otherKeysPanel = new javax.swing.JPanel();
        keyLabelInsert = new javax.swing.JLabel();
        keyLabelHome = new javax.swing.JLabel();
        keyLabelPageUp = new javax.swing.JLabel();
        keyLabelDelete = new javax.swing.JLabel();
        keyLabelEnd = new javax.swing.JLabel();
        keyLabelPageDown = new javax.swing.JLabel();
        arrowKeysPanel = new javax.swing.JPanel();
        keyLabelArrowUp = new javax.swing.JLabel();
        arrowKeysAlignmentSpacer = new javax.swing.JPanel();
        keyLabelArrowLeft = new javax.swing.JLabel();
        keyLabelArrowDown = new javax.swing.JLabel();
        keyLabelArrowRight = new javax.swing.JLabel();
        numPadPanel = new javax.swing.JPanel();
        keyLabelNumPadNumLock = new javax.swing.JLabel();
        keyLabelNumPadEquals = new javax.swing.JLabel();
        keyLabelNumPadDivide = new javax.swing.JLabel();
        keyLabelNumPadMultiply = new javax.swing.JLabel();
        keyLabelNumPadSubtract = new javax.swing.JLabel();
        keyLabelNumPadMacSubtract = new javax.swing.JLabel();
        keyLabelNumPadAdd = new javax.swing.JLabel();
        keyLabelNumPadEnter = new javax.swing.JLabel();
        keyLabelNumPad7 = new javax.swing.JLabel();
        keyLabelNumPad8 = new javax.swing.JLabel();
        keyLabelNumPad9 = new javax.swing.JLabel();
        keyLabelNumPad4 = new javax.swing.JLabel();
        keyLabelNumPad5 = new javax.swing.JLabel();
        keyLabelNumPad6 = new javax.swing.JLabel();
        keyLabelNumPad1 = new javax.swing.JLabel();
        keyLabelNumPad2 = new javax.swing.JLabel();
        keyLabelNumPad3 = new javax.swing.JLabel();
        keyLabelNumPad0 = new javax.swing.JLabel();
        keyLabelNumPadDecimal = new javax.swing.JLabel();
        mainMenuBar = new javax.swing.JMenuBar();
        editMenu = new javax.swing.JMenu();
        resetPressedKeysMenuItem = new javax.swing.JMenuItem();
        toggleFullKeyboardMenuItem = new javax.swing.JMenuItem();
        uiScaleMenu = new javax.swing.JMenu();
        resetUIScaleMenuItem = new javax.swing.JMenuItem();
        increaseUIScaleMenuItem = new javax.swing.JMenuItem();
        decreaseUIScaleMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Keyboard Test");
        setName("keyboardTestFrame"); // NOI18N
        setResizable(false);

        contentScrollPane.setBorder(null);
        contentScrollPane.setFocusTraversalKeysEnabled(false);
        contentScrollPane.setFocusable(false);

        contentPane.setFocusTraversalKeysEnabled(false);
        contentPane.setFocusable(false);

        textAreaScrollPane.setBackground(java.awt.Color.white);
        textAreaScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.lightGray, UIScale.scale(2)));
        textAreaScrollPane.setForeground(java.awt.Color.black);
        textAreaScrollPane.setPreferredSize(new java.awt.Dimension(UIScale.scale(100), UIScale.scale(130)));

        textArea.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(14))); // NOI18N
        textArea.setForeground(java.awt.Color.black);
        textArea.setLineWrap(true);
        textArea.setTabSize(4);
        textArea.setWrapStyleWord(true);
        textArea.setCaretColor(java.awt.Color.darkGray);
        textArea.setFocusTraversalKeysEnabled(false);
        textArea.setMargin(new java.awt.Insets(6, 6, 6, 6));
        textArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                onKeyPressed(evt);
            }
        });
        textAreaScrollPane.setViewportView(textArea);

        keyLabelEscape.setBackground(java.awt.Color.white);
        keyLabelEscape.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelEscape.setForeground(java.awt.Color.black);
        keyLabelEscape.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelEscape.setText("Esc");
        keyLabelEscape.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelEscape.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelEscape.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelEscape.setOpaque(true);
        keyLabelEscape.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelF1.setBackground(java.awt.Color.white);
        keyLabelF1.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF1.setForeground(java.awt.Color.black);
        keyLabelF1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF1.setText("F1");
        keyLabelF1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF1.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF1.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF1.setOpaque(true);
        keyLabelF1.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF2.setBackground(java.awt.Color.white);
        keyLabelF2.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF2.setForeground(java.awt.Color.black);
        keyLabelF2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF2.setText("F2");
        keyLabelF2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF2.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF2.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF2.setOpaque(true);
        keyLabelF2.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF3.setBackground(java.awt.Color.white);
        keyLabelF3.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF3.setForeground(java.awt.Color.black);
        keyLabelF3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF3.setText("F3");
        keyLabelF3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF3.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF3.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF3.setOpaque(true);
        keyLabelF3.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF4.setBackground(java.awt.Color.white);
        keyLabelF4.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF4.setForeground(java.awt.Color.black);
        keyLabelF4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF4.setText("F4");
        keyLabelF4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF4.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF4.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF4.setOpaque(true);
        keyLabelF4.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF5.setBackground(java.awt.Color.white);
        keyLabelF5.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF5.setForeground(java.awt.Color.black);
        keyLabelF5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF5.setText("F5");
        keyLabelF5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF5.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF5.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF5.setOpaque(true);
        keyLabelF5.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF6.setBackground(java.awt.Color.white);
        keyLabelF6.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF6.setForeground(java.awt.Color.black);
        keyLabelF6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF6.setText("F6");
        keyLabelF6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF6.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF6.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF6.setOpaque(true);
        keyLabelF6.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF7.setBackground(java.awt.Color.white);
        keyLabelF7.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF7.setForeground(java.awt.Color.black);
        keyLabelF7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF7.setText("F7");
        keyLabelF7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF7.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF7.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF7.setOpaque(true);
        keyLabelF7.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF8.setBackground(java.awt.Color.white);
        keyLabelF8.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF8.setForeground(java.awt.Color.black);
        keyLabelF8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF8.setText("F8");
        keyLabelF8.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF8.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF8.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF8.setOpaque(true);
        keyLabelF8.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF9.setBackground(java.awt.Color.white);
        keyLabelF9.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF9.setForeground(java.awt.Color.black);
        keyLabelF9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF9.setText("F9");
        keyLabelF9.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF9.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF9.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF9.setOpaque(true);
        keyLabelF9.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF10.setBackground(java.awt.Color.white);
        keyLabelF10.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF10.setForeground(java.awt.Color.black);
        keyLabelF10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF10.setText("F10");
        keyLabelF10.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF10.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF10.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF10.setOpaque(true);
        keyLabelF10.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF11.setBackground(java.awt.Color.white);
        keyLabelF11.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF11.setForeground(java.awt.Color.black);
        keyLabelF11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF11.setText("F11");
        keyLabelF11.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF11.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF11.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF11.setOpaque(true);
        keyLabelF11.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelF12.setBackground(java.awt.Color.white);
        keyLabelF12.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelF12.setForeground(java.awt.Color.black);
        keyLabelF12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelF12.setText("F12");
        keyLabelF12.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelF12.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF12.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
        keyLabelF12.setOpaque(true);
        keyLabelF12.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

        keyLabelBackQuote.setBackground(java.awt.Color.white);
        keyLabelBackQuote.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelBackQuote.setForeground(java.awt.Color.black);
        keyLabelBackQuote.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelBackQuote.setText("`");
        keyLabelBackQuote.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelBackQuote.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelBackQuote.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelBackQuote.setOpaque(true);
        keyLabelBackQuote.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel1.setBackground(java.awt.Color.white);
        keyLabel1.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel1.setForeground(java.awt.Color.black);
        keyLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel1.setText("1");
        keyLabel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel1.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel1.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel1.setOpaque(true);
        keyLabel1.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel2.setBackground(java.awt.Color.white);
        keyLabel2.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel2.setForeground(java.awt.Color.black);
        keyLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel2.setText("2");
        keyLabel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel2.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel2.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel2.setOpaque(true);
        keyLabel2.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel3.setBackground(java.awt.Color.white);
        keyLabel3.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel3.setForeground(java.awt.Color.black);
        keyLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel3.setText("3");
        keyLabel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel3.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel3.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel3.setOpaque(true);
        keyLabel3.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel4.setBackground(java.awt.Color.white);
        keyLabel4.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel4.setForeground(java.awt.Color.black);
        keyLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel4.setText("4");
        keyLabel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel4.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel4.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel4.setOpaque(true);
        keyLabel4.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel5.setBackground(java.awt.Color.white);
        keyLabel5.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel5.setForeground(java.awt.Color.black);
        keyLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel5.setText("5");
        keyLabel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel5.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel5.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel5.setOpaque(true);
        keyLabel5.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel6.setBackground(java.awt.Color.white);
        keyLabel6.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel6.setForeground(java.awt.Color.black);
        keyLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel6.setText("6");
        keyLabel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel6.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel6.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel6.setOpaque(true);
        keyLabel6.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel7.setBackground(java.awt.Color.white);
        keyLabel7.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel7.setForeground(java.awt.Color.black);
        keyLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel7.setText("7");
        keyLabel7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel7.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel7.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel7.setOpaque(true);
        keyLabel7.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel8.setBackground(java.awt.Color.white);
        keyLabel8.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel8.setForeground(java.awt.Color.black);
        keyLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel8.setText("8");
        keyLabel8.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel8.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel8.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel8.setOpaque(true);
        keyLabel8.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel9.setBackground(java.awt.Color.white);
        keyLabel9.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel9.setForeground(java.awt.Color.black);
        keyLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel9.setText("9");
        keyLabel9.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel9.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel9.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel9.setOpaque(true);
        keyLabel9.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabel0.setBackground(java.awt.Color.white);
        keyLabel0.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabel0.setForeground(java.awt.Color.black);
        keyLabel0.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabel0.setText("0");
        keyLabel0.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabel0.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel0.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabel0.setOpaque(true);
        keyLabel0.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelMinus.setBackground(java.awt.Color.white);
        keyLabelMinus.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelMinus.setForeground(java.awt.Color.black);
        keyLabelMinus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelMinus.setText("-");
        keyLabelMinus.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelMinus.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelMinus.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelMinus.setOpaque(true);
        keyLabelMinus.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelEquals.setBackground(java.awt.Color.white);
        keyLabelEquals.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelEquals.setForeground(java.awt.Color.black);
        keyLabelEquals.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelEquals.setText("=");
        keyLabelEquals.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelEquals.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelEquals.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelEquals.setOpaque(true);
        keyLabelEquals.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelBackspace.setBackground(java.awt.Color.white);
        keyLabelBackspace.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelBackspace.setForeground(java.awt.Color.black);
        keyLabelBackspace.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelBackspace.setText("Backspace");
        keyLabelBackspace.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelBackspace.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelBackspace.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelBackspace.setOpaque(true);
        keyLabelBackspace.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelTab.setBackground(java.awt.Color.white);
        keyLabelTab.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelTab.setForeground(java.awt.Color.black);
        keyLabelTab.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelTab.setText("Tab");
        keyLabelTab.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelTab.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelTab.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelTab.setOpaque(true);
        keyLabelTab.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelQ.setBackground(java.awt.Color.white);
        keyLabelQ.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelQ.setForeground(java.awt.Color.black);
        keyLabelQ.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelQ.setText("Q");
        keyLabelQ.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelQ.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelQ.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelQ.setOpaque(true);
        keyLabelQ.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelW.setBackground(java.awt.Color.white);
        keyLabelW.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelW.setForeground(java.awt.Color.black);
        keyLabelW.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelW.setText("W");
        keyLabelW.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelW.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelW.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelW.setOpaque(true);
        keyLabelW.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelE.setBackground(java.awt.Color.white);
        keyLabelE.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelE.setForeground(java.awt.Color.black);
        keyLabelE.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelE.setText("E");
        keyLabelE.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelE.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelE.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelE.setOpaque(true);
        keyLabelE.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelR.setBackground(java.awt.Color.white);
        keyLabelR.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelR.setForeground(java.awt.Color.black);
        keyLabelR.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelR.setText("R");
        keyLabelR.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelR.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelR.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelR.setOpaque(true);
        keyLabelR.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelT.setBackground(java.awt.Color.white);
        keyLabelT.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelT.setForeground(java.awt.Color.black);
        keyLabelT.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelT.setText("T");
        keyLabelT.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelT.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelT.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelT.setOpaque(true);
        keyLabelT.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelY.setBackground(java.awt.Color.white);
        keyLabelY.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelY.setForeground(java.awt.Color.black);
        keyLabelY.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelY.setText("Y");
        keyLabelY.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelY.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelY.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelY.setOpaque(true);
        keyLabelY.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelU.setBackground(java.awt.Color.white);
        keyLabelU.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelU.setForeground(java.awt.Color.black);
        keyLabelU.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelU.setText("U");
        keyLabelU.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelU.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelU.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelU.setOpaque(true);
        keyLabelU.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelI.setBackground(java.awt.Color.white);
        keyLabelI.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelI.setForeground(java.awt.Color.black);
        keyLabelI.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelI.setText("I");
        keyLabelI.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelI.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelI.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelI.setOpaque(true);
        keyLabelI.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelO.setBackground(java.awt.Color.white);
        keyLabelO.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelO.setForeground(java.awt.Color.black);
        keyLabelO.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelO.setText("O");
        keyLabelO.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelO.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelO.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelO.setOpaque(true);
        keyLabelO.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelP.setBackground(java.awt.Color.white);
        keyLabelP.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelP.setForeground(java.awt.Color.black);
        keyLabelP.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelP.setText("P");
        keyLabelP.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelP.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelP.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelP.setOpaque(true);
        keyLabelP.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelOpenBracket.setBackground(java.awt.Color.white);
        keyLabelOpenBracket.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelOpenBracket.setForeground(java.awt.Color.black);
        keyLabelOpenBracket.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelOpenBracket.setText("[");
        keyLabelOpenBracket.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelOpenBracket.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelOpenBracket.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelOpenBracket.setOpaque(true);
        keyLabelOpenBracket.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelCloseBracket.setBackground(java.awt.Color.white);
        keyLabelCloseBracket.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelCloseBracket.setForeground(java.awt.Color.black);
        keyLabelCloseBracket.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelCloseBracket.setText("]");
        keyLabelCloseBracket.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
        keyLabelCloseBracket.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelCloseBracket.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
        keyLabelCloseBracket.setOpaque(true);
        keyLabelCloseBracket.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

        keyLabelBackSlash.setBackground(java.awt.Color.white);
        keyLabelBackSlash.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
        keyLabelBackSlash.setForeground(java.awt.Color.black);
        keyLabelBackSlash.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        keyLabelBackSlash.setText("\\");
            keyLabelBackSlash.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelBackSlash.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelBackSlash.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelBackSlash.setOpaque(true);
            keyLabelBackSlash.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelCapsLock.setBackground(java.awt.Color.white);
            keyLabelCapsLock.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelCapsLock.setForeground(java.awt.Color.black);
            keyLabelCapsLock.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelCapsLock.setText("Caps Lock");
            keyLabelCapsLock.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelCapsLock.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelCapsLock.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelCapsLock.setOpaque(true);
            keyLabelCapsLock.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelA.setBackground(java.awt.Color.white);
            keyLabelA.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelA.setForeground(java.awt.Color.black);
            keyLabelA.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelA.setText("A");
            keyLabelA.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelA.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelA.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelA.setOpaque(true);
            keyLabelA.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelS.setBackground(java.awt.Color.white);
            keyLabelS.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelS.setForeground(java.awt.Color.black);
            keyLabelS.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelS.setText("S");
            keyLabelS.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelS.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelS.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelS.setOpaque(true);
            keyLabelS.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelD.setBackground(java.awt.Color.white);
            keyLabelD.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelD.setForeground(java.awt.Color.black);
            keyLabelD.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelD.setText("D");
            keyLabelD.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelD.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelD.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelD.setOpaque(true);
            keyLabelD.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelF.setBackground(java.awt.Color.white);
            keyLabelF.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelF.setForeground(java.awt.Color.black);
            keyLabelF.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelF.setText("F");
            keyLabelF.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelF.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelF.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelF.setOpaque(true);
            keyLabelF.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelG.setBackground(java.awt.Color.white);
            keyLabelG.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelG.setForeground(java.awt.Color.black);
            keyLabelG.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelG.setText("G");
            keyLabelG.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelG.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelG.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelG.setOpaque(true);
            keyLabelG.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelH.setBackground(java.awt.Color.white);
            keyLabelH.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelH.setForeground(java.awt.Color.black);
            keyLabelH.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelH.setText("H");
            keyLabelH.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelH.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelH.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelH.setOpaque(true);
            keyLabelH.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelJ.setBackground(java.awt.Color.white);
            keyLabelJ.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelJ.setForeground(java.awt.Color.black);
            keyLabelJ.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelJ.setText("J");
            keyLabelJ.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelJ.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelJ.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelJ.setOpaque(true);
            keyLabelJ.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelK.setBackground(java.awt.Color.white);
            keyLabelK.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelK.setForeground(java.awt.Color.black);
            keyLabelK.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelK.setText("K");
            keyLabelK.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelK.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelK.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelK.setOpaque(true);
            keyLabelK.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelL.setBackground(java.awt.Color.white);
            keyLabelL.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelL.setForeground(java.awt.Color.black);
            keyLabelL.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelL.setText("L");
            keyLabelL.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelL.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelL.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelL.setOpaque(true);
            keyLabelL.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelSemicolon.setBackground(java.awt.Color.white);
            keyLabelSemicolon.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelSemicolon.setForeground(java.awt.Color.black);
            keyLabelSemicolon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelSemicolon.setText(";");
            keyLabelSemicolon.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelSemicolon.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelSemicolon.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelSemicolon.setOpaque(true);
            keyLabelSemicolon.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelQuote.setBackground(java.awt.Color.white);
            keyLabelQuote.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelQuote.setForeground(java.awt.Color.black);
            keyLabelQuote.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelQuote.setText("'");
            keyLabelQuote.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelQuote.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelQuote.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelQuote.setOpaque(true);
            keyLabelQuote.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelEnter.setBackground(java.awt.Color.white);
            keyLabelEnter.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelEnter.setForeground(java.awt.Color.black);
            keyLabelEnter.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelEnter.setText("Enter");
            keyLabelEnter.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelEnter.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelEnter.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelEnter.setOpaque(true);
            keyLabelEnter.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelLeftShift.setBackground(java.awt.Color.white);
            keyLabelLeftShift.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelLeftShift.setForeground(java.awt.Color.black);
            keyLabelLeftShift.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelLeftShift.setText("Shift");
            keyLabelLeftShift.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelLeftShift.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelLeftShift.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelLeftShift.setOpaque(true);
            keyLabelLeftShift.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelZ.setBackground(java.awt.Color.white);
            keyLabelZ.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelZ.setForeground(java.awt.Color.black);
            keyLabelZ.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelZ.setText("Z");
            keyLabelZ.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelZ.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelZ.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelZ.setOpaque(true);
            keyLabelZ.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelX.setBackground(java.awt.Color.white);
            keyLabelX.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelX.setForeground(java.awt.Color.black);
            keyLabelX.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelX.setText("X");
            keyLabelX.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelX.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelX.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelX.setOpaque(true);
            keyLabelX.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelC.setBackground(java.awt.Color.white);
            keyLabelC.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelC.setForeground(java.awt.Color.black);
            keyLabelC.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelC.setText("C");
            keyLabelC.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelC.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelC.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelC.setOpaque(true);
            keyLabelC.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelV.setBackground(java.awt.Color.white);
            keyLabelV.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelV.setForeground(java.awt.Color.black);
            keyLabelV.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelV.setText("V");
            keyLabelV.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelV.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelV.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelV.setOpaque(true);
            keyLabelV.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelB.setBackground(java.awt.Color.white);
            keyLabelB.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelB.setForeground(java.awt.Color.black);
            keyLabelB.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelB.setText("B");
            keyLabelB.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelB.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelB.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelB.setOpaque(true);
            keyLabelB.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelN.setBackground(java.awt.Color.white);
            keyLabelN.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelN.setForeground(java.awt.Color.black);
            keyLabelN.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelN.setText("N");
            keyLabelN.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelN.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelN.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelN.setOpaque(true);
            keyLabelN.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelM.setBackground(java.awt.Color.white);
            keyLabelM.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelM.setForeground(java.awt.Color.black);
            keyLabelM.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelM.setText("M");
            keyLabelM.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelM.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelM.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelM.setOpaque(true);
            keyLabelM.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelComma.setBackground(java.awt.Color.white);
            keyLabelComma.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelComma.setForeground(java.awt.Color.black);
            keyLabelComma.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelComma.setText(",");
            keyLabelComma.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelComma.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelComma.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelComma.setOpaque(true);
            keyLabelComma.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelPeriod.setBackground(java.awt.Color.white);
            keyLabelPeriod.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelPeriod.setForeground(java.awt.Color.black);
            keyLabelPeriod.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelPeriod.setText(".");
            keyLabelPeriod.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelPeriod.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPeriod.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPeriod.setOpaque(true);
            keyLabelPeriod.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelSlash.setBackground(java.awt.Color.white);
            keyLabelSlash.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelSlash.setForeground(java.awt.Color.black);
            keyLabelSlash.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelSlash.setText("/");
            keyLabelSlash.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelSlash.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelSlash.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelSlash.setOpaque(true);
            keyLabelSlash.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelRightShift.setBackground(java.awt.Color.white);
            keyLabelRightShift.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelRightShift.setForeground(java.awt.Color.black);
            keyLabelRightShift.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelRightShift.setText("Shift");
            keyLabelRightShift.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelRightShift.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightShift.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightShift.setOpaque(true);
            keyLabelRightShift.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelLeftControl.setBackground(java.awt.Color.white);
            keyLabelLeftControl.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelLeftControl.setForeground(java.awt.Color.black);
            keyLabelLeftControl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelLeftControl.setText("Ctrl");
            keyLabelLeftControl.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelLeftControl.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelLeftControl.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelLeftControl.setOpaque(true);
            keyLabelLeftControl.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelLeftStart.setBackground(java.awt.Color.white);
            keyLabelLeftStart.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelLeftStart.setForeground(java.awt.Color.black);
            keyLabelLeftStart.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelLeftStart.setText("Start");
            keyLabelLeftStart.setToolTipText("");
            keyLabelLeftStart.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelLeftStart.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
            keyLabelLeftStart.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
            keyLabelLeftStart.setOpaque(true);
            keyLabelLeftStart.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

            keyLabelLeftAlt.setBackground(java.awt.Color.white);
            keyLabelLeftAlt.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelLeftAlt.setForeground(java.awt.Color.black);
            keyLabelLeftAlt.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelLeftAlt.setText("Alt");
            keyLabelLeftAlt.setToolTipText("");
            keyLabelLeftAlt.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelLeftAlt.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelLeftAlt.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelLeftAlt.setOpaque(true);
            keyLabelLeftAlt.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelLeftCommand.setBackground(java.awt.Color.white);
            keyLabelLeftCommand.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelLeftCommand.setForeground(java.awt.Color.black);
            keyLabelLeftCommand.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelLeftCommand.setText("command");
            keyLabelLeftCommand.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelLeftCommand.setMaximumSize(new java.awt.Dimension(UIScale.scale(75), UIScale.scale(40)));
            keyLabelLeftCommand.setMinimumSize(new java.awt.Dimension(UIScale.scale(75), UIScale.scale(40)));
            keyLabelLeftCommand.setOpaque(true);
            keyLabelLeftCommand.setPreferredSize(new java.awt.Dimension(UIScale.scale(75), UIScale.scale(40)));

            keyLabelSpace.setBackground(java.awt.Color.white);
            keyLabelSpace.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelSpace.setForeground(java.awt.Color.black);
            keyLabelSpace.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelSpace.setText("Space");
            keyLabelSpace.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelSpace.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelSpace.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelSpace.setOpaque(true);
            keyLabelSpace.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelRightCommand.setBackground(java.awt.Color.white);
            keyLabelRightCommand.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelRightCommand.setForeground(java.awt.Color.black);
            keyLabelRightCommand.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelRightCommand.setText("command");
            keyLabelRightCommand.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelRightCommand.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightCommand.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightCommand.setOpaque(true);
            keyLabelRightCommand.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelRightAlt.setBackground(java.awt.Color.white);
            keyLabelRightAlt.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelRightAlt.setForeground(java.awt.Color.black);
            keyLabelRightAlt.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelRightAlt.setText("Alt");
            keyLabelRightAlt.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelRightAlt.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightAlt.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightAlt.setOpaque(true);
            keyLabelRightAlt.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelRightStart.setBackground(java.awt.Color.white);
            keyLabelRightStart.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelRightStart.setForeground(java.awt.Color.black);
            keyLabelRightStart.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelRightStart.setText("Start");
            keyLabelRightStart.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelRightStart.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightStart.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightStart.setOpaque(true);
            keyLabelRightStart.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelMenu.setBackground(java.awt.Color.white);
            keyLabelMenu.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelMenu.setForeground(java.awt.Color.black);
            keyLabelMenu.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelMenu.setText("Menu");
            keyLabelMenu.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelMenu.setMaximumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
            keyLabelMenu.setMinimumSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));
            keyLabelMenu.setOpaque(true);
            keyLabelMenu.setPreferredSize(new java.awt.Dimension(UIScale.scale(45), UIScale.scale(40)));

            keyLabelRightControl.setBackground(java.awt.Color.white);
            keyLabelRightControl.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelRightControl.setForeground(java.awt.Color.black);
            keyLabelRightControl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelRightControl.setText("Ctrl");
            keyLabelRightControl.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelRightControl.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightControl.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelRightControl.setOpaque(true);
            keyLabelRightControl.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            javax.swing.GroupLayout mainKeysPanelLayout = new javax.swing.GroupLayout(mainKeysPanel);
            mainKeysPanel.setLayout(mainKeysPanelLayout);
            mainKeysPanelLayout.setHorizontalGroup(
                mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(mainKeysPanelLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addGroup(mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainKeysPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelLeftControl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelLeftStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelLeftAlt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelLeftCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelSpace, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelRightCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelRightAlt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelRightStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelRightControl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainKeysPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelBackQuote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabel0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelMinus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelEquals, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelBackspace, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainKeysPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelEscape, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelF12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainKeysPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelTab, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelQ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelU, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelOpenBracket, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelCloseBracket, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelBackSlash, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(mainKeysPanelLayout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addGroup(mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(mainKeysPanelLayout.createSequentialGroup()
                                    .addComponent(keyLabelLeftShift, javax.swing.GroupLayout.DEFAULT_SIZE, UIScale.scale(107), Short.MAX_VALUE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelComma, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelSlash, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelRightShift, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(mainKeysPanelLayout.createSequentialGroup()
                                    .addComponent(keyLabelCapsLock, javax.swing.GroupLayout.DEFAULT_SIZE, UIScale.scale(84), Short.MAX_VALUE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelG, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelJ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelK, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelSemicolon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelQuote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelEnter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGap(0, 0, 0))
            );

            mainKeysPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {keyLabelLeftShift, keyLabelRightShift});

            mainKeysPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {keyLabelCapsLock, keyLabelEnter});

            mainKeysPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {keyLabelLeftControl, keyLabelRightControl});

            mainKeysPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {keyLabelLeftCommand, keyLabelRightCommand});

            mainKeysPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {keyLabelLeftAlt, keyLabelRightAlt});

            mainKeysPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {keyLabelLeftStart, keyLabelRightStart});

            mainKeysPanelLayout.setVerticalGroup(
                mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(mainKeysPanelLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addGroup(mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(keyLabelF1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelEscape, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(keyLabelBackQuote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabel0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelMinus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelEquals, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelBackspace, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(keyLabelTab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelQ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelU, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelOpenBracket, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelCloseBracket, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelBackSlash, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(keyLabelCapsLock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelG, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelJ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelK, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelSemicolon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelQuote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelEnter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(keyLabelLeftShift, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelComma, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelSlash, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelRightShift, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(mainKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(keyLabelLeftControl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelLeftCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelLeftAlt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelSpace, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelRightAlt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelRightCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelRightControl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelLeftStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(keyLabelRightStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(0, 0, 0))
            );

            keyLabelPrintScreen.setBackground(java.awt.Color.white);
            keyLabelPrintScreen.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelPrintScreen.setForeground(java.awt.Color.black);
            keyLabelPrintScreen.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelPrintScreen.setText("<html><center>Print<br/>Scrn</center></html>");
            keyLabelPrintScreen.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelPrintScreen.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPrintScreen.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPrintScreen.setOpaque(true);
            keyLabelPrintScreen.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelScrollLock.setBackground(java.awt.Color.white);
            keyLabelScrollLock.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelScrollLock.setForeground(java.awt.Color.black);
            keyLabelScrollLock.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelScrollLock.setText("<html><center>Scroll<br/>Lock</center></html>");
            keyLabelScrollLock.setToolTipText("");
            keyLabelScrollLock.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelScrollLock.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelScrollLock.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelScrollLock.setOpaque(true);
            keyLabelScrollLock.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelPause.setBackground(java.awt.Color.white);
            keyLabelPause.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelPause.setForeground(java.awt.Color.black);
            keyLabelPause.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelPause.setText("Pause");
            keyLabelPause.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelPause.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPause.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPause.setOpaque(true);
            keyLabelPause.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            javax.swing.GroupLayout topOtherKeysPanelLayout = new javax.swing.GroupLayout(topOtherKeysPanel);
            topOtherKeysPanel.setLayout(topOtherKeysPanelLayout);
            topOtherKeysPanelLayout.setHorizontalGroup(
                topOtherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(topOtherKeysPanelLayout.createSequentialGroup()
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                    .addComponent(keyLabelPrintScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(keyLabelScrollLock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(keyLabelPause, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, 0))
            );
            topOtherKeysPanelLayout.setVerticalGroup(
                topOtherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(topOtherKeysPanelLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addGroup(topOtherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(topOtherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(keyLabelPrintScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(keyLabelScrollLock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(keyLabelPause, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(0, 0, 0))
            );

            lastKeyPressedLabel.setBackground(java.awt.Color.white);
            lastKeyPressedLabel.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(12))); // NOI18N
            lastKeyPressedLabel.setForeground(java.awt.Color.gray);
            lastKeyPressedLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            lastKeyPressedLabel.setText("<html><center><i>Last Key Pressed:</i><br/><b>NONE</b></center></html>");
            lastKeyPressedLabel.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.lightGray, UIScale.scale(2)));
            lastKeyPressedLabel.setOpaque(true);

            javax.swing.GroupLayout topOtherKeysAndLastKeyPressedPanelLayout = new javax.swing.GroupLayout(topOtherKeysAndLastKeyPressedPanel);
            topOtherKeysAndLastKeyPressedPanel.setLayout(topOtherKeysAndLastKeyPressedPanelLayout);
            topOtherKeysAndLastKeyPressedPanelLayout.setHorizontalGroup(
                topOtherKeysAndLastKeyPressedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(topOtherKeysAndLastKeyPressedPanelLayout.createSequentialGroup()
                    .addComponent(topOtherKeysPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                    .addComponent(lastKeyPressedLabel))
            );
            topOtherKeysAndLastKeyPressedPanelLayout.setVerticalGroup(
                topOtherKeysAndLastKeyPressedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(topOtherKeysAndLastKeyPressedPanelLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addGroup(topOtherKeysAndLastKeyPressedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(topOtherKeysPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lastKeyPressedLabel, javax.swing.GroupLayout.PREFERRED_SIZE, UIScale.scale(40), javax.swing.GroupLayout.PREFERRED_SIZE)))
            );

            keyLabelInsert.setBackground(java.awt.Color.white);
            keyLabelInsert.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelInsert.setForeground(java.awt.Color.black);
            keyLabelInsert.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelInsert.setText("Ins");
            keyLabelInsert.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelInsert.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelInsert.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelInsert.setOpaque(true);
            keyLabelInsert.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelHome.setBackground(java.awt.Color.white);
            keyLabelHome.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelHome.setForeground(java.awt.Color.black);
            keyLabelHome.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelHome.setText("Home");
            keyLabelHome.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelHome.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelHome.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelHome.setOpaque(true);
            keyLabelHome.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelPageUp.setBackground(java.awt.Color.white);
            keyLabelPageUp.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelPageUp.setForeground(java.awt.Color.black);
            keyLabelPageUp.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelPageUp.setText("<html><center>Page<br/>Up</center></html>");
            keyLabelPageUp.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelPageUp.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPageUp.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPageUp.setOpaque(true);
            keyLabelPageUp.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelDelete.setBackground(java.awt.Color.white);
            keyLabelDelete.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelDelete.setForeground(java.awt.Color.black);
            keyLabelDelete.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelDelete.setText("Del");
            keyLabelDelete.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelDelete.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelDelete.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelDelete.setOpaque(true);
            keyLabelDelete.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelEnd.setBackground(java.awt.Color.white);
            keyLabelEnd.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelEnd.setForeground(java.awt.Color.black);
            keyLabelEnd.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelEnd.setText("End");
            keyLabelEnd.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelEnd.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelEnd.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelEnd.setOpaque(true);
            keyLabelEnd.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelPageDown.setBackground(java.awt.Color.white);
            keyLabelPageDown.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelPageDown.setForeground(java.awt.Color.black);
            keyLabelPageDown.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelPageDown.setText("<html><center>Page<br/>Down</center></html>");
            keyLabelPageDown.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelPageDown.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPageDown.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelPageDown.setOpaque(true);
            keyLabelPageDown.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            javax.swing.GroupLayout otherKeysPanelLayout = new javax.swing.GroupLayout(otherKeysPanel);
            otherKeysPanel.setLayout(otherKeysPanelLayout);
            otherKeysPanelLayout.setHorizontalGroup(
                otherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(otherKeysPanelLayout.createSequentialGroup()
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                    .addGroup(otherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(otherKeysPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelInsert, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelHome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelPageUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(otherKeysPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelDelete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelEnd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelPageDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
            );
            otherKeysPanelLayout.setVerticalGroup(
                otherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(otherKeysPanelLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addGroup(otherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(otherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(keyLabelInsert, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(keyLabelHome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(keyLabelPageUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(otherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(otherKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(keyLabelDelete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(keyLabelEnd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(keyLabelPageDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            );

            keyLabelArrowUp.setBackground(java.awt.Color.white);
            keyLabelArrowUp.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(14))); // NOI18N
            keyLabelArrowUp.setForeground(java.awt.Color.black);
            keyLabelArrowUp.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelArrowUp.setText("↑");
            keyLabelArrowUp.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelArrowUp.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelArrowUp.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelArrowUp.setOpaque(true);
            keyLabelArrowUp.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            arrowKeysAlignmentSpacer.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            arrowKeysAlignmentSpacer.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            arrowKeysAlignmentSpacer.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            javax.swing.GroupLayout arrowKeysAlignmentSpacerLayout = new javax.swing.GroupLayout(arrowKeysAlignmentSpacer);
            arrowKeysAlignmentSpacer.setLayout(arrowKeysAlignmentSpacerLayout);
            arrowKeysAlignmentSpacerLayout.setHorizontalGroup(
                arrowKeysAlignmentSpacerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, UIScale.scale(40), Short.MAX_VALUE)
            );
            arrowKeysAlignmentSpacerLayout.setVerticalGroup(
                arrowKeysAlignmentSpacerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, UIScale.scale(40), Short.MAX_VALUE)
            );

            keyLabelArrowLeft.setBackground(java.awt.Color.white);
            keyLabelArrowLeft.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(14))); // NOI18N
            keyLabelArrowLeft.setForeground(java.awt.Color.black);
            keyLabelArrowLeft.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelArrowLeft.setText("←");
            keyLabelArrowLeft.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelArrowLeft.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelArrowLeft.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelArrowLeft.setOpaque(true);
            keyLabelArrowLeft.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelArrowDown.setBackground(java.awt.Color.white);
            keyLabelArrowDown.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(14))); // NOI18N
            keyLabelArrowDown.setForeground(java.awt.Color.black);
            keyLabelArrowDown.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelArrowDown.setText("↓");
            keyLabelArrowDown.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelArrowDown.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelArrowDown.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelArrowDown.setOpaque(true);
            keyLabelArrowDown.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelArrowRight.setBackground(java.awt.Color.white);
            keyLabelArrowRight.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(14))); // NOI18N
            keyLabelArrowRight.setForeground(java.awt.Color.black);
            keyLabelArrowRight.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelArrowRight.setText("→");
            keyLabelArrowRight.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelArrowRight.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelArrowRight.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelArrowRight.setOpaque(true);
            keyLabelArrowRight.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            javax.swing.GroupLayout arrowKeysPanelLayout = new javax.swing.GroupLayout(arrowKeysPanel);
            arrowKeysPanel.setLayout(arrowKeysPanelLayout);
            arrowKeysPanelLayout.setHorizontalGroup(
                arrowKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(arrowKeysPanelLayout.createSequentialGroup()
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                    .addGroup(arrowKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(keyLabelArrowUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(arrowKeysPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelArrowLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelArrowDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(arrowKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(keyLabelArrowRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(arrowKeysAlignmentSpacer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            );
            arrowKeysPanelLayout.setVerticalGroup(
                arrowKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(arrowKeysPanelLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addGroup(arrowKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(arrowKeysPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelArrowUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(arrowKeysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(keyLabelArrowLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelArrowDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(arrowKeysPanelLayout.createSequentialGroup()
                            .addComponent(arrowKeysAlignmentSpacer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelArrowRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGap(0, 0, 0))
            );

            keyLabelNumPadNumLock.setBackground(java.awt.Color.white);
            keyLabelNumPadNumLock.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelNumPadNumLock.setForeground(java.awt.Color.black);
            keyLabelNumPadNumLock.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPadNumLock.setText("<html><center>Num<br/>Lock</center></html>");
            keyLabelNumPadNumLock.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPadNumLock.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadNumLock.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadNumLock.setOpaque(true);
            keyLabelNumPadNumLock.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPadEquals.setBackground(java.awt.Color.white);
            keyLabelNumPadEquals.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPadEquals.setForeground(java.awt.Color.black);
            keyLabelNumPadEquals.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPadEquals.setText("=");
            keyLabelNumPadEquals.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPadEquals.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadEquals.setMinimumSize(new java.awt.Dimension(UIScale.scale(20), UIScale.scale(40)));
            keyLabelNumPadEquals.setOpaque(true);
            keyLabelNumPadEquals.setPreferredSize(new java.awt.Dimension(UIScale.scale(20), UIScale.scale(40)));

            keyLabelNumPadDivide.setBackground(java.awt.Color.white);
            keyLabelNumPadDivide.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPadDivide.setForeground(java.awt.Color.black);
            keyLabelNumPadDivide.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPadDivide.setText("/");
            keyLabelNumPadDivide.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPadDivide.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadDivide.setMinimumSize(new java.awt.Dimension(UIScale.scale(20), UIScale.scale(40)));
            keyLabelNumPadDivide.setOpaque(true);
            keyLabelNumPadDivide.setPreferredSize(new java.awt.Dimension(UIScale.scale(20), UIScale.scale(40)));

            keyLabelNumPadMultiply.setBackground(java.awt.Color.white);
            keyLabelNumPadMultiply.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPadMultiply.setForeground(java.awt.Color.black);
            keyLabelNumPadMultiply.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPadMultiply.setText("*");
            keyLabelNumPadMultiply.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPadMultiply.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadMultiply.setMinimumSize(new java.awt.Dimension(UIScale.scale(20), UIScale.scale(40)));
            keyLabelNumPadMultiply.setOpaque(true);
            keyLabelNumPadMultiply.setPreferredSize(new java.awt.Dimension(UIScale.scale(20), UIScale.scale(40)));

            keyLabelNumPadSubtract.setBackground(java.awt.Color.white);
            keyLabelNumPadSubtract.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPadSubtract.setForeground(java.awt.Color.black);
            keyLabelNumPadSubtract.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPadSubtract.setText("-");
            keyLabelNumPadSubtract.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPadSubtract.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadSubtract.setMinimumSize(new java.awt.Dimension(UIScale.scale(20), UIScale.scale(40)));
            keyLabelNumPadSubtract.setOpaque(true);
            keyLabelNumPadSubtract.setPreferredSize(new java.awt.Dimension(UIScale.scale(20), UIScale.scale(40)));

            keyLabelNumPadMacSubtract.setBackground(java.awt.Color.white);
            keyLabelNumPadMacSubtract.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPadMacSubtract.setForeground(java.awt.Color.black);
            keyLabelNumPadMacSubtract.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPadMacSubtract.setText("-");
            keyLabelNumPadMacSubtract.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPadMacSubtract.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadMacSubtract.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadMacSubtract.setOpaque(true);
            keyLabelNumPadMacSubtract.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPadAdd.setBackground(java.awt.Color.white);
            keyLabelNumPadAdd.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPadAdd.setForeground(java.awt.Color.black);
            keyLabelNumPadAdd.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPadAdd.setText("+");
            keyLabelNumPadAdd.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPadAdd.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadAdd.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadAdd.setOpaque(true);
            keyLabelNumPadAdd.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPadEnter.setBackground(java.awt.Color.white);
            keyLabelNumPadEnter.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(11))); // NOI18N
            keyLabelNumPadEnter.setForeground(java.awt.Color.black);
            keyLabelNumPadEnter.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPadEnter.setText("Enter");
            keyLabelNumPadEnter.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPadEnter.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(86)));
            keyLabelNumPadEnter.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(86)));
            keyLabelNumPadEnter.setOpaque(true);
            keyLabelNumPadEnter.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(86)));

            keyLabelNumPad7.setBackground(java.awt.Color.white);
            keyLabelNumPad7.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad7.setForeground(java.awt.Color.black);
            keyLabelNumPad7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad7.setText("7");
            keyLabelNumPad7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad7.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad7.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad7.setOpaque(true);
            keyLabelNumPad7.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPad8.setBackground(java.awt.Color.white);
            keyLabelNumPad8.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad8.setForeground(java.awt.Color.black);
            keyLabelNumPad8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad8.setText("8");
            keyLabelNumPad8.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad8.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad8.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad8.setOpaque(true);
            keyLabelNumPad8.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPad9.setBackground(java.awt.Color.white);
            keyLabelNumPad9.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad9.setForeground(java.awt.Color.black);
            keyLabelNumPad9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad9.setText("9");
            keyLabelNumPad9.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad9.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad9.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad9.setOpaque(true);
            keyLabelNumPad9.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPad4.setBackground(java.awt.Color.white);
            keyLabelNumPad4.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad4.setForeground(java.awt.Color.black);
            keyLabelNumPad4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad4.setText("4");
            keyLabelNumPad4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad4.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad4.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad4.setOpaque(true);
            keyLabelNumPad4.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPad5.setBackground(java.awt.Color.white);
            keyLabelNumPad5.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad5.setForeground(java.awt.Color.black);
            keyLabelNumPad5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad5.setText("5");
            keyLabelNumPad5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad5.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad5.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad5.setOpaque(true);
            keyLabelNumPad5.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPad6.setBackground(java.awt.Color.white);
            keyLabelNumPad6.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad6.setForeground(java.awt.Color.black);
            keyLabelNumPad6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad6.setText("6");
            keyLabelNumPad6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad6.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad6.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad6.setOpaque(true);
            keyLabelNumPad6.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPad1.setBackground(java.awt.Color.white);
            keyLabelNumPad1.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad1.setForeground(java.awt.Color.black);
            keyLabelNumPad1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad1.setText("1");
            keyLabelNumPad1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad1.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad1.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad1.setOpaque(true);
            keyLabelNumPad1.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPad2.setBackground(java.awt.Color.white);
            keyLabelNumPad2.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad2.setForeground(java.awt.Color.black);
            keyLabelNumPad2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad2.setText("2");
            keyLabelNumPad2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad2.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad2.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad2.setOpaque(true);
            keyLabelNumPad2.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPad3.setBackground(java.awt.Color.white);
            keyLabelNumPad3.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad3.setForeground(java.awt.Color.black);
            keyLabelNumPad3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad3.setText("3");
            keyLabelNumPad3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad3.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad3.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad3.setOpaque(true);
            keyLabelNumPad3.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPad0.setBackground(java.awt.Color.white);
            keyLabelNumPad0.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPad0.setForeground(java.awt.Color.black);
            keyLabelNumPad0.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPad0.setText("0");
            keyLabelNumPad0.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPad0.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad0.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPad0.setOpaque(true);
            keyLabelNumPad0.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            keyLabelNumPadDecimal.setBackground(java.awt.Color.white);
            keyLabelNumPadDecimal.setFont(new java.awt.Font("Helvetica", 0, UIScale.scale(13))); // NOI18N
            keyLabelNumPadDecimal.setForeground(java.awt.Color.black);
            keyLabelNumPadDecimal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            keyLabelNumPadDecimal.setText(".");
            keyLabelNumPadDecimal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), UIScale.scale(2)));
            keyLabelNumPadDecimal.setMaximumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadDecimal.setMinimumSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));
            keyLabelNumPadDecimal.setOpaque(true);
            keyLabelNumPadDecimal.setPreferredSize(new java.awt.Dimension(UIScale.scale(40), UIScale.scale(40)));

            javax.swing.GroupLayout numPadPanelLayout = new javax.swing.GroupLayout(numPadPanel);
            numPadPanel.setLayout(numPadPanelLayout);
            numPadPanelLayout.setHorizontalGroup(
                numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(numPadPanelLayout.createSequentialGroup()
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                    .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(numPadPanelLayout.createSequentialGroup()
                            .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(numPadPanelLayout.createSequentialGroup()
                                    .addComponent(keyLabelNumPad0, javax.swing.GroupLayout.PREFERRED_SIZE, UIScale.scale(86), javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(keyLabelNumPadDecimal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(numPadPanelLayout.createSequentialGroup()
                                        .addComponent(keyLabelNumPad7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(keyLabelNumPad8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(keyLabelNumPad9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(numPadPanelLayout.createSequentialGroup()
                                        .addComponent(keyLabelNumPad4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(keyLabelNumPad5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(keyLabelNumPad6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(numPadPanelLayout.createSequentialGroup()
                                        .addComponent(keyLabelNumPad1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(keyLabelNumPad2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(keyLabelNumPad3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(keyLabelNumPadEnter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelNumPadAdd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelNumPadMacSubtract, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(numPadPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelNumPadNumLock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelNumPadEquals, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelNumPadDivide, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelNumPadMultiply, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelNumPadSubtract, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGap(0, 0, 0))
            );
            numPadPanelLayout.setVerticalGroup(
                numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(numPadPanelLayout.createSequentialGroup()
                    .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(keyLabelNumPadEquals, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(keyLabelNumPadDivide, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(keyLabelNumPadNumLock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(keyLabelNumPadMultiply, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(keyLabelNumPadSubtract, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(numPadPanelLayout.createSequentialGroup()
                            .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(keyLabelNumPad7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelNumPad8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelNumPad9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(keyLabelNumPad4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelNumPad5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelNumPad6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(keyLabelNumPad3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelNumPad2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelNumPad1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(numPadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(keyLabelNumPadDecimal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(keyLabelNumPad0, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, numPadPanelLayout.createSequentialGroup()
                            .addComponent(keyLabelNumPadMacSubtract, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelNumPadAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keyLabelNumPadEnter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
            );

            javax.swing.GroupLayout contentPaneLayout = new javax.swing.GroupLayout(contentPane);
            contentPane.setLayout(contentPaneLayout);
            contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                    .addGroup(contentPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(textAreaScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(contentPaneLayout.createSequentialGroup()
                            .addComponent(mainKeysPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(contentPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(contentPaneLayout.createSequentialGroup()
                                    .addGroup(contentPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(otherKeysPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(arrowKeysPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGap(0, 0, Short.MAX_VALUE)
                                    .addComponent(numPadPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addComponent(topOtherKeysAndLastKeyPressedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18)))
            );
            contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                    .addComponent(textAreaScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                    .addGroup(contentPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(mainKeysPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(contentPaneLayout.createSequentialGroup()
                            .addComponent(topOtherKeysAndLastKeyPressedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(contentPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(numPadPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(contentPaneLayout.createSequentialGroup()
                                    .addComponent(otherKeysPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(arrowKeysPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18)))
            );

            contentScrollPane.setViewportView(contentPane);

            mainMenuBar.setFocusable(false);

            editMenu.setText("Edit");
            editMenu.setFocusTraversalKeysEnabled(false);
            editMenu.setFocusable(false);

            resetPressedKeysMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            resetPressedKeysMenuItem.setText("Reset Pressed Keys");
            resetPressedKeysMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    resetPressedKeysMenuItemActionPerformed(evt);
                }
            });
            editMenu.add(resetPressedKeysMenuItem);

            toggleFullKeyboardMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            toggleFullKeyboardMenuItem.setText("Toggle Full Keyboard");
            toggleFullKeyboardMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    toggleFullKeyboardMenuItemActionPerformed(evt);
                }
            });
            editMenu.add(toggleFullKeyboardMenuItem);

            mainMenuBar.add(editMenu);

            uiScaleMenu.setText("UI Scale");

            resetUIScaleMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            resetUIScaleMenuItem.setText("Reset UI Scale");
            resetUIScaleMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    resetUIScaleMenuItemActionPerformed(evt);
                }
            });
            uiScaleMenu.add(resetUIScaleMenuItem);

            increaseUIScaleMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            increaseUIScaleMenuItem.setText("Increase UI Scale");
            increaseUIScaleMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    increaseUIScaleMenuItemActionPerformed(evt);
                }
            });
            uiScaleMenu.add(increaseUIScaleMenuItem);

            decreaseUIScaleMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            decreaseUIScaleMenuItem.setText("Decrease UI Scale ");
            decreaseUIScaleMenuItem.setToolTipText("");
            decreaseUIScaleMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    decreaseUIScaleMenuItemActionPerformed(evt);
                }
            });
            uiScaleMenu.add(decreaseUIScaleMenuItem);

            mainMenuBar.add(uiScaleMenu);

            setJMenuBar(mainMenuBar);

            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
            getContentPane().setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(contentScrollPane)
                    .addGap(0, 0, 0))
            );
            layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(contentScrollPane)
                    .addGap(0, 0, 0))
            );

            pack();
        }// </editor-fold>//GEN-END:initComponents

    private void onKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_onKeyPressed
        if (textArea.getCaretColor().equals(Color.WHITE)) {
            textArea.setText("");
            textArea.setFont(new Font("Helvetica", 0, UIScale.scale(14)));
            textArea.setForeground(Color.BLACK);
            textArea.setCaretColor(Color.DARK_GRAY);
        }

        int keyCode = evt.getKeyCode();

        if ((keyCode == KeyEvent.VK_CAPS_LOCK) || (!isMacOS && ((keyCode == KeyEvent.VK_NUM_LOCK) || (keyCode == KeyEvent.VK_SCROLL_LOCK)))) {
            updateLockKeysState();
        } else if (!isMacOS && ((keyCode == KeyEvent.VK_ALT) || (keyCode == KeyEvent.VK_F10))) { // If Alt or F10 keys are pressed, it will highlight the menu bar and steal focus from the "textArea" and interrupt typing, so manually re-focus "textArea" after a half second delay.
            (new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    TimeUnit.MILLISECONDS.sleep(500);

                    return null;
                }

                @Override
                protected void done() {
                    textArea.requestFocusInWindow();
                }
            }).execute();
        }

        if (debugLogging) {
            System.out.println("-----\n"
                    + "keyLocation INT: " + evt.getKeyLocation());
        }

        String keyLocation = "";
        switch (evt.getKeyLocation()) {
            case KeyEvent.KEY_LOCATION_LEFT:
                keyLocation = "Left";
                break;
            case KeyEvent.KEY_LOCATION_RIGHT:
                keyLocation = "Right";
                break;
            case KeyEvent.KEY_LOCATION_NUMPAD:
                keyLocation = "NumPad";
        }

        String keyCodeText = KeyEvent.getKeyText(keyCode);
        if (keyCodeText.equals("Windows")) {
            keyCodeText = "Start"; // Re-name "Windows" Key to "Start" Key for cross-platform consistency.
        } else if (keyCodeText.startsWith("Unknown keyCode: ")) {
            if (isMacOS && (keyCode == 0)) {
                if (evt.getModifiersEx() != 0) { // On macOS, if two of the SAME modifiers are pressed at the same time, 0 key codes can be sent after the actual modifier key code was already sent. So, just ignore any 0 key codes if any modifiers are currently held down.
                    return;
                } else {
                    keyCodeText = "fn"; // On Mac keyboards, pressing the "fn" key by itself sends a key code 0, so show it as the "fn" key (only when no modifiers are held down for reasons described above).
                }
            } else {
                keyCodeText = keyCodeText.replace("Unknown keyCode: ", "UNKNOWN (") + ")";
            }
        } else if (isMacOS && keyCodeText.startsWith("⌨")) { // On Mac keyboards, the NumPad keys start with this Unicode Keyboard symbol for some reason.
            keyCodeText = keyCodeText.replace("⌨", "NumPad");
        }

        if (keyCodeText.startsWith("NumPad-")) { // NumPad Numbers show as "NumPad-1", etc. Get rid of the dash since I don't think it looks good.
            keyCodeText = keyCodeText.replace("NumPad-", "NumPad ");
        }

        if (!keyLocation.isEmpty() && !keyCodeText.startsWith(keyLocation)) { // Include the "keyLocation" in the "keyCodeText" for display (this also catches some NumPad keys that don't start with "NumPad ").
            keyCodeText = keyLocation + " " + keyCodeText;
        }

        lastKeyPressedLabel.setText("<html><center><i>Last Key Pressed:</i><br/><b>" + keyCodeText + "</b></center></html>");
        if (!lastKeyPressedLabel.getBackground().equals(Color.WHITE)) { // Reset "lastKeyPressedLabel" colors in case an unknown key was pressed last and the box is highlighted.
            lastKeyPressedLabel.setBorder(lastKeyPressedLabelBorder);
            lastKeyPressedLabel.setBackground(Color.WHITE);
            lastKeyPressedLabel.setForeground(Color.GRAY);
        }

        if (!isMacOS) {
            try {
                if (keyLocation.equals("NumPad") && !Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK)) {
                    if (debugLogging) {
                        System.out.println("ACTUAL NumPad keyCode: " + keyCode);
                    }

                    switch (keyCode) { // Translate NumPad Navigation Keys to their regular NumPad Number Key equivalents to highlight the physical key being pressed regardless of whether or not Num Lock is enabled.
                        case KeyEvent.VK_HOME:
                            keyCode = KeyEvent.VK_NUMPAD7;
                            break;
                        case KeyEvent.VK_UP: // Key Code KV_UP (38) on Windows
                        case KeyEvent.VK_KP_UP: // and KV_KP_UP (224) on Linux
                            keyCode = KeyEvent.VK_NUMPAD8;
                            break;
                        case KeyEvent.VK_PAGE_UP:
                            keyCode = KeyEvent.VK_NUMPAD9;
                            break;
                        case KeyEvent.VK_LEFT: // Key Code VK_LEFT (37) on Windows
                        case KeyEvent.VK_KP_LEFT: // and VK_KP_LEFT (226) on Linux
                            keyCode = KeyEvent.VK_NUMPAD4;
                            break;
                        case KeyEvent.VK_CLEAR: // Key Code VK_CLEAR (12) on Windows
                        case KeyEvent.VK_BEGIN: // and VK_BEGIN (65368) on Linux
                            // NOTE: This may conflict if a Mac keyboard is being tested on Windows since the "=" in the NumPad is recognized as "Clear" regardless of the Num Lock state,
                            // so that means that when Num Lock is OFF the "5" could be highlighted when the physical NumPad "=" key is being pressed on a Mac keyboard in Windows.
                            keyCode = KeyEvent.VK_NUMPAD5;
                            break;
                        case KeyEvent.VK_RIGHT: // Key Code VK_RIGHT (39) on Windows
                        case KeyEvent.VK_KP_RIGHT: // and VK_KP_RIGHT (227) on Linux
                            keyCode = KeyEvent.VK_NUMPAD6;
                            break;
                        case KeyEvent.VK_END:
                            keyCode = KeyEvent.VK_NUMPAD1;
                            break;
                        case KeyEvent.VK_DOWN: // Key Code VK_DOWN (40) on Windows
                        case KeyEvent.VK_KP_DOWN: // and VK_KP_DOWN (225) on Linux
                            keyCode = KeyEvent.VK_NUMPAD2;
                            break;
                        case KeyEvent.VK_PAGE_DOWN:
                            keyCode = KeyEvent.VK_NUMPAD3;
                            break;
                        case KeyEvent.VK_INSERT:
                            keyCode = KeyEvent.VK_NUMPAD0;
                            break;
                        case KeyEvent.VK_DELETE:
                            keyCode = KeyEvent.VK_DECIMAL;
                            break;
                    }
                }
            } catch (UnsupportedOperationException updateLockKeysStateException) {
                if (debugLogging) {
                    System.out.println("updateLockKeysStateException: " + updateLockKeysStateException);
                }
            }
        }

        String keyLabelKeyCodeName = "keyLabel" + keyLocation + keyCode;

        if (debugLogging) {
            System.out.println("keyLabelKeyCodeName: " + keyLabelKeyCodeName);
        }

        JLabel pressedKeyLabel = keyLabels.get(keyLabelKeyCodeName);
        if (pressedKeyLabel == null) {
            if (debugLogging) {
                System.out.println("keyLabelKeyCodeName NOT FOUND: " + keyLabelKeyCodeName);
            }

            lastKeyPressedLabel.setBorder(keyLabelOrangeHighlightBorder);
            lastKeyPressedLabel.setBackground(Color.ORANGE);
            lastKeyPressedLabel.setForeground(Color.BLACK);

            (new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    TimeUnit.MILLISECONDS.sleep(200);

                    return null;
                }

                @Override
                protected void done() {
                    if (lastKeyPressedLabel.getBackground().equals(Color.ORANGE)) { // If keyboard was reset, the button will no longer be Orange and the color should not be changed to Green after the delay.
                        lastKeyPressedLabel.setBorder(keyLabelGreenHighlightBorder);
                        lastKeyPressedLabel.setBackground(keyLabelGreenHighlightBackgroundColor);
                        lastKeyPressedLabel.setForeground(Color.WHITE);
                    }

                    textArea.requestFocusInWindow();
                }
            }).execute();
        } else {
            if ((!pressedKeyLabel.isVisible() && keyLocation.equals("Right"))
                    || (!topOtherKeysPanel.isVisible() && ((keyCode == KeyEvent.VK_PRINTSCREEN) || (keyCode == KeyEvent.VK_SCROLL_LOCK) || (keyCode == KeyEvent.VK_PAUSE)))
                    || (!otherKeysPanel.isVisible() && ((keyCode == KeyEvent.VK_INSERT) || (keyCode == KeyEvent.VK_HOME) || (keyCode == KeyEvent.VK_PAGE_UP) || (keyCode == KeyEvent.VK_DELETE) || (keyCode == KeyEvent.VK_END) || (keyCode == KeyEvent.VK_PAGE_DOWN)))
                    || (!numPadPanel.isVisible() && keyLocation.equals("NumPad"))) {
                toggleFullKeyboardMenuItemActionPerformed(null);
            }

            pressedKeyLabel.setBorder(keyLabelOrangeHighlightBorder);
            pressedKeyLabel.setBackground(Color.ORANGE);
            pressedKeyLabel.setForeground(Color.BLACK);

            (new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    TimeUnit.MILLISECONDS.sleep(200);

                    return null;
                }

                @Override
                protected void done() {
                    if (pressedKeyLabel.getBackground().equals(Color.ORANGE)) { // If keyboard was reset, the button will no longer be Orange and the color should not be changed to Green after the delay.
                        pressedKeyLabel.setBorder(keyLabelGreenHighlightBorder);
                        pressedKeyLabel.setBackground(keyLabelGreenHighlightBackgroundColor);
                        pressedKeyLabel.setForeground(Color.WHITE);
                    }

                    textArea.requestFocusInWindow();
                }
            }).execute();

            if (isMacLaptop && !didPressAllMacLaptopKeyboardKeys && !fullKeyboardHasBeenShown) { // Can only *know* if all keys have been pressed on Mac laptops since they are the only devices with consistent keyboards across all models.
                int pressedKeyCount = 0;
                for (JLabel thisKeyLabel : keyLabels.values()) {
                    if (!thisKeyLabel.getBackground().equals(Color.WHITE)) {
                        pressedKeyCount++;
                    }
                }

                if (pressedKeyCount == 77) {
                    didPressAllMacLaptopKeyboardKeys = true;

                    JFrame keyboardTestWindow = this;
                    (new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            TimeUnit.MILLISECONDS.sleep(400);

                            return null;
                        }

                        @Override
                        protected void done() {
                            boolean isRunningFromQAHelper = false;
                            ArrayList<String> launchNextMacTestBootAppDialogButtons = new ArrayList<>();
                            try {
                                String launchPath = new File(KeyboardTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
                                if (launchPath.contains("/qa_helper-Keyboard_Test") && launchPath.endsWith(".jar")) {
                                    isRunningFromQAHelper = true;
                                } else if (launchPath.equals("/Applications/Keyboard Test.app/Contents/app/Keyboard_Test.jar") && new File("/Applications/Test Boot Setup.app").exists() && System.getProperty("user.name").equals("Tester")) {
                                    if (new File("/Applications/CPU Stress Test.app").exists()) {
                                        launchNextMacTestBootAppDialogButtons.add("Launch \"CPU Stress Test\"");
                                    }
                                    if (new File("/Applications/DriveDx.app").exists()) {
                                        launchNextMacTestBootAppDialogButtons.add("Launch \"DriveDx\"");
                                    }
                                }
                            } catch (URISyntaxException checkMacLaunchPathException) {
                                if (debugLogging) {
                                    System.out.println("checkMacLaunchPathException: " + checkMacLaunchPathException);
                                }
                            }

                            // Make sure all pressed buttons are Green before the dialog is displayed so they aren't stuck as Orange while the dialog is displayed.
                            for (JLabel thisKeyLabel : keyLabels.values()) {
                                if (thisKeyLabel.getBackground().equals(Color.ORANGE)) {
                                    thisKeyLabel.setBorder(keyLabelGreenHighlightBorder);
                                    thisKeyLabel.setBackground(keyLabelGreenHighlightBackgroundColor);
                                    thisKeyLabel.setForeground(Color.WHITE);
                                }
                            }

                            if (lastKeyPressedLabel.getBackground().equals(Color.ORANGE)) {
                                lastKeyPressedLabel.setBorder(keyLabelGreenHighlightBorder);
                                lastKeyPressedLabel.setBackground(keyLabelGreenHighlightBackgroundColor);
                                lastKeyPressedLabel.setForeground(Color.WHITE);
                            }

                            String[] everyKeyPressedDialogButtons = new String[]{(isRunningFromQAHelper ? "Quit & Return to \"QA Helper\"" : (launchNextMacTestBootAppDialogButtons.isEmpty() ? "Quit" : "Continue")), "Reset Keyboard Test"};
                            int everyKeyPressedDialogReturn = JOptionPane.showOptionDialog(keyboardTestWindow, "<html>"
                                    + "<b style=\"color: orange;\">Every Key Was Pressed!</b><br/>"
                                    + "<br/><br/>"
                                    + "<b style=\"color: green;\"><u>KEYBOARD TEST PASSED IF:</u></b><br/>"
                                    + "- Every key functioned correctly.<br/>"
                                    + "- No keys felt funky in any way.<br/>"
                                    + "- No keys felt sticky or got stuck down.<br/>"
                                    + "- No key caps are broken or missing.<br/>"
                                    + "<br/><br/>"
                                    + "<b style=\"color: #D83048;\"><u>KEYBOARD TEST FAILED IF:</u></b><br/>"
                                    + "- Any key did not function correctly.<br/>"
                                    + "- Any key triggered the wrong key.<br/>"
                                    + "- Any key triggered multiple keys.<br/>"
                                    + "- Any key felt funky in any way.<br/>"
                                    + "- Any key felt sticky or got stuck down.<br/>"
                                    + "- Any key caps are broken or missing."
                                    + "</html>", "Finished Keyboard Test", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, everyKeyPressedDialogButtons, everyKeyPressedDialogButtons[0]);

                            if (everyKeyPressedDialogReturn == 0) {
                                if (isRunningFromQAHelper) {
                                    try {
                                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", "-b", "org.freegeek.QA-Helper"}).waitFor();
                                    } catch (IOException | InterruptedException focusQAHelperException) {
                                        if (debugLogging) {
                                            System.out.println("focusQAHelperException: " + focusQAHelperException);
                                        }
                                    }
                                } else if (!launchNextMacTestBootAppDialogButtons.isEmpty()) {
                                    launchNextMacTestBootAppDialogButtons.add("Quit");
                                    int launchNextMacTestBootAppDialogReturn = JOptionPane.showOptionDialog(keyboardTestWindow, "What would you like to do next?", "Finished Keyboard Test", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, launchNextMacTestBootAppDialogButtons.toArray(), launchNextMacTestBootAppDialogButtons.get(0));

                                    String launchNextMacTestBootAppDialogDialogResponseString = "Quit";
                                    if (launchNextMacTestBootAppDialogReturn > -1) {
                                        launchNextMacTestBootAppDialogDialogResponseString = launchNextMacTestBootAppDialogButtons.get(launchNextMacTestBootAppDialogReturn);
                                    }

                                    if (!launchNextMacTestBootAppDialogDialogResponseString.equals("Quit")) {
                                        try {
                                            Runtime.getRuntime().exec(new String[]{"/usr/bin/open", "-a", "/Applications/" + (launchNextMacTestBootAppDialogDialogResponseString.contains("DriveDx") ? "DriveDx" : "CPU Stress Test") + ".app"}).waitFor();
                                        } catch (IOException | InterruptedException launchNextMacTestBootAppException) {
                                            if (debugLogging) {
                                                System.out.println("launchNextMacTestBootAppException: " + launchNextMacTestBootAppException);
                                            }
                                        }
                                    }
                                }

                                System.exit(0);
                            } else {
                                resetPressedKeysMenuItemActionPerformed(null);
                            }
                        }
                    }).execute();
                }
            }
        }
    }//GEN-LAST:event_onKeyPressed

    private void resetPressedKeysMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetPressedKeysMenuItemActionPerformed
        if (isMacLaptop) {
            didPressAllMacLaptopKeyboardKeys = false;
        }

        (new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (Map.Entry<String, JLabel> thisKeyLabelEntry : keyLabels.entrySet()) { // Since a "keyLabels" is a "LinkedHashMap", the order of keys and values is preserved (https://stackoverflow.com/a/2924143).
                    if (!thisKeyLabelEntry.getValue().getBackground().equals(Color.WHITE)) {
                        TimeUnit.MILLISECONDS.sleep(10); // Add a slight delay so there is a nice looking affect of the keys being reset in order across the keyboard.

                        publish(thisKeyLabelEntry.getKey());
                    }
                }

                return null;
            }

            @Override
            protected void process(java.util.List<String> tasks) {
                tasks.forEach((thisKeyLabelKey) -> {
                    JLabel thisKeyLabel = keyLabels.get(thisKeyLabelKey);
                    thisKeyLabel.setBorder(keyLabelBorder);
                    thisKeyLabel.setBackground(Color.WHITE);
                    thisKeyLabel.setForeground(Color.BLACK);
                });
            }
        }).execute();

        lastKeyPressedLabel.setText("<html><center><i>Last Key Pressed:</i><br/><b>NONE</b></center></html>");
        if (!lastKeyPressedLabel.getBackground().equals(Color.WHITE)) {
            lastKeyPressedLabel.setBorder(lastKeyPressedLabelBorder);
            lastKeyPressedLabel.setBackground(Color.WHITE);
            lastKeyPressedLabel.setForeground(Color.GRAY);
        }

        textArea.setText("- The best way to test a keyboard is to TYPE ACTUAL WORDS and make sure that exactly what you typed shows up in this text box and that each key below this text box highlights GREEN as you type.\n"
                + "\n"
                + "- Start testing the keyboard by typing \"The quick brown fox jumps over the lazy dog.\"\n"
                + "\n"
                + "SCROLL DOWN FOR MORE KEYBOARD TESTING TIPS\n"
                + "\n"
                + "- When a key is pressed on the keyboard, it will momentarily highlight ORANGE and then highlight GREEN in this window.\n"
                + "\n"
                + "- You SHOULD NOT just slide your finger across the keyboard to hit every key. With water damaged keyboards, it’s common that one key on the keyboard may trigger the wrong key, or multiple keys. Also, modifier keys such as Shift, Control, " + (isMacOS ? "Option" : "Alt") + ", etc could be stuck down which can make other keys behave incorrectly.\n"
                + "\n"
                + "- DO NOT just press the Shift, " + (isMacOS ? "Option" : "Alt") + ", and Caps Lock keys by themselves. Type while using these keys to make sure they are working properly.\n"
                + "\n"
                + (isMacOS ? "- The Caps Lock key WILL highlight green when turned ON, but WILL NOT highlight green when being turned OFF.\n\n" : "")
                + "- You MAY need to hold down the FN key to test the top row of Function keys.\n"
                + "\n"
                + "- Also, make sure that no keys feel funky, sticky, or get stuck down as you type.");
        textArea.setFont(new Font("Helvetica", 0, UIScale.scale(isMacOS ? 17 : 14)));
        textArea.setForeground(Color.GRAY);
        textArea.setCaretColor(Color.WHITE);
        textArea.setCaretPosition(0);
        textAreaScrollPane.getVerticalScrollBar().setValue(0);
        textAreaScrollPane.getHorizontalScrollBar().setValue(0);
        textArea.requestFocusInWindow();
    }//GEN-LAST:event_resetPressedKeysMenuItemActionPerformed

    private void toggleFullKeyboardMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleFullKeyboardMenuItemActionPerformed
        if (!isTogglingFullKeyboard) {
            isTogglingFullKeyboard = true;

            numPadPanel.setVisible(!numPadPanel.isVisible());

            if (isMacOS) {
                otherKeysPanel.setVisible(!otherKeysPanel.isVisible());
                keyLabelRightControl.setVisible(!keyLabelRightControl.isVisible()); // On Mac keyboards, Right Control Key is only shown on full keyboard layouts.
            } else {
                topOtherKeysPanel.setVisible(!topOtherKeysPanel.isVisible());

                if (isWindows) {
                    keyLabelRightStart.setVisible(!keyLabelRightStart.isVisible()); // If a Right Start/Windows Key exists, it usually only exists on full keyboard layouts (and it will only be detected on Windows since Linux doesn't differentiate between Left and Right Start/Windows Keys).
                }
            }

            if (!fullKeyboardHasBeenShown && numPadPanel.isVisible()) {
                fullKeyboardHasBeenShown = true;
            }

            setMinimumSize(null); // Clear minimum and preferred sizes so pack can go smaller than current size if size was reduced to fit screen.
            setPreferredSize(null);

            pack();
            textArea.requestFocusInWindow();

            // The following code to set the max window size based on the screen size is based on code from QA Helper (Copyright Free Geek - MIT License): https://github.com/freegeek-pdx/Java-QA-Helper/blob/b511d0259a657d1eebd4224a0950094f03d48156/src/GUI/QAHelper.java#L3221-L3287
            // setMaximumSize does not work. But do not want the windows to get larger than the screen.
            // The window contents are within a scroll pane, so if we set the window size to the screen size, the contents can still be scrolled.
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
            screenSize.height -= screenInsets.top + screenInsets.bottom;
            screenSize.width -= screenInsets.left + screenInsets.right;

            Dimension windowSize = getSize();
            Dimension reducedWindowSize = getSize();

            int scrollbarWidth = (isMacOS ? 12 : 10);
            if (UIManager.get("ScrollBar.width") != null) {
                scrollbarWidth = (int) UIManager.get("ScrollBar.width");
            }

            boolean didReduceWidth = false;
            if (windowSize.width > screenSize.width) {
                reducedWindowSize.width = screenSize.width;
                didReduceWidth = true;

                // If reduced width, add the height of the scrollbar to the window to not
                // need to also scroll vertically since the scrollbar would block content.
                reducedWindowSize.height += UIScale.scale(scrollbarWidth);

                if (reducedWindowSize.height > screenSize.height) {
                    reducedWindowSize.height = screenSize.height;
                }
            }

            if (windowSize.height > screenSize.height) {
                reducedWindowSize.height = screenSize.height;

                if (!didReduceWidth) {
                    // If reduced height, add the width of the scrollbar to the window to not
                    // need to also scroll horizonally since the scrollbar would block content.
                    reducedWindowSize.width += UIScale.scale(scrollbarWidth);

                    if (reducedWindowSize.width > screenSize.width) {
                        reducedWindowSize.width = screenSize.width;
                    }
                }
            }

            if (!reducedWindowSize.equals(windowSize)) {
                setMinimumSize(reducedWindowSize);
                setPreferredSize(reducedWindowSize);
                setSize(reducedWindowSize);
                textArea.requestFocusInWindow();

                // Wait up to 1/2 second in the background before re-centering because setSize() may not happen immediately.
                (new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        for (int waitForSetSize = 0; waitForSetSize < 50; waitForSetSize++) {
                            if (!windowSize.equals(getSize())) {
                                break;
                            }

                            TimeUnit.MILLISECONDS.sleep(10);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        setLocationRelativeTo(null);
                        textArea.requestFocusInWindow();
                        isTogglingFullKeyboard = false;
                    }
                }).execute();
            } else {
                setLocationRelativeTo(null);
                textArea.requestFocusInWindow();
                isTogglingFullKeyboard = false;
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_toggleFullKeyboardMenuItemActionPerformed

    private void setUIScale(int newUIScalePercentage) {
        if ((newUIScalePercentage >= 50) && (newUIScalePercentage <= 200)) {
            if (!launchPath.isEmpty() && new File(launchPath).exists() && ((isMacOS && launchPath.endsWith(".app")) || (!javaPath.isEmpty() && new File(javaPath).exists() && new File(javaPath).canExecute()))) {
                mainMenuBar.removeAll();
                contentPane.removeAll();
                contentPane.setLayout(new FlowLayout(FlowLayout.CENTER, UIScale.scale(60), UIScale.scale(30)));
                contentPane.add(new JLabel("<html><b>Relaunching <i>Keyboard Test</i> to Scale UI to " + newUIScalePercentage + "%</b></html>"));
                contentPane.revalidate();
                contentPane.repaint();
                setMinimumSize(null); // Clear minimum and preferred sizes so pack can go smaller than current size if size was reduced to fit screen.
                setPreferredSize(null);
                pack();
                setLocationRelativeTo(null);

                JFrame keyboardTestWindow = this;

                (new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        TimeUnit.MILLISECONDS.sleep(100); // Add a little delay so that the window has time to update to show the "Relaunching" message set above.

                        return null;
                    }

                    @Override
                    protected void done() {
                        if (isMacOS && launchPath.endsWith(".app")) {
                            // The following code to relaunch Mac app is based on code from QA Helper (Copyright Free Geek - MIT License): https://github.com/freegeek-pdx/Java-QA-Helper/blob/b511d0259a657d1eebd4224a0950094f03d48156/src/GUI/QAHelper.java#L1715-L1719 & https://github.com/freegeek-pdx/Java-QA-Helper/blob/b511d0259a657d1eebd4224a0950094f03d48156/src/GUI/QAHelper.java#L1834-L1835

                            try {
                                Runtime.getRuntime().exec(new String[]{"/usr/bin/osascript",
                                    "-e", "use scripting additions",
                                    "-e", "set appPath to \"" + launchPath.replace("\\", "\\\\").replace("\"", "\\\"") + "\"",
                                    "-e", "delay 0.5",
                                    "-e", "repeat while (application appPath is running)",
                                    "-e", "delay 0.5",
                                    "-e", "end repeat",
                                    "-e", "try",
                                    "-e", "do shell script \"/usr/bin/open -na \" & (quoted form of appPath) & \" --args '" + newUIScalePercentage + "%'\"",
                                    "-e", "end try"});
                            } catch (IOException relaunchKeyboardTestMacAppException) {
                                if (debugLogging) {
                                    System.out.println("relaunchKeyboardTestMacAppException: " + relaunchKeyboardTestMacAppException);
                                }
                            }
                        } else {
                            // The following code to relaunch jar is based on code from QA Helper (Copyright Free Geek - MIT License): https://github.com/freegeek-pdx/Java-QA-Helper/blob/b511d0259a657d1eebd4224a0950094f03d48156/src/GUI/QAHelper.java#L1553-L1599

                            if (isWindows) {
                                File windowsKeyboardTestRelauncherFile = new File(System.getProperty("java.io.tmpdir"), "Keyboard_Test-Relauncher.cmd");

                                try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(windowsKeyboardTestRelauncherFile))) {
                                    bufferedWriter.write(
                                            "@ECHO OFF" + "\n"
                                            + "\n"
                                            + ":WaitForQuit" + "\n"
                                            + "\\Windows\\System32\\timeout.exe /t 1 /nobreak >NUL" + "\n"
                                            + "\\Windows\\System32\\tasklist.exe /nh /fi \"WINDOWTITLE eq Keyboard Test\" | \\Windows\\System32\\find.exe \"No tasks are running\" >NUL" + "\n"
                                            + "IF ERRORLEVEL 1 (" + "\n"
                                            + "\t" + "\\Windows\\System32\\taskkill.exe /fi \"WINDOWTITLE eq Keyboard Test\" >NUL" + "\n"
                                            + "\t" + "GOTO WaitForQuit" + "\n"
                                            + ")" + "\n"
                                            + "\n"
                                            + "START \"Keyboard Test Relauncher\" \"" + javaPath + "\" -jar \"" + launchPath + "\" \"" + newUIScalePercentage + "%%\"" + "\n" // In batch script, the percent sign needs to be doubled to escape it to a literal percent sign character.
                                            + "\n"
                                            + "EXIT 0"
                                            + "\n"
                                    );
                                } catch (IOException writeWindowsKeyboardTestRelauncherFileException) {
                                    if (debugLogging) {
                                        System.out.println("writeWindowsKeyboardTestRelauncherFileException: " + writeWindowsKeyboardTestRelauncherFileException);
                                    }
                                }

                                if (windowsKeyboardTestRelauncherFile.exists()) {
                                    try {
                                        // Need to create a CMD file and launch it with Start-Process so it doesn't get killed when Keyboard Test quits
                                        Runtime.getRuntime().exec(new String[]{"\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "Start-Process -WindowStyle Hidden '" + windowsKeyboardTestRelauncherFile.getPath() + "'"}).waitFor();
                                    } catch (IOException | InterruptedException launchWindowsKeyboardTestRelauncherException) {
                                        if (debugLogging) {
                                            System.out.println("launchWindowsKeyboardTestRelauncherException: " + launchWindowsKeyboardTestRelauncherException);
                                        }
                                    }
                                } else {
                                    Toolkit.getDefaultToolkit().beep();
                                    JOptionPane.showMessageDialog(keyboardTestWindow, "<html><b>Error Relaunching <i>Keyboard Test</i></b><br/><br/><i>Failed to create relaunch command file.</i></html>", "Scale Keyboard Test UI Error", JOptionPane.ERROR_MESSAGE);
                                }
                            } else {
                                try {
                                    Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", "/bin/sleep 0.5; while [[ \"$(/usr/bin/pgrep -fl 'Keyboard_Test.*\\.jar')\" == *java* ]]; do " + (isLinux ? "/usr/bin/wmctrl -Fc 'Keyboard Test'; " : "") + "/bin/sleep 0.5; done; '" + javaPath.replace("'", "'\\''") + "' -jar '" + launchPath.replace("'", "'\\''") + "' '" + newUIScalePercentage + "%' & disown"});
                                } catch (IOException relaunchKeyboardTestJarException) {
                                    if (debugLogging) {
                                        System.out.println("relaunchKeyboardTestJarException: " + relaunchKeyboardTestJarException);
                                    }
                                }
                            }
                        }

                        System.exit(0);
                    }
                }).execute();
            } else {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "<html><b>Failed to Determine <i>Keyboard Test</i> Launch Path</b><br/><br/>Cannot scale <i>Keyboard Test</i> UI without being able to relaunch app.</html>", "Scale Keyboard Test UI Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void resetUIScaleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetUIScaleMenuItemActionPerformed
        int uiScalePercentage = 100;
        if (System.getProperty("KeyboardTest.uiScalePercentage") != null) {
            uiScalePercentage = Integer.parseInt(System.getProperty("KeyboardTest.uiScalePercentage"));
        }

        if (uiScalePercentage != 100) {
            setUIScale(100);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_resetUIScaleMenuItemActionPerformed

    private void increaseUIScaleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_increaseUIScaleMenuItemActionPerformed
        int uiScalePercentage = 100;
        if (System.getProperty("KeyboardTest.uiScalePercentage") != null) {
            uiScalePercentage = Integer.parseInt(System.getProperty("KeyboardTest.uiScalePercentage"));
        }

        if (uiScalePercentage < 200) {
            int newUIScalePercentage = (uiScalePercentage + 25);
            if (newUIScalePercentage > 200) {
                newUIScalePercentage = 200;
            }

            setUIScale(newUIScalePercentage);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_increaseUIScaleMenuItemActionPerformed

    private void decreaseUIScaleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decreaseUIScaleMenuItemActionPerformed
        int uiScalePercentage = 100;
        if (System.getProperty("KeyboardTest.uiScalePercentage") != null) {
            uiScalePercentage = Integer.parseInt(System.getProperty("KeyboardTest.uiScalePercentage"));
        }

        if (uiScalePercentage > 50) {
            int newUIScalePercentage = (uiScalePercentage - 25);
            if (newUIScalePercentage < 50) {
                newUIScalePercentage = 50;
            }

            setUIScale(newUIScalePercentage);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_decreaseUIScaleMenuItemActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel arrowKeysAlignmentSpacer;
    private javax.swing.JPanel arrowKeysPanel;
    private javax.swing.JPanel contentPane;
    private javax.swing.JScrollPane contentScrollPane;
    private javax.swing.JMenuItem decreaseUIScaleMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem increaseUIScaleMenuItem;
    private javax.swing.JLabel keyLabel0;
    private javax.swing.JLabel keyLabel1;
    private javax.swing.JLabel keyLabel2;
    private javax.swing.JLabel keyLabel3;
    private javax.swing.JLabel keyLabel4;
    private javax.swing.JLabel keyLabel5;
    private javax.swing.JLabel keyLabel6;
    private javax.swing.JLabel keyLabel7;
    private javax.swing.JLabel keyLabel8;
    private javax.swing.JLabel keyLabel9;
    private javax.swing.JLabel keyLabelA;
    private javax.swing.JLabel keyLabelArrowDown;
    private javax.swing.JLabel keyLabelArrowLeft;
    private javax.swing.JLabel keyLabelArrowRight;
    private javax.swing.JLabel keyLabelArrowUp;
    private javax.swing.JLabel keyLabelB;
    private javax.swing.JLabel keyLabelBackQuote;
    private javax.swing.JLabel keyLabelBackSlash;
    private javax.swing.JLabel keyLabelBackspace;
    private javax.swing.JLabel keyLabelC;
    private javax.swing.JLabel keyLabelCapsLock;
    private javax.swing.JLabel keyLabelCloseBracket;
    private javax.swing.JLabel keyLabelComma;
    private javax.swing.JLabel keyLabelD;
    private javax.swing.JLabel keyLabelDelete;
    private javax.swing.JLabel keyLabelE;
    private javax.swing.JLabel keyLabelEnd;
    private javax.swing.JLabel keyLabelEnter;
    private javax.swing.JLabel keyLabelEquals;
    private javax.swing.JLabel keyLabelEscape;
    private javax.swing.JLabel keyLabelF;
    private javax.swing.JLabel keyLabelF1;
    private javax.swing.JLabel keyLabelF10;
    private javax.swing.JLabel keyLabelF11;
    private javax.swing.JLabel keyLabelF12;
    private javax.swing.JLabel keyLabelF2;
    private javax.swing.JLabel keyLabelF3;
    private javax.swing.JLabel keyLabelF4;
    private javax.swing.JLabel keyLabelF5;
    private javax.swing.JLabel keyLabelF6;
    private javax.swing.JLabel keyLabelF7;
    private javax.swing.JLabel keyLabelF8;
    private javax.swing.JLabel keyLabelF9;
    private javax.swing.JLabel keyLabelG;
    private javax.swing.JLabel keyLabelH;
    private javax.swing.JLabel keyLabelHome;
    private javax.swing.JLabel keyLabelI;
    private javax.swing.JLabel keyLabelInsert;
    private javax.swing.JLabel keyLabelJ;
    private javax.swing.JLabel keyLabelK;
    private javax.swing.JLabel keyLabelL;
    private javax.swing.JLabel keyLabelLeftAlt;
    private javax.swing.JLabel keyLabelLeftCommand;
    private javax.swing.JLabel keyLabelLeftControl;
    private javax.swing.JLabel keyLabelLeftShift;
    private javax.swing.JLabel keyLabelLeftStart;
    private javax.swing.JLabel keyLabelM;
    private javax.swing.JLabel keyLabelMenu;
    private javax.swing.JLabel keyLabelMinus;
    private javax.swing.JLabel keyLabelN;
    private javax.swing.JLabel keyLabelNumPad0;
    private javax.swing.JLabel keyLabelNumPad1;
    private javax.swing.JLabel keyLabelNumPad2;
    private javax.swing.JLabel keyLabelNumPad3;
    private javax.swing.JLabel keyLabelNumPad4;
    private javax.swing.JLabel keyLabelNumPad5;
    private javax.swing.JLabel keyLabelNumPad6;
    private javax.swing.JLabel keyLabelNumPad7;
    private javax.swing.JLabel keyLabelNumPad8;
    private javax.swing.JLabel keyLabelNumPad9;
    private javax.swing.JLabel keyLabelNumPadAdd;
    private javax.swing.JLabel keyLabelNumPadDecimal;
    private javax.swing.JLabel keyLabelNumPadDivide;
    private javax.swing.JLabel keyLabelNumPadEnter;
    private javax.swing.JLabel keyLabelNumPadEquals;
    private javax.swing.JLabel keyLabelNumPadMacSubtract;
    private javax.swing.JLabel keyLabelNumPadMultiply;
    private javax.swing.JLabel keyLabelNumPadNumLock;
    private javax.swing.JLabel keyLabelNumPadSubtract;
    private javax.swing.JLabel keyLabelO;
    private javax.swing.JLabel keyLabelOpenBracket;
    private javax.swing.JLabel keyLabelP;
    private javax.swing.JLabel keyLabelPageDown;
    private javax.swing.JLabel keyLabelPageUp;
    private javax.swing.JLabel keyLabelPause;
    private javax.swing.JLabel keyLabelPeriod;
    private javax.swing.JLabel keyLabelPrintScreen;
    private javax.swing.JLabel keyLabelQ;
    private javax.swing.JLabel keyLabelQuote;
    private javax.swing.JLabel keyLabelR;
    private javax.swing.JLabel keyLabelRightAlt;
    private javax.swing.JLabel keyLabelRightCommand;
    private javax.swing.JLabel keyLabelRightControl;
    private javax.swing.JLabel keyLabelRightShift;
    private javax.swing.JLabel keyLabelRightStart;
    private javax.swing.JLabel keyLabelS;
    private javax.swing.JLabel keyLabelScrollLock;
    private javax.swing.JLabel keyLabelSemicolon;
    private javax.swing.JLabel keyLabelSlash;
    private javax.swing.JLabel keyLabelSpace;
    private javax.swing.JLabel keyLabelT;
    private javax.swing.JLabel keyLabelTab;
    private javax.swing.JLabel keyLabelU;
    private javax.swing.JLabel keyLabelV;
    private javax.swing.JLabel keyLabelW;
    private javax.swing.JLabel keyLabelX;
    private javax.swing.JLabel keyLabelY;
    private javax.swing.JLabel keyLabelZ;
    private javax.swing.JLabel lastKeyPressedLabel;
    private javax.swing.JPanel mainKeysPanel;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JPanel numPadPanel;
    private javax.swing.JPanel otherKeysPanel;
    private javax.swing.JMenuItem resetPressedKeysMenuItem;
    private javax.swing.JMenuItem resetUIScaleMenuItem;
    private javax.swing.JTextArea textArea;
    private javax.swing.JScrollPane textAreaScrollPane;
    private javax.swing.JMenuItem toggleFullKeyboardMenuItem;
    private javax.swing.JPanel topOtherKeysAndLastKeyPressedPanel;
    private javax.swing.JPanel topOtherKeysPanel;
    private javax.swing.JMenu uiScaleMenu;
    // End of variables declaration//GEN-END:variables
}
