#!/bin/bash
. ./rest.rc
java -cp target/CivRestHTTP-1.0-SNAPSHOT-jar-with-dependencies.jar  CivHttpServer ${PORT} ${REDISHOST} ${REDISPORT}
