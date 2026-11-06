#!/bin/sh
APP_BASE_NAME=`basename "$0"`
DIRNAME=`dirname "$0"`
if [ -z "$DIRNAME" ]; then
  DIRNAME="."
fi
APP_HOME=`cd "$DIRNAME" && pwd -P`
export APP_HOME
DEFAULT_JVM_OPTS=""
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVA_EXE="java"
which "$JAVA_EXE" >/dev/null 2>&1 || {
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
}
exec "$JAVA_EXE" $DEFAULT_JVM_OPTS -jar "$CLASSPATH" "$@"
