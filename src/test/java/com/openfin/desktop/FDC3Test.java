package com.openfin.desktop;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.fdc3.DesktopAgent;
import com.openfin.desktop.fdc3.context.ContactContext;
import com.openfin.desktop.fdc3.context.Context;
import com.openfin.desktop.fdc3.context.InstrumentContext;

public class FDC3Test {
	private static Logger logger = LoggerFactory.getLogger(FDC3Test.class.getName());

	private static final String DESKTOP_UUID = FDC3Test.class.getName();
	private static DesktopConnection desktopConnection;

	@BeforeClass
	public static void setup() throws Exception {
		logger.debug("starting");
		desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
	}

	@AfterClass
	public static void teardown() throws Exception {
		TestUtils.teardownDesktopConnection(desktopConnection);
	}

	@Test
	public void findIntent() throws Exception {

		AtomicBoolean gotIntent = new AtomicBoolean(false);
		DesktopAgent fdc3 = new DesktopAgent(desktopConnection);

		fdc3.findIntent("ViewChart", null).thenAccept(appIntent -> {

			java.lang.System.out.println(appIntent);

			gotIntent.set(true);

		}).toCompletableFuture().get(10, TimeUnit.SECONDS);

		assertTrue(gotIntent.get());
	}

	@Test
	public void findIntentsByContext() throws Exception {

		AtomicBoolean getIntents = new AtomicBoolean(false);
		DesktopAgent fdc3 = new DesktopAgent(desktopConnection);

		fdc3.findIntentsByContext(new ContactContext()).thenAccept(appIntents -> {

			java.lang.System.out.println(appIntents);

			getIntents.set(true);

		}).toCompletableFuture().get(10, TimeUnit.SECONDS);

		assertTrue(getIntents.get());
	}

	@Test
	public void getSystemChannels() throws Exception {

		AtomicBoolean gotSystemChannels = new AtomicBoolean(false);
		DesktopAgent fdc3 = new DesktopAgent(desktopConnection);

		fdc3.getSystemChannels().thenAccept(channels -> {
			java.lang.System.out.println(channels);

			gotSystemChannels.set(true);

		}).toCompletableFuture().get(10, TimeUnit.SECONDS);

		assertTrue(gotSystemChannels.get());
	}
	
	@Test
	public void open() throws Exception {
		DesktopAgent fdc3 = new DesktopAgent(desktopConnection);

		fdc3.findIntent("ViewChart", null).thenCompose(appIntent -> {

			String firstAppName = appIntent.getApps()[0].getName();
			
			java.lang.System.out.println("firstAppName: " + firstAppName);
			
			return fdc3.open(firstAppName, null);
			
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
	}
}
