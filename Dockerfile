FROM azul/zulu-openjdk-debian:24-latest AS build
WORKDIR /app
COPY . /app
RUN chmod +x && ./gradlew build --no-daemon
FROM azul/zulu-openjdk-debian:24-latest AS prod
WORKDIR /app/data
ENV APP_HOME=/app
ENV LISTEN_PORT=9274
EXPOSE 9274
COPY --from=build /app/components/launchserver/build/install/launchserver/* /app
ENTRYPOINT ["/app/bin/launchserver"]