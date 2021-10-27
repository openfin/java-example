package com.openfin.desktop;

import java.io.InvalidClassException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

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

		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test
	public void launchManifest_Application_Success() throws InvalidClassException, InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);

		 desktopConnection.launchManifest(
				"https://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/app.json",
				Application.class,
				null).thenApply(app ->{
					assertTrue(app.getClass().getSimpleName() == "Application");

					assertNotNull(app);
			 try {
				 app.close();
			 } catch (DesktopException e) {
				 e.printStackTrace();
			 }
			 return app;
		});

		latch.await(10, TimeUnit.SECONDS);
	}

	@Test
	public void launchManifest_Platform_Success() throws InvalidClassException, InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);

		desktopConnection.launchManifest(
				"https://openfin.github.io/platform-api-project-seed/public.json",
				com.openfin.desktop.platform.Platform.class,
				null).thenApply(app ->{
			assertTrue(app.getClass().getSimpleName() == "Platform");

			assertNotNull(app);

				app.quit();

			return app;
		});

		latch.await(10, TimeUnit.SECONDS);
	}

	@Test
	public void launchManifest_Manifest_Success() throws InvalidClassException, InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);

		desktopConnection.launchManifest(
				"https://openfin.github.io/platform-api-project-seed/public.json",
				com.openfin.desktop.platform.Platform.class,
				null).thenApply(app ->{
			assertTrue(app.getClass().getSimpleName() == "JSONObject");

			assertNotNull(app);

			app.quit();

			return app;
		});

		latch.await(10, TimeUnit.SECONDS);
	}
	
	@Test 
	public void getChildWindows() throws Exception {
		final int cnt = 5;
		Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);

		for (int i=0; i<cnt; i++) {
			WindowOptions childOptions = TestUtils.getWindowOptions("childWindow_" + i, TestUtils.openfin_app_url);
			TestUtils.createChildWindow(application, childOptions, desktopConnection);
		}
		
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger winCnt = new AtomicInteger(0);
		application.getChildWindows(
				new AsyncCallback<List<Window>>() {
					@Override
					public void onSuccess(List<Window> windows) {
						for (Window w : windows) {
							try {
								w.close();
								winCnt.incrementAndGet();
							}
							catch (DesktopException e) {
								e.printStackTrace();
							}
						}
						latch.countDown();
					}
				}, new AckListener() {
					@Override
					public void onSuccess(Ack ack) {
					}

					@Override
					public void onError(Ack ack) {
						logger.info("error getting child windows: {}", ack.getReason());
					}
				});

		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
		assertEquals(cnt, winCnt.get());
	}

	@Test 
	public void getInfo() throws Exception {
		Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
		final CountDownLatch latch = new CountDownLatch(1);
		application.getInfo(
				new AsyncCallback<JSONObject>() {
					@Override
					public void onSuccess(JSONObject obj) {
						logger.info("getInfo: {}", obj.toString());
						latch.countDown();
					}
				}, new AckListener() {
					@Override
					public void onSuccess(Ack ack) {
					}

					@Override
					public void onError(Ack ack) {
						logger.info("error getting application info: {}", ack.getReason());
					}
				});

		latch.await(5, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test 
	public void getParentUuid() throws Exception {
		Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
		final CountDownLatch latch = new CountDownLatch(1);
		application.getParentUuid(
				new AsyncCallback<String>() {
					@Override
					public void onSuccess(String uuid) {
						logger.info("getParentUuid: {}", uuid);
						latch.countDown();
					}
				}, new AckListener() {
					@Override
					public void onSuccess(Ack ack) {
					}

					@Override
					public void onError(Ack ack) {
						logger.info("error getting uuid of parent application: {}", ack.getReason());
					}
				});

		latch.await(5, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test 
	public void getTrayIconInfo() throws Exception {
		Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
		final CountDownLatch latch = new CountDownLatch(1);
		application.setTrayIcon(
				"http://icons.iconarchive.com/icons/marcus-roberto/google-play/512/Google-Search-icon.png", 
				null,
				new AckListener() {
					@Override
					public void onSuccess(Ack ack) {
						application.getTrayIconInfo(
								new AsyncCallback<JSONObject>() {
									@Override
									public void onSuccess(JSONObject obj) {
										logger.info("getTrayIconInfo: {}", obj.toString());
										latch.countDown();
									}
								}, new AckListener() {
									@Override
									public void onSuccess(Ack ack) {
									}

									@Override
									public void onError(Ack ack) {
										logger.info("error getting tray icon info: {}", ack.getReason());
									}
								});
					}

					@Override
					public void onError(Ack ack) {
					}
				});
		

		latch.await(5, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test 
	public void isRunning() throws Exception {
		Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
		final CountDownLatch latch = new CountDownLatch(1);
		application.isRunning(
				new AsyncCallback<Boolean>() {
					@Override
					public void onSuccess(Boolean running) {
						logger.info("isRunning: {}", running);
						latch.countDown();
					}
				}, new AckListener() {
					@Override
					public void onSuccess(Ack ack) {
					}

					@Override
					public void onError(Ack ack) {
						logger.info("error getting application running status: {}", ack.getReason());
					}
				});

		latch.await(5, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test
	public void registerUser() throws Exception {
		Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
		final CountDownLatch latch = new CountDownLatch(1);
		application.registerUser("MyUser", "MyApp", new AckListener() {
					@Override
					public void onSuccess(Ack ack) {
						logger.info("registered custom data");
						latch.countDown();
					}

					@Override
					public void onError(Ack ack) {
						logger.info("error registering user: {}", ack.getReason());
					}
				});

		latch.await(5, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}
	
	@Test
	public void errorStack() throws Exception {
		Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
		final CountDownLatch latch = new CountDownLatch(1);

		application.close(true, new AckListener() {

			@Override
			public void onSuccess(Ack ack) {
				application.getChildWindows(new AsyncCallback<List<Window>>() {

					@Override
					public void onSuccess(List<Window> result) {
					}
				}, new AckListener() {

					@Override
					public void onSuccess(Ack ack) {
					}

					@Override
					public void onError(Ack ack) {
						if (ack.getErrorStack() != null) {
							latch.countDown();
						}
					}
				});
			}

			@Override
			public void onError(Ack ack) {
			}
		});

		latch.await(5, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}
}
