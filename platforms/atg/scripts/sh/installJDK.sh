#!/bin/sh

if [ -f $JAVA_HOME/bin/java ]
then
        echo "JDK already installed.."
else
        echo "JDK not installed.."
        cd /usr/local/java
        chmod +x jdk-6u25-linux-x64.bin
        ./jdk-6u25-linux-x64.bin < answer.txt &> /dev/null
        rm jdk-6u25-linux-x64.bin
fi


