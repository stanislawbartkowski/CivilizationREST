. ./rest.rc
echo $PORT
echo $REDISHOST
echo $REDISPORT
echo $CORS

docker build --build-arg PORT=$PORT --build-arg REDISHOST=$REDISHOST --build-arg REDISPORT=$REDISPORT --build-arg CORS=$CORS -t civrest . 
docker run --name civrest -d -p $PORT:$PORT civrest

