# ---- Etapa 1: build ----
# Imagen con Maven 3.9 + Eclipse Temurin JDK 17. Sólo se usa para compilar.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copiamos primero pom.xml para aprovechar la cache de capas:
# si no cambia el pom, las dependencias no se vuelven a descargar.
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

# ---- Etapa 2: runtime ----
# Imagen JRE 17 sin Maven ni código fuente: más pequeña y con menos superficie de ataque.
FROM eclipse-temurin:17-jre
WORKDIR /app

# Usuario no-root: práctica estándar de seguridad para procesos en contenedor.
# El volumen de uploads se crea aquí (con dueño "spring") antes de montarlo:
# Docker copia el contenido y los permisos de este directorio al volumen
# nombrado la primera vez que se monta si está vacío; si se creara después
# de USER spring, el volumen quedaría de root y la app no podría escribir.
RUN useradd --system --uid 1001 spring \
    && mkdir -p /uploads \
    && chown spring:spring /uploads
USER spring

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
