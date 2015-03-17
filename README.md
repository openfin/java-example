# java-example
Examples for OpenFin Java adapter

## Run the example of connecting to OpenFin and creating applications

1. Clone this repository

2. Go to release directory and start run.bat

3. Once the java app starts, click on Start button, which should start OpenFin Runtime and "Hello OpenFin" HTML5 demo app.  The java app will wait and try to connect to OpenFin Runtime.

4. You can use buttons in Window Control section to move and re-size HTML5 window of Hello OpenFin app.

5. Click "Create Application" button, which should start a dialog with all the fields pre-populated for our FXLive demo HTML5 application.  Just click on "Create" button.

6. After FXLive starts, you can use the buttons under Window Control of Java app to control FXLive window.

## Source Code Review

Source code for the example is located in /src/main/java/com/openfin/desktop/demo/OpenFinDesktopDemo.java.  The followings overview of how it communicates with OpenFin Runtime with API calls supported by the Java adapter:

1. Create connection object:

            this.controller = new DesktopConnection("OpenFinDesktopDemo", "localhost", port);

    This code just creates an instance and it does not try to connect to runtime.

2. Launch and connect to OpenFin runtime:

            controller.launchAndConnect(this.desktopCommandLine, listener, 10000);

   listener is an instance of DesktopStateListener which provides callback on status of connections to runtime.  desktopCommandLine is a string of arguments passed to OpenFinRVM.
   This example by default passes remote config file for Hello OpenFin app, which will be started as the first app in OpenFin Runtime.

3. Create new application when clicking on Create App:

        Application app = new Application(options, controller, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                Application application = (Application) ack.getSource();
                application.run();   // run the app
                addApplication(options);
            }
            @Override
            public void onError(Ack ack) {
            }
        });

   options is an instance of ApplicationOptions, which is populated from App Create dialog.  AckListener interface provides callback for the operation.

   Once the application is created successfully, you can take actions on its window:

4.  Change opacity:

                WindowOptions options = new WindowOptions();
                options.setOpacity(newOpacityValue);
                application.getWindow().updateOptions(options, null);

5. Change Window size

                application.getWindow().resizeBy(10, 10, "top-left");


6. Publishes messages to a topic with InterApplicationBus

            org.json.JSONObject message = createSomeJsonMessage();
            controller.getInterApplicationBus().publish("someTopic", message);

7. Subscribes to a topic with InterApplicationBus

                            controller.getInterApplicationBus().subscribe("*", "someTopic", new BusListener() {
                                public void onMessageReceived(String sourceUuid, String topic, Object payload) {
                                    JSONObject message = (JSONObject) payload;
                                }
                            });

## Run the example of docking Java Swing window with HTML5 application

1. Clone this repository

2. Go to release directory and start docking.bat

3. Once the java app starts, click on "Launch OpenFin" button, which should start OpenFin Runtime and "Hello OpenFin" HTML5 demo app.  The java app will wait and try to connect to OpenFin Runtime.

4. After clicking "Dock to HTML5 app" button, you can move either window to see docking effect.

5. Click "Undock from HTML5 app" to undock 2 windows

## Source Code Review for docking windows

Source code for the example is located in /src/main/java/com/openfin/desktop/demo/OpenFinDockingDemo.java.  The followings overview of how it communicates with OpenFin Runtime with API calls supported by the Java adapter:

1. Create connection object:

            this.controller = new DesktopConnection("OpenFinDockingDemo", "localhost", port);

    This code just creates an instance and it does not try to connect to runtime.

2. Launch and connect to OpenFin runtime:

            controller.launchAndConnect(this.desktopCommandLine, listener, 10000);

   listener is an instance of DesktopStateListener which provides callback on status of connections to runtime.  desktopCommandLine is a string of arguments passed to OpenFinRVM.
   This example by default passes remote config file for Hello OpenFin app, which will be started as the first app in OpenFin Runtime.

3. dockToStartupApp and undockFromStartupApp show to dock and undock 2 windows.


## Run the example of embeding HTML5 application to Java Swing window with awt.canvas

1. Clone this repository

2. Go to release directory and start embedding.bat

3. Once the java app starts, click on "Launch OpenFin" button, which should start OpenFin Runtime and "Hello OpenFin" HTML5 demo app.  The java app will wait and try to connect to OpenFin Runtime.

4. You can click "Embed HTML5 app" button bring HTML5 window into Swing window.

## Source Code Review for embedding windows

Source code for the example is located in /src/main/java/com/openfin/desktop/demo/WindowEmbedDemo.java.  The followings overview of how it communicates with OpenFin Runtime with API calls supported by the Java adapter:

1. Create connection object:

            this.controller = new DesktopConnection("OpenFinDockingDemo", "localhost", port);

    This code just creates an instance and it does not try to connect to runtime.

2. Launch and connect to OpenFin runtime:

            controller.launchAndConnect(this.desktopCommandLine, listener, 10000);

   listener is an instance of DesktopStateListener which provides callback on status of connections to runtime.  desktopCommandLine is a string of arguments passed to OpenFinRVM.
   This example by default passes remote config file for Hello OpenFin app, which will be started as the first app in OpenFin Runtime.

3. embedStartupApp method shows to embed HTML5 app to Swing window.


## More Info

More information and API documentation can be found at https://www.openfin.co/developers.html?url=developers/api/java/overview.html

## Getting help

Please contact support@openfin.co