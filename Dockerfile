# ---- Estágio 1: build — usa o mvnw do próprio projeto ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copia o wrapper e o pom primeiro. Essa separação cria uma layer de cache:
# enquanto pom.xml e wrapper não mudarem, o Docker reusa as dependências baixadas.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Só agora o código-fonte. Mudou só o src (caso comum)? O build reaproveita
# a layer de dependências acima — build muito mais rápido.
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---- Estágio 2: runtime — só roda o .jar ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Traz APENAS o .jar pronto do estágio de build. Sem Maven, sem código-fonte.
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]