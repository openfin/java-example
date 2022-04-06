REM usage: junit.bat runtime_version
REM version defaults to stable
if "%1" == "" (
set RuntimeVersion="stable"
) else (
set RuntimeVersion="%1"
)

java -cp lib/hamcrest-core-1.3.jar;lib/hamcrest-library-1.1.jar;lib/jetty-client-9.4.18.v20190429.jar;lib/jetty-http-9.4.18.v20190429.jar;lib/jetty-io-9.4.18.v20190429.jar;lib/jetty-util-9.4.18.v20190429.jar;lib/jetty-xml-9.4.18.v20190429.jar;lib/jna-4.5.1.jar;lib/jna-platform-4.5.1.jar;lib/json-20160810.jar;lib/junit-4.11.jar;lib/log4j-1.2.17.jar;lib/mockito-core-1.9.5.jar;lib/objenesis-1.0.jar;lib/openfin-desktop-java-adapter-9.2.0.jar;lib/openfin-desktop-java-example-9.0.1-tests.jar;lib/slf4j-api-1.7.21.jar;lib/slf4j-jdk14-1.6.1.jar;lib/slf4j-log4j12-1.7.18.jar;lib/TableLayout-20050920.jar;lib/websocket-api-9.4.18.v20190429.jar;lib/websocket-client-9.4.18.v20190429.jar;lib/websocket-common-9.4.18.v20190429.jar -Djava.util.logging.config.file=logging.properties -Dcom.openfin.test.runtime.version=%RuntimeVersion% org.junit.runner.JUnitCore com.openfin.desktop.AllTests
