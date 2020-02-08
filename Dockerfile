FROM openjdk:8
COPY target/CivRestHTTP-1.0-SNAPSHOT-jar-with-dependencies.jar .
CMD java -cp CivRestHTTP-1.0-SNAPSHOT-jar-with-dependencies.jar  CivHttpServer
