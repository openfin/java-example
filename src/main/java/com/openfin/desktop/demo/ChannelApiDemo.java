package com.openfin.desktop.demo;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import javax.json.Json;
import javax.json.JsonNumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.FinLauncher;
import com.openfin.desktop.FinRuntime;
import com.openfin.desktop.FinRuntimeConnectionListener;

public class ChannelApiDemo {

	private final static Logger logger = LoggerFactory.getLogger(ChannelApiDemo.class);
	private final static String CHANNEL_NAME = "ChannelApiDemo";
	private Thread invokingThread;
	private FinRuntime fin;
	
	public ChannelApiDemo(String type) {
		this.invokingThread = Thread.currentThread();
		this.initOpenFin(type);
	}
	
	private void initOpenFin(String type) {
		FinLauncher.newLauncherBuilder()
			.connectionListener(new FinRuntimeConnectionListener() {
				@Override
				public void onClose(String reason) {
					LockSupport.unpark(invokingThread);
				}
			}).build().launch().thenAccept(fin->{
				this.fin = fin;
				if ("client".equals(type)) {
					this.createChannelClient();
				}
				else {
					this.createChannelProvider();
				}
			});
	}
	
	private void createChannelClient( ) {
		fin.Channel.connect(CHANNEL_NAME).thenAccept(client->{
			logger.info("channel client connected");
			client.dispatch("getValue").thenAccept(v->{
				logger.info("client got value \"{}\" from provider after invoking getValue", ((JsonNumber)v).intValue());
			}).thenCompose(v->{
				return client.dispatch("increment").thenAccept(v2->{
					logger.info("client got value \"{}\" from provider after invoking increment", ((JsonNumber)v2).intValue());
				});
			}).thenCompose(v->{
				return client.dispatch("incrementBy", Json.createValue(13)).thenAccept(v2->{
					logger.info("client got value \"{}\" from provider after invoking incrementBy", ((JsonNumber)v2).intValue());
				});
			}).thenAccept(v->{
				try {
					client.dispatch("quitProvider").toCompletableFuture().get(500, TimeUnit.MILLISECONDS);
				}
				catch (InterruptedException | ExecutionException | TimeoutException e) {
					//probably won't get response and don't care.
				} 
				fin.disconnect();
			});
		});
	}
	
	private void createChannelProvider() {
		fin.Channel.addChannelDisconnectListener(e->{
			if (Objects.equals(CHANNEL_NAME, e.getString("channelName"))) {
				logger.info("provider disconnected");
				fin.disconnect();
			}
		});
		fin.Channel.create(CHANNEL_NAME).thenAccept(provider -> {
			logger.info("provider created");
			// provider created, register actions.
			AtomicInteger x = new AtomicInteger(0);

			provider.register("getValue", (payload, senderIdentity) -> {
				logger.info("provider processing action getValue");
				return Json.createValue(x.get());
			});

			provider.register("increment", (payload, senderIdentity) -> {
				logger.info("provider processing action increment");
				return Json.createValue(x.incrementAndGet());
			});

			provider.register("incrementBy", (payload, senderIdentity) -> {
				logger.info("provider processing action incrementBy, payload={}", payload.toString());
				int delta = ((JsonNumber) payload).intValue();
				return Json.createValue(x.addAndGet(delta));
			});
			provider.register("quitProvider", (payload, senderIdentity) -> {
				provider.destroy();
				return null;
			});
		});
	}
	
	public static void main(String[] args) {
		new ChannelApiDemo(args.length > 0 ? args[0] : null);
		LockSupport.park();
	}
}
