package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by wche on 2/12/16.
 */
public class DemoUtils {
    private static Logger logger = LoggerFactory.getLogger(DemoUtils.class.getName());

    public static void addEventListener(Application application, String evenType, EventListener eventListener, AckListener ackListener) throws Exception {
        logger.debug("addEventListener " + evenType);
        application.addEventListener(evenType, eventListener, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                logger.debug("addEventListener ack " + ack.isSuccessful());
                ackSuccess(ackListener, application);
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("Error adding event listener %s %s", evenType, ack.getReason()));
                ackError(ackListener, ack.getReason());
            }
        });
    }

    public static void runApplication(ApplicationOptions options, DesktopConnection desktopConnection, AckListener ackListener) throws Exception {
        AtomicReference<Application> appRef = new AtomicReference<>();
        Application application = createApplication(options, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                try {
                    runApplication(appRef.get(), true, ackListener);
                } catch (Exception e) {
                    logger.error("Error running application", e);
                    ackError(ackListener, e.getMessage());
                }
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
                ackError(ackListener, ack.getReason());
            }
        });
        appRef.set(application);
    }

    public static void runApplication(Application application, boolean checkAppConnected, AckListener ackListener) throws Exception {
        EventListener listener = new EventListener() {
            @Override
            public void eventReceived(ActionEvent actionEvent) {
                if (actionEvent.getType().equals("started")) {
                    if (!checkAppConnected) {
                        ackSuccess(ackListener, application);
                    }
                }
                else if (actionEvent.getType().equals("app-connected")) {
                    if (checkAppConnected) {
                        ackSuccess(ackListener, application);
                    }
                }
            }
        };

        AckListener eventAddAck = new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                application.run(new AckListener() {
                    @Override
                    public void onSuccess(Ack ack) {
                        logger.debug(String.format("Successful run %s", application.getOptions().getUUID()));
                    }
                    @Override
                    public void onError(Ack ack) {
                        ackError(ackListener, ack.getReason());
                        logger.error("Error running application", ack.getReason());
                    }
                });
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("Error adding event listener %s", ack.getReason()));
            }
        };
        if (checkAppConnected) {
            addEventListener(application.getWindow(), "app-connected", listener, eventAddAck);
        } else {
            addEventListener(application, "started", listener, eventAddAck);
        }

    }

    public static Application createApplication(ApplicationOptions options, DesktopConnection desktopConnection, AckListener ackListener) throws Exception {
        Application application = new Application(options, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                ackSuccess(ackListener, options);
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
                ackError(ackListener, ack.getReason());
            }
        });
        return application;
    }

    public static void addEventListener(Window window, String evenType, EventListener eventListener, AckListener ackListener) throws Exception {
        logger.debug("addEventListener " + evenType);
        window.addEventListener(evenType, eventListener, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                logger.debug("addEventListener ack " + ack.isSuccessful());
                ackSuccess(ackListener, window);
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("Error adding event listener %s %s", evenType, ack.getReason()));
                ackError(ackListener, ack.getReason());
            }
        });
    }

    public static void ackSuccess(AckListener ackListener, Object source) {
        if (ackListener != null) {
            ackListener.onSuccess(new Ack(new JSONObject(), source));
        }
    }

    public static void ackError(AckListener ackListener, String reason) {
        if (ackListener != null) {
            JSONObject obj = new JSONObject();
            obj.put("reason", reason);
            ackListener.onError(new Ack(obj, ackListener));
        }
    }

}
