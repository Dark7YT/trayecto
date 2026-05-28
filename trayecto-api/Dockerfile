# =============================================================================
# Multi-stage Dockerfile para trayecto-api.
#
# Stage 1 (builder): compila con Maven + JDK 21 dentro del contenedor para que
# el build sea reproducible sin depender del entorno local. Cachea las
# dependencias (~/.m2) en una capa propia copiando primero pom.xml — si solo
# cambia el código fuente, no re-descarga el internet entero.
#
# Stage 2 (runtime): JRE 21 alpine — imagen pequeña (~180MB final), con un
# usuario no-root y health check apuntando a /actuator/health/liveness.
# =============================================================================

# -------------------------- Stage 1: builder ---------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Maven wrapper + descriptor → copiamos primero para que el `go-offline` quede
# cacheado mientras no cambien las dependencias.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline -B

# Ahora sí: copiamos el código y compilamos.
COPY src ./src
RUN ./mvnw -q -DskipTests package -B \
    && mv target/trayecto-api-*.jar /build/app.jar

# -------------------------- Stage 2: runtime ---------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# curl es opcional pero útil para el HEALTHCHECK dentro del contenedor; pesa ~1MB.
RUN apk add --no-cache curl tzdata && \
    addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=builder --chown=app:app /build/app.jar /app/app.jar

USER app
EXPOSE 8080

# JVM tuneado para container de 512MB (Render free tier).
# Cada flag explicada:
#   MaxRAMPercentage=55  → heap ~280MB (deja ~230MB para non-heap: metaspace, code cache, threads, direct buffers)
#   UseSerialGC          → ahorra ~30-50MB vs G1 en heaps chicos; pausas más largas pero aceptables
#   Xss256k              → stack por thread (default 1MB); con ~30 threads ahorra ~25MB
#   TieredStopAtLevel=1  → JIT solo tier 1 (sin C2 compiler); ahorra ~50MB de code cache + heap
#   ReservedCodeCacheSize=48m → limita code cache (default 240MB)
#   CICompilerCount=2    → menos threads del compilador JIT
#   Spring jmx/preinit   → deshabilita features que cargan beans extra al boot
ENV JAVA_OPTS="-XX:MaxRAMPercentage=55.0 \
               -XX:+UseSerialGC \
               -Xss256k \
               -XX:TieredStopAtLevel=1 \
               -XX:ReservedCodeCacheSize=48m \
               -XX:CICompilerCount=2 \
               -XX:+ExitOnOutOfMemoryError \
               -Dspring.jmx.enabled=false \
               -Dspring.backgroundpreinitializer.ignore=true" \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Lima

# Render hace su propio healthcheck (configurado en render.yaml), pero este
# HEALTHCHECK también ayuda a depurar localmente con `docker inspect`.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS "http://localhost:${PORT:-8080}/actuator/health/liveness" || exit 1

# `sh -c` para que ${JAVA_OPTS} se expanda en runtime y permita override por env.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
