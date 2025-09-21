FROM azul/zulu-openjdk-debian:24-latest AS build
WORKDIR /app
COPY . /app
RUN chmod +x ./gradlew && ./gradlew build installDist
FROM azul/zulu-openjdk-debian:24-latest AS prod
RUN apt-get update && apt-get install -y --no-install-recommends \
  osslsigncode \
  nano \
  vim \
  rsync \
  socat \
  git \
  unzip \
  curl \
  wget \
  && rm -rf /var/lib/apt/lists/* && \
  wget https://download2.gluonhq.com/openjfx/25/openjfx-25_linux-x64_bin-jmods.zip && \
      unzip openjfx-25_linux-x64_bin-jmods.zip && \
      cp javafx-jmods-25/* /usr/lib/jvm/zulu24/jmods && \
      rm -r javafx-jmods-25 && \
      rm -rf openjfx-25_linux-x64_bin-jmods.zip
WORKDIR /app/data
ENV APP_HOME=/app
ENV LISTEN_PORT=9274
EXPOSE 9274
VOLUME /app/data
COPY --from=build /app/components/launchserver/build/install/launchserver/ /app
ENTRYPOINT ["/app/bin/launchserver"]