package com.openfin.desktop;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesktopConnectionTest {
	private static Logger logger = LoggerFactory.getLogger(DesktopConnectionTest.class.getName());

	private static final String DESKTOP_UUID = DesktopConnectionTest.class.getName();

	@BeforeClass
	public static void setup() throws Exception {
	}

	@AfterClass
	public static void teardown() throws Exception {
	}

	@Test
	public void reconnect() throws Exception {
		// IAB subscription shouldn't survive reconnection

		final AtomicInteger invokeCnt = new AtomicInteger(0);
		String topic1 = "myTopic1";
		String topic2 = "myTopic2";
		String message1 = "myMessage1";
		String message2 = "myMessage2";
		final CyclicBarrier connectLatch = new CyclicBarrier(2);
		final CountDownLatch disconnectLatch = new CountDownLatch(1);
		DesktopConnection conn = new DesktopConnection(DESKTOP_UUID);
		RuntimeConfiguration conf = new RuntimeConfiguration();
		conf.setRuntimeVersion(TestUtils.getRuntimeVersion());

		DesktopStateListener listener = new DesktopStateListener() {

			@Override
			public void onReady() {
				try {
					connectLatch.await();
					logger.debug("onReady");
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onClose(String error) {
				disconnectLatch.countDown();
				logger.debug("onClose: " + error);
			}

			@Override
			public void onError(String reason) {
			}

			@Override
			public void onMessage(String message) {
			}

			@Override
			public void onOutgoingMessage(String message) {
			}

		};

		conn.connect(conf, listener, 10);
		connectLatch.await(10, TimeUnit.SECONDS);

		assertEquals(0, connectLatch.getNumberWaiting());

		// create an app and let it sit through the reconnection.
		ApplicationOptions options = TestUtils.getAppOptions(null);
		Application application = TestUtils.runApplication(options, conn);

		CountDownLatch listener1Latch = new CountDownLatch(1);
		// subscribe to a topic
		conn.getInterApplicationBus().subscribe("*", topic1, new BusListener() {

			@Override
			public void onMessageReceived(String sourceUuid, String topic, Object payload) {
				logger.info("listener1 received: " + payload.toString());
				invokeCnt.incrementAndGet();
				if (message1.equals(payload.toString())) {
					listener1Latch.countDown();
				}
			}
		}, new AckListener() {
			@Override
			public void onSuccess(Ack ack) {
				if (ack.isSuccessful()) {
					try {
						conn.getInterApplicationBus().publish(topic1, message1);
					}
					catch (DesktopException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onError(Ack ack) {
				logger.error(ack.getReason());
			}
		});

		listener1Latch.await(5, TimeUnit.SECONDS);
		assertEquals(0, listener1Latch.getCount());

		try {
			conn.disconnect("junit test");
		}
		catch (DesktopException e) {
			e.printStackTrace();
		}

		disconnectLatch.await(5, TimeUnit.SECONDS);

		assertEquals(0, disconnectLatch.getCount());

		// connect it again.
		connectLatch.reset();
		conn.connect(conf, listener, 10);
		connectLatch.await(10, TimeUnit.SECONDS);
		assertEquals(0, connectLatch.getNumberWaiting());

		CountDownLatch listener2Latch = new CountDownLatch(1);
		
		conn.getInterApplicationBus().subscribe("*", topic2, new BusListener() {

			@Override
			public void onMessageReceived(String sourceUuid, String topic, Object payload) {
				listener2Latch.countDown();
				logger.info("listener2 received: " + payload.toString());
			}
		}, new AckListener() {
			@Override
			public void onSuccess(Ack ack) {
				if (ack.isSuccessful()) {
					try {
						conn.getInterApplicationBus().publish(topic1, message2);
						conn.getInterApplicationBus().publish(topic2, message2);
					}
					catch (DesktopException e) {
						e.printStackTrace();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onError(Ack ack) {
				logger.error(ack.getReason());
			}
		});
		
		//can kill the app now.
		Application app = Application.wrap(application.uuid, conn);
		app.close();

		listener2Latch.await(5, TimeUnit.SECONDS);

		assertEquals(0, listener2Latch.getCount());
		assertEquals(1, invokeCnt.get());
	}

}
