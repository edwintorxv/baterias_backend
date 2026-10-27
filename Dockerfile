# Construcción
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Ejecución
FROM eclipse-temurin:17-jre

RUN groupadd --system app && useradd --system --gid app --no-create-home app

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

USER app

# Perfil de producción y hora de Colombia (Render corre en UTC: sin esto "hoy" y las fechas de
# auditoría saldrían corridas, también en la sesión de PostgreSQL, que toma la zona del JVM)
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Bogota \
    JAVA_TOOL_OPTIONS="-Duser.timezone=America/Bogota -XX:MaxRAMPercentage=75"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
