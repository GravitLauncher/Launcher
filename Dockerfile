FROM azul/zulu-openjdk-debian:24-latest AS build
WORKDIR /app
COPY . /app
RUN chmod +x ./gradlew && ./gradlew build --no-daemon
FROM azul/zulu-openjdk-debian:24-latest AS prod
RUN apt-get update && apt-get install -y --no-install-recommends \
  osslsigncode \
  nano \
  vim \
  rsync \
  socat \
  && rm -rf /var/lib/apt/lists/*
WORKDIR /app/data
ENV APP_HOME=/app
ENV LISTEN_PORT=9274
EXPOSE 9274
VOLUME /app/data
COPY --from=build /app/components/launchserver/build/install/launchserver /app
ENTRYPOINT ["/app/bin/launchserver"]