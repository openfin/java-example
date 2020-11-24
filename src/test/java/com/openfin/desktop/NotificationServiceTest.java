package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.notifications.ButtonOptions;
import com.openfin.desktop.notifications.NotificationActionResult;
import com.openfin.desktop.notifications.NotificationEventListener;
import com.openfin.desktop.notifications.NotificationIndicator;
import com.openfin.desktop.notifications.NotificationOptions;
import com.openfin.desktop.notifications.Notifications;
import com.openfin.desktop.notifications.ProviderStatus;
import com.openfin.desktop.notifications.events.NotificationActionEvent;

public class NotificationServiceTest {
	private static Logger logger = LoggerFactory.getLogger(NotificationServiceTest.class.getName());

	private static final String DESKTOP_UUID = NotificationServiceTest.class.getName();
	private static DesktopConnection desktopConnection;

	@BeforeClass
	public static void setup() throws Exception {
		desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
	}

	@AfterClass
	public static void teardown() throws Exception {
		desktopConnection.disconnect();
	}

	@Test
	public void addThenRemoveEventListener() throws Exception {
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(2);
		Notifications n = new Notifications(desktopConnection);
		
		NotificationEventListener listener = e -> {
			if (Notifications.EVENT_TYPE_CREATED.equals(e.getType())) {
				latch1.countDown();
				latch2.countDown();
			}
		};
		n.addEventListener(Notifications.EVENT_TYPE_CREATED, listener);
		
		n.create(new NotificationOptions("Title", "Body", "Category"));
		
		latch1.await(5, TimeUnit.SECONDS);

		assertEquals(0, latch1.getCount());
		
		n.removeEventListener(Notifications.EVENT_TYPE_CREATED, listener);

		n.create(new NotificationOptions("Title", "Body", "Category"));
		
		latch2.await(5, TimeUnit.SECONDS);

		assertEquals(1, latch2.getCount());
	}

	@Ignore
	@Test
	public void buttonAction() throws Exception {
		CountDownLatch latch1 = new CountDownLatch(1);
		Notifications n = new Notifications(desktopConnection);
		
		NotificationEventListener listener = e -> {
			if (Notifications.EVENT_TYPE_ACTION.equals(e.getType())) {
				java.lang.System.out.println("event source: " + ((NotificationActionEvent)e).getSource());
				java.lang.System.out.println("event result: " + ((NotificationActionEvent)e).getResult());
				latch1.countDown();
			}
		};
		n.addEventListener(Notifications.EVENT_TYPE_ACTION, listener);
		
		NotificationOptions opts = new NotificationOptions("Title", "Body", "Category");

		ButtonOptions btn1 = new ButtonOptions("btn 1");
		NotificationActionResult btn1Action = new NotificationActionResult();
		btn1Action.setString("type", "GotIt");
		btn1.setOnClick(btn1Action);
		
		opts.setButtons(btn1);

		NotificationActionResult onClose = new NotificationActionResult();
		onClose.setString("action", "notification_onClose");
		opts.setOnClose(onClose);
		
		n.create(opts);
				
		latch1.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch1.getCount());
	}
	
	
	@Test
	public void clear() throws Exception {
		Notifications n = new Notifications(desktopConnection);
		Boolean b = n.create(new NotificationOptions("AAA", "Body", "Category")).thenCompose(nn -> {
			return n.clear(nn.getId());
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);

		assertTrue(b);
	}

	@Test
	public void clearAll() throws Exception {
		Notifications n = new Notifications(desktopConnection);
		final int cnt = 5;
		CompletionStage<NotificationOptions> opt = null;
		for (int i = 0; i < cnt; i++) {
			String title = "Title-" + i;
			if (opt != null) {
				opt = opt.thenCompose(v -> {
					return n.create(new NotificationOptions(title, "Body", "Category"));
				});
			}
			else {
				opt = n.create(new NotificationOptions(title, "Body", "Category"));
			}

		}

		Integer cleared = opt.thenCompose(v -> {
			return n.clearAll();

		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
		
		assertTrue(cleared >= cnt);
	}

	@Test
	public void create() throws Exception {
		Notifications n = new Notifications(desktopConnection);
		NotificationOptions opts = new NotificationOptions("Title", "Body", "Category");

		ButtonOptions btn1 = new ButtonOptions("btn 1");
		NotificationActionResult btn1Action = new NotificationActionResult();
		btn1Action.setString("type", "GotIt");
		btn1.setOnClick(btn1Action);
		
		opts.setButtons(btn1);
		
		NotificationIndicator indicator = new NotificationIndicator(NotificationIndicator.TYPE_SUCCESS);
		
		opts.setIndicator(indicator);
		
		opts.setSticky(NotificationOptions.STICKY_STICKY);
		
		opts.setExpires(new Date(java.lang.System.currentTimeMillis() + (10*1000)));
		
		NotificationActionResult onClose = new NotificationActionResult();
		onClose.setString("action", "notification_onClose");
		opts.setOnClose(onClose);
		
		NotificationOptions opt = n.create(opts).toCompletableFuture().get(10,
				TimeUnit.SECONDS);
		assertNotNull(opt.getId());
		java.lang.System.out.println("body: " + opt.getBody());
		java.lang.System.out.println("buttons: " + opt.getButtons());
		java.lang.System.out.println("category: " + opt.getCategory());
		java.lang.System.out.println("date: " + opt.getDate());
		java.lang.System.out.println("expires: " + opt.getExpires());
		java.lang.System.out.println("icon: " + opt.getIcon());
		java.lang.System.out.println("id: " + opt.getId());
		java.lang.System.out.println("indicator: " + opt.getIndicator());
		java.lang.System.out.println("sticky: " + opt.getSticky());
		java.lang.System.out.println("title: " + opt.getTitle());
	}

	@Test
	public void getAll() throws Exception {
		Notifications n = new Notifications(desktopConnection);

		CompletionStage<Integer> clearAllFuture = n.clearAll();

		final int cnt = 5;
		CompletionStage<NotificationOptions> opt = null;
		for (int i = 0; i < cnt; i++) {
			String title = "Title-" + i;
			if (opt != null) {
				opt = opt.thenCompose(v -> {
					return n.create(new NotificationOptions(title, "Body", "Category"));
				});
			}
			else {
				opt = clearAllFuture.thenCompose(ii -> {
					return n.create(new NotificationOptions(title, "Body", "Category"));
				});
			}
		}

		List<NotificationOptions> opts = opt.thenCompose(v -> {
			return n.getAll();
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);

		assertEquals(cnt, opts.size());
	}

	@Test
	public void toggleNotificationCenter() throws Exception {
		Notifications n = new Notifications(desktopConnection);
		n.create(new NotificationOptions("Title", "Body", "Category")).thenCompose(nOpts -> {
			return n.toggleNotificationCenter();
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
	}

	@Test
	public void getProviderStatus() throws Exception {
		Notifications n = new Notifications(desktopConnection);
		ProviderStatus providerStatus = n.getProviderStatus().toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertTrue(providerStatus.isConnected());
	}

}
