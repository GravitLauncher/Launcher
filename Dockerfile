FROM ubuntu:latest
RUN apt-get update && apt-get install -y osslsigncode openjdk-11-jdk unzip jq screen
ADD https://download2.gluonhq.com/openjfx/11.0.2/openjfx-11.0.2_linux-x64_bin-jmods.zip .
RUN unzip openjfx-11.0.2_linux-x64_bin-jmods.zip && mv javafx-jmods-11.0.2/* /usr/lib/jvm/java-11-openjdk-amd64/jmods/ && rmdir javafx-jmods-11.0.2 && rm openjfx-11.0.2_linux-x64_bin-jmods.zip
RUN mkdir ./libraries ./launcher-libraries ./launcher-libraries-compile ./compat ./compat/modules
COPY ./LaunchServer/build/libs/LaunchServer.jar .
COPY ./LaunchServer/build/libs/libraries ./libraries
COPY ./LaunchServer/build/libs/launcher-libraries ./launcher-libraries
COPY ./LaunchServer/build/libs/launcher-libraries-compile ./launcher-libraries-compile
COPY ./compat/authlib/authlib-clean.jar ./LauncherAuthlib/build/libs/* ./ServerWrapper/build/libs/ServerWrapper.jar ./compat/
COPY ./modules/*_module/build/libs/* ./modules/*_lmodule/build/libs/* ./compat/modules/
CMD screen -DmS launchserver java -javaagent:LaunchServer.jar -jar LaunchServer.jar
