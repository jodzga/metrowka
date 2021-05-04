#!/bin/bash

RPS=6000
DURATION=1800
TIMEOUT=5000
MAX_IN_FLIGHT=5000
RPC_TRACE_SIZE_MULTIPLIER=4.0
URL_BASE="https://lor1-app18323.prod.linkedin.com:11937"
ARRIVAL_TYPE="poisson"
KEY_STORE="/export/content/lid/apps/espresso-storage-node/i001/var/identity.p12"
PARTIAL_PA_LOGS="/export/content/lid/apps/metrowka-generator/palogs"
FILTER="CareersMemberJobActivitiesDB"
JAVA_HOME="/export/apps/jdk/JDK-1_8_0_172/"
JAVA_OPTIONS="-Xms8g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=10"

JARS=$(ls -la *.jar | awk '{print $9}' | tr "\n" ":" | sed -e "s/^\(.*\):$/\1/g");

is_done=0

sigterm_handler()
{
  sudo -uapp pkill -uapp -f ".*com.linkedin.metrowka.generator.EspressoRouterLoadGenerator"
  sleep 120
  sudo -uapp pkill -uapp -9 -f ".*com.linkedin.metrowka.generator.EspressoRouterLoadGenerator"
}

trap sigterm_handler SIGTERM

echo "Executing $JAVA_HOME/bin/java $JAVA_OPTIONS -classpath .:$JARS com.linkedin.metrowka.generator.EspressoRouterLoadGenerator $RPS $DURATION $URL_BASE $ARRIVAL_TYPE $KEY_STORE $PARTIAL_PA_LOGS $FILTER  $TIMEOUT $MAX_IN_FLIGHT $RPC_TRACE_SIZE_MULTIPLIER";
sudo -uapp $JAVA_HOME/bin/java $JAVA_OPTIONS -classpath .:$JARS com.linkedin.metrowka.generator.EspressoRouterLoadGenerator $RPS $DURATION $URL_BASE $ARRIVAL_TYPE $KEY_STORE $PARTIAL_PA_LOGS $FILTER $TIMEOUT $MAX_IN_FLIGHT $RPC_TRACE_SIZE_MULTIPLIER
