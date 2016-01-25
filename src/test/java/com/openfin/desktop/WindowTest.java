package com.openfin.desktop;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wche on 1/25/16.
 *
 */
public class WindowTest {
    private static Logger logger = LoggerFactory.getLogger(WindowTest.class.getName());

    private static final String DESKTOP_UUID = WindowTest.class.getName();
    private static DesktopConnection desktopConnection;
    private static final String child_window_url = "http://test.openf.in/test.html";  // simple test app

    @Test
    public void hideAndShow() throws Exception {

    }
}
