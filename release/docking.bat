REM batch to run Docking example of OpenFin and Java windows with Layout service.
REM layout.html is a simple example of OpenFin window that uses Layout service.
REM For this example to work, layout.html needs to be hosted by a web server and its URL needs to be configured with -Dcom.openfin.demo.layout.url

java -cp openfin-desktop-java-example-7.1.1.jar;openfin-desktop-java-adapter-7.1.1-SNAPSHOT.jar;openfin-snap-dock-1.0.0.1.jar;TableLayout-20050920.jar;jna-4.1.0.jar;jna-platform-4.1.0.jar;json-20140107.jar;slf4j-api-1.7.5.jar;slf4j-jdk14-1.6.1.jar;websocket-api-9.3.12.v20160915.jar;websocket-client-9.3.12.v20160915.jar;websocket-common-9.3.12.v20160915.jar;jetty-io-9.3.12.v20160915.jar;jetty-util-9.3.12.v20160915.jar -Djava.util.logging.config.file=logging.properties -Dcom.openfin.demo.layout.url=http://localhost:8000/layout.html com.openfin.desktop.demo.LayoutServiceDemo
