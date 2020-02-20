FROM gradle:4.10-jdk8 as GradleBuilder

COPY --chown=gradle:gradle build.gradle /home/gradle/MqttWriter/build.gradle
COPY --chown=gradle:gradle gradle.properties /home/gradle/MqttWriter/gradle.properties
COPY --chown=gradle:gradle settings.gradle /home/gradle/MqttWriter/settings.gradle

COPY --chown=gradle:gradle src /home/gradle/MqttWriter/src

WORKDIR /home/gradle/MqttWriter
RUN ls -R
ENV GRADLE_USER_HOME=/home/gradle/MqttWriter
RUN gradle --no-daemon installDist --info --stacktrace

#======
FROM openjdk:8-jre

ENV APP_NAME MqttWriter
ENV APP_HOME /opt/$APP_NAME


COPY --from=GradleBuilder /home/gradle/MqttWriter/build/install/$APP_NAME $APP_HOME
ENTRYPOINT $APP_HOME/bin/$APP_NAME
