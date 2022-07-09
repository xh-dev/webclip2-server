FROM sbtscala/scala-sbt:17.0.2_1.6.2_3.1.3 as sbt-build
ENV branchName ${branchName}
ENV commitId ${commitId}
COPY . /app
WORKDIR /app
RUN ["sbt", "assembly"]

FROM xethhung/jdk11-runner:latest
COPY --from=sbt-build /app/target/scala-2.13/webclip2.jar /app/
WORKDIR /app

# Exposing web port
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "webclip2.jar"]
