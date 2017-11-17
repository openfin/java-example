package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.animation.AnimationTransitions;
import com.openfin.desktop.animation.OpacityTransition;
import com.openfin.desktop.animation.PositionTransition;
import com.openfin.desktop.animation.SizeTransition;

/**
 * Created by wche on 1/25/16.
 *
 */
public class WindowTest {
    private static Logger logger = LoggerFactory.getLogger(WindowTest.class.getName());

    private static final String DESKTOP_UUID = WindowTest.class.getName();
    private static DesktopConnection desktopConnection;
    private static final String child_window_url = "http://test.openf.in/test.html";  // simple test app

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
    public void hideShowMinMax() throws Exception {
        Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
        Window window = application.getWindow();
        Map<String, CountDownLatch> map = new HashMap<>();
        map.put("shown", new CountDownLatch(1));
        map.put("hidden", new CountDownLatch(1));
        map.put("minimized", new CountDownLatch(1));
        map.put("maximized", new CountDownLatch(1));
        map.put("restored", new CountDownLatch(1));
        EventListener listener = actionEvent -> {
            CountDownLatch latch = map.get(actionEvent.getType());
            if (latch != null) {
                latch.countDown();
            } else {
                logger.warn(String.format("event not being processed %s", actionEvent.getType()));
            }
        };
        for (String key: map.keySet()) {
            TestUtils.addEventListener(window, key, listener);
        }
        window.hide();
        map.get("hidden").await(5, TimeUnit.SECONDS);
        assertEquals("Window.hide timeout", map.get("hidden").getCount(), 0);
        assertFalse(getIsShowing(window));

        window.show();
        map.get("shown").await(5, TimeUnit.SECONDS);
        assertEquals("Window.show timeout", map.get("shown").getCount(), 0);
        assertTrue(getIsShowing(window));

        window.maximize();
        map.get("maximized").await(5, TimeUnit.SECONDS);
        assertEquals("Window.maximize timeout", map.get("maximized").getCount(), 0);
        assertEquals(getWindowState(window), "maximized");

        window.restore();
        map.get("restored").await(5, TimeUnit.SECONDS);
        assertEquals("Window.restore timeout", map.get("restored").getCount(), 0);

        window.minimize();
        map.get("minimized").await(5, TimeUnit.SECONDS);
        assertEquals("Window.minimize timeout", map.get("minimized").getCount(), 0);
        assertEquals(getWindowState(window), "minimized");

        window.restore();
        map.get("restored").await(5, TimeUnit.SECONDS);
        assertEquals("Window.restore timeout", map.get("restored").getCount(), 0);

        TestUtils.closeApplication(application);
    }

    @Test
    public void move() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        Window window = application.getWindow();
        CountDownLatch boundsLatch = new CountDownLatch(2); // used for 2 tests: moveBy and moveTo
        EventListener listener = actionEvent -> {
            if (actionEvent.getType().equals("bounds-changed")) {
                boundsLatch.countDown();
            }
        };
        TestUtils.addEventListener(window, "bounds-changed", listener);
        int left = options.getMainWindowOptions().getDefaultLeft() + 100, top = options.getMainWindowOptions().getDefaultTop() + 100;
        window.moveTo(left, top);
        boundsLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.moveTo timeout", boundsLatch.getCount(), 1);
        WindowBounds windowBounds = TestUtils.getBounds(window);
        assertTrue(windowBounds.getLeft() == left && windowBounds.getTop() == top);

        window.moveBy(100, 100);
        boundsLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.moveBy timeout", boundsLatch.getCount(), 0);
        windowBounds = TestUtils.getBounds(window);
        assertTrue(windowBounds.getLeft() == left+100 && windowBounds.getTop() == top+100);

        TestUtils.closeApplication(application);
    }

    @Test
    public void resize() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        Window window = application.getWindow();
        CountDownLatch boundsLatch = new CountDownLatch(2); // used for 2 tests: moveBy and moveTo
        EventListener listener = actionEvent -> {
            if (actionEvent.getType().equals("bounds-changed")) {
                boundsLatch.countDown();
            }
        };
        TestUtils.addEventListener(window, "bounds-changed", listener);
        int top = options.getMainWindowOptions().getDefaultTop();
        int width = options.getMainWindowOptions().getDefaultWidth() + 100, height = options.getMainWindowOptions().getCornerRoundingHeight() + 100;

        window.resizeTo(width, height, "top-left");  // @TODO need to add tests for other anchors
        boundsLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.resizeTo timeout", boundsLatch.getCount(), 1);
        WindowBounds windowBounds = TestUtils.getBounds(window);
        assertTrue(windowBounds.getWidth() == width && windowBounds.getHeight() == height && windowBounds.getTop() == top);

        window.resizeBy(100, 100, "top-left");  // @TODO need to add tests for other anchors
        boundsLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.resizeBy timeout", boundsLatch.getCount(), 0);
        windowBounds = TestUtils.getBounds(window);
        assertTrue(windowBounds.getWidth() == width+100 && windowBounds.getHeight() == height+100 && windowBounds.getTop() == top);

        TestUtils.closeApplication(application);
    }

    @Test
    public void updateBounds() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        Window window = application.getWindow();
        int top = options.getMainWindowOptions().getDefaultTop() + 50, left = options.getMainWindowOptions().getDefaultLeft();
        int height = options.getMainWindowOptions().getDefaultLeft() + 80, width = options.getMainWindowOptions().getDefaultWidth() + 80;
        CountDownLatch latch = new CountDownLatch(1);
        window.setBounds(left, top, width, height, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    latch.countDown();
                }
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.setBounds timeout", latch.getCount(), 0);
        WindowBounds windowBounds = TestUtils.getBounds(window);
        assertTrue(windowBounds.getWidth() == width && windowBounds.getHeight() == height && windowBounds.getTop() == top &&
                        windowBounds.getLeft() == left);

        TestUtils.closeApplication(application);
    }

    @Test
    public void updateAndCheckOptions() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        WindowOptions newOptions = new WindowOptions();
        boolean newFrame = !options.getMainWindowOptions().getFrame();
        newOptions.setFrame(newFrame);
        Window window = application.getWindow();
        CountDownLatch latch = new CountDownLatch(1);
        window.updateOptions(newOptions, new AckListener() {
                    @Override
                    public void onSuccess(Ack ack) {
                        if (ack.isSuccessful()) {
                            latch.countDown();
                        }
                    }
                    @Override
                    public void onError(Ack ack) {
                        logger.error(String.format("onError %s", ack.getReason()));
                    }
                });
        latch.await(3, TimeUnit.SECONDS);
        assertEquals("Window.updateOptions timeout", latch.getCount(), 0);

        newOptions = getOptions(window);
        assertEquals(newOptions.getFrame(), newFrame);

        TestUtils.closeApplication(application);
    }

    @Test
    public void dockAndUndock() throws Exception {
        String childName = "docking test";
        Application application = TestUtils.runApplication(TestUtils.getAppOptions(null), desktopConnection);
        Window mainWindow = application.getWindow();
        WindowOptions childOptions = TestUtils.getWindowOptions(childName, child_window_url);
        Window childWindow = TestUtils.createChildWindow(application, childOptions, desktopConnection);
        WindowBounds beforeMoveBounds = TestUtils.getBounds(childWindow);
        CountDownLatch joinLatch = new CountDownLatch(1);
        childWindow.joinGroup(mainWindow, new AckListener() {
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

        CountDownLatch groupInfoLatch = new CountDownLatch(2);
        mainWindow.getGroup(result -> {
            for (Window window : result) {
                if (window.getUuid().equals(mainWindow.getUuid()) && window.getName().equals(mainWindow.getName())) {
                    groupInfoLatch.countDown();
                }
                else if (window.getUuid().equals(childWindow.getUuid()) && window.getName().equals(childWindow.getName())) {
                    groupInfoLatch.countDown();
                }
            }
        }, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
            }
        });
        groupInfoLatch.await(3, TimeUnit.SECONDS);
        assertEquals(groupInfoLatch.getCount(), 0);

        int leftBy = 20, topBy = 30;
        TestUtils.moveWindowBy(mainWindow, leftBy, topBy);
        // child window sohuld move with main window since they are docked
        WindowBounds afterMoveBounds = TestUtils.getBounds(childWindow);
        int topAfterDockMove = afterMoveBounds.getTop(), leftAfterDockMove = afterMoveBounds.getLeft();
        assertEquals(afterMoveBounds.getTop() - beforeMoveBounds.getTop(), topBy);
        assertEquals(afterMoveBounds.getLeft() - beforeMoveBounds.getLeft(), leftBy);

        // undock by leaving the group
        CountDownLatch undockLatch = new CountDownLatch(1);
        childWindow.leaveGroup(new AckListener() {
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
        TestUtils.moveWindowBy(mainWindow, leftBy, topBy);
        // child window should not move afer leaving group
        afterMoveBounds = TestUtils.getBounds(childWindow);
        assertEquals(afterMoveBounds.getLeft().intValue(), leftAfterDockMove);
        assertEquals(afterMoveBounds.getTop().intValue(), topAfterDockMove);

        TestUtils.closeApplication(application);
    }


    @Test
    public void animateMove() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);

        AnimationTransitions transitions = new AnimationTransitions();
        transitions.setPosition(new PositionTransition(300, 400, 2000));  //duration in millisecods
        CountDownLatch transitionLatch = new CountDownLatch(1);
        application.getWindow().animate(transitions, null, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                transitionLatch.countDown();
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        transitionLatch.await(20, TimeUnit.SECONDS);

        WindowBounds movedBounds = TestUtils.getBounds(application.getWindow());

        assertEquals(300, movedBounds.getLeft().intValue());
        assertEquals(400, movedBounds.getTop().intValue());

        TestUtils.closeApplication(application);
    }

    @Test
    public void animateSize() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);

        AnimationTransitions transitions = new AnimationTransitions();
        transitions.setSize(new SizeTransition(300, 400, 2000));  //duration in millisecods
        CountDownLatch transitionLatch = new CountDownLatch(1);
        application.getWindow().animate(transitions, null, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                transitionLatch.countDown();
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        transitionLatch.await(20, TimeUnit.SECONDS);

        WindowBounds movedBounds = TestUtils.getBounds(application.getWindow());

        assertEquals(300, movedBounds.getWidth().intValue());
        assertEquals(400, movedBounds.getHeight().intValue());

        TestUtils.closeApplication(application);
    }

    @Test
    public void animateOpacity() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);

        AnimationTransitions transitions = new AnimationTransitions();
        transitions.setOpacity(new OpacityTransition(0.5, 2000));  //duration in millisecods
        CountDownLatch transitionLatch = new CountDownLatch(1);
        application.getWindow().animate(transitions, null, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                transitionLatch.countDown();
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        transitionLatch.await(20, TimeUnit.SECONDS);

        WindowOptions changedOptions = getOptions(application.getWindow());

        assertEquals(0.5, changedOptions.getOpacity(), 0);

        TestUtils.closeApplication(application);
    }

    private String getWindowState(Window window) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> windowAtomicReference = new AtomicReference<>();
        window.getState(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    windowAtomicReference.set(ack.getData().toString());
                    latch.countDown();
                }
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.getState timeout", latch.getCount(), 0);
        return windowAtomicReference.get();
    }

    private boolean getIsShowing(Window window) throws Exception {
        CountDownLatch isShowingLatch = new CountDownLatch(1);
        AtomicReference<Boolean> windowAtomicReference = new AtomicReference<>();
        window.isShowing(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    windowAtomicReference.set(Boolean.parseBoolean(ack.getData().toString()));
                    isShowingLatch.countDown();
                }
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        isShowingLatch.await(3, TimeUnit.SECONDS);
        assertEquals("Window.isShowing timeout", isShowingLatch.getCount(), 0);
        return windowAtomicReference.get();
    }

    private WindowOptions getOptions(Window window) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<WindowOptions> windowAtomicReference = new AtomicReference<>();
        window.getOptions(result -> {
            windowAtomicReference.set(result);
            latch.countDown();
        }, null);
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.getOptions timeout", latch.getCount(), 0);
        return windowAtomicReference.get();
    }

    @Test
    public void executeJavaScript() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        Window window = application.getWindow();
        CountDownLatch latch = new CountDownLatch(1);
        window.executeJavaScript("var w = fin.desktop.Window.getCurrent(); w.name;", result -> {
            if (result != null && result.toString().equals(window.getName())) {
                latch.countDown();
            }
        }, null);

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.executeJavaScript timeout", latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }

    @Test
    public void navigate() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        Window window = application.getWindow();
        CountDownLatch latch = new CountDownLatch(1);
        window.navigate("https://openfin.co", new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    latch.countDown();
                }
            }
            @Override
            public void onError(Ack ack) {
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.navigate timeout", latch.getCount(), 0);
        Thread.sleep(1000); // give time for https://openfin.co to load
        window.executeJavaScript("location.href", result -> {
            if (result != null && result.toString().equals("https://openfin.co")) {
                latch.countDown();
            }
        }, null);

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.executeJavaScript timeout", latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }

    @Test
    public void navigateBack() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        Window window = application.getWindow();
        CountDownLatch latch = new CountDownLatch(1);
        window.navigate("https://openfin.co", new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                	window.navigate("https://www.google.com", new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            if (ack.isSuccessful()) {
                            	window.navigateBack(new AckListener() {
                                    @Override
                                    public void onSuccess(Ack ack) {
                                        if (ack.isSuccessful()) {
                                            if (ack.isSuccessful()) {
                                                latch.countDown();
                                            }
                                        }
                                    }
                                    @Override
                                    public void onError(Ack ack) {
                                    }
                                });
                            }
                        }
                        @Override
                        public void onError(Ack ack) {
                        }
                    });
                }
            }
            @Override
            public void onError(Ack ack) {
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.navigate timeout", latch.getCount(), 0);
        Thread.sleep(1000); // give time for https://openfin.co to load
        window.executeJavaScript("location.href", result -> {
            if (result != null && result.toString().equals("https://openfin.co")) {
                latch.countDown();
            }
        }, null);

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.executeJavaScript timeout", latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }

    @Test
    public void navigateForward() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        Window window = application.getWindow();
        CountDownLatch latch = new CountDownLatch(1);
        window.navigate("https://www.google.com", new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                	window.navigate("https://openfin.co", new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            if (ack.isSuccessful()) {
                            	window.navigateBack(new AckListener() {
                                    @Override
                                    public void onSuccess(Ack ack) {
                                        if (ack.isSuccessful()) {
                                        	window.navigateForward(new AckListener() {
                                                @Override
                                                public void onSuccess(Ack ack) {
                                                    if (ack.isSuccessful()) {
                                                        latch.countDown();
                                                    }
                                                }
                                                @Override
                                                public void onError(Ack ack) {
                                                }
                                            });
                                        }
                                    }
                                    @Override
                                    public void onError(Ack ack) {
                                    }
                                });
                            }
                        }
                        @Override
                        public void onError(Ack ack) {
                        }
                    });
                }
            }
            @Override
            public void onError(Ack ack) {
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.navigate timeout", latch.getCount(), 0);
        Thread.sleep(1000); // give time for https://openfin.co to load
        window.executeJavaScript("location.href", result -> {
            if (result != null && result.toString().equals("https://openfin.co")) {
                latch.countDown();
            }
        }, null);

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.executeJavaScript timeout", latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }

    @Test
    public void stopNavigate() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);
        Window window = application.getWindow();
        CountDownLatch latch = new CountDownLatch(1);
        window.navigate("https://openfin.co", new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                	window.navigate("https://www.cnn.com", null);
                	window.stopWindowNavigation(new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            if (ack.isSuccessful()) {
                                latch.countDown();
                            }
                        }
                        @Override
                        public void onError(Ack ack) {
                        }
                    });
                }
            }
            @Override
            public void onError(Ack ack) {
            }
        });
        
        
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.navigate timeout", latch.getCount(), 0);
        Thread.sleep(1000);
        window.executeJavaScript("location.href", result -> {
            if (result != null && result.toString().equals("https://openfin.co")) {
                latch.countDown();
            }
        }, null);

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Window.executeJavaScript timeout", latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }
    
    @Test
    public void preload() throws Exception {
    	
        ApplicationOptions options = TestUtils.getAppOptions(null);
        options.getMainWindowOptions().setPreload("https://cdn.openfin.co/examples/junit/preload.js");
        
        Application application = TestUtils.runApplication(options, desktopConnection);
        
        Window mainWindow = application.getWindow();
        
        Thread.sleep(1000);
        CountDownLatch latch = new CountDownLatch(1);
        mainWindow.executeJavaScript("_preload", result -> {
            if (result != null && result.toString().equals("12345")) {
                latch.countDown();
            }
        }, null);
        
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("execute java script timeout: " + options.getUUID(), 0, latch.getCount());

        TestUtils.closeApplication(application);
    }
    
    @Test
    public void maxWidthHeight() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(null);
        options.getMainWindowOptions().setDefaultWidth(300);
        options.getMainWindowOptions().setDefaultHeight(300);
        options.getMainWindowOptions().setMaxWidth(300);
        options.getMainWindowOptions().setMaxHeight(300);
        
        Application application = TestUtils.runApplication(options, desktopConnection);
        Window window = application.getWindow();
        CountDownLatch latch = new CountDownLatch(1);
        window.setBounds(0, 0, 400, 400, new AckListener() {

			@Override
			public void onSuccess(Ack ack) {
				window.getBounds(
						new AsyncCallback<WindowBounds>() {
							@Override
							public void onSuccess(WindowBounds result) {
								if (result.getWidth() == 300 && result.getHeight() == 300) {
									latch.countDown();
								}
							}
						}, 
						new AckListener() {
							@Override
							public void onSuccess(Ack ack) {
							}
							@Override
							public void onError(Ack ack) {
							}
						});
			}

			@Override
			public void onError(Ack ack) {
			}
        	
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("maxWidthHeight test timeout", latch.getCount(), 0);

        TestUtils.closeApplication(application);
    }
    
	@Test
	public void blur() throws Exception {
		ApplicationOptions options = TestUtils.getAppOptions(null);
		Application application = TestUtils.runApplication(options, desktopConnection);
		Window window = application.getWindow();
		CountDownLatch latch = new CountDownLatch(1);

		window.addEventListener("blurred", new EventListener() {

			@Override
			public void eventReceived(ActionEvent e) {
				if ("blurred".equals(e.getType())) {
					latch.countDown();
				}
			}
		}, new AckListener() {

			@Override
			public void onSuccess(Ack ack) {
			}

			@Override
			public void onError(Ack ack) {
			}
		});

		window.focus(new AckListener() {

			@Override
			public void onSuccess(Ack ack) {
				try {
					window.blur();
				}
				catch (DesktopException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onError(Ack ack) {
			}
		});
		latch.await(10, TimeUnit.SECONDS);
		assertEquals("blur test timeout", 0, latch.getCount());

		TestUtils.closeApplication(application);
	}

	@Ignore
	@Test
	public void getSetZoomLevel() throws Exception {
		double level = -1.5;
		ApplicationOptions options = TestUtils.getAppOptions(null);
		Application application = TestUtils.runApplication(options, desktopConnection);
		Window window = application.getWindow();
		CountDownLatch latch1 = new CountDownLatch(1);

		window.setZoomLevel(level, new AckListener() {

			@Override
			public void onSuccess(Ack ack) {
				latch1.countDown();
			}

			@Override
			public void onError(Ack ack) {
				logger.error("error setting zoom level, reason: {}", ack.getReason());
			}
		});
		latch1.await(10, TimeUnit.SECONDS);
		assertEquals("setZoomLevel test timeout", 0, latch1.getCount());
		
		CountDownLatch latch2 =  new CountDownLatch(1); 
		window.getZoomLevel(new AsyncCallback<Double>() {
			
			@Override
			public void onSuccess(Double zLevel) {
				if (zLevel.doubleValue() == level) {
					latch2.countDown();
				}
			}
		}, null);
		latch2.await(10, TimeUnit.SECONDS);
		assertEquals("getZoomLevel test timeout", 0, latch2.getCount());

		TestUtils.closeApplication(application);
	}
	
	@Test
	public void reload() throws Exception {
		ApplicationOptions options = TestUtils.getAppOptions(null);
		Application application = TestUtils.runApplication(options, desktopConnection);
		Window window = application.getWindow();
		CountDownLatch latch = new CountDownLatch(1);

		window.reload(true, new AckListener() {

			@Override
			public void onSuccess(Ack ack) {
				latch.countDown();
			}

			@Override
			public void onError(Ack ack) {
				logger.error("error reloading window, reason: {}", ack.getReason());
			}
		});
		latch.await(10, TimeUnit.SECONDS);
		assertEquals("reload test timeout", 0, latch.getCount());
	}
}
