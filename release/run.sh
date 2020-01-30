#!/usr/bin/env bash
# Example shell script to launch Runtime on Mac & Linux
# Since RVM is not available,  this script uses openfin-cli to launch Runtime
# After installing openfin-cli, the following script needs to be created as start.sh in openfin-cli root directory

#cd "$(dirname ${BASH_SOURCE[0]})"

#if [[ $1 == file:///* ]] ;
#then
#    configFile=`echo $1 | cut -c8-`
#else
#    configFile=$1
#fi
#node cli.js -l -c $configFile

java -cp openfin-desktop-java-example-7.1.1.jar;openfin-desktop-java-adapter-7.1.2.jar:TableLayout-20050920.jar:jna-4.2.2.jar:jna-platform-4.2.2.jar:json-20140107.jar:slf4j-api-1.7.5.jar:slf4j-jdk14-1.6.1.jar:websocket-api-9.3.12.v20160915.jar:websocket-client-9.3.12.v20160915.jar:websocket-common-9.3.12.v20160915.jar:jetty-io-9.3.12.v20160915.jar:jetty-util-9.3.12.v20160915.jar -Djava.util.logging.config.file=logging.properties -Dcom.openfin.installer.location=/path-to-openfin-cli/start.sh  -Dcom.openfin.demo.version=8.56.27.22 com.openfin.desktop.demo.OpenFinDesktopDemo

