package com.openfin.desktop.demo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import com.openfin.desktop.FinApplicationObject;
import com.openfin.desktop.FinLauncher;
import com.openfin.desktop.FinRuntime;
import com.openfin.desktop.FinRuntimeConnectionListener;

public class OpenFinHelloWorld implements FinRuntimeConnectionListener {
	
	private final static String helloOpenFinManifest = "https://cdn.openfin.co/demos/hello/app.json";
	
	private FinRuntime fin;

	private CompletableFuture<?> disconnectionFuture;

	
	public OpenFinHelloWorld(CompletableFuture<?> disconnectionFuture) {
		this.disconnectionFuture = disconnectionFuture;
		FinLauncher.newLauncherBuilder().connectionListener(this).build().launch().thenAccept(runtime->{
			this.fin = runtime;
			this.startFromManifest(helloOpenFinManifest);
		});
	}
	
	private void startFromManifest(String manifestUrl) {
		fin.Application.startFromManifest(manifestUrl).thenAccept(app->{
			app.quit(true);
		}).thenCompose(v->{
			return fin.disconnect();
		});
	}
	
	@Override
	public void onClose(String reason) {
		disconnectionFuture.complete(null);
	}

	public static void main(String[] args) {
		CompletableFuture<?> disconnectionFuture = new CompletableFuture<>();
		new OpenFinHelloWorld(disconnectionFuture);
		
		disconnectionFuture.join();
	}

}
