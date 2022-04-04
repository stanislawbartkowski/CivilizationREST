source ./rest.rc
LOGGING=-Djava.util.logging.config.file=logging.properties
exec java $LOGGING -cp target/CivRestHTTP-1.0-SNAPSHOT-jar-with-dependencies.jar CivHttpServer -p $PORT -rh $REDISHOST -rp $REDISPORT $CORS

