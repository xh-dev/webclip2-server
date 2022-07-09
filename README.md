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