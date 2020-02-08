FROM openjdk:8
MAINTAINER "sb" <stanislawbartkowski@gmail.com>

ARG PORT
ARG REDISHOST
ARG REDISPORT
ENV PORT=${PORT}
ENV REDISHOST=${REDISHOST}
ENV REDISPORT=${REDISPORT}

COPY target/CivRestHTTP-1.0-SNAPSHOT-jar-with-dependencies.jar .
COPY main.sh .
ENTRYPOINT ["./main.sh"]
