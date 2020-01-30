@ECHO OFF
set url=%1 
IF "%~1" == "" set url=https://openfin.co
echo Embedded url set to: %url%
java -cp openfin-desktop-java-example-7.1.1.jar;openfin-desktop-java-adapter-7.1.2.jar;TableLayout-20050920.jar;jna-4.5.1.jar;jna-platform-4.5.1.jar;json-20140107.jar;slf4j-api-1.7.5.jar;slf4j-jdk14-1.6.1.jar;websocket-api-9.3.12.v20160915.jar;websocket-client-9.3.12.v20160915.jar;websocket-common-9.3.12.v20160915.jar;jetty-io-9.3.12.v20160915.jar;jetty-util-9.3.12.v20160915.jar -Djava.util.logging.config.file=logging.properties -Dcom.openfin.demo.embed.URL=%URL% -Dcom.openfin.demo.version=stable com.openfin.desktop.demo.WindowEmbedDemo