# ====== 1. 构建 ======
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
# 如启用 wrapper，请改用 ./mvnw
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/* \
 && mvn -B -DskipTests package

# ====== 2. 运行 ======
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd -r -u 10001 appuser
COPY --from=builder /workspace/target/handwriting-client.jar /app/app.jar
RUN mkdir -p /app/logs && chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
