. ./rest.rc
echo $PORT
echo $REDISHOST
echo $REDISPORT

docker build --build-arg PORT=$PORT --build-arg REDISHOST=$REDISHOST --build-arg REDISPORT=$REDISPORT -t civrest . 
docker run --name civrest -d -p $PORT:$PORT civrest

