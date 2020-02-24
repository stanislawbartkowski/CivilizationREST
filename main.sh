#!/bin/bash
[ "${CORS}" == "cors" ] && echo "CORS policy enabled"
java -cp CivRestHTTP-1.0-SNAPSHOT-jar-with-dependencies.jar  CivHttpServer ${PORT} ${REDISHOST} ${REDISPORT} ${CORS}
