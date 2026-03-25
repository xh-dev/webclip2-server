FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.7_3.3.7 AS sbt-build
ARG branchName
ARG commitId
ENV branchName=${branchName}
ENV commitId=${commitId}
COPY . /app
WORKDIR /app
RUN ["sbt", "assembly"]

#FROM xethhung/jdk11-runner:latest
FROM eclipse-temurin:17-jdk-jammy
COPY --from=sbt-build /app/target/scala-3.3.7/webclip2.jar /app/
WORKDIR /app

# Exposing web port
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "webclip2.jar"]
