FROM archlinux/base
RUN pacman -Sy --noconfirm jdk11-openjdk unzip && pacman -Scc --noconfirm
ADD https://download2.gluonhq.com/openjfx/11.0.2/openjfx-11.0.2_linux-x64_bin-jmods.zip .
RUN unzip openjfx-11.0.2_linux-x64_bin-jmods.zip && mv javafx-jmods-11.0.2/* /usr/lib/jvm/java-11-openjdk/jmods/ && rmdir javafx-jmods-11.0.2 && rm openjfx-11.0.2_linux-x64_bin-jmods.zip
RUN mkdir ./libraries ./launcher-libraries ./launcher-libraries-compile
COPY ./LaunchServer/build/libs/LaunchServer.jar .
COPY ./LaunchServer/build/libs/libraries ./libraries
COPY ./LaunchServer/build/libs/launcher-libraries ./launcher-libraries
COPY ./LaunchServer/build/libs/launcher-libraries-compile ./launcher-libraries-compile
RUN mkdir ./compat/
COPY ./compat/authlib/authlib-clean.jar ./compat
COPY ./LauncherAuthlib/build/libs/* ./compat/
COPY ./ServerWrapper/build/libs/ServerWrapper.jar ./compat/
RUN mkdir ./compat/modules
COPY ./modules/*_module/build/libs/* ./compat/modules/
COPY ./modules/*_lmodule/build/libs/* ./compat/modules/
CMD java -javaagent:LaunchServer.jar -jar LaunchServer.jar
