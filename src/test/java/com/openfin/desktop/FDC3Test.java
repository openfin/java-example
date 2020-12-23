package com.openfin.desktop;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;
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
	
	@Test
	public void broadcast() throws Exception {
		DesktopAgent fdc3 = new DesktopAgent(desktopConnection);

		Context context = new Context("testContext");
		
		fdc3.broadcast(context).toCompletableFuture().get(10, TimeUnit.SECONDS);
	}
	
	@Test
	public void getCurrentChannel() throws Exception {
		DesktopAgent fdc3 = new DesktopAgent(desktopConnection);
		fdc3.getCurrentChannel(null).thenAccept(c->{
			java.lang.System.out.println("currentChannel: " + c);
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
	}
	
	@Test
	public void getChannelById() throws Exception {
		AtomicBoolean gotDefaultChannel = new AtomicBoolean(false);
		DesktopAgent fdc3 = new DesktopAgent(desktopConnection);
		fdc3.getChannelById("default").thenAccept(c->{
			gotDefaultChannel.set("default".equals(c.getId()));
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
		
		assertTrue(gotDefaultChannel.get());
	}
	
	@Test
	public void getOrCreateAppChannel() throws Exception {
		String appChannelName = "whatever";
		AtomicBoolean gotChannel = new AtomicBoolean(false);
		DesktopAgent fdc3 = new DesktopAgent(desktopConnection);
		fdc3.getOrCreateAppChannel(appChannelName).thenAccept(c->{
			java.lang.System.out.println("app channel: " + c);
			gotChannel.set(appChannelName.equals(c.getName()));
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
		
		assertTrue(gotChannel.get());
	}
	
	@Test
	public void raiseIntent() throws Exception {
		AtomicBoolean gotIntentResolution = new AtomicBoolean(false);
		DesktopAgent fdc3 = new DesktopAgent(desktopConnection);
		InstrumentContext context = new InstrumentContext();
		JSONObject id = new JSONObject();
		id.put("ticker", "MSFT");
		context.setId(id);
		context.setName("Microsoft");
		
		fdc3.raiseIntent("ViewChart", context, "fdc3-charts-red").thenAccept(ir->{
			java.lang.System.out.println("intent resolution: " + ir);
			if (ir != null) {
				gotIntentResolution.set(true);
			}
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
		
		assertTrue(gotIntentResolution.get());
	}
	
	
	
}
