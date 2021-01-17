FROM adoptopenjdk:11-jre-hotspot
ARG JAR_FILE=target/*spring-boot.jar
COPY ${JAR_FILE} app.jar
COPY ./src/main/resources ./src/main/resources
ENTRYPOINT ["java","-jar","/app.jar"]
