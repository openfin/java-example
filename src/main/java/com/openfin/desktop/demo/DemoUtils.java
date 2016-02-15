package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by wche on 2/12/16.
 */
public class DemoUtils {
    private static Logger logger = LoggerFactory.getLogger(DemoUtils.class.getName());

    public static void addEventListener(Window window, String evenType, EventListener eventListener) throws Exception {
        logger.debug("addEventListener " + evenType);
        CountDownLatch latch = new CountDownLatch(1);
        window.addEventListener(evenType, eventListener, null);
//        window.addEventListener(evenType, eventListener, new AckListener() {
//            @Override
//            public void onSuccess(Ack ack) {
//                latch.countDown();
//                logger.debug("addEventListener ack " + ack.isSuccessful());
//            }
//            @Override
//            public void onError(Ack ack) {
//                logger.error(String.format("Error adding event listener %s %s", evenType, ack.getReason()));
//            }
//        });
//        latch.await(5, TimeUnit.SECONDS);
    }

}
