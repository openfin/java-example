package com.openfin.desktop;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * JUnit tests for com.openfin.desktop.Application class
 *
 * Created by wche on 1/22/16.
 *
 */
public class ApplicationTest {
    private static Logger logger = LoggerFactory.getLogger(ApplicationTest.class.getName());

    private static final String DESKTOP_UUID = ApplicationTest.class.getName();
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
    public void runAndClose() throws Exception {
        logger.debug("runAndClose");
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);

        // duplicate UUID is not allowed
        ApplicationOptions options2 = TestUtils.getAppOptions(options.getUUID(), null);
        CountDownLatch dupLatch = new CountDownLatch(1);
        Application application2 = new Application(options2, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
                if (ack.getReason().contains("Application with specified UUID already exists")) {
                    dupLatch.countDown();
                }
            }
        });
        dupLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Duplicate app UUID validation timeout " + options.getUUID(), dupLatch.getCount(), 0);

        TestUtils.closeApplication(application);
    }

    @Test
    public void runAndTerminate() throws Exception {
        logger.debug("runAndTerminate");
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.createApplication(options, desktopConnection);

        CountDownLatch stoppedLatch = new CountDownLatch(1);
        EventListener listener = new EventListener() {
            @Override
            public void eventReceived(ActionEvent actionEvent) {
                if (actionEvent.getType().equals("closed")) {
                    stoppedLatch.countDown();
                }
            }
        };
        TestUtils.addEventListener(application, "closed", listener);
        TestUtils.runApplication(application, true);
        application.terminate();
        stoppedLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Terminate application timeout  " + options.getUUID(), stoppedLatch.getCount(), 0);
    }

    @Test
    public void getApplicationManifest() throws Exception {
        logger.debug("getApplicationManifest");
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        application.getManifest(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
                logger.debug(ack.getJsonObject().toString());
                logger.debug(String.format("Reason reason: %s", ack.getReason()));
                if (ack.getReason().contains("App not started from manifest")) {
                    latch.countDown();
                }
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getApplicationManifest timeout " + options.getUUID(), latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }

    @Test
    public void restartApplication() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);

        CountDownLatch latch = new CountDownLatch(1);
        TestUtils.addEventListener(application.getWindow(), "shown", actionEvent -> {
            if (actionEvent.getType().equals("shown")) {
                latch.countDown();
            }
        });
        application.restart();
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("restartApplication timeout " + options.getUUID(), latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }

    @Test
    public void runReqestedEventListeners() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.createApplication(options, desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        TestUtils.addEventListener(application, "run-requested", actionEvent -> {
            if (actionEvent.getType().equals("run-requested")) {
                logger.debug(String.format("%s", actionEvent.getEventObject().toString()));
                latch.countDown();
            }
        });
        TestUtils.runApplication(application, true);
        // run-requested is generated when Application.run is called on an active application
        application.run();
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("run-requested timeout " + options.getUUID(), latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }

    @Test
    public void createChildWindow() throws Exception {
        Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
        WindowOptions childOptions = TestUtils.getWindowOptions("child1", TestUtils.openfin_app_url); // use same URL as main app
        Window childWindow = TestUtils.createChildWindow(application, childOptions, desktopConnection);
        TestUtils.closeApplication(application);
    }

    @Test
    public void crossAppDockAndUndock() throws Exception {
        Application application1 = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
        Application application2 = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);

        WindowBounds beforeMoveBounds = TestUtils.getBounds(application2.getWindow());
        CountDownLatch joinLatch = new CountDownLatch(1);
        application2.getWindow().joinGroup(application1.getWindow(), new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    joinLatch.countDown();
                }
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        joinLatch.await(3, TimeUnit.SECONDS);
        assertEquals(joinLatch.getCount(), 0);

        // get group info for a window
        CountDownLatch groupInfoLatch = new CountDownLatch(2);
        application1.getWindow().getGroup(result -> {
            for (Window window : result) {
                if (window.getUuid().equals(application1.getWindow().getUuid()) && window.getName().equals(application1.getWindow().getName())) {
                    groupInfoLatch.countDown();
                } else if (window.getUuid().equals(application2.getWindow().getUuid()) && window.getName().equals(application2.getWindow().getName())) {
                    groupInfoLatch.countDown();
                }
            }
        }, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        groupInfoLatch.await(3, TimeUnit.SECONDS);
        assertEquals(groupInfoLatch.getCount(), 0);

        // get group info for an application
        CountDownLatch appGroupInfoLatch = new CountDownLatch(2);
        application1.getGroups(result -> {
                    for (List<Window> list: result) {
                        for (Window window : list) {
                            if (window.getUuid().equals(application1.getWindow().getUuid()) && window.getName().equals(application1.getWindow().getName())) {
                                appGroupInfoLatch.countDown();
                            } else if (window.getUuid().equals(application2.getWindow().getUuid()) && window.getName().equals(application2.getWindow().getName())) {
                                appGroupInfoLatch.countDown();
                            }
                        }
                    }
                }, new AckListener() {
                    @Override
                    public void onSuccess(Ack ack) {
                    }
                    @Override
                    public void onError(Ack ack) {
                        logger.error(String.format("onError %s", ack.getReason()));
                    }
                }
        );
        appGroupInfoLatch.await(3, TimeUnit.SECONDS);
        assertEquals(appGroupInfoLatch.getCount(), 0);

        int leftBy = 20, topBy = 30;
        TestUtils.moveWindowBy(application1.getWindow(), leftBy, topBy);
        // child window sohuld move with main window since they are docked
        WindowBounds afterMoveBounds = TestUtils.getBounds(application2.getWindow());
        int topAfterDockMove = afterMoveBounds.getTop(), leftAfterDockMove = afterMoveBounds.getLeft();
        assertEquals(afterMoveBounds.getTop() - beforeMoveBounds.getTop(), topBy);
        assertEquals(afterMoveBounds.getLeft() - beforeMoveBounds.getLeft(), leftBy);

        // undock by leaving the group
        CountDownLatch undockLatch = new CountDownLatch(1);
        application2.getWindow().leaveGroup(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    undockLatch.countDown();
                }
            }

            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        undockLatch.await(5, TimeUnit.SECONDS);
        assertEquals(undockLatch.getCount(), 0);
        TestUtils.moveWindowBy(application1.getWindow(), leftBy, topBy);
        // child window should not move afer leaving group
        afterMoveBounds = TestUtils.getBounds(application2.getWindow());
        assertEquals(afterMoveBounds.getLeft().intValue(), leftAfterDockMove);
        assertEquals(afterMoveBounds.getTop().intValue(), topAfterDockMove);

        TestUtils.closeApplication(application1);
        TestUtils.closeApplication(application2);
    }
    
	@Test
	public void addEventListener() throws Exception {
		ApplicationOptions options = TestUtils.getAppOptions(null);
		Application application = TestUtils.createApplication(options, desktopConnection);
		CountDownLatch latch = new CountDownLatch(1);
		application.addEventListener("started", event -> {
			latch.countDown();
		}, new AckListener() {
			@Override
			public void onSuccess(Ack ack) {
			}

			@Override
			public void onError(Ack ack) {
			}
		});

		application.run(new AckListener() {
			@Override
			public void onSuccess(Ack ack) {
			}

			@Override
			public void onError(Ack ack) {
			}
		});

		latch.await(5, TimeUnit.SECONDS);
		assertEquals("eventListener test timeout", 0, latch.getCount());

		TestUtils.closeApplication(application);
	}
	
	@Test
	public void getWindow() throws Exception {
		ApplicationOptions options = TestUtils.getAppOptions(null);
		String mainWindowName = options.getName();
		Application application = TestUtils.createApplication(options, desktopConnection);
		TestUtils.runApplication(application, true);
		Window window = application.getWindow();
		assertEquals(window.getName(), mainWindowName);
		TestUtils.closeApplication(application);
	}
	
	@Test
	public void removeEventListener() throws Exception {
		ApplicationOptions options = TestUtils.getAppOptions(null);
		Application application = TestUtils.createApplication(options, desktopConnection);
	
		int cnt = 10;
		CountDownLatch latch = new CountDownLatch(cnt);
		AtomicInteger invokeCnt = new AtomicInteger(0);
		EventListener[] listeners = new EventListener[cnt];
		for (int i=0; i<cnt; i++) {
			listeners[i] = new EventListener(){
				@Override
				public void eventReceived(ActionEvent actionEvent) {
					invokeCnt.incrementAndGet();
					latch.countDown();
				}
			};
		}
		
		for (int i=0; i<cnt; i++) {
			TestUtils.addEventListener(application, "window-closed", listeners[i]);
		}
		TestUtils.runApplication(application, true);
		
		for (int i=0; i<cnt/2; i++) {
			application.removeEventListener("window-closed", listeners[i*2], null);
		}

		Window window = application.getWindow();
		window.close();
		
		latch.await(5, TimeUnit.SECONDS);
		
		assertEquals(cnt/2, latch.getCount());
		assertEquals(cnt/2, invokeCnt.get());
	}
	
	
	@Test
	public void createFromManifest() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Application.createFromManifest(
				"https://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/app.json",
				new AsyncCallback<Application>() {
					@Override
					public void onSuccess(Application app) {
						latch.countDown();
					}
				}, new AckListener() {
					@Override
					public void onSuccess(Ack ack) {
					}

					@Override
					public void onError(Ack ack) {
						logger.info("error creating app: {}", ack.getReason());
					}
				}, desktopConnection);

		latch.await(50, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
		

	}
}
