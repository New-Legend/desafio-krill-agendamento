# syntax=docker/dockerfile:1.7
# ---- build: compila e gera o jar (cache das dependências em camada separada)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests -Djacoco.skip=true \
 && java -Djarmode=tools -jar target/agendamento-api.jar extract --layers --launcher --destination target/extracted

# ---- runtime: apenas JRE, usuário sem privilégios, camadas do Spring Boot
FROM eclipse-temurin:21-jre-alpine
RUN apk upgrade --no-cache && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./
USER app
ENV TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
