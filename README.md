# Java Adapter Example

## Overview
The following repo contains examples for OpenFin's Java adapter.

## Guidelines
Run the example of connecting to OpenFin and creating applications

1. Clone this repository

2. Go to release directory and start run.bat

3. Once the java app starts, click on Start button, which should start OpenFin Runtime.  The java app will wait and try to connect to OpenFin Runtime.

4. Once OpenFin Runtime is started and Java app connects successfully,  "Create Application" button is enabled.  You can click on the button to bring up a dialog for entering configuration of any HTML5 app.  By default, the dialog is pre-populated with configuration for Hello OpenFin demo app.

5. You can use buttons in Window Control section to move and re-size HTML5 window of Hello OpenFin app.

6. Click "Create Application" button, which should start a dialog with all the fields pre-populated for our Hello OpenFin demo HTML5 application.  Just click on "Create" button.

7. After Hello OpenFin starts, you can use the buttons under Window Control of Java app to control Hello OpenFin window.

## Source Code Review

Source code for the example is located in /src/main/java/com/openfin/desktop/demo/OpenFinDesktopDemo.java.  The followings overview of how it communicates with OpenFin Runtime with API calls supported by the Java adapter:

1. Create connection object:

```java
	this.desktopConnection = new DesktopConnection("OpenFinDesktopDemo");
```
   This code just creates an instance of DesktopConnection and it does not try to connect to runtime.

2. Launch and connect to stable version of OpenFin runtime:

```java
	// create an instance of RuntimeConfiguration and configure Runtime by setting properties in RuntimeConfiguration
	this.runtimeConfiguration = new RuntimeConfiguration();
	// launch and connect to OpenFin Runtime
	desktopConnection.connect(this.runtimeConfiguration, listener, 10000);
```
   listener is an instance of DesktopStateListener which provides callback on status of connections to runtime.

3. Create new application when clicking on Create App:

 ```java
	Application app = new Application(options, desktopConnection, new AckListener() {
		@Override
		public void onSuccess(Ack ack) {
			Application application = (Application) ack.getSource();
			application.run();   // run the app
		}
		@Override
		public void onError(Ack ack) {
		}
	});
```
   options is an instance of ApplicationOptions, which is populated from App Create dialog.  AckListener interface provides callback for the operation.

   Once the application is created successfully, you can take actions on its window:

4.  Change opacity:

```java
	WindowOptions options = new WindowOptions();
	options.setOpacity(newOpacityValue);
	application.getWindow().updateOptions(options, null);
```

5. Change Window size

```java
	application.getWindow().resizeBy(10, 10, "top-left");
```

6. Publishes messages to a topic with InterApplicationBus

```java
	org.json.JSONObject message = createSomeJsonMessage();
	desktopConnection.getInterApplicationBus().publish("someTopic", message);
```

7. Subscribes to a topic with InterApplicationBus

```java
	desktopConnection.getInterApplicationBus().subscribe("*", "someTopic", new BusListener() {
		public void onMessageReceived(String sourceUuid, String topic, Object payload) {
			JSONObject message = (JSONObject) payload;
		}
	});
```

## Run the example of docking Java Swing window with HTML5 application

1. Clone this repository

2. Go to release directory and start docking.bat

3. Once the java app starts, click on "Launch OpenFin" button, which should start OpenFin Runtime and "Hello OpenFin" HTML5 demo app.  The java app will wait and try to connect to OpenFin Runtime.

4. After clicking "Dock to HTML5 app" button, you can move either window to see docking effect.

5. Click "Undock from HTML5 app" to undock 2 windows

## Source Code Review for docking windows

Source code for the example is located in /src/main/java/com/openfin/desktop/demo/OpenFinDockingDemo.java.  This example uses Snap&Dock library from https://github.com/openfin/java-snap-and-dock

1. Create connection object:

```java
	this.desktopConnection = new DesktopConnection("OpenFinDockingDemo", "localhost", port);
```

   This code just creates an instance and it does not try to connect to runtime.

2. Launch and connect to stable version of OpenFin runtime:

```java
	desktopConnection.connectToVersion("stable", listener, 60);
```

   listener is an instance of DesktopStateListener which provides callback on status of connections to runtime.

3. Once Runtime is running, an instance of DockingManager is create with

```java
	this.dockingManager = new DockingManager(this.desktopConnection, javaParentAppUuid);
```

4. Any OpenFin window can be registered with DockingManager with

```java
	dockingManager.registerWindow(openFinWindow);
```

5. Any Java window can be registered with DockingManager with

```java
	dockingManager.registerJavaWindow(javaWindowName, jFrame, AckListener);
```

6. An application can receive dock and undock events from DockingManger with

```java
	desktopConnection.getInterApplicationBus().subscribe("*", "window-docked", EventListener);
	desktopConnection.getInterApplicationBus().subscribe("*", "window-undocked", EventListener);
```

7. An application can request DockingManager to undock a window with:

```java
	JSONObject msg = new JSONObject();
	msg.put("applicationUuid", javaParentAppUuid);
	msg.put("windowName", javaWindowName);
	desktopConnection.getInterApplicationBus().publish("undock-window", msg);
```

Once the demo is running, Windows snap while being draggted close to other windows.  Snapped windows dock on mounse release. 

## Run the example of embedding HTML5 application into a Java Swing window

1. Clone this repository

2. Go to release directory and start embed.bat

3. Once the java app starts, click on "Launch OpenFin" button, which should start OpenFin Runtime and embed the OpenFin application that points to https://openfin.co

4. Click "Shutdown OpenFin" button to close HTML5 application and the Java Swing window

## Source Code Review for embedded OpenFin application

Source code for the example is located in /src/main/java/com/openfin/desktop/demo/WindowEmbedDemo.java

1. create a canvas and place it where the HTML5 application should be embedded.

```java
	embedCanvas = new java.awt.Canvas();
	panel.add(embedCanvas, BorderLayout.CENTER);
```

2. listen to the canvas resize event, and resize embedded HTML5 application accordingly.

```java
	embedCanvas.addComponentListener(new ComponentAdapter() {
	    @Override
	    public void componentResized(ComponentEvent event) {
	        super.componentResized(event);
	        Dimension newSize = event.getComponent().getSize();
	        try {
	            if (startupHtml5app != null) {
	                startupHtml5app.getWindow().embedComponentSizeChange((int)newSize.getWidth(), (int)newSize.getHeight());
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	});
```

3. launch and connect to OpenFin runtime 

```java
	this.desktopConnection = new DesktopConnection(appUuid);
	DesktopStateListener listener = new DesktopStateListener() {...};
	RuntimeConfiguration configuration = new RuntimeConfiguration();
	configuration.setRuntimeVersion(desktopVersion);
	desktopConnection.connect(configuration, listener, 60);
```

4. create HTML5 application

```java
	ApplicationOptions options = new ApplicationOptions(startupUuid, startupUuid, openfin_app_url);
	WindowOptions mainWindowOptions = new WindowOptions();
	options.setMainWindowOptions(mainWindowOptions);
	DemoUtils.runApplication(options, this.desktopConnection, new AckListener() {...});
```

5. embed HTML5 application into the canvas

```java
	startupHtml5app = Application.wrap(this.startupUuid, this.desktopConnection);
	Window html5Wnd = startupHtml5app.getWindow();
	long parentHWndId = Native.getComponentID(this.embedCanvas);
	html5Wnd.embedInto(parentHWndId, this.embedCanvas.getWidth(), this.embedCanvas.getHeight(), new AckListener() {...});
```

## More Info
More information and API documentation can be found at https://openfin.co/java-api/

## Disclaimers
* This is a starter example and intended to demonstrate to app providers a sample of how to approach an implementation. There are potentially other ways to approach it and alternatives could be considered. 
* Its possible that the repo is not actively maintained.

## Support
Please enter an issue in the repo for any questions or problems. 
<br> Alternatively, please contact us at support@openfin.co
