FROM gradle:4.10-jdk8 as GradleBuilder

COPY --chown=gradle:gradle build.gradle /home/gradle/PravegaGateway/build.gradle
COPY --chown=gradle:gradle gradle.properties /home/gradle/PravegaGateway/gradle.properties
COPY --chown=gradle:gradle settings.gradle /home/gradle/PravegaGateway/settings.gradle

COPY --chown=gradle:gradle src /home/gradle/PravegaGateway/src
COPY --chown=gradle:gradle lib /home/gradle/PravegaGateway/lib

WORKDIR /home/gradle/PravegaGateway
ENV GRADLE_USER_HOME=/home/gradle/PravegaGateway
RUN gradle --no-daemon installDist --info --stacktrace

#======
FROM openjdk:8-jre

ENV APP_NAME PravegaGateway
ENV APP_HOME /opt/$APP_NAME

COPY --from=GradleBuilder /home/gradle/PravegaGateway/build/install/$APP_NAME $APP_HOME
ENTRYPOINT $APP_HOME/bin/$APP_NAME
