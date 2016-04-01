package com.openfin.desktop;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Run all tests
 *
 * Created by wche on 1/25/16.
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({ ApplicationTest.class, OpenFinRuntimeTest.class, WindowTest.class, SystemTest.class, InterApplicationBusTest.class, RuntimeConfigTest.class})
public class AllTests {
}
