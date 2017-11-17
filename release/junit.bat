REM usage: junit.bat runtime_version
REM version defaults to stable
if "%1" == "" (
set RuntimeVersion="stable"
) else (
set RuntimeVersion="%1"
)

java -cp openfin-desktop-java-example-6.0.1.2-tests.jar;openfin-desktop-java-adapter-6.0.1.3-SNAPSHOT.jar;TableLayout-20050920.jar;jna-4.1.0.jar;jna-platform-4.1.0.jar;json-20160810.jar;slf4j-api-1.7.5.jar;slf4j-jdk14-1.6.1.jar;junit-4.11.jar;hamcrest-core-1.3.jar;hamcrest-core-1.3.jar;hamcrest-library-1.1.jar;mockito-core-1.9.5.jar;websocket-api-9.3.12.v20160915.jar;websocket-client-9.3.12.v20160915.jar;websocket-common-9.3.12.v20160915.jar;jetty-io-9.3.12.v20160915.jar;jetty-util-9.3.12.v20160915.jar -Djava.util.logging.config.file=logging.properties -Dcom.openfin.test.runtime.version=%RuntimeVersion% org.junit.runner.JUnitCore com.openfin.desktop.AllTests
