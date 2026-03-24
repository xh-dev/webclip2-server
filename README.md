## Build
```shell
sbt assembly
```

## Run
```shell
java -jar target/scala-2.13/webclip2.jar
```

## Docker
```shell
docker run -p {port}:8080 xethhung/webclip2-server:latest
```


## Test
```shell
curl http://localhost:8080/version -s ; echo
curl http://localhost:8080/config -s ; echo
curl http://localhost:8080/status -s ; echo
export code=$(curl -X POST -d "{\"msg\":\"hixhi\"}" http://localhost:8080/msg/create -s | jq -r ".id")
echo $code
curl -X POST -d "{\"code\":\"$code\"}" http://localhost:8080/msg/retrieve ; echo
```