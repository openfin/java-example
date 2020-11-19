package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.channel.NotificationOpts;
import com.openfin.desktop.channel.Notifications;
import com.openfin.desktop.notifications.NotificationEventListener;
import com.openfin.desktop.notifications.events.NotificationCreatedEvent;

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
			latch1.countDown();
			latch2.countDown();
		};
		n.addEventListener(Notifications.EVENT_TYPE_CREATED, listener);
		
		n.create(new NotificationOpts("Title", "Body", "Category"));
		
		latch1.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch1.getCount());
		
		n.removeEventListener(Notifications.EVENT_TYPE_CREATED, listener);

		n.create(new NotificationOpts("Title", "Body", "Category"));
		
		latch2.await(10, TimeUnit.SECONDS);

		assertEquals(1, latch2.getCount());
	}

	@Test
	public void clear() throws Exception {
		Notifications n = new Notifications(desktopConnection);
		Boolean b = n.create(new NotificationOpts("AAA", "Body", "Category")).thenCompose(nn -> {
			return n.clear(nn.getId());
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);

		assertTrue(b);
	}

	@Test
	public void clearAll() throws Exception {
		Notifications n = new Notifications(desktopConnection);
		final int cnt = 5;
		CompletionStage<NotificationOpts> opt = null;
		for (int i = 0; i < cnt; i++) {
			String title = "Title-" + i;
			if (opt != null) {
				opt = opt.thenCompose(v -> {
					return n.create(new NotificationOpts(title, "Body", "Category"));
				});
			}
			else {
				opt = n.create(new NotificationOpts(title, "Body", "Category"));
			}

		}

		Integer cleared = opt.thenCompose(v -> {
			return n.clearAll();

		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertEquals(cnt, cleared.intValue());
	}

	@Test
	public void create() throws Exception {
		Notifications n = new Notifications(desktopConnection);
		NotificationOpts opt = n.create(new NotificationOpts("Title", "Body", "Category")).toCompletableFuture().get(10,
				TimeUnit.SECONDS);
		assertNotNull(opt.getId());
	}

	@Test
	public void getAll() throws Exception {
		Notifications n = new Notifications(desktopConnection);

		CompletionStage<Integer> clearAllFuture = n.clearAll();

		final int cnt = 5;
		CompletionStage<NotificationOpts> opt = null;
		for (int i = 0; i < cnt; i++) {
			String title = "Title-" + i;
			if (opt != null) {
				opt = opt.thenCompose(v -> {
					return n.create(new NotificationOpts(title, "Body", "Category"));
				});
			}
			else {
				opt = clearAllFuture.thenCompose(ii -> {
					return n.create(new NotificationOpts(title, "Body", "Category"));
				});
			}
		}

		List<NotificationOpts> opts = opt.thenCompose(v -> {
			return n.getAll();
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);

		assertEquals(cnt, opts.size());
	}

	@Test
	public void toggleNotificationCenter() throws Exception {
		Notifications n = new Notifications(desktopConnection);
		n.create(new NotificationOpts("Title", "Body", "Category")).thenCompose(nOpts -> {
			return n.toggleNotificationCenter();
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
	}

}
