FROM alpine:3.23 AS runtime-builder
RUN apk add --no-cache openjdk21-jdk binutils \
    && jlink --add-modules jdk.httpserver --strip-debug \
        --no-header-files --no-man-pages --compress=zip-6 --output /runtime

RUN mkdir -p /runtime-parts/modules/lib /runtime-parts/server/lib \
    && mv /runtime/lib/modules /runtime-parts/modules/lib/ \
    && mv /runtime/lib/server /runtime-parts/server/lib/

FROM alpine:3.23
RUN apk add --no-cache libstdc++ zlib
COPY --from=runtime-builder /runtime/ /opt/java/
COPY --from=runtime-builder /runtime-parts/modules/ /opt/java/
COPY --from=runtime-builder /runtime-parts/server/ /opt/java/
WORKDIR /app
COPY build/libs/service.jar service.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["/opt/java/bin/java", "-jar", "/app/service.jar"]
