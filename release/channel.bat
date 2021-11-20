@ECHO OFF
REM This program is an example of channel API.   It can be run as channel provider or client
REM more information can be found in comments of com.openfin.desktop.demo.ChannelExample.java
REM Channel name: ChannelExample
REM usage:  channel.bat [ provider | client ]
set mode=%1
IF "%~1" == "" set mode=provider
echo Channel type set to: %mode%
java -cp lib/hamcrest-core-1.3.jar;lib/hamcrest-library-1.1.jar;lib/jetty-client-9.4.18.v20190429.jar;lib/jetty-http-9.4.18.v20190429.jar;lib/jetty-io-9.4.18.v20190429.jar;lib/jetty-util-9.4.18.v20190429.jar;lib/jetty-xml-9.4.18.v20190429.jar;lib/jna-5.9.0.jar;lib/jna-platform-5.9.0.jar;lib/json-20210307.jar;lib/junit-4.11.jar;lib/mockito-core-1.9.5.jar;lib/objenesis-1.0.jar;lib/openfin-desktop-java-adapter-10.0.1-SNAPSHOT.jar;lib/openfin-desktop-java-example-10.0.1.jar;lib/slf4j-api-1.7.21.jar;lib/slf4j-jdk14-1.6.1.jar;lib/slf4j-log4j12-1.7.18.jar;lib/TableLayout-20050920.jar;lib/websocket-api-9.4.18.v20190429.jar;lib/websocket-client-9.4.18.v20190429.jar;lib/websocket-common-9.4.18.v20190429.jar;lib/webrtc-java-0.3.0.jar;lib/webrtc-java-0.3.0-windows-x86_64.jar -Djava.util.logging.config.file=logging.properties -Dcom.openfin.demo.runtime.version=24.96.67.4 com.openfin.desktop.demo.ChannelExample %mode%

