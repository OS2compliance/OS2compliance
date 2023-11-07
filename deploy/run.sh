#!/bin/bash

if [[ -z "${JAVA_OPTS}" ]]; then
	JAVA_OPTS=-Xmx200m
fi

java -Dloader.path=config/* $JAVA_OPTS -jar os2compliance.jar
